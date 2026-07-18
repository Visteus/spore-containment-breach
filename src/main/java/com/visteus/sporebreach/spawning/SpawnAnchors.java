package com.visteus.sporebreach.spawning;

import com.Harbinger.Spore.SBlockEntities.LivingStructureBlocks;
import com.Harbinger.Spore.Sentities.BaseEntities.Organoid;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

/**
 * Resolves the point spawn/raid logic should anchor around, and enforces the "no spore
 * structures/spawns near world spawn" protected radius from Goal #8. See "Why
 * structureAnchorSearchRadius exists" in the Goal #1 plan for the rationale behind preferring a
 * LivingStructureBlocks anchor over the organoid's bare position.
 */
public final class SpawnAnchors {

    private SpawnAnchors() {
    }

    public static BlockPos resolveAnchor(Organoid organoid) {
        BlockPos own = organoid.getOnPos();
        int radius = SporeBreachServerConfig.STRUCTURE_ANCHOR_SEARCH_RADIUS.get();
        if (radius <= 0) {
            return own;
        }

        AABB area = organoid.getBoundingBox().inflate(radius);
        for (BlockPos pos : BlockPos.betweenClosed(
                Mth.floor(area.minX), Mth.floor(area.minY), Mth.floor(area.minZ),
                Mth.floor(area.maxX), Mth.floor(area.maxY), Mth.floor(area.maxZ)
        )) {
            BlockEntity blockEntity = organoid.level().getBlockEntity(pos);
            if (blockEntity instanceof LivingStructureBlocks) {
                return pos.immutable();
            }
        }

        return own;
    }

    public static boolean isWithinProtectedSpawnRadius(ServerLevel level, BlockPos pos) {
        int protectedRadiusChunks = SporeBreachServerConfig.PROTECTED_SPAWN_RADIUS_CHUNKS.get();
        ChunkPos target = new ChunkPos(pos);
        ChunkPos spawn = new ChunkPos(level.getSharedSpawnPos());
        int chunkDistance = Math.max(Math.abs(target.x - spawn.x), Math.abs(target.z - spawn.z));
        return chunkDistance <= protectedRadiusChunks;
    }

    /**
     * Randomized search for a standable position near {@code center} - not a raster scan,
     * since the search radii involved (moundDefenderSearchRadius, protoRaidSearchRadius) are far
     * too large for that to be cheap. Shared by MoundDefenseSpawner and ProtoRaidDirector.
     */
    public static Optional<BlockPos> findGroundPosition(Level level, BlockPos center, int radius, RandomSource random) {
        if (radius <= 0) {
            return isValidGroundSpawn(level, center) ? Optional.of(center) : Optional.empty();
        }

        for (int attempt = 0; attempt < 20; attempt++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dy = random.nextInt(13) - 6;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            BlockPos candidate = center.offset(dx, dy, dz);
            if (isValidGroundSpawn(level, candidate)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private static boolean isValidGroundSpawn(Level level, BlockPos pos) {
        return level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir()
                && level.getBlockState(pos.below()).isSolidRender(level, pos.below());
    }
}
