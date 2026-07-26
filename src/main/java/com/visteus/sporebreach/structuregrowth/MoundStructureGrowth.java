package com.visteus.sporebreach.structuregrowth;

import com.Harbinger.Spore.Sentities.Organoids.Mound;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import com.visteus.sporebreach.spawning.SpawnAnchors;
import com.visteus.sporebreach.structuregrowth.StructureFootprintData.Kind;
import com.visteus.sporebreach.structuregrowth.StructureFootprintData.Record;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Grows the goal #3 "small fortress" of structures around a single Mound: the first structure is
 * always centered on the Mound itself (terrain-anchored, collision-pushed - see
 * {@link StructureGrowthJob}); the 2nd+ keep a random offset spread away from earlier anchors.
 * Each surface structure may optionally grow an underground companion beneath it once complete.
 * What's already been built is tracked in {@link StructureFootprintData}, so both the per-Mound cap
 * and an unfinished tower survive an unload.
 */
public final class MoundStructureGrowth {

    private static final Map<UUID, OrganoidStructureState> STATE = new HashMap<>();

    private MoundStructureGrowth() {
    }

    /** Called on the recheck cadence: decide whether to start a new job for this Mound. */
    public static void tryStartJob(ServerLevel level, Mound mound) {
        OrganoidStructureState state = STATE.computeIfAbsent(mound.getUUID(), id -> new OrganoidStructureState());
        if (state.hasActiveJob()) {
            return;
        }
        if (resumeUnfinishedJob(level, mound, state)) {
            return;
        }

        if (mound.getAge() < SporeBreachServerConfig.MOUND_STRUCTURE_MIN_AGE.get()) {
            return;
        }
        if (StructureFootprintData.countOwned(level, mound.getUUID(), Kind.SURFACE)
                >= SporeBreachServerConfig.MOUND_STRUCTURE_MAX_PER_MOUND.get()) {
            return;
        }

        RandomSource random = mound.getRandom();
        if (random.nextDouble() >= SporeBreachServerConfig.GROWTH_PLACEMENT_CHANCE.get()) {
            return;
        }

        StructurePool pool = StructurePool.fromConfig(SporeBreachServerConfig.MOUND_STRUCTURE_POOL.get());
        Optional<StructurePoolEntry> entry = pool.pickWeighted(random);
        if (entry.isEmpty()) {
            return;
        }

        BlockPos anchor = resolveAnchor(level, mound, random);
        if (anchor == null) {
            return;
        }

        StructureTemplate template = OrganoidStructurePlacer.resolveTemplate(level, entry.get().structureId());
        BlockPos origin = OrganoidStructurePlacer.jobOrigin(template, anchor, false);
        Record record = StructureFootprintData.claim(
                level, mound.getUUID(), Kind.SURFACE, entry.get().structureId(), anchor,
                OrganoidStructurePlacer.footprint(template, origin), false
        );
        state.setSurfaceJob(OrganoidStructurePlacer.buildJobAtOrigin(template, origin, false), record);
        state.setPendingUndergroundAnchor(anchor);
    }

    /** Called on the pass cadence: advance whichever job is currently running for this Mound. */
    public static void tryAdvanceJob(ServerLevel level, Mound mound) {
        OrganoidStructureState state = STATE.get(mound.getUUID());
        if (state == null) {
            return;
        }

        int blocksPerPass = SporeBreachServerConfig.MOUND_STRUCTURE_BLOCKS_PER_PASS.get();
        RandomSource random = mound.getRandom();
        if (state.surfaceJob() != null) {
            state.surfaceJob().advance(level, mound, random, blocksPerPass);
            if (state.surfaceJob().isComplete()) {
                StructureFootprintData.markComplete(state.surfaceRecord());
                state.clearSurfaceJob();
                maybeStartUnderground(level, mound, state);
            }
        } else if (state.undergroundJob() != null) {
            state.undergroundJob().advance(level, mound, random, blocksPerPass);
            if (state.undergroundJob().isComplete()) {
                StructureFootprintData.markComplete(state.undergroundRecord());
                state.clearUndergroundJob();
            }
        }
    }

