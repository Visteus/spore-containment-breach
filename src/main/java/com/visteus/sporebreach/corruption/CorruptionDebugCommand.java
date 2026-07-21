package com.visteus.sporebreach.corruption;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.visteus.sporebreach.SporeContainmentBreach;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /sporebreach:corruption_debug} - reports the executor's dimension's
 * current World Corruption value and which of the six gates are open, mirroring {@link
 * com.visteus.sporebreach.chunkloading.ChunkloadDebugCommand}'s precedent. Also exposes {@code
 * set}/{@code add} subcommands (both requiring permission level 2, same as the root) for jumping
 * straight to or across a breakpoint while tuning the config defaults.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class CorruptionDebugCommand {

    private CorruptionDebugCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("sporebreach:corruption_debug")
                        .requires(source -> source.hasPermission(2))
                        .executes(CorruptionDebugCommand::report)
                        .then(
                                Commands.literal("set")
                                        .then(
                                                Commands.argument("value", IntegerArgumentType.integer(0))
                                                        .executes(CorruptionDebugCommand::runSet)
                                        )
                        )
                        .then(
                                Commands.literal("add")
                                        .then(
                                                Commands.argument("amount", IntegerArgumentType.integer())
                                                        .executes(CorruptionDebugCommand::runAdd)
                                        )
                        )
        );
    }

    private static int report(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        report(context.getSource(), level);
        return CorruptionData.get(level);
    }

    private static int runSet(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        CorruptionData.set(level, IntegerArgumentType.getInteger(context, "value"));
        report(context.getSource(), level);
        return CorruptionData.get(level);
    }

    private static int runAdd(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        int amount = IntegerArgumentType.getInteger(context, "amount");
        if (amount >= 0) {
            CorruptionData.add(level, amount);
        } else {
            CorruptionData.subtract(level, -amount);
        }
        report(context.getSource(), level);
        return CorruptionData.get(level);
    }

    private static void report(CommandSourceStack source, ServerLevel level) {
        int value = CorruptionData.get(level);
        source.sendSuccess(() -> Component.literal(
                "sporebreach World Corruption in " + level.dimension().location() + ": " + value
        ), false);
        source.sendSuccess(() -> Component.literal(
                "  Stage 1 (raids allowed): " + CorruptionTier.areRaidsAllowed(level)
        ), false);
        source.sendSuccess(() -> Component.literal(
                "  Stage 2 (instant evolution): " + CorruptionTier.isInstantEvolutionAllowed(level)
        ), false);
        source.sendSuccess(() -> Component.literal(
                "  Stage 3 (calamities/wombs, enchanted gear): " + CorruptionTier.isCalamitySpawningAllowed(level)
        ), false);
        source.sendSuccess(() -> Component.literal(
                "  Stage 4 (calamities in raids): " + CorruptionTier.isCalamityRaidAllowed(level)
        ), false);
        source.sendSuccess(() -> Component.literal(
                "  Stage 5 (calamity adaptations): " + CorruptionTier.isAdaptationAllowed(level)
        ), false);
        source.sendSuccess(() -> Component.literal(
                "  Stage 6 (linked spawns): " + CorruptionTier.isLinkedSpawnAllowed(level)
        ), false);
    }
}
