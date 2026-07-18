package com.visteus.sporebreach.spawning;

import com.mojang.logging.LogUtils;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;

/**
 * One "entityId|weight|min|max" entry, the same grammar Spore's own SConfig.SERVER.spawns and
 * structure_spawns use (see BiomeModification.addSpawns).
 */
public record SpawnPoolEntry(EntityType<?> type, int weight, int min, int max) {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static Optional<SpawnPoolEntry> parse(String raw) {
        String[] parts = raw.split("\\|");
        if (parts.length != 4) {
            LOGGER.warn("spore_containment_breach: invalid spawn pool entry (expected entityId|weight|min|max): {}", raw);
            return Optional.empty();
        }

        ResourceLocation id;
        try {
            id = ResourceLocation.parse(parts[0]);
        } catch (Exception e) {
            LOGGER.warn("spore_containment_breach: invalid entity id in spawn pool entry: {}", raw);
            return Optional.empty();
        }

        Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id);
        if (type.isEmpty()) {
            LOGGER.warn("spore_containment_breach: unknown entity type in spawn pool entry: {}", raw);
            return Optional.empty();
        }

        try {
            int weight = Integer.parseInt(parts[1]);
            int min = Integer.parseInt(parts[2]);
            int max = Integer.parseInt(parts[3]);
            return Optional.of(new SpawnPoolEntry(type.get(), weight, min, max));
        } catch (NumberFormatException e) {
            LOGGER.warn("spore_containment_breach: invalid number in spawn pool entry: {}", raw);
            return Optional.empty();
        }
    }
}
