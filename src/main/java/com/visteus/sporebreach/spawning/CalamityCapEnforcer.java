package com.visteus.sporebreach.spawning;

import com.Harbinger.Spore.Sentities.BaseEntities.Calamity;
import com.mojang.logging.LogUtils;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import com.visteus.sporebreach.tracking.CalamityRegistry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

/**
 * Enforces Goal #2's global Calamity cap: once the tracked, server-wide Calamity count is at or
 * over {@code calamityCap}, silently discards whichever tracked Calamity is currently furthest
 * from any player - compared across all dimensions by each Calamity's own distance to its nearest
 * player, so a Calamity nobody is near always loses first. Called from
 * {@link com.visteus.sporebreach.tracking.CalamityTrackingEvents#onJoin} whenever the tracked
 * count grows.
 */
public final class CalamityCapEnforcer {

    private static final Logger LOGGER = LogUtils.getLogger();

    private CalamityCapEnforcer() {
    }

    public static void enforceCap() {
        if (!SporeBreachServerConfig.CALAMITY_CAP_ENABLED.get()) {
            return;
        }

        int cap = SporeBreachServerConfig.CALAMITY_CAP.get();
        List<Calamity> loaded = loadedOnly(CalamityRegistry.getAll());

        while (loaded.size() > cap) {
            Calamity furthest = findFurthestFromAnyPlayer(loaded);
            if (furthest == null) {
                break;
            }
            loaded.remove(furthest);
            LOGGER.debug(
                    "spore_containment_breach: Calamity cap ({}) exceeded, culling furthest-from-player Calamity {} at {}",
                    cap, furthest.getType(), furthest.blockPosition()
            );
            furthest.discard();
        }
    }

    /**
     * A Calamity whose chunk has since unloaded (no player nearby, not force-loaded) is still a
     * live Java object in {@link CalamityRegistry} - discard() on it is a no-op, since an unloaded
     * chunk reloads that entity fresh from disk rather than reusing this stale reference. Counting
     * or trying to cull such an entry would either waste a cull attempt that silently does nothing,
     * or worse, force a genuinely active Calamity to be culled in its place to compensate. The cap
     * is therefore only meaningful (and only enforceable) against currently loaded/ticking
     * Calamities - consistent with every other registry in this mod only reasoning about loaded
     * organoids.
     */
    private static List<Calamity> loadedOnly(List<Calamity> calamities) {
        List<Calamity> loaded = new ArrayList<>(calamities.size());
        for (Calamity calamity : calamities) {
            if (calamity.level() instanceof ServerLevel level && level.isLoaded(calamity.blockPosition())) {
                loaded.add(calamity);
            }
        }
        return loaded;
    }

    private static Calamity findFurthestFromAnyPlayer(List<Calamity> calamities) {
        Calamity furthest = null;
        double furthestDistSq = -1.0;

        for (Calamity calamity : calamities) {
            ServerLevel level = (ServerLevel) calamity.level();
            Player nearest = level.getNearestPlayer(calamity, -1.0);
            double distSq = nearest == null ? Double.MAX_VALUE : calamity.distanceToSqr(nearest);
            if (distSq > furthestDistSq) {
                furthestDistSq = distSq;
                furthest = calamity;
            }
        }

        return furthest;
    }
}
