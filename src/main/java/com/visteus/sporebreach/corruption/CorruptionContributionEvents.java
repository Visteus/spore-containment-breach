package com.visteus.sporebreach.corruption;

import com.Harbinger.Spore.Sentities.Organoids.Mound;
import com.Harbinger.Spore.Sentities.Organoids.Proto;
import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Awards World Corruption when a new Mound/Proto-Hivemind is created, and refunds exactly what
 * that organoid contributed (creation + any age-ups) when it's killed. Age-up contributions
 * themselves are handled by {@link CorruptionAgeDirector} - this class only handles the
 * creation/death edges.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class CorruptionContributionEvents {

    private static final String COUNTED_KEY = "sporebreach_corruption_counted";
    static final String CONTRIBUTED_KEY = "sporebreach_corruption_contributed";
    static final String LAST_MOUND_AGE_KEY = "sporebreach_corruption_last_age";
    static final String LAST_PROTO_AGE_KEY = "sporebreach_corruption_last_proto_age";

    private CorruptionContributionEvents() {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!SporeBreachServerConfig.CORRUPTION_ENABLED.get() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (event.getEntity() instanceof Mound mound && !mound.getPersistentData().getBoolean(COUNTED_KEY)) {
            contribute(level, mound, SporeBreachServerConfig.CORRUPTION_PER_MOUND_CREATED.get());
            mound.getPersistentData().putInt(LAST_MOUND_AGE_KEY, mound.getAge());
        } else if (event.getEntity() instanceof Proto proto && !proto.getPersistentData().getBoolean(COUNTED_KEY)) {
            contribute(level, proto, SporeBreachServerConfig.CORRUPTION_PER_PROTO_CREATED.get());
            proto.getPersistentData().putInt(LAST_PROTO_AGE_KEY, 0);
        }
    }

    private static void contribute(ServerLevel level, Entity entity, int amount) {
        CorruptionData.add(level, amount);
        CompoundTag data = entity.getPersistentData();
        data.putInt(CONTRIBUTED_KEY, data.getInt(CONTRIBUTED_KEY) + amount);
        data.putBoolean(COUNTED_KEY, true);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!SporeBreachServerConfig.CORRUPTION_ENABLED.get() || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getEntity() instanceof Mound) && !(event.getEntity() instanceof Proto)) {
            return;
        }

        int contributed = event.getEntity().getPersistentData().getInt(CONTRIBUTED_KEY);
        if (contributed > 0) {
            CorruptionData.subtract(level, contributed);
        }
    }
}
