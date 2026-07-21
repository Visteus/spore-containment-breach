package com.visteus.sporebreach.structuregrowth;

import com.mojang.logging.LogUtils;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

/**
 * One "structureId|weight" entry - the structure-pool counterpart of
 * {@link com.visteus.sporebreach.spawning.SpawnPoolEntry}'s "entityId|weight|min|max" grammar.
 */
public record StructurePoolEntry(ResourceLocation structureId, int weight) {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static Optional<StructurePoolEntry> parse(String raw) {
        String[] parts = raw.split("\\|");
        if (parts.length != 2) {
            LOGGER.warn("sporebreach: invalid structure pool entry (expected structureId|weight): {}", raw);
            return Optional.empty();
        }

        ResourceLocation id;
        try {
            id = ResourceLocation.parse(parts[0]);
        } catch (Exception e) {
            LOGGER.warn("sporebreach: invalid structure id in structure pool entry: {}", raw);
            return Optional.empty();
        }

        try {
            int weight = Integer.parseInt(parts[1]);
            return Optional.of(new StructurePoolEntry(id, weight));
        } catch (NumberFormatException e) {
            LOGGER.warn("sporebreach: invalid weight in structure pool entry: {}", raw);
            return Optional.empty();
        }
    }
}
