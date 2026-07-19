package com.visteus.sporebreach.chunkloading;

import java.util.UUID;
import net.minecraft.core.BlockPos;

/**
 * Unifying key for both entity- and block-owned chunkload state, exploiting that
 * {@link net.neoforged.neoforge.common.world.chunk.TicketController#forceChunk} already has both
 * a UUID and a BlockPos overload - no synthetic UUID is needed for block owners.
 */
public sealed interface ChunkloadOwnerId {

    record EntityOwner(UUID entityId) implements ChunkloadOwnerId {
    }

    record BlockOwner(BlockPos pos) implements ChunkloadOwnerId {
    }
}
