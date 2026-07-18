package com.visteus.sporebreach.tracking;

import com.Harbinger.Spore.ExtremelySusThings.SporeSavedData;
import com.Harbinger.Spore.Sentities.Organoids.Mound;
import com.Harbinger.Spore.Sentities.Organoids.Proto;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Memory-only, per-level registry of live Mound instances, populated by
 * {@link OrganoidTrackingEvents} via join/leave events - mirroring Spore's own
 * {@link SporeSavedData#getHiveminds(ServerLevel)} registry for Protos, which is reused
 * directly here rather than duplicated. Deliberately not persisted to disk: see the "Persistence:
 * memory-only vs. disk-backed" note in the Goal #1 plan for the reasoning.
 */
public final class OrganoidRegistry {

    private static final Map<ResourceKey<Level>, List<Mound>> MOUNDS_BY_LEVEL = new HashMap<>();

    private OrganoidRegistry() {
    }

    public static void add(ServerLevel level, Mound mound) {
        MOUNDS_BY_LEVEL.computeIfAbsent(level.dimension(), key -> new ArrayList<>()).add(mound);
    }

    public static void remove(ServerLevel level, Mound mound) {
        List<Mound> mounds = MOUNDS_BY_LEVEL.get(level.dimension());
        if (mounds != null) {
            mounds.remove(mound);
        }
    }

    public static List<Mound> get(ServerLevel level) {
        return MOUNDS_BY_LEVEL.getOrDefault(level.dimension(), Collections.emptyList());
    }

    public static List<Proto> getProtos(ServerLevel level) {
        return SporeSavedData.getHiveminds(level);
    }
}
