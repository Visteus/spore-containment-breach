package com.visteus.sporebreach;

import com.visteus.sporebreach.config.SporeBreachClientConfig;
import com.visteus.sporebreach.config.SporeBreachCommonConfig;
import com.visteus.sporebreach.config.SporeBreachOverridesConfig;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;

@Mod(SporeContainmentBreach.MODID)
public class SporeContainmentBreach {

    public static final String MODID = "sporebreach";

    public SporeContainmentBreach(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(Type.COMMON, SporeBreachCommonConfig.SPEC);
        modContainer.registerConfig(Type.SERVER, SporeBreachServerConfig.SPEC);
        modContainer.registerConfig(Type.CLIENT, SporeBreachClientConfig.SPEC);
        modContainer.registerConfig(Type.COMMON, SporeBreachOverridesConfig.SPEC, "sporebreach-overrides-common.toml");
    }
}
