package com.visteus.sporebreach.biome;

import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

/**
 * Sphere-column intersection math for Goal #5's biome paint. The sphere is centered at the
 * organoid's frozen anchor - chunk-center X/Z ({@link ChunkPos#getMiddleBlockX()}/{@code Z}), exact
 * block Y - with a radius equal to {@code allowedRadiusChunks * 16} blocks, i.e. the same chunk
 * radius {@link com.visteus.sporebreach.chunkloading.ChunkCircleOffsets} already uses to pick which
 * columns are in play, just expressed in blocks so it also bounds height. {@link BiomeRepaint} can
 * only paint a column in contiguous Y-bands (one {@code FillBiomeCommand.fill} box per band), so a
 * true per-block sphere isn't achievable without an explosion of fill calls per column - instead
 * each column gets one vertical band sized to the sphere's actual cross-section at that column's
 * closest horizontal approach, producing a stepped/banded sphere at chunk-column granularity.
 */
public final class BiomePaintShape {

    private BiomePaintShape() {
    }

    /**
     * Returns {@code null} if {@code columnPos}'s footprint doesn't intersect the sphere at all,
     * otherwise the inclusive {@code [minY, maxY]} band of that column falling inside it, clamped to
     * the level's build height.
     */
    @Nullable
    public static int[] verticalRange(
            ChunkPos columnPos, ChunkPos anchorChunk, int anchorY, int radiusBlocks, int minBuildHeight, int maxBuildHeight
    ) {
        if (radiusBlocks <= 0) {
            return null;
        }

        int centerX = anchorChunk.getMiddleBlockX();
        int centerZ = anchorChunk.getMiddleBlockZ();
        int nearestX = Mth.clamp(centerX, columnPos.getMinBlockX(), columnPos.getMaxBlockX());
        int nearestZ = Mth.clamp(centerZ, columnPos.getMinBlockZ(), columnPos.getMaxBlockZ());
        long dx = nearestX - centerX;
        long dz = nearestZ - centerZ;
        long horizDistSq = dx * dx + dz * dz;
        long radiusSq = (long) radiusBlocks * radiusBlocks;
        if (horizDistSq >= radiusSq) {
            return null;
        }

        int halfExtent = (int) Math.ceil(Math.sqrt(radiusSq - horizDistSq));
        int minY = Math.max(minBuildHeight, anchorY - halfExtent);
        int maxY = Math.min(maxBuildHeight - 1, anchorY + halfExtent);
        if (minY > maxY) {
            return null;
        }
        return new int[]{minY, maxY};
    }
}
