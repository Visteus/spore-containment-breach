package com.visteus.sporebreach.biome;

import com.mojang.brigadier.context.CommandContext;
import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.chunkloading.ChunkloadData;
import com.visteus.sporebreach.chunkloading.ChunkloadOwnerId;
import com.visteus.sporebreach.chunkloading.ChunkloadState;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /sporebreach:biome_paint_status} - reports the nearest chunkload-tracked
 * organoid in the executor's dimension, comparing its chunkload radius against biome-paint's
 * allowed spread boundary and how many chunks its BFS frontier still has pending. Mirrors {@link
 * com.visteus.sporebreach.chunkloading.ChunkloadDebugCommand}'s shape, since biome painting
 * piggybacks directly on chunkload's own radius growth (see {@link BiomePaintManager}).
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class BiomePaintStatusCommand {

    private BiomePaintStatusCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("sporebreach:biome_paint_status")
                        .requires(source -> source.hasPermission(2))
                        .executes(BiomePaintStatusCommand::run)
        );
    }

    private static int run(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        Vec3 origin = source.getPosition();

        ChunkloadOwnerId.EntityOwner nearestId = null;
        ChunkloadState nearestState = null;
        BlockPos nearestPos = BlockPos.ZERO;
        double nearestDistSq = Double.MAX_VALUE;

        for (Map.Entry<ChunkloadOwnerId, ChunkloadState> entry : ChunkloadData.snapshot(level).entrySet()) {
            if (!(entry.getKey() instanceof ChunkloadOwnerId.EntityOwner entityOwner)) {
                continue;
            }
            Entity entity = level.getEntity(entityOwner.entityId());
            if (entity == null) {
                continue;
            }
            BlockPos pos = entity.blockPosition();
            double distSq = origin.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearestId = entityOwner;
                nearestState = entry.getValue();
                nearestPos = pos;
            }
        }

        if (nearestId == null) {
            source.sendFailure(Component.literal(
                    "sporebreach: no loaded chunkload-owning entity tracked in " + level.dimension().location()
            ));
            return 0;
        }

        int chunkloadRadius = nearestState.lastIssuedRadius();
        int extraRadius = SporeBreachServerConfig.BIOME_PAINT_EXTRA_RADIUS_CHUNKS.get();
        int allowedRadius = BiomePaintManager.getAllowedRadius(nearestId.entityId());
        int frontierPending = BiomePaintManager.pendingFrontierCount(nearestId.entityId());

        Entity entity = level.getEntity(nearestId.entityId());
        ResourceLocation typeId = entity != null ? BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()) : null;

        ChunkloadOwnerId.EntityOwner finalNearestId = nearestId;
        BlockPos finalNearestPos = nearestPos;
        source.sendSuccess(() -> Component.literal(
                "sporebreach biome paint status - nearest owner in " + level.dimension().location()
        ), false);
        source.sendSuccess(() -> Component.literal(
                "  Owner: " + (typeId != null ? typeId : "?") + " (uuid " + finalNearestId.entityId() + ")"
        ), false);
        source.sendSuccess(() -> Component.literal(
                "  Position: " + finalNearestPos.getX() + ", " + finalNearestPos.getY() + ", " + finalNearestPos.getZ()
        ), false);
        source.sendSuccess(() -> Component.literal("  Chunkload radius: " + chunkloadRadius), false);
        source.sendSuccess(() -> Component.literal(
                "  Biome spread boundary: " + allowedRadius + " (chunkload + " + extraRadius + ")"
        ), false);
        source.sendSuccess(() -> Component.literal(
                "  Frontier chunks pending for this owner: " + frontierPending
                        + (frontierPending == 0 ? " (spread caught up to boundary)" : " (still radiating outward)")
        ), false);
        return 1;
    }
}
