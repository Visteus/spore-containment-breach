package com.visteus.sporebreach.structuregrowth;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;

/**
 * Per-organoid progress for the goal #3 structure growth system. Deliberately memory-only, not
 * persisted to disk - same rationale as
 * {@link com.visteus.sporebreach.tracking.OrganoidRegistry}: an organoid mid-job that unloads
 * simply restarts fresh next time it's picked up again, an acceptable loss for an in-progress
 * structure rather than something worth persisting.
 */
final class OrganoidStructureState {

    private final List<BlockPos> anchors = new ArrayList<>();
    private int structuresStarted;
    private boolean toppingPlaced;
    private StructureGrowthJob surfaceJob;
    private StructureGrowthJob undergroundJob;
    private BlockPos pendingUndergroundAnchor;

    List<BlockPos> anchors() {
        return anchors;
    }

    int structuresStarted() {
        return structuresStarted;
    }

    void recordAnchor(BlockPos pos) {
        anchors.add(pos);
        structuresStarted++;
    }

    boolean toppingPlaced() {
        return toppingPlaced;
    }

    void markToppingPlaced() {
        toppingPlaced = true;
    }

    boolean hasActiveJob() {
        return surfaceJob != null || undergroundJob != null;
    }

    StructureGrowthJob surfaceJob() {
        return surfaceJob;
    }

    void setSurfaceJob(StructureGrowthJob job) {
        surfaceJob = job;
    }

    StructureGrowthJob undergroundJob() {
        return undergroundJob;
    }

    void setUndergroundJob(StructureGrowthJob job) {
        undergroundJob = job;
    }

    BlockPos pendingUndergroundAnchor() {
        return pendingUndergroundAnchor;
    }

    void setPendingUndergroundAnchor(BlockPos pos) {
        pendingUndergroundAnchor = pos;
    }
}
