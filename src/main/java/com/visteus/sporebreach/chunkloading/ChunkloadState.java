package com.visteus.sporebreach.chunkloading;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

/**
 * Mutable per-owner growth state. {@code anchorChunk} is frozen at activation (matching Spore's
 * own {@code Proto.loadChunks()} behavior of capturing position once rather than re-tracking as
 * the organoid wanders). {@code anchorY} is frozen alongside it, at chunk-center X/Z but exact
 * block Y - it's the vertical center Goal #5's biome-paint sphere is built around; {@link
 * #ANCHOR_Y_UNSET} marks state loaded from a save predating this field, letting callers fall back
 * to a live entity lookup instead of silently centering on 0. {@code activationGameTime} is an
 * absolute {@code level.getGameTime()} snapshot rather than an incrementing counter, so elapsed
 * time is restart-safe for free. {@code lastIssuedRadius} of 0 means nothing has been force-loaded
 * yet. {@code expectedBlockId} is only set for {@link ChunkloadOwnerId.BlockOwner} owners - it's
 * what let ChunkloadManager tell "the config entry for this block id was removed" (freeze in
 * place) apart from "the block itself is gone" (auto-teardown as a leak guard), since BlockOwner
 * itself only carries a position, not the block that was there at registration time.
 */
public final class ChunkloadState {

    /** Sentinel {@link #anchorY} for state loaded from a save predating that field. */
    public static final int ANCHOR_Y_UNSET = Integer.MIN_VALUE;

    private final ChunkPos anchorChunk;
    private final int anchorY;
    private final long activationGameTime;
    private int lastIssuedRadius;
    @Nullable
    private final ResourceLocation expectedBlockId;

    public ChunkloadState(
            ChunkPos anchorChunk, int anchorY, long activationGameTime, int lastIssuedRadius, @Nullable ResourceLocation expectedBlockId
    ) {
        this.anchorChunk = anchorChunk;
        this.anchorY = anchorY;
        this.activationGameTime = activationGameTime;
        this.lastIssuedRadius = lastIssuedRadius;
        this.expectedBlockId = expectedBlockId;
    }

    public ChunkPos anchorChunk() {
        return anchorChunk;
    }

    public int anchorY() {
        return anchorY;
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
