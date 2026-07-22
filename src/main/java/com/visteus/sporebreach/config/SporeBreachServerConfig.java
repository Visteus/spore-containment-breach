package com.visteus.sporebreach.config;

import com.google.common.collect.Lists;
import com.visteus.sporebreach.chunkloading.ChunkCircleOffsets;
import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public final class SporeBreachServerConfig {

    /**
     * Which system builds structures around Mounds/Proto-Hiveminds as they grow: base Spore's own
     * casing/seed-block growth, or this mod's staged NBT structure growth. Only one runs at a time.
     */
    public enum StructureGrowthMode {
        BASE_SPORE_SHELLS,
        SPORE_BREACH_TOWERS
    }

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
    public static final DoubleValue PROTO_RAID_GROUP_SIZE_MAX_MULTIPLIER;
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

    public static final BooleanValue CORRUPTION_MOB_SCALING_ENABLED;
    public static final DoubleValue CORRUPTION_MOB_HEALTH_MAX_MULTIPLIER;
    public static final DoubleValue CORRUPTION_MOB_DAMAGE_MAX_MULTIPLIER;
    public static final DoubleValue CORRUPTION_MOB_ARMOR_MAX_BONUS;
    public static final DoubleValue CORRUPTION_MOB_ARMOR_TOUGHNESS_MAX_BONUS;

    public static final IntValue CHUNKLOAD_RECHECK_INTERVAL_TICKS;
    public static final ConfigValue<List<? extends String>> CHUNKLOAD_ENTITY_OWNERS;
    public static final ConfigValue<List<? extends String>> CHUNKLOAD_BLOCK_OWNERS;

    public static final BooleanValue CALAMITY_CAP_ENABLED;
    public static final IntValue CALAMITY_CAP;

    public static final IntValue STRUCTURE_ANCHOR_SEARCH_RADIUS;

    public static final BooleanValue BIOME_PAINT_ENABLED;
    public static final IntValue BIOME_PAINT_EXTRA_RADIUS_CHUNKS;
    public static final IntValue BIOME_PAINT_COLUMNS_PER_PASS;
    public static final IntValue BIOME_PAINT_RECHECK_INTERVAL_TICKS;
    public static final IntValue BIOME_PAINT_SCAR_DELAY_TICKS;
    public static final BooleanValue AREA_WATER_REPLACEMENT_ENABLED;
    public static final IntValue AREA_WATER_REPLACEMENT_DEPTH;
    public static final IntValue AREA_WATER_REPLACEMENT_BLOCKS_PER_PASS;
    public static final IntValue AREA_WATER_RESEED_INTERVAL_TICKS;
    public static final DoubleValue AREA_WATER_RESEED_CHANCE;

    public static final BooleanValue MOUND_GENESIS_ENABLED;
    public static final IntValue MOUND_GENESIS_SCAN_INTERVAL_TICKS;
    public static final ConfigValue<List<? extends String>> MOUND_GENESIS_ELIGIBLE_STRUCTURES;
    public static final IntValue MOUND_GENESIS_COUNT_MIN;
    public static final IntValue MOUND_GENESIS_COUNT_MAX;

    public static final BooleanValue ENABLE_SCAMPER_MOUND_DENSITY_GATE;
    public static final IntValue SCAMPER_MOUND_DENSITY_RADIUS;
    public static final IntValue SCAMPER_MOUND_MAX_NEARBY;
    public static final IntValue SCAMPER_MOUND_SUMMON_MIN;
    public static final IntValue SCAMPER_MOUND_SUMMON_MAX;
    public static final IntValue SCAMPER_MOUND_SUMMON_RANGE;

    public static final EnumValue<StructureGrowthMode> STRUCTURE_GROWTH_MODE;
    public static final DoubleValue STRUCTURE_UNDERGROUND_MIN_NATURAL_GROUND_COVERAGE;

    public static final IntValue MOUND_STRUCTURE_RECHECK_INTERVAL_TICKS;
    public static final IntValue MOUND_STRUCTURE_PASS_INTERVAL_TICKS;
    public static final IntValue MOUND_STRUCTURE_MIN_AGE;
    public static final IntValue MOUND_STRUCTURE_MAX_PER_MOUND;
    public static final DoubleValue MOUND_STRUCTURE_PLACEMENT_CHANCE;
    public static final IntValue MOUND_STRUCTURE_MIN_DISTANCE;
    public static final IntValue MOUND_STRUCTURE_BLOCKS_PER_PASS;
    public static final ConfigValue<List<? extends String>> MOUND_STRUCTURE_POOL;
    public static final DoubleValue MOUND_STRUCTURE_UNDERGROUND_CHANCE;
    public static final ConfigValue<List<? extends String>> MOUND_STRUCTURE_UNDERGROUND_POOL;

    public static final IntValue PROTO_STRUCTURE_RECHECK_INTERVAL_TICKS;
    public static final IntValue PROTO_STRUCTURE_PASS_INTERVAL_TICKS;
    public static final IntValue PROTO_STRUCTURE_MIN_AGE;
    public static final IntValue PROTO_STRUCTURE_MAX_PER_PROTO;
    public static final DoubleValue PROTO_STRUCTURE_PLACEMENT_CHANCE;
    public static final IntValue PROTO_STRUCTURE_MIN_DISTANCE;
    public static final IntValue PROTO_STRUCTURE_BIOMASS_COST_PER_PASS;
    public static final IntValue PROTO_STRUCTURE_BLOCKS_PER_PASS;
    public static final ConfigValue<List<? extends String>> PROTO_STRUCTURE_POOL;
    public static final DoubleValue PROTO_STRUCTURE_UNDERGROUND_CHANCE;
    public static final ConfigValue<List<? extends String>> PROTO_STRUCTURE_UNDERGROUND_POOL;

    public static final BooleanValue PROTO_OUTPOST_SEED_ENABLED;
    public static final IntValue PROTO_OUTPOST_SEED_MIN_AGE;
    public static final IntValue PROTO_OUTPOST_SEED_COOLDOWN_TICKS;
    public static final IntValue PROTO_OUTPOST_SEED_MIN_DISTANCE;
    public static final IntValue PROTO_OUTPOST_SEED_MAX_DISTANCE;
    public static final IntValue PROTO_OUTPOST_SEED_MOUND_CHECK_RADIUS;
    public static final IntValue PROTO_OUTPOST_SEED_MOUND_LIMIT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("systems");

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
                        CORRUPTION_MOB_SCALING_ENABLED = builder
                                .comment(" Toggle for corruption-scaled spore mob health/damage/armor. Default true.")
                                .define("mobScalingEnabled", true);
                        CORRUPTION_MOB_HEALTH_MAX_MULTIPLIER = builder
                                .comment(" Health multiplier applied to spore mobs at maximum World Corruption, scaling",
                                        " linearly from 1.0x at zero corruption. Set to 1.0 to disable. Default 3.0.")
                                .defineInRange("mobHealthMaxMultiplier", 3.0, 1.0, 1000.0);
                        CORRUPTION_MOB_DAMAGE_MAX_MULTIPLIER = builder
                                .comment(" Attack damage multiplier applied to spore mobs at maximum World Corruption, scaling",
                                        " linearly from 1.0x at zero corruption. Set to 1.0 to disable. Default 2.0.")
                                .defineInRange("mobDamageMaxMultiplier", 2.0, 1.0, 1000.0);
                        CORRUPTION_MOB_ARMOR_MAX_BONUS = builder
                                .comment(" Flat armor points added to spore mobs at maximum World Corruption, scaling linearly",
                                        " from 0 at zero corruption. Set to 0 to disable. Default 10.0.")
                                .defineInRange("mobArmorMaxBonus", 10.0, 0.0, 100.0);
                        CORRUPTION_MOB_ARMOR_TOUGHNESS_MAX_BONUS = builder
                                .comment(" Flat armor toughness added to spore mobs at maximum World Corruption, scaling linearly",
                                        " from 0 at zero corruption. Set to 0 to disable. Default 4.0.")
                                .defineInRange("mobArmorToughnessMaxBonus", 4.0, 0.0, 100.0);
                builder.pop();

                builder.push("chunkloading");
                        CHUNKLOAD_RECHECK_INTERVAL_TICKS = builder
                                .comment(" How often (in ticks) chunkloading organoids are rechecked for growth. Default 200 (10s).")
                                .defineInRange("chunkloadRecheckIntervalTicks", 200, 20, Integer.MAX_VALUE);
                        CHUNKLOAD_ENTITY_OWNERS = builder
                                .comment(" Entities that chunkload around themselves, growing from a starting radius to a max",
                                        " radius (radius=1 is a 3x3 area) as they age. 'tickingRadius' is the inner radius that",
                                        " keeps actively simulating; chunks beyond it out to the current radius stay loaded but idle. ",
                                        " Maximum radius of 10 chunks.",
                                        " Format: \"entityId|minRadius|maxRadius|tickingRadius|ticksToMaxRadius\"."
                                )
                                .defineListAllowEmpty(
                                        "chunkloadEntityOwners",
                                        () -> Lists.newArrayList(
                                                "spore:mound|1|3|1|54000",
                                                "spore:proto|3|9|2|144000"
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

                builder.push("structures");
                        STRUCTURE_ANCHOR_SEARCH_RADIUS = builder
                                .comment(" Radius (blocks) searched for a nearby structure to anchor to before falling back to",
                                        " the organoid's own position. Default 8.")
                                .defineInRange("structureAnchorSearchRadius", 8, 0, Integer.MAX_VALUE);
                builder.pop();

                builder.push("biome");
                        BIOME_PAINT_ENABLED = builder
                                .comment(" Whether Mounds/Proto-Hiveminds repaint the area around themselves into this mod's",
                                        " corruption biome as they chunkload-grow. Default true.")
                                .define("biomePaintEnabled", true);
                        BIOME_PAINT_EXTRA_RADIUS_CHUNKS = builder
                                .comment(" How many chunks past an organoid's own chunkload radius the biome ring extends.",
                                        " Default 2.")
                                .defineInRange("biomePaintExtraRadiusChunks", 2, 0, ChunkCircleOffsets.MAX_RADIUS);
                        BIOME_PAINT_COLUMNS_PER_PASS = builder
                                .comment(" Max chunk columns (re)painted per recheck. Default 4.")
                                .defineInRange("biomePaintColumnsPerPass", 4, 1, Integer.MAX_VALUE);
                        BIOME_PAINT_RECHECK_INTERVAL_TICKS = builder
                                .comment(" How often (in ticks) biome-paint growth and its paint queue are advanced. Default",
                                        " 200 (10s).")
                                .defineInRange("biomePaintRecheckIntervalTicks", 200, 20, Integer.MAX_VALUE);
                        BIOME_PAINT_SCAR_DELAY_TICKS = builder
                                .comment(" Ticks an unclaimed column lingers as active corruption biome before downgrading to",
                                        " the deader scar biome. Default 12000 (10 min).")
                                .defineInRange("biomePaintScarDelayTicks", 12000, 0, Integer.MAX_VALUE);
                        AREA_WATER_REPLACEMENT_ENABLED = builder
                                .comment(" Whether surface water under newly painted corruption biome gradually crusts over",
                                        " with bile. Default true.")
                                .define("areaWaterReplacementEnabled", true);
                        AREA_WATER_REPLACEMENT_DEPTH = builder
                                .comment(" Max depth (blocks) below the surface that water is converted to crusted bile.",
                                        " Default 3.")
                                .defineInRange("areaWaterReplacementDepth", 3, 1, 32);
                        AREA_WATER_REPLACEMENT_BLOCKS_PER_PASS = builder
                                .comment(" Max water blocks converted to crusted bile per recheck.",
                                        " Default 16.")
                                .defineInRange("areaWaterReplacementBlocksPerPass", 16, 1, Integer.MAX_VALUE);
                        AREA_WATER_RESEED_INTERVAL_TICKS = builder
                                .comment(" How often (in ticks) each organoid rolls a chance to seed a fresh water source",
                                        " somewhere within its biome radius, catching pools not connected to its main spread.",
                                        " Default 1200 (1 min).")
                                .defineInRange("areaWaterReseedIntervalTicks", 1200, 20, Integer.MAX_VALUE);
                        AREA_WATER_RESEED_CHANCE = builder
                                .comment(" Chance, checked every areaWaterReseedIntervalTicks per organoid, of rolling a new",
                                        " reseed. Default 0.1.")
                                .defineInRange("areaWaterReseedChance", 0.1, 0.0, 1.0);
                builder.pop();
                builder.push("structures");
                        STRUCTURE_GROWTH_MODE = builder
                                .comment(" Which system builds structures around Mounds/Proto-Hiveminds as they grow:",
                                        " BASE_SPORE_SHELLS keeps base Spore's own casing/seed-block growth untouched;",
                                        " SPORE_BREACH_TOWERS replaces it with this mod's staged NBT structure growth.",
                                        " Only one system runs at a time. Default SPORE_BREACH_TOWERS.")
                                .defineEnum("structureGrowthMode", StructureGrowthMode.SPORE_BREACH_TOWERS);
                        STRUCTURE_UNDERGROUND_MIN_NATURAL_GROUND_COVERAGE = builder
                                .comment(" Minimum fraction of an underground structure's blocks that must currently sit in",
                                        " natural terrain for it to be allowed to grow. Default 0.25.")
                                .defineInRange("undergroundNaturalGroundCoverage", 0.25, 0.0, 1.0);
                builder.pop();

        builder.pop();

        builder.push("mobs");

                builder.push("mound");
                        MOUND_DEFENDER_COOLDOWN_TICKS = builder
                                .comment(" Cooldown between defender spawns for a single Mound. Default 600 (30s).")
                                .defineInRange("moundDefenderCooldownTicks", 600, 20, Integer.MAX_VALUE);
                        MOUND_DEFENDER_SEARCH_RADIUS = builder
                                .comment(" Radius (blocks) around a Mound used to count and place defenders. Default 16.")
                                .defineInRange("moundDefenderSearchRadius", 16, 0, Integer.MAX_VALUE);
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
                                                        "spore:mass_grave",
                                                        "sporebreach:end_lab",
                                                        "sporebreach:nether_lab",
                                                        "sporebreach:warped_lab"
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

                        builder.push("structures");
                                MOUND_STRUCTURE_RECHECK_INTERVAL_TICKS = builder
                                        .comment(" How often (in ticks) a Mound is checked for starting a new structure growth job.",
                                                " Default 1200 (1 min).")
                                        .defineInRange("recheckIntervalTicks", 1200, 20, Integer.MAX_VALUE);
                                MOUND_STRUCTURE_PASS_INTERVAL_TICKS = builder
                                        .comment(" How often (in ticks) an in-progress structure growth job is advanced by one",
                                                " building pass. Default 100 (5s).")
                                        .defineInRange("passIntervalTicks", 100, 20, Integer.MAX_VALUE);
                                MOUND_STRUCTURE_MIN_AGE = builder
                                        .comment(" Minimum Mound age (0-4) before it can start growing structures. Default 2.")
                                        .defineInRange("minAge", 2, 0, 4);
                                MOUND_STRUCTURE_MAX_PER_MOUND = builder
                                        .comment(" Max structures a single Mound will grow around itself. Default 3.")
                                        .defineInRange("maxPerMound", 3, 0, Integer.MAX_VALUE);
                                MOUND_STRUCTURE_PLACEMENT_CHANCE = builder
                                        .comment(" Chance, checked every recheckIntervalTicks, that a Mound starts growing its next",
                                                " structure. Default 0.1.")
                                        .defineInRange("placementChance", 0.1, 0.0, 1.0);
                                MOUND_STRUCTURE_MIN_DISTANCE = builder
                                        .comment(" Minimum distance (blocks) between a Mound's 2nd+ grown structure and its earlier",
                                                " ones. The first structure is always centered on the Mound itself. Default 12.")
                                        .defineInRange("minDistance", 12, 0, Integer.MAX_VALUE);
                                MOUND_STRUCTURE_BLOCKS_PER_PASS = builder
                                        .comment(" Max blocks placed per building pass. Default 20.")
                                        .defineInRange("blocksPerPass", 20, 1, Integer.MAX_VALUE);
                                MOUND_STRUCTURE_POOL = builder
                                        .comment(" Structures a Mound may grow around itself. Format: \"structureId|weight\".")
                                        .defineListAllowEmpty(
                                                "structurePool",
                                                () -> Lists.newArrayList(
                                                        "sporebreach:mound_watchtower|30",
                                                        "sporebreach:mound_fort_bulb|35",
                                                        "sporebreach:mound_fort_cluster|35"
                                                ),
                                                () -> "modid:structure_id|weight",
                                                o -> o instanceof String
                                        );
                                MOUND_STRUCTURE_UNDERGROUND_CHANCE = builder
                                        .comment(" Chance an underground structure grows beneath a completed surface structure.",
                                                " Default 0.5.")
                                        .defineInRange("undergroundChance", 0.5, 0.0, 1.0);
                                MOUND_STRUCTURE_UNDERGROUND_POOL = builder
                                        .comment(" Underground structures a Mound may grow beneath a surface structure. Skipped if it",
                                                " wouldn't clear undergroundNaturalGroundCoverage (see [mobs.structures]).",
                                                " Format: \"structureId|weight\".")
                                        .defineListAllowEmpty(
                                                "undergroundPool",
                                                () -> Lists.newArrayList(
                                                        "sporebreach:underground_pillar_small|30",
                                                        "sporebreach:underground_pillar_large|25",
                                                        "sporebreach:underground_chambers|25",
                                                        "sporebreach:underground_column|20"
                                                ),
                                                () -> "modid:structure_id|weight",
                                                o -> o instanceof String
                                        );
                        builder.pop();
                builder.pop();

                builder.push("scamper");
                        ENABLE_SCAMPER_MOUND_DENSITY_GATE = builder
                                .comment(" Limits how many Mounds a Scamper can spawn into an already-crowded area when it ages",
                                        " up into one. Default true.")
                                .define("enableScamperMoundDensityGate", true);
                        SCAMPER_MOUND_DENSITY_RADIUS = builder
                                .comment(" Radius (blocks) around a Scamper's age-up point checked for existing Mounds before",
                                        " allowing it to spawn new ones. Default 16.")
                                .defineInRange("scamperMoundDensityRadius", 16, 0, Integer.MAX_VALUE);
                        SCAMPER_MOUND_MAX_NEARBY = builder
                                .comment(" Max Mounds allowed within scamperMoundDensityRadius before a Scamper's age-up spawns",
                                        " no new Mounds at all. Default 1.")
                                .defineInRange("scamperMoundMaxNearby", 1, 0, Integer.MAX_VALUE);
                        SCAMPER_MOUND_SUMMON_MIN = builder
                                .comment(" Minimum Mounds placed by a Scamper age-up that passes the density check. Default 1.")
                                .defineInRange("scamperMoundSummonMin", 1, 1, Integer.MAX_VALUE);
                        SCAMPER_MOUND_SUMMON_MAX = builder
                                .comment(" Maximum Mounds placed by a Scamper age-up that passes the density check. Should be >=",
                                        " scamperMoundSummonMin. Default 1.")
                                .defineInRange("scamperMoundSummonMax", 1, 1, Integer.MAX_VALUE);
                        SCAMPER_MOUND_SUMMON_RANGE = builder
                                .comment(" Horizontal radius (blocks) within which a Scamper's age-up places its new Mounds.",
                                        " Default 6.")
                                .defineInRange("scamperMoundSummonRange", 6, 0, Integer.MAX_VALUE);
                builder.pop();

                builder.push("proto");
                        PROTO_MAX_AGE = builder
                                .comment(" Maximum 'age counter' for Proto-Hiveminds. Default 20.")
                                .defineInRange("protoMaxAge", 4, 0, Integer.MAX_VALUE);
                        PROTO_AGE_UP_INTERVAL_TICKS = builder
                                .comment(" Ticks between Proto-Hivemind age-ups. Default 18000 (15 min)")
                                .defineInRange("protoAgeUpIntervalTicks", 18000, 20, Integer.MAX_VALUE);
                        
                        builder.push("raids");
                                PROTO_RAID_COOLDOWN_MIN_TICKS = builder
                                        .comment(" Lower bound of the randomized cooldown between raids sent out by a Proto-Hivemind.",
                                                " Default 12000 (10 min).")
                                        .defineInRange("protoRaidCooldownMinTicks", 12000, 20, Integer.MAX_VALUE);
                                PROTO_RAID_COOLDOWN_MAX_TICKS = builder
                                        .comment(" Upper bound of the randomized cooldown between raids sent out by a Proto-Hivemind.",
                                                " Default 54000 (45 min). Should be >= protoRaidCooldownMinTicks.")
                                        .defineInRange("protoRaidCooldownMaxTicks", 54000, 20, Integer.MAX_VALUE);
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
                                PROTO_RAID_GROUP_SIZE_MAX_MULTIPLIER = builder
                                        .comment(" How much protoRaidGroupSizeMin/Max are multiplied by at maximum World Corruption,",
                                                " scaling linearly from 1.0 at zero corruption. Set to 1.0 to disable scaling.",
                                                " Default 10.0.")
                                        .defineInRange("protoRaidGroupSizeMaxMultiplier", 10.0, 1.0, 1000.0);
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
                                                        "spore:inf_evoker|30|1|1",
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
                                                " Defaults include land and air calamities.",
                                                " Format: \"entityId|weight|min|max\"."
                                        )
                                        .defineListAllowEmpty(
                                                "protoCalamitySpawnPool",
                                                () -> Lists.newArrayList(
                                                        "spore:stahl|100|1|1",
                                                        "spore:sieger|100|1|1",
                                                        "spore:howitzer|80|1|1",
                                                        "spore:hohlfresser|25|1|1",
                                                        "spore:hindenburg|15|1|1",
                                                        "spore:verfall|15|1|1"
                                                ),
                                                () -> "modid:entity_id|weight|min|max",
                                                o -> o instanceof String
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
                                        .comment(" How often (in ticks) traveling raiders are checked for timeout. Default 3000 (2.5 min).")
                                        .defineInRange("protoRaidTravelSweepIntervalTicks", 3000, 20, Integer.MAX_VALUE);
                                builder.pop();

                        builder.push("wombs");
                                builder.comment(" Proto-Hiveminds can summon Wombs when a player is nearby and they are attacked or",
                                        " killed. This section controls how often that can happen, and how many Wombs are allowed in the area at once.");
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

                        builder.push("structures");
                                PROTO_STRUCTURE_RECHECK_INTERVAL_TICKS = builder
                                        .comment(" How often (in ticks) a Proto-Hivemind is checked for starting a new structure",
                                                " growth job. Default 6000 (5 min).")
                                        .defineInRange("recheckIntervalTicks", 6000, 20, Integer.MAX_VALUE);
                                PROTO_STRUCTURE_PASS_INTERVAL_TICKS = builder
                                        .comment(" How often (in ticks) an in-progress structure growth job is advanced by one",
                                                " building pass. Default 40 (2s).")
                                        .defineInRange("passIntervalTicks", 40, 20, Integer.MAX_VALUE);
                                PROTO_STRUCTURE_MIN_AGE = builder
                                        .comment(" Minimum Proto-Hivemind age before it can start growing structures. Default 1.")
                                        .defineInRange("minAge", 1, 0, Integer.MAX_VALUE);
                                PROTO_STRUCTURE_MAX_PER_PROTO = builder
                                        .comment(" Max structures a single Proto-Hivemind will grow around itself. Default 5.")
                                        .defineInRange("maxPerProto", 5, 1, Integer.MAX_VALUE);
                                PROTO_STRUCTURE_PLACEMENT_CHANCE = builder
                                        .comment(" Chance, checked every recheckIntervalTicks, that a Proto-Hivemind starts growing",
                                                " its next structure. Default 0.1.")
                                        .defineInRange("placementChance", 0.1, 0.0, 1.0);
                                PROTO_STRUCTURE_MIN_DISTANCE = builder
                                        .comment(" Minimum distance (blocks) between a Proto-Hivemind's 2nd+ grown structure and its",
                                                " earlier ones. The first structure is always centered on the Proto-Hivemind itself.",
                                                " Default 64.")
                                        .defineInRange("minDistance", 64, 0, Integer.MAX_VALUE);
                                PROTO_STRUCTURE_BIOMASS_COST_PER_PASS = builder
                                        .comment(" Biomass spent per building pass, the same resource base Spore's own Proto-Hivemind",
                                                " shell growth spends. A pass is skipped if biomass is too low. Default 8.")
                                        .defineInRange("biomassCostPerPass", 8, 0, Integer.MAX_VALUE);
                                PROTO_STRUCTURE_BLOCKS_PER_PASS = builder
                                        .comment(" Max blocks placed per building pass. Default 80.")
                                        .defineInRange("blocksPerPass", 80, 1, Integer.MAX_VALUE);
                                PROTO_STRUCTURE_POOL = builder
                                        .comment(" Structures a Proto-Hivemind may grow around itself. Format: \"structureId|weight\".")
                                        .defineListAllowEmpty(
                                                "structurePool",
                                                () -> Lists.newArrayList(
                                                        "sporebreach:proto_spire_hollow|40",
                                                        "sporebreach:proto_spire_hollow_alt|40",
                                                        "sporebreach:proto_spire_apex|20",
                                                        "sporebreach:proto_spire_needle|20",
                                                        "sporebreach:proto_spire_shelf|20"
                                                ),
                                                () -> "modid:structure_id|weight",
                                                o -> o instanceof String
                                        );
                                PROTO_STRUCTURE_UNDERGROUND_CHANCE = builder
                                        .comment(" Chance an underground structure grows beneath a completed surface structure.",
                                                " Always guaranteed for a Proto-Hivemind's first structure regardless of this chance.",
                                                " Default 0.5.")
                                        .defineInRange("undergroundChance", 0.5, 0.0, 1.0);
                                PROTO_STRUCTURE_UNDERGROUND_POOL = builder
                                        .comment(" Underground structures a Proto-Hivemind may grow beneath a surface structure. Skipped",
                                                " if it wouldn't clear undergroundNaturalGroundCoverage (see [mobs.structures]).",
                                                " Format: \"structureId|weight\".")
                                        .defineListAllowEmpty(
                                                "undergroundPool",
                                                () -> Lists.newArrayList(
                                                        // "sporebreach:underground_pillar_small|30",
                                                        "sporebreach:underground_pillar_large|25",
                                                        // "sporebreach:underground_chambers|25",
                                                        "sporebreach:underground_column|20"
                                                ),
                                                () -> "modid:structure_id|weight",
                                                o -> o instanceof String
                                        );
                        builder.pop();

                        builder.push("outposts");
                                PROTO_OUTPOST_SEED_ENABLED = builder
                                        .comment(" Whether Proto-Hiveminds seed new Mound outposts at a distance from themselves",
                                                " to extend the infected area. Default true.")
                                        .define("enabled", true);
                                PROTO_OUTPOST_SEED_MIN_AGE = builder
                                        .comment(" Minimum Proto-Hivemind age before it starts rolling to seed outposts. Default 2.")
                                        .defineInRange("minAge", 2, 0, Integer.MAX_VALUE);
                                PROTO_OUTPOST_SEED_COOLDOWN_TICKS = builder
                                        .comment(" Minimum ticks between outpost-seeding rolls from the same Proto-Hivemind.",
                                                " Default 12000 (10 min).")
                                        .defineInRange("cooldownTicks", 12000, 20, Integer.MAX_VALUE);
                                PROTO_OUTPOST_SEED_MIN_DISTANCE = builder
                                        .comment(" Minimum distance (blocks) from a Proto-Hivemind that a new outpost may be seeded.",
                                                " Default 96.")
                                        .defineInRange("minDistance", 96, 0, Integer.MAX_VALUE);
                                PROTO_OUTPOST_SEED_MAX_DISTANCE = builder
                                        .comment(" Maximum distance (blocks) from a Proto-Hivemind that a new outpost may be seeded.",
                                                " Should be >= minDistance. Default 128.")
                                        .defineInRange("maxDistance", 128, 0, Integer.MAX_VALUE);
                                PROTO_OUTPOST_SEED_MOUND_CHECK_RADIUS = builder
                                        .comment(" Radius (blocks) around a candidate outpost site checked for existing Mounds",
                                                " before seeding a new one. Default 32.")
                                        .defineInRange("moundCheckRadius", 32, 0, Integer.MAX_VALUE);
                                PROTO_OUTPOST_SEED_MOUND_LIMIT = builder
                                        .comment(" Max Mounds allowed within moundCheckRadius of a candidate site before seeding",
                                                " there is skipped for this roll. Default 1.")
                                        .defineInRange("moundLimit", 1, 0, Integer.MAX_VALUE);
                        builder.pop();
                builder.pop();

                builder.push("calamity");
                        CALAMITY_CAP_ENABLED = builder
                                .comment(" Master toggle for the global Calamity cap. Default true.")
                                .define("calamityCapEnabled", true);
                        CALAMITY_CAP = builder
                                .comment(" Max Calamities allowed to exist at once, across every dimension. New Calamities beyond",
                                        " this cull whichever existing one is furthest from any player. Default 8.")
                                .defineInRange("calamityCap", 8, 0, Integer.MAX_VALUE);
                builder.pop();

        builder.pop();

        SPEC = builder.build();
    }

    private SporeBreachServerConfig() {
    }
}
