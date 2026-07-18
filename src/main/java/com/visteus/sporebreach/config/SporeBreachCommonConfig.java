package com.visteus.sporebreach.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;

public final class SporeBreachCommonConfig {

    public static final ModConfigSpec SPEC;
    public static final BooleanValue SUPPRESS_VANILLA_SPORE_SPAWNS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("spawning");
        SUPPRESS_VANILLA_SPORE_SPAWNS = builder
                .comment(
                        "If true, clears Spore's own biome/structure spawn-injection lists",
                        "(SConfig.SERVER.spawns and structure_spawns) during common setup, so vanilla",
                        "natural spawning no longer places Spore mobs in dark areas. Organoid-driven",
                        "spawning (Mounds/Proto-Hiveminds) is unaffected by this toggle.",
                        "Applied once at startup - requires a game restart to take effect.",
                        "Default true."
                )
                .define("suppressVanillaSporeSpawns", true);
        builder.pop();

        SPEC = builder.build();
    }

    private SporeBreachCommonConfig() {
    }
}
