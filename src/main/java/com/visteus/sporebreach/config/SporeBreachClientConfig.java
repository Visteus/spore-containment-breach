package com.visteus.sporebreach.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;

public final class SporeBreachClientConfig {

    public static final ModConfigSpec SPEC;
    public static final BooleanValue DEBUG_OVERLAY_ENABLED;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("debug");
        DEBUG_OVERLAY_ENABLED = builder
                .comment(
                        " Shows organoid cooldowns and search/raid radii on-screen. Default false."
                )
                .define("debugOverlayEnabled", false);
        builder.pop();

        SPEC = builder.build();
    }

    private SporeBreachClientConfig() {
    }
}
