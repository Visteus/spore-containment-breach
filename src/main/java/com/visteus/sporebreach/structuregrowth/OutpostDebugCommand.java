package com.visteus.sporebreach.structuregrowth;

import com.Harbinger.Spore.Sentities.BaseEntities.Organoid;
import com.mojang.brigadier.context.CommandContext;
import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.chunkloading.ChunkloadData;
import com.visteus.sporebreach.chunkloading.ChunkloadOwnerId;
import com.visteus.sporebreach.chunkloading.ChunkloadState;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import com.visteus.sporebreach.structuregrowth.StructureFootprintData.Kind;
import com.visteus.sporebreach.structuregrowth.StructureFootprintData.Record;
import com.visteus.sporebreach.tracking.OrganoidRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /sporebreach:outpost_debug} - forces the nearest organoid to attempt an Outpost Watcher
 * tower right now and reports exactly which gate stopped it, plus the structures already recorded
 * nearby. Outpost placement is silent by design (a roll that finds no free site simply waits for
 * the next recheck), and the live cadence is one 10% roll every five minutes, so without this
 * there's no practical way to tell "correctly declined" apart from "broken".
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class OutpostDebugCommand {

    private static final int REPORT_RADIUS = 256;

    private OutpostDebugCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("sporebreach:outpost_debug")
                        .requires(source -> source.hasPermission(2))
                        .executes(OutpostDebugCommand::run)
        );
    }

    private static int run(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos origin = BlockPos.containing(source.getPosition());

        Organoid organoid = nearestOrganoid(level, origin);
        if (organoid == null) {
            source.sendFailure(Component.literal(
                    "sporebreach: no Mound or Proto-Hivemind loaded in " + level.dimension().location()
            ));
            return 0;
        }

        ChunkloadState chunkload = ChunkloadData.getState(level, new ChunkloadOwnerId.EntityOwner(organoid.getUUID()));
        int reach = chunkload == null ? 0 : chunkload.lastIssuedRadius() * 16;
        BlockPos organoidPos = organoid.blockPosition();
        int built = StructureFootprintData.countOwned(level, organoid.getUUID(), Kind.OUTPOST);
        int cap = SporeBreachServerConfig.OUTPOST_WATCHER_MAX_PER_ORGANOID.get();

        source.sendSuccess(() -> Component.literal(
                "sporebreach outpost debug - nearest " + organoid.getType().getDescriptionId()
                        + " " + shortId(organoid.getUUID())
                        + " at " + organoidPos.getX() + ", " + organoidPos.getY() + ", " + organoidPos.getZ()
        ), false);
        source.sendSuccess(() -> Component.literal(
                "  Chunkload reach: " + reach + " blocks" + (reach <= 0 ? " (not chunkloading yet)" : "")
        ), false);
        source.sendSuccess(() -> Component.literal("  Outposts grown: " + built + "/" + cap), false);

        OutpostWatcherGrowth.Result result = SporeBreachServerConfig.OUTPOST_WATCHER_ENABLED.get()
                ? OutpostWatcherGrowth.tryStartJob(level, organoid, true)
                : OutpostWatcherGrowth.Result.DISABLED;
        source.sendSuccess(() -> Component.literal("  Forced attempt: " + describe(result)), false);

        List<Record> nearby = StructureFootprintData.nearby(level, origin, REPORT_RADIUS);
        source.sendSuccess(() -> Component.literal(
                "  Recorded structures within " + REPORT_RADIUS + " blocks: " + nearby.size()
        ), false);
        for (Record record : nearby) {
            BlockPos anchor = record.anchor();
            source.sendSuccess(() -> Component.literal(
                    "    " + record.kind() + " " + record.structureId()
                            + " at " + anchor.getX() + ", " + anchor.getY() + ", " + anchor.getZ()
                            + (record.complete() ? "" : " (still growing)")
            ), false);
        }
        return result == OutpostWatcherGrowth.Result.STARTED ? 1 : 0;
    }

    private static String describe(OutpostWatcherGrowth.Result result) {
        return switch (result) {
            case STARTED -> "started a new tower";
            case RESUMED -> "resumed an unfinished tower";
            case ALREADY_GROWING -> "already growing one";
            case DISABLED -> "skipped, outpost watchers are disabled in the config";
            case TOO_YOUNG -> "too young (should not happen on a forced attempt)";
            case AT_CAP -> "already at maxPerOrganoid";
            case ROLL_FAILED -> "failed its chance roll (should not happen on a forced attempt)";
            case NOT_CHUNKLOADED -> "not chunkloading yet, so it has no range to place into";
            case EMPTY_POOL -> "structurePool is empty or unparseable";
            case NO_VALID_SITE -> "no valid site: every candidate was too close to another watcher, "
                    + "overlapped a recorded structure, or wasn't natural ground";
        };
    }

    private static Organoid nearestOrganoid(ServerLevel level, BlockPos pos) {
        List<Organoid> candidates = new ArrayList<>(OrganoidRegistry.get(level));
        candidates.addAll(OrganoidRegistry.getProtos(level));

        Organoid nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        for (Organoid candidate : candidates) {
            double distSq = candidate.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }
}
