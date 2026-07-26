package com.visteus.sporebreach.structuregrowth;

import com.Harbinger.Spore.Sentities.Organoids.Proto;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import com.visteus.sporebreach.spawning.SpawnAnchors;
import com.visteus.sporebreach.structuregrowth.StructureFootprintData.Kind;
import com.visteus.sporebreach.structuregrowth.StructureFootprintData.Record;
import com.visteus.sporebreach.tracking.ProtoAgeTracker;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Grows the goal #3 spire field around a single Proto-Hivemind: the first structure is always
 * centered on the Proto itself (terrain-anchored, collision-pushed - see
 * {@link StructureGrowthJob}); the 2nd+ spires keep a random offset spread away from earlier
 * anchors, drawn from the same weighted pool. Passes are gated on {@link Proto#getBiomass()}, the
 * same resource base Spore's own {@code CasingGenerator} shell growth spends - see the Goal #3
 * plan. What's already been built is tracked in {@link StructureFootprintData}, so both the
 * per-Proto cap and an unfinished spire survive an unload.
 */
public final class ProtoStructureGrowth {

    private static final Map<UUID, OrganoidStructureState> STATE = new HashMap<>();

    private ProtoStructureGrowth() {
    }

    /** Called on the recheck cadence: decide whether to start a new job for this Proto-Hivemind. */
    public static void tryStartJob(ServerLevel level, Proto proto) {
        OrganoidStructureState state = STATE.computeIfAbsent(proto.getUUID(), id -> new OrganoidStructureState());
        if (state.hasActiveJob()) {
            return;
        }
        if (resumeUnfinishedJob(level, proto, state)) {
            return;
        }

        long gameTime = level.getGameTime();
        ProtoAgeTracker.markCreatedIfAbsent(proto, gameTime);
        int age = ProtoAgeTracker.getAge(proto, gameTime);
        if (age < SporeBreachServerConfig.PROTO_STRUCTURE_MIN_AGE.get()) {
            return;
        }

        int built = StructureFootprintData.countOwned(level, proto.getUUID(), Kind.SURFACE);
        if (built >= SporeBreachServerConfig.PROTO_STRUCTURE_MAX_PER_PROTO.get()) {
            return;
        }

        RandomSource random = proto.getRandom();
        boolean isFirst = built == 0;
        if (!isFirst && random.nextDouble() >= SporeBreachServerConfig.GROWTH_PLACEMENT_CHANCE.get()) {
            return;
        }

        Optional<StructurePoolEntry> picked =
                StructurePool.fromConfig(SporeBreachServerConfig.PROTO_STRUCTURE_POOL.get()).pickWeighted(random);
        if (picked.isEmpty()) {
            return;
        }

        BlockPos anchor = resolveAnchor(level, proto, isFirst, random);
        if (anchor == null) {
            return;
        }

        StructureTemplate template = OrganoidStructurePlacer.resolveTemplate(level, picked.get().structureId());
        BlockPos origin = OrganoidStructurePlacer.jobOrigin(template, anchor, false);
        Record record = StructureFootprintData.claim(
                level, proto.getUUID(), Kind.SURFACE, picked.get().structureId(), anchor,
                OrganoidStructurePlacer.footprint(template, origin), false
        );
        state.setSurfaceJob(OrganoidStructurePlacer.buildJobAtOrigin(template, origin, false), record);
        state.setPendingUndergroundAnchor(anchor);
        state.setPendingUndergroundGuaranteed(isFirst);
    }

    /** Called on the pass cadence: advance whichever job is currently running for this Proto-Hivemind. */
    public static void tryAdvanceJob(ServerLevel level, Proto proto) {
        OrganoidStructureState state = STATE.get(proto.getUUID());
        if (state == null) {
            return;
        }

        int costPerPass = SporeBreachServerConfig.PROTO_STRUCTURE_BIOMASS_COST_PER_PASS.get();
        int blocksPerPass = SporeBreachServerConfig.PROTO_STRUCTURE_BLOCKS_PER_PASS.get();
        RandomSource random = proto.getRandom();

        if (proto.getBiomass() >= costPerPass) {
            if (state.surfaceJob() != null) {
                proto.eatBiomass(costPerPass);
                state.surfaceJob().advance(level, proto, random, blocksPerPass);
                if (state.surfaceJob().isComplete()) {
                    StructureFootprintData.markComplete(state.surfaceRecord());
                    state.clearSurfaceJob();
                    maybeStartUnderground(level, proto, state);
                }
            } else if (state.undergroundJob() != null) {
                proto.eatBiomass(costPerPass);
                state.undergroundJob().advance(level, proto, random, blocksPerPass);
                if (state.undergroundJob().isComplete()) {
                    StructureFootprintData.markComplete(state.undergroundRecord());
                    state.clearUndergroundJob();
                }
            }
        }
    }

    /**
     * Rebuilds a job left unfinished by an unload. Growth re-walks the whole template rather than
     * resuming at an exact block count - already-placed material is itself replaceable, so those
     * positions are simply re-set to the state they already hold.
     */
    private static boolean resumeUnfinishedJob(ServerLevel level, Proto proto, OrganoidStructureState state) {
        UUID id = proto.getUUID();
        Optional<Record> surface = StructureFootprintData.incompleteFor(level, id, Kind.SURFACE);
        if (surface.isPresent()) {
            Record record = surface.get();
            StructureTemplate template = OrganoidStructurePlacer.resolveTemplate(level, record.structureId());
            state.setSurfaceJob(
                    OrganoidStructurePlacer.buildJobAtOrigin(template, record.origin(), record.growDownward()), record
            );
            state.setPendingUndergroundAnchor(record.anchor());
            state.setPendingUndergroundGuaranteed(StructureFootprintData.countOwned(level, id, Kind.SURFACE) == 1);
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

    private static BlockPos resolveAnchor(ServerLevel level, Proto proto, boolean isFirst, RandomSource random) {
        if (isFirst) {
            return new BlockPos(proto.getBlockX(), proto.getBlockY() - 2, proto.getBlockZ());
        }

        int minDistance = SporeBreachServerConfig.PROTO_STRUCTURE_MIN_DISTANCE.get();
        Optional<BlockPos> candidate = SpawnAnchors.findGroundPosition(level, proto.blockPosition(), minDistance * 2, random);
        if (candidate.isEmpty()) {
            return null;
        }

        BlockPos pos = candidate.get();
        if (StructureFootprintData.anyOwnedWithin(level, proto.getUUID(), Kind.SURFACE, pos, minDistance)) {
            return null;
        }
        return new BlockPos(pos.getX(), OrganoidStructurePlacer.surfaceHeight(level, pos.getX(), pos.getZ()) - 2, pos.getZ());
    }

    private static void maybeStartUnderground(ServerLevel level, Proto proto, OrganoidStructureState state) {
        RandomSource random = proto.getRandom();
        if (!state.pendingUndergroundGuaranteed()
                && random.nextDouble() >= SporeBreachServerConfig.PROTO_STRUCTURE_UNDERGROUND_CHANCE.get()) {
            return;
        }

        Optional<StructurePoolEntry> entry =
                StructurePool.fromConfig(SporeBreachServerConfig.PROTO_STRUCTURE_UNDERGROUND_POOL.get()).pickWeighted(random);
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
                level, proto.getUUID(), Kind.UNDERGROUND, entry.get().structureId(), anchor,
                OrganoidStructurePlacer.footprint(template, origin), true
        );
        state.setUndergroundJob(job, record);
    }
}
