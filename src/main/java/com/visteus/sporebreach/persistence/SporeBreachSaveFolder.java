package com.visteus.sporebreach.persistence;

import com.mojang.logging.LogUtils;
import com.visteus.sporebreach.SporeContainmentBreach;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

/**
 * Resolves this mod's own per-world save location, {@code {world-folder}/.spore-breach/} -
 * sibling to vanilla's region/data/playerdata folders, but kept separate from vanilla's own
 * data/*.dat files so anything this mod persists (Goal #1's genesis tracking, later Goal #7
 * corruption state, etc.) lives in one place instead of scattered one-off SavedData files.
 */
@EventBusSubscriber(modid = SporeContainmentBreach.MODID)
public final class SporeBreachSaveFolder {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FOLDER_NAME = ".spore-breach";

    private static Path currentWorldFolder;

    private SporeBreachSaveFolder() {
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        Path folder = event.getServer().getWorldPath(LevelResource.ROOT).resolve(FOLDER_NAME);
        try {
            Files.createDirectories(folder);
            currentWorldFolder = folder;
        } catch (IOException e) {
            LOGGER.error("spore_containment_breach: could not create save folder {}", folder, e);
            currentWorldFolder = null;
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        currentWorldFolder = null;
    }

    public static Path resolve(String fileName) {
        if (currentWorldFolder == null) {
            throw new IllegalStateException("spore_containment_breach: save folder accessed outside a running server");
        }
        return currentWorldFolder.resolve(fileName);
    }
}
