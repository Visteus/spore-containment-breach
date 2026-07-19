package com.visteus.sporebreach.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;

public final class SporeBreachCommonConfig {

    public static final ModConfigSpec SPEC;

    public static final BooleanValue SUPPRESS_VANILLA_SPORE_SPAWNS;
    public static final BooleanValue SUPPRESS_PROTO_VIGIL_RAID;
    public static final BooleanValue NEUTRALIZE_PROTO_WORLD_MODIFIER;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("spawning");
        SUPPRESS_VANILLA_SPORE_SPAWNS = builder
                .comment(
                        " Stops Spore mobs from randomly spawning in the world, so this mod's own spawn system",
                        " is the only source of Spore mobs. Default true."
                )
                .define("suppressVanillaSporeSpawns", true);
        SUPPRESS_PROTO_VIGIL_RAID = builder
                .comment(
                        " Disables Spore's own Vigil-ambush raid, which teleports a Vigil next to a detected",
                        " player, since this mod's own raid system replaces it. Default true."
                )
                .define("suppressProtoVigilRaid", true);
        builder.pop();

        builder.push("corruption");
        NEUTRALIZE_PROTO_WORLD_MODIFIER = builder
                .comment(
                        " Replaces Spore's own Proto World difficulty modifier with this mod's World Corruption",
                        " system. Default true."
                )
                .define("neutralizeProtoWorldModifier", true);
        builder.pop();

        SPEC = builder.build();
    }

    private SporeBreachCommonConfig() {
    }
}
