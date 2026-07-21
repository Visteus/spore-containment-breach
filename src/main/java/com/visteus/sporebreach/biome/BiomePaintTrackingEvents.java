package com.visteus.sporebreach.biome;

import com.Harbinger.Spore.Sentities.Organoids.Mound;
import com.Harbinger.Spore.Sentities.Organoids.Proto;
import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.chunkloading.ChunkCircleOffsets;
import com.visteus.sporebreach.chunkloading.ChunkloadData;
import com.visteus.sporebreach.chunkloading.ChunkloadOwnerId;
import com.visteus.sporebreach.chunkloading.ChunkloadState;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Starts Goal #5's "lingering scar" countdown when a Mound/Proto-Hivemind dies: unclaims every
 * column within its biome-paint spread boundary, mirroring {@link
 * com.visteus.sporebreach.corruption.CorruptionContributionEvents}'s creation/death-edge listener
 * shape. Reads the owner's current spread boundary from {@link BiomePaintManager} (which grows
 * independently of, but in lockstep with, chunkload's own radius) and its anchor chunk from {@link
 * ChunkloadData}, since both organoid types share that one anchor. Iterating the whole boundary
 * circle rather than just the chunks the BFS spread has actually reached so far is a harmless
 * superset - {@link BiomePaintData#unclaim} no-ops on a chunk this owner never actually claimed.
 * Deliberately doesn't release the underlying force-load tickets or repaint anything here - the
 * column must stay loaded and painted as {@code infection_zone} through its scar countdown;
 * actually downgrading to {@code dead_scar} (and releasing the ticket) happens in {@link
 * BiomePaintManager#processDowngrades}.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class BiomePaintTrackingEvents {

    private BiomePaintTrackingEvents() {
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!SporeBreachServerConfig.BIOME_PAINT_ENABLED.get() || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getEntity() instanceof Mound) && !(event.getEntity() instanceof Proto)) {
            return;
        }
        Entity entity = event.getEntity();

        ChunkloadOwnerId ownerId = new ChunkloadOwnerId.EntityOwner(entity.getUUID());
        ChunkloadState chunkloadState = ChunkloadData.getState(level, ownerId);
        if (chunkloadState == null) {
            return;
        }

        int radius = BiomePaintManager.getAllowedRadius(entity.getUUID());
        if (radius <= 0) {
            return;
        }

        long now = level.getGameTime();
        long scarDelayTicks = SporeBreachServerConfig.BIOME_PAINT_SCAR_DELAY_TICKS.get();
        ChunkPos anchor = chunkloadState.anchorChunk();
        for (ChunkCircleOffsets.ChunkOffset offset : ChunkCircleOffsets.fullOffsets(radius)) {
            ChunkPos pos = new ChunkPos(anchor.x + offset.dx(), anchor.z + offset.dz());
            BiomePaintData.unclaim(level, pos, entity.getUUID(), now, scarDelayTicks);
        }
    }
}
