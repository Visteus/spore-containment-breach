package com.visteus.sporebreach.spawning;

import com.Harbinger.Spore.Sentities.BaseEntities.Calamity;
import com.Harbinger.Spore.Sentities.BaseEntities.Infected;
import com.Harbinger.Spore.Sentities.Organoids.Mound;
import com.Harbinger.Spore.Sentities.Organoids.Proto;
import com.Harbinger.Spore.Sentities.Organoids.Womb;
import com.Harbinger.Spore.Sentities.Utility.Vanguard;
import com.mojang.logging.LogUtils;
import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.chunkloading.ChunkloadManager;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import com.visteus.sporebreach.tracking.OrganoidRegistry;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
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
    private static final int MAX_PICK_ATTEMPTS = 5;

    private ProtoRaidDirector() {
    }

    public static OrganoidEligibility checkEligibility(Proto proto) {
        if (!(proto.level() instanceof ServerLevel level)) {
            return OrganoidEligibility.INVALID_LEVEL;
        }
        if (SpawnAnchors.isWithinProtectedSpawnRadius(level, proto.getOnPos())) {
            return OrganoidEligibility.PROTECTED_SPAWN_RADIUS;
        }
        long readyAt = proto.getPersistentData().getLong(COOLDOWN_KEY);
        if (level.getGameTime() < readyAt) {
            return OrganoidEligibility.ON_COOLDOWN;
        }
        if (!CorruptionGate.areRaidsAllowed(level)) {
            return OrganoidEligibility.CORRUPTION_TOO_LOW;
        }
        return OrganoidEligibility.ELIGIBLE;
    }

    public static boolean isEligible(Proto proto) {
        return checkEligibility(proto) == OrganoidEligibility.ELIGIBLE;
    }

    public static void attempt(Proto proto) {
        if (!(proto.level() instanceof ServerLevel level)) {
            return;
        }

        // Reroll the cooldown unconditionally so a Proto with no reachable target doesn't retry
        // every director cycle.
        rerollCooldown(proto, level);

        int searchRadius = SporeBreachServerConfig.PROTO_RAID_SEARCH_RADIUS.get();
        Optional<BlockPos> target = findRaidTarget(level, proto, searchRadius);
        if (target.isEmpty()
                || SpawnAnchors.isWithinProtectedSpawnRadius(level, target.get())
                || targetsForbiddenStructure(level, target.get())) {
            return;
        }
        BlockPos targetPos = target.get();

        SpawnPool troops = SpawnPool.fromConfig(SporeBreachServerConfig.PROTO_RAID_SPAWN_POOL.get(), true, "protoRaidSpawnPool");
        boolean calamityAllowed = CorruptionGate.isCalamityRaidAllowed(level, targetPos);
        SpawnPool calamities = calamityAllowed
                ? SpawnPool.fromConfig(SporeBreachServerConfig.PROTO_CALAMITY_SPAWN_POOL.get(), false, "protoCalamitySpawnPool")
                : null;

        CorruptionGate.GroupSizeRange groupSize = CorruptionGate.getRaidGroupSizeRange(level);
        int min = Math.min(groupSize.min(), groupSize.max());
        int max = Math.max(groupSize.min(), groupSize.max());
        int count = min + proto.getRandom().nextInt(max - min + 1);

        List<UUID> raiderIds = new ArrayList<>();
        Map<EntityType<?>, Integer> perTypeCounts = new HashMap<>();
        for (int i = 0; i < count; i++) {
            SpawnPool pool = (calamities != null && !calamities.isEmpty() && proto.getRandom().nextBoolean()) ? calamities : troops;
            if (pool.isEmpty()) {
                pool = troops;
            }
            if (pool.isEmpty()) {
                break;
            }
            Optional<SpawnPoolEntry> picked = pickWithinCap(pool, proto.getRandom(), perTypeCounts);
            if (picked.isEmpty()) {
                continue;
            }
            SpawnPoolEntry entry = picked.get();
            boolean allowCalamityHere = pool == calamities;
            spawnRaider(level, proto, entry, targetPos, searchRadius, allowCalamityHere).ifPresent(id -> {
                raiderIds.add(id);
                perTypeCounts.merge(entry.type(), 1, Integer::sum);
            });
        }

        if (raiderIds.isEmpty()) {
            LOGGER.debug("spore_containment_breach: Proto raid at {} found a target but placed no raiders", targetPos);
        } else {
            RaidRegistry.register(
                    level, new RaidRegistry.RaidRecord(UUID.randomUUID(), proto.getUUID(), targetPos, level.getGameTime(), raiderIds)
            );
            LOGGER.info(
                    "spore_containment_breach: Proto {} at {} dispatched a raid of {} ({}) targeting {}",
                    proto.getUUID(), proto.blockPosition(), raiderIds.size(), summarizeCounts(perTypeCounts), targetPos
            );
        }
    }

    private static String summarizeCounts(Map<EntityType<?>, Integer> perTypeCounts) {
        StringBuilder summary = new StringBuilder();
        for (Map.Entry<EntityType<?>, Integer> entry : perTypeCounts.entrySet()) {
            if (summary.length() > 0) {
                summary.append(", ");
            }
            summary.append(entry.getValue()).append("x ").append(EntityType.getKey(entry.getKey()));
        }
        return summary.toString();
    }

    /**
     * Vetoes raid targets that fall within a naturally-generated structure from either {@code
     * spore} (e.g. its lab/church/military_camp ruins) or this mod's own namespace, so raids don't
     * path raiders into a preexisting structure that a player might be actively exploring/looting.
     * Reads structure references already cached on the target's loaded chunk, same idiom as {@link
     * com.visteus.sporebreach.genesis.MoundGenesisDirector#scanNear}.
     */
    private static boolean targetsForbiddenStructure(ServerLevel level, BlockPos pos) {
        Map<Structure, LongSet> nearby = level.structureManager().getAllStructuresAt(pos);
        if (nearby.isEmpty()) {
            return false;
        }
        for (Structure structure : nearby.keySet()) {
            Holder<Structure> holder = level.registryAccess().registryOrThrow(Registries.STRUCTURE).wrapAsHolder(structure);
            Optional<ResourceKey<Structure>> key = holder.unwrapKey();
            if (key.isEmpty()) {
                continue;
            }
            String namespace = key.get().location().getNamespace();
            if (namespace.equals("spore") || namespace.equals(SporeContainmentBreach.MODID)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Picks a weighted entry, rerolling (bounded by {@link #MAX_PICK_ATTEMPTS}) if the result has
     * already hit its per-raid cap. An entry's {@code max} field doubles as that cap here - unlike
     * {@link MoundDefenseSpawner}, which uses {@code min}/{@code max} as a batch-size range for a
     * single pick, ProtoRaidDirector picks one raider per slot, so max instead bounds how many of
     * that entity type may appear across the whole raid. A cap of 0 or lower means unlimited, which
     * is what every entry other than {@code spore:vanguard} defaults to - Vanguard is capped at 1
     * so its high pick weight doesn't produce a raid with more than one.
     */
    private static Optional<SpawnPoolEntry> pickWithinCap(SpawnPool pool, RandomSource random, Map<EntityType<?>, Integer> perTypeCounts) {
        for (int attempt = 0; attempt < MAX_PICK_ATTEMPTS; attempt++) {
            Optional<SpawnPoolEntry> picked = pool.pickWeighted(random);
            if (picked.isEmpty()) {
                return Optional.empty();
            }
            SpawnPoolEntry entry = picked.get();
            int cap = entry.max();
            if (cap <= 0 || perTypeCounts.getOrDefault(entry.type(), 0) < cap) {
                return picked;
            }
        }
        return Optional.empty();
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

    private static Optional<UUID> spawnRaider(
            ServerLevel level, Proto proto, SpawnPoolEntry entry, BlockPos target, int searchRadius, boolean calamityAllowedHere
    ) {
        Optional<BlockPos> position = SpawnAnchors.findGroundPosition(level, target, searchRadius, proto.getRandom());
        if (position.isEmpty()) {
            return Optional.empty();
        }

        Entity spawned = entry.type().create(level);
        if (spawned == null) {
            return Optional.empty();
        }

        boolean forbidden = spawned instanceof Womb || (!calamityAllowedHere && spawned instanceof Calamity);
        if (forbidden) {
            LOGGER.error(
                    "spore_containment_breach: refused to let a Proto raid spawn a forbidden entity type ({}) - "
                            + "this indicates a bug in spawn pool filtering, please report it",
                    entry.type()
            );
            spawned.discard();
            return Optional.empty();
        }

        BlockPos pos = position.get();
        RandomSource random = proto.getRandom();
        spawned.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
        spawned.getPersistentData().putUUID(RAID_BY_KEY, proto.getUUID());
        if (!level.addFreshEntity(spawned)) {
            return Optional.empty();
        }
        applyDirectedTravel(level, spawned, target);
        return Optional.of(spawned.getUUID());
    }

    /**
     * Points a fresh raider at the raid target instead of leaving it to idle at its spawn point.
     * Calamities get {@code setSearchArea}, which also self-chunkloads them while traveling via
     * base Spore's own ChunkLoaderMob handling - no further action needed there. Vanguard is its
     * own case: it isn't Infected (extends UtilityEntity) and has no searchPos at all - its
     * equivalent is {@code setVillage}, which its own tick()/tickMovement() already paths toward,
     * and since Vanguard also implements ChunkLoaderMob (shouldLoadChunk() true whenever its
     * village isn't BlockPos.ZERO) it self-chunkloads while walking through that same generic
     * HandlerEvents handler, exactly like a Calamity - no RaidTravelTracker needed for it either.
     * Without this branch a raid-spawned Vanguard would fall through to setSearchPos (a no-op,
     * since it doesn't have one) and instead wander off toward whatever village its own
     * locateVillageOnSpawn happened to find on spawn, ignoring the raid target entirely.
     * Everything else that extends Infected gets {@code setSearchPos} (the same base-Spore
     * "Search Positions" system Vanguard/Wombs/Linked mobs already use) plus our own lean
     * two-chunk "inching forward" travel-chunkload window, since protoRaidSearchRadius is large
     * enough that most targets won't already be in loaded/simulated space.
     */
    private static void applyDirectedTravel(ServerLevel level, Entity spawned, BlockPos target) {
        if (!SporeBreachServerConfig.PROTO_RAID_DIRECTED_TRAVEL.get()) {
            return;
        }
        if (spawned instanceof Calamity calamity) {
            calamity.setSearchArea(target);
        } else if (spawned instanceof Vanguard vanguard) {
            vanguard.setVillage(target);
        } else if (spawned instanceof Infected infected) {
            infected.setSearchPos(target);
            ChunkPos startChunk = new ChunkPos(spawned.blockPosition());
            RaidTravelTracker.track(level, spawned.getUUID(), startChunk);
            ChunkloadManager.advanceRaidTravelChunk(level, spawned.getUUID(), startChunk, null);
        }
    }
}
