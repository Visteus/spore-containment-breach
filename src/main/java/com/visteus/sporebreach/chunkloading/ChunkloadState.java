package com.visteus.sporebreach.chunkloading;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

/**
 * Mutable per-owner growth state. {@code anchorChunk} is frozen at activation (matching Spore's
 * own {@code Proto.loadChunks()} behavior of capturing position once rather than re-tracking as
 * the organoid wanders). {@code activationGameTime} is an absolute {@code level.getGameTime()}
 * snapshot rather than an incrementing counter, so elapsed time is restart-safe for free.
 * {@code lastIssuedRadius} of 0 means nothing has been force-loaded yet. {@code expectedBlockId}
 * is only set for {@link ChunkloadOwnerId.BlockOwner} owners - it's what let ChunkloadManager
 * tell "the config entry for this block id was removed" (freeze in place) apart from "the block
 * itself is gone" (auto-teardown as a leak guard), since BlockOwner itself only carries a
 * position, not the block that was there at registration time.
 */
public final class ChunkloadState {

    private final ChunkPos anchorChunk;
    private final long activationGameTime;
    private int lastIssuedRadius;
    @Nullable
    private final ResourceLocation expectedBlockId;

    public ChunkloadState(ChunkPos anchorChunk, long activationGameTime, int lastIssuedRadius, @Nullable ResourceLocation expectedBlockId) {
        this.anchorChunk = anchorChunk;
        this.activationGameTime = activationGameTime;
        this.lastIssuedRadius = lastIssuedRadius;
        this.expectedBlockId = expectedBlockId;
    }

    public ChunkPos anchorChunk() {
        return anchorChunk;
    }

    public long activationGameTime() {
        return activationGameTime;
    }

    public int lastIssuedRadius() {
        return lastIssuedRadius;
    }

    public void setLastIssuedRadius(int lastIssuedRadius) {
        this.lastIssuedRadius = lastIssuedRadius;
    }

    @Nullable
    public ResourceLocation expectedBlockId() {
        return expectedBlockId;
    }
}
