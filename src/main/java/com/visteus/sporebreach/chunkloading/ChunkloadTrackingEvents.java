package com.visteus.sporebreach.chunkloading;

import com.visteus.sporebreach.SporeContainmentBreach;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

/**
 * Bridges entity join/leave to {@link ChunkloadManager}. Only permanent removal (kill/discard)
 * tears down chunkload state on leave - a transient unload (the organoid's own chunk being
 * unloaded, which shouldn't normally happen since it holds its own forced ticket, or a player
 * disconnect edge case) must not, since ChunkloadData.snapshot/recheckAll is what re-resolves a
 * still-tracked owner on the next cycle.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class ChunkloadTrackingEvents {

    private ChunkloadTrackingEvents() {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Entity entity = event.getEntity();
        ChunkloadEntry entry = ChunkloadEntryLookup.forEntityType(entity.getType());
        if (entry != null) {
            ChunkloadManager.activateEntityOwner(level, entity, entry);
        }
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Entity entity = event.getEntity();
        Entity.RemovalReason reason = entity.getRemovalReason();
        if (reason != null && reason.shouldDestroy()) {
            ChunkloadManager.teardownEntityOwner(level, entity);
        }
    }
}
