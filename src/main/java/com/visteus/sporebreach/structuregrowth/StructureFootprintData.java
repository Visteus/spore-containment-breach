package com.visteus.sporebreach.structuregrowth;

import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.persistence.PersistedData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Every structure this mod has ever started growing, per dimension, persisted into
 * {@code .spore-breach/}. Replaces the old per-organoid in-memory anchor list, which forgot
 * everything on unload - an organoid could then rebuild past its cap, and a half-grown tower was
 * abandoned as a permanent stump. Records outlive their owner on purpose: the blocks are still
 * standing, so the space stays reserved even after the Mound/Proto that grew them is killed.
 *
 * <p>Queries are linear scans over one dimension's list. Real worlds hold tens to low hundreds of
 * records, and these are consulted on slow recheck cadences (minutes), not per tick - if that ever
 * changes, bucket by {@link net.minecraft.world.level.ChunkPos}.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class StructureFootprintData extends PersistedData {

    /** Which growth slot a record belongs to, so each kind can be counted against its own cap. */
    public enum Kind {
        SURFACE,
        UNDERGROUND,
        OUTPOST
    }

    /**
     * One structure. {@code anchor} is the position growth was aimed at (used for the
     * spread-apart distance checks), while {@code bounds} is the space the template actually
     * occupies; the job origin is the bounds' minimum corner, which is all a resume needs.
     */
    public static final class Record {

        private final UUID owner;
        private final Kind kind;
        private final ResourceLocation structureId;
        private final BlockPos anchor;
        private final BoundingBox bounds;
        private final boolean growDownward;
        private boolean complete;

        private Record(UUID owner, Kind kind, ResourceLocation structureId, BlockPos anchor,
                       BoundingBox bounds, boolean growDownward, boolean complete) {
            this.owner = owner;
            this.kind = kind;
            this.structureId = structureId;
            this.anchor = anchor;
            this.bounds = bounds;
            this.growDownward = growDownward;
            this.complete = complete;
        }

        public UUID owner() {
            return owner;
        }

        public Kind kind() {
            return kind;
        }

        public ResourceLocation structureId() {
            return structureId;
        }

        public BlockPos anchor() {
            return anchor;
        }

        public BoundingBox bounds() {
            return bounds;
        }

        public boolean growDownward() {
            return growDownward;
        }

        public boolean complete() {
            return complete;
        }

        /** The template's own (0,0,0) corner in world space - enough to rebuild the job. */
        public BlockPos origin() {
            return new BlockPos(bounds.minX(), bounds.minY(), bounds.minZ());
        }
    }

    private static StructureFootprintData instance;

    private final Map<ResourceKey<Level>, List<Record>> recordsByDimension = new HashMap<>();

    private StructureFootprintData() {
    }

    private static StructureFootprintData get() {
        if (instance == null) {
            instance = new StructureFootprintData();
            instance.initialize();
        }
        return instance;
    }

    private static List<Record> records(ServerLevel level) {
        return get().recordsByDimension.getOrDefault(level.dimension(), List.of());
    }

    public static Record claim(
            ServerLevel level, UUID owner, Kind kind, ResourceLocation structureId,
            BlockPos anchor, BoundingBox bounds, boolean growDownward
    ) {
        Record record = new Record(owner, kind, structureId, anchor, bounds, growDownward, false);
        StructureFootprintData data = get();
        data.recordsByDimension.computeIfAbsent(level.dimension(), key -> new ArrayList<>()).add(record);
        data.markDirty();
        return record;
    }

    public static void markComplete(Record record) {
        if (record.complete) {
            return;
        }
        record.complete = true;
        get().markDirty();
    }

    public static int countOwned(ServerLevel level, UUID owner, Kind kind) {
        int count = 0;
        for (Record record : records(level)) {
            if (record.kind == kind && record.owner.equals(owner)) {
                count++;
            }
        }
        return count;
    }

    /** The unfinished job of this kind for this organoid, if it has one - drives growth resume. */
    public static Optional<Record> incompleteFor(ServerLevel level, UUID owner, Kind kind) {
        for (Record record : records(level)) {
            if (!record.complete && record.kind == kind && record.owner.equals(owner)) {
                return Optional.of(record);
            }
        }
        return Optional.empty();
    }

    /**
     * True if any structure's footprint comes within {@code clearance} blocks of {@code box}. Used
     * to keep a new structure from growing into an existing one; a clearance of 0 tests raw
     * overlap.
     */
    public static boolean intersects(ServerLevel level, BoundingBox box, int clearance) {
        BoundingBox inflated = new BoundingBox(
                box.minX() - clearance, box.minY() - clearance, box.minZ() - clearance,
                box.maxX() + clearance, box.maxY() + clearance, box.maxZ() + clearance
        );
        for (Record record : records(level)) {
            if (record.bounds.intersects(inflated)) {
                return true;
            }
        }
        return false;
    }

    /**
     * True if {@code owner} already aimed a structure of this kind within {@code radius} of
     * {@code pos}. Deliberately an anchor-to-anchor distance rather than a footprint overlap: the
     * spread-apart rule these organoids use is about spacing out where structures are centered,
     * and templates are wide enough that a box-to-box test at the same radius would reject
     * essentially every candidate site.
     */
    public static boolean anyOwnedWithin(ServerLevel level, UUID owner, Kind kind, BlockPos pos, int radius) {
        double radiusSq = (double) radius * radius;
        for (Record record : records(level)) {
            if (record.kind == kind && record.owner.equals(owner) && record.anchor.distSqr(pos) < radiusSq) {
                return true;
            }
        }
        return false;
    }

    /** True if any structure of this kind, from any owner, is anchored within {@code radius}. */
    public static boolean anyWithin(ServerLevel level, Kind kind, BlockPos pos, int radius) {
        double radiusSq = (double) radius * radius;
        for (Record record : records(level)) {
            if (record.kind == kind && record.anchor.distSqr(pos) < radiusSq) {
                return true;
            }
        }
        return false;
    }

    /** Every record anchored within {@code radius} of {@code pos} - readback for the debug command. */
    public static List<Record> nearby(ServerLevel level, BlockPos pos, int radius) {
        double radiusSq = (double) radius * radius;
        List<Record> found = new ArrayList<>();
        for (Record record : records(level)) {
            if (record.anchor.distSqr(pos) <= radiusSq) {
                found.add(record);
            }
        }
        return found;
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        instance = null;
    }

    @Override
    protected String fileName() {
        return "structure_footprints.dat";
    }

    @Override
    protected CompoundTag save(CompoundTag tag) {
        ListTag dimensions = new ListTag();
        for (Map.Entry<ResourceKey<Level>, List<Record>> entry : recordsByDimension.entrySet()) {
            ListTag footprints = new ListTag();
            for (Record record : entry.getValue()) {
                CompoundTag recordTag = new CompoundTag();
                recordTag.putUUID("Owner", record.owner);
                recordTag.putString("Kind", record.kind.name());
                recordTag.putString("Structure", record.structureId.toString());
                recordTag.putIntArray("Anchor", new int[]{record.anchor.getX(), record.anchor.getY(), record.anchor.getZ()});
                recordTag.putIntArray("Bounds", new int[]{
                        record.bounds.minX(), record.bounds.minY(), record.bounds.minZ(),
                        record.bounds.maxX(), record.bounds.maxY(), record.bounds.maxZ()
                });
                recordTag.putBoolean("Down", record.growDownward);
                recordTag.putBoolean("Done", record.complete);
                footprints.add(recordTag);
            }
            CompoundTag dimensionTag = new CompoundTag();
            dimensionTag.putString("Dimension", entry.getKey().location().toString());
            dimensionTag.put("Footprints", footprints);
            dimensions.add(dimensionTag);
        }
        tag.put("Dimensions", dimensions);
        return tag;
    }

    @Override
    protected void load(CompoundTag tag) {
        recordsByDimension.clear();
        ListTag dimensions = tag.getList("Dimensions", Tag.TAG_COMPOUND);
        for (int i = 0; i < dimensions.size(); i++) {
            CompoundTag dimensionTag = dimensions.getCompound(i);
            ResourceKey<Level> key = ResourceKey.create(
                    Registries.DIMENSION, ResourceLocation.parse(dimensionTag.getString("Dimension"))
            );

            List<Record> records = new ArrayList<>();
            ListTag footprints = dimensionTag.getList("Footprints", Tag.TAG_COMPOUND);
            for (int j = 0; j < footprints.size(); j++) {
                CompoundTag recordTag = footprints.getCompound(j);
                int[] anchor = recordTag.getIntArray("Anchor");
                int[] bounds = recordTag.getIntArray("Bounds");
                if (anchor.length != 3 || bounds.length != 6) {
                    continue;
                }
                records.add(new Record(
                        recordTag.getUUID("Owner"),
                        Kind.valueOf(recordTag.getString("Kind")),
                        ResourceLocation.parse(recordTag.getString("Structure")),
                        new BlockPos(anchor[0], anchor[1], anchor[2]),
                        new BoundingBox(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]),
                        recordTag.getBoolean("Down"),
                        recordTag.getBoolean("Done")
                ));
            }
            recordsByDimension.put(key, records);
        }
    }
}
