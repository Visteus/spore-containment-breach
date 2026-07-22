package com.visteus.sporebreach.structuregrowth;

import com.Harbinger.Spore.Sentities.Organoids.Mound;
import com.Harbinger.Spore.Sentities.Organoids.Proto;
import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.config.SporeBreachServerConfig;
import com.visteus.sporebreach.config.SporeBreachServerConfig.StructureGrowthMode;
import com.visteus.sporebreach.tracking.OrganoidRegistry;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Periodic driver for Goal #3's structure growth, copying the ServerTickEvent.Post + static tick
 * counter + interval gate skeleton every other director in this mod uses. Two cadences per
 * organoid type: recheck (start a new job) and pass (advance an in-progress job). No-ops entirely
 * when structureGrowthMode isn't SPORE_BREACH_TOWERS - see {@code MoundStructureMixin} and
 * {@code ProtoMixin#sporebreach$gateBaseCasingGrowth} for the base-game side of that switch.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class StructureGrowthDirector {

    private static long moundRecheckCounter;
    private static long moundPassCounter;
    private static long protoRecheckCounter;
    private static long protoPassCounter;

    private StructureGrowthDirector() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!event.getServer().tickRateManager().runsNormally()) {
            return;
        }
        if (SporeBreachServerConfig.STRUCTURE_GROWTH_MODE.get() != StructureGrowthMode.SPORE_BREACH_TOWERS) {
            return;
        }

        moundRecheckCounter++;
        moundPassCounter++;
        protoRecheckCounter++;
        protoPassCounter++;

        boolean moundRecheck = due(moundRecheckCounter, SporeBreachServerConfig.MOUND_STRUCTURE_RECHECK_INTERVAL_TICKS.get());
        boolean moundPass = due(moundPassCounter, SporeBreachServerConfig.MOUND_STRUCTURE_PASS_INTERVAL_TICKS.get());
        boolean protoRecheck = due(protoRecheckCounter, SporeBreachServerConfig.PROTO_STRUCTURE_RECHECK_INTERVAL_TICKS.get());
        boolean protoPass = due(protoPassCounter, SporeBreachServerConfig.PROTO_STRUCTURE_PASS_INTERVAL_TICKS.get());

        if (!moundRecheck && !moundPass && !protoRecheck && !protoPass) {
            return;
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (moundRecheck || moundPass) {
                for (Mound mound : OrganoidRegistry.get(level)) {
                    if (moundRecheck) {
                        MoundStructureGrowth.tryStartJob(level, mound);
                    }
                    if (moundPass) {
                        MoundStructureGrowth.tryAdvanceJob(level, mound);
                    }
                }
            }
            if (protoRecheck || protoPass) {
                for (Proto proto : OrganoidRegistry.getProtos(level)) {
                    if (protoRecheck) {
                        ProtoStructureGrowth.tryStartJob(level, proto);
                    }
                    if (protoPass) {
                        ProtoStructureGrowth.tryAdvanceJob(level, proto);
                    }
                }
            }
        }
    }

    private static boolean due(long counter, int interval) {
        return interval > 0 && counter % interval == 0;
    }
}
