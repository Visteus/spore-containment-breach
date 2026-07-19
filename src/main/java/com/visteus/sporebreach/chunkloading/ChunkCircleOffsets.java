package com.visteus.sporebreach.chunkloading;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Circular ("pixel circle") chunk-offset sets for Goal #4's chunkloading radii, eagerly
 * precomputed for r=0..{@link #MAX_RADIUS} at class-init so growth steps never need to compute
 * or cache offsets on demand. A chunk offset (dx, dz) belongs to radius r iff
 * {@code 4 * (dx*dx + dz*dz) <= (2r + 1) * (2r + 1)} - the integer form of "distance from center
 * <= r + 0.5". r=1 yields the full 3x3 (9 offsets, no corners cut); r=3 yields 37, matching
 * CLAUDE.md's Goal #4 figures.
 */
public final class ChunkCircleOffsets {

    /**
     * Hard ceiling on any configured radius (min/max/ticking) - see {@link ChunkloadEntry#parse}.
     * Keeps this cache fixed-size rather than lazily unbounded.
     */
    public static final int MAX_RADIUS = 10;

    private static final Set<ChunkOffset> EMPTY = Set.of();

    private static final Set<ChunkOffset>[] CACHE = buildCache();

    private ChunkCircleOffsets() {
    }

    /**
     * r=0 is the "nothing issued yet" sentinel and always returns an empty set, even though the
     * center offset (0,0) would otherwise satisfy the inclusion test.
     */
    public static Set<ChunkOffset> fullOffsets(int radius) {
        if (radius <= 0) {
            return EMPTY;
        }
        if (radius > MAX_RADIUS) {
            radius = MAX_RADIUS;
        }
        return CACHE[radius];
    }

    @SuppressWarnings("unchecked")
    private static Set<ChunkOffset>[] buildCache() {
        Set<ChunkOffset>[] cache = new Set[MAX_RADIUS + 1];
        cache[0] = EMPTY;
        for (int r = 1; r <= MAX_RADIUS; r++) {
            Set<ChunkOffset> offsets = new LinkedHashSet<>();
            long limit = (long) (2 * r + 1) * (2 * r + 1);
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (4L * (dx * dx + dz * dz) <= limit) {
                        offsets.add(new ChunkOffset(dx, dz));
                    }
                }
            }
            cache[r] = Set.copyOf(offsets);
        }
        return cache;
    }

    public record ChunkOffset(int dx, int dz) {
    }
}
