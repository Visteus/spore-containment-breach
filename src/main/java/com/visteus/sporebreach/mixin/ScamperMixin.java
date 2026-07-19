package com.visteus.sporebreach.mixin;

import com.Harbinger.Spore.Sentities.EvolvedInfected.Scamper;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import com.visteus.sporebreach.tracking.OrganoidRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gates base Spore's uncapped Scamper-to-Mound age-up ({@code Scamper.Summon}, called 2-3 times
 * per age-up burst with no distance/density check at all) behind a density check against existing
 * Mounds, and makes the burst's size and placement range configurable - see the Goal #1
 * "Density-gated Scamper->Mound spawning" plan for the full design rationale, in particular why
 * the density check is cached once per burst (per game-tick) rather than re-run on every
 * {@code Summon()} call, so a passing burst still places its full 2-3 Mounds instead of collapsing
 * to 1 as soon as its own first Mound registers itself.
 */
@Mixin(Scamper.class)
public abstract class ScamperMixin {

    @Unique private long sporebreach$densityCheckTick = -1;
    @Unique private boolean sporebreach$densityGatePassed;

    @Inject(method = "Summon", at = @At("HEAD"), cancellable = true)
    private void sporebreach$gateMoundDensity(int i, CallbackInfo ci) {
        if (!SporeBreachServerConfig.ENABLE_SCAMPER_MOUND_DENSITY_GATE.get()) {
            return;
        }
        Scamper self = (Scamper) (Object) this;
        if (!(self.level() instanceof ServerLevel level)) {
            return;
        }

        if (this.sporebreach$densityCheckTick != self.tickCount) {
            int radius = SporeBreachServerConfig.SCAMPER_MOUND_DENSITY_RADIUS.get();
            int maxNearby = SporeBreachServerConfig.SCAMPER_MOUND_MAX_NEARBY.get();
            double radiusSq = (double) radius * radius;
            long existingNearby = OrganoidRegistry.get(level).stream()
                    .filter(m -> m.distanceToSqr(self.getX(), self.getY(), self.getZ()) <= radiusSq)
                    .count();
            this.sporebreach$densityGatePassed = existingNearby < maxNearby;
            this.sporebreach$densityCheckTick = self.tickCount;
        }

        if (!this.sporebreach$densityGatePassed) {
            ci.cancel();
        }
    }

    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(II)I", ordinal = 0)
    )
    private int sporebreach$configurableBurstSize(RandomSource random, int origin, int bound) {
        int min = SporeBreachServerConfig.SCAMPER_MOUND_SUMMON_MIN.get();
        int max = Math.max(min, SporeBreachServerConfig.SCAMPER_MOUND_SUMMON_MAX.get());
        int chanceMin = Math.max(0, min - 1);
        int chanceMaxExclusive = Math.max(chanceMin + 1, max);
        return random.nextInt(chanceMin, chanceMaxExclusive);
    }

    @Redirect(
            method = "Summon",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(II)I")
    )
    private int sporebreach$configurableSummonRange(RandomSource random, int origin, int bound) {
        int range = SporeBreachServerConfig.SCAMPER_MOUND_SUMMON_RANGE.get();
        return range <= 0 ? 0 : random.nextInt(-range, range);
    }
}
