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

    public static final BooleanValue SUPPRESS_VANILLA_SPORE_SPAWNS;

    public static final IntValue DIRECTOR_TICK_INTERVAL_TICKS;
    public static final IntValue DIRECTOR_BUDGET_PER_CYCLE;

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

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("world");
        PROTECTED_SPAWN_RADIUS_CHUNKS = builder
                .comment(
                        " The director will not spawn anything within this many chunks of world spawn.",
                        " Set to 0 to disable. Default 3."
                )
                .defineInRange("protectedSpawnRadiusChunks", 3, 0, 64);
        builder.pop();

        builder.push("spawning");
        SUPPRESS_VANILLA_SPORE_SPAWNS = builder
                .comment(
                        " If true, clears Spore's own biome/structure spawn-injection lists",
                        " (SConfig.SERVER.spawns and structure_spawns) during common setup, so vanilla",
                        " natural spawning no longer places Spore mobs in dark areas. Organoid-driven",
                        " spawning (Mounds/Proto-Hiveminds) is unaffected by this toggle.",
                        " Applied once at startup - requires a game restart to take effect.",
                        " Default true."
                )
                .define("suppressVanillaSporeSpawns", true);
        builder.pop();

        builder.push("director");
        DIRECTOR_TICK_INTERVAL_TICKS = builder
                .comment(" How often (in ticks) the spawn director scans a level. Default 100 (5s).")
                .defineInRange("directorTickIntervalTicks", 100, 20, Integer.MAX_VALUE);
        DIRECTOR_BUDGET_PER_CYCLE = builder
                .comment(
                        " Max organoids per director cycle per dimension, closest-to-player first, which actually get spawns.",
                        " Default 3."
                )
                .defineInRange("directorBudgetPerCycle", 3, 1, Integer.MAX_VALUE);
        builder.pop();

        builder.push("mound");
        MOUND_DEFENDER_COOLDOWN_TICKS = builder
                .comment(" Per-Mound cooldown between defender-spawn attempts. Default 600 (30s).")
                .defineInRange("moundDefenderCooldownTicks", 600, 20, Integer.MAX_VALUE);
        MOUND_DEFENDER_SEARCH_RADIUS = builder
                .comment(
                        " Radius (blocks) used for both the defender cap and defender placement",
                        " search around a Mound. Default 32."
                )
                .defineInRange("moundDefenderSearchRadius", 32, 0, Integer.MAX_VALUE);
        MOUND_MAX_DEFENDERS_NEARBY = builder
                .comment(
                        " Max number of mound defenders allowed within the radius defined by moundDefenderSearchRadius. ",
                        " If there are already this many defenders nearby, the Mound wi not spawn any more defenders. ",
                        " Default 12."
                )
                .defineInRange("moundMaxDefendersNearby", 12, 0, Integer.MAX_VALUE);
        MOUND_DEFENDER_SPAWN_POOL = builder
                .comment(
                        " Entities a Mound may spawn as defenders",
                        " Format: \"entityId|weight|min|max\"."
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
                .comment(" Should Mounds be spawned guaranteedat eligible structures? Default true.")
                .define("enabled", true);
        MOUND_GENESIS_SCAN_INTERVAL_TICKS = builder
                .comment(
                        " How often (in ticks) the game checks for eligible structures to genesis-spawn Mounds at. Default 40 (2s)."
                )
                .defineInRange("scanIntervalTicks", 40, 20, Integer.MAX_VALUE);
        MOUND_GENESIS_ELIGIBLE_STRUCTURES = builder
                .comment(
                        " Structures a Mound may genesis-spawn at, the first time a player comes near one.",
                        " Each entry is either \"#namespace:path\" (a structure tag) or \"namespace:path\" (a single structure id)."
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
                        " Minimum number of mounds per structure. The director will attempt to spawn at least this many mounds",
                        " Default 1."
                )
                .defineInRange("countMin", 1, 0, Integer.MAX_VALUE);
        MOUND_GENESIS_COUNT_MAX = builder
                .comment(" Maximum number of mounds per structure. Default 1.")
                .defineInRange("countMax", 1, 0, Integer.MAX_VALUE);
        builder.pop();

        builder.pop();

        builder.push("proto");
        PROTO_RAID_COOLDOWN_MIN_TICKS = builder
                .comment(
                        " Lower bound of the randomized per-Proto raid cooldown.",
                        " Default 18000 (15 min)."
                )
                .defineInRange("protoRaidCooldownMinTicks", 18000, 20, Integer.MAX_VALUE);
        PROTO_RAID_COOLDOWN_MAX_TICKS = builder
                .comment(
                        " Upper bound of the randomized per-Proto raid cooldown in ticks.",
                        " This cooldown period is rolled per-Proto, and follows a normal distribution between the min and max values.",
                        " Default 72000 (1 hr). Should be >= protoRaidCooldownMinTicks."
                )
                .defineInRange("protoRaidCooldownMaxTicks", 72000, 20, Integer.MAX_VALUE);
        PROTO_RAID_SEARCH_RADIUS = builder
                .comment(" Radius (blocks) to find raid targets - structure anchors or players. Default 128.")
                .defineInRange("protoRaidSearchRadius", 128, 0, Integer.MAX_VALUE);
        PROTO_RAID_GROUP_SIZE_MIN = builder
                .comment(
                        " Baseline minimum raid group size. World Corruption level will scale this up.",
                        " Default 2."
                )
                .defineInRange("protoRaidGroupSizeMin", 2, 0, Integer.MAX_VALUE);
        PROTO_RAID_GROUP_SIZE_MAX = builder
                .comment(
                        " Baseline maximum raid group size. World Corruption level will scale this up.",
                        " Default 5."
                )
                .defineInRange("protoRaidGroupSizeMax", 5, 0, Integer.MAX_VALUE);
        PROTO_RAID_SPAWN_POOL = builder
                .comment(
                        " Entities a Proto-Hivemind may send on a periodic raid. Format: \"entityId|weight|min|max\".",
                        " Calamities are stripped from this pool, as they are inserted into raids at higher World Corruption levels."
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
                        " Calamities a Proto-Hivemind may fold into a raid once CorruptionGate allows it.",
                        " Format: \"entityId|weight|min|max\"."
                )
                .defineListAllowEmpty(
                        "protoCalamitySpawnPool", Lists::newArrayList, () -> "modid:entity_id|weight|min|max", o -> o instanceof String
                );
        builder.pop();

        builder.push("structures");
        STRUCTURE_ANCHOR_SEARCH_RADIUS = builder
                .comment(
                        " Radius (blocks) to scan for a LivingStructureBlocks anchor near an organoid before",
                        " falling back to the organoid's own position as an anchor.",
                        " Default 8."
                )
                .defineInRange("structureAnchorSearchRadius", 8, 0, Integer.MAX_VALUE);
        builder.pop();

        SPEC = builder.build();
    }

    private SporeBreachServerConfig() {
    }
}
