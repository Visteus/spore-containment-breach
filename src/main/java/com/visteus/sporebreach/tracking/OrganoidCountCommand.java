package com.visteus.sporebreach.tracking;

import com.Harbinger.Spore.Sentities.Organoids.Mound;
import com.Harbinger.Spore.Sentities.Organoids.Proto;
import com.mojang.brigadier.context.CommandContext;
import com.visteus.sporebreach.SporeContainmentBreach;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /sporebreach:organoid_counts} - reports how many Mounds and Proto-Hiveminds
 * {@link OrganoidRegistry} currently has tracked in the executor's dimension, split by type.
 * There's otherwise no in-game way to see these lists directly.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class OrganoidCountCommand {

    private OrganoidCountCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("sporebreach:organoid_counts")
                        .requires(source -> source.hasPermission(2))
                        .executes(OrganoidCountCommand::run)
        );
    }

    private static int run(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        List<Mound> mounds = OrganoidRegistry.get(level);
        List<Proto> protos = OrganoidRegistry.getProtos(level);
        int total = mounds.size() + protos.size();

        source.sendSuccess(() -> Component.literal(
                "Currently tracked organoids in " + level.dimension().location()
        ), false);
        source.sendSuccess(() -> Component.literal("  Mound: " + mounds.size()), false);
        source.sendSuccess(() -> Component.literal("  Proto-Hivemind: " + protos.size()), false);
        source.sendSuccess(() -> Component.literal("  Total: " + total), false);

        return total;
    }
}
