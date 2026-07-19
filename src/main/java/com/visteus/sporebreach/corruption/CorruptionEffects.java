package com.visteus.sporebreach.corruption;

import com.Harbinger.Spore.Sentities.BaseEntities.Calamity;
import com.Harbinger.Spore.Sentities.BaseEntities.EvolvedInfected;
import com.Harbinger.Spore.Sentities.BaseEntities.Infected;
import com.Harbinger.Spore.Sentities.EvolvingInfected;
import com.Harbinger.Spore.Sentities.Organoids.Mound;
import com.Harbinger.Spore.core.SConfig;
import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Reimplements the four effects Spore's own (now-neutered, see {@link
 * ProtoWorldModifierSuppression}) Proto World Modifier used to bundle behind one threshold -
 * instant evolution, gear enchanting, the Linked flag, and Calamity Adaptations - driven instead
 * by this mod's own World Corruption breakpoints. {@code Infected.setDefaultLinkage} bundled the
 * first three behind a single check; since they now unlock at three different breakpoints, this
 * calls the same underlying public methods directly instead of that bundled method.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class CorruptionEffects {

    private static final String APPLIED_KEY = "sporebreach_corruption_effects_applied";

    private CorruptionEffects() {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!SporeBreachServerConfig.CORRUPTION_ENABLED.get() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        CompoundTag data = event.getEntity().getPersistentData();
        if (data.getBoolean(APPLIED_KEY)) {
            return;
        }
        data.putBoolean(APPLIED_KEY, true);

        if (event.getEntity() instanceof Infected infected) {
            applyToInfected(level, infected);
        }
        if (event.getEntity() instanceof Mound mound && CorruptionTier.isLinkedSpawnAllowed(level)) {
            mound.setLinked(true);
        }
        if (event.getEntity() instanceof Calamity calamity && CorruptionTier.isAdaptationAllowed(level)) {
            calamity.ActivateAdaptation();
        }
    }

    private static void applyToInfected(ServerLevel level, Infected infected) {
        if (CorruptionTier.isInstantEvolutionAllowed(level)) {
            rollInstantEvolution(infected);
        }
        if (CorruptionTier.isCalamitySpawningAllowed(level)) {
            infected.enchantEquipment(infected);
        }
        if (CorruptionTier.isLinkedSpawnAllowed(level)) {
            infected.setLinked(true);
        }
    }

    private static void rollInstantEvolution(Infected infected) {
        if (Math.random() >= 0.3 || !(infected instanceof EvolvingInfected)) {
            return;
        }
        if (infected instanceof EvolvedInfected) {
            infected.setEvoPoints(infected.getEvoPoints() + SConfig.SERVER.min_kills_hyper.get());
            infected.setEvolution(SConfig.SERVER.evolution_age_hyper.get());
        } else {
            infected.setEvoPoints(infected.getEvoPoints() + SConfig.SERVER.min_kills.get());
            infected.setEvolution(SConfig.SERVER.evolution_age_human.get());
        }
    }
}
