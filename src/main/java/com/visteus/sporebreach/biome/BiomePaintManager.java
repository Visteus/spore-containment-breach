package com.visteus.sporebreach.biome;

import com.mojang.logging.LogUtils;
import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.chunkloading.ChunkCircleOffsets;
import com.visteus.sporebreach.chunkloading.ChunkloadData;
import com.visteus.sporebreach.chunkloading.ChunkloadManager;
import com.visteus.sporebreach.chunkloading.ChunkloadOwnerId;
import com.visteus.sporebreach.chunkloading.ChunkloadState;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;

/**
 * Orchestration core for Goal #5's biome painting, mirroring {@link ChunkloadManager}'s
 * radius-growth shape but piggybacking directly on each organoid's already-tracked chunkload
 * radius instead of tracking a second age/growth curve: biome target radius is always
 * {@code chunkloadState.lastIssuedRadius() + biomePaintExtraRadiusChunks}. Force-loading (via
 * {@link ChunkloadManager#forceBiomePaintChunk}) and claiming ({@link BiomePaintData#claim})
 * happen immediately when a chunk enters that target radius; the actual repaint is deferred onto
 * a small per-dimension queue drained at a budgeted rate by {@link #advance}, both to spread the
 * cost and to dodge an empirically-observed quirk where a chunk force-loaded for the first time in
 * a session can briefly report itself fully loaded while an async finalization pass is still in
 * flight, silently swallowing a repaint attempted in that same window - see {@link #SETTLE_TICKS}.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class BiomePaintManager {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Minimum age (in game ticks) a chunk must have been force-loaded before its first paint attempt. */
    private static final long SETTLE_TICKS = 40L;
    /** Backoff before retrying a column whose chunk wasn't actually loaded yet when attempted. */
    private static final long RETRY_DELAY_TICKS = 20L;

    private static final Map<UUID, Integer> lastForceLoadedRadius = new HashMap<>();
    private static final Map<ResourceKey<Level>, Deque<PaintTask>> paintQueueByDimension = new HashMap<>();
    private static final Map<ResourceKey<Level>, AreaWaterReplacementJob> waterJobByDimension = new HashMap<>();

    private record PaintTask(UUID ownerId, ChunkPos pos, long readyAtGameTime) {
    }

    private BiomePaintManager() {
    }

    /**
     * Called every biomePaintRecheckIntervalTicks by BiomePaintGrowthDirector. For each tracked
     * chunkload entity owner, grows its biome radius toward the current target if behind.
     */
    public static void recheckAll(ServerLevel level) {
        int extraRadius = SporeBreachServerConfig.BIOME_PAINT_EXTRA_RADIUS_CHUNKS.get();
        for (Map.Entry<ChunkloadOwnerId, ChunkloadState> owner : ChunkloadData.snapshot(level).entrySet()) {
            if (!(owner.getKey() instanceof ChunkloadOwnerId.EntityOwner entityOwner)) {
                continue;
            }
            growOwner(level, entityOwner.entityId(), owner.getValue(), extraRadius);
        }
    }

    private static void growOwner(ServerLevel level, UUID ownerId, ChunkloadState chunkloadState, int extraRadius) {
        int targetRadius = chunkloadState.lastIssuedRadius() + extraRadius;
        int previousRadius = lastForceLoadedRadius.getOrDefault(ownerId, 0);
        if (targetRadius <= previousRadius) {
            return;
        }

        Set<ChunkCircleOffsets.ChunkOffset> toAdd = new LinkedHashSet<>(ChunkCircleOffsets.fullOffsets(targetRadius));
        toAdd.removeAll(ChunkCircleOffsets.fullOffsets(previousRadius));

        long readyAt = level.getGameTime() + SETTLE_TICKS;
        ChunkPos anchor = chunkloadState.anchorChunk();
        Deque<PaintTask> queue = paintQueueByDimension.computeIfAbsent(level.dimension(), key -> new ArrayDeque<>());
        for (ChunkCircleOffsets.ChunkOffset offset : toAdd) {
            ChunkPos pos = new ChunkPos(anchor.x + offset.dx(), anchor.z + offset.dz());
            BiomePaintData.ClaimResult result = BiomePaintData.claim(level, pos, ownerId);
            if (result.needsTicket()) {
                ChunkloadManager.forceBiomePaintChunk(level, ownerId, pos.x, pos.z, true);
            }
            if (result.needsPaint()) {
                queue.addLast(new PaintTask(ownerId, pos, readyAt));
            }
        }
        lastForceLoadedRadius.put(ownerId, targetRadius);
    }

    /**
     * Called every biomePaintRecheckIntervalTicks by BiomePaintGrowthDirector, after
     * {@link #recheckAll}. Drains up to biomePaintColumnsPerPass ready columns from this
     * dimension's paint queue, repainting each to {@link BiomeRepaint#INFECTION_ZONE} and logging
     * one line per success - the log is deliberately here (not buried in BiomeRepaint) since it's
     * meant as an RCON-testable signal that spread is actually happening.
     */
    public static void advance(ServerLevel level) {
        Deque<PaintTask> queue = paintQueueByDimension.get(level.dimension());
        if (queue == null || queue.isEmpty()) {
            return;
        }

        int budget = SporeBreachServerConfig.BIOME_PAINT_COLUMNS_PER_PASS.get();
        long now = level.getGameTime();
        int painted = 0;
        while (painted < budget) {
            PaintTask task = queue.peekFirst();
            if (task == null || task.readyAtGameTime() > now) {
                return;
            }
            queue.pollFirst();

            if (BiomeRepaint.paintColumn(level, task.pos(), BiomeRepaint.INFECTION_ZONE)) {
                painted++;
                LOGGER.info(
                        "spore_containment_breach: painted chunk {} to {} for owner {}",
                        task.pos(), BiomeRepaint.INFECTION_ZONE.location(), task.ownerId()
                );
                if (SporeBreachServerConfig.AREA_WATER_REPLACEMENT_ENABLED.get()) {
                    waterJobByDimension.computeIfAbsent(level.dimension(), key -> new AreaWaterReplacementJob())
                            .seedColumn(level, task.pos());
                }
            } else {
                queue.addLast(new PaintTask(task.ownerId(), task.pos(), now + RETRY_DELAY_TICKS));
            }
        }
    }

    /**
     * Called every biomePaintRecheckIntervalTicks by BiomePaintGrowthDirector, alongside {@link
     * #advance}. Drains this dimension's shared water-crusting frontier (seeded by {@link
     * #advance} whenever a column is freshly painted) at areaWaterReplacementBlocksPerPass,
     * regardless of how many columns were painted that same pass - see {@link
     * AreaWaterReplacementJob}'s own javadoc for why the frontier is shared rather than per-column.
     */
    public static void advanceWater(ServerLevel level) {
        if (!SporeBreachServerConfig.AREA_WATER_REPLACEMENT_ENABLED.get()) {
            return;
        }
        AreaWaterReplacementJob job = waterJobByDimension.get(level.dimension());
        if (job == null || job.isEmpty()) {
            return;
        }
        int budget = SporeBreachServerConfig.AREA_WATER_REPLACEMENT_BLOCKS_PER_PASS.get();
        int depth = SporeBreachServerConfig.AREA_WATER_REPLACEMENT_DEPTH.get();
        job.advance(level, level.getRandom(), budget, depth);
    }

    /**
     * Called every biomePaintRecheckIntervalTicks by BiomePaintGrowthDirector, after {@link
     * #advance}. Repaints any column whose lingering-scar countdown has elapsed to {@link
     * BiomeRepaint#DEAD_SCAR}, releasing its force-load ticket only once that repaint actually
     * succeeds - a column whose chunk happens to not be loaded right now is left exactly as due,
     * so it retries every subsequent pass rather than the downgrade silently never taking visual
     * effect.
     */
    public static void processDowngrades(ServerLevel level) {
        long now = level.getGameTime();
        for (ChunkPos pos : BiomePaintData.peekDuePendingDowngrades(level, now)) {
            if (!BiomeRepaint.paintColumn(level, pos, BiomeRepaint.DEAD_SCAR)) {
                continue;
            }
            UUID ticketOwner = BiomePaintData.markDowngraded(level, pos);
            if (ticketOwner != null) {
                ChunkloadManager.forceBiomePaintChunk(level, ticketOwner, pos.x, pos.z, false);
            }
            LOGGER.debug("spore_containment_breach: downgraded chunk {} to {}", pos, BiomeRepaint.DEAD_SCAR.location());
        }
    }

    /** Last biome radius (chunks) fully force-loaded/claimed for this owner - 0 if never grown. */
    public static int getLastForceLoadedRadius(UUID ownerId) {
        return lastForceLoadedRadius.getOrDefault(ownerId, 0);
    }

    /** Columns still waiting for their repaint pass in this dimension - for status inspection. */
    public static int pendingPaintCount(ServerLevel level) {
        Deque<PaintTask> queue = paintQueueByDimension.get(level.dimension());
        return queue == null ? 0 : queue.size();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        lastForceLoadedRadius.clear();
        paintQueueByDimension.clear();
        waterJobByDimension.clear();
    }
}
