package com.visteus.sporebreach.chunkloading;

import com.mojang.brigadier.context.CommandContext;
import com.visteus.sporebreach.SporeContainmentBreach;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /sporebreach:chunkload_debug} - reports the nearest tracked chunkload
 * owner in the executor's dimension: its position, current/min/max/ticking radius, and growth
 * progress. Exists because {@code /forceload query} only reports vanilla's own "Forced" ticket
 * set (ServerLevel#getForcedChunks) and has no visibility into NeoForge's mod-registered
 * entity/block ticket types, which is what ChunkloadManager actually uses.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class ChunkloadDebugCommand {

    private ChunkloadDebugCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("sporebreach:chunkload_debug")
                        .requires(source -> source.hasPermission(2))
                        .executes(ChunkloadDebugCommand::run)
        );
    }

    private static int run(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        Vec3 origin = source.getPosition();

        Map<ChunkloadOwnerId, ChunkloadState> owners = ChunkloadData.snapshot(level);
        if (owners.isEmpty()) {
            source.sendFailure(Component.literal("sporebreach: no chunkload owners tracked in " + level.dimension().location()));
            return 0;
        }

        ChunkloadOwnerId nearestId = null;
        ChunkloadState nearestState = null;
        BlockPos nearestPos = BlockPos.ZERO;
        double nearestDistSq = Double.MAX_VALUE;

        for (Map.Entry<ChunkloadOwnerId, ChunkloadState> entry : owners.entrySet()) {
            BlockPos pos = resolveCurrentPos(level, entry.getKey(), entry.getValue());
            double distSq = origin.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearestId = entry.getKey();
                nearestState = entry.getValue();
                nearestPos = pos;
            }
        }

        report(source, level, nearestId, nearestState, nearestPos, Math.sqrt(nearestDistSq));
        return 1;
    }

    private static BlockPos resolveCurrentPos(ServerLevel level, ChunkloadOwnerId ownerId, ChunkloadState state) {
        if (ownerId instanceof ChunkloadOwnerId.EntityOwner entityOwner) {
            Entity entity = level.getEntity(entityOwner.entityId());
            if (entity != null) {
                return entity.blockPosition();
            }
        } else if (ownerId instanceof ChunkloadOwnerId.BlockOwner blockOwner) {
            return blockOwner.pos();
        }
        ChunkPos anchor = state.anchorChunk();
        return new BlockPos(anchor.getMinBlockX() + 8, 64, anchor.getMinBlockZ() + 8);
    }

    private static void report(
            CommandSourceStack source, ServerLevel level, ChunkloadOwnerId ownerId, ChunkloadState state, BlockPos pos, double distance
    ) {
        ChunkloadEntry entry;
        String ownerLabel;
        if (ownerId instanceof ChunkloadOwnerId.EntityOwner entityOwner) {
            Entity entity = level.getEntity(entityOwner.entityId());
            if (entity != null) {
                entry = ChunkloadEntryLookup.forEntityType(entity.getType());
                ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                ownerLabel = "entity " + typeId + " (uuid " + entityOwner.entityId() + ")";
            } else {
                entry = null;
                ownerLabel = "entity uuid " + entityOwner.entityId() + " (not currently loaded)";
            }
        } else if (ownerId instanceof ChunkloadOwnerId.BlockOwner blockOwner) {
            ResourceLocation expected = state.expectedBlockId();
            entry = expected != null ? ChunkloadEntryLookup.forBlockId(expected) : null;
            ownerLabel = "block " + (expected != null ? expected : "?") + " at " + blockOwner.pos();
        } else {
            entry = null;
            ownerLabel = "unknown owner";
        }

        source.sendSuccess(() -> Component.literal(
                "sporebreach chunkload debug - nearest owner in " + level.dimension().location()
        ), false);
        source.sendSuccess(() -> Component.literal("  Owner: " + ownerLabel), false);
        source.sendSuccess(() -> Component.literal(
                "  Position: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()
                        + " (" + String.format("%.1f", distance) + " blocks away)"
        ), false);
        source.sendSuccess(() -> Component.literal("  Anchor chunk: " + state.anchorChunk()), false);
        source.sendSuccess(() -> Component.literal("  Last issued radius: " + state.lastIssuedRadius()), false);

        long elapsed = level.getGameTime() - state.activationGameTime();
        source.sendSuccess(() -> Component.literal("  Ticks since activation: " + elapsed), false);

        if (entry != null) {
            double progress = Math.min(1.0, elapsed / (double) entry.ticksToMaxRadius());
            int targetRadius = entry.minRadius() + (int) Math.floor(progress * (entry.maxRadius() - entry.minRadius()));
            source.sendSuccess(() -> Component.literal(
                    "  Config: min=" + entry.minRadius() + " max=" + entry.maxRadius()
                            + " tickingRadius=" + entry.tickingRadius() + " ticksToMaxRadius=" + entry.ticksToMaxRadius()
            ), false);
            source.sendSuccess(() -> Component.literal(
                    "  Growth progress: " + String.format("%.1f%%", progress * 100) + ", target radius " + targetRadius
                            + (targetRadius == state.lastIssuedRadius() ? " (matches last issued)" : " (pending next recheck)")
            ), false);
        } else {
            source.sendSuccess(() -> Component.literal("  Config entry: unresolved (owner not currently loaded, or its config entry was removed)"), false);
        }
    }
}
