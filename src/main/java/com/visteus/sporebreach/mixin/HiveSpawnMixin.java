package com.visteus.sporebreach.mixin;

import com.Harbinger.Spore.Sblocks.HiveSpawn;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Suppresses base Spore's own Outpost Watcher tower drop, leaving this mod's
 * {@code OutpostWatcherDirector} as the single source of watchers.
 *
 * <p>Worth suppressing on its own merits: a Reconstructed Mind that matures within
 * {@code hive_generate} of an existing Proto-Hivemind takes the tower branch instead of the
 * become-a-Proto branch, and that branch neither clears the block's kill counter nor removes the
 * block - while the block reschedules its own tick every 80 ticks. The result is the entire tower
 * template being slammed back into the world every four seconds, forever, ignoring every
 * block-protection rule this mod applies to its own growth.
 *
 * <p>Only the placement is cancelled; the block itself is deliberately left alone. Its
 * become-a-Proto branch tests for nearby Proto-Hiveminds every tick, so once the neighbor that
 * blocked it dies, the Reconstructed Mind still matures normally.
 */
@Mixin(HiveSpawn.class)
public abstract class HiveSpawnMixin {

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;"
                            + "placeInWorld(Lnet/minecraft/world/level/ServerLevelAccessor;"
                            + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;"
                            + "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;"
                            + "Lnet/minecraft/util/RandomSource;I)Z"
            )
    )
    private boolean sporebreach$suppressReconstructedMindTower(
            StructureTemplate template, ServerLevelAccessor level, BlockPos origin, BlockPos pivot,
            StructurePlaceSettings settings, RandomSource random, int flags
    ) {
        if (SporeBreachServerConfig.OUTPOST_WATCHER_SUPPRESS_BASE_TOWERS.get()) {
            return false;
        }
        return template.placeInWorld(level, origin, pivot, settings, random, flags);
    }
}
