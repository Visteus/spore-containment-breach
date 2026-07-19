package com.visteus.sporebreach.genesis;

import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.persistence.PersistedData;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Tracks, per dimension, which structure-genesis anchors (Goal #1 "Mound genesis" plan) have
 * already produced their one genesis event, so a permanently-cleared structure stays cleared
 * across server restarts. Persisted via {@link PersistedData} into {@code .spore-breach/}
 * rather than a vanilla SavedData file. All 12 eligible structures are Overworld-only per wiki
 * research, but this keys by dimension anyway rather than assuming that never changes.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class MoundGenesisData extends PersistedData {

    private static MoundGenesisData instance;

    private final Map<ResourceKey<Level>, LongSet> seededByDimension = new HashMap<>();

    private MoundGenesisData() {
    }

    private static MoundGenesisData get() {
        if (instance == null) {
            instance = new MoundGenesisData();
            instance.initialize();
        }
        return instance;
    }

    public static boolean hasSeeded(ServerLevel level, BlockPos anchor) {
        LongSet seeded = get().seededByDimension.get(level.dimension());
        return seeded != null && seeded.contains(anchor.asLong());
    }

    public static void markSeeded(ServerLevel level, BlockPos anchor) {
        MoundGenesisData data = get();
        data.seededByDimension.computeIfAbsent(level.dimension(), key -> new LongOpenHashSet()).add(anchor.asLong());
        data.markDirty();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        instance = null;
    }

    @Override
    protected String fileName() {
        return "mound_genesis.dat";
    }

    @Override
    protected CompoundTag save(CompoundTag tag) {
        ListTag dimensions = new ListTag();
        for (Map.Entry<ResourceKey<Level>, LongSet> entry : seededByDimension.entrySet()) {
            CompoundTag dimensionTag = new CompoundTag();
            dimensionTag.putString("Dimension", entry.getKey().location().toString());
            dimensionTag.putLongArray("Anchors", entry.getValue().toLongArray());
            dimensions.add(dimensionTag);
        }
        tag.put("Dimensions", dimensions);
        return tag;
    }

    @Override
    protected void load(CompoundTag tag) {
        seededByDimension.clear();
        ListTag dimensions = tag.getList("Dimensions", Tag.TAG_COMPOUND);
        for (int i = 0; i < dimensions.size(); i++) {
            CompoundTag dimensionTag = dimensions.getCompound(i);
            ResourceKey<Level> key = ResourceKey.create(
                    Registries.DIMENSION, ResourceLocation.parse(dimensionTag.getString("Dimension"))
            );
            seededByDimension.put(key, new LongOpenHashSet(dimensionTag.getLongArray("Anchors")));
        }
    }
}
