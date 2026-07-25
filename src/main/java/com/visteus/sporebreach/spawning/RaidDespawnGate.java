package com.visteus.sporebreach.spawning;

import com.visteus.sporebreach.config.SporeBreachServerConfig;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * Whether an entity is an active raid member that base Spore's despawn systems ({@code
 * Sevents.DespawnSystem}'s population-cap trim, and vanilla {@code Mob.checkDespawn()} via {@code
 * removeWhenFarAway}) must leave alone - see {@link com.visteus.sporebreach.mixin.DespawnSystemMixin}/
 * {@link com.visteus.sporebreach.mixin.MobMixin}. Without this, a raider deliberately traveling
 * far from every player on its way home to a Proto (see {@link RaidReturnDirector}) would get
 * reaped mid-transit, silently defeating the return trip.
 * <p>
 * Protection is derived entirely from timers the raid system already tracks - {@link
 * RaidRegistry.RaidRecord#dispatchGameTime()} and {@link RaidEngagementTracker}'s per-raider
 * arrival clock - so it lapses automatically the moment {@link RaidReturnDirector} itself would
 * give up steering a raider (travel timeout while outbound, or {@code abandonReturn} once past
 * the engagement + return window), rather than needing its own separate state.
 */
public final class RaidDespawnGate {

    private RaidDespawnGate() {
    }

    public static boolean isProtected(Entity entity) {
        if (!SporeBreachServerConfig.PROTO_RAID_DIRECTED_TRAVEL.get()) {
            return false;
        }
        if (!(entity.level() instanceof ServerLevel level)) {
            return false;
        }
        CompoundTag data = entity.getPersistentData();
        if (!data.hasUUID(ProtoRaidDirector.RAID_BY_KEY) || !data.hasUUID(ProtoRaidDirector.RAID_ID_KEY)) {
            return false;
        }

        UUID raidId = data.getUUID(ProtoRaidDirector.RAID_ID_KEY);
        Optional<RaidRegistry.RaidRecord> record = RaidRegistry.findByRaidId(level, raidId);
        if (record.isEmpty()) {
            return false;
        }

        long now = level.getGameTime();
        Long arrivalTime = RaidEngagementTracker.arrivalGameTime(level, entity.getUUID());
        if (arrivalTime == null) {
            long travelTimeout = SporeBreachServerConfig.PROTO_RAID_TRAVEL_MAX_DURATION_TICKS.get();
            return now - record.get().dispatchGameTime() <= travelTimeout;
        }

        long protectedWindow = (long) SporeBreachServerConfig.PROTO_RAID_ENGAGEMENT_TICKS.get()
                + SporeBreachServerConfig.PROTO_RAID_RETURN_MAX_DURATION_TICKS.get();
        return now - arrivalTime <= protectedWindow;
    }
}
