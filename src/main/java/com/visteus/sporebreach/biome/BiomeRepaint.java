package com.visteus.sporebreach.biome;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.visteus.sporebreach.SporeContainmentBreach;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biome;
import org.slf4j.Logger;

/**
 * Repaints a single loaded chunk column to one of Goal #5's corruption biomes via the same public
 * {@link FillBiomeCommand#fill} vanilla's own {@code /fillbiome} command calls, rather than
 * spamming that command as text. Confirmed against the actual 1.21.1 source: {@code fill} writes
 * the new biome into each touched {@code ChunkAccess} via {@code fillBiomesFromNoise}, marks it
 * unsaved, then unconditionally calls {@code ServerChunkCache#chunkMap.resendBiomesForChunks} -
 * no separate resend call is needed here. See {@code BiomePaintManager} for the budgeted,
 * incremental caller this is designed for - deliberately whole-chunk-at-a-time, since biome
 * storage is inherently 4x4x4-cell granular and there's no benefit to finer-grained budgeting the
 * way there is for block-level work.
 *
 * <p>{@code fill} itself (not just its brigadier-parsing overload) rejects any single call whose
 * volume exceeds the {@code commandModificationBlockLimit} gamerule (32768 by default) - a
 * full-height 16x16 column trips this, so each column is painted in vertical bands sized to stay
 * under whatever that gamerule is currently set to. This is the same limit spore-inquisition's own
 * {@code /fillbiome} calls work around, by raising the gamerule instead of banding.
 */
public final class BiomeRepaint {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final ResourceKey<Biome> INFECTION_ZONE = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(SporeContainmentBreach.MODID, "infection_zone")
    );
    public static final ResourceKey<Biome> DEAD_SCAR = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(SporeContainmentBreach.MODID, "dead_scar")
    );

    private BiomeRepaint() {
    }

    /**
     * Repaints one full-height chunk column. Returns false (and logs a warning on an actual
     * failure) if the chunk isn't currently loaded to full status - callers should skip and retry
     * next pass rather than treating this as fatal, since a forced non-ticking chunk can lag a
     * tick or two behind the paint queue that requested it.
     */
    public static boolean paintColumn(ServerLevel level, ChunkPos pos, ResourceKey<Biome> biomeKey) {
        if (level.getChunkSource().getChunkNow(pos.x, pos.z) == null) {
            return false;
        }

        Holder<Biome> holder = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(biomeKey);

        int blockLimit = level.getGameRules().getInt(GameRules.RULE_COMMAND_MODIFICATION_BLOCK_LIMIT);
        int bandHeight = Math.max(1, blockLimit / (16 * 16));

        boolean allSucceeded = true;
        int maxY = level.getMaxBuildHeight() - 1;
        for (int bandMinY = level.getMinBuildHeight(); bandMinY <= maxY; bandMinY += bandHeight) {
            int bandMaxY = Math.min(bandMinY + bandHeight - 1, maxY);
            BlockPos min = new BlockPos(pos.getMinBlockX(), bandMinY, pos.getMinBlockZ());
            BlockPos max = new BlockPos(pos.getMaxBlockX(), bandMaxY, pos.getMaxBlockZ());

            Either<Integer, CommandSyntaxException> result = FillBiomeCommand.fill(level, min, max, holder);
            if (result.right().isPresent()) {
                LOGGER.warn(
                        "sporebreach: failed to paint chunk {} band y={}..{} to biome {}: {}",
                        pos, bandMinY, bandMaxY, biomeKey.location(), result.right().get().getMessage()
                );
                allSucceeded = false;
            }
        }
        return allSucceeded;
    }
}
