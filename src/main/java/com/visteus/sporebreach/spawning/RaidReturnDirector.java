package com.visteus.sporebreach.spawning;

import com.Harbinger.Spore.Sentities.BaseEntities.Calamity;
import com.Harbinger.Spore.Sentities.BaseEntities.Infected;
import com.Harbinger.Spore.Sentities.Organoids.Proto;
import com.Harbinger.Spore.Sentities.Utility.Vanguard;
import com.mojang.logging.LogUtils;
import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.chunkloading.ChunkloadManager;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

/**
 * New Goal #1 behavior: raid survivors (and any backup a raiding Vanguard calls in mid-fight, see
 * {@link com.visteus.sporebreach.mixin.VanguardMixin}) walk back to the Proto-Hivemind that sent
 * them once they've engaged at the raid's target for a while, converting into biomass for that
 * Proto on arrival. Follows the same tick-cadence skeleton as {@link RaidTravelSweepDirector},
 * piggybacking on the same sweep interval since both are periodic per-raider bookkeeping over
 * {@link RaidRegistry}.
 * <p>
 * Each living raider moves through a small state machine tracked via {@link
 * RaidEngagementTracker}: not-yet-arrived (still on the outbound leg driven by {@link
 * ProtoRaidDirector#attempt}) -&gt; engaging (arrived at the target, fighting) -&gt; returning
 * (past the engagement window, redirected toward the Proto) -&gt; paid out (close enough to the
 * Proto, discarded with biomass granted). A raid whose Proto has died is left alone entirely -
 * there's nowhere to send survivors and nothing to pay out, so they keep their last order and
 * fall back to normal despawn handling.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class RaidReturnDirector {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static long tickCounter;

    private RaidReturnDirector() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!event.getServer().tickRateManager().runsNormally()) {
            return;
        }
        tickCounter++;
        int interval = SporeBreachServerConfig.PROTO_RAID_TRAVEL_SWEEP_INTERVAL_TICKS.get();
        if (interval <= 0 || tickCounter % interval != 0 || !SporeBreachServerConfig.ENABLE_PROTO_RAID_RETURN.get()) {
            return;
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (RaidRegistry.RaidRecord record : RaidRegistry.snapshot(level)) {
                sweepRaid(level, record);
            }
        }
    }

    private static void sweepRaid(ServerLevel level, RaidRegistry.RaidRecord record) {
        if (!(level.getEntity(record.protoId()) instanceof Proto proto)) {
            return;
        }

        long searchRadiusSq = (long) SporeBreachServerConfig.PROTO_RAID_SEARCH_RADIUS.get() * SporeBreachServerConfig.PROTO_RAID_SEARCH_RADIUS.get();
        int engagementTicks = SporeBreachServerConfig.PROTO_RAID_ENGAGEMENT_TICKS.get();
        int maxReturnTicks = SporeBreachServerConfig.PROTO_RAID_RETURN_MAX_DURATION_TICKS.get();
        long now = level.getGameTime();

        for (UUID raiderId : RaidRegistry.livingMembers(level, record)) {
            Entity raider = level.getEntity(raiderId);
            if (raider == null) {
                RaidEngagementTracker.clear(level, raiderId);
                continue;
            }

            Long arrivalTime = RaidEngagementTracker.arrivalGameTime(level, raiderId);
            if (arrivalTime == null) {
                if (raider.blockPosition().distSqr(record.target()) <= searchRadiusSq) {
                    RaidEngagementTracker.markArrived(level, raiderId);
                }
                continue;
            }

            long ticksSinceArrival = now - arrivalTime;
            if (ticksSinceArrival < engagementTicks) {
                continue;
            }

            BlockPos protoPos = proto.blockPosition();
            if (raider.blockPosition().distSqr(protoPos) <= searchRadiusSq) {
                payOut(level, proto, raider, raiderId);
                continue;
            }

            long ticksReturning = ticksSinceArrival - engagementTicks;
            if (ticksReturning > maxReturnTicks) {
                abandonReturn(level, raiderId);
                continue;
            }

            redirectHome(level, raider, raiderId, protoPos);
        }
    }

    /**
     * Mirrors {@link ProtoRaidDirector#applyDirectedTravel} but pointed at the Proto's current
     * position instead of the raid's original target - same per-type branch, same
     * "inching forward" travel-chunkload reuse, gated behind the same {@code
     * protoRaidDirectedTravel} toggle.
     */
    private static void redirectHome(ServerLevel level, Entity raider, UUID raiderId, BlockPos protoPos) {
        if (!SporeBreachServerConfig.PROTO_RAID_DIRECTED_TRAVEL.get()) {
            return;
        }
        if (raider instanceof Calamity calamity) {
            calamity.setSearchArea(protoPos);
        } else if (raider instanceof Vanguard vanguard) {
            vanguard.setVillage(protoPos);
        } else if (raider instanceof Infected infected) {
            infected.setSearchPos(protoPos);
            ChunkPos currentChunk = RaidTravelTracker.currentChunk(level, raiderId);
            ChunkPos newChunk = new ChunkPos(raider.blockPosition());
            RaidTravelTracker.track(level, raiderId, newChunk);
            ChunkloadManager.advanceRaidTravelChunk(level, raiderId, newChunk, currentChunk);
        }
    }

    private static void payOut(ServerLevel level, Proto proto, Entity raider, UUID raiderId) {
        int payout = SporeBreachServerConfig.PROTO_RAID_RETURN_BASE_BIOMASS.get();
        if (raider instanceof Infected infected) {
            payout += infected.getKills();
        } else if (raider instanceof Calamity calamity) {
            payout += calamity.getKills();
        }
        proto.addBiomass(payout);
        LOGGER.debug("sporebreach: raider {} returned to Proto {} for {} biomass", raiderId, proto.getUUID(), payout);

        stopTravelTracking(level, raiderId);
        RaidEngagementTracker.clear(level, raiderId);
        raider.discard();
    }

    private static void abandonReturn(ServerLevel level, UUID raiderId) {
        stopTravelTracking(level, raiderId);
        RaidEngagementTracker.clear(level, raiderId);
    }

    private static void stopTravelTracking(ServerLevel level, UUID raiderId) {
        ChunkPos lastChunk = RaidTravelTracker.stopTracking(level, raiderId);
        if (lastChunk != null) {
            ChunkloadManager.releaseRaidTravelChunk(level, raiderId, lastChunk);
        }
    }
}
