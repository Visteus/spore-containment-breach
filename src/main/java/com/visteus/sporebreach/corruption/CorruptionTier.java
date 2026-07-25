package com.visteus.sporebreach.corruption;

import com.visteus.sporebreach.config.SporeBreachServerConfig;
import net.minecraft.server.level.ServerLevel;

/**
 * Single source of truth for which of Goal #7's six World Corruption stages a dimension has
 * reached. Pure lookups against {@link CorruptionData} and the {@code corruption} breakpoint
 * config - no state of its own.
 */
public final class CorruptionTier {

    private CorruptionTier() {
    }

    public static int value(ServerLevel level) {
        return CorruptionData.get(level);
    }

    private static int breakpointValue(ServerLevel level) {
        return SporeBreachServerConfig.CORRUPTION_USE_MAX_FOR_BREAKPOINTS.get()
                ? CorruptionData.getMax(level)
                : CorruptionData.get(level);
    }

    private static double fractionOf(int rawValue) {
        int cap = SporeBreachServerConfig.CORRUPTION_CAP.get();
        return cap > 0 ? Math.min(1.0, (double) rawValue / cap) : 0.0;
    }

    /**
     * Raid group size scaling basis - see {@link SporeBreachServerConfig#CORRUPTION_USE_MAX_FOR_RAID_SIZE}.
     */
    public static double fractionForRaidSize(ServerLevel level) {
        int value = SporeBreachServerConfig.CORRUPTION_USE_MAX_FOR_RAID_SIZE.get()
                ? CorruptionData.getMax(level)
                : CorruptionData.get(level);
        return fractionOf(value);
    }

    /**
     * Mob stat scaling basis - see {@link SporeBreachServerConfig#CORRUPTION_USE_MAX_FOR_MOB_SCALING}.
     */
    public static double fractionForMobScaling(ServerLevel level) {
        int value = SporeBreachServerConfig.CORRUPTION_USE_MAX_FOR_MOB_SCALING.get()
                ? CorruptionData.getMax(level)
                : CorruptionData.get(level);
        return fractionOf(value);
    }

    /**
     * Stage 1: Proto-Hivemind raids on players.
     */
    public static boolean areRaidsAllowed(ServerLevel level) {
        return breakpointValue(level) >= SporeBreachServerConfig.CORRUPTION_BREAKPOINT_RAIDS.get();
    }

    /**
     * Stage 2: newly spawned Infected may instantly evolve.
     */
    public static boolean isInstantEvolutionAllowed(ServerLevel level) {
        return breakpointValue(level) >= SporeBreachServerConfig.CORRUPTION_BREAKPOINT_INSTANT_EVOLUTION.get();
    }

    /**
     * Stage 3: Calamities/Wombs may be created or spawned, and newly spawned Infected gear gets
     * at least 1 enchantment per piece.
     */
    public static boolean isCalamitySpawningAllowed(ServerLevel level) {
        return breakpointValue(level) >= SporeBreachServerConfig.CORRUPTION_BREAKPOINT_CALAMITY_WOMB.get();
    }

    /**
     * Stage 4: Proto raids may include Calamities.
     */
    public static boolean isCalamityRaidAllowed(ServerLevel level) {
        return breakpointValue(level) >= SporeBreachServerConfig.CORRUPTION_BREAKPOINT_CALAMITY_RAIDS.get();
    }

    /**
     * Stage 5: spawned Calamities activate their Adaptation.
     */
    public static boolean isAdaptationAllowed(ServerLevel level) {
        return breakpointValue(level) >= SporeBreachServerConfig.CORRUPTION_BREAKPOINT_ADAPTATIONS.get();
    }

    /**
     * Stage 6: all applicable spore mobs spawn as Linked.
     */
    public static boolean isLinkedSpawnAllowed(ServerLevel level) {
        return breakpointValue(level) >= SporeBreachServerConfig.CORRUPTION_BREAKPOINT_LINKED_SPAWNS.get();
    }
}
