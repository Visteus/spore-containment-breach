package com.visteus.sporebreach.chunkloading;

import com.mojang.logging.LogUtils;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;

/**
 * Memoized resolution of Goal #4's config-list entries to {@link ChunkloadEntry}, keyed by
 * {@link EntityType} for {@code chunkloadEntityOwners} and by block {@link ResourceLocation} for
 * {@code chunkloadBlockOwners}. Re-parses only when the config's raw list reference changes,
 * since (unlike SpawnPool) this is consulted from EntityJoinLevelEvent - fired for every entity
 * joining every level, every tick.
 */
public final class ChunkloadEntryLookup {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static List<? extends String> lastEntityRaw;
    private static Map<EntityType<?>, ChunkloadEntry> entityMap = Map.of();

    private static List<? extends String> lastBlockRaw;
    private static Map<ResourceLocation, ChunkloadEntry> blockMap = Map.of();

    private ChunkloadEntryLookup() {
    }

    public static ChunkloadEntry forEntityType(EntityType<?> type) {
        refreshEntityMapIfStale();
        return entityMap.get(type);
    }

    public static ChunkloadEntry forBlockId(ResourceLocation blockId) {
        refreshBlockMapIfStale();
        return blockMap.get(blockId);
    }

    private static void refreshEntityMapIfStale() {
        List<? extends String> raw = SporeBreachServerConfig.CHUNKLOAD_ENTITY_OWNERS.get();
        if (raw == lastEntityRaw) {
            return;
        }
        lastEntityRaw = raw;

        Map<EntityType<?>, ChunkloadEntry> map = new HashMap<>();
        for (String line : raw) {
            ChunkloadEntry.parse(line).ifPresent(entry -> {
                Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(entry.ownerId());
                if (type.isEmpty()) {
                    LOGGER.warn("sporebreach: unknown entity type in chunkloadEntityOwners: {}", entry.ownerId());
                    return;
                }
                map.put(type.get(), entry);
            });
        }
        entityMap = Map.copyOf(map);
    }

    private static void refreshBlockMapIfStale() {
        List<? extends String> raw = SporeBreachServerConfig.CHUNKLOAD_BLOCK_OWNERS.get();
        if (raw == lastBlockRaw) {
            return;
        }
        lastBlockRaw = raw;

        Map<ResourceLocation, ChunkloadEntry> map = new HashMap<>();
        for (String line : raw) {
            ChunkloadEntry.parse(line).ifPresent(entry -> {
                if (!BuiltInRegistries.BLOCK.containsKey(entry.ownerId())) {
                    LOGGER.warn("sporebreach: unknown block id in chunkloadBlockOwners: {}", entry.ownerId());
                    return;
                }
                map.put(entry.ownerId(), entry);
            });
        }
        blockMap = Map.copyOf(map);
    }
}
