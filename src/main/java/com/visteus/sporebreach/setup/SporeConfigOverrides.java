package com.visteus.sporebreach.setup;

import com.Harbinger.Spore.core.SConfig;
import com.mojang.logging.LogUtils;
import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.config.SporeBreachOverridesConfig;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

/**
 * Replaces a handful of base Spore's own {@code SConfig} defaults (both its STARTUP and COMMON
 * specs) with this mod's own values, listed in {@link SporeBreachOverridesConfig}.
 *
 * <p>Safe to do from {@link FMLCommonSetupEvent}: {@code neoforge.mods.toml} declares an
 * {@code ordering="AFTER"} dependency on {@code spore}, so Spore's own constructor - which
 * registers and synchronously loads {@code SConfig.SERVER_SPEC} (STARTUP) - has already run by
 * the time this mod's constructor runs, and {@code SConfig.DATAGEN_SPEC} (COMMON) is loaded well
 * before {@code FMLCommonSetupEvent} fires. {@code ConfigValue.set()} updates the live value read
 * by {@code .get()} immediately, matching the pattern already used by {@link
 * SporeSpawnSuppression} and {@link com.visteus.sporebreach.corruption.ProtoWorldModifierSuppression}.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class SporeConfigOverrides {

    private static final Logger LOGGER = LogUtils.getLogger();

    private SporeConfigOverrides() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        if (!SporeBreachOverridesConfig.APPLY_SPORE_CONFIG_OVERRIDES.get()) {
            return;
        }

        SConfig.DATAGEN.block_infection.set(SporeBreachOverridesConfig.INFECTED_BLOCK_COUNTERPARTS.get());
        SConfig.DATAGEN.name.set(SporeBreachOverridesConfig.INFECTED_PLAYER_NAMES.get());
        SConfig.SERVER.max_infected_cap.set(SporeBreachOverridesConfig.MAX_REGULAR_INFECTED.get());
        SConfig.SERVER.max_evolved_cap.set(SporeBreachOverridesConfig.MAX_EVOLVED_INFECTED.get());
        SConfig.SERVER.max_hyper_cap.set(SporeBreachOverridesConfig.MAX_HYPER_INFECTED.get());
        SConfig.SERVER.max_organoid_cap.set(SporeBreachOverridesConfig.MAX_ORGANOIDS.get());
        SConfig.SERVER.despawn_blacklist.set(SporeBreachOverridesConfig.DESPAWN_BLACKLIST.get());

        LOGGER.info(
                "sporebreach: applied spore config overrides (applySporeConfigOverrides=true) - "
                        + "block infection, infected player names, despawn caps, and the despawn blacklist "
                        + "now use this mod's own defaults"
        );
    }
}
