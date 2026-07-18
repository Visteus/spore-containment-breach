package com.visteus.sporebreach.tracking;

import com.Harbinger.Spore.Sentities.Organoids.Mound;
import com.visteus.sporebreach.SporeContainmentBreach;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

/**
 * Keeps {@link OrganoidRegistry} in sync with live Mound entities. Spore already tracks Protos
 * itself (SporeSavedData.getHiveminds, populated the same way in Spore's own HandlerEvents), so
 * only Mounds need mirroring here.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class OrganoidTrackingEvents {

    private OrganoidTrackingEvents() {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Mound mound && event.getLevel() instanceof ServerLevel level) {
            OrganoidRegistry.add(level, mound);
        }
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof Mound mound && event.getLevel() instanceof ServerLevel level) {
            OrganoidRegistry.remove(level, mound);
        }
    }
}
