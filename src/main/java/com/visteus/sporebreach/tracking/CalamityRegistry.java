package com.visteus.sporebreach.tracking;

import com.Harbinger.Spore.Sentities.BaseEntities.Calamity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Memory-only, per-level registry of live Calamity instances, populated by
 * {@link CalamityTrackingEvents} - mirrors {@link OrganoidRegistry}'s exact Mound-tracking shape,
 * kept separate since Calamity isn't part of the Organoid hierarchy. Backs
 * {@link com.visteus.sporebreach.spawning.CalamityCapEnforcer}'s global (cross-dimension) cap via
 * {@link #getAll()}.
 */
public final class CalamityRegistry {

    private static final Map<ResourceKey<Level>, List<Calamity>> CALAMITIES_BY_LEVEL = new HashMap<>();

    private CalamityRegistry() {
    }

    public static void add(ServerLevel level, Calamity calamity) {
        List<Calamity> calamities = CALAMITIES_BY_LEVEL.computeIfAbsent(level.dimension(), key -> new ArrayList<>());
        // EntityJoinLevelEvent can refire for the same entity (e.g. its chunk reloading after
        // being unloaded without a matching leave event ever reaching #remove) - guard against
        // double-tracking the same live instance rather than trusting join/leave to always net out.
        if (!calamities.contains(calamity)) {
            calamities.add(calamity);
        }
    }

    public static void remove(ServerLevel level, Calamity calamity) {
        List<Calamity> calamities = CALAMITIES_BY_LEVEL.get(level.dimension());
        if (calamities != null) {
            calamities.remove(calamity);
        }
    }

    public static List<Calamity> get(ServerLevel level) {
        return CALAMITIES_BY_LEVEL.getOrDefault(level.dimension(), Collections.emptyList());
    }

    public static List<Calamity> getAll() {
        List<Calamity> all = new ArrayList<>();
        for (List<Calamity> calamities : CALAMITIES_BY_LEVEL.values()) {
            all.addAll(calamities);
        }
        return all;
    }
}
