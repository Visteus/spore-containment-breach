package com.visteus.sporebreach.util;

import com.visteus.sporebreach.config.SporeBreachServerConfig;
import net.minecraft.util.RandomSource;

/**
 * Applies the global {@code timerJitterPercent} config knob to a base tick interval, so periodic
 * timers reroll within a band around their configured value each time they fire instead of
 * staying phase-locked forever. Uniform, not triangular: unlike {@code
 * ProtoRaidDirector#rerollCooldown}, which spans an explicit min/max gameplay range where the
 * midpoint should be the common case, the goal here is decorrelating timers from each other, and
 * a uniform roll spreads flattest.
 */
public final class TimerJitter {

    private TimerJitter() {
    }

    /** Rerolls {@code baseTicks} by plus or minus {@code timerJitterPercent}, clamped to at least 1. */
    public static int roll(RandomSource random, int baseTicks) {
        int percent = SporeBreachServerConfig.TIMER_JITTER_PERCENT.get();
        if (percent <= 0) {
            return baseTicks;
        }
        int span = Math.round(baseTicks * percent / 100.0f);
        int low = Math.max(1, baseTicks - span);
        int high = Math.max(low, baseTicks + span);
        return low + random.nextInt(high - low + 1);
    }

    /** {@code gameTime} plus a freshly rolled interval - the idiom for rerolling a cooldown/due stamp. */
    public static long dueAt(RandomSource random, long gameTime, int baseTicks) {
        return gameTime + roll(random, baseTicks);
    }

    /**
     * A uniform phase seed in {@code [gameTime, gameTime + baseTicks)}, for scattering the first due
     * stamp of something that has never fired before - existing organoids catching an update, or a
     * director's very first sweep - so they don't all land on the same tick.
     */
    public static long firstDueAt(RandomSource random, long gameTime, int baseTicks) {
        return gameTime + random.nextInt(Math.max(1, baseTicks));
    }
}
