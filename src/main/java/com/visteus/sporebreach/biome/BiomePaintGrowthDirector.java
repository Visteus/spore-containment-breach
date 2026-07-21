package com.visteus.sporebreach.biome;

import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Periodic driver for Goal #5's biome painting, copying {@link
 * com.visteus.sporebreach.chunkloading.ChunkloadGrowthDirector}'s tick-cadence skeleton on its own
 * configurable interval - biome painting piggybacks on chunkload's own growth numbers, so it never
 * needs to react faster than chunkload itself grows, and can safely run on a slower cadence.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class BiomePaintGrowthDirector {

    private static long tickCounter;

    private BiomePaintGrowthDirector() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        int interval = SporeBreachServerConfig.BIOME_PAINT_RECHECK_INTERVAL_TICKS.get();
        if (interval <= 0 || tickCounter % interval != 0 || !SporeBreachServerConfig.BIOME_PAINT_ENABLED.get()) {
            return;
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            BiomePaintManager.recheckAll(level);
            BiomePaintManager.advance(level);
            BiomePaintManager.advanceWater(level);
            BiomePaintManager.processDowngrades(level);
        }
    }
}
