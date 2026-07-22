package com.visteus.sporebreach.spawning;

import com.Harbinger.Spore.Sentities.Organoids.Mound;
import com.Harbinger.Spore.Sentities.Organoids.Proto;
import com.Harbinger.Spore.core.Sentities;
import com.mojang.logging.LogUtils;
import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import com.visteus.sporebreach.structuregrowth.OrganoidStructurePlacer;
import com.visteus.sporebreach.tracking.OrganoidRegistry;
import com.visteus.sporebreach.tracking.ProtoAgeTracker;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

/**
 * Goal #1/#3 "outpost seeding": once a Proto-Hivemind reaches a configurable age, it periodically
 * rolls to place a new Mound at a random surface point some distance away from itself, extending
 * the infected area over time. Independent of {@code OrganoidSpawnDirector}'s closest-to-player
 * budget dispatch - that system exists to bound cost when multiple organoids compete for the same
 * per-cycle combat-spawn slot (raids/defenders); outpost seeding has no such contention, since
 * each Proto acts purely against its own cooldown, so it gets its own lightweight tick subscriber
 * instead.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class ProtoOutpostSeedDirector {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String COOLDOWN_KEY = "sporebreach_outpost_seed_cd";
    private static final int MAX_SITE_ATTEMPTS = 20;

    private static long tickCounter;

    private ProtoOutpostSeedDirector() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!event.getServer().tickRateManager().runsNormally()) {
            return;
        }
        if (!SporeBreachServerConfig.PROTO_OUTPOST_SEED_ENABLED.get()) {
            return;
        }
        tickCounter++;
        int interval = SporeBreachServerConfig.DIRECTOR_TICK_INTERVAL_TICKS.get();
        if (interval <= 0 || tickCounter % interval != 0) {
            return;
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (Proto proto : OrganoidRegistry.getProtos(level)) {
                if (checkEligibility(level, proto) == OrganoidEligibility.ELIGIBLE) {
                    attempt(level, proto);
                }
            }
        }
    }

    public static OrganoidEligibility checkEligibility(ServerLevel level, Proto proto) {
        if (SpawnAnchors.isWithinProtectedSpawnRadius(level, proto.getOnPos())) {
            return OrganoidEligibility.PROTECTED_SPAWN_RADIUS;
        }
        int minAge = SporeBreachServerConfig.PROTO_OUTPOST_SEED_MIN_AGE.get();
        if (ProtoAgeTracker.getAge(proto, level.getGameTime()) < minAge) {
            return OrganoidEligibility.TOO_YOUNG;
        }
        long readyAt = proto.getPersistentData().getLong(COOLDOWN_KEY);
        if (level.getGameTime() < readyAt) {
            return OrganoidEligibility.ON_COOLDOWN;
        }
        return OrganoidEligibility.ELIGIBLE;
    }

    public static void attempt(ServerLevel level, Proto proto) {
        // Reroll unconditionally so a Proto with no valid site this cycle doesn't retry every
        // director tick - only on its next scheduled roll.
        int cooldownTicks = SporeBreachServerConfig.PROTO_OUTPOST_SEED_COOLDOWN_TICKS.get();
        proto.getPersistentData().putLong(COOLDOWN_KEY, level.getGameTime() + cooldownTicks);

        Optional<BlockPos> site = pickOutpostSite(level, proto);
        if (site.isEmpty()) {
            return;
        }
        BlockPos pos = site.get();

        int checkRadius = SporeBreachServerConfig.PROTO_OUTPOST_SEED_MOUND_CHECK_RADIUS.get();
        int moundLimit = SporeBreachServerConfig.PROTO_OUTPOST_SEED_MOUND_LIMIT.get();
        if (countNearbyMounds(level, pos, checkRadius) >= moundLimit) {
            return;
        }

        Mound mound = Sentities.MOUND.get().create(level);
        if (mound == null) {
            return;
        }
        RandomSource random = proto.getRandom();
        mound.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
        if (level.addFreshEntity(mound)) {
            LOGGER.debug("sporebreach: Proto {} seeded an outpost Mound at {}", proto.getUUID(), pos);
        }
    }

    private static Optional<BlockPos> pickOutpostSite(ServerLevel level, Proto proto) {
        RandomSource random = proto.getRandom();
        int minDistConfig = SporeBreachServerConfig.PROTO_OUTPOST_SEED_MIN_DISTANCE.get();
        int maxDistConfig = SporeBreachServerConfig.PROTO_OUTPOST_SEED_MAX_DISTANCE.get();
        int minDist = Math.min(minDistConfig, maxDistConfig);
        int maxDist = Math.max(minDistConfig, maxDistConfig);
        BlockPos origin = proto.getOnPos();

        for (int attempt = 0; attempt < MAX_SITE_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            int dist = minDist + random.nextInt(maxDist - minDist + 1);
            int x = origin.getX() + (int) Math.round(Math.cos(angle) * dist);
            int z = origin.getZ() + (int) Math.round(Math.sin(angle) * dist);
            int y = OrganoidStructurePlacer.surfaceHeight(level, x, z);
            BlockPos candidate = new BlockPos(x, y, z);

            if (SpawnAnchors.isWithinProtectedSpawnRadius(level, candidate)) {
                continue;
            }
            if (OrganoidStructurePlacer.isNaturalGround(level, candidate.below())) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private static long countNearbyMounds(ServerLevel level, BlockPos site, int radius) {
        double radiusSq = (double) radius * radius;
        double x = site.getX() + 0.5;
        double y = site.getY() + 0.5;
        double z = site.getZ() + 0.5;
        return OrganoidRegistry.get(level).stream()
                .filter(mound -> mound.distanceToSqr(x, y, z) <= radiusSq)
                .count();
    }
}
