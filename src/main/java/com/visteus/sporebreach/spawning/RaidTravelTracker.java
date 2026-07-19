package com.visteus.sporebreach.spawning;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * Memory-only, per-level tracking of non-Calamity raiders currently being "inched forward" by
 * {@link RaidTravelTrackingEvents}/{@link RaidTravelSweepDirector} - one entry per traveling
 * raider mapping to its last forced chunk and when tracking started. Deliberately not persisted:
 * losing this on restart just means a raider stops getting its extra chunk ticket until it next
 * crosses a chunk boundary, the same acceptable-loss tradeoff {@link
 * com.visteus.sporebreach.tracking.OrganoidRegistry} already makes for organoid tracking.
 */
public final class RaidTravelTracker {

    private static final Map<ResourceKey<Level>, Map<UUID, RaidTravelState>> STATE_BY_LEVEL = new HashMap<>();

    private RaidTravelTracker() {
    }

    public record RaidTravelState(ChunkPos currentChunk, long trackedSinceGameTime) {
    }

    public static void track(ServerLevel level, UUID travelerId, ChunkPos initialChunk) {
        STATE_BY_LEVEL
                .computeIfAbsent(level.dimension(), key -> new HashMap<>())
                .put(travelerId, new RaidTravelState(initialChunk, level.getGameTime()));
    }

    public static boolean isTracked(ServerLevel level, UUID travelerId) {
        Map<UUID, RaidTravelState> tracked = STATE_BY_LEVEL.get(level.dimension());
        return tracked != null && tracked.containsKey(travelerId);
    }

    public static ChunkPos currentChunk(ServerLevel level, UUID travelerId) {
        Map<UUID, RaidTravelState> tracked = STATE_BY_LEVEL.get(level.dimension());
        RaidTravelState state = tracked != null ? tracked.get(travelerId) : null;
        return state != null ? state.currentChunk() : null;
    }

    public static void updateChunk(ServerLevel level, UUID travelerId, ChunkPos newChunk) {
        Map<UUID, RaidTravelState> tracked = STATE_BY_LEVEL.get(level.dimension());
        if (tracked == null) {
            return;
        }
        RaidTravelState existing = tracked.get(travelerId);
        if (existing != null) {
            tracked.put(travelerId, new RaidTravelState(newChunk, existing.trackedSinceGameTime()));
        }
    }

    /**
     * Stops tracking a raider, returning its last forced chunk so the caller can release it.
     * Returns {@code null} if the raider wasn't tracked.
     */
    public static ChunkPos stopTracking(ServerLevel level, UUID travelerId) {
        Map<UUID, RaidTravelState> tracked = STATE_BY_LEVEL.get(level.dimension());
        if (tracked == null) {
            return null;
        }
        RaidTravelState removed = tracked.remove(travelerId);
        return removed != null ? removed.currentChunk() : null;
    }

    /**
     * Defensive copy for {@link RaidTravelSweepDirector} to iterate while this map may be
     * concurrently mutated (e.g. a raider arriving/dying mid-sweep).
     */
    public static Map<UUID, RaidTravelState> snapshot(ServerLevel level) {
        Map<UUID, RaidTravelState> tracked = STATE_BY_LEVEL.get(level.dimension());
        return tracked != null ? new HashMap<>(tracked) : Map.of();
    }
}
