package com.visteus.sporebreach.spawning;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Memory-only, per-level record of when each raider first arrived at its raid's target - one
 * entry per raider mapping to the game time it was first seen within {@code
 * protoRaidSearchRadius} of the target, so {@link RaidReturnDirector} can give every raider the
 * full {@code protoRaidEngagementTicks} window regardless of how long its trip there took.
 * Deliberately not persisted: losing this on restart just resets that raider's engagement clock,
 * the same acceptable-loss tradeoff {@link RaidTravelTracker} already makes.
 */
public final class RaidEngagementTracker {

    private static final Map<ResourceKey<Level>, Map<UUID, Long>> ARRIVAL_TIME_BY_LEVEL = new HashMap<>();

    private RaidEngagementTracker() {
    }

    public static boolean hasArrived(ServerLevel level, UUID raiderId) {
        Map<UUID, Long> arrivals = ARRIVAL_TIME_BY_LEVEL.get(level.dimension());
        return arrivals != null && arrivals.containsKey(raiderId);
    }

    public static void markArrived(ServerLevel level, UUID raiderId) {
        ARRIVAL_TIME_BY_LEVEL.computeIfAbsent(level.dimension(), key -> new HashMap<>()).putIfAbsent(raiderId, level.getGameTime());
    }

    /**
     * Returns the game time this raider was first marked arrived, or {@code null} if it hasn't
     * arrived yet.
     */
    public static Long arrivalGameTime(ServerLevel level, UUID raiderId) {
        Map<UUID, Long> arrivals = ARRIVAL_TIME_BY_LEVEL.get(level.dimension());
        return arrivals != null ? arrivals.get(raiderId) : null;
    }

    public static void clear(ServerLevel level, UUID raiderId) {
        Map<UUID, Long> arrivals = ARRIVAL_TIME_BY_LEVEL.get(level.dimension());
        if (arrivals != null) {
            arrivals.remove(raiderId);
        }
    }
}
