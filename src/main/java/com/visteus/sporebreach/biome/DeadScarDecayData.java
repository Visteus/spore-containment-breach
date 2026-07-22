package com.visteus.sporebreach.biome;

import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.persistence.PersistedData;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Persisted backlog of dead_scar columns awaiting (or mid-) block decay, following {@link
 * BiomePaintData}'s exact singleton/PersistedData pattern. Unlike {@link BiomePaintData}, this only
 * ever tracks this feature's own scan-progress cursor per column - ticket ownership itself doesn't
 * need to be persisted here, since NeoForge's {@code TicketController} already persists and silently
 * reinstates forced chunks on its own (see {@code DeadScarDecayManager}'s class doc).
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class DeadScarDecayData extends PersistedData {

    /** Sentinel cursor value meaning "enqueued, but its sweep hasn't started yet". */
    public static final int NOT_STARTED = Integer.MIN_VALUE;

    private static DeadScarDecayData instance;

    private final Map<ResourceKey<Level>, Map<ChunkPos, Integer>> cursorsByDimension = new HashMap<>();

    private DeadScarDecayData() {
    }

    private static DeadScarDecayData get() {
        if (instance == null) {
            instance = new DeadScarDecayData();
            instance.initialize();
        }
        return instance;
    }

    /** Adds {@code pos} to the backlog at {@link #NOT_STARTED}, if not already tracked. */
    public static void enqueue(ServerLevel level, ChunkPos pos) {
        DeadScarDecayData data = get();
        Map<ChunkPos, Integer> columns = data.cursorsByDimension.computeIfAbsent(level.dimension(), key -> new HashMap<>());
        if (columns.putIfAbsent(pos, NOT_STARTED) == null) {
            data.markDirty();
        }
    }

    /** Persists sweep progress for {@code pos} - a no-op if it isn't (or is no longer) tracked. */
    public static void advanceCursor(ServerLevel level, ChunkPos pos, int newCursor) {
        DeadScarDecayData data = get();
        Map<ChunkPos, Integer> columns = data.cursorsByDimension.get(level.dimension());
        if (columns == null || !columns.containsKey(pos)) {
            return;
        }
        columns.put(pos, newCursor);
        data.markDirty();
    }

    /** Current resume cursor for {@code pos}, or {@link #NOT_STARTED} if untracked. */
    public static int getCursor(ServerLevel level, ChunkPos pos) {
        Map<ChunkPos, Integer> columns = get().cursorsByDimension.get(level.dimension());
        return columns == null ? NOT_STARTED : columns.getOrDefault(pos, NOT_STARTED);
    }

    /** Removes {@code pos} from the backlog entirely - either its sweep finished, or it was cancelled. */
    public static void remove(ServerLevel level, ChunkPos pos) {
        DeadScarDecayData data = get();
        Map<ChunkPos, Integer> columns = data.cursorsByDimension.get(level.dimension());
        if (columns != null && columns.remove(pos) != null) {
            data.markDirty();
        }
    }

    /** Read-only snapshot of this dimension's backlog, for {@link DeadScarDecayManager} to pick from. */
    public static Map<ChunkPos, Integer> snapshotPending(ServerLevel level) {
        Map<ChunkPos, Integer> columns = get().cursorsByDimension.get(level.dimension());
        return columns == null ? Map.of() : Map.copyOf(columns);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        instance = null;
    }

    @Override
    protected String fileName() {
        return "dead_scar_decay.dat";
    }

    @Override
    protected CompoundTag save(CompoundTag tag) {
        ListTag dimensions = new ListTag();
        for (Map.Entry<ResourceKey<Level>, Map<ChunkPos, Integer>> dimEntry : cursorsByDimension.entrySet()) {
            if (dimEntry.getValue().isEmpty()) {
                continue;
            }
            CompoundTag dimensionTag = new CompoundTag();
            dimensionTag.putString("Dimension", dimEntry.getKey().location().toString());

            ListTag columns = new ListTag();
            for (Map.Entry<ChunkPos, Integer> columnEntry : dimEntry.getValue().entrySet()) {
                CompoundTag columnTag = new CompoundTag();
                columnTag.putLong("Pos", columnEntry.getKey().toLong());
                columnTag.putInt("Cursor", columnEntry.getValue());
                columns.add(columnTag);
            }
            dimensionTag.put("Columns", columns);
            dimensions.add(dimensionTag);
        }
        tag.put("Dimensions", dimensions);
        return tag;
    }

    @Override
    protected void load(CompoundTag tag) {
        cursorsByDimension.clear();
        ListTag dimensions = tag.getList("Dimensions", Tag.TAG_COMPOUND);
        for (int i = 0; i < dimensions.size(); i++) {
            CompoundTag dimensionTag = dimensions.getCompound(i);
            ResourceKey<Level> dimensionKey = ResourceKey.create(
                    Registries.DIMENSION, ResourceLocation.parse(dimensionTag.getString("Dimension"))
            );

            Map<ChunkPos, Integer> columns = new HashMap<>();
            ListTag columnList = dimensionTag.getList("Columns", Tag.TAG_COMPOUND);
            for (int j = 0; j < columnList.size(); j++) {
                CompoundTag columnTag = columnList.getCompound(j);
                columns.put(new ChunkPos(columnTag.getLong("Pos")), columnTag.getInt("Cursor"));
            }
            cursorsByDimension.put(dimensionKey, columns);
        }
    }
}
