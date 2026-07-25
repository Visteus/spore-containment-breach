package com.visteus.sporebreach.structuregrowth;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

/**
 * Parses {@code structureGrowthReplaceableBlocks} into block tags/ids and checks a candidate block
 * state against them. Re-parsed on every call (same no-caching convention as {@code
 * StructureAnchorSelector}) so a config edit takes effect immediately. Each entry is either
 * {@code #namespace:path} (a block tag) or {@code namespace:path} (a single block id). Air is
 * always treated as replaceable regardless of the configured list. An empty list disables the
 * restriction entirely - {@link #canReplace} then allows anything, matching growth's old
 * unconditional-overwrite behavior - rather than the opposite (nothing but air replaceable), since
 * an empty whitelist is a more useful "off switch" than a maximally restrictive one.
 */
public final class ReplaceableBlockSet {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final List<TagKey<Block>> tags;
    private final List<Block> blocks;
    private final boolean unrestricted;

    private ReplaceableBlockSet(List<TagKey<Block>> tags, List<Block> blocks, boolean unrestricted) {
        this.tags = tags;
        this.blocks = blocks;
        this.unrestricted = unrestricted;
    }

    public static ReplaceableBlockSet fromConfig(List<? extends String> raw) {
        List<TagKey<Block>> tags = new ArrayList<>();
        List<Block> blocks = new ArrayList<>();
        for (String entry : raw) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                if (trimmed.startsWith("#")) {
                    tags.add(TagKey.create(Registries.BLOCK, ResourceLocation.parse(trimmed.substring(1))));
                } else {
                    ResourceLocation id = ResourceLocation.parse(trimmed);
                    Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(id);
                    if (block.isEmpty()) {
                        LOGGER.warn("sporebreach: unknown block in structureGrowthReplaceableBlocks entry '{}'", entry);
                        continue;
                    }
                    blocks.add(block.get());
                }
            } catch (Exception e) {
                LOGGER.warn("sporebreach: invalid structureGrowthReplaceableBlocks entry '{}': {}", entry, e.getMessage());
            }
        }
        return new ReplaceableBlockSet(tags, blocks, tags.isEmpty() && blocks.isEmpty());
    }

    public boolean canReplace(BlockState state) {
        if (unrestricted || state.isAir()) {
            return true;
        }
        for (TagKey<Block> tag : tags) {
            if (state.is(tag)) {
                return true;
            }
        }
        for (Block block : blocks) {
            if (state.is(block)) {
                return true;
            }
        }
        return false;
    }
}
