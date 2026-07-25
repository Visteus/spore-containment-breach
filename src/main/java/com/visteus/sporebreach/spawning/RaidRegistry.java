package com.visteus.sporebreach.spawning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Memory-only, per-level record of raids dispatched by {@link ProtoRaidDirector}. One entry per
 * raid attempt that actually placed at least one raider: which Proto sent it, its target, when
 * it was dispatched, and the UUIDs of every raider it spawned (which thins out over time as
 * raiders die - see {@link #livingMembers}, and grows if a Vanguard calls in backup - see {@link
 * com.visteus.sporebreach.mixin.VanguardMixin}). Originally purely for observability via {@link
 * RaidDebugCommand}, {@link RaidReturnDirector} now also reads it to drive raiders back to their
 * Proto after they've engaged for a while. Not persisted, same acceptable-loss tradeoff as {@link
 * RaidTravelTracker} - losing this on restart means in-flight raiders just never return home
 * (they fall back to normal despawn handling), never a hard crash or invalid state.
 */
public final class RaidRegistry {

    private static final Map<ResourceKey<Level>, List<RaidRecord>> RAIDS_BY_LEVEL = new HashMap<>();

    private RaidRegistry() {
    }

    public record RaidRecord(UUID raidId, UUID protoId, BlockPos target, long dispatchGameTime, List<UUID> raiderIds) {
    }

    public static void register(ServerLevel level, RaidRecord record) {
        RAIDS_BY_LEVEL.computeIfAbsent(level.dimension(), key -> new ArrayList<>()).add(record);
    }

    public static List<RaidRecord> snapshot(ServerLevel level) {
        List<RaidRecord> raids = RAIDS_BY_LEVEL.get(level.dimension());
        return raids != null ? List.copyOf(raids) : List.of();
    }

    /**
     * Finds the active record for a specific raid, so {@link
     * com.visteus.sporebreach.mixin.VanguardMixin} can append a Vanguard's called-in backup to
     * the right raid's roster even if the same Proto has more than one raid in flight at once.
     */
    public static Optional<RaidRecord> findByRaidId(ServerLevel level, UUID raidId) {
        List<RaidRecord> raids = RAIDS_BY_LEVEL.get(level.dimension());
        if (raids == null) {
            return Optional.empty();
        }
        for (RaidRecord record : raids) {
            if (record.raidId().equals(raidId)) {
                return Optional.of(record);
            }
        }
        return Optional.empty();
    }

    /**
     * Drops any raid whose raiders have all died/despawned. Called opportunistically by {@link
     * RaidDebugCommand} on every invocation, and periodically by {@link RaidTravelSweepDirector}'s
     * existing tick, so the registry doesn't grow unbounded across a long session even if the
     * debug command is never run.
     */
    public static void pruneFinished(ServerLevel level) {
        List<RaidRecord> raids = RAIDS_BY_LEVEL.get(level.dimension());
        if (raids == null) {
            return;
        }
        raids.removeIf(record -> livingMembers(level, record).isEmpty());
    }

    public static List<UUID> livingMembers(ServerLevel level, RaidRecord record) {
        List<UUID> living = new ArrayList<>();
        for (UUID id : record.raiderIds()) {
            if (level.getEntity(id) != null) {
                living.add(id);
            }
        }
        return living;
    }
}
