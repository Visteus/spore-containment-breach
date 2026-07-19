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
    public static final IntValue PROTO_AGE_UP_INTERVAL_TICKS;
    public static final IntValue PROTO_MAX_AGE;

    public static final BooleanValue CORRUPTION_ENABLED;
    public static final IntValue CORRUPTION_CAP;
    public static final IntValue CORRUPTION_PER_MOUND_CREATED;
    public static final IntValue CORRUPTION_PER_MOUND_AGE_UP;
    public static final IntValue CORRUPTION_PER_PROTO_CREATED;
    public static final IntValue CORRUPTION_PER_PROTO_AGE_UP;
    public static final IntValue CORRUPTION_AGE_SCAN_INTERVAL_TICKS;
    public static final IntValue CORRUPTION_BREAKPOINT_RAIDS;
    public static final IntValue CORRUPTION_BREAKPOINT_INSTANT_EVOLUTION;
    public static final IntValue CORRUPTION_BREAKPOINT_CALAMITY_WOMB;
    public static final IntValue CORRUPTION_BREAKPOINT_CALAMITY_RAIDS;
    public static final IntValue CORRUPTION_BREAKPOINT_ADAPTATIONS;
    public static final IntValue CORRUPTION_BREAKPOINT_LINKED_SPAWNS;

    public static final IntValue CHUNKLOAD_RECHECK_INTERVAL_TICKS;
    public static final ConfigValue<List<? extends String>> CHUNKLOAD_ENTITY_OWNERS;
    public static final ConfigValue<List<? extends String>> CHUNKLOAD_BLOCK_OWNERS;

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
        PROTO_AGE_UP_INTERVAL_TICKS = builder
                .comment(
                        " Ticks between Proto-Hivemind age-levels. Unlike Mound, base Spore gives Proto no age of",
                        " its own, so this mod tracks one itself (see ProtoAgeTracker), starting at 0 when a Proto",
                        " is created. Default 18000 (15 min), matching the pace Mounds age at."
                )
                .defineInRange("protoAgeUpIntervalTicks", 18000, 20, Integer.MAX_VALUE);
        PROTO_MAX_AGE = builder
                .comment(
                        " Caps how many times a single Proto-Hivemind's tracked age can increase. Generous rather",
                        " than tight (unlike Mound's fixed 0-4), since an old Proto is meant to keep aging if left",
                        " alive. Default 20."
                )
                .defineInRange("protoMaxAge", 20, 0, Integer.MAX_VALUE);
        builder.pop();

        builder.push("corruption");
        CORRUPTION_ENABLED = builder
                .comment(" Master toggle for the World Corruption system. Default true.")
                .define("enabled", true);
        CORRUPTION_CAP = builder
                .comment(" Hard ceiling for a dimension's corruption value; increments clamp here. Default 1000.")
                .defineInRange("cap", 1000, 1, Integer.MAX_VALUE);
        CORRUPTION_PER_MOUND_CREATED = builder
                .comment(" Corruption added when a new Mound is created. Default 5.")
                .defineInRange("perMoundCreated", 5, 0, Integer.MAX_VALUE);
        CORRUPTION_PER_MOUND_AGE_UP = builder
                .comment(" Corruption added per age-level a Mound gains (Mound's own age, 0-4). Default 5.")
                .defineInRange("perMoundAgeUp", 5, 0, Integer.MAX_VALUE);
        CORRUPTION_PER_PROTO_CREATED = builder
                .comment(" Corruption added when a new Proto-Hivemind is created. Default 20.")
                .defineInRange("perProtoCreated", 20, 0, Integer.MAX_VALUE);
        CORRUPTION_PER_PROTO_AGE_UP = builder
                .comment(" Corruption added per age-level a Proto-Hivemind gains. Default 20.")
                .defineInRange("perProtoAgeUp", 20, 0, Integer.MAX_VALUE);
        CORRUPTION_AGE_SCAN_INTERVAL_TICKS = builder
                .comment(" How often (in ticks) Mounds/Protos are checked for age increases. Default 1200 (1 min), min 20.")
                .defineInRange("ageScanIntervalTicks", 1200, 20, Integer.MAX_VALUE);
        CORRUPTION_BREAKPOINT_RAIDS = builder
                .comment(" Corruption value at which Proto-Hivemind can target raids at Players (beyond existing Signal system). Default 100.")
                .defineInRange("breakpointRaids", 100, 0, Integer.MAX_VALUE);
        CORRUPTION_BREAKPOINT_INSTANT_EVOLUTION = builder
                .comment(" Corruption value at which newly spawned Infected may instantly evolve. Default 150.")
                .defineInRange("breakpointInstantEvolution", 150, 0, Integer.MAX_VALUE);
        CORRUPTION_BREAKPOINT_CALAMITY_WOMB = builder
                .comment(
                        " Corruption value at which Calamities and Wombs may be created/spawned, and newly",
                        " spawned Infected gear gets at least 1 enchantment per piece. Default 250."
                )
                .defineInRange("breakpointCalamityWomb", 250, 0, Integer.MAX_VALUE);
        CORRUPTION_BREAKPOINT_CALAMITY_RAIDS = builder
                .comment(
                        " Corruption value at which Proto raids may include Calamities. Roughly double",
                        " breakpointCalamityWomb by default, but independently configurable. Default 500."
                )
                .defineInRange("breakpointCalamityRaids", 500, 0, Integer.MAX_VALUE);
        CORRUPTION_BREAKPOINT_ADAPTATIONS = builder
                .comment(" Corruption value at which spawned Calamities activate their Adaptation. Default 650.")
                .defineInRange("breakpointAdaptations", 650, 0, Integer.MAX_VALUE);
        CORRUPTION_BREAKPOINT_LINKED_SPAWNS = builder
                .comment(" Corruption value at which all applicable spore mobs spawn as Linked. Default 850.")
                .defineInRange("breakpointLinkedSpawns", 850, 0, Integer.MAX_VALUE);
        builder.pop();

        builder.push("chunkloading");
        CHUNKLOAD_RECHECK_INTERVAL_TICKS = builder
                .comment(" How often (in ticks) tracked owners are rechecked for chunkload growth. Default 200 (10s).")
                .defineInRange("chunkloadRecheckIntervalTicks", 200, 20, Integer.MAX_VALUE);
        CHUNKLOAD_ENTITY_OWNERS = builder
                .comment(
                        " Entities that dynamically chunkload around themselves as they age, growing from a starting",
                        " radius up to a max radius (in chunks, radius=1 is a 3x3 area) over ticksToMaxRadius ticks.",
                        " tickingRadius is the inner radius that stays actively simulating; chunks beyond it out to",
                        " the current grown radius are loaded but not ticked, to avoid full mob-AI/block-tick cost",
                        " across an organoid's whole territory. All radii are clamped to 1-10.",
                        " Format: \"entityId|minRadius|maxRadius|tickingRadius|ticksToMaxRadius\"."
                )
                .defineListAllowEmpty(
                        "chunkloadEntityOwners",
                        () -> Lists.newArrayList(
                                "spore:mound|1|3|1|54000",
                                "spore:proto|3|7|2|144000"
                        ),
                        () -> "modid:entity_id|minRadius|maxRadius|tickingRadius|ticksToMaxRadius",
                        o -> o instanceof String
                );
        CHUNKLOAD_BLOCK_OWNERS = builder
                .comment(
                        " Same format as chunkloadEntityOwners, keyed by block id instead. Reserved for future",
                        " structure blocks (e.g. an \"outpost watcher\") that register themselves as chunkload owners;",
                        " empty by default since no such block exists yet."
                )
                .defineListAllowEmpty(
                        "chunkloadBlockOwners", Lists::newArrayList,
                        () -> "modid:block_id|minRadius|maxRadius|tickingRadius|ticksToMaxRadius", o -> o instanceof String
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
