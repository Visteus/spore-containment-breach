package com.visteus.sporebreach.corruption;

import com.Harbinger.Spore.Sentities.BaseEntities.UtilityEntity;
import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Scales health, attack damage, armor, and armor toughness for every combat-capable spore mob
 * ({@link UtilityEntity}) by the dimension's World Corruption fraction ({@link
 * CorruptionTier#fraction(ServerLevel)}) - the same linear-fraction basis {@link
 * com.visteus.sporebreach.spawning.CorruptionGate#getRaidGroupSizeRange} uses for raid group size.
 * Locked in once per entity at join/first-load, mirroring {@link CorruptionEffects}.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class CorruptionMobStatScaling {

    private static final String APPLIED_KEY = "sporebreach_corruption_stat_scaling_applied";

    private static final ResourceLocation HEALTH_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(SporeContainmentBreach.MODID, "corruption_health_scaling");
    private static final ResourceLocation DAMAGE_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(SporeContainmentBreach.MODID, "corruption_damage_scaling");
    private static final ResourceLocation ARMOR_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(SporeContainmentBreach.MODID, "corruption_armor_scaling");
    private static final ResourceLocation ARMOR_TOUGHNESS_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(SporeContainmentBreach.MODID, "corruption_armor_toughness_scaling");

    private CorruptionMobStatScaling() {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!SporeBreachServerConfig.CORRUPTION_ENABLED.get()
                || !SporeBreachServerConfig.CORRUPTION_MOB_SCALING_ENABLED.get()
                || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof UtilityEntity mob)) {
            return;
        }

        CompoundTag data = mob.getPersistentData();
        if (data.getBoolean(APPLIED_KEY)) {
            return;
        }
        data.putBoolean(APPLIED_KEY, true);

        double fraction = CorruptionTier.fraction(level);
        if (fraction <= 0.0) {
            return;
        }

        applyMultiplier(mob, Attributes.MAX_HEALTH, HEALTH_MODIFIER_ID,
                SporeBreachServerConfig.CORRUPTION_MOB_HEALTH_MAX_MULTIPLIER.get(), fraction);
        applyMultiplier(mob, Attributes.ATTACK_DAMAGE, DAMAGE_MODIFIER_ID,
                SporeBreachServerConfig.CORRUPTION_MOB_DAMAGE_MAX_MULTIPLIER.get(), fraction);
        applyFlatBonus(mob, Attributes.ARMOR, ARMOR_MODIFIER_ID,
                SporeBreachServerConfig.CORRUPTION_MOB_ARMOR_MAX_BONUS.get(), fraction);
        applyFlatBonus(mob, Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS_MODIFIER_ID,
                SporeBreachServerConfig.CORRUPTION_MOB_ARMOR_TOUGHNESS_MAX_BONUS.get(), fraction);

        mob.setHealth(mob.getMaxHealth());
    }

    private static void applyMultiplier(LivingEntity mob, Holder<Attribute> attribute, ResourceLocation id,
            double maxMultiplier, double fraction) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        double amount = (maxMultiplier - 1.0) * fraction;
        instance.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    private static void applyFlatBonus(LivingEntity mob, Holder<Attribute> attribute, ResourceLocation id,
            double maxBonus, double fraction) {
        AttributeInstance instance = mob.getAttribute(attribute);
        double amount = maxBonus * fraction;
        if (instance == null || amount <= 0.0) {
            return;
        }
        instance.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
    }
}
