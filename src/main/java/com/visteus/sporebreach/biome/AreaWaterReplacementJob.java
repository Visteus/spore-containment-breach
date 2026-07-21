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
 * under Goal #5 corruption biome territory. This is now the mod's only water-crusting system - it
 * replaced the old per-structure {@code WaterReplacementJob} (goal #3.6), which shared the same
 * BFS/frontier/budget shape: random pop from the frontier each pass, for the "reads as bile
 * creeping into the water" organic texture, rather than a uniform ring-by-ring sweep. Differs from
 * that retired job in two ways: seeded from a single water tile nearest an organoid's anchor (see
 * {@link #seedWithinChunk}) rather than a structure footprint ring, and bounded to a configurable
 * depth measured fresh off each block's own column heightmap at conversion time (not a fixed footprint
 * box) - so no bounds tracking is needed alongside the frontier itself. Seeding just the one closest
 * tile and letting connectivity carry the rest is deliberate: it's what makes the spread visibly
 * originate at the organoid rather than appearing simultaneously in every pond scattered across its
 * territory, and a pond that isn't water-connected to anything nearer the anchor simply never gets
 * reached, which reads as the bile spreading through the water table rather than teleporting into it.
 * A separate periodic reseed (see {@code BiomePaintManager#tryReseedWater}) reuses {@link
 * #seedWithinChunk} on a random chunk within an owner's territory every so often, specifically to
 * eventually catch these disconnected pools too.
 *
 * <p>Every pop re-checks {@link BiomePaintData}'s current stage for that block's column, not just
 * at seed time - a block seeded while its column was still {@code ACTIVE} can sit in the frontier
 * for a while before its turn comes up, and by then the organoid that claimed it may have died and
 * started (or finished) its scar downgrade. Without this check the bile would keep crusting - and
 * keep spreading into fresh water - through a decaying or already-{@code dead_scar} area even
 * though nothing is claiming it anymore.
 */
public final class AreaWaterReplacementJob {

    private final List<BlockPos> frontier = new ArrayList<>();
    private final Set<BlockPos> queued = new HashSet<>();
    private final Set<BlockPos> visited = new HashSet<>();

    public boolean isEmpty() {
        return frontier.isEmpty();
    }

    /**
     * Seeds the single water-topped block within {@code chunkPos} closest to its center, if any
     * exists - intentionally not the whole column, so a chunk with no water nearby simply never
     * starts a bile spread rather than one appearing at an arbitrary corner of it. Used both for an
     * organoid's initial near-anchor seed and for {@code BiomePaintManager}'s periodic random reseed.
     */
    public void seedWithinChunk(ServerLevel level, ChunkPos chunkPos) {
        int centerX = chunkPos.getMinBlockX() + 8;
        int centerZ = chunkPos.getMinBlockZ() + 8;

        BlockPos nearest = null;
        long nearestDistSq = Long.MAX_VALUE;
        for (int x = chunkPos.getMinBlockX(); x <= chunkPos.getMaxBlockX(); x++) {
            for (int z = chunkPos.getMinBlockZ(); z <= chunkPos.getMaxBlockZ(); z++) {
                int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                BlockPos candidate = new BlockPos(x, surfaceY, z);
                if (!isWaterSource(level, candidate)) {
                    continue;
                }
                long dx = x - centerX;
                long dz = z - centerZ;
                long distSq = dx * dx + dz * dz;
                if (distSq < nearestDistSq) {
                    nearestDistSq = distSq;
                    nearest = candidate;
                }
            }
        }

        if (nearest != null) {
            enqueue(nearest.immutable());
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
            if (!isColumnActive(level, pos)) {
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

    private static boolean isWaterSource(ServerLevel level, BlockPos pos) {
        FluidState fluid = level.getFluidState(pos);
        return fluid.isSource() && fluid.is(Fluids.WATER);
    }

    private static boolean isColumnActive(ServerLevel level, BlockPos pos) {
        BiomePaintData.ColumnState state = BiomePaintData.getState(level, new ChunkPos(pos));
        return state != null && state.stage() == BiomePaintData.BiomeStage.ACTIVE;
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
