package com.visteus.sporebreach.structuregrowth;

import com.Harbinger.Spore.Sentities.Organoids.Mound;
import com.Harbinger.Spore.Sentities.Organoids.Proto;
import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import com.visteus.sporebreach.tracking.OrganoidRegistry;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Periodic driver for Outpost Watcher towers, using the same recheck/pass cadence pair as
 * {@link StructureGrowthDirector}. Deliberately *not* gated on {@code structureGrowthMode}:
 * watchers are a base Spore feature this mod is making more common, so they still appear for
 * players who have chosen to keep base Spore's own shell growth.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class OutpostWatcherDirector {

    private static long recheckCounter;
    private static long passCounter;

    private OutpostWatcherDirector() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!event.getServer().tickRateManager().runsNormally()) {
            return;
        }
        if (!SporeBreachServerConfig.OUTPOST_WATCHER_ENABLED.get()) {
            return;
        }

        recheckCounter++;
        passCounter++;

        boolean recheck = due(recheckCounter, SporeBreachServerConfig.OUTPOST_WATCHER_RECHECK_INTERVAL_TICKS.get());
        boolean pass = due(passCounter, SporeBreachServerConfig.OUTPOST_WATCHER_PASS_INTERVAL_TICKS.get());
        if (!recheck && !pass) {
            return;
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (Mound mound : OrganoidRegistry.get(level)) {
                if (recheck) {
                    OutpostWatcherGrowth.tryStartJob(level, mound, false);
                }
                if (pass) {
                    OutpostWatcherGrowth.tryAdvanceJob(level, mound);
                }
            }
            for (Proto proto : OrganoidRegistry.getProtos(level)) {
                if (recheck) {
                    OutpostWatcherGrowth.tryStartJob(level, proto, false);
                }
                if (pass) {
                    OutpostWatcherGrowth.tryAdvanceJob(level, proto);
                }
            }
        }
    }

    private static boolean due(long counter, int interval) {
        return interval > 0 && counter % interval == 0;
    }
}
