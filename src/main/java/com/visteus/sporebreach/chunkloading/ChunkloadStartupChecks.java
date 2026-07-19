package com.visteus.sporebreach.chunkloading;

import com.Harbinger.Spore.core.SConfig;
import com.mojang.logging.LogUtils;
import com.visteus.sporebreach.SporeContainmentBreach;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import org.slf4j.Logger;

/**
 * One-shot warning that Spore's own built-in single-chunk Proto loader (SConfig.SERVER.proto_chunk)
 * is redundant now that this mod's chunkload system is the source of truth for Goal #4. We don't
 * mutate Spore's config at runtime - leaving proto_chunk on is only a harmless extra 1-chunk
 * ticket - so this just recommends the user disable it, mirroring SporeSpawnSuppression's use of
 * the same event for reading another mod's config at the right lifecycle point.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class ChunkloadStartupChecks {

    private static final Logger LOGGER = LogUtils.getLogger();

    private ChunkloadStartupChecks() {
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        if (SConfig.SERVER.proto_chunk.get()) {
            LOGGER.warn(
                    "spore_containment_breach: Spore's own proto_chunk chunkloader is enabled and redundant now that "
                            + "spore_containment_breach's chunkloading system manages Proto-Hivemind/Mound chunkloading - "
                            + "consider disabling proto_chunk in Spore's config"
            );
        }
    }
}
