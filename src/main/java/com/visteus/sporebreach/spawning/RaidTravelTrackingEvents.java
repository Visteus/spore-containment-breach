package com.visteus.sporebreach.spawning;

import com.Harbinger.Spore.Sentities.BaseEntities.Infected;
import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.chunkloading.ChunkloadManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityEvent.EnteringSection;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

/**
 * Event-driven half of the raid-travel sliding window (see {@link RaidTravelTracker}/
 * {@link RaidTravelSweepDirector}): advances the two-chunk "inching forward" window as a tracked
 * raider crosses chunk boundaries. Mirrors the exact same {@code EntityEvent.EnteringSection}
 * base Spore itself subscribes to for Calamity/Vanguard-style {@code ChunkLoaderMob} travel
 * (see the decompiled {@code HandlerEvents.LoadCalamity}) - just scoped to our own tracked
 * non-Calamity raiders instead of anything implementing {@code ChunkLoaderMob}.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class RaidTravelTrackingEvents {

    private RaidTravelTrackingEvents() {
    }

    @SubscribeEvent
    public static void onEnteringSection(EnteringSection event) {
        Entity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || !event.didChunkChange()) {
            return;
        }
        if (!RaidTravelTracker.isTracked(level, entity.getUUID())) {
            return;
        }

        if (entity instanceof Infected infected && infected.getSearchPos() == null) {
            // SearchAreaGoal already decided it arrived (within 9 blocks) and cleared its own
            // target - stop babysitting it here too, no separate arrival check needed.
            releaseAndStopTracking(level, entity);
            return;
        }

        ChunkPos newChunk = event.getNewPos().chunk();
        ChunkPos previous = RaidTravelTracker.currentChunk(level, entity.getUUID());
        ChunkloadManager.advanceRaidTravelChunk(level, entity.getUUID(), newChunk, previous);
        RaidTravelTracker.updateChunk(level, entity.getUUID(), newChunk);
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Entity entity = event.getEntity();
        Entity.RemovalReason reason = entity.getRemovalReason();
        if (reason != null && reason.shouldDestroy() && RaidTravelTracker.isTracked(level, entity.getUUID())) {
            releaseAndStopTracking(level, entity);
        }
    }

    private static void releaseAndStopTracking(ServerLevel level, Entity entity) {
        ChunkPos last = RaidTravelTracker.stopTracking(level, entity.getUUID());
        if (last != null) {
            ChunkloadManager.releaseRaidTravelChunk(level, entity.getUUID(), last);
        }
    }
}
