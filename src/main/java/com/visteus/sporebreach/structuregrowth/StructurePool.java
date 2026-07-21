package com.visteus.sporebreach.structuregrowth;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.util.RandomSource;

/**
 * A parsed, weighted structure pool built from a "structureId|weight" config list. The structure
 * counterpart of {@link com.visteus.sporebreach.spawning.SpawnPool}.
 */
public final class StructurePool {

    private final List<StructurePoolEntry> entries;
    private final int totalWeight;

    private StructurePool(List<StructurePoolEntry> entries) {
        this.entries = entries;
        int sum = 0;
        for (StructurePoolEntry entry : entries) {
            sum += Math.max(entry.weight(), 0);
        }
        this.totalWeight = sum;
    }

    public static StructurePool fromConfig(List<? extends String> raw) {
        List<StructurePoolEntry> parsed = new ArrayList<>();
        for (String rawEntry : raw) {
            StructurePoolEntry.parse(rawEntry).ifPresent(parsed::add);
        }
        return new StructurePool(parsed);
    }

    public boolean isEmpty() {
        return entries.isEmpty() || totalWeight <= 0;
    }

    public Optional<StructurePoolEntry> pickWeighted(RandomSource random) {
        if (isEmpty()) {
            return Optional.empty();
        }

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (StructurePoolEntry entry : entries) {
            cumulative += Math.max(entry.weight(), 0);
            if (roll < cumulative) {
                return Optional.of(entry);
            }
        }
        return Optional.of(entries.get(entries.size() - 1));
    }
}
