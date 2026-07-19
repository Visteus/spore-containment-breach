package com.visteus.sporebreach.corruption;

import com.Harbinger.Spore.Sentities.Organoids.Mound;
import com.Harbinger.Spore.Sentities.Organoids.Proto;
import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import com.visteus.sporebreach.tracking.OrganoidRegistry;
import com.visteus.sporebreach.tracking.ProtoAgeTracker;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Periodically checks live Mounds/Protos for age increases and awards World Corruption for each
 * level gained, modeled on {@link com.visteus.sporebreach.genesis.MoundGenesisDirector}'s
 * tick-interval counter pattern. Mound's age is base Spore's own real field; Proto's is this
 * mod's own tracked age from {@link ProtoAgeTracker} - either way, this class only reads the
 * age and tracks its own "last seen" cursor, never computing age itself.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class CorruptionAgeDirector {

    private static long tickCounter;

    private CorruptionAgeDirector() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!SporeBreachServerConfig.CORRUPTION_ENABLED.get()) {
            return;
        }
        tickCounter++;
        int interval = SporeBreachServerConfig.CORRUPTION_AGE_SCAN_INTERVAL_TICKS.get();
        if (interval <= 0 || tickCounter % interval != 0) {
            return;
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            scanMounds(level);
            scanProtos(level);
        }
    }

    private static void scanMounds(ServerLevel level) {
        int perAgeUp = SporeBreachServerConfig.CORRUPTION_PER_MOUND_AGE_UP.get();
        for (Mound mound : OrganoidRegistry.get(level)) {
            CompoundTag data = mound.getPersistentData();
            int lastAge = data.getInt(CorruptionContributionEvents.LAST_MOUND_AGE_KEY);
            int currentAge = mound.getAge();
            if (currentAge <= lastAge) {
                continue;
            }
            awardAgeUp(level, data, (currentAge - lastAge) * perAgeUp);
            data.putInt(CorruptionContributionEvents.LAST_MOUND_AGE_KEY, currentAge);
        }
    }

    private static void scanProtos(ServerLevel level) {
        int perAgeUp = SporeBreachServerConfig.CORRUPTION_PER_PROTO_AGE_UP.get();
        long gameTime = level.getGameTime();
        for (Proto proto : OrganoidRegistry.getProtos(level)) {
            CompoundTag data = proto.getPersistentData();
            int lastAge = data.getInt(CorruptionContributionEvents.LAST_PROTO_AGE_KEY);
            int currentAge = ProtoAgeTracker.getAge(proto, gameTime);
            if (currentAge <= lastAge) {
                continue;
            }
            awardAgeUp(level, data, (currentAge - lastAge) * perAgeUp);
            data.putInt(CorruptionContributionEvents.LAST_PROTO_AGE_KEY, currentAge);
        }
    }

    private static void awardAgeUp(ServerLevel level, CompoundTag persistentData, int amount) {
        CorruptionData.add(level, amount);
        persistentData.putInt(
                CorruptionContributionEvents.CONTRIBUTED_KEY,
                persistentData.getInt(CorruptionContributionEvents.CONTRIBUTED_KEY) + amount
        );
    }
}
