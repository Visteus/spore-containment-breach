package com.visteus.sporebreach.biome;

import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import com.visteus.sporebreach.util.JitteredTimer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Periodic driver for dead_scar block decay, copying {@link BiomePaintGrowthDirector}'s exact
 * tick-cadence skeleton.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class DeadScarDecayDirector {

    private static final JitteredTimer TIMER = new JitteredTimer();

    private DeadScarDecayDirector() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!event.getServer().tickRateManager().runsNormally()) {
            return;
        }
        if (!SporeBreachServerConfig.DEAD_SCAR_DECAY_ENABLED.get()) {
            return;
        }

        if (!TIMER.tick(SporeBreachServerConfig.DEAD_SCAR_DECAY_INTERVAL_TICKS.get())) {
            return;
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            DeadScarDecayManager.topUpTickets(level);
            DeadScarDecayManager.advanceSweeps(level);
        }
    }
}
