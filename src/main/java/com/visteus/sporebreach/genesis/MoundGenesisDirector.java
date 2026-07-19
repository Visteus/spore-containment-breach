package com.visteus.sporebreach.genesis;

import com.Harbinger.Spore.Sentities.Organoids.Mound;
import com.Harbinger.Spore.core.Sentities;
import com.mojang.logging.LogUtils;
import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import com.visteus.sporebreach.spawning.SpawnAnchors;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

/**
 * Goal #1 "Mound genesis": the first time a player comes near an eligible pre-generated
 * structure ({@code moundGenesisEligibleStructures}), places a one-time batch of Mounds there
 * and permanently marks that structure instance as seeded ({@link MoundGenesisData}) so it
 * never spawns another. Independent of {@code OrganoidSpawnDirector} - that system only ever
 * dispatches organoids that already exist; this is the sole place new Mounds enter the world
 * from world generation.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class MoundGenesisDirector {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static long tickCounter;

    private MoundGenesisDirector() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!SporeBreachServerConfig.MOUND_GENESIS_ENABLED.get()) {
            return;
        }
        tickCounter++;
        int interval = SporeBreachServerConfig.MOUND_GENESIS_SCAN_INTERVAL_TICKS.get();
        if (interval <= 0 || tickCounter % interval != 0) {
            return;
        }

        StructureAnchorSelector selector = StructureAnchorSelector.fromConfig(
                SporeBreachServerConfig.MOUND_GENESIS_ELIGIBLE_STRUCTURES.get()
        );

        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                scanNear(level, player.blockPosition(), selector);
            }
        }
    }

    private static void scanNear(ServerLevel level, BlockPos playerPos, StructureAnchorSelector selector) {
        // Reads structure references already cached on the player's loaded chunk - does not
        // force structure generation/search. (Verify exact StructureManager method/signature
        // once compiling - this mirrors the vanilla structure-location-check idiom.)
        Map<Structure, LongSet> nearby = level.structureManager().getAllStructuresAt(playerPos);
        if (nearby.isEmpty()) {
            return;
        }

        for (Structure structure : nearby.keySet()) {
            Holder<Structure> holder = level.registryAccess().registryOrThrow(Registries.STRUCTURE).wrapAsHolder(structure);
            if (!selector.matches(holder)) {
                continue;
            }

            StructureStart start = level.structureManager().getStructureAt(playerPos, structure);
            if (start == null || !start.isValid()) {
                continue;
            }

            attemptGenesis(level, start.getBoundingBox());
        }
    }

    private static void attemptGenesis(ServerLevel level, BoundingBox structureBox) {
        BlockPos anchor = structureBox.getCenter();
        if (MoundGenesisData.hasSeeded(level, anchor)) {
            return;
        }
        if (SpawnAnchors.isWithinProtectedSpawnRadius(level, anchor)) {
            return;
        }

        RandomSource random = level.getRandom();
        int minCount = SporeBreachServerConfig.MOUND_GENESIS_COUNT_MIN.get();
        int maxCount = SporeBreachServerConfig.MOUND_GENESIS_COUNT_MAX.get();
        int min = Math.min(minCount, maxCount);
        int max = Math.max(minCount, maxCount);
        int count = min + random.nextInt(max - min + 1);
        // Scales with the structure instead of a flat config value: mounds should land roughly
        // within a structure's own footprint, not wander an arbitrary fixed distance from tiny
        // structures or hug the center of huge ones.
        double halfExtent = (structureBox.getXSpan() + structureBox.getZSpan()) / 4.0;
        int radius = (int) Math.round(halfExtent * 0.75);

        int placed = 0;
        for (int i = 0; i < count; i++) {
            Optional<BlockPos> position = SpawnAnchors.findGroundPosition(level, anchor, radius, random);
            if (position.isEmpty()) {
                continue;
            }

            Mound mound = Sentities.MOUND.get().create(level);
            if (mound == null) {
                continue;
            }

            BlockPos pos = position.get();
            mound.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
            if (level.addFreshEntity(mound)) {
                placed++;
            }
        }

        // Consume the genesis event as soon as at least one Mound landed, even if terrain only
        // had room for fewer than the rolled count - avoids retrying forever on a cramped site.
        if (placed > 0) {
            LOGGER.debug("spore_containment_breach: genesis-placed {} Mound(s) at structure anchor {}", placed, anchor);
            MoundGenesisData.markSeeded(level, anchor);
        }
    }
}
