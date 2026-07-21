package com.visteus.sporebreach.structuregrowth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;

/**
 * One in-progress structure placement, grown a few blocks at a time instead of dropped in with a
 * single {@code placeInWorld} call. Seeded from the structure's base (or, for a downward-growing
 * underground piece, its top) layer, then expands along block adjacency - a block only ever
 * places once something already-placed touches it, which is what makes growth read as organic and
 * terrain-following rather than a blind geometric slice. Each pass draws its blocks at *random*
 * from the current frontier rather than in strict insertion order, so some columns can chain
 * several picks in one pass while their neighbors sit untouched - this is what gives growth an
 * uneven, spire-like character instead of a uniform rising slab. See the Goal #3 plan.
 */
public final class StructureGrowthJob {

    private final Map<BlockPos, StructureTemplate.StructureBlockInfo> byPos = new HashMap<>();
    private final Set<BlockPos> placed = new HashSet<>();
    private final List<BlockPos> frontier = new ArrayList<>();
    private final Set<BlockPos> queued = new HashSet<>();
    private final int baseY;

    public StructureGrowthJob(List<StructureTemplate.StructureBlockInfo> worldBlocks, boolean growDownward) {
        for (StructureTemplate.StructureBlockInfo info : worldBlocks) {
            byPos.put(info.pos(), info);
        }

        baseY = growDownward
                ? worldBlocks.stream().mapToInt(info -> info.pos().getY()).max().orElse(0)
                : worldBlocks.stream().mapToInt(info -> info.pos().getY()).min().orElse(0);
        for (StructureTemplate.StructureBlockInfo info : worldBlocks) {
            if (info.pos().getY() == baseY) {
                enqueue(info.pos());
            }
        }
    }

    public boolean isComplete() {
        return frontier.isEmpty();
    }

    /** Every position this job will eventually place a block at, regardless of progress so far. */
    public Set<BlockPos> targetPositions() {
        return byPos.keySet();
    }

    /**
     * Places up to {@code blockBudget} blocks, each drawn at random from the current frontier. A
     * block is placed regardless of whether it overlaps {@code organoid}'s bounding box; only if
     * that placement would leave the organoid with no collision-free room at all (i.e. it would
     * suffocate) is it moved, up onto the block just placed. This is what makes solid structures
     * push the organoid upward as they rise around it, while structures with real interior voids -
     * or a solid structure that still leaves the organoid room to stand - simply never displace it.
     */
    public void advance(ServerLevel level, Mob organoid, RandomSource random, int blockBudget) {
        int placedThisPass = 0;
        while (placedThisPass < blockBudget && !frontier.isEmpty()) {
            int index = random.nextInt(frontier.size());
            BlockPos pos = removeAt(index);
            queued.remove(pos);
            if (placed.contains(pos)) {
                continue;
            }
            StructureTemplate.StructureBlockInfo info = byPos.get(pos);
            if (info == null) {
                continue;
            }

            boolean mightSuffocate = organoid != null && organoid.isAlive() && organoid.getBoundingBox().intersects(new AABB(pos));

            level.setBlock(pos, info.state(), 3);
            placed.add(pos);
            placedThisPass++;

            if (mightSuffocate) {
                rescueFromSuffocation(level, organoid, pos);
            }

            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                if (byPos.containsKey(neighbor) && !placed.contains(neighbor) && !queued.contains(neighbor)) {
                    enqueue(neighbor);
                }
            }
        }
    }

    /**
     * Moves {@code organoid} straight up from {@code placedPos} until it lands somewhere
     * genuinely collision-free, re-checking after every step rather than trusting a single blind
     * bump. A structure already several blocks deep can have solid material waiting immediately
     * above {@code placedPos} too (placed earlier this same pass, or an earlier pass) - moving
     * only one block up without re-verifying can relocate the organoid straight into that
     * material instead of out of it, leaving it just as stuck (and still taking suffocation
     * damage) as before the "rescue".
     */
    private static void rescueFromSuffocation(ServerLevel level, Mob organoid, BlockPos placedPos) {
        if (level.noCollision(organoid)) {
            return;
        }
        double x = placedPos.getX() + 0.5;
        double z = placedPos.getZ() + 0.5;
        for (int y = placedPos.getY() + 1; y <= level.getMaxBuildHeight(); y++) {
            organoid.moveTo(x, y, z, organoid.getYRot(), organoid.getXRot());
            if (level.noCollision(organoid)) {
                return;
            }
        }
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
