package com.visteus.sporebreach.biome;

import com.Harbinger.Spore.ExtremelySusThings.CustomJsonReader.SporeConversionData;
import com.Harbinger.Spore.core.Sblocks;
import com.mojang.logging.LogUtils;
import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.chunkloading.ChunkloadManager;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;

/**
 * Orchestration for the dead_scar block decay backlog: once a column reaches {@link
 * BiomePaintData.BiomeStage#DOWNGRADED} (see {@link BiomePaintManager#processDowngrades}), it's
 * enqueued here and periodically force-loaded - purely for this feature, independent of whether a
 * player is anywhere nearby - to sweep its infested/biomass/fungal-growth blocks back toward normal.
 *
 * <p>Ticket ownership itself is deliberately not persisted (see {@link DeadScarDecayData}) - NeoForge's
 * {@code TicketController} (which {@link ChunkloadManager#forceBiomePaintChunk} already goes through)
 * persists forced chunks per-level on its own and silently reinstates them at world load, exactly like
 * {@link ChunkloadManager#activateEntityOwner} already relies on for goal #4's chunkloading. {@link
 * #topUpTickets} re-requesting a ticket for a column already in the persisted backlog is therefore a
 * safe, idempotent resync rather than a "must re-establish from scratch" situation.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class DeadScarDecayManager {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Synthetic, fixed ticket owner shared by every column this feature force-loads. */
    private static final UUID DECAY_TICKET_OWNER =
            UUID.nameUUIDFromBytes("sporebreach:dead_scar_decay".getBytes(StandardCharsets.UTF_8));

    private static final TagKey<Block> BIOMASS_TAG = TagKey.create(
            Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(SporeContainmentBreach.MODID, "biomass")
    );

    /** Positions per Y-layer of a chunk column (16x16). */
    private static final int POSITIONS_PER_LAYER = 16 * 16;

    /** Cached once per column at sweep-start; see {@link #sampleIsCold}. */
    private record ColumnSweep(boolean cold) {
    }

    private static final Map<ResourceKey<Level>, Map<ChunkPos, ColumnSweep>> sweepsByDimension = new HashMap<>();

    private DeadScarDecayManager() {
    }

    /** Called from {@link BiomePaintManager#processDowngrades} right after a successful downgrade. */
    public static void enqueue(ServerLevel level, ChunkPos pos) {
        DeadScarDecayData.enqueue(level, pos);
    }

    /**
     * Called unconditionally from {@link BiomePaintManager#discover} right after every {@link
     * BiomePaintData#claim} - harmless no-op if {@code pos} was never enqueued. Releases the
     * decay-sweep ticket immediately if one was held.
     */
    public static void cancel(ServerLevel level, ChunkPos pos) {
        DeadScarDecayData.remove(level, pos);
        Map<ChunkPos, ColumnSweep> sweeps = sweepsByDimension.get(level.dimension());
        if (sweeps != null && sweeps.remove(pos) != null) {
            ChunkloadManager.forceBiomePaintChunk(level, DECAY_TICKET_OWNER, pos.x, pos.z, false);
        }
    }

    /**
     * Called every deadScarDecayIntervalTicks by {@link DeadScarDecayDirector}. Tops up this
     * dimension's currently-ticketed sweep set up to deadScarDecayMaxConcurrentSweeps, pulling from
     * the persisted backlog - the actual performance lever bounding how many extra chunks this
     * feature can ever force-load at once regardless of how large the backlog grows.
     */
    public static void topUpTickets(ServerLevel level) {
        int maxConcurrent = SporeBreachServerConfig.DEAD_SCAR_DECAY_MAX_CONCURRENT_SWEEPS.get();
        Map<ChunkPos, ColumnSweep> sweeps = sweepsByDimension.computeIfAbsent(level.dimension(), key -> new HashMap<>());

        for (Map.Entry<ChunkPos, Integer> entry : DeadScarDecayData.snapshotPending(level).entrySet()) {
            if (sweeps.size() >= maxConcurrent) {
                return;
            }
            ChunkPos pos = entry.getKey();
            if (sweeps.containsKey(pos)) {
                continue;
            }

            BiomePaintData.ColumnState state = BiomePaintData.getState(level, pos);
            if (state == null || state.stage() != BiomePaintData.BiomeStage.DOWNGRADED) {
                // Reclaimed (or never actually downgraded) since being enqueued - drop it rather
                // than force-loading a column that no longer needs decaying.
                DeadScarDecayData.remove(level, pos);
                continue;
            }

            ChunkloadManager.forceBiomePaintChunk(level, DECAY_TICKET_OWNER, pos.x, pos.z, true);
            sweeps.put(pos, new ColumnSweep(sampleIsCold(level, pos)));
        }
    }

    /**
     * Called every deadScarDecayIntervalTicks by {@link DeadScarDecayDirector}, after {@link
     * #topUpTickets}. Advances every currently-ticketed column's sweep by up to
     * deadScarDecayBlocksPerPass examined positions.
     */
    public static void advanceSweeps(ServerLevel level) {
        Map<ChunkPos, ColumnSweep> sweeps = sweepsByDimension.get(level.dimension());
        if (sweeps == null || sweeps.isEmpty()) {
            return;
        }
        int budget = SporeBreachServerConfig.DEAD_SCAR_DECAY_BLOCKS_PER_PASS.get();

        for (ChunkPos pos : List.copyOf(sweeps.keySet())) {
            // Defensive re-check, mirroring AreaWaterReplacementJob.isColumnActive's rationale -
            // belt-and-suspenders against a missed cancel() call.
            BiomePaintData.ColumnState state = BiomePaintData.getState(level, pos);
            if (state == null || state.stage() != BiomePaintData.BiomeStage.DOWNGRADED) {
                cancel(level, pos);
                continue;
            }

            LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
            if (chunk == null) {
                continue;
            }

            ColumnSweep sweep = sweeps.get(pos);
            boolean finished = advanceColumn(level, chunk, pos, sweep.cold(), budget);
            if (finished) {
                sweeps.remove(pos);
                DeadScarDecayData.remove(level, pos);
                ChunkloadManager.forceBiomePaintChunk(level, DECAY_TICKET_OWNER, pos.x, pos.z, false);
                LOGGER.debug("sporebreach: finished dead scar decay sweep of chunk {}", pos);
            }
        }
    }

    /**
     * Examines up to {@code budget} non-air positions in {@code chunkPos} starting from its
     * persisted cursor, skipping whole {@link LevelChunkSection}s that report {@link
     * LevelChunkSection#hasOnlyAir()} for free (not counted against {@code budget}) so tall empty
     * stretches above/below any actual structure cost O(1), not O(height). Returns true once the
     * whole column has been examined.
     */
    private static boolean advanceColumn(ServerLevel level, LevelChunk chunk, ChunkPos chunkPos, boolean cold, int budget) {
        int minY = level.getMinBuildHeight();
        int totalIndices = (level.getMaxBuildHeight() - minY) * POSITIONS_PER_LAYER;

        int storedCursor = DeadScarDecayData.getCursor(level, chunkPos);
        int index = storedCursor == DeadScarDecayData.NOT_STARTED ? 0 : storedCursor;

        int examined = 0;
        int steps = 0;
        int maxSteps = Math.max(budget * 20, 256);
        while (examined < budget && index < totalIndices && steps < maxSteps) {
            steps++;
            int y = minY + index / POSITIONS_PER_LAYER;
            int rem = index % POSITIONS_PER_LAYER;
            int localX = rem / 16;
            int localZ = rem % 16;

            LevelChunkSection section = chunk.getSection(level.getSectionIndex(y));
            if (section.hasOnlyAir()) {
                int nextSectionMinY = SectionPos.sectionToBlockCoord(SectionPos.blockToSectionCoord(y) + 1);
                index = (nextSectionMinY - minY) * POSITIONS_PER_LAYER;
                continue;
            }

            BlockPos pos = new BlockPos(chunkPos.getMinBlockX() + localX, y, chunkPos.getMinBlockZ() + localZ);
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) {
                applyDecay(level, pos, state, cold);
            }
            examined++;
            index++;
        }

        if (index >= totalIndices) {
            return true;
        }
        DeadScarDecayData.advanceCursor(level, chunkPos, index);
        return false;
    }

    /**
     * Biomass and {@code spore:remains} have a cold-biome target that the static, data-driven {@link
     * SporeConversionData} table can't express, so they're special-cased here; everything else goes
     * through the generic {@code data/sporebreach/spore_block_conversion/dead_scar_decay.json}-driven
     * lookup.
     */
    private static void applyDecay(ServerLevel level, BlockPos pos, BlockState state, boolean cold) {
        Block block = state.getBlock();
        Block target;
        if (state.is(BIOMASS_TAG)) {
            target = cold ? Sblocks.FROST_BURNED_BIOMASS.get() : Blocks.AIR;
        } else if (block == Sblocks.REMAINS.get()) {
            target = cold ? Sblocks.FROZEN_REMAINS.get() : Blocks.AIR;
        } else {
            target = SporeConversionData.getResult(block);
        }
        if (target != null && target != block) {
            level.setBlock(pos, target.defaultBlockState(), 3);
        }
    }

    /**
     * Samples the biome that would have naturally generated at {@code pos} - bypassing whatever
     * biome {@link BiomeRepaint} has since painted into the chunk's stored per-column biome array,
     * since {@code fillBiomesFromNoise} never touches the level's underlying {@code BiomeSource}/
     * climate noise function - and checks vanilla's own "is it cold enough here" method, so
     * temperature-modifier biomes (e.g. frozen oceans) are handled the same way vanilla itself does.
     */
    private static boolean sampleIsCold(ServerLevel level, ChunkPos pos) {
        int x = pos.getMinBlockX() + 8;
        int z = pos.getMinBlockZ() + 8;
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);

        Holder<Biome> biome = level.getChunkSource().getGenerator().getBiomeSource().getNoiseBiome(
                QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z),
                level.getChunkSource().randomState().sampler()
        );
        return biome.value().coldEnoughToSnow(new BlockPos(x, y, z));
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        sweepsByDimension.clear();
    }
}
