package com.visteus.sporebreach.util;

/**
 * Y-level bias for the mod's overdue-first structure/watcher sweeps (StructureGrowthDirector,
 * OutpostWatcherDirector): shifts an organoid's sort key by a fixed amount per block of Y-level,
 * pivoted at Y=0 - organoids above it look more overdue (built sooner), organoids below it look
 * less overdue (built later) - so structures higher up tend to get built before ones lower down
 * when a sweep's block budget can't cover every due organoid. Deliberately not anchored to sea
 * level or a terrain heightmap: both are Overworld-shaped assumptions that don't hold for every
 * dimension, while an organoid's own Y is always meaningful. The shift is a fixed amount per
 * organoid rather than unbounded, so the sweeps' existing anti-starvation property still holds -
 * real overdueness (now - dueAt) keeps growing every sweep an organoid is skipped, eventually
 * outweighing even the largest fixed shift.
 */
public final class OrganoidDepthBias {

    private OrganoidDepthBias() {
    }

    public static long biasedDueAt(OrganoidDue candidate, int ticksPerBlockOfDepth) {
        if (ticksPerBlockOfDepth <= 0) {
            return candidate.dueAt();
        }
        return candidate.dueAt() - (long) candidate.organoid().getBlockY() * ticksPerBlockOfDepth;
    }
}
