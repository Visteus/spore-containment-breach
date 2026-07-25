package com.visteus.sporebreach.mixin;

import com.visteus.sporebreach.spawning.RaidDespawnGate;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Vanilla {@code Mob.checkDespawn()} gates both its instant far-away discard and its
 * noActionTime-based random despawn chance behind {@code this.removeWhenFarAway(distSq)} - see
 * the Goal #1 "raid return" plan's despawn-exemption addendum. Redirecting the call site here
 * (rather than injecting into every {@code Infected} subclass's own override, e.g. {@code Hyper}/
 * {@code EvolvedInfected}, which don't all delegate to {@code Infected}'s override) exempts an
 * active raid member uniformly regardless of its concrete type, while leaving {@code
 * checkDespawn()}'s other behavior (Peaceful-difficulty clearing, {@code noActionTime}
 * bookkeeping) untouched. {@code Calamity.removeWhenFarAway} already hard-codes {@code false}
 * unconditionally, so this is a no-op for Calamities either way.
 */
@Mixin(Mob.class)
public abstract class MobMixin {

    @Redirect(
            method = "checkDespawn",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;removeWhenFarAway(D)Z")
    )
    private boolean sporebreach$exemptActiveRaidMembers(Mob mob, double distanceToClosestPlayer) {
        return !RaidDespawnGate.isProtected(mob) && mob.removeWhenFarAway(distanceToClosestPlayer);
    }
}
