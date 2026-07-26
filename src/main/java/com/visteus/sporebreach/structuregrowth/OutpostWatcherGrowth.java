package com.visteus.sporebreach.structuregrowth;

import com.Harbinger.Spore.Sentities.BaseEntities.Organoid;
import com.Harbinger.Spore.Sentities.Organoids.Mound;
import com.Harbinger.Spore.Sentities.Organoids.Proto;
import com.visteus.sporebreach.chunkloading.ChunkloadData;
import com.visteus.sporebreach.chunkloading.ChunkloadOwnerId;
import com.visteus.sporebreach.chunkloading.ChunkloadState;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import com.visteus.sporebreach.spawning.SpawnAnchors;
import com.visteus.sporebreach.structuregrowth.StructureFootprintData.Kind;
import com.visteus.sporebreach.structuregrowth.StructureFootprintData.Record;
import com.visteus.sporebreach.tracking.ProtoAgeTracker;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Goal #3 Outpost Watcher towers. Base Spore only ever grows one of these when a Reconstructed Mind
 * happens to mature too close to an existing Proto-Hivemind, which makes them a rarity; here every
 * Mound and Proto-Hivemind periodically rolls to seed one somewhere inside the area it already
 * chunkloads, so watchers become a normal part of an infested region instead of an accident.
 *
 * <p>One code path serves both organoid types - the config is shared, and the only per-type
 * differences are where "age" comes from and how far the organoid's chunkloading currently reaches.
 *
 * <p>Every rejection here is silent by design. A roll that finds no site - because everywhere in
 * range is too close to another watcher, overlaps an existing structure, or isn't natural ground -
 * simply does nothing until the next recheck. Only {@code OutpostDebugCommand} surfaces reasons.
 */
public final class OutpostWatcherGrowth {

    /** Why a placement attempt did or didn't start a tower. Reported only by the debug command. */
    public enum Result {
        STARTED,
        RESUMED,
        ALREADY_GROWING,
        DISABLED,
        TOO_YOUNG,
        AT_CAP,
        ROLL_FAILED,
        NOT_CHUNKLOADED,
        EMPTY_POOL,
        NO_VALID_SITE
    }

    private static final int MAX_SITE_ATTEMPTS = 20;

    private record ActiveOutpost(StructureGrowthJob job, Record record) {
    }

    private static final Map<UUID, ActiveOutpost> ACTIVE = new HashMap<>();

    private OutpostWatcherGrowth() {
    }

    /** Called on the recheck cadence. {@code force} skips the age gate and the chance roll. */
    public static Result tryStartJob(ServerLevel level, Organoid organoid, boolean force) {
        UUID id = organoid.getUUID();
        if (ACTIVE.containsKey(id)) {
            return Result.ALREADY_GROWING;
        }

        Optional<Record> unfinished = StructureFootprintData.incompleteFor(level, id, Kind.OUTPOST);
        if (unfinished.isPresent()) {
            Record record = unfinished.get();
            StructureTemplate template = OrganoidStructurePlacer.resolveTemplate(level, record.structureId());
            ACTIVE.put(id, new ActiveOutpost(
                    OrganoidStructurePlacer.buildJobAtOrigin(template, record.origin(), record.growDownward()), record
            ));
            return Result.RESUMED;
        }

        if (!force && ageOf(level, organoid) < SporeBreachServerConfig.OUTPOST_WATCHER_MIN_AGE.get()) {
            return Result.TOO_YOUNG;
        }
        if (StructureFootprintData.countOwned(level, id, Kind.OUTPOST)
                >= SporeBreachServerConfig.OUTPOST_WATCHER_MAX_PER_ORGANOID.get()) {
            return Result.AT_CAP;
        }

        RandomSource random = organoid.getRandom();
        if (!force && random.nextDouble() >= SporeBreachServerConfig.GROWTH_PLACEMENT_CHANCE.get()) {
            return Result.ROLL_FAILED;
        }

        int maxDistance = currentChunkloadReach(level, organoid);
        if (maxDistance <= 0) {
            return Result.NOT_CHUNKLOADED;
        }

        Optional<StructurePoolEntry> entry =
                StructurePool.fromConfig(SporeBreachServerConfig.OUTPOST_WATCHER_POOL.get()).pickWeighted(random);
        if (entry.isEmpty()) {
            return Result.EMPTY_POOL;
        }

        StructureTemplate template = OrganoidStructurePlacer.resolveTemplate(level, entry.get().structureId());
        BlockPos anchor = pickSite(level, organoid, template, maxDistance, random);
        if (anchor == null) {
            return Result.NO_VALID_SITE;
        }

        BlockPos origin = OrganoidStructurePlacer.jobOrigin(template, anchor, false);
        Record record = StructureFootprintData.claim(
                level, id, Kind.OUTPOST, entry.get().structureId(), anchor,
                OrganoidStructurePlacer.footprint(template, origin), false
        );
        ACTIVE.put(id, new ActiveOutpost(OrganoidStructurePlacer.buildJobAtOrigin(template, origin, false), record));
        return Result.STARTED;
    }

