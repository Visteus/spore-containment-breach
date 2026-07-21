package com.visteus.sporebreach.spawning;

import com.Harbinger.Spore.core.SConfig;
import com.mojang.logging.LogUtils;
import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.config.SporeBreachCommonConfig;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

/**
 * Disables Spore's own {@code Proto.scanForHosts()} Vigil-ambush raid - gated by {@code
 * proto_raid}/{@code proto_raid_chance}, it reactively teleport-spawns a Vigil next to a
 * Player/sapient target already inside the Proto's scan box - by forcing {@code proto_raid}
 * false. {@link ProtoRaidDirector} now owns "a Proto-Hivemind sends something at a target" for
 * this mod; left enabled, both would fire independently and double up on the same intent with
 * two different-feeling mechanics.
 *
 * <p>{@code proto_raid} is read live via {@code .get()} inside {@code scanForHosts()} on every
 * scan, so unlike {@link com.visteus.sporebreach.setup.SporeSpawnSuppression} there's no bake-in
 * timing hazard here - {@code FMLCommonSetupEvent} is used anyway, matching this project's other
 * base-Spore-config-suppression classes ({@link
 * com.visteus.sporebreach.corruption.ProtoWorldModifierSuppression}) for consistency.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ProtoVigilRaidSuppression {

    private static final Logger LOGGER = LogUtils.getLogger();

    private ProtoVigilRaidSuppression() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        if (!SporeBreachCommonConfig.SUPPRESS_PROTO_VIGIL_RAID.get()) {
            return;
        }

        SConfig.SERVER.proto_raid.set(false);
        LOGGER.info(
                "sporebreach: disabled Spore's own Proto Vigil-ambush raid "
                        + "(suppressProtoVigilRaid=true) - ProtoRaidDirector now owns raid dispatch instead"
        );
    }
}
