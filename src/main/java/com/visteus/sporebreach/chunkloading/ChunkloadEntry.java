package com.visteus.sporebreach.chunkloading;

import com.mojang.logging.LogUtils;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

/**
 * One "ownerId|minRadius|maxRadius|tickingRadius|ticksToMaxRadius" config entry, shared by both
 * the entity-owner and block-owner config lists - this record intentionally doesn't resolve
 * {@code ownerId} against a registry itself (unlike SpawnPoolEntry) since the two lists resolve
 * against different registries; that lookup is done by the caller (ChunkloadEntryLookup).
 */
public record ChunkloadEntry(ResourceLocation ownerId, int minRadius, int maxRadius, int tickingRadius, int ticksToMaxRadius) {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static Optional<ChunkloadEntry> parse(String raw) {
        String[] parts = raw.split("\\|");
        if (parts.length != 5) {
            LOGGER.warn(
                    "spore_containment_breach: invalid chunkload entry (expected ownerId|minRadius|maxRadius|tickingRadius|ticksToMaxRadius): {}",
                    raw
            );
            return Optional.empty();
        }

        ResourceLocation ownerId;
        try {
            ownerId = ResourceLocation.parse(parts[0]);
        } catch (Exception e) {
            LOGGER.warn("spore_containment_breach: invalid owner id in chunkload entry: {}", raw);
            return Optional.empty();
        }

        int minRadius;
        int maxRadius;
        int tickingRadius;
        int ticksToMaxRadius;
        try {
            minRadius = Integer.parseInt(parts[1]);
            maxRadius = Integer.parseInt(parts[2]);
            tickingRadius = Integer.parseInt(parts[3]);
            ticksToMaxRadius = Integer.parseInt(parts[4]);
        } catch (NumberFormatException e) {
            LOGGER.warn("spore_containment_breach: invalid number in chunkload entry: {}", raw);
            return Optional.empty();
        }

        int clampedMin = Math.max(1, Math.min(minRadius, ChunkCircleOffsets.MAX_RADIUS));
        if (clampedMin != minRadius) {
            LOGGER.warn(
                    "spore_containment_breach: chunkload entry {} minRadius {} clamped to {} (must be 1-{})",
                    ownerId, minRadius, clampedMin, ChunkCircleOffsets.MAX_RADIUS
            );
        }

        int clampedMax = Math.max(clampedMin, Math.min(maxRadius, ChunkCircleOffsets.MAX_RADIUS));
        if (clampedMax != maxRadius) {
            LOGGER.warn(
                    "spore_containment_breach: chunkload entry {} maxRadius {} clamped to {} (must be {}-{})",
                    ownerId, maxRadius, clampedMax, clampedMin, ChunkCircleOffsets.MAX_RADIUS
            );
        }

        int clampedTicking = Math.max(1, Math.min(tickingRadius, clampedMax));
        if (clampedTicking != tickingRadius) {
            LOGGER.warn(
                    "spore_containment_breach: chunkload entry {} tickingRadius {} clamped to {} (must be 1-{})",
                    ownerId, tickingRadius, clampedTicking, clampedMax
            );
        }

        int clampedTicks = Math.max(1, ticksToMaxRadius);
        if (clampedTicks != ticksToMaxRadius) {
            LOGGER.warn(
                    "spore_containment_breach: chunkload entry {} ticksToMaxRadius {} clamped to {} (must be >=1)",
                    ownerId, ticksToMaxRadius, clampedTicks
            );
        }

        return Optional.of(new ChunkloadEntry(ownerId, clampedMin, clampedMax, clampedTicking, clampedTicks));
    }
}
