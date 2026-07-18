package com.visteus.sporebreach.spawning;

import com.Harbinger.Spore.Sentities.BaseEntities.Organoid;
import com.Harbinger.Spore.Sentities.Organoids.Mound;
import com.Harbinger.Spore.Sentities.Organoids.Proto;
import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import com.visteus.sporebreach.tracking.OrganoidRegistry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Central dispatcher for Goal #1's new organoid-driven spawning: every
 * coordinatorTickIntervalTicks, per level, gathers organoids whose own cooldown has expired,
 * sorts them closest-to-any-player first, and dispatches up to coordinatorBudgetPerCycle of them
 * to MoundDefenseSpawner/ProtoRaidDirector. Deliberately contains no Mound/Proto-specific spawn
 * logic itself, so Goal #2 (despawning) and Goal #4 (chunkloading) can reuse this same
 * gather/sort/budget/dispatch skeleton against the same OrganoidRegistry.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class OrganoidSpawnCoordinator {

    private static long tickCounter;

    private OrganoidSpawnCoordinator() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        int interval = SporeBreachServerConfig.COORDINATOR_TICK_INTERVAL_TICKS.get();
        if (interval <= 0 || tickCounter % interval != 0) {
            return;
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            runCycle(level);
        }
    }

    private static void runCycle(ServerLevel level) {
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) {
            return;
        }

        List<Organoid> eligible = new ArrayList<>();
        for (Mound mound : OrganoidRegistry.get(level)) {
            if (MoundDefenseSpawner.isEligible(mound)) {
                eligible.add(mound);
            }
        }
        for (Proto proto : OrganoidRegistry.getProtos(level)) {
            if (ProtoRaidDirector.isEligible(proto)) {
                eligible.add(proto);
            }
        }
        if (eligible.isEmpty()) {
            return;
        }

        eligible.sort(Comparator.comparingDouble(organoid -> nearestPlayerDistanceSqr(organoid, players)));

        int budget = SporeBreachServerConfig.COORDINATOR_BUDGET_PER_CYCLE.get();
        int dispatched = 0;
        for (Organoid organoid : eligible) {
            if (dispatched >= budget) {
                break;
            }
            if (organoid instanceof Mound mound) {
                MoundDefenseSpawner.attempt(mound);
            } else if (organoid instanceof Proto proto) {
                ProtoRaidDirector.attempt(proto);
            }
            dispatched++;
        }
    }

    private static double nearestPlayerDistanceSqr(Organoid organoid, List<ServerPlayer> players) {
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
