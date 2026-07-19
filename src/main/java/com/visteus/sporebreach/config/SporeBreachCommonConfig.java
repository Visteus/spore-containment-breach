package com.visteus.sporebreach.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;

public final class SporeBreachCommonConfig {

    public static final ModConfigSpec SPEC;

    public static final BooleanValue SUPPRESS_VANILLA_SPORE_SPAWNS;
    public static final BooleanValue NEUTRALIZE_PROTO_WORLD_MODIFIER;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("spawning");
        SUPPRESS_VANILLA_SPORE_SPAWNS = builder
                .comment(
                        " If true, removes Spore's own spawns from the worldgen spawn table, so that this mod's",
                        " own spawn system is the only one that can spawn Spore mobs.",
                        " Default true."
                )
                .define("suppressVanillaSporeSpawns", true);
        builder.pop();

        builder.push("corruption");
        NEUTRALIZE_PROTO_WORLD_MODIFIER = builder
                .comment(
                        " If true, replaces Spore's Proto World modifier with the custom World Corruption system from this mod.",
                        " Default true."
                )
                .define("neutralizeProtoWorldModifier", true);
        builder.pop();

        SPEC = builder.build();
    }

    private SporeBreachCommonConfig() {
    }
}
