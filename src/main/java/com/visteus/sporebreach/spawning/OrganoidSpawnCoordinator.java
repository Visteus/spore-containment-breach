package com.visteus.sporebreach.spawning;

import com.Harbinger.Spore.Sentities.BaseEntities.Organoid;
import com.Harbinger.Spore.Sentities.Organoids.Mound;
import com.Harbinger.Spore.Sentities.Organoids.Proto;
import com.mojang.logging.LogUtils;
import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import com.visteus.sporebreach.tracking.OrganoidRegistry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

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

    private static final Logger LOGGER = LogUtils.getLogger();

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
        List<Mound> mounds = OrganoidRegistry.get(level);
        for (Mound mound : mounds) {
            OrganoidEligibility reason = MoundDefenseSpawner.checkEligibility(mound);
            if (reason == OrganoidEligibility.ELIGIBLE) {
                eligible.add(mound);
            } else {
                LOGGER.info(
                        "spore_containment_breach: Mound at {} excluded from cycle - {}",
                        mound.getOnPos(), reason
                );
            }
        }
        List<Proto> protos = OrganoidRegistry.getProtos(level);
        for (Proto proto : protos) {
            OrganoidEligibility reason = ProtoRaidDirector.checkEligibility(proto);
            if (reason == OrganoidEligibility.ELIGIBLE) {
                eligible.add(proto);
            } else {
                LOGGER.info(
                        "spore_containment_breach: Proto at {} excluded from cycle - {}",
                        proto.getOnPos(), reason
                );
            }
        }
        if (eligible.isEmpty()) {
            if (!mounds.isEmpty() || !protos.isEmpty()) {
                LOGGER.info(
                        "spore_containment_breach: cycle in {} - 0 eligible organoid(s) out of {} tracked",
                        level.dimension().location(), mounds.size() + protos.size()
                );
            }
            return;
        }

        eligible.sort(Comparator.comparingDouble(organoid -> nearestPlayerDistanceSqr(organoid, players)));

        int budget = SporeBreachServerConfig.COORDINATOR_BUDGET_PER_CYCLE.get();
        LOGGER.info(
                "spore_containment_breach: cycle in {} - {} eligible organoid(s) out of {} tracked, budget {}",
                level.dimension().location(), eligible.size(), mounds.size() + protos.size(), budget
        );
        for (int i = 0; i < eligible.size(); i++) {
            Organoid organoid = eligible.get(i);
            double dist = Math.sqrt(nearestPlayerDistanceSqr(organoid, players));
            BlockPos pos = organoid.getOnPos();
            LOGGER.info(
                    "spore_containment_breach:   [{}] {} at {} - {} blocks from nearest player{}",
                    i, organoid.getClass().getSimpleName(), pos, String.format("%.1f", dist),
                    i < budget ? " -> DISPATCHED" : " (skipped, over budget)"
            );
        }

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
