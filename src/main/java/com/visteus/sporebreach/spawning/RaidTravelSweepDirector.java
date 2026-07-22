package com.visteus.sporebreach.spawning;

import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.chunkloading.ChunkloadManager;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Safety net for the raid-travel sliding window (see {@link RaidTravelTracker}/{@link
 * RaidTravelTrackingEvents}), following {@link
 * com.visteus.sporebreach.chunkloading.ChunkloadGrowthDirector}'s same tick-cadence skeleton.
 * The event-driven path handles almost everything, but can't catch a raider that clears its own
 * search position without crossing another chunk boundary afterward - without this sweep, such
 * a raider would hold its last forced chunk indefinitely. Also enforces
 * protoRaidTravelMaxDurationTicks so a permanently stuck raider doesn't leak a ticket forever.
 * Piggybacks {@link RaidRegistry#pruneFinished} onto the same cadence so raids with no living
 * members drop out of {@link RaidDebugCommand}'s reporting even if that command is never run.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class RaidTravelSweepDirector {

    private static long tickCounter;

    private RaidTravelSweepDirector() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!event.getServer().tickRateManager().runsNormally()) {
            return;
        }
        tickCounter++;
        int interval = SporeBreachServerConfig.PROTO_RAID_TRAVEL_SWEEP_INTERVAL_TICKS.get();
        if (interval <= 0 || tickCounter % interval != 0) {
            return;
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            sweep(level);
            RaidRegistry.pruneFinished(level);
        }
    }

    private static void sweep(ServerLevel level) {
        int maxDuration = SporeBreachServerConfig.PROTO_RAID_TRAVEL_MAX_DURATION_TICKS.get();
        long gameTime = level.getGameTime();

        for (Map.Entry<UUID, RaidTravelTracker.RaidTravelState> entry : RaidTravelTracker.snapshot(level).entrySet()) {
            UUID travelerId = entry.getKey();
            RaidTravelTracker.RaidTravelState state = entry.getValue();

            boolean gone = level.getEntity(travelerId) == null;
            boolean timedOut = gameTime - state.trackedSinceGameTime() > maxDuration;
            if (gone || timedOut) {
                RaidTravelTracker.stopTracking(level, travelerId);
                ChunkloadManager.releaseRaidTravelChunk(level, travelerId, state.currentChunk());
            }
        }
    }
}
