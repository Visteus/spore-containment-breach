package com.visteus.sporebreach.genesis;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.slf4j.Logger;

/**
 * Parses {@code moundGenesisEligibleStructures} into structure tags/ids and checks a candidate
 * structure against them. Re-parsed on every call (same no-caching convention as
 * {@code SpawnPool.fromConfig} from the prior Goal #1 plan) so a config edit takes effect
 * immediately. Each entry is either {@code #namespace:path} (a structure tag) or
 * {@code namespace:path} (a single structure id) - same {@code Holder<Structure>.is(...)} idiom
 * Spore's own {@code StructureModification} uses for its (fixed) laboratories tag check.
 */
public final class StructureAnchorSelector {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final List<TagKey<Structure>> tags;
    private final List<ResourceKey<Structure>> ids;

    private StructureAnchorSelector(List<TagKey<Structure>> tags, List<ResourceKey<Structure>> ids) {
        this.tags = tags;
        this.ids = ids;
    }

    public static StructureAnchorSelector fromConfig(List<? extends String> raw) {
        List<TagKey<Structure>> tags = new ArrayList<>();
        List<ResourceKey<Structure>> ids = new ArrayList<>();
        for (String entry : raw) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                if (trimmed.startsWith("#")) {
                    tags.add(TagKey.create(Registries.STRUCTURE, ResourceLocation.parse(trimmed.substring(1))));
                } else {
                    ids.add(ResourceKey.create(Registries.STRUCTURE, ResourceLocation.parse(trimmed)));
                }
            } catch (Exception e) {
                LOGGER.warn("sporebreach: invalid moundGenesisEligibleStructures entry '{}': {}", entry, e.getMessage());
            }
        }
        return new StructureAnchorSelector(tags, ids);
    }

    public boolean matches(Holder<Structure> holder) {
        for (TagKey<Structure> tag : tags) {
            if (holder.is(tag)) {
                return true;
            }
        }
        for (ResourceKey<Structure> id : ids) {
            if (holder.is(id)) {
                return true;
            }
        }
        return false;
    }
}
