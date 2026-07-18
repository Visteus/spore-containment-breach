package com.visteus.sporebreach.setup;

import com.Harbinger.Spore.core.SConfig;
import com.mojang.logging.LogUtils;
import com.visteus.sporebreach.config.SporeBreachCommonConfig;
import java.util.List;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

/**
 * Neuters Spore's own vanilla biome/structure spawn injection (BiomeModification,
 * StructureModification) by zeroing the config lists they read live on every call, so Spore
 * mobs stop appearing via natural dark-area spawning and only appear through this mod's
 * organoid-driven spawning instead.
 */
public final class SporeSpawnSuppression {

    private static final Logger LOGGER = LogUtils.getLogger();

    private SporeSpawnSuppression() {
    }

    public static void onCommonSetup(FMLCommonSetupEvent event) {
        if (!SporeBreachCommonConfig.SUPPRESS_VANILLA_SPORE_SPAWNS.get()) {
            return;
        }

        SConfig.SERVER.spawns.set(List.of());
        SConfig.SERVER.structure_spawns.set(List.of());
        LOGGER.info(
                "spore_containment_breach: cleared Spore's vanilla biome/structure spawn lists "
                        + "(suppressVanillaSporeSpawns=true) - Spore mobs will only appear via organoid-driven spawning"
        );
    }
}
