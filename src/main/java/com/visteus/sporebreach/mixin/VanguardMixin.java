package com.visteus.sporebreach.mixin;

import com.Harbinger.Spore.Sentities.Utility.Vanguard;
import com.visteus.sporebreach.spawning.ProtoRaidDirector;
import com.visteus.sporebreach.spawning.RaidRegistry;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Base Spore's {@code Vanguard.callReinforcements()} (called from its own {@code tick()} whenever
 * it's fighting a "sapient" target and its raid-timeout has elapsed) spawns backup mobs without
 * any way to tell they belong to the same fight - see the Goal #1 "raid return" plan. If this
 * Vanguard is itself one of our raiders (tagged by {@link ProtoRaidDirector#spawnRaider} with
 * {@link ProtoRaidDirector#RAID_BY_KEY}/{@link ProtoRaidDirector#RAID_ID_KEY}), stamp the same
 * tags onto whatever it calls in and add it to that raid's roster in {@link RaidRegistry}, so
 * {@code RaidReturnDirector} picks the reinforcement up exactly like an original raider. A
 * naturally-spawned or otherwise-summoned Vanguard carries neither tag, so this is a no-op for
 * every Vanguard outside our own raid system.
 */
@Mixin(Vanguard.class)
public abstract class VanguardMixin {

    @Redirect(
            method = "callReinforcements",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/ServerLevelAccessor;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"
            )
    )
    private boolean sporebreach$tagReinforcement(ServerLevelAccessor accessor, Entity mob) {
        Vanguard self = (Vanguard) (Object) this;
        CompoundTag selfData = self.getPersistentData();
        if (selfData.hasUUID(ProtoRaidDirector.RAID_BY_KEY) && selfData.hasUUID(ProtoRaidDirector.RAID_ID_KEY)) {
            UUID raidBy = selfData.getUUID(ProtoRaidDirector.RAID_BY_KEY);
            UUID raidId = selfData.getUUID(ProtoRaidDirector.RAID_ID_KEY);
            mob.getPersistentData().putUUID(ProtoRaidDirector.RAID_BY_KEY, raidBy);
            mob.getPersistentData().putUUID(ProtoRaidDirector.RAID_ID_KEY, raidId);
            if (self.level() instanceof ServerLevel level) {
                RaidRegistry.findByRaidId(level, raidId).ifPresent(record -> record.raiderIds().add(mob.getUUID()));
            }
        }
        return accessor.addFreshEntity(mob);
    }
}
