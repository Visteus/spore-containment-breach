package com.visteus.sporebreach.spawning;

import com.mojang.brigadier.context.CommandContext;
import com.visteus.sporebreach.SporeContainmentBreach;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /sporebreach:raid_debug} - lists every currently-dispatched Proto raid in
 * the executor's dimension: originating Proto, target, age (ticks since dispatch), living/total
 * raider count, and the current position of one randomly chosen living raider. Exists because
 * raid members are ordinary mobs scattered around a target rather than a single trackable
 * entity/ticket the way {@link com.visteus.sporebreach.chunkloading.ChunkloadDebugCommand}'s
 * owners are - {@code /forceload query} in particular won't usefully show the raid-travel
 * sliding window from {@link RaidTravelTrackingEvents}, since at most two chunks are ever forced
 * at a time and they shift every time a raider crosses a chunk boundary.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class RaidDebugCommand {

    private RaidDebugCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("sporebreach:raid_debug")
                        .requires(source -> source.hasPermission(2))
                        .executes(RaidDebugCommand::run)
        );
    }

    private static int run(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        RaidRegistry.pruneFinished(level);
        List<RaidRegistry.RaidRecord> raids = RaidRegistry.snapshot(level);
        if (raids.isEmpty()) {
            source.sendFailure(Component.literal("sporebreach: no active raids in " + level.dimension().location()));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                "sporebreach raid debug - " + raids.size() + " active raid(s) in " + level.dimension().location()
        ), false);
        for (RaidRegistry.RaidRecord raid : raids) {
            report(source, level, raid);
        }
        return raids.size();
    }

    private static void report(CommandSourceStack source, ServerLevel level, RaidRegistry.RaidRecord raid) {
        List<UUID> living = RaidRegistry.livingMembers(level, raid);
        long age = level.getGameTime() - raid.dispatchGameTime();
        BlockPos target = raid.target();

        source.sendSuccess(() -> Component.literal(
                "  Raid " + shortId(raid.raidId()) + " - dispatched by Proto " + shortId(raid.protoId())
        ), false);
        source.sendSuccess(() -> Component.literal(
                "    Target: " + target.getX() + ", " + target.getY() + ", " + target.getZ()
        ), false);
        source.sendSuccess(() -> Component.literal("    Age: " + age + " ticks"), false);
        source.sendSuccess(() -> Component.literal(
                "    Members: " + living.size() + "/" + raid.raiderIds().size() + " still alive"
        ), false);

        if (living.isEmpty()) {
            source.sendSuccess(() -> Component.literal("    Current location: no living members left"), false);
            return;
        }

        UUID sampledId = living.get(level.getRandom().nextInt(living.size()));
        Entity member = level.getEntity(sampledId);
        if (member == null) {
            // Defensive only - livingMembers() just confirmed this id resolves.
            return;
        }
        BlockPos memberPos = member.blockPosition();
        ResourceLocation memberType = BuiltInRegistries.ENTITY_TYPE.getKey(member.getType());
        source.sendSuccess(() -> Component.literal(
                "    Current location (sampled member " + memberType + "): "
                        + memberPos.getX() + ", " + memberPos.getY() + ", " + memberPos.getZ()
        ), false);
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }
}
