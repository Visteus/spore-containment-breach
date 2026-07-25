package com.visteus.sporebreach.mixin;

import com.Harbinger.Spore.Sevents.DespawnSystem;
import com.visteus.sporebreach.spawning.RaidDespawnGate;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Base Spore's population-cap despawn trim ({@code DespawnSystem.cleanUpMobs}) already skips any
 * entity that's blacklisted or {@code hasCustomName()} before bucketing it for {@code
 * despawnExcess} to potentially discard - see the Goal #1 "raid return" plan's despawn-exemption
 * addendum. Piggybacks an active raid member's exemption onto that same existing check instead of
 * touching the bucketing/sort/discard logic, so a raider traveling home to its Proto (see {@code
 * spawning.RaidReturnDirector}) can't be swept up by the population cap mid-transit.
 */
@Mixin(DespawnSystem.class)
public abstract class DespawnSystemMixin {

    @Redirect(
            method = "cleanUpMobs",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hasCustomName()Z")
    )
    private static boolean sporebreach$exemptActiveRaidMembers(Entity entity) {
        return entity.hasCustomName() || RaidDespawnGate.isProtected(entity);
    }
}
