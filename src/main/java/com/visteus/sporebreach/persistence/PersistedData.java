package com.visteus.sporebreach.persistence;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.slf4j.Logger;

/**
 * Base for anything this mod persists under {@link SporeBreachSaveFolder}, instead of a
 * one-off vanilla SavedData subclass per concern. Subclasses own their in-memory state and only
 * need to implement the NBT round-trip; {@link PersistedDataManager} drives when
 * {@link #saveIfDirty()} actually gets called.
 */
public abstract class PersistedData {

    private static final Logger LOGGER = LogUtils.getLogger();

    private boolean dirty;

    protected PersistedData() {
        PersistedDataManager.register(this);
    }

    /**
     * Must be called once by subclasses after construction completes (e.g. at the end of a
     * static factory/singleton accessor) - never from this base constructor, since that would
     * invoke the subclass's overridden {@link #load(CompoundTag)} before the subclass's own
     * field initializers have run.
     */
    protected final void initialize() {
        loadFromDisk();
    }

    protected abstract String fileName();

    protected abstract CompoundTag save(CompoundTag tag);

    protected abstract void load(CompoundTag tag);

    protected final void markDirty() {
        dirty = true;
    }

    final void saveIfDirty() {
        if (!dirty) {
            return;
        }
        Path path = SporeBreachSaveFolder.resolve(fileName());
        try {
            NbtIo.writeCompressed(save(new CompoundTag()), path);
            dirty = false;
        } catch (IOException e) {
            LOGGER.error("sporebreach: failed to save {}", fileName(), e);
        }
    }

    private void loadFromDisk() {
        Path path = SporeBreachSaveFolder.resolve(fileName());
        if (!Files.exists(path)) {
            return;
        }
        try {
            load(NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap()));
        } catch (IOException e) {
            LOGGER.error("sporebreach: failed to load {}", fileName(), e);
        }
    }
}
