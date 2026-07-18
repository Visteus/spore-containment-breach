package com.visteus.sporebreach.spawning;

import com.visteus.sporebreach.config.SporeBreachServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Inert placeholder seam for Goal #7's World Corruption system. Every method here is a stub in
 * Goal #1 - {@link ProtoRaidDirector} and {@link MoundDefenseSpawner} consult these instead of
 * their own logic so Goal #7 only has to change this one file.
 */
public final class CorruptionGate {

    private CorruptionGate() {
    }

    /**
     * Goal #1: always false. Goal #7 will allow this once a world's corruption level passes the
     * "Calamities and Wombs" breakpoint described in CLAUDE.md's World Corruption section.
     */
    public static boolean isCalamitySpawningAllowed(ServerLevel level, BlockPos anchor) {
        return false;
    }

    /**
     * Goal #1: always returns the configured baseline (protoRaidGroupSizeMin/Max). Goal #7 will
     * scale this up with corruption tier (probably a configurable multiplier)
     */
    public static GroupSizeRange getRaidGroupSizeRange(ServerLevel level) {
        return new GroupSizeRange(SporeBreachServerConfig.PROTO_RAID_GROUP_SIZE_MIN.get(), SporeBreachServerConfig.PROTO_RAID_GROUP_SIZE_MAX.get());
    }

    public record GroupSizeRange(int min, int max) {
    }
}