    /**
     * Rebuilds a job left unfinished by an unload. Growth re-walks the whole template rather than
     * resuming at an exact block count - already-placed material is itself replaceable, so those
     * positions are simply re-set to the state they already hold.
     */
    private static boolean resumeUnfinishedJob(ServerLevel level, Mound mound, OrganoidStructureState state) {
        UUID id = mound.getUUID();
        Optional<Record> surface = StructureFootprintData.incompleteFor(level, id, Kind.SURFACE);
        if (surface.isPresent()) {
            Record record = surface.get();
            StructureTemplate template = OrganoidStructurePlacer.resolveTemplate(level, record.structureId());
            state.setSurfaceJob(
                    OrganoidStructurePlacer.buildJobAtOrigin(template, record.origin(), record.growDownward()), record
            );
            state.setPendingUndergroundAnchor(record.anchor());
            return true;
        }

        Optional<Record> underground = StructureFootprintData.incompleteFor(level, id, Kind.UNDERGROUND);
        if (underground.isPresent()) {
            Record record = underground.get();
            StructureTemplate template = OrganoidStructurePlacer.resolveTemplate(level, record.structureId());
            state.setUndergroundJob(
                    OrganoidStructurePlacer.buildJobAtOrigin(template, record.origin(), record.growDownward()), record
            );
            return true;
        }
        return false;
    }

    private static BlockPos resolveAnchor(ServerLevel level, Mound mound, RandomSource random) {
        if (StructureFootprintData.countOwned(level, mound.getUUID(), Kind.SURFACE) == 0) {
            return new BlockPos(mound.getBlockX(), mound.getBlockY() - 2, mound.getBlockZ());
        }

        int minDistance = SporeBreachServerConfig.MOUND_STRUCTURE_MIN_DISTANCE.get();
        Optional<BlockPos> candidate = SpawnAnchors.findGroundPosition(level, mound.blockPosition(), minDistance * 2, random);
        if (candidate.isEmpty()) {
            return null;
        }

        BlockPos pos = candidate.get();
        if (StructureFootprintData.anyOwnedWithin(level, mound.getUUID(), Kind.SURFACE, pos, minDistance)) {
            return null;
        }
        return new BlockPos(pos.getX(), OrganoidStructurePlacer.surfaceHeight(level, pos.getX(), pos.getZ()) - 2, pos.getZ());
    }

    private static void maybeStartUnderground(ServerLevel level, Mound mound, OrganoidStructureState state) {
        RandomSource random = mound.getRandom();
        if (random.nextDouble() >= SporeBreachServerConfig.MOUND_STRUCTURE_UNDERGROUND_CHANCE.get()) {
            return;
        }

        StructurePool pool = StructurePool.fromConfig(SporeBreachServerConfig.MOUND_STRUCTURE_UNDERGROUND_POOL.get());
        Optional<StructurePoolEntry> entry = pool.pickWeighted(random);
        if (entry.isEmpty()) {
            return;
        }

        BlockPos anchor = state.pendingUndergroundAnchor();
        StructureTemplate template = OrganoidStructurePlacer.resolveTemplate(level, entry.get().structureId());
        BlockPos origin = OrganoidStructurePlacer.jobOrigin(template, anchor, true);
        StructureGrowthJob job = OrganoidStructurePlacer.buildJobAtOrigin(template, origin, true);
        double minCoverage = SporeBreachServerConfig.STRUCTURE_UNDERGROUND_MIN_NATURAL_GROUND_COVERAGE.get();
        if (OrganoidStructurePlacer.naturalGroundCoverage(level, job) < minCoverage) {
            return;
        }

        Record record = StructureFootprintData.claim(
                level, mound.getUUID(), Kind.UNDERGROUND, entry.get().structureId(), anchor,
                OrganoidStructurePlacer.footprint(template, origin), true
        );
        state.setUndergroundJob(job, record);
    }
}
