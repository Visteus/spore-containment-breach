package com.visteus.sporebreach.util;

import net.minecraft.util.RandomSource;

/**
 * Drop-in replacement for the {@code long tickCounter; tickCounter++; counter % interval == 0}
 * idiom used across this mod's periodic directors. Counts down to a jittered target instead of a
 * fixed one, rerolling every time it fires - including its very first fire, so a director's boot
 * phase is randomized too instead of every director aligning on server start. Owns its own {@link
 * RandomSource} since every caller runs on the server tick thread.
 */
public final class JitteredTimer {

    private final RandomSource random = RandomSource.create();
    private long remaining = -1;

    /** Call once per server tick. Returns true on ticks the timer fires. */
    public boolean tick(int baseInterval) {
        if (remaining < 0) {
            remaining = TimerJitter.roll(random, baseInterval);
        }
        remaining--;
        if (remaining > 0) {
            return false;
        }
        remaining = TimerJitter.roll(random, baseInterval);
        return true;
    }
}
