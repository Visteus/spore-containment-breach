package com.visteus.sporebreach.biome;

import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Periodic driver for Goal #5's biome painting, copying {@link
 * com.visteus.sporebreach.chunkloading.ChunkloadGrowthDirector}'s tick-cadence skeleton. Two
 * cadences, mirroring {@code StructureGrowthDirector}'s recheck+pass shape: the main paint cadence
 * (grow/advance/downgrade) and a separate, much slower water-reseed cadence - biome painting never
 * needs to react faster than chunkload itself grows, and the reseed roll only needs to happen every
 * so often per {@code AreaWaterReplacementJob}'s own javadoc.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class BiomePaintGrowthDirector {

    private static long paintCounter;
    private static long reseedCounter;

    private BiomePaintGrowthDirector() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!SporeBreachServerConfig.BIOME_PAINT_ENABLED.get()) {
            return;
        }

        paintCounter++;
        reseedCounter++;

        boolean paintDue = due(paintCounter, SporeBreachServerConfig.BIOME_PAINT_RECHECK_INTERVAL_TICKS.get());
        boolean reseedDue = due(reseedCounter, SporeBreachServerConfig.AREA_WATER_RESEED_INTERVAL_TICKS.get());
        if (!paintDue && !reseedDue) {
            return;
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (paintDue) {
                BiomePaintManager.recheckAll(level);
                BiomePaintManager.advance(level);
                BiomePaintManager.advanceWater(level);
                BiomePaintManager.processDowngrades(level);
            }
            if (reseedDue) {
                BiomePaintManager.tryReseedWater(level);
            }
        }
    }

    private static boolean due(long counter, int interval) {
        return interval > 0 && counter % interval == 0;
    }
}
