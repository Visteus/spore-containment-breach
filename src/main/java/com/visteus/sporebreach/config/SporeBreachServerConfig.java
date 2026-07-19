package com.visteus.sporebreach.config;

import com.google.common.collect.Lists;
import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public final class SporeBreachServerConfig {

    public static final ModConfigSpec SPEC;

    public static final IntValue PROTECTED_SPAWN_RADIUS_CHUNKS;

    public static final IntValue COORDINATOR_TICK_INTERVAL_TICKS;
    public static final IntValue COORDINATOR_BUDGET_PER_CYCLE;

    public static final IntValue MOUND_DEFENDER_COOLDOWN_TICKS;
    public static final IntValue MOUND_DEFENDER_SEARCH_RADIUS;
    public static final IntValue MOUND_MAX_DEFENDERS_NEARBY;
    public static final ConfigValue<List<? extends String>> MOUND_DEFENDER_SPAWN_POOL;

    public static final IntValue PROTO_RAID_COOLDOWN_MIN_TICKS;
    public static final IntValue PROTO_RAID_COOLDOWN_MAX_TICKS;
    public static final IntValue PROTO_RAID_SEARCH_RADIUS;
    public static final IntValue PROTO_RAID_GROUP_SIZE_MIN;
    public static final IntValue PROTO_RAID_GROUP_SIZE_MAX;
    public static final ConfigValue<List<? extends String>> PROTO_RAID_SPAWN_POOL;
    public static final ConfigValue<List<? extends String>> PROTO_CALAMITY_SPAWN_POOL;

    public static final IntValue STRUCTURE_ANCHOR_SEARCH_RADIUS;

    public static final BooleanValue MOUND_GENESIS_ENABLED;
    public static final IntValue MOUND_GENESIS_SCAN_INTERVAL_TICKS;
    public static final ConfigValue<List<? extends String>> MOUND_GENESIS_ELIGIBLE_STRUCTURES;
    public static final IntValue MOUND_GENESIS_COUNT_MIN;
    public static final IntValue MOUND_GENESIS_COUNT_MAX;
    public static final IntValue MOUND_GENESIS_PLACEMENT_RADIUS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("world");
        PROTECTED_SPAWN_RADIUS_CHUNKS = builder
                .comment(
                        "No organoid spawn-anchor logic (Mound defender spawn, Proto raid target, future",
                        "structure placement) may target a chunk within this chunk-radius of world spawn.",
                        "Default 3."
                )
                .defineInRange("protectedSpawnRadiusChunks", 3, 0, 64);
        builder.pop();

        builder.push("coordinator");
        COORDINATOR_TICK_INTERVAL_TICKS = builder
                .comment("How often (in ticks) the spawn coordinator scans a level. Default 100 (5s).")
                .defineInRange("coordinatorTickIntervalTicks", 100, 20, Integer.MAX_VALUE);
        COORDINATOR_BUDGET_PER_CYCLE = builder
                .comment(
                        "Max organoids actually dispatched (spawn/raid attempted) per coordinator cycle",
                        "per level, closest-to-any-player first. Default 3."
                )
                .defineInRange("coordinatorBudgetPerCycle", 3, 1, Integer.MAX_VALUE);
        builder.pop();

        builder.push("mound");
        MOUND_DEFENDER_COOLDOWN_TICKS = builder
                .comment("Per-Mound cooldown between defender-spawn attempts. Default 600 (30s).")
                .defineInRange("moundDefenderCooldownTicks", 600, 20, Integer.MAX_VALUE);
        MOUND_DEFENDER_SEARCH_RADIUS = builder
                .comment(
                        "Radius (blocks) used for both the defender density cap and defender placement",
                        "search around a Mound. Default 32."
                )
                .defineInRange("moundDefenderSearchRadius", 32, 0, Integer.MAX_VALUE);
        MOUND_MAX_DEFENDERS_NEARBY = builder
                .comment(
                        "Density cap on Mound-spawned defenders only (unrelated mobs and Proto raid",
                        "members don't count against this) - a per-Mound garrison size, not an area-wide",
                        "mob cap. Default 12."
                )
                .defineInRange("moundMaxDefendersNearby", 12, 0, Integer.MAX_VALUE);
        MOUND_DEFENDER_SPAWN_POOL = builder
                .comment(
                        "Entities a Mound may spawn as vicinity defenders. Format: \"entityId|weight|min|max\",",
                        "same grammar as Spore's own SConfig.SERVER.spawns. Entries tagged",
                        "spore_containment_breach:calamities_and_wombs are always stripped - Mounds can",
                        "never spawn calamities or Wombs. Placeholder defaults - tune to taste."
                )
                .defineListAllowEmpty(
                        "moundDefenderSpawnPool",
                        () -> Lists.newArrayList(
                                "spore:inf_human|80|1|2",
                                "spore:inf_villager|50|1|2",
                                "spore:inf_pillager|30|1|1"
                        ),
                        () -> "modid:entity_id|weight|min|max",
                        o -> o instanceof String
                );

        builder.push("spawning");
        MOUND_GENESIS_ENABLED = builder
                .comment("Should Mounds genesis-spawn at eligible structures at all? Default true.")
                .define("enabled", true);
        MOUND_GENESIS_SCAN_INTERVAL_TICKS = builder
                .comment(
                        "How often (in ticks) the genesis director checks online players' positions for",
                        "newly-discovered eligible structures. Cheap per-check (reads already-cached",
                        "structure references, doesn't force generation). Default 40 (2s)."
                )
                .defineInRange("scanIntervalTicks", 40, 20, Integer.MAX_VALUE);
        MOUND_GENESIS_ELIGIBLE_STRUCTURES = builder
                .comment(
                        "Structures a Mound may genesis-spawn at, the first time a player comes near one.",
                        "Each entry is either \"#namespace:path\" (a structure tag) or \"namespace:path\"",
                        "(a single structure id). Default covers all 12 wiki-documented Spore structures."
                )
                .defineListAllowEmpty(
                        "eligibleStructures",
                        () -> Lists.newArrayList(
                                "#spore:laboratories",
                                "#spore:churches",
                                "spore:cell",
                                "spore:celltower",
                                "spore:iceberg_mines",
                                "spore:lodge",
                                "spore:mass_grave"
                        ),
                        () -> "#namespace:tag_path or namespace:structure_id",
                        o -> o instanceof String
                );
        MOUND_GENESIS_COUNT_MIN = builder
                .comment(
                        "Minimum number of Mounds a single structure instance's one-time genesis event",
                        "produces. Default 1."
                )
                .defineInRange("countMin", 1, 0, Integer.MAX_VALUE);
        MOUND_GENESIS_COUNT_MAX = builder
                .comment("Maximum number of Mounds a genesis event produces. Default 1.")
                .defineInRange("countMax", 1, 0, Integer.MAX_VALUE);
        MOUND_GENESIS_PLACEMENT_RADIUS = builder
                .comment(
                        "Radius (blocks) around a structure's bounding-box center to search for valid ground",
                        "to place each genesis Mound. Default 16."
                )
                .defineInRange("placementRadius", 16, 0, Integer.MAX_VALUE);
        builder.pop();

        builder.pop();

        builder.push("proto");
        PROTO_RAID_COOLDOWN_MIN_TICKS = builder
                .comment("Lower bound of the randomized per-Proto raid cooldown. Default 18000 (15 min).")
                .defineInRange("protoRaidCooldownMinTicks", 18000, 20, Integer.MAX_VALUE);
        PROTO_RAID_COOLDOWN_MAX_TICKS = builder
                .comment(
                        "Upper bound of the randomized per-Proto raid cooldown. A new cooldown is rerolled",
                        "after every raid attempt using a triangular (centrally-weighted) distribution over",
                        "[min, max], not uniform. Default 72000 (1 hr). Should be >= protoRaidCooldownMinTicks."
                )
                .defineInRange("protoRaidCooldownMaxTicks", 72000, 20, Integer.MAX_VALUE);
        PROTO_RAID_SEARCH_RADIUS = builder
                .comment("Radius (blocks) to find raid targets - structure anchors or players. Default 128.")
                .defineInRange("protoRaidSearchRadius", 128, 0, Integer.MAX_VALUE);
        PROTO_RAID_GROUP_SIZE_MIN = builder
                .comment(
                        "Baseline (low-corruption) minimum raid group size. Goal #7's World Corruption",
                        "system scales this up at higher corruption tiers via CorruptionGate. Default 2."
                )
                .defineInRange("protoRaidGroupSizeMin", 2, 0, Integer.MAX_VALUE);
        PROTO_RAID_GROUP_SIZE_MAX = builder
                .comment("Baseline (low-corruption) maximum raid group size. Default 5.")
                .defineInRange("protoRaidGroupSizeMax", 5, 0, Integer.MAX_VALUE);
        PROTO_RAID_SPAWN_POOL = builder
                .comment(
                        "Entities a Proto-Hivemind may send on a periodic raid. Format:",
                        "\"entityId|weight|min|max\". Entries tagged",
                        "spore_containment_breach:calamities_and_wombs are always stripped from regular",
                        "raids. Defaults mirror Spore's own proto_summonable_troops roster (excluding",
                        "spore:mound, which isn't a raid-appropriate combatant)."
                )
                .defineListAllowEmpty(
                        "protoRaidSpawnPool",
                        () -> Lists.newArrayList(
                                "spore:vigil|20|1|2",
                                "spore:umarmed|20|1|2",
                                "spore:usurper|20|1|2",
                                "spore:braurei|15|1|1",
                                "spore:verva|15|1|1",
                                "spore:delusioner|15|1|1"
                        ),
                        () -> "modid:entity_id|weight|min|max",
                        o -> o instanceof String
                );
        PROTO_CALAMITY_SPAWN_POOL = builder
                .comment(
                        "Calamities a Proto-Hivemind may fold into a raid once CorruptionGate allows it.",
                        "Format: \"entityId|weight|min|max\". Inert in Goal #1 - CorruptionGate always denies",
                        "calamity spawning, so this pool is never consulted yet. Goal #7 populates this.",
                        "Empty by default."
                )
                .defineListAllowEmpty(
                        "protoCalamitySpawnPool", Lists::newArrayList, () -> "modid:entity_id|weight|min|max", o -> o instanceof String
                );
        builder.pop();

        builder.push("structures");
        STRUCTURE_ANCHOR_SEARCH_RADIUS = builder
                .comment(
                        "Radius (blocks) to scan for a LivingStructureBlocks anchor near an organoid before",
                        "falling back to the organoid's own position as the spawn/raid-target anchor.",
                        "Default 8."
                )
                .defineInRange("structureAnchorSearchRadius", 8, 0, Integer.MAX_VALUE);
        builder.pop();

        SPEC = builder.build();
    }

    private SporeBreachServerConfig() {
    }
}
