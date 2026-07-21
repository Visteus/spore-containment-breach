package com.visteus.sporebreach.spawning;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.util.RandomSource;
import org.slf4j.Logger;

/**
 * A parsed, weighted spawn pool built from a "entityId|weight|min|max" config list. When
 * {@code banCalamitiesAndWombs} is true, entries tagged
 * {@link ForbiddenSpawnTags#CALAMITIES_AND_WOMBS} are stripped at load time rather than trusted -
 * this is the structural enforcement of "Mounds/regular raids can never spawn calamities or
 * Wombs" (see the Goal #1 plan, Phase 4).
 */
public final class SpawnPool {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final List<SpawnPoolEntry> entries;
    private final int totalWeight;

    private SpawnPool(List<SpawnPoolEntry> entries) {
        this.entries = entries;
        int sum = 0;
        for (SpawnPoolEntry entry : entries) {
            sum += Math.max(entry.weight(), 0);
        }
        this.totalWeight = sum;
    }

    public static SpawnPool fromConfig(List<? extends String> raw, boolean banCalamitiesAndWombs, String logContext) {
        List<SpawnPoolEntry> parsed = new ArrayList<>();
        for (String rawEntry : raw) {
            SpawnPoolEntry.parse(rawEntry).ifPresent(entry -> {
                if (banCalamitiesAndWombs && entry.type().is(ForbiddenSpawnTags.CALAMITIES_AND_WOMBS)) {
                    LOGGER.warn(
                            "sporebreach: stripped forbidden calamity/womb entry from {}: {}", logContext, rawEntry
                    );
                    return;
                }
                parsed.add(entry);
            });
        }
        return new SpawnPool(parsed);
    }

    public boolean isEmpty() {
        return entries.isEmpty() || totalWeight <= 0;
    }

    public Optional<SpawnPoolEntry> pickWeighted(RandomSource random) {
        if (isEmpty()) {
            return Optional.empty();
        }

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (SpawnPoolEntry entry : entries) {
            cumulative += Math.max(entry.weight(), 0);
            if (roll < cumulative) {
                return Optional.of(entry);
            }
        }
        return Optional.of(entries.get(entries.size() - 1));
    }

    public int pickCount(RandomSource random, SpawnPoolEntry entry) {
        int min = Math.min(entry.min(), entry.max());
        int max = Math.max(entry.min(), entry.max());
        return min + random.nextInt(max - min + 1);
    }
}
