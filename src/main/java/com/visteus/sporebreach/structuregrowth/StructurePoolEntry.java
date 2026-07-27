package com.visteus.sporebreach.structuregrowth;

import com.mojang.logging.LogUtils;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

/**
 * One "structureId|weight" or "structureId|weight|verticalOffset" entry - the structure-pool
 * counterpart of {@link com.visteus.sporebreach.spawning.SpawnPoolEntry}'s "entityId|weight|min|max"
 * grammar. {@code verticalOffset} defaults to 0 when omitted, and shifts the anchor used for the
 * organoid's first/next-placed surface structure up (positive) or down (negative).
 */
public record StructurePoolEntry(ResourceLocation structureId, int weight, int verticalOffset) {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static Optional<StructurePoolEntry> parse(String raw) {
        String[] parts = raw.split("\\|");
        if (parts.length != 2 && parts.length != 3) {
            LOGGER.warn("sporebreach: invalid structure pool entry (expected structureId|weight or structureId|weight|verticalOffset): {}", raw);
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
            int verticalOffset = parts.length == 3 ? Integer.parseInt(parts[2]) : 0;
            return Optional.of(new StructurePoolEntry(id, weight, verticalOffset));
        } catch (NumberFormatException e) {
            LOGGER.warn("sporebreach: invalid weight or vertical offset in structure pool entry: {}", raw);
            return Optional.empty();
        }
    }
}
