package com.visteus.sporebreach;

import com.visteus.sporebreach.config.SporeBreachClientConfig;
import com.visteus.sporebreach.config.SporeBreachCommonConfig;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import com.visteus.sporebreach.setup.SporeSpawnSuppression;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;

@Mod(SporeContainmentBreach.MODID)
public class SporeContainmentBreach {

    public static final String MODID = "spore_containment_breach";

    public SporeContainmentBreach(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(Type.COMMON, SporeBreachCommonConfig.SPEC);
        modContainer.registerConfig(Type.SERVER, SporeBreachServerConfig.SPEC);
        modContainer.registerConfig(Type.CLIENT, SporeBreachClientConfig.SPEC);

        modBus.addListener(SporeSpawnSuppression::onCommonSetup);
    }
}
