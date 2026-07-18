package com.visteus.sporebreach.spawning;

import com.Harbinger.Spore.Sentities.BaseEntities.Calamity;
import com.Harbinger.Spore.Sentities.Organoids.Mound;
import com.Harbinger.Spore.Sentities.Organoids.Womb;
import com.mojang.logging.LogUtils;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

/**
 * New Goal #1 behavior: Mounds periodically spawn a small number of defensive mobs in their
 * vicinity. Base Spore's Mound has no mob-spawning logic at all (only tendril-spawning and
 * one-shot structure-block placement) - this is wholly new, dispatched by
 * OrganoidSpawnCoordinator on a per-Mound cooldown.
 */
public final class MoundDefenseSpawner {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String COOLDOWN_KEY = "sporebreach_defender_cd";
    private static final String SPAWNED_BY_KEY = "sporebreach_spawned_by";

    private MoundDefenseSpawner() {
    }

    public static boolean isEligible(Mound mound) {
        if (!(mound.level() instanceof ServerLevel level)) {
            return false;
        }
        if (SpawnAnchors.isWithinProtectedSpawnRadius(level, mound.getOnPos())) {
            return false;
        }
        long readyAt = mound.getPersistentData().getLong(COOLDOWN_KEY);
        return level.getGameTime() >= readyAt;
    }

    public static void attempt(Mound mound) {
        if (!(mound.level() instanceof ServerLevel level)) {
            return;
        }

        int cooldownTicks = SporeBreachServerConfig.MOUND_DEFENDER_COOLDOWN_TICKS.get();
        mound.getPersistentData().putLong(COOLDOWN_KEY, level.getGameTime() + cooldownTicks);

        int searchRadius = SporeBreachServerConfig.MOUND_DEFENDER_SEARCH_RADIUS.get();
        int maxNearby = SporeBreachServerConfig.MOUND_MAX_DEFENDERS_NEARBY.get();
        AABB searchBox = mound.getBoundingBox().inflate(searchRadius);
        long existingDefenders = level.getEntities(mound, searchBox, entity -> isOurDefender(entity, mound)).size();
        if (existingDefenders >= maxNearby) {
            return;
        }

        SpawnPool pool = SpawnPool.fromConfig(SporeBreachServerConfig.MOUND_DEFENDER_SPAWN_POOL.get(), true, "moundDefenderSpawnPool");
        Optional<SpawnPoolEntry> picked = pool.pickWeighted(mound.getRandom());
        if (picked.isEmpty()) {
            return;
        }
        SpawnPoolEntry entry = picked.get();

        BlockPos anchor = SpawnAnchors.resolveAnchor(mound);
        int count = pool.pickCount(mound.getRandom(), entry);
        for (int i = 0; i < count; i++) {
            spawnOne(level, mound, entry, anchor, searchRadius);
        }
    }

    private static void spawnOne(ServerLevel level, Mound mound, SpawnPoolEntry entry, BlockPos anchor, int searchRadius) {
        Optional<BlockPos> position = SpawnAnchors.findGroundPosition(level, anchor, searchRadius, mound.getRandom());
        if (position.isEmpty()) {
            return;
        }

        Entity spawned = entry.type().create(level);
        if (spawned == null) {
            return;
        }

        // Defense-in-depth: the pool is already built with banCalamitiesAndWombs=true, so this
        // should never trip, but Mounds must never spawn calamities/Wombs under any circumstance.
        if (spawned instanceof Calamity || spawned instanceof Womb) {
            LOGGER.error(
                    "spore_containment_breach: refused to let a Mound spawn a forbidden entity type ({}) - "
                            + "this indicates a bug in spawn pool filtering, please report it",
                    entry.type()
            );
            spawned.discard();
            return;
        }

        BlockPos pos = position.get();
        spawned.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, mound.getRandom().nextFloat() * 360.0F, 0.0F);
        spawned.getPersistentData().putUUID(SPAWNED_BY_KEY, mound.getUUID());
        level.addFreshEntity(spawned);
    }

    private static boolean isOurDefender(Entity entity, Mound mound) {
        CompoundTag data = entity.getPersistentData();
        return data.hasUUID(SPAWNED_BY_KEY) && mound.getUUID().equals(data.getUUID(SPAWNED_BY_KEY));
    }
}
