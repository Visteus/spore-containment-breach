package com.visteus.sporebreach.structuregrowth;

import com.Harbinger.Spore.Sentities.BaseEntities.Organoid;
import com.Harbinger.Spore.Sentities.Organoids.Mound;
import com.Harbinger.Spore.Sentities.Organoids.Proto;
import com.mojang.logging.LogUtils;
import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import com.visteus.sporebreach.config.SporeBreachServerConfig.StructureGrowthMode;
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
 * Periodic driver for Goal #3's structure growth. Every {@code growth.sweepIntervalTicks}, gathers
 * every Mound/Proto-Hivemind whose own recheck or pass is due (tracked as absolute game-time stamps
 * in entity NBT, jittered independently per organoid so they don't stay phase-locked), sorts them
 * most-overdue-first with distance-to-nearest-player as a tiebreak, and
 * dispatches until a per-sweep block budget runs out. An organoid skipped for budget simply stays
 * due - being more overdue, it sorts first next sweep instead of being starved - which is why this
 * uses overdue-first rather than {@link com.visteus.sporebreach.spawning.OrganoidSpawnDirector}'s
 * closest-first: a hard budget with closest-first would let the nearest handful of organoids take
 * every sweep forever, so a distant Mound's fortress would never advance. Mound and Proto-Hivemind
 * structures keep separate block budgets, since they're already tuned independently (different
 * recheck/pass intervals, a 4-10x gap in blocksPerPass) - unlike Outpost Watchers, which share one
 * budget because every other watcher setting already treats both organoid types identically. No-ops
 * entirely when structureGrowthMode isn't SPORE_BREACH_TOWERS - see {@code MoundStructureMixin} and
 * {@code ProtoMixin#sporebreach$gateBaseCasingGrowth} for the base-game side of that switch.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class StructureGrowthDirector {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String RECHECK_AT_KEY = "sporebreach_struct_recheck_at";
    private static final String PASS_AT_KEY = "sporebreach_struct_pass_at";

    private static final JitteredTimer SWEEP_TIMER = new JitteredTimer();

    private StructureGrowthDirector() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!event.getServer().tickRateManager().runsNormally()) {
            return;
        }
        if (SporeBreachServerConfig.STRUCTURE_GROWTH_MODE.get() != StructureGrowthMode.SPORE_BREACH_TOWERS) {
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

        List<OrganoidDue> candidates = new ArrayList<>();
        candidates.addAll(gather(
                now, OrganoidRegistry.get(level),
                SporeBreachServerConfig.MOUND_STRUCTURE_RECHECK_INTERVAL_TICKS.get(),
                SporeBreachServerConfig.MOUND_STRUCTURE_PASS_INTERVAL_TICKS.get()
        ));
        candidates.addAll(gather(
                now, OrganoidRegistry.getProtos(level),
                SporeBreachServerConfig.PROTO_STRUCTURE_RECHECK_INTERVAL_TICKS.get(),
                SporeBreachServerConfig.PROTO_STRUCTURE_PASS_INTERVAL_TICKS.get()
        ));
        if (candidates.isEmpty()) {
            return;
        }

        List<ServerPlayer> players = level.players();
        int depthPenalty = SporeBreachServerConfig.GROWTH_UNDERGROUND_DEPTH_PENALTY_TICKS_PER_BLOCK.get();
        candidates.sort(
                Comparator.<OrganoidDue>comparingLong(c -> OrganoidDepthBias.biasedDueAt(c, depthPenalty))
                        .thenComparingDouble(c -> OrganoidDistance.nearestPlayerDistanceSqr(c.organoid(), players))
        );

        int moundBudget = SporeBreachServerConfig.MOUND_STRUCTURE_BLOCKS_PER_SWEEP.get();
        int protoBudget = SporeBreachServerConfig.PROTO_STRUCTURE_BLOCKS_PER_SWEEP.get();
        int moundBudgetStart = moundBudget;
        int protoBudgetStart = protoBudget;

        for (OrganoidDue candidate : candidates) {
            if (candidate.organoid() instanceof Mound mound) {
                moundBudget = processMound(level, mound, now, moundBudget);
            } else if (candidate.organoid() instanceof Proto proto) {
                protoBudget = processProto(level, proto, now, protoBudget);
            }
        }

        LOGGER.debug(
                "sporebreach: structure sweep in {} - {} candidate(s), Mound blocks {}/{}, Proto blocks {}/{}",
                level.dimension().location(), candidates.size(),
                moundBudgetStart - moundBudget, moundBudgetStart, protoBudgetStart - protoBudget, protoBudgetStart
        );
    }

    /** Seeds a fresh due stamp with a uniform random phase the first time an organoid is seen. */
    private static List<OrganoidDue> gather(
            long now, List<? extends Organoid> organoids, int recheckInterval, int passInterval
    ) {
        List<OrganoidDue> due = new ArrayList<>();
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
                due.add(new OrganoidDue(organoid, Math.min(recheckAt, passAt)));
            }
        }
        return due;
    }

    private static int processMound(ServerLevel level, Mound mound, long now, int remainingBudget) {
        CompoundTag data = mound.getPersistentData();
        RandomSource random = mound.getRandom();

        if (now >= data.getLong(RECHECK_AT_KEY)) {
            MoundStructureGrowth.tryStartJob(level, mound);
            data.putLong(RECHECK_AT_KEY,
                    TimerJitter.dueAt(random, now, SporeBreachServerConfig.MOUND_STRUCTURE_RECHECK_INTERVAL_TICKS.get()));
        }
        if (now >= data.getLong(PASS_AT_KEY)) {
            int blocksPerPass = SporeBreachServerConfig.MOUND_STRUCTURE_BLOCKS_PER_PASS.get();
            if (remainingBudget >= blocksPerPass) {
                MoundStructureGrowth.tryAdvanceJob(level, mound);
                remainingBudget -= blocksPerPass;
                data.putLong(PASS_AT_KEY,
                        TimerJitter.dueAt(random, now, SporeBreachServerConfig.MOUND_STRUCTURE_PASS_INTERVAL_TICKS.get()));
            }
            // Over budget: leave the stamp due so it's more overdue (and sorts first) next sweep.
        }
        return remainingBudget;
    }

    private static int processProto(ServerLevel level, Proto proto, long now, int remainingBudget) {
        CompoundTag data = proto.getPersistentData();
        RandomSource random = proto.getRandom();

        if (now >= data.getLong(RECHECK_AT_KEY)) {
            ProtoStructureGrowth.tryStartJob(level, proto);
            data.putLong(RECHECK_AT_KEY,
                    TimerJitter.dueAt(random, now, SporeBreachServerConfig.PROTO_STRUCTURE_RECHECK_INTERVAL_TICKS.get()));
        }
        if (now >= data.getLong(PASS_AT_KEY)) {
            int blocksPerPass = SporeBreachServerConfig.PROTO_STRUCTURE_BLOCKS_PER_PASS.get();
            if (remainingBudget >= blocksPerPass) {
                ProtoStructureGrowth.tryAdvanceJob(level, proto);
                remainingBudget -= blocksPerPass;
                data.putLong(PASS_AT_KEY,
                        TimerJitter.dueAt(random, now, SporeBreachServerConfig.PROTO_STRUCTURE_PASS_INTERVAL_TICKS.get()));
            }
            // Over budget: leave the stamp due so it's more overdue (and sorts first) next sweep.
        }
        return remainingBudget;
    }
}
