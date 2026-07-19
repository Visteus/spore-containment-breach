package com.visteus.sporebreach.chunkloading;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Stretch-goal extension point: a future block (e.g. an "outpost watcher") implements this to
 * register itself as a chunkload owner the same way an entity does, via the parallel
 * {@code chunkloadBlockOwners} config list. No block implements this yet.
 *
 * <p>Contract for a future implementer: call {@link ChunkloadManager#registerBlockOwner} from its
 * own load lifecycle (e.g. {@code onLoad}/{@code setPlacedBy}) - safe to call on every load, since
 * activation is idempotent - and call {@link ChunkloadManager#unregisterBlockOwner} only on
 * genuine permanent removal (the block being broken), not a transient chunk unload.
 */
public interface ChunkloadAnchor {

    BlockPos chunkloadAnchorPos();

    ResourceLocation chunkloadOwnerBlockId();
}
