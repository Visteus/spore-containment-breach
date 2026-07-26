package com.visteus.sporebreach.genesis;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
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
 * immediately. Each entry is {@code #namespace:path[|chance]} (a structure tag) or
 * {@code namespace:path[|chance]} (a single structure id) - same {@code Holder<Structure>.is(...)}
 * idiom Spore's own {@code StructureModification} uses for its (fixed) laboratories tag check.
 *
 * <p>{@code chance} is the 0.0-1.0 probability that a matched structure instance produces Mounds
 * at all, and defaults to 1.0 when omitted or unparseable - a typo'd chance should not silently
 * make an otherwise-valid structure ineligible. An explicit id entry wins over a tag entry that
 * also covers the structure, so a tag can set a baseline that individual structures override;
 * within one category the first matching entry wins.
 */
public final class StructureAnchorSelector {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final double DEFAULT_CHANCE = 1.0;

    private record TagEntry(TagKey<Structure> tag, double chance) {
    }

    private record IdEntry(ResourceKey<Structure> id, double chance) {
    }

    private final List<TagEntry> tags;
    private final List<IdEntry> ids;

    private StructureAnchorSelector(List<TagEntry> tags, List<IdEntry> ids) {
        this.tags = tags;
        this.ids = ids;
    }

    public static StructureAnchorSelector fromConfig(List<? extends String> raw) {
        List<TagEntry> tags = new ArrayList<>();
        List<IdEntry> ids = new ArrayList<>();
        for (String entry : raw) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                String target = trimmed;
                double chance = DEFAULT_CHANCE;
                int separator = trimmed.indexOf('|');
                if (separator >= 0) {
                    target = trimmed.substring(0, separator).trim();
                    chance = parseChance(trimmed.substring(separator + 1).trim(), entry);
                }

                if (target.startsWith("#")) {
                    tags.add(new TagEntry(
                            TagKey.create(Registries.STRUCTURE, ResourceLocation.parse(target.substring(1))), chance
                    ));
                } else {
                    ids.add(new IdEntry(
                            ResourceKey.create(Registries.STRUCTURE, ResourceLocation.parse(target)), chance
                    ));
                }
            } catch (Exception e) {
                LOGGER.warn("sporebreach: invalid moundGenesisEligibleStructures entry '{}': {}", entry, e.getMessage());
            }
        }
        return new StructureAnchorSelector(tags, ids);
    }

    private static double parseChance(String raw, String entry) {
        try {
            double parsed = Double.parseDouble(raw);
            if (parsed < 0.0 || parsed > 1.0) {
                LOGGER.warn(
                        "sporebreach: moundGenesisEligibleStructures entry '{}' has an out-of-range chance,"
                                + " clamping to 0.0-1.0", entry
                );
            }
            return Math.clamp(parsed, 0.0, 1.0);
        } catch (NumberFormatException e) {
            LOGGER.warn(
                    "sporebreach: moundGenesisEligibleStructures entry '{}' has an unreadable chance,"
                            + " using {}", entry, DEFAULT_CHANCE
            );
            return DEFAULT_CHANCE;
        }
    }

    /**
     * The configured Mound-spawn chance for this structure, or empty if it is not eligible at all.
     */
    public OptionalDouble chanceFor(Holder<Structure> holder) {
        for (IdEntry entry : ids) {
            if (holder.is(entry.id())) {
                return OptionalDouble.of(entry.chance());
            }
        }
        for (TagEntry entry : tags) {
            if (holder.is(entry.tag())) {
                return OptionalDouble.of(entry.chance());
            }
        }
        return OptionalDouble.empty();
    }
}
