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
    public static final BooleanValue PROTO_RAID_DIRECTED_TRAVEL;
    public static final IntValue PROTO_RAID_TRAVEL_MAX_DURATION_TICKS;
    public static final IntValue PROTO_RAID_TRAVEL_SWEEP_INTERVAL_TICKS;
    public static final IntValue PROTO_AGE_UP_INTERVAL_TICKS;
    public static final IntValue PROTO_MAX_AGE;

    public static final BooleanValue ENABLE_PROTO_WOMB_SIGNAL_GATE;
    public static final IntValue PROTO_WOMB_SIGNAL_DENSITY_RADIUS;
    public static final IntValue PROTO_WOMB_SIGNAL_MAX_NEARBY;
    public static final IntValue PROTO_WOMB_SIGNAL_COOLDOWN_TICKS;

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

    public static final BooleanValue CALAMITY_CAP_ENABLED;
    public static final IntValue CALAMITY_CAP;

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
                .comment(" No spore structures will spawn within this many chunks of world spawn.",
                        " Set to 0 to disable. Default 3.")
                .defineInRange("protectedSpawnRadiusChunks", 3, 0, 64);
        builder.pop();

        builder.push("director");
        DIRECTOR_TICK_INTERVAL_TICKS = builder
                .comment(" How often (in ticks) spore spawns are attempted. Default 100 (5s).")
                .defineInRange("directorTickIntervalTicks", 100, 20, Integer.MAX_VALUE);
        DIRECTOR_BUDGET_PER_CYCLE = builder
                .comment(" Max number of organoids that can spawn something per cycle, closest to a player first.",
                        " Default 3.")
                .defineInRange("directorBudgetPerCycle", 3, 1, Integer.MAX_VALUE);
        builder.pop();

        builder.push("mound");
        MOUND_DEFENDER_COOLDOWN_TICKS = builder
                .comment(" Cooldown between defender spawns for a single Mound. Default 600 (30s).")
                .defineInRange("moundDefenderCooldownTicks", 600, 20, Integer.MAX_VALUE);
        MOUND_DEFENDER_SEARCH_RADIUS = builder
                .comment(" Radius (blocks) around a Mound used to count and place defenders. Default 32.")
                .defineInRange("moundDefenderSearchRadius", 32, 0, Integer.MAX_VALUE);
        MOUND_MAX_DEFENDERS_NEARBY = builder
                .comment(" Max defenders a Mound allows within its search radius before it stops spawning more.",
                        " Default 12.")
                .defineInRange("moundMaxDefendersNearby", 12, 0, Integer.MAX_VALUE);
        MOUND_DEFENDER_SPAWN_POOL = builder
                .comment(" Entities a Mound may spawn as defenders.",
                        " Format: \"entityId|weight|min|max\".")
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
                .comment(" Whether Mounds are guaranteed to spawn at eligible structures. Default true.")
                .define("enabled", true);
        MOUND_GENESIS_SCAN_INTERVAL_TICKS = builder
                .comment(" How often (in ticks) eligible structures are checked for guaranteed Mound spawns.",
                        " Default 40 (2s).")
                .defineInRange("scanIntervalTicks", 40, 20, Integer.MAX_VALUE);
        MOUND_GENESIS_ELIGIBLE_STRUCTURES = builder
                .comment(" Structures that spawn a Mound the first time a player comes near them.",
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
                .comment(" Minimum number of Mounds spawned per eligible structure. Default 1.")
                .defineInRange("countMin", 1, 0, Integer.MAX_VALUE);
        MOUND_GENESIS_COUNT_MAX = builder
                .comment(" Maximum number of Mounds spawned per eligible structure. Default 1.")
                .defineInRange("countMax", 1, 0, Integer.MAX_VALUE);
        builder.pop();

        builder.pop();

        builder.push("proto");
        PROTO_RAID_COOLDOWN_MIN_TICKS = builder
                .comment(" Lower bound of the randomized cooldown between raids sent out by a Proto-Hivemind.",
                        " Default 18000 (15 min).")
                .defineInRange("protoRaidCooldownMinTicks", 18000, 20, Integer.MAX_VALUE);
        PROTO_RAID_COOLDOWN_MAX_TICKS = builder
                .comment(" Upper bound of the randomized cooldown between raids sent out by a Proto-Hivemind.",
                        " Default 72000 (1 hr). Should be >= protoRaidCooldownMinTicks.")
                .defineInRange("protoRaidCooldownMaxTicks", 72000, 20, Integer.MAX_VALUE);
        PROTO_RAID_SEARCH_RADIUS = builder
                .comment(" Radius (blocks) a Proto-Hivemind searches for raid targets - structures or players.",
                        " Default 300.")
                .defineInRange("protoRaidSearchRadius", 300, 0, Integer.MAX_VALUE);
        PROTO_RAID_GROUP_SIZE_MIN = builder
                .comment(" Baseline minimum raid group size. Scales up with World Corruption. Default 2.")
                .defineInRange("protoRaidGroupSizeMin", 2, 0, Integer.MAX_VALUE);
        PROTO_RAID_GROUP_SIZE_MAX = builder
                .comment(" Baseline maximum raid group size. Scales up with World Corruption. Default 5.")
                .defineInRange("protoRaidGroupSizeMax", 5, 0, Integer.MAX_VALUE);
        PROTO_RAID_SPAWN_POOL = builder
                .comment(" Entities a Proto-Hivemind may send on a raid. Format: \"entityId|weight|min|max\".",
                        " \"max\" caps how many of that entity a single raid can include (0 or less means unlimited).",
                        " Calamities are not included here; see protoCalamitySpawnPool."
                )
                .defineListAllowEmpty(
                        "protoRaidSpawnPool",
                        () -> Lists.newArrayList(
                                "spore:vanguard|70|1|1",
                                "spore:inf_pillager|60|1|0",
                                "spore:inf_vindicator|40|1|0",
                                "spore:inf_evoker|30|1|0",
                                "spore:inf_witch|20|1|1",
                                "spore:stalker|20|1|0",
                                "spore:brute|20|1|0",
                                "spore:volatile|20|1|0",
                                "spore:mephitic|20|1|0"
                        ),
                        () -> "modid:entity_id|weight|min|max",
                        o -> o instanceof String
                );
        PROTO_CALAMITY_SPAWN_POOL = builder
                .comment(" Calamities a Proto-Hivemind may include in a raid once World Corruption allows it.",
                        " Format: \"entityId|weight|min|max\"."
                )
                .defineListAllowEmpty(
                        "protoCalamitySpawnPool", Lists::newArrayList, () -> "modid:entity_id|weight|min|max", o -> o instanceof String
                );
        PROTO_RAID_DIRECTED_TRAVEL = builder
                .comment(" Whether raiders head straight for their raid's destination instead of idling near",
                        " their spawn point. Default true.")
                .define("protoRaidDirectedTravel", true);
        PROTO_RAID_TRAVEL_MAX_DURATION_TICKS = builder
                .comment(" Max time a traveling raider keeps forced chunkloading before it's released, even if",
                        " it never arrives. Default 18000 (15 min).")
                .defineInRange("protoRaidTravelMaxDurationTicks", 18000, 20, Integer.MAX_VALUE);
        PROTO_RAID_TRAVEL_SWEEP_INTERVAL_TICKS = builder
                .comment(" How often (in ticks) traveling raiders are checked for timeout. Default 6000 (5 min).")
                .defineInRange("protoRaidTravelSweepIntervalTicks", 6000, 1200, Integer.MAX_VALUE);
        PROTO_AGE_UP_INTERVAL_TICKS = builder
                .comment(" Ticks between Proto-Hivemind age-ups. Default 18000 (15 min), matching the pace Mounds age at.")
                .defineInRange("protoAgeUpIntervalTicks", 18000, 20, Integer.MAX_VALUE);
        PROTO_MAX_AGE = builder
                .comment(" Max number of times a single Proto-Hivemind can age up. Default 20.")
                .defineInRange("protoMaxAge", 20, 0, Integer.MAX_VALUE);
        ENABLE_PROTO_WOMB_SIGNAL_GATE = builder
                .comment(" Limits how often kills near a Proto-Hivemind can summon a Womb, so a killing spree",
                        " can't place many in a row. Default true.")
                .define("enableProtoWombSignalGate", true);
        PROTO_WOMB_SIGNAL_DENSITY_RADIUS = builder
                .comment(" Radius (blocks) around a Proto-Hivemind checked for existing Wombs before allowing a",
                        " new one. Default 48.")
                .defineInRange("protoWombSignalDensityRadius", 48, 0, Integer.MAX_VALUE);
        PROTO_WOMB_SIGNAL_MAX_NEARBY = builder
                .comment(" Max Wombs allowed within protoWombSignalDensityRadius before new ones are blocked.",
                        " Default 2.")
                .defineInRange("protoWombSignalMaxNearby", 2, 0, Integer.MAX_VALUE);
        PROTO_WOMB_SIGNAL_COOLDOWN_TICKS = builder
                .comment(" Minimum ticks between Womb summons from the same Proto-Hivemind. Set to 0 to disable.",
                        " Default 3000 (2.5 min).")
                .defineInRange("protoWombSignalCooldownTicks", 3000, 0, Integer.MAX_VALUE);
        builder.pop();

        builder.push("corruption");
        CORRUPTION_ENABLED = builder
                .comment(" Master toggle for the World Corruption system. Default true.")
                .define("enabled", true);
        CORRUPTION_CAP = builder
                .comment(" Hard ceiling for a dimension's World Corruption value. Default 1000.")
                .defineInRange("cap", 1000, 1, Integer.MAX_VALUE);
        CORRUPTION_PER_MOUND_CREATED = builder
                .comment(" Corruption added when a new Mound is created. Default 5.")
                .defineInRange("perMoundCreated", 5, 0, Integer.MAX_VALUE);
        CORRUPTION_PER_MOUND_AGE_UP = builder
                .comment(" Corruption added per Mound age-up. Default 5.")
                .defineInRange("perMoundAgeUp", 5, 0, Integer.MAX_VALUE);
        CORRUPTION_PER_PROTO_CREATED = builder
                .comment(" Corruption added when a new Proto-Hivemind is created. Default 20.")
                .defineInRange("perProtoCreated", 20, 0, Integer.MAX_VALUE);
        CORRUPTION_PER_PROTO_AGE_UP = builder
                .comment(" Corruption added per Proto-Hivemind age-up. Default 20.")
                .defineInRange("perProtoAgeUp", 20, 0, Integer.MAX_VALUE);
        CORRUPTION_AGE_SCAN_INTERVAL_TICKS = builder
                .comment(" How often (in ticks) Mounds and Proto-Hiveminds are checked for age-ups. Default 1200 (1 min).")
                .defineInRange("ageScanIntervalTicks", 1200, 20, Integer.MAX_VALUE);
        CORRUPTION_BREAKPOINT_RAIDS = builder
                .comment(" Corruption value at which Proto-Hiveminds can target raids at players. Default 100.")
                .defineInRange("breakpointRaids", 100, 0, Integer.MAX_VALUE);
        CORRUPTION_BREAKPOINT_INSTANT_EVOLUTION = builder
                .comment(" Corruption value at which newly spawned Infected may instantly evolve. Default 150.")
                .defineInRange("breakpointInstantEvolution", 150, 0, Integer.MAX_VALUE);
        CORRUPTION_BREAKPOINT_CALAMITY_WOMB = builder
                .comment(" Corruption value at which Calamities and Wombs can spawn, and new Infected gear gets",
                        " at least 1 enchantment per piece. Default 250.")
                .defineInRange("breakpointCalamityWomb", 250, 0, Integer.MAX_VALUE);
        CORRUPTION_BREAKPOINT_CALAMITY_RAIDS = builder
                .comment(" Corruption value at which Proto raids may include Calamities. Default 500.")
                .defineInRange("breakpointCalamityRaids", 500, 0, Integer.MAX_VALUE);
        CORRUPTION_BREAKPOINT_ADAPTATIONS = builder
                .comment(" Corruption value at which spawned Calamities gain their Adaptation. Default 650.")
                .defineInRange("breakpointAdaptations", 650, 0, Integer.MAX_VALUE);
        CORRUPTION_BREAKPOINT_LINKED_SPAWNS = builder
                .comment(" Corruption value at which all applicable spore mobs spawn as Linked. Default 850.")
                .defineInRange("breakpointLinkedSpawns", 850, 0, Integer.MAX_VALUE);
        builder.pop();

        builder.push("chunkloading");
        CHUNKLOAD_RECHECK_INTERVAL_TICKS = builder
                .comment(" How often (in ticks) chunkloading organoids are rechecked for growth. Default 200 (10s).")
                .defineInRange("chunkloadRecheckIntervalTicks", 200, 20, Integer.MAX_VALUE);
        CHUNKLOAD_ENTITY_OWNERS = builder
                .comment(" Entities that chunkload around themselves, growing from a starting radius to a max",
                        " radius (radius=1 is a 3x3 area) as they age. tickingRadius is the inner radius that",
                        " keeps actively simulating; chunks beyond it out to the current radius stay loaded but",
                        " idle. All radii are clamped to 1-10.",
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
                .comment(" Same format as chunkloadEntityOwners, keyed by block id instead. Empty by default.")
                .defineListAllowEmpty(
                        "chunkloadBlockOwners", Lists::newArrayList,
                        () -> "modid:block_id|minRadius|maxRadius|tickingRadius|ticksToMaxRadius", o -> o instanceof String
                );
        builder.pop();

        builder.push("calamity");
        CALAMITY_CAP_ENABLED = builder
                .comment(" Master toggle for the global Calamity cap. Default true.")
                .define("calamityCapEnabled", true);
        CALAMITY_CAP = builder
                .comment(" Max Calamities allowed to exist at once, across every dimension. New Calamities beyond",
                        " this cull whichever existing one is furthest from any player. Default 6.")
                .defineInRange("calamityCap", 6, 0, Integer.MAX_VALUE);
        builder.pop();

        builder.push("structures");
        STRUCTURE_ANCHOR_SEARCH_RADIUS = builder
                .comment(" Radius (blocks) searched for a nearby structure to anchor to before falling back to",
                        " the organoid's own position. Default 8.")
                .defineInRange("structureAnchorSearchRadius", 8, 0, Integer.MAX_VALUE);
        builder.pop();

        SPEC = builder.build();
    }

    private SporeBreachServerConfig() {
    }
}
