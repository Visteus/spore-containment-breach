package com.visteus.sporebreach.config;

import com.google.common.collect.Lists;
import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

/**
 * Values pushed into base Spore's own config ({@code com.Harbinger.Spore.core.SConfig}) at mod
 * load, replacing a handful of its defaults with this modpack's own. See {@link
 * com.visteus.sporebreach.setup.SporeConfigOverrides} for where these are applied.
 */
public final class SporeBreachOverridesConfig {

    public static final ModConfigSpec SPEC;

    public static final BooleanValue APPLY_SPORE_CONFIG_OVERRIDES;

    public static final ConfigValue<List<? extends String>> INFECTED_BLOCK_COUNTERPARTS;
    public static final ConfigValue<List<? extends String>> INFECTED_PLAYER_NAMES;

    public static final IntValue MAX_REGULAR_INFECTED;
    public static final IntValue MAX_EVOLVED_INFECTED;
    public static final IntValue MAX_HYPER_INFECTED;
    public static final IntValue MAX_ORGANOIDS;
    public static final ConfigValue<List<? extends String>> DESPAWN_BLACKLIST;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("sporeOverrides");
                APPLY_SPORE_CONFIG_OVERRIDES = builder
                        .comment(" Master toggle for overriding base Spore's own config values below with this",
                                " mod's own defaults, applied once at mod load. Default true.")
                        .define("applySporeConfigOverrides", true);

                builder.push("blockInfection");
                        INFECTED_BLOCK_COUNTERPARTS = builder
                                .comment(" Replaces Spore's own \"Blocks and their infected counterparts\" list.",
                                        " Format: \"sourceBlock|infectedBlock\".")
                                .defineListAllowEmpty(
                                        "infectedBlockCounterparts",
                                        () -> Lists.newArrayList(
                                                "minecraft:stone|spore:infested_stone",
                                                "minecraft:grass_block|minecraft:mycelium",
                                                "minecraft:dirt|spore:infested_dirt",
                                                "minecraft:coarse_dirt|spore:infested_dirt",
                                                "minecraft:podzol|spore:mycelium_block",
                                                "minecraft:rooted_dirt|spore:rooted_mycelium",
                                                "minecraft:deepslate|spore:infested_deepslate",
                                                "minecraft:sand|spore:infested_sand",
                                                "minecraft:gravel|spore:infested_gravel",
                                                "minecraft:netherrack|spore:infested_netherrack",
                                                "minecraft:end_stone|spore:infested_end_stone",
                                                "minecraft:soul_sand|spore:infested_soul_sand",
                                                "minecraft:soul_soil|spore:infested_soul_sand",
                                                "minecraft:sculk|spore:rooted_mycelium",
                                                "minecraft:red_sand|spore:infested_red_sand",
                                                "minecraft:bricks|spore:infested_bricks",
                                                "minecraft:clay|spore:infested_clay",
                                                "minecraft:cobblestone|spore:infested_cobblestone",
                                                "minecraft:cobbled_deepslate|spore:infested_cobbled_deepslate",
                                                "minecraft:stone_bricks|spore:infested_stone_bricks",
                                                "spore:lab_block|spore:infested_laboratory_block",
                                                "spore:lab_block1|spore:infested_laboratory_block1",
                                                "spore:lab_block2|spore:infested_laboratory_block2",
                                                "spore:lab_block3|spore:infested_laboratory_block3",
                                                "the_flesh_that_hates:flesh_block|spore:biomass_block",
                                                "the_flesh_that_hates:flesh_sand|spore:biomass_block",
                                                "the_flesh_that_hates:flesh_tree|spore:biomass_block",
                                                "the_flesh_that_hates:flesh_plank|spore:biomass_block",
                                                "the_flesh_that_hates:flesh_pile|spore:biomass_block",
                                                "the_flesh_that_hates:tumor|spore:biomass_block",
                                                "the_flesh_that_hates:purulent_tumor|spore:biomass_block"
                                        ),
                                        () -> "sourceBlock|infectedBlock",
                                        o -> o instanceof String
                                );
                builder.pop();

