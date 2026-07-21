package com.visteus.sporebreach.biome;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.chunkloading.ChunkCircleOffsets;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /spore_containment_breach:biome_paint_debug <biome> [radiusChunks]} - Phase B throwaway
 * tool for proving {@link BiomeRepaint#paintColumn} actually reaches a connected client live (no
 * relog) before the real growth machinery in {@code BiomePaintManager} is built on top of it.
 * Paints the chunk(s) around the executor to any biome resource location, including vanilla ones
 * (e.g. {@code minecraft:plains}), so it doubles as an "unpaint" by pointing it back at whatever
 * biome the area started as.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class BiomePaintDebugCommand {

    private static final SimpleCommandExceptionType UNKNOWN_BIOME = new SimpleCommandExceptionType(
            Component.literal("Unknown biome")
    );

    private BiomePaintDebugCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("spore_containment_breach:biome_paint_debug")
                        .requires(source -> source.hasPermission(2))
                        .then(
                                Commands.argument("biome", ResourceLocationArgument.id())
                                        .executes(context -> run(context, 0))
                                        .then(
                                                Commands.argument("radiusChunks", IntegerArgumentType.integer(0, ChunkCircleOffsets.MAX_RADIUS))
                                                        .executes(context -> run(context, IntegerArgumentType.getInteger(context, "radiusChunks")))
                                        )
                        )
        );
    }

    private static int run(CommandContext<CommandSourceStack> context, int radiusChunks) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        ResourceLocation biomeId = ResourceLocationArgument.getId(context, "biome");
        ResourceKey<Biome> biomeKey = ResourceKey.create(Registries.BIOME, biomeId);

        Holder<Biome> holder = level.registryAccess().registryOrThrow(Registries.BIOME).getHolder(biomeKey).orElse(null);
        if (holder == null) {
            throw UNKNOWN_BIOME.create();
        }

        ChunkPos center = new ChunkPos(BlockPos.containing(source.getPosition()));

        Set<ChunkCircleOffsets.ChunkOffset> offsets = radiusChunks <= 0
                ? Set.of(new ChunkCircleOffsets.ChunkOffset(0, 0))
                : ChunkCircleOffsets.fullOffsets(radiusChunks);

        int painted = 0;
        for (ChunkCircleOffsets.ChunkOffset offset : offsets) {
            ChunkPos pos = new ChunkPos(center.x + offset.dx(), center.z + offset.dz());
            if (BiomeRepaint.paintColumn(level, pos, biomeKey)) {
                painted++;
            }
        }

        int paintedCount = painted;
        source.sendSuccess(() -> Component.literal(
                "spore_containment_breach: painted " + paintedCount + "/" + offsets.size() + " chunk column(s) around "
                        + center + " to " + biomeId
        ), false);
        return painted;
    }
}
