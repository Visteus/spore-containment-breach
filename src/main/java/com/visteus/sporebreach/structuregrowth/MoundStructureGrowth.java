package com.visteus.sporebreach.structuregrowth;

import com.Harbinger.Spore.Sentities.Organoids.Mound;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import com.visteus.sporebreach.spawning.SpawnAnchors;
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
 * Grows the goal #3 "small fortress" of structures around a single Mound: the first structure is
 * always centered on the Mound itself (terrain-anchored, collision-pushed - see
 * {@link StructureGrowthJob}); the 2nd+ keep a random offset spread away from earlier anchors.
 * Each surface structure may optionally grow an underground companion beneath it once complete.
 */
public final class MoundStructureGrowth {

    private static final Map<UUID, OrganoidStructureState> STATE = new HashMap<>();

    private MoundStructureGrowth() {
    }

    /** Called on the recheck cadence: decide whether to start a new job for this Mound. */
    public static void tryStartJob(ServerLevel level, Mound mound) {
        if (mound.getAge() < SporeBreachServerConfig.MOUND_STRUCTURE_MIN_AGE.get()) {
            return;
        }

        OrganoidStructureState state = STATE.computeIfAbsent(mound.getUUID(), id -> new OrganoidStructureState());
        if (state.hasActiveJob()) {
            return;
        }
        if (state.structuresStarted() >= SporeBreachServerConfig.MOUND_STRUCTURE_MAX_PER_MOUND.get()) {
            return;
        }

        RandomSource random = mound.getRandom();
        if (random.nextDouble() >= SporeBreachServerConfig.MOUND_STRUCTURE_PLACEMENT_CHANCE.get()) {
            return;
        }

        StructurePool pool = StructurePool.fromConfig(SporeBreachServerConfig.MOUND_STRUCTURE_POOL.get());
        Optional<StructurePoolEntry> entry = pool.pickWeighted(random);
        if (entry.isEmpty()) {
            return;
        }

        BlockPos anchor = resolveAnchor(level, mound, state, random);
        if (anchor == null) {
            return;
        }

        StructureTemplate template = OrganoidStructurePlacer.resolveTemplate(level, entry.get().structureId());
        StructureGrowthJob job = buildJob(template, anchor, false);
        state.setSurfaceJob(job);
        state.setPendingUndergroundAnchor(anchor);
        state.recordAnchor(anchor);
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
                if (SporeBreachServerConfig.STRUCTURE_WATER_REPLACEMENT_ENABLED.get()) {
                    OrganoidStructurePlacer.replaceNearbyWaterWithBile(
                            level, state.surfaceJob().baseFootprint(), SporeBreachServerConfig.STRUCTURE_WATER_REPLACEMENT_RADIUS.get());
                }
                state.setSurfaceJob(null);
                maybeStartUnderground(level, mound, state);
            }
        } else if (state.undergroundJob() != null) {
            state.undergroundJob().advance(level, mound, random, blocksPerPass);
            if (state.undergroundJob().isComplete()) {
                state.setUndergroundJob(null);
            }
        }
    }

    private static BlockPos resolveAnchor(ServerLevel level, Mound mound, OrganoidStructureState state, RandomSource random) {
        if (state.structuresStarted() == 0) {
            return new BlockPos(mound.getBlockX(), mound.getBlockY() - 2, mound.getBlockZ());
        }

        int minDistance = SporeBreachServerConfig.MOUND_STRUCTURE_MIN_DISTANCE.get();
        Optional<BlockPos> candidate = SpawnAnchors.findGroundPosition(level, mound.blockPosition(), minDistance * 2, random);
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

    private static void maybeStartUnderground(ServerLevel level, Mound mound, OrganoidStructureState state) {
        BlockPos anchor = state.pendingUndergroundAnchor();
        if (!OrganoidStructurePlacer.isNaturalGround(level, anchor.below())) {
            return;
        }

        RandomSource random = mound.getRandom();
        if (random.nextDouble() >= SporeBreachServerConfig.MOUND_STRUCTURE_UNDERGROUND_CHANCE.get()) {
            return;
        }

        StructurePool pool = StructurePool.fromConfig(SporeBreachServerConfig.MOUND_STRUCTURE_UNDERGROUND_POOL.get());
        Optional<StructurePoolEntry> entry = pool.pickWeighted(random);
        if (entry.isEmpty()) {
            return;
        }

        StructureTemplate template = OrganoidStructurePlacer.resolveTemplate(level, entry.get().structureId());
        StructureGrowthJob job = buildJob(template, state.pendingUndergroundAnchor(), true);
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