                builder.push("players");
                        INFECTED_PLAYER_NAMES = builder
                                .comment(" Replaces Spore's own \"Infected Player possible names\" list.")
                                .defineListAllowEmpty(
                                        "infectedPlayerNames",
                                        () -> Lists.newArrayList(
                                                "The_Harbinger69",
                                                "ABucketOfFriedChicken",
                                                "LoneGuy",
                                                "cheesepuff",
                                                "Sire_AwfulThe1st",
                                                "Azami",
                                                "Deyvid",
                                                "Dany_Why",
                                                "Technoblade",
                                                "Ike",
                                                "Hypnotizd",
                                                "That_Insane_Guy",
                                                "JhonOK22",
                                                "Tabcaps",
                                                "WhisperFire26",
                                                "ButtonHatBoy",
                                                "Gistique",
                                                "yile_ouo",
                                                "BigXplosion",
                                                "Atomiclbomb",
                                                "Mad_Dog",
                                                "Ripley",
                                                "gregTheTyrant",
                                                "Joker_de_Coeur",
                                                "xXFuryXx",
                                                "Nova69",
                                                "Belladonna",
                                                "Entity",
                                                "Keymind",
                                                "Whisper",
                                                "Helldwin",
                                                "ExceedingSky74",
                                                "Flash62724",
                                                "Hank_o",
                                                "JWT114",
                                                "DawnsSlayers",
                                                "Dr_Pilot_MOO",
                                                "NexouuZ",
                                                "Mr_Door12323",
                                                "PedroHenrry",
                                                "TVGuy",
                                                "ThatGardener",
                                                "TheCaramelGuy",
                                                "TokenOni420",
                                                "lightigivhi",
                                                "CODATOWER",
                                                "hammbug",
                                                "mrlambert6",
                                                "DivnejFelix",
                                                "ThatGardener",
                                                "SyrCrypt",
                                                "KaratFeng",
                                                "Toasteroni",
                                                "UnmeiHa",
                                                "AllToAshes",
                                                "0dna",
                                                "minisketchy0919",
                                                "Visteus"
                                        ),
                                        () -> "playerName",
                                        o -> o instanceof String
                                );
                builder.pop();

                builder.push("despawning");
                        MAX_REGULAR_INFECTED = builder
                                .comment(" Replaces Spore's \"Maximum number of regular infected\". Default 120.")
                                .defineInRange("maxRegularInfected", 120, 0, Integer.MAX_VALUE);
                        MAX_EVOLVED_INFECTED = builder
                                .comment(" Replaces Spore's \"Maximum number of evolved infected\". Default 70.")
                                .defineInRange("maxEvolvedInfected", 70, 0, Integer.MAX_VALUE);
                        MAX_HYPER_INFECTED = builder
                                .comment(" Replaces Spore's \"Maximum number of hyper infected\". Default 40.")
                                .defineInRange("maxHyperInfected", 40, 0, Integer.MAX_VALUE);
                        MAX_ORGANOIDS = builder
                                .comment(" Replaces Spore's \"Maximum number of organoids\". Default 60.")
                                .defineInRange("maxOrganoids", 60, 0, Integer.MAX_VALUE);
                        DESPAWN_BLACKLIST = builder
                                .comment(" Replaces Spore's \"Mobs that will not be despawned by the system\" list.")
                                .defineListAllowEmpty(
                                        "despawnBlacklist",
                                        () -> Lists.newArrayList(
                                                "spore:proto",
                                                "spore:reconstructor",
                                                "spore:vanguard",
                                                "spore:reaper",
                                                "spore:gastgaber",
                                                "spore:specter",
                                                "spore:inf_construct",
                                                "spore:scamper",
                                                "spore:hivetumor",
                                                "spore:mound"
                                        ),
                                        () -> "modid:entity_id",
                                        o -> o instanceof String
                                );
                builder.pop();
        builder.pop();

        SPEC = builder.build();
    }

    private SporeBreachOverridesConfig() {
    }
}
