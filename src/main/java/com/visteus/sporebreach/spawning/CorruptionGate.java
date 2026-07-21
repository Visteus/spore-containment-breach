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
     * Scales the configured baseline (protoRaidGroupSizeMin/Max) by the dimension's raw
     * corruption level (not tier/breakpoint) - linear from 1.0x at zero corruption up to
     * protoRaidGroupSizeMaxMultiplier at the corruption cap.
     */
    public static GroupSizeRange getRaidGroupSizeRange(ServerLevel level) {
        int baseMin = SporeBreachServerConfig.PROTO_RAID_GROUP_SIZE_MIN.get();
        int baseMax = SporeBreachServerConfig.PROTO_RAID_GROUP_SIZE_MAX.get();
        double maxMultiplier = SporeBreachServerConfig.PROTO_RAID_GROUP_SIZE_MAX_MULTIPLIER.get();

        int cap = SporeBreachServerConfig.CORRUPTION_CAP.get();
        double fraction = cap > 0 ? Math.min(1.0, (double) CorruptionTier.value(level) / cap) : 0.0;
        double multiplier = 1.0 + (maxMultiplier - 1.0) * fraction;

        int scaledMin = (int) Math.round(baseMin * multiplier);
        int scaledMax = (int) Math.round(baseMax * multiplier);
        return new GroupSizeRange(scaledMin, Math.max(scaledMin, scaledMax));
    }

    public record GroupSizeRange(int min, int max) {
    }
}
