package com.visteus.sporebreach.chunkloading;

import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.persistence.PersistedData;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
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
 * Storage facade for Goal #4's chunkload growth state, following {@link
 * com.visteus.sporebreach.genesis.MoundGenesisData}'s exact singleton/PersistedData pattern.
 * Per-dimension map of {@link ChunkloadOwnerId} to {@link ChunkloadState}. {@link ChunkloadManager}
 * never touches state directly - every mutation here calls {@code markDirty()}.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class ChunkloadData extends PersistedData {

    private static ChunkloadData instance;

    private final Map<ResourceKey<Level>, Map<ChunkloadOwnerId, ChunkloadState>> stateByDimension = new HashMap<>();

    private ChunkloadData() {
    }

    private static ChunkloadData get() {
        if (instance == null) {
            instance = new ChunkloadData();
            instance.initialize();
        }
        return instance;
    }

    public static boolean isTracked(ServerLevel level, ChunkloadOwnerId ownerId) {
        Map<ChunkloadOwnerId, ChunkloadState> owners = get().stateByDimension.get(level.dimension());
        return owners != null && owners.containsKey(ownerId);
    }

    public static ChunkloadState getState(ServerLevel level, ChunkloadOwnerId ownerId) {
        Map<ChunkloadOwnerId, ChunkloadState> owners = get().stateByDimension.get(level.dimension());
        return owners == null ? null : owners.get(ownerId);
    }

    /**
     * Defensive copy so teardown during iteration (recheckAll removing a stale owner) is safe.
     */
    public static Map<ChunkloadOwnerId, ChunkloadState> snapshot(ServerLevel level) {
        Map<ChunkloadOwnerId, ChunkloadState> owners = get().stateByDimension.get(level.dimension());
        return owners == null ? Map.of() : new HashMap<>(owners);
    }

    public static void activate(
            ServerLevel level, ChunkloadOwnerId ownerId, ChunkPos anchorChunk, long activationGameTime, ResourceLocation expectedBlockId
    ) {
        ChunkloadData data = get();
        data.stateByDimension.computeIfAbsent(level.dimension(), key -> new HashMap<>())
                .put(ownerId, new ChunkloadState(anchorChunk, activationGameTime, 0, expectedBlockId));
        data.markDirty();
    }

    public static void updateLastIssuedRadius(ServerLevel level, ChunkloadOwnerId ownerId, int newRadius) {
        ChunkloadState state = getState(level, ownerId);
        if (state == null) {
            return;
        }
        state.setLastIssuedRadius(newRadius);
        get().markDirty();
    }

    public static void remove(ServerLevel level, ChunkloadOwnerId ownerId) {
        ChunkloadData data = get();
        Map<ChunkloadOwnerId, ChunkloadState> owners = data.stateByDimension.get(level.dimension());
        if (owners != null && owners.remove(ownerId) != null) {
            data.markDirty();
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        instance = null;
    }

    @Override
    protected String fileName() {
        return "chunkload.dat";
    }

    @Override
    protected CompoundTag save(CompoundTag tag) {
        ListTag dimensions = new ListTag();
        for (Map.Entry<ResourceKey<Level>, Map<ChunkloadOwnerId, ChunkloadState>> dimEntry : stateByDimension.entrySet()) {
            CompoundTag dimensionTag = new CompoundTag();
            dimensionTag.putString("Dimension", dimEntry.getKey().location().toString());

            ListTag owners = new ListTag();
            for (Map.Entry<ChunkloadOwnerId, ChunkloadState> ownerEntry : dimEntry.getValue().entrySet()) {
                CompoundTag ownerTag = new CompoundTag();
                ChunkloadOwnerId ownerId = ownerEntry.getKey();
                if (ownerId instanceof ChunkloadOwnerId.EntityOwner entityOwner) {
                    ownerTag.putString("Type", "entity");
                    ownerTag.putUUID("Uuid", entityOwner.entityId());
                } else if (ownerId instanceof ChunkloadOwnerId.BlockOwner blockOwner) {
                    ownerTag.putString("Type", "block");
                    ownerTag.put("Pos", NbtUtils.writeBlockPos(blockOwner.pos()));
                }

                ChunkloadState state = ownerEntry.getValue();
                ownerTag.putLong("AnchorChunk", state.anchorChunk().toLong());
                ownerTag.putLong("ActivationGameTime", state.activationGameTime());
                ownerTag.putInt("LastIssuedRadius", state.lastIssuedRadius());
                if (state.expectedBlockId() != null) {
                    ownerTag.putString("ExpectedBlockId", state.expectedBlockId().toString());
                }
                owners.add(ownerTag);
            }
            dimensionTag.put("Owners", owners);
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

            Map<ChunkloadOwnerId, ChunkloadState> owners = new HashMap<>();
            ListTag ownerList = dimensionTag.getList("Owners", Tag.TAG_COMPOUND);
            for (int j = 0; j < ownerList.size(); j++) {
                CompoundTag ownerTag = ownerList.getCompound(j);
                ChunkloadOwnerId ownerId;
                if ("entity".equals(ownerTag.getString("Type"))) {
                    ownerId = new ChunkloadOwnerId.EntityOwner(ownerTag.getUUID("Uuid"));
                } else if ("block".equals(ownerTag.getString("Type"))) {
                    Optional<BlockPos> pos = NbtUtils.readBlockPos(ownerTag, "Pos");
                    if (pos.isEmpty()) {
                        continue;
                    }
                    ownerId = new ChunkloadOwnerId.BlockOwner(pos.get());
                } else {
                    continue;
                }

                ChunkPos anchorChunk = new ChunkPos(ownerTag.getLong("AnchorChunk"));
                long activationGameTime = ownerTag.getLong("ActivationGameTime");
                int lastIssuedRadius = ownerTag.getInt("LastIssuedRadius");
                ResourceLocation expectedBlockId = ownerTag.contains("ExpectedBlockId")
                        ? ResourceLocation.parse(ownerTag.getString("ExpectedBlockId"))
                        : null;
                owners.put(ownerId, new ChunkloadState(anchorChunk, activationGameTime, lastIssuedRadius, expectedBlockId));
            }
            stateByDimension.put(dimensionKey, owners);
        }
    }
}
