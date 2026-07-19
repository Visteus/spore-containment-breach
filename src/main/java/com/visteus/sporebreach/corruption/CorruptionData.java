package com.visteus.sporebreach.corruption;

import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
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
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Storage facade for Goal #7's World Corruption value, following {@link
 * com.visteus.sporebreach.chunkloading.ChunkloadData}'s exact singleton/PersistedData pattern.
 * Per-dimension corruption value, clamped to {@code [0, corruptionCap]}.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class CorruptionData extends PersistedData {

    private static CorruptionData instance;

    private final Map<ResourceKey<Level>, Integer> valueByDimension = new HashMap<>();

    private CorruptionData() {
    }

    private static CorruptionData get() {
        if (instance == null) {
            instance = new CorruptionData();
            instance.initialize();
        }
        return instance;
    }

    public static int get(ServerLevel level) {
        return get().valueByDimension.getOrDefault(level.dimension(), 0);
    }

    public static void add(ServerLevel level, int amount) {
        if (amount == 0) {
            return;
        }
        set(level, get(level) + amount);
    }

    public static void subtract(ServerLevel level, int amount) {
        if (amount == 0) {
            return;
        }
        set(level, get(level) - amount);
    }

    public static void set(ServerLevel level, int value) {
        int cap = SporeBreachServerConfig.CORRUPTION_CAP.get();
        int clamped = Math.max(0, Math.min(cap, value));
        CorruptionData data = get();
        data.valueByDimension.put(level.dimension(), clamped);
        data.markDirty();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        instance = null;
    }

    @Override
    protected String fileName() {
        return "corruption.dat";
    }

    @Override
    protected CompoundTag save(CompoundTag tag) {
        ListTag dimensions = new ListTag();
        for (Map.Entry<ResourceKey<Level>, Integer> entry : valueByDimension.entrySet()) {
            CompoundTag dimensionTag = new CompoundTag();
            dimensionTag.putString("Dimension", entry.getKey().location().toString());
            dimensionTag.putInt("Value", entry.getValue());
            dimensions.add(dimensionTag);
        }
        tag.put("Dimensions", dimensions);
        return tag;
    }

    @Override
    protected void load(CompoundTag tag) {
        valueByDimension.clear();
        ListTag dimensions = tag.getList("Dimensions", Tag.TAG_COMPOUND);
        for (int i = 0; i < dimensions.size(); i++) {
            CompoundTag dimensionTag = dimensions.getCompound(i);
            ResourceKey<Level> dimensionKey = ResourceKey.create(
                    Registries.DIMENSION, ResourceLocation.parse(dimensionTag.getString("Dimension"))
            );
            valueByDimension.put(dimensionKey, dimensionTag.getInt("Value"));
        }
    }
}
