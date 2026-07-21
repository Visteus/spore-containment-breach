package com.visteus.sporebreach.mixin;

import com.Harbinger.Spore.Sentities.Organoids.Mound;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancels base Spore's one-shot seed-block placement ({@code Mound.placeStructureBlock}) when this
 * mod's own staged NBT structure growth (goal #3) is active, so the two systems never both grow
 * structures around the same Mound. Targets this method specifically rather than
 * {@code additionPlacers}/{@code additionIgnoreConfigPlacers}, since those also drive Mound's
 * unrelated foliage-conversion infection spread, which must keep running regardless of this
 * toggle.
 */
@Mixin(Mound.class)
public abstract class MoundStructureMixin {

    @Inject(method = "placeStructureBlock", at = @At("HEAD"), cancellable = true)
    private void sporebreach$gateBaseStructureGrowth(Level level, BlockPos pos, CallbackInfo ci) {
        if (SporeBreachServerConfig.STRUCTURE_GROWTH_MODE.get() == SporeBreachServerConfig.StructureGrowthMode.SPORE_BREACH_TOWERS) {
            ci.cancel();
        }
    }
}
