package com.visteus.sporebreach.mixin;

import com.Harbinger.Spore.Sentities.Organoids.Proto;
import com.Harbinger.Spore.Sentities.Organoids.Womb;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gates base Spore's own Signal-driven Womb summons ({@code Proto.SummonConstructor}, fired from
 * {@code Proto.tick()} whenever a qualifying kill near a Proto-Hivemind sets a Signal and no
 * existing Calamity absorbs it first) with a per-Proto cooldown and a density check against
 * existing Wombs - see the Goal #1 "Gate Proto to Womb Signal spawning" plan for the full design
 * rationale, in particular why a blocked attempt drops the Signal outright rather than leaving it
 * pending for an unconditional retry once the gate reopens.
 */
@Mixin(Proto.class)
public abstract class ProtoMixin {

    private static final String WOMB_SIGNAL_COOLDOWN_KEY = "sporebreach_womb_signal_cd";

    @Inject(method = "SummonConstructor", at = @At("HEAD"), cancellable = true)
    private void sporebreach$gateWombSignal(Level level, Entity entity, BlockPos pos, CallbackInfo ci) {
        if (!SporeBreachServerConfig.ENABLE_PROTO_WOMB_SIGNAL_GATE.get() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Proto self = (Proto) (Object) this;

        long readyAt = self.getPersistentData().getLong(WOMB_SIGNAL_COOLDOWN_KEY);
        if (serverLevel.getGameTime() < readyAt) {
            self.setSignal(null);
            ci.cancel();
            return;
        }

        int radius = SporeBreachServerConfig.PROTO_WOMB_SIGNAL_DENSITY_RADIUS.get();
        int maxNearby = SporeBreachServerConfig.PROTO_WOMB_SIGNAL_MAX_NEARBY.get();
        AABB area = self.getBoundingBox().inflate(radius);
        long existingWombs = serverLevel.getEntities(self, area, e -> e instanceof Womb).size();
        if (existingWombs >= maxNearby) {
            self.setSignal(null);
            ci.cancel();
        }
    }

    @Inject(method = "SummonConstructor", at = @At("TAIL"))
    private void sporebreach$commitWombSignalCooldown(Level level, Entity entity, BlockPos pos, CallbackInfo ci) {
        if (!SporeBreachServerConfig.ENABLE_PROTO_WOMB_SIGNAL_GATE.get() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Proto self = (Proto) (Object) this;
        if (self.getSignal() == null) {
            int cooldownTicks = SporeBreachServerConfig.PROTO_WOMB_SIGNAL_COOLDOWN_TICKS.get();
            self.getPersistentData().putLong(WOMB_SIGNAL_COOLDOWN_KEY, serverLevel.getGameTime() + cooldownTicks);
        }
    }

    /**
     * Cancels base Spore's own procedural shell growth ({@code CasingGenerator.generateChasing},
     * invoked from here) when this mod's staged NBT structure growth (goal #3) is active, so the
     * two systems never both build around the same Proto-Hivemind.
     */
    @Inject(method = "generateCasing", at = @At("HEAD"), cancellable = true)
    private void sporebreach$gateBaseCasingGrowth(CallbackInfo ci) {
        if (SporeBreachServerConfig.STRUCTURE_GROWTH_MODE.get() == SporeBreachServerConfig.StructureGrowthMode.SPORE_BREACH_TOWERS) {
            ci.cancel();
        }
    }
}