    /** Called on the pass cadence: advance this organoid's tower, if it has one growing. */
    public static void tryAdvanceJob(ServerLevel level, Organoid organoid) {
        ActiveOutpost active = ACTIVE.get(organoid.getUUID());
        if (active == null) {
            return;
        }

        int blocksPerPass = SporeBreachServerConfig.OUTPOST_WATCHER_BLOCKS_PER_PASS.get();
        active.job().advance(level, organoid, organoid.getRandom(), blocksPerPass);
        if (active.job().isComplete()) {
            StructureFootprintData.markComplete(active.record());
            ACTIVE.remove(organoid.getUUID());
        }
    }

    /**
     * How far out this organoid currently holds chunks, in blocks. Zero means it hasn't started
     * chunkloading at all - no player has come near it yet - which is also the outer bound for
     * placement, so a tower can never be seeded into terrain the organoid isn't keeping loaded.
     */
    private static int currentChunkloadReach(ServerLevel level, Organoid organoid) {
        ChunkloadState state = ChunkloadData.getState(level, new ChunkloadOwnerId.EntityOwner(organoid.getUUID()));
        return state == null ? 0 : state.lastIssuedRadius() * 16;
    }

    private static int ageOf(ServerLevel level, Organoid organoid) {
        if (organoid instanceof Mound mound) {
            return mound.getAge();
        }
        if (organoid instanceof Proto proto) {
            return ProtoAgeTracker.getAge(proto, level.getGameTime());
        }
        return 0;
    }

    /**
     * Random surface points inside the organoid's chunkload reach, rejected until one is natural
     * ground, spaced away from every other watcher, and clear of every structure this mod has
     * grown. There is deliberately no inner bound - staying off the parent's own spires is already
     * the clearance rule's job.
     */
    private static BlockPos pickSite(
            ServerLevel level, Organoid organoid, StructureTemplate template, int maxDistance, RandomSource random
    ) {
        int minSpacing = SporeBreachServerConfig.OUTPOST_WATCHER_MIN_DISTANCE_BETWEEN.get();
        int clearance = SporeBreachServerConfig.OUTPOST_WATCHER_MIN_CLEARANCE.get();
        BlockPos center = organoid.getOnPos();

        for (int attempt = 0; attempt < MAX_SITE_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            int distance = random.nextInt(maxDistance + 1);
            int x = center.getX() + (int) Math.round(Math.cos(angle) * distance);
            int z = center.getZ() + (int) Math.round(Math.sin(angle) * distance);

            // Checked before any heightmap read so an unloaded column can't trigger generation.
            if (!level.isLoaded(new BlockPos(x, level.getMinBuildHeight(), z))) {
                continue;
            }

            BlockPos candidate = new BlockPos(x, OrganoidStructurePlacer.surfaceHeight(level, x, z), z);
            if (SpawnAnchors.isWithinProtectedSpawnRadius(level, candidate)) {
                continue;
            }
            if (!OrganoidStructurePlacer.isNaturalGround(level, candidate.below())) {
                continue;
            }
            if (OutpostWatcherIndex.hasWatcherWithin(level, candidate, minSpacing)) {
                continue;
            }

            BlockPos anchor = candidate.below(2);
            BoundingBox footprint =
                    OrganoidStructurePlacer.footprint(template, OrganoidStructurePlacer.jobOrigin(template, anchor, false));
            if (StructureFootprintData.intersects(level, footprint, clearance)) {
                continue;
            }
            return anchor;
        }
        return null;
    }
}
