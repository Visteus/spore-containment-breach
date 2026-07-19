package com.visteus.sporebreach.spawning;

import com.visteus.sporebreach.config.SporeBreachServerConfig;
import com.visteus.sporebreach.corruption.CorruptionTier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Spawning-facing facade over Goal #7's World Corruption system - {@link ProtoRaidDirector} and
 * {@link MoundDefenseSpawner} consult these instead of {@link CorruptionTier} directly, keeping
 * the breakpoint semantics for spawning decisions in one place.
 */
public final class CorruptionGate {

    private CorruptionGate() {
    }

    /**
     * Stage 1: whether a Proto-Hivemind may send raids at all.
     */
    public static boolean areRaidsAllowed(ServerLevel level) {
        return CorruptionTier.areRaidsAllowed(level);
    }

    /**
     * Stage 3: whether Calamities/Wombs may be created or spawned.
     */
    public static boolean isCalamitySpawningAllowed(ServerLevel level, BlockPos anchor) {
        return CorruptionTier.isCalamitySpawningAllowed(level);
    }

    /**
     * Stage 4: whether a Proto raid may include Calamities - a later, separate breakpoint than
     * calamities-can-spawn-at-all.
     */
    public static boolean isCalamityRaidAllowed(ServerLevel level, BlockPos anchor) {
        return CorruptionTier.isCalamityRaidAllowed(level);
    }

    /**
     * Always returns the configured baseline (protoRaidGroupSizeMin/Max). Not corruption-scaled -
     * left as a documented future enhancement rather than one of the six explicit breakpoints.
     */
    public static GroupSizeRange getRaidGroupSizeRange(ServerLevel level) {
        return new GroupSizeRange(SporeBreachServerConfig.PROTO_RAID_GROUP_SIZE_MIN.get(), SporeBreachServerConfig.PROTO_RAID_GROUP_SIZE_MAX.get());
    }

    public record GroupSizeRange(int min, int max) {
    }
}
