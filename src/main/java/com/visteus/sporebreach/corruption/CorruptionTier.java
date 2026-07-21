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

    /**
     * Raw corruption value as a fraction of the cap - 0.0 at zero corruption, 1.0 at
     * {@code CORRUPTION_CAP}. Shared basis for anything that scales linearly with corruption
     * (raid group size, mob stat scaling, etc).
     */
    public static double fraction(ServerLevel level) {
        int cap = SporeBreachServerConfig.CORRUPTION_CAP.get();
        return cap > 0 ? Math.min(1.0, (double) value(level) / cap) : 0.0;
    }

    /**
     * Stage 1: Proto-Hivemind raids on players.
     */
    public static boolean areRaidsAllowed(ServerLevel level) {
        return value(level) >= SporeBreachServerConfig.CORRUPTION_BREAKPOINT_RAIDS.get();
    }

    /**
     * Stage 2: newly spawned Infected may instantly evolve.
     */
    public static boolean isInstantEvolutionAllowed(ServerLevel level) {
        return value(level) >= SporeBreachServerConfig.CORRUPTION_BREAKPOINT_INSTANT_EVOLUTION.get();
    }

    /**
     * Stage 3: Calamities/Wombs may be created or spawned, and newly spawned Infected gear gets
     * at least 1 enchantment per piece.
     */
    public static boolean isCalamitySpawningAllowed(ServerLevel level) {
        return value(level) >= SporeBreachServerConfig.CORRUPTION_BREAKPOINT_CALAMITY_WOMB.get();
    }

    /**
     * Stage 4: Proto raids may include Calamities.
     */
    public static boolean isCalamityRaidAllowed(ServerLevel level) {
        return value(level) >= SporeBreachServerConfig.CORRUPTION_BREAKPOINT_CALAMITY_RAIDS.get();
    }

    /**
     * Stage 5: spawned Calamities activate their Adaptation.
     */
    public static boolean isAdaptationAllowed(ServerLevel level) {
        return value(level) >= SporeBreachServerConfig.CORRUPTION_BREAKPOINT_ADAPTATIONS.get();
    }

    /**
     * Stage 6: all applicable spore mobs spawn as Linked.
     */
    public static boolean isLinkedSpawnAllowed(ServerLevel level) {
        return value(level) >= SporeBreachServerConfig.CORRUPTION_BREAKPOINT_LINKED_SPAWNS.get();
    }
}
