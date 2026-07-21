package com.visteus.sporebreach.biome;

import com.mojang.logging.LogUtils;
import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.chunkloading.ChunkCircleOffsets;
import com.visteus.sporebreach.chunkloading.ChunkloadData;
import com.visteus.sporebreach.chunkloading.ChunkloadManager;
import com.visteus.sporebreach.chunkloading.ChunkloadOwnerId;
import com.visteus.sporebreach.chunkloading.ChunkloadState;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;

/**
 * Orchestration core for Goal #5's biome painting, mirroring {@link ChunkloadManager}'s
 * radius-growth shape but piggybacking directly on each organoid's already-tracked chunkload
 * radius instead of tracking a second age/growth curve: the boundary a spread is allowed to reach
 * is always {@code chunkloadState.lastIssuedRadius() + biomePaintExtraRadiusChunks}. Unlike a plain
 * radius-ring dump, each owner grows from a single seed at its anchor chunk via its own BFS
 * frontier - copying {@link AreaWaterReplacementJob}'s "only the 8 neighbors of an
 * already-converted cell become candidates" shape - so the visible spread genuinely radiates
 * outward from the organoid one ring of adjacency at a time rather than the whole newly-unlocked
 * ring appearing at once in scan order.
 *
 * <p>Each owner's frontier is a min-priority-queue ordered by squared distance from its anchor,
 * not a plain FIFO - 8-directional BFS discovery grows in Chebyshev (square) rings, but {@link
 * ChunkCircleOffsets}'s boundary is a true circle, so draining strictly by discovery order would
 * process a corner chunk before a same-or-lesser-Chebyshev-layer edge chunk that's actually closer
 * to the anchor, visibly squaring off the spread instead of rounding it. Ordering by true distance
 * makes the boundary check on the queue's head still valid as a hard stop for the whole pass -
 * since membership in {@link ChunkCircleOffsets#fullOffsets} is itself a distance threshold, if the
 * closest pending chunk is outside it, everything farther behind it is too.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class BiomePaintManager {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Minimum age (in game ticks) a chunk must have been force-loaded before its first paint attempt. */
    private static final long SETTLE_TICKS = 40L;
    /** Backoff before retrying a column whose chunk wasn't actually loaded yet when attempted. */
    private static final long RETRY_DELAY_TICKS = 20L;

    private static final int[][] NEIGHBOR_OFFSETS = {
            {-1, -1}, {-1, 0}, {-1, 1},
            {0, -1}, {0, 1},
            {1, -1}, {1, 0}, {1, 1}
    };

    private static final Map<UUID, Integer> allowedRadiusByOwner = new HashMap<>();
    private static final Map<UUID, Queue<PaintTask>> frontierByOwner = new HashMap<>();
    private static final Map<UUID, Set<ChunkPos>> discoveredByOwner = new HashMap<>();
    private static final Map<ResourceKey<Level>, AreaWaterReplacementJob> waterJobByDimension = new HashMap<>();

    /**
     * A chunk this owner's spread has reached. {@code needsPaint} is resolved once, at discovery
     * (via {@link BiomePaintData#claim}), rather than re-checked at processing time, since {@code
     * claim} isn't safe to call twice for the same (owner, chunk) pair - a second call would see
     * the column already {@code ACTIVE} and wrongly report "no paint needed" even if the first
     * call's actual paint attempt hasn't happened yet.
     */
    private record PaintTask(ChunkPos pos, boolean needsPaint, long readyAtGameTime) {
    }

    private BiomePaintManager() {
    }

    /**
     * Called every biomePaintRecheckIntervalTicks by BiomePaintGrowthDirector. Updates each tracked
     * chunkload entity owner's currently-allowed spread radius, and seeds a brand new owner's
     * frontier at its anchor chunk the first time it's seen.
     */
    public static void recheckAll(ServerLevel level) {
        int extraRadius = SporeBreachServerConfig.BIOME_PAINT_EXTRA_RADIUS_CHUNKS.get();
        for (Map.Entry<ChunkloadOwnerId, ChunkloadState> owner : ChunkloadData.snapshot(level).entrySet()) {
            if (!(owner.getKey() instanceof ChunkloadOwnerId.EntityOwner entityOwner)) {
                continue;
            }
            UUID ownerId = entityOwner.entityId();
            ChunkloadState chunkloadState = owner.getValue();
            allowedRadiusByOwner.put(ownerId, chunkloadState.lastIssuedRadius() + extraRadius);

            if (!discoveredByOwner.containsKey(ownerId)) {
                ChunkPos anchor = chunkloadState.anchorChunk();
                Set<ChunkPos> discovered = new HashSet<>();
                Queue<PaintTask> frontier = new PriorityQueue<>(Comparator.comparingLong(task -> distanceSq(task.pos(), anchor)));
                discoveredByOwner.put(ownerId, discovered);
                frontierByOwner.put(ownerId, frontier);
                discover(level, ownerId, anchor, frontier, discovered, level.getGameTime());

                if (SporeBreachServerConfig.AREA_WATER_REPLACEMENT_ENABLED.get()) {
                    waterJobByDimension.computeIfAbsent(level.dimension(), key -> new AreaWaterReplacementJob())
                            .seedWithinChunk(level, anchor);
                }
            }
        }
    }

    /**
     * Claims and (if needed) force-loads {@code pos} for {@code ownerId} exactly once, then queues
     * it for an eventual paint attempt - immediately, if the column is already active (another
     * owner reached it first, or a reactivation), or after {@link #SETTLE_TICKS} if this owner
     * needs to actually call {@link BiomeRepaint}.
     */
    private static void discover(
            ServerLevel level, UUID ownerId, ChunkPos pos, Queue<PaintTask> frontier, Set<ChunkPos> discovered, long now
    ) {
        if (!discovered.add(pos)) {
            return;
        }
        BiomePaintData.ClaimResult result = BiomePaintData.claim(level, pos, ownerId);
        if (result.needsTicket()) {
            ChunkloadManager.forceBiomePaintChunk(level, ownerId, pos.x, pos.z, true);
        }
        long readyAt = result.needsPaint() ? now + SETTLE_TICKS : now;
        frontier.offer(new PaintTask(pos, result.needsPaint(), readyAt));
    }

    private static long distanceSq(ChunkPos pos, ChunkPos anchor) {
        long dx = pos.x - anchor.x;
        long dz = pos.z - anchor.z;
        return dx * dx + dz * dz;
    }

    /**
     * Called every biomePaintRecheckIntervalTicks by BiomePaintGrowthDirector, after {@link
     * #recheckAll}. Round-robins each owner's frontier against a shared per-dimension paint
     * budget, only popping a candidate once it's both within the owner's currently-allowed radius
     * and past its settle delay - anything blocked on either is left exactly where it is (nothing
     * farther out in the same frontier could be any less blocked), so a pass simply stops early for
     * that owner rather than skipping ahead out of spread order.
     */
    public static void advance(ServerLevel level) {
        int budget = SporeBreachServerConfig.BIOME_PAINT_COLUMNS_PER_PASS.get();
        long now = level.getGameTime();

        for (Map.Entry<ChunkloadOwnerId, ChunkloadState> owner : ChunkloadData.snapshot(level).entrySet()) {
            if (budget <= 0) {
                return;
            }
            if (!(owner.getKey() instanceof ChunkloadOwnerId.EntityOwner entityOwner)) {
                continue;
            }
            UUID ownerId = entityOwner.entityId();
            Queue<PaintTask> frontier = frontierByOwner.get(ownerId);
            if (frontier == null || frontier.isEmpty()) {
                continue;
            }
            budget -= drainFrontier(
                    level, ownerId, frontier, discoveredByOwner.get(ownerId),
                    owner.getValue().anchorChunk(), allowedRadiusByOwner.getOrDefault(ownerId, 0), now, budget
            );
        }
    }

    /** Returns how much of {@code budget} was actually spent on real paint calls. */
    private static int drainFrontier(
            ServerLevel level, UUID ownerId, Queue<PaintTask> frontier, Set<ChunkPos> discovered,
            ChunkPos anchor, int allowedRadius, long now, int budget
    ) {
        Set<ChunkCircleOffsets.ChunkOffset> allowedOffsets = ChunkCircleOffsets.fullOffsets(allowedRadius);
        int consumed = 0;
        int steps = 0;
        int maxSteps = Math.max(budget * 5, 20);
        while (consumed < budget && steps < maxSteps && !frontier.isEmpty()) {
            PaintTask task = frontier.peek();
            ChunkCircleOffsets.ChunkOffset offset = new ChunkCircleOffsets.ChunkOffset(task.pos().x - anchor.x, task.pos().z - anchor.z);
            if (!allowedOffsets.contains(offset) || task.readyAtGameTime() > now) {
                break;
            }
            frontier.poll();
            steps++;

            boolean confirmedPainted;
            if (task.needsPaint()) {
                if (BiomeRepaint.paintColumn(level, task.pos(), BiomeRepaint.INFECTION_ZONE)) {
                    confirmedPainted = true;
                    consumed++;
                    LOGGER.info(
                            "sporebreach: painted chunk {} to {} for owner {}",
                            task.pos(), BiomeRepaint.INFECTION_ZONE.location(), ownerId
                    );
                } else {
                    frontier.offer(new PaintTask(task.pos(), true, now + RETRY_DELAY_TICKS));
                    confirmedPainted = false;
                }
            } else {
                confirmedPainted = true;
            }

            if (confirmedPainted) {
                for (int[] neighborOffset : NEIGHBOR_OFFSETS) {
                    ChunkPos neighbor = new ChunkPos(task.pos().x + neighborOffset[0], task.pos().z + neighborOffset[1]);
                    discover(level, ownerId, neighbor, frontier, discovered, now);
                }
            }
        }
        return consumed;
    }

    /**
     * Called every biomePaintRecheckIntervalTicks by BiomePaintGrowthDirector, alongside {@link
     * #advance}. Drains this dimension's shared water-crusting frontier - seeded once per owner in
     * {@link #recheckAll} from the water tile nearest its anchor, then grown by {@link
     * AreaWaterReplacementJob} itself via water connectivity - at areaWaterReplacementBlocksPerPass.
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
     * Called every areaWaterReseedIntervalTicks by BiomePaintGrowthDirector - a separate, much
     * slower cadence than paint/water advancement, mirroring the recheck+chance shape {@code
     * MoundStructureGrowth}/{@code ProtoStructureGrowth} use for their own secondary structures.
     * Per tracked organoid, rolls areaWaterReseedChance; on success, seeds a random chunk within its
     * current biome radius via {@link AreaWaterReplacementJob#seedWithinChunk}. This is what
     * eventually reaches a pond that's within an organoid's territory but not water-connected to
     * anything nearer its anchor - {@link #recheckAll}'s one-time near-anchor seed alone would never
     * find it, since {@link AreaWaterReplacementJob} only spreads along connected water.
     */
    public static void tryReseedWater(ServerLevel level) {
        if (!SporeBreachServerConfig.AREA_WATER_REPLACEMENT_ENABLED.get()) {
            return;
        }
        double chance = SporeBreachServerConfig.AREA_WATER_RESEED_CHANCE.get();
        if (chance <= 0) {
            return;
        }

        RandomSource random = level.getRandom();
        for (Map.Entry<ChunkloadOwnerId, ChunkloadState> owner : ChunkloadData.snapshot(level).entrySet()) {
            if (!(owner.getKey() instanceof ChunkloadOwnerId.EntityOwner entityOwner)) {
                continue;
            }
            if (random.nextDouble() >= chance) {
                continue;
            }

            UUID ownerId = entityOwner.entityId();
            int allowedRadius = allowedRadiusByOwner.getOrDefault(ownerId, 0);
            if (allowedRadius <= 0) {
                continue;
            }

            ChunkPos target = randomChunkWithin(owner.getValue().anchorChunk(), allowedRadius, random);
            waterJobByDimension.computeIfAbsent(level.dimension(), key -> new AreaWaterReplacementJob())
                    .seedWithinChunk(level, target);
        }
    }

    private static ChunkPos randomChunkWithin(ChunkPos anchor, int radius, RandomSource random) {
        Set<ChunkCircleOffsets.ChunkOffset> offsets = ChunkCircleOffsets.fullOffsets(radius);
        int skip = random.nextInt(offsets.size());
        Iterator<ChunkCircleOffsets.ChunkOffset> iterator = offsets.iterator();
        ChunkCircleOffsets.ChunkOffset offset = iterator.next();
        for (int i = 0; i < skip; i++) {
            offset = iterator.next();
        }
        return new ChunkPos(anchor.x + offset.dx(), anchor.z + offset.dz());
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
            LOGGER.debug("sporebreach: downgraded chunk {} to {}", pos, BiomeRepaint.DEAD_SCAR.location());
        }
    }

    /** Current spread boundary (chunks) for this owner - 0 if never grown. */
    public static int getAllowedRadius(UUID ownerId) {
        return allowedRadiusByOwner.getOrDefault(ownerId, 0);
    }

    /** Chunks this owner's spread has discovered but not yet finished processing. */
    public static int pendingFrontierCount(UUID ownerId) {
        Queue<PaintTask> frontier = frontierByOwner.get(ownerId);
        return frontier == null ? 0 : frontier.size();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        allowedRadiusByOwner.clear();
        frontierByOwner.clear();
        discoveredByOwner.clear();
        waterJobByDimension.clear();
    }
}
