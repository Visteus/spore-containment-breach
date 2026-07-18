package com.visteus.sporebreach.spawning;

import com.Harbinger.Spore.Sentities.BaseEntities.Calamity;
import com.Harbinger.Spore.Sentities.Organoids.Mound;
import com.Harbinger.Spore.Sentities.Organoids.Proto;
import com.Harbinger.Spore.Sentities.Organoids.Womb;
import com.mojang.logging.LogUtils;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import com.visteus.sporebreach.tracking.OrganoidRegistry;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

/**
 * New Goal #1 behavior: Proto-Hiveminds periodically send a raid group to a nearby structure or
 * player, independent of Proto's existing reactive Signal-driven summonMob(). This does not
 * touch or replace that existing mechanism.
 * <p>
 * Note on Spore's "hivemind"/"decision"/"member" reinforcement-learning NBT convention (used by
 * Proto.summonMob for its own team_1..4 rosters): raiders here are intentionally NOT tagged with
 * it. Those keys are indices into Proto's own decide()/getDecisionList() system, and
 * Organoid.adjustWeightsForDecision() indexes Proto's internal weights array with
 * {@code decision * 4} and no bounds check. Since protoRaidSpawnPool is an independently
 * configured pool with no corresponding "decision index," faking a value there risks an
 * ArrayIndexOutOfBoundsException the first time a raider we spawned is judged. Raiders are
 * instead tagged with our own namespaced {@code sporebreach_raid_by} key for bookkeeping.
 */
public final class ProtoRaidDirector {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String COOLDOWN_KEY = "sporebreach_raid_cd";
    private static final String RAID_BY_KEY = "sporebreach_raid_by";

    private ProtoRaidDirector() {
    }

    public static boolean isEligible(Proto proto) {
        if (!(proto.level() instanceof ServerLevel level)) {
            return false;
        }
        if (SpawnAnchors.isWithinProtectedSpawnRadius(level, proto.getOnPos())) {
            return false;
        }
        long readyAt = proto.getPersistentData().getLong(COOLDOWN_KEY);
        return level.getGameTime() >= readyAt;
    }

    public static void attempt(Proto proto) {
        if (!(proto.level() instanceof ServerLevel level)) {
            return;
        }

        // Reroll the cooldown unconditionally so a Proto with no reachable target doesn't retry
        // every coordinator cycle.
        rerollCooldown(proto, level);

        int searchRadius = SporeBreachServerConfig.PROTO_RAID_SEARCH_RADIUS.get();
        Optional<BlockPos> target = findRaidTarget(level, proto, searchRadius);
        if (target.isEmpty() || SpawnAnchors.isWithinProtectedSpawnRadius(level, target.get())) {
            return;
        }
        BlockPos targetPos = target.get();

        SpawnPool troops = SpawnPool.fromConfig(SporeBreachServerConfig.PROTO_RAID_SPAWN_POOL.get(), true, "protoRaidSpawnPool");
        boolean calamityAllowed = CorruptionGate.isCalamitySpawningAllowed(level, targetPos);
        SpawnPool calamities = calamityAllowed
                ? SpawnPool.fromConfig(SporeBreachServerConfig.PROTO_CALAMITY_SPAWN_POOL.get(), false, "protoCalamitySpawnPool")
                : null;

        CorruptionGate.GroupSizeRange groupSize = CorruptionGate.getRaidGroupSizeRange(level);
        int min = Math.min(groupSize.min(), groupSize.max());
        int max = Math.max(groupSize.min(), groupSize.max());
        int count = min + proto.getRandom().nextInt(max - min + 1);

        boolean spawnedAny = false;
        for (int i = 0; i < count; i++) {
            SpawnPool pool = (calamities != null && !calamities.isEmpty() && proto.getRandom().nextBoolean()) ? calamities : troops;
            if (pool.isEmpty()) {
                pool = troops;
            }
            if (pool.isEmpty()) {
                break;
            }
            Optional<SpawnPoolEntry> picked = pool.pickWeighted(proto.getRandom());
            if (picked.isEmpty()) {
                continue;
            }
            boolean allowCalamityHere = pool == calamities;
            if (spawnRaider(level, proto, picked.get(), targetPos, searchRadius, allowCalamityHere)) {
                spawnedAny = true;
            }
        }

        if (!spawnedAny) {
            LOGGER.debug("spore_containment_breach: Proto raid at {} found a target but placed no raiders", targetPos);
        }
    }

    private static void rerollCooldown(Proto proto, ServerLevel level) {
        int minTicks = SporeBreachServerConfig.PROTO_RAID_COOLDOWN_MIN_TICKS.get();
        int maxTicks = Math.max(minTicks, SporeBreachServerConfig.PROTO_RAID_COOLDOWN_MAX_TICKS.get());
        double mode = (minTicks + maxTicks) / 2.0;
        double deviation = (maxTicks - minTicks) / 2.0;
        long cooldown = Math.round(proto.getRandom().triangle(mode, deviation));
        proto.getPersistentData().putLong(COOLDOWN_KEY, level.getGameTime() + cooldown);
    }

    private static Optional<BlockPos> findRaidTarget(ServerLevel level, Proto proto, int searchRadius) {
        double bestDistSq = (double) searchRadius * searchRadius;
        BlockPos best = null;

        for (Mound mound : OrganoidRegistry.get(level)) {
            double distSq = mound.distanceToSqr(proto);
            if (distSq <= bestDistSq) {
                bestDistSq = distSq;
                best = SpawnAnchors.resolveAnchor(mound);
            }
        }
        if (best != null) {
            return Optional.of(best);
        }

        Player nearestPlayer = level.getNearestPlayer(proto, searchRadius);
        if (nearestPlayer != null) {
            return Optional.of(nearestPlayer.getOnPos());
        }
        return Optional.empty();
    }

    private static boolean spawnRaider(
            ServerLevel level, Proto proto, SpawnPoolEntry entry, BlockPos target, int searchRadius, boolean calamityAllowedHere
    ) {
        Optional<BlockPos> position = SpawnAnchors.findGroundPosition(level, target, searchRadius, proto.getRandom());
        if (position.isEmpty()) {
            return false;
        }

        Entity spawned = entry.type().create(level);
        if (spawned == null) {
            return false;
        }

        boolean forbidden = spawned instanceof Womb || (!calamityAllowedHere && spawned instanceof Calamity);
        if (forbidden) {
            LOGGER.error(
                    "spore_containment_breach: refused to let a Proto raid spawn a forbidden entity type ({}) - "
                            + "this indicates a bug in spawn pool filtering, please report it",
                    entry.type()
            );
            spawned.discard();
            return false;
        }

        BlockPos pos = position.get();
        RandomSource random = proto.getRandom();
        spawned.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
        spawned.getPersistentData().putUUID(RAID_BY_KEY, proto.getUUID());
        return level.addFreshEntity(spawned);
    }
}
