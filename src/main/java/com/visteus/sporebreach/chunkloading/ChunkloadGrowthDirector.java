package com.visteus.sporebreach.chunkloading;

import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import com.visteus.sporebreach.util.JitteredTimer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Periodic driver for Goal #4's chunkload growth, copying {@link
 * com.visteus.sporebreach.spawning.OrganoidSpawnDirector}'s tick-cadence skeleton
 * (ServerTickEvent.Post + jittered interval gate).
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class ChunkloadGrowthDirector {

    private static final JitteredTimer TIMER = new JitteredTimer();

    private ChunkloadGrowthDirector() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!event.getServer().tickRateManager().runsNormally()) {
            return;
        }
        int interval = SporeBreachServerConfig.CHUNKLOAD_RECHECK_INTERVAL_TICKS.get();
        if (!TIMER.tick(interval)) {
            return;
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            ChunkloadManager.recheckAll(level);
        }
    }
}
