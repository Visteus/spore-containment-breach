package com.visteus.sporebreach.structuregrowth;

import com.Harbinger.Spore.Sentities.Organoids.Proto;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import com.visteus.sporebreach.spawning.SpawnAnchors;
import com.visteus.sporebreach.tracking.ProtoAgeTracker;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Grows the goal #3 spire field around a single Proto-Hivemind: the first structure is always
 * centered on the Proto itself (terrain-anchored, collision-pushed - see
 * {@link StructureGrowthJob}); the 2nd+ spires keep a random offset spread away from earlier
 * anchors, drawn from the same weighted pool. Passes are gated on {@link Proto#getBiomass()}, the
 * same resource base Spore's own {@code CasingGenerator} shell growth spends - see the Goal #3
 * plan.
 */
public final class ProtoStructureGrowth {

    private static final Map<UUID, OrganoidStructureState> STATE = new HashMap<>();

    private ProtoStructureGrowth() {
    }

    /** Called on the recheck cadence: decide whether to start a new job for this Proto-Hivemind. */
    public static void tryStartJob(ServerLevel level, Proto proto) {
        long gameTime = level.getGameTime();
        ProtoAgeTracker.markCreatedIfAbsent(proto, gameTime);
        int age = ProtoAgeTracker.getAge(proto, gameTime);
        if (age < SporeBreachServerConfig.PROTO_STRUCTURE_MIN_AGE.get()) {
            return;
        }

        OrganoidStructureState state = STATE.computeIfAbsent(proto.getUUID(), id -> new OrganoidStructureState());
        if (state.hasActiveJob()) {
            return;
        }
        if (state.structuresStarted() >= SporeBreachServerConfig.PROTO_STRUCTURE_MAX_PER_PROTO.get()) {
            return;
        }

        RandomSource random = proto.getRandom();
        boolean isFirst = state.structuresStarted() == 0;
        if (!isFirst && random.nextDouble() >= SporeBreachServerConfig.PROTO_STRUCTURE_PLACEMENT_CHANCE.get()) {
            return;
        }

        Optional<StructurePoolEntry> picked =
                StructurePool.fromConfig(SporeBreachServerConfig.PROTO_STRUCTURE_POOL.get()).pickWeighted(random);
        if (picked.isEmpty()) {
            return;
        }

        BlockPos anchor = resolveAnchor(level, proto, state, random);
        if (anchor == null) {
            return;
        }

        StructureTemplate template = OrganoidStructurePlacer.resolveTemplate(level, picked.get().structureId());
        StructureGrowthJob job = buildJob(template, anchor, false);
        state.setSurfaceJob(job);
        state.setPendingUndergroundAnchor(anchor);
        state.setPendingUndergroundGuaranteed(isFirst);
        state.recordAnchor(anchor);
    }

    /** Called on the pass cadence: advance whichever job is currently running for this Proto-Hivemind. */
    public static void tryAdvanceJob(ServerLevel level, Proto proto) {
        OrganoidStructureState state = STATE.get(proto.getUUID());
        if (state == null) {
            return;
        }

        int costPerPass = SporeBreachServerConfig.PROTO_STRUCTURE_BIOMASS_COST_PER_PASS.get();
        if (proto.getBiomass() < costPerPass) {
            return;
        }
        int blocksPerPass = SporeBreachServerConfig.PROTO_STRUCTURE_BLOCKS_PER_PASS.get();
        RandomSource random = proto.getRandom();

        if (state.surfaceJob() != null) {
            proto.eatBiomass(costPerPass);
            state.surfaceJob().advance(level, proto, random, blocksPerPass);
            if (state.surfaceJob().isComplete()) {
                if (SporeBreachServerConfig.STRUCTURE_WATER_REPLACEMENT_ENABLED.get()) {
                    OrganoidStructurePlacer.replaceNearbyWaterWithBile(
                            level, state.surfaceJob().baseFootprint(), SporeBreachServerConfig.STRUCTURE_WATER_REPLACEMENT_RADIUS.get());
                }
                state.setSurfaceJob(null);
                maybeStartUnderground(level, proto, state);
            }
        } else if (state.undergroundJob() != null) {
            proto.eatBiomass(costPerPass);
            state.undergroundJob().advance(level, proto, random, blocksPerPass);
            if (state.undergroundJob().isComplete()) {
                state.setUndergroundJob(null);
            }
        }
    }

    private static BlockPos resolveAnchor(ServerLevel level, Proto proto, OrganoidStructureState state, RandomSource random) {
        if (state.structuresStarted() == 0) {
            return new BlockPos(proto.getBlockX(), proto.getBlockY() - 2, proto.getBlockZ());
        }

        int minDistance = SporeBreachServerConfig.PROTO_STRUCTURE_MIN_DISTANCE.get();
        Optional<BlockPos> candidate = SpawnAnchors.findGroundPosition(level, proto.blockPosition(), minDistance * 2, random);
        if (candidate.isEmpty()) {
            return null;
        }

        BlockPos pos = candidate.get();
        double minDistanceSq = (double) minDistance * minDistance;
        for (BlockPos existing : state.anchors()) {
            if (existing.distSqr(pos) < minDistanceSq) {
                return null;
            }
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

        StructureTemplate template = OrganoidStructurePlacer.resolveTemplate(level, entry.get().structureId());
        StructureGrowthJob job = buildJob(template, state.pendingUndergroundAnchor(), true);
        double minCoverage = SporeBreachServerConfig.STRUCTURE_UNDERGROUND_MIN_NATURAL_GROUND_COVERAGE.get();
        if (OrganoidStructurePlacer.naturalGroundCoverage(level, job) < minCoverage) {
            return;
        }
        state.setUndergroundJob(job);
    }

    private static StructureGrowthJob buildJob(StructureTemplate template, BlockPos anchor, boolean growDownward) {
        Vec3i size = template.getSize();
        BlockPos origin = growDownward
                ? new BlockPos(anchor.getX() - size.getX() / 2, anchor.getY() - size.getY(), anchor.getZ() - size.getZ() / 2)
                : new BlockPos(anchor.getX() - size.getX() / 2, anchor.getY(), anchor.getZ() - size.getZ() / 2);
        StructurePlaceSettings settings = new StructurePlaceSettings();
        return new StructureGrowthJob(OrganoidStructurePlacer.worldBlocks(template, origin, settings), growDownward);
    }
}
