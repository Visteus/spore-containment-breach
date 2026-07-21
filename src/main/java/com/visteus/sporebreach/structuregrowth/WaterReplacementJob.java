package com.visteus.sporebreach.structuregrowth;

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
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * Spreads {@code spore:crusted_bile} through the water around a structure the same way {@link
 * StructureGrowthJob} spreads structure blocks: seeded from the ring of water immediately outside
 * the structure's base footprint, then expanding outward one connected water block at a time each
 * pass so it reads as bile creeping into the water rather than an instant area-wide swap. Bounded
 * to the footprint inflated by a max radius so it can't drain an entire ocean. Runs alongside the
 * structure's own growth job rather than waiting for it to finish - goal #3.6.
 */
public final class WaterReplacementJob {

    private final BlockPos boundsMin;
    private final BlockPos boundsMax;
    private final Set<BlockPos> visited = new HashSet<>();
    private final List<BlockPos> frontier = new ArrayList<>();
    private final Set<BlockPos> queued = new HashSet<>();

    public WaterReplacementJob(ServerLevel level, StructureGrowthJob.Footprint footprint, int radius) {
        boundsMin = footprint.min().offset(-radius, -1, -radius);
        boundsMax = footprint.max().offset(radius, 2, radius);

        int minX = footprint.min().getX();
        int maxX = footprint.max().getX();
        int minZ = footprint.min().getZ();
        int maxZ = footprint.max().getZ();
        for (int y = boundsMin.getY(); y <= boundsMax.getY(); y++) {
            for (int x = minX - 1; x <= maxX + 1; x++) {
                seedIfWater(level, new BlockPos(x, y, minZ - 1));
                seedIfWater(level, new BlockPos(x, y, maxZ + 1));
            }
            for (int z = minZ; z <= maxZ; z++) {
                seedIfWater(level, new BlockPos(minX - 1, y, z));
                seedIfWater(level, new BlockPos(maxX + 1, y, z));
            }
        }
    }

    public boolean isComplete() {
        return frontier.isEmpty();
    }

    /** Converts up to {@code blockBudget} connected water blocks to crusted bile this pass. */
    public void advance(ServerLevel level, RandomSource random, int blockBudget) {
        BlockState bile = Sblocks.CRUSTED_BILE.get().defaultBlockState();
        int converted = 0;
        while (converted < blockBudget && !frontier.isEmpty()) {
            BlockPos pos = removeAt(random.nextInt(frontier.size()));
            queued.remove(pos);
            if (!visited.add(pos) || !isWaterSource(level, pos)) {
                continue;
            }

            level.setBlock(pos, bile, 3);
            converted++;

            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                if (isWithinBounds(neighbor) && !visited.contains(neighbor) && !queued.contains(neighbor)
                        && isWaterSource(level, neighbor)) {
                    enqueue(neighbor);
                }
            }
        }
    }

    private boolean isWithinBounds(BlockPos pos) {
        return pos.getX() >= boundsMin.getX() && pos.getX() <= boundsMax.getX()
                && pos.getY() >= boundsMin.getY() && pos.getY() <= boundsMax.getY()
                && pos.getZ() >= boundsMin.getZ() && pos.getZ() <= boundsMax.getZ();
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
