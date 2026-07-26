package com.visteus.sporebreach.util;

import com.Harbinger.Spore.Sentities.BaseEntities.Organoid;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;

/**
 * Shared distance tiebreak for the mod's gather/sort/budget/dispatch directors ({@code
 * OrganoidSpawnDirector}, {@code StructureGrowthDirector}, {@code OutpostWatcherDirector}), lifted
 * out of {@code OrganoidSpawnDirector} so all three share one copy instead of duplicating it.
 */
public final class OrganoidDistance {

    private OrganoidDistance() {
    }

    public static double nearestPlayerDistanceSqr(Organoid organoid, List<ServerPlayer> players) {
        double best = Double.MAX_VALUE;
        for (ServerPlayer player : players) {
            double distSq = organoid.distanceToSqr(player);
            if (distSq < best) {
                best = distSq;
            }
        }
        return best;
    }
}
