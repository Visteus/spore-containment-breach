package com.visteus.sporebreach.structuregrowth;

import com.Harbinger.Spore.core.SblockEntities;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * "Is there already an Outpost Watcher near here?", answered without any registry of our own.
 *
 * <p>Reading the live world matters because this mod is not the only source of watchers: base
 * Spore's Reconstructed Mind tower has one, and so does {@code sporebreach:proto_spire_hollow}, and
 * neither goes through {@link StructureFootprintData}. The persisted records are still consulted
 * on top of that, to cover a tower that has been claimed but hasn't grown far enough to place its
 * watcher block yet.
 */
public final class OutpostWatcherIndex {

    private OutpostWatcherIndex() {
    }

    public static boolean hasWatcherWithin(ServerLevel level, BlockPos center, int radius) {
        if (radius <= 0) {
            return false;
        }
        return StructureFootprintData.anyWithin(level, StructureFootprintData.Kind.OUTPOST, center, radius)
                || hasPlacedWatcherWithin(level, center, radius);
    }

    /**
     * Scans only chunks that are already loaded - {@code getChunkNow} returns null rather than
     * generating one, and a candidate site always sits inside its own organoid's forced-load
     * radius, so nothing relevant is missed while a stray query can never trigger worldgen.
     */
    private static boolean hasPlacedWatcherWithin(ServerLevel level, BlockPos center, int radius) {
        double radiusSq = (double) radius * radius;
        int minChunkX = SectionPos.blockToSectionCoord(center.getX() - radius);
        int maxChunkX = SectionPos.blockToSectionCoord(center.getX() + radius);
        int minChunkZ = SectionPos.blockToSectionCoord(center.getZ() - radius);
        int maxChunkZ = SectionPos.blockToSectionCoord(center.getZ() + radius);

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                    if (entry.getValue().getType() == SblockEntities.OUTPOST_WATCHER.get()
                            && entry.getKey().distSqr(center) < radiusSq) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
