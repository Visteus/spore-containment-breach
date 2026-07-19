package com.visteus.sporebreach.chunkloading;

import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Periodic driver for Goal #4's chunkload growth, copying {@link
 * com.visteus.sporebreach.spawning.OrganoidSpawnDirector}'s tick-cadence skeleton
 * (ServerTickEvent.Post + static tick counter + interval gate).
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class ChunkloadGrowthDirector {

    private static long tickCounter;

    private ChunkloadGrowthDirector() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        int interval = SporeBreachServerConfig.CHUNKLOAD_RECHECK_INTERVAL_TICKS.get();
        if (interval <= 0 || tickCounter % interval != 0) {
            return;
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            ChunkloadManager.recheckAll(level);
        }
    }
}
