package com.visteus.sporebreach.biome;

import com.Harbinger.Spore.core.Sblocks;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.ChunkPos;

/**
 * Per-dimension shared frontier that gradually crusts surface water into {@code spore:crusted_bile}
 * under newly painted Goal #5 corruption biome columns, copying {@link
 * com.visteus.sporebreach.structuregrowth.WaterReplacementJob}'s BFS/frontier/budget shape.
 * Differs from that job in two ways: seeded per painted 16x16 column from its {@code WORLD_SURFACE}
 * heightmap rather than a structure footprint ring, and bounded to a configurable depth measured
 * fresh off each block's own column heightmap at conversion time (not a fixed footprint box) - so
 * no bounds tracking is needed alongside the frontier itself. One instance is shared across every
 * column painted in a pass (see {@code BiomePaintManager}), so seeding many columns in one pass
 * doesn't multiply the per-pass block budget the way one job per column would.
 */
public final class AreaWaterReplacementJob {

    private final List<BlockPos> frontier = new ArrayList<>();
    private final Set<BlockPos> queued = new HashSet<>();
    private final Set<BlockPos> visited = new HashSet<>();

    public boolean isEmpty() {
        return frontier.isEmpty();
    }

    /** Seeds every water-topped column within {@code chunkPos} at its current surface height. */
    public void seedColumn(ServerLevel level, ChunkPos chunkPos) {
        int minX = chunkPos.getMinBlockX();
        int maxX = chunkPos.getMaxBlockX();
        int minZ = chunkPos.getMinBlockZ();
        int maxZ = chunkPos.getMaxBlockZ();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                seedIfWater(level, new BlockPos(x, surfaceY, z));
            }
        }
    }

    /** Converts up to {@code blockBudget} connected, in-depth water blocks to crusted bile this pass. */
    public void advance(ServerLevel level, RandomSource random, int blockBudget, int depth) {
        BlockState bile = Sblocks.CRUSTED_BILE.get().defaultBlockState();
        int converted = 0;
        while (converted < blockBudget && !frontier.isEmpty()) {
            BlockPos pos = removeAt(random.nextInt(frontier.size()));
            queued.remove(pos);
            if (!visited.add(pos) || !isWaterSource(level, pos)) {
                continue;
            }

            int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ()) - 1;
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                if (!visited.contains(neighbor) && !queued.contains(neighbor) && isWaterSource(level, neighbor)) {
                    enqueue(neighbor);
                }
            }

            if (pos.getY() < surfaceY - depth + 1) {
                continue;
            }

            level.setBlock(pos, bile, 3);
            converted++;
        }
    }

    private void seedIfWater(ServerLevel level, BlockPos pos) {
        BlockPos immutable = pos.immutable();
        if (isWaterSource(level, immutable)) {
            enqueue(immutable);
        }
    }

    private static boolean isWaterSource(ServerLevel level, BlockPos pos) {
        FluidState fluid = level.getFluidState(pos);
        return fluid.isSource() && fluid.is(Fluids.WATER);
    }

    private BlockPos removeAt(int index) {
        int lastIndex = frontier.size() - 1;
        BlockPos value = frontier.get(index);
        BlockPos last = frontier.remove(lastIndex);
        if (index != lastIndex) {
            frontier.set(index, last);
        }
        return value;
    }

    private void enqueue(BlockPos pos) {
        frontier.add(pos);
        queued.add(pos);
    }
}
