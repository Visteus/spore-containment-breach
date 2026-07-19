package com.visteus.sporebreach.tracking;

import com.Harbinger.Spore.Sentities.BaseEntities.Calamity;
import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.spawning.CalamityCapEnforcer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

/**
 * Keeps {@link CalamityRegistry} in sync with live Calamity entities, and triggers
 * {@link CalamityCapEnforcer} whenever the tracked count grows - see the "Global Calamity cap"
 * plan for why the cap is enforced globally, not per-dimension.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class CalamityTrackingEvents {

    private CalamityTrackingEvents() {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Calamity calamity && event.getLevel() instanceof ServerLevel level) {
            CalamityRegistry.add(level, calamity);
            CalamityCapEnforcer.enforceCap();
        }
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof Calamity calamity && event.getLevel() instanceof ServerLevel level) {
            CalamityRegistry.remove(level, calamity);
        }
    }
}
