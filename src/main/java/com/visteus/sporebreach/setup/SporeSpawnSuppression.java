package com.visteus.sporebreach.setup;

import com.Harbinger.Spore.core.SConfig;
import com.mojang.logging.LogUtils;
import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import java.util.List;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import org.slf4j.Logger;

/**
 * Neuters Spore's own vanilla biome/structure spawn injection (BiomeModification,
 * StructureModification) by zeroing the config lists they read live on every call, so Spore
 * mobs stop appearing via natural dark-area spawning and only appear through this mod's
 * organoid-driven spawning instead.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class SporeSpawnSuppression {

    private static final Logger LOGGER = LogUtils.getLogger();

    private SporeSpawnSuppression() {
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        if (!SporeBreachServerConfig.SUPPRESS_VANILLA_SPORE_SPAWNS.get()) {
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
