package com.visteus.sporebreach.persistence;

import com.visteus.sporebreach.SporeContainmentBreach;
import java.util.ArrayList;
import java.util.List;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Tracks every live {@link PersistedData} instance and decides when {@link PersistedData#saveIfDirty()}
 * actually runs, so individual concerns (MoundGenesisData, later Goal #7 state, etc.) don't each
 * need their own autosave/shutdown-flush wiring.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class PersistedDataManager {

    private static final int AUTOSAVE_INTERVAL_TICKS = 6000; // 5 minutes - bookkeeping cadence, not a balance lever.

    private static final List<PersistedData> INSTANCES = new ArrayList<>();

    private static long tickCounter;

    private PersistedDataManager() {
    }

    static synchronized void register(PersistedData data) {
        INSTANCES.add(data);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        if (tickCounter % AUTOSAVE_INTERVAL_TICKS == 0) {
            saveAll();
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        saveAll();
    }

    @SubscribeEvent
    public static synchronized void onServerStopped(ServerStoppedEvent event) {
        INSTANCES.clear();
    }

    private static synchronized void saveAll() {
        for (PersistedData data : INSTANCES) {
            data.saveIfDirty();
        }
    }
}
