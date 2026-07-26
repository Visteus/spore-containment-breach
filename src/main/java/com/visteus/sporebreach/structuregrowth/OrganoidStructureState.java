package com.visteus.sporebreach.structuregrowth;

import net.minecraft.core.BlockPos;

/**
 * The in-flight half of an organoid's structure growth: which job is running right now and which
 * persisted record it belongs to. What's already been built - anchors, counts toward the per-organoid
 * cap, and enough detail to rebuild an unfinished job - lives in {@link StructureFootprintData}
 * instead, so an organoid that unloads mid-tower picks up where it left off rather than starting over.
 */
final class OrganoidStructureState {

    private StructureGrowthJob surfaceJob;
    private StructureFootprintData.Record surfaceRecord;
    private StructureGrowthJob undergroundJob;
    private StructureFootprintData.Record undergroundRecord;
    private BlockPos pendingUndergroundAnchor;
    private boolean pendingUndergroundGuaranteed;

    boolean hasActiveJob() {
        return surfaceJob != null || undergroundJob != null;
    }

    StructureGrowthJob surfaceJob() {
        return surfaceJob;
    }

    StructureFootprintData.Record surfaceRecord() {
        return surfaceRecord;
    }

    void setSurfaceJob(StructureGrowthJob job, StructureFootprintData.Record record) {
        surfaceJob = job;
        surfaceRecord = record;
    }

    void clearSurfaceJob() {
        surfaceJob = null;
        surfaceRecord = null;
    }

    StructureGrowthJob undergroundJob() {
        return undergroundJob;
    }

    StructureFootprintData.Record undergroundRecord() {
        return undergroundRecord;
    }

    void setUndergroundJob(StructureGrowthJob job, StructureFootprintData.Record record) {
        undergroundJob = job;
        undergroundRecord = record;
    }

    void clearUndergroundJob() {
        undergroundJob = null;
        undergroundRecord = null;
    }

    BlockPos pendingUndergroundAnchor() {
        return pendingUndergroundAnchor;
    }

    void setPendingUndergroundAnchor(BlockPos pos) {
        pendingUndergroundAnchor = pos;
    }

    boolean pendingUndergroundGuaranteed() {
        return pendingUndergroundGuaranteed;
    }

    void setPendingUndergroundGuaranteed(boolean guaranteed) {
        pendingUndergroundGuaranteed = guaranteed;
    }
}
