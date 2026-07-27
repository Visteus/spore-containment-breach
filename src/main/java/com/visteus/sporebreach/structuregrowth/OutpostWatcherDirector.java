package com.visteus.sporebreach.structuregrowth;

import com.Harbinger.Spore.Sentities.BaseEntities.Organoid;
import com.mojang.logging.LogUtils;
import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import com.visteus.sporebreach.tracking.OrganoidRegistry;
import com.visteus.sporebreach.util.JitteredTimer;
import com.visteus.sporebreach.util.OrganoidDepthBias;
import com.visteus.sporebreach.util.OrganoidDistance;
import com.visteus.sporebreach.util.OrganoidDue;
import com.visteus.sporebreach.util.TimerJitter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

/**
 * Periodic driver for Outpost Watcher towers, using the same per-organoid due-stamp + overdue-first
 * sweep design as {@link StructureGrowthDirector} - see that class's javadoc for the reasoning.
 * Unlike structure growth, Mounds and Proto-Hiveminds share a single recheck/pass cadence and a
 * single {@code watcherBlocksPerSweep} budget here, since every other Outpost Watcher setting
 * already treats both organoid types identically. Deliberately *not* gated on
 * {@code structureGrowthMode}: watchers are a base Spore feature this mod is making more common, so
 * they still appear for players who have chosen to keep base Spore's own shell growth.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class OutpostWatcherDirector {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String RECHECK_AT_KEY = "sporebreach_watcher_recheck_at";
    private static final String PASS_AT_KEY = "sporebreach_watcher_pass_at";

    private static final JitteredTimer SWEEP_TIMER = new JitteredTimer();

    private OutpostWatcherDirector() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!event.getServer().tickRateManager().runsNormally()) {
            return;
        }
        if (!SporeBreachServerConfig.OUTPOST_WATCHER_ENABLED.get()) {
            return;
        }
        if (!SWEEP_TIMER.tick(SporeBreachServerConfig.GROWTH_SWEEP_INTERVAL_TICKS.get())) {
            return;
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            runSweep(level);
        }
    }

    private static void runSweep(ServerLevel level) {
        long now = level.getGameTime();
        int recheckInterval = SporeBreachServerConfig.OUTPOST_WATCHER_RECHECK_INTERVAL_TICKS.get();
        int passInterval = SporeBreachServerConfig.OUTPOST_WATCHER_PASS_INTERVAL_TICKS.get();

        List<OrganoidDue> candidates = new ArrayList<>();
        gatherInto(candidates, now, OrganoidRegistry.get(level), recheckInterval, passInterval);
        gatherInto(candidates, now, OrganoidRegistry.getProtos(level), recheckInterval, passInterval);
        if (candidates.isEmpty()) {
            return;
        }

        List<ServerPlayer> players = level.players();
        int depthPenalty = SporeBreachServerConfig.GROWTH_UNDERGROUND_DEPTH_PENALTY_TICKS_PER_BLOCK.get();
        candidates.sort(
                Comparator.<OrganoidDue>comparingLong(c -> OrganoidDepthBias.biasedDueAt(c, depthPenalty))
                        .thenComparingDouble(c -> OrganoidDistance.nearestPlayerDistanceSqr(c.organoid(), players))
        );

        int budget = SporeBreachServerConfig.WATCHER_BLOCKS_PER_SWEEP.get();
        int budgetStart = budget;
        for (OrganoidDue candidate : candidates) {
            budget = process(level, candidate.organoid(), now, budget);
        }

        LOGGER.debug(
                "sporebreach: watcher sweep in {} - {} candidate(s), blocks {}/{}",
                level.dimension().location(), candidates.size(), budgetStart - budget, budgetStart
        );
    }

    /** Seeds a fresh due stamp with a uniform random phase the first time an organoid is seen. */
    private static void gatherInto(
            List<OrganoidDue> out, long now, List<? extends Organoid> organoids, int recheckInterval, int passInterval
    ) {
        for (Organoid organoid : organoids) {
            CompoundTag data = organoid.getPersistentData();
            RandomSource random = organoid.getRandom();

            long recheckAt = data.getLong(RECHECK_AT_KEY);
            if (recheckAt == 0) {
                recheckAt = TimerJitter.firstDueAt(random, now, recheckInterval);
                data.putLong(RECHECK_AT_KEY, recheckAt);
            }
            long passAt = data.getLong(PASS_AT_KEY);
            if (passAt == 0) {
                passAt = TimerJitter.firstDueAt(random, now, passInterval);
                data.putLong(PASS_AT_KEY, passAt);
            }

            if (now >= recheckAt || now >= passAt) {
                out.add(new OrganoidDue(organoid, Math.min(recheckAt, passAt)));
            }
        }
    }

    private static int process(ServerLevel level, Organoid organoid, long now, int remainingBudget) {
        CompoundTag data = organoid.getPersistentData();
        RandomSource random = organoid.getRandom();

        if (now >= data.getLong(RECHECK_AT_KEY)) {
            OutpostWatcherGrowth.tryStartJob(level, organoid, false);
            data.putLong(RECHECK_AT_KEY,
                    TimerJitter.dueAt(random, now, SporeBreachServerConfig.OUTPOST_WATCHER_RECHECK_INTERVAL_TICKS.get()));
        }
        if (now >= data.getLong(PASS_AT_KEY)) {
            int blocksPerPass = SporeBreachServerConfig.OUTPOST_WATCHER_BLOCKS_PER_PASS.get();
            if (remainingBudget >= blocksPerPass) {
                OutpostWatcherGrowth.tryAdvanceJob(level, organoid);
                remainingBudget -= blocksPerPass;
                data.putLong(PASS_AT_KEY,
                        TimerJitter.dueAt(random, now, SporeBreachServerConfig.OUTPOST_WATCHER_PASS_INTERVAL_TICKS.get()));
            }
            // Over budget: leave the stamp due so it's more overdue (and sorts first) next sweep.
        }
        return remainingBudget;
    }
}
