package com.visteus.sporebreach.biome;

import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.persistence.PersistedData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
import org.jetbrains.annotations.Nullable;

/**
 * Storage facade for Goal #5's per-column biome claim/stage tracking, following {@link
 * com.visteus.sporebreach.chunkloading.ChunkloadData}'s exact singleton/PersistedData pattern. A
 * column stays claimed by every organoid whose grown radius currently covers it; the claimant set
 * emptying (an organoid dying) is what starts the "lingering scar" downgrade countdown, and a new
 * claim on a column mid-countdown (or already downgraded) cancels it - see {@link #claim} /
 * {@link #unclaim}. Columns that were never claimed, or have fully reverted, aren't persisted at
 * all - only ones with claimants or an in-flight/completed downgrade are worth the NBT.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class BiomePaintData extends PersistedData {

    public enum BiomeStage {
        ACTIVE,
        PENDING_DOWNGRADE,
        DOWNGRADED
    }

    public static final class ColumnState {
        private final Set<UUID> claimants = new HashSet<>();
        private BiomeStage stage = BiomeStage.ACTIVE;
        private long pendingDowngradeDeadlineGameTime = -1;
        @Nullable
        private UUID ticketOwner;

        public Set<UUID> claimants() {
            return claimants;
        }

        public BiomeStage stage() {
            return stage;
        }

        public long pendingDowngradeDeadlineGameTime() {
            return pendingDowngradeDeadlineGameTime;
        }

        @Nullable
        public UUID ticketOwner() {
            return ticketOwner;
        }
    }

    /**
     * Result of {@link #claim}: whether the caller still needs to issue a physical force-load
     * ticket ({@code ticketOwner} was previously unset - e.g. brand new, or its last ticket was
     * released after a completed downgrade), and whether the caller needs to (re)enqueue an actual
     * repaint (brand new, or reactivated from a pending/completed downgrade).
     */
    public record ClaimResult(boolean needsTicket, boolean needsPaint) {
    }

    private static BiomePaintData instance;

    private final Map<ResourceKey<Level>, Map<ChunkPos, ColumnState>> stateByDimension = new HashMap<>();

    private BiomePaintData() {
    }

    private static BiomePaintData get() {
        if (instance == null) {
            instance = new BiomePaintData();
            instance.initialize();
        }
        return instance;
    }

    /**
     * Adds {@code ownerId} as a claimant of {@code pos}, reactivating the column (clearing any
     * pending or completed downgrade) if it wasn't already {@link BiomeStage#ACTIVE}. Only the
     * first claimant to ever hold a physical ticket on a column becomes its recorded
     * {@code ticketOwner} - later overlapping claimants just grow the refcount ({@link
     * ClaimResult#needsTicket} tells the caller whether to actually call {@code
     * ChunkloadManager.forceBiomePaintChunk}), so a column already kept loaded by one owner's
     * ticket doesn't accumulate a redundant one per additional claimant.
     */
    public static ClaimResult claim(ServerLevel level, ChunkPos pos, UUID ownerId) {
        BiomePaintData data = get();
        Map<ChunkPos, ColumnState> columns = data.stateByDimension.computeIfAbsent(level.dimension(), key -> new HashMap<>());
        ColumnState state = columns.get(pos);
        boolean isNew = state == null;
        if (isNew) {
            state = new ColumnState();
            columns.put(pos, state);
        }

        boolean wasActive = state.stage == BiomeStage.ACTIVE;
        boolean needsTicket = state.ticketOwner == null;

        state.claimants.add(ownerId);
        if (needsTicket) {
            state.ticketOwner = ownerId;
        }
        if (state.stage != BiomeStage.ACTIVE) {
            state.stage = BiomeStage.ACTIVE;
            state.pendingDowngradeDeadlineGameTime = -1;
        }
        data.markDirty();

        return new ClaimResult(needsTicket, isNew || !wasActive);
    }

    /**
     * Removes {@code ownerId} as a claimant of {@code pos}. If this empties the claimant set and
     * the column is currently {@link BiomeStage#ACTIVE}, starts the lingering-scar countdown
     * rather than reverting immediately.
     */
    public static void unclaim(ServerLevel level, ChunkPos pos, UUID ownerId, long nowGameTime, long scarDelayTicks) {
        BiomePaintData data = get();
        Map<ChunkPos, ColumnState> columns = data.stateByDimension.get(level.dimension());
        if (columns == null) {
            return;
        }
        ColumnState state = columns.get(pos);
        if (state == null || !state.claimants.remove(ownerId)) {
            return;
        }
        if (state.claimants.isEmpty() && state.stage == BiomeStage.ACTIVE) {
            state.stage = BiomeStage.PENDING_DOWNGRADE;
            state.pendingDowngradeDeadlineGameTime = nowGameTime + scarDelayTicks;
        }
        data.markDirty();
    }

    @Nullable
    public static ColumnState getState(ServerLevel level, ChunkPos pos) {
        Map<ChunkPos, ColumnState> columns = get().stateByDimension.get(level.dimension());
        return columns == null ? null : columns.get(pos);
    }

    /**
     * Columns whose scar countdown has elapsed with no reclaiming owner in the meantime. Read-only
     * - doesn't mutate anything, so a repaint failure (chunk not currently loaded) can safely leave
     * a column exactly as due next pass too. See {@link #markDowngraded} for committing the result
     * once the caller has confirmed the repaint actually happened.
     */
    public static List<ChunkPos> peekDuePendingDowngrades(ServerLevel level, long nowGameTime) {
        Map<ChunkPos, ColumnState> columns = get().stateByDimension.get(level.dimension());
        if (columns == null) {
            return List.of();
        }
        List<ChunkPos> due = new ArrayList<>();
        for (Map.Entry<ChunkPos, ColumnState> entry : columns.entrySet()) {
            ColumnState state = entry.getValue();
            if (state.stage == BiomeStage.PENDING_DOWNGRADE && state.claimants.isEmpty()
                    && state.pendingDowngradeDeadlineGameTime <= nowGameTime) {
                due.add(entry.getKey());
            }
        }
        return due;
    }

    /**
     * Commits a completed downgrade: flips the column to {@link BiomeStage#DOWNGRADED} and hands
     * back its {@code ticketOwner} (clearing it here) so the caller can release the now-unneeded
     * force-load ticket. Only call after the repaint to {@code dead_scar} actually succeeded.
     */
    @Nullable
    public static UUID markDowngraded(ServerLevel level, ChunkPos pos) {
        BiomePaintData data = get();
        Map<ChunkPos, ColumnState> columns = data.stateByDimension.get(level.dimension());
        if (columns == null) {
            return null;
        }
        ColumnState state = columns.get(pos);
        if (state == null) {
            return null;
        }
        state.stage = BiomeStage.DOWNGRADED;
        UUID ticketOwner = state.ticketOwner;
        state.ticketOwner = null;
        data.markDirty();
        return ticketOwner;
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        instance = null;
    }

    @Override
    protected String fileName() {
        return "biome_paint.dat";
    }

    @Override
    protected CompoundTag save(CompoundTag tag) {
        ListTag dimensions = new ListTag();
        for (Map.Entry<ResourceKey<Level>, Map<ChunkPos, ColumnState>> dimEntry : stateByDimension.entrySet()) {
            CompoundTag dimensionTag = new CompoundTag();
            dimensionTag.putString("Dimension", dimEntry.getKey().location().toString());

            ListTag columns = new ListTag();
            for (Map.Entry<ChunkPos, ColumnState> columnEntry : dimEntry.getValue().entrySet()) {
                ColumnState state = columnEntry.getValue();
                if (state.claimants.isEmpty() && state.stage == BiomeStage.ACTIVE) {
                    continue;
                }
                CompoundTag columnTag = new CompoundTag();
                columnTag.putLong("Pos", columnEntry.getKey().toLong());
                columnTag.putString("Stage", state.stage.name());
                columnTag.putLong("Deadline", state.pendingDowngradeDeadlineGameTime);
                if (state.ticketOwner != null) {
                    columnTag.putUUID("TicketOwner", state.ticketOwner);
                }
                ListTag claimants = new ListTag();
                for (UUID claimant : state.claimants) {
                    CompoundTag claimantTag = new CompoundTag();
                    claimantTag.putUUID("Uuid", claimant);
                    claimants.add(claimantTag);
                }
                columnTag.put("Claimants", claimants);
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
        stateByDimension.clear();
        ListTag dimensions = tag.getList("Dimensions", Tag.TAG_COMPOUND);
        for (int i = 0; i < dimensions.size(); i++) {
            CompoundTag dimensionTag = dimensions.getCompound(i);
            ResourceKey<Level> dimensionKey = ResourceKey.create(
                    Registries.DIMENSION, ResourceLocation.parse(dimensionTag.getString("Dimension"))
            );

            Map<ChunkPos, ColumnState> columns = new HashMap<>();
            ListTag columnList = dimensionTag.getList("Columns", Tag.TAG_COMPOUND);
            for (int j = 0; j < columnList.size(); j++) {
                CompoundTag columnTag = columnList.getCompound(j);
                ColumnState state = new ColumnState();
                state.stage = BiomeStage.valueOf(columnTag.getString("Stage"));
                state.pendingDowngradeDeadlineGameTime = columnTag.getLong("Deadline");
                if (columnTag.hasUUID("TicketOwner")) {
                    state.ticketOwner = columnTag.getUUID("TicketOwner");
                }
                ListTag claimants = columnTag.getList("Claimants", Tag.TAG_COMPOUND);
                for (int k = 0; k < claimants.size(); k++) {
                    state.claimants.add(claimants.getCompound(k).getUUID("Uuid"));
                }
                columns.put(new ChunkPos(columnTag.getLong("Pos")), state);
            }
            stateByDimension.put(dimensionKey, columns);
        }
    }
}
