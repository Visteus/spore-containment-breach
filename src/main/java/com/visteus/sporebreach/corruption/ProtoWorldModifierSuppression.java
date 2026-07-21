package com.visteus.sporebreach.corruption;

import com.Harbinger.Spore.core.SConfig;
import com.mojang.logging.LogUtils;
import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.config.SporeBreachCommonConfig;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

/**
 * Neuters Spore's own "Proto World Modifier" - the {@code hiveminds >= proto_spawn_world_mod}
 * threshold that gates instant evolution, gear enchanting, the Linked flag, and Calamity
 * Adaptations - by pushing the threshold out of reach, so it never fires on its own. Goal #7's
 * World Corruption system ({@link CorruptionEffects}) reimplements those same effects itself,
 * driven by its own breakpoints instead of live hivemind count.
 *
 * <p>Kept as its own class rather than folded into {@link
 * com.visteus.sporebreach.setup.SporeSpawnSuppression}: that class is specifically about vanilla
 * spawn-list suppression, a distinct concern from neutralizing this particular base-mod system.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ProtoWorldModifierSuppression {

    private static final Logger LOGGER = LogUtils.getLogger();

    private ProtoWorldModifierSuppression() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        if (!SporeBreachCommonConfig.NEUTRALIZE_PROTO_WORLD_MODIFIER.get()) {
            return;
        }

        SConfig.SERVER.proto_spawn_world_mod.set(Integer.MAX_VALUE);
        LOGGER.info(
                "sporebreach: neutralized Spore's Proto World Modifier "
                        + "(neutralizeProtoWorldModifier=true) - World Corruption now drives those effects instead"
        );
    }
}
