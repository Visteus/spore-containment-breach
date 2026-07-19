package com.visteus.sporebreach.chunkloading;

import com.mojang.logging.LogUtils;
import com.visteus.sporebreach.SporeContainmentBreach;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import org.slf4j.Logger;

/**
 * Orchestration core for Goal #4's chunkloading: registers this mod's {@link TicketController},
 * and drives activation/growth/teardown of both entity- and block-owned chunkload state. Per-chunk
 * {@code ticking} is decided by circle-membership within {@link ChunkloadEntry#tickingRadius()},
 * not a blanket true - see goal-2-efficiency-investigation.md's "central insight" that forced
 * chunks are loaded *and* ticking by default, so an organoid's whole grown territory would
 * otherwise simulate mob AI/block ticks at full cost even unobserved.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ChunkloadManager {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ResourceLocation CONTROLLER_ID = ResourceLocation.fromNamespaceAndPath(SporeContainmentBreach.MODID, "chunkload");

    private static volatile TicketController controller;

    private ChunkloadManager() {
    }

    @SubscribeEvent
    public static void onRegisterControllers(RegisterTicketControllersEvent event) {
        TicketController registered = new TicketController(CONTROLLER_ID);
        event.register(registered);
        controller = registered;
    }

    /**
     * Idempotent - no-ops if already tracked, since EntityJoinLevelEvent refires for an
     * already-tracked owner whenever NeoForge reinstates its own persisted tickets at startup,
     * not just on a genuine first player approach. Runs one immediate growth check on first
     * activation so the owner doesn't wait a full recheck interval for its first (min-radius)
     * chunks.
     */
    public static void activateEntityOwner(ServerLevel level, Entity entity, ChunkloadEntry entry) {
        ChunkloadOwnerId ownerId = new ChunkloadOwnerId.EntityOwner(entity.getUUID());
        if (ChunkloadData.isTracked(level, ownerId)) {
            return;
        }
        ChunkPos anchor = new ChunkPos(entity.blockPosition());
        ChunkloadData.activate(level, ownerId, anchor, level.getGameTime(), null);
        growOwner(level, ownerId, entry, ChunkloadData.getState(level, ownerId));
    }

    /**
     * Stretch-goal entry point for a future {@link ChunkloadAnchor} block. Same idempotent/
     * immediate-growth-check behavior as {@link #activateEntityOwner}.
     */
    public static void registerBlockOwner(ServerLevel level, ChunkloadAnchor anchor, ChunkloadEntry entry) {
        ChunkloadOwnerId ownerId = new ChunkloadOwnerId.BlockOwner(anchor.chunkloadAnchorPos());
        if (ChunkloadData.isTracked(level, ownerId)) {
            return;
        }
        ChunkPos anchorChunk = new ChunkPos(anchor.chunkloadAnchorPos());
        ChunkloadData.activate(level, ownerId, anchorChunk, level.getGameTime(), anchor.chunkloadOwnerBlockId());
        growOwner(level, ownerId, entry, ChunkloadData.getState(level, ownerId));
    }

    public static void unregisterBlockOwner(ServerLevel level, BlockPos pos) {
        teardown(level, new ChunkloadOwnerId.BlockOwner(pos));
    }

    /**
     * Called every chunkloadRecheckIntervalTicks by ChunkloadGrowthDirector. For each tracked
     * owner: resolve its live entity/block and config entry, then grow if due. An owner whose
     * entity/block can't currently be resolved, or whose config entry was removed, is frozen in
     * place rather than torn down - only an actual block-content mismatch (broken without going
     * through unregisterBlockOwner) auto-tears-down, as a defensive leak guard.
     */
    public static void recheckAll(ServerLevel level) {
        for (Map.Entry<ChunkloadOwnerId, ChunkloadState> owner : ChunkloadData.snapshot(level).entrySet()) {
            ChunkloadOwnerId ownerId = owner.getKey();
            ChunkloadState state = owner.getValue();

            if (ownerId instanceof ChunkloadOwnerId.EntityOwner entityOwner) {
                Entity entity = level.getEntity(entityOwner.entityId());
                if (entity == null) {
                    continue;
                }
                ChunkloadEntry entry = ChunkloadEntryLookup.forEntityType(entity.getType());
                if (entry == null) {
                    continue;
                }
                growOwner(level, ownerId, entry, state);
            } else if (ownerId instanceof ChunkloadOwnerId.BlockOwner blockOwner) {
                ResourceLocation actualBlockId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(blockOwner.pos()).getBlock());
                ResourceLocation expected = state.expectedBlockId();
                if (expected != null && !expected.equals(actualBlockId)) {
                    LOGGER.warn(
                            "spore_containment_breach: chunkload block owner at {} no longer matches expected block {} (found {}) - tearing down",
                            blockOwner.pos(), expected, actualBlockId
                    );
                    teardown(level, ownerId);
                    continue;
                }
                ChunkloadEntry entry = ChunkloadEntryLookup.forBlockId(expected != null ? expected : actualBlockId);
                if (entry == null) {
                    continue;
                }
                growOwner(level, ownerId, entry, state);
            }
        }
    }

    private static void growOwner(ServerLevel level, ChunkloadOwnerId ownerId, ChunkloadEntry entry, ChunkloadState state) {
        long elapsed = level.getGameTime() - state.activationGameTime();
        double progress = Math.min(1.0, elapsed / (double) entry.ticksToMaxRadius());
        int targetRadius = entry.minRadius() + (int) Math.floor(progress * (entry.maxRadius() - entry.minRadius()));
        if (targetRadius <= state.lastIssuedRadius()) {
            return;
        }

        Set<ChunkCircleOffsets.ChunkOffset> toAdd = new LinkedHashSet<>(ChunkCircleOffsets.fullOffsets(targetRadius));
        toAdd.removeAll(ChunkCircleOffsets.fullOffsets(state.lastIssuedRadius()));
        Set<ChunkCircleOffsets.ChunkOffset> tickingOffsets = ChunkCircleOffsets.fullOffsets(entry.tickingRadius());

        ChunkPos anchor = state.anchorChunk();
        for (ChunkCircleOffsets.ChunkOffset offset : toAdd) {
            boolean ticking = tickingOffsets.contains(offset);
            forceChunkForOwner(level, ownerId, anchor.x + offset.dx(), anchor.z + offset.dz(), true, ticking);
        }
        ChunkloadData.updateLastIssuedRadius(level, ownerId, targetRadius);
    }

    private static void teardown(ServerLevel level, ChunkloadOwnerId ownerId) {
        ChunkloadState state = ChunkloadData.getState(level, ownerId);
        if (state == null) {
            return;
        }
        ChunkPos anchor = state.anchorChunk();
        for (ChunkCircleOffsets.ChunkOffset offset : ChunkCircleOffsets.fullOffsets(state.lastIssuedRadius())) {
            int chunkX = anchor.x + offset.dx();
            int chunkZ = anchor.z + offset.dz();
            // A chunk may carry either a ticking or non-ticking ticket from us - remove is a
            // harmless no-op for whichever flag wasn't actually used.
            forceChunkForOwner(level, ownerId, chunkX, chunkZ, false, true);
            forceChunkForOwner(level, ownerId, chunkX, chunkZ, false, false);
        }
        ChunkloadData.remove(level, ownerId);
    }

    static void teardownEntityOwner(ServerLevel level, Entity entity) {
        teardown(level, new ChunkloadOwnerId.EntityOwner(entity.getUUID()));
    }

    private static boolean forceChunkForOwner(ServerLevel level, ChunkloadOwnerId ownerId, int chunkX, int chunkZ, boolean add, boolean ticking) {
        TicketController activeController = controller;
        if (activeController == null) {
            LOGGER.warn("spore_containment_breach: chunkload ticket controller not yet registered, dropping forceChunk call");
            return false;
        }
        if (ownerId instanceof ChunkloadOwnerId.EntityOwner entityOwner) {
            return activeController.forceChunk(level, entityOwner.entityId(), chunkX, chunkZ, add, ticking);
        } else if (ownerId instanceof ChunkloadOwnerId.BlockOwner blockOwner) {
            return activeController.forceChunk(level, blockOwner.pos(), chunkX, chunkZ, add, ticking);
        }
        return false;
    }
}
