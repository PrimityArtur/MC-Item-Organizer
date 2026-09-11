package com.itemorganizer.storage;

import com.itemorganizer.core.model.VersionCatalog;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// repository for loading and saving items per version in porVersion.json
public class VersionCatalogRepository {
    private final Path versionFile;
    private VersionCatalog cachedCatalog;

    public VersionCatalogRepository(Path baseDir) {
        this.versionFile = baseDir.resolve("porVersion.json");
    }

    public VersionCatalog load() {
        if (!Files.exists(versionFile)) {
            cachedCatalog = createDefaultTemplate();
            save(cachedCatalog);
            return cachedCatalog;
        }
        cachedCatalog = JsonHelper.load(versionFile, VersionCatalog.class, new VersionCatalog());
        if (cachedCatalog.getVersions().isEmpty() || isLegacyTemplate(cachedCatalog)) {
            cachedCatalog = createDefaultTemplate();
            save(cachedCatalog);
        }
        return cachedCatalog;
    }

    public boolean save(VersionCatalog catalog) {
        if (catalog == null) return false;
        this.cachedCatalog = catalog;
        return JsonHelper.saveAtomic(versionFile, catalog);
    }

    public VersionCatalog getCatalog() {
        if (cachedCatalog == null) {
            return load();
        }
        return cachedCatalog;
    }

    private boolean isLegacyTemplate(VersionCatalog catalog) {
        if (!catalog.getVersions().containsKey("26.3") || !catalog.getVersions().containsKey("1.8")) {
            return true;
        }
        int total = 0;
        for (List<String> list : catalog.getVersions().values()) {
            total += list.size();
        }
        return total < 500;
    }

    public VersionCatalog createDefaultTemplate() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        add_1_8(map);
        add_Beta_1_9_Prerelease(map);
        add_1_10(map);
        add_1_11(map);
        add_1_12(map);
        add_1_13(map);
        add_1_13_1(map);
        add_1_14(map);
        add_1_15(map);
        add_1_16(map);
        add_1_17(map);
        add_1_19(map);
        add_1_19_3(map);
        add_1_20(map);
        add_1_21(map);
        add_1_21_4(map);
        add_1_21_5(map);
        add_1_21_6(map);
        add_1_21_9(map);
        add_26_1(map);
        add_26_2(map);
        add_26_3(map);
        return new VersionCatalog(map);
    }

    private static void add_1_8(Map<String, List<String>> map) {
        map.put("1.8", Arrays.asList(
                "minecraft:stone", "minecraft:grass_block", "minecraft:dirt",
                "minecraft:cobblestone", "minecraft:oak_planks", "minecraft:oak_sapling", "minecraft:bedrock",
                "minecraft:sand", "minecraft:gravel",
                "minecraft:gold_ore", "minecraft:iron_ore", "minecraft:coal_ore", "minecraft:oak_log",
                "minecraft:oak_leaves", "minecraft:sponge", "minecraft:glass", "minecraft:white_wool",
                "minecraft:dandelion", "minecraft:poppy", "minecraft:brown_mushroom", "minecraft:red_mushroom",
                "minecraft:gold_block", "minecraft:iron_block", "minecraft:smooth_stone_slab", "minecraft:bricks",
                "minecraft:tnt", "minecraft:bookshelf", "minecraft:mossy_cobblestone", "minecraft:obsidian",
                "minecraft:torch", "minecraft:chest",
                "minecraft:diamond_ore", "minecraft:diamond_block", "minecraft:crafting_table", "minecraft:wheat",
                "minecraft:farmland", "minecraft:furnace", "minecraft:oak_sign", "minecraft:oak_door",
                "minecraft:ladder", "minecraft:rail", "minecraft:spawner", "minecraft:oak_stairs",
                "minecraft:cobblestone_stairs", "minecraft:lever",
                "minecraft:stone_pressure_plate", "minecraft:iron_door", "minecraft:oak_pressure_plate", "minecraft:redstone_ore",
                "minecraft:redstone_torch", "minecraft:stone_button", "minecraft:snow",
                "minecraft:ice", "minecraft:snow_block", "minecraft:cactus", "minecraft:clay",
                "minecraft:sugar_cane", "minecraft:jukebox", "minecraft:oak_fence", "minecraft:carved_pumpkin",
                "minecraft:netherrack", "minecraft:soul_sand", "minecraft:glowstone",
                "minecraft:jack_o_lantern", "minecraft:spruce_log", "minecraft:birch_log", "minecraft:spruce_leaves",
                "minecraft:birch_leaves", "minecraft:lapis_ore", "minecraft:lapis_block", "minecraft:dispenser",
                "minecraft:sandstone", "minecraft:note_block", "minecraft:orange_wool", "minecraft:magenta_wool",
                "minecraft:light_blue_wool", "minecraft:yellow_wool", "minecraft:lime_wool", "minecraft:pink_wool",
                "minecraft:gray_wool", "minecraft:light_gray_wool", "minecraft:cyan_wool", "minecraft:purple_wool",
                "minecraft:blue_wool", "minecraft:brown_wool", "minecraft:green_wool", "minecraft:red_wool",
                "minecraft:black_wool", "minecraft:cake", "minecraft:red_bed", "minecraft:smooth_stone",
                "minecraft:sandstone_slab", "minecraft:petrified_oak_slab", "minecraft:cobblestone_slab", "minecraft:repeater",
                "minecraft:spruce_sapling", "minecraft:birch_sapling", "minecraft:powered_rail", "minecraft:detector_rail",
                "minecraft:cobweb", "minecraft:tall_grass", "minecraft:short_grass", "minecraft:fern",
                "minecraft:dead_bush", "minecraft:oak_trapdoor", "minecraft:sticky_piston", "minecraft:piston",
                "minecraft:brick_slab", "minecraft:stone_brick_slab",
                "minecraft:infested_stone", "minecraft:infested_cobblestone", "minecraft:infested_stone_bricks", "minecraft:stone_bricks",
                "minecraft:mossy_stone_bricks", "minecraft:cracked_stone_bricks", "minecraft:brown_mushroom_block", "minecraft:mushroom_stem",
                "minecraft:red_mushroom_block", "minecraft:iron_bars", "minecraft:glass_pane", "minecraft:melon",
                "minecraft:vine", "minecraft:oak_fence_gate", "minecraft:brick_stairs", "minecraft:stone_brick_stairs",
                "minecraft:jungle_log", "minecraft:jungle_leaves", "minecraft:jungle_sapling", "minecraft:redstone_lamp",
                "minecraft:chiseled_stone_bricks", "minecraft:spruce_planks", "minecraft:birch_planks", "minecraft:jungle_planks",
                "minecraft:chiseled_sandstone", "minecraft:cut_sandstone", "minecraft:oak_slab", "minecraft:spruce_slab",
                "minecraft:birch_slab", "minecraft:jungle_slab", "minecraft:cocoa_beans", "minecraft:sandstone_stairs",
                "minecraft:emerald_ore", "minecraft:ender_chest", "minecraft:tripwire_hook", "minecraft:string",
                "minecraft:emerald_block", "minecraft:spruce_stairs", "minecraft:birch_stairs", "minecraft:jungle_stairs",
                "minecraft:oak_wood", "minecraft:spruce_wood", "minecraft:birch_wood", "minecraft:jungle_wood",
                "minecraft:command_block", "minecraft:beacon", "minecraft:cobblestone_wall", "minecraft:mossy_cobblestone_wall",
                "minecraft:flower_pot",
                "minecraft:carrot", "minecraft:potato", "minecraft:oak_button", "minecraft:skeleton_skull",
                "minecraft:wither_skeleton_skull", "minecraft:zombie_head",
                "minecraft:player_head", "minecraft:creeper_head",
                "minecraft:anvil", "minecraft:chipped_anvil", "minecraft:damaged_anvil",
                "minecraft:nether_brick_slab", "minecraft:trapped_chest", "minecraft:light_weighted_pressure_plate", "minecraft:heavy_weighted_pressure_plate",
                "minecraft:comparator", "minecraft:daylight_detector", "minecraft:redstone_block", "minecraft:nether_quartz_ore",
                "minecraft:hopper", "minecraft:quartz_slab", "minecraft:quartz_block", "minecraft:chiseled_quartz_block",
                "minecraft:quartz_pillar", "minecraft:quartz_stairs", "minecraft:activator_rail", "minecraft:dropper",
                "minecraft:smooth_sandstone", "minecraft:smooth_quartz", "minecraft:hay_block", "minecraft:white_carpet",
                "minecraft:orange_carpet", "minecraft:magenta_carpet", "minecraft:light_blue_carpet", "minecraft:yellow_carpet",
                "minecraft:lime_carpet", "minecraft:pink_carpet", "minecraft:gray_carpet", "minecraft:light_gray_carpet",
                "minecraft:cyan_carpet", "minecraft:purple_carpet", "minecraft:blue_carpet", "minecraft:brown_carpet",
                "minecraft:green_carpet", "minecraft:red_carpet", "minecraft:black_carpet", "minecraft:terracotta",
                "minecraft:coal_block", "minecraft:white_terracotta", "minecraft:orange_terracotta", "minecraft:magenta_terracotta",
                "minecraft:light_blue_terracotta", "minecraft:yellow_terracotta", "minecraft:lime_terracotta", "minecraft:pink_terracotta",
                "minecraft:gray_terracotta", "minecraft:light_gray_terracotta", "minecraft:cyan_terracotta", "minecraft:purple_terracotta",
                "minecraft:blue_terracotta", "minecraft:brown_terracotta", "minecraft:green_terracotta", "minecraft:red_terracotta",
                "minecraft:black_terracotta", "minecraft:coarse_dirt", "minecraft:podzol", "minecraft:blue_orchid",
                "minecraft:allium", "minecraft:azure_bluet", "minecraft:red_tulip", "minecraft:orange_tulip",
                "minecraft:white_tulip", "minecraft:pink_tulip", "minecraft:oxeye_daisy", "minecraft:infested_mossy_stone_bricks",
                "minecraft:infested_cracked_stone_bricks", "minecraft:infested_chiseled_stone_bricks", "minecraft:packed_ice", "minecraft:sunflower",
                "minecraft:lilac", "minecraft:tall_grass", "minecraft:large_fern", "minecraft:rose_bush",
                "minecraft:peony", "minecraft:red_sand", "minecraft:white_stained_glass", "minecraft:orange_stained_glass",
                "minecraft:magenta_stained_glass", "minecraft:light_blue_stained_glass", "minecraft:yellow_stained_glass", "minecraft:lime_stained_glass",
                "minecraft:pink_stained_glass", "minecraft:gray_stained_glass", "minecraft:light_gray_stained_glass", "minecraft:cyan_stained_glass",
                "minecraft:purple_stained_glass", "minecraft:blue_stained_glass", "minecraft:brown_stained_glass", "minecraft:green_stained_glass",
                "minecraft:red_stained_glass", "minecraft:black_stained_glass", "minecraft:white_stained_glass_pane", "minecraft:orange_stained_glass_pane",
                "minecraft:magenta_stained_glass_pane", "minecraft:light_blue_stained_glass_pane", "minecraft:yellow_stained_glass_pane", "minecraft:lime_stained_glass_pane",
                "minecraft:pink_stained_glass_pane", "minecraft:gray_stained_glass_pane", "minecraft:light_gray_stained_glass_pane", "minecraft:cyan_stained_glass_pane",
                "minecraft:purple_stained_glass_pane", "minecraft:blue_stained_glass_pane", "minecraft:brown_stained_glass_pane", "minecraft:green_stained_glass_pane",
                "minecraft:red_stained_glass_pane", "minecraft:black_stained_glass_pane", "minecraft:acacia_sapling", "minecraft:dark_oak_sapling",
                "minecraft:acacia_leaves", "minecraft:dark_oak_leaves",
                "minecraft:acacia_log", "minecraft:dark_oak_log", "minecraft:acacia_wood", "minecraft:dark_oak_wood",
                "minecraft:acacia_planks", "minecraft:dark_oak_planks", "minecraft:acacia_slab", "minecraft:dark_oak_slab",
                "minecraft:acacia_stairs", "minecraft:dark_oak_stairs", "minecraft:granite", "minecraft:polished_granite",
                "minecraft:diorite", "minecraft:polished_diorite", "minecraft:andesite", "minecraft:polished_andesite",
                "minecraft:slime_block", "minecraft:barrier", "minecraft:iron_trapdoor", "minecraft:wet_sponge",
                "minecraft:prismarine", "minecraft:prismarine_bricks", "minecraft:dark_prismarine", "minecraft:sea_lantern",
                "minecraft:white_banner", "minecraft:orange_banner",
                "minecraft:magenta_banner", "minecraft:light_blue_banner",
                "minecraft:yellow_banner", "minecraft:lime_banner",
                "minecraft:pink_banner", "minecraft:gray_banner",
                "minecraft:light_gray_banner", "minecraft:cyan_banner",
                "minecraft:purple_banner", "minecraft:blue_banner",
                "minecraft:brown_banner", "minecraft:green_banner",
                "minecraft:red_banner", "minecraft:black_banner",
                "minecraft:red_sandstone", "minecraft:chiseled_red_sandstone", "minecraft:cut_red_sandstone", "minecraft:red_sandstone_stairs",
                "minecraft:smooth_red_sandstone", "minecraft:red_sandstone_slab", "minecraft:spruce_fence_gate", "minecraft:birch_fence_gate",
                "minecraft:jungle_fence_gate", "minecraft:dark_oak_fence_gate", "minecraft:acacia_fence_gate", "minecraft:spruce_fence",
                "minecraft:birch_fence", "minecraft:jungle_fence", "minecraft:dark_oak_fence", "minecraft:acacia_fence",
                "minecraft:spruce_door", "minecraft:birch_door", "minecraft:jungle_door", "minecraft:acacia_door",
                "minecraft:dark_oak_door"
        ));
    }

    private static void add_Beta_1_9_Prerelease(Map<String, List<String>> map) {
        map.put("Beta 1.9 Prerelease", Arrays.asList(
                "minecraft:mycelium", "minecraft:lily_pad", "minecraft:nether_bricks", "minecraft:nether_brick_fence",
                "minecraft:nether_brick_stairs", "minecraft:nether_wart", "minecraft:enchanting_table", "minecraft:brewing_stand",
                "minecraft:cauldron", "minecraft:end_portal_frame",
                "minecraft:end_stone", "minecraft:dragon_egg", "minecraft:dragon_head",
                "minecraft:end_rod", "minecraft:chorus_plant", "minecraft:chorus_flower", "minecraft:purpur_block",
                "minecraft:purpur_pillar", "minecraft:purpur_stairs", "minecraft:purpur_slab", "minecraft:end_stone_bricks",
                "minecraft:beetroot", "minecraft:dirt_path", "minecraft:structure_block",
                "minecraft:repeating_command_block", "minecraft:chain_command_block", "minecraft:frosted_ice"
        ));
    }

    private static void add_1_10(Map<String, List<String>> map) {
        map.put("1.10", Arrays.asList(
                "minecraft:magma_block", "minecraft:nether_wart_block", "minecraft:red_nether_bricks", "minecraft:bone_block",
                "minecraft:structure_void"
        ));
    }

    private static void add_1_11(Map<String, List<String>> map) {
        map.put("1.11", Arrays.asList(
                "minecraft:observer", "minecraft:white_shulker_box", "minecraft:orange_shulker_box", "minecraft:magenta_shulker_box",
                "minecraft:light_blue_shulker_box", "minecraft:yellow_shulker_box", "minecraft:lime_shulker_box", "minecraft:pink_shulker_box",
                "minecraft:gray_shulker_box", "minecraft:light_gray_shulker_box", "minecraft:cyan_shulker_box", "minecraft:purple_shulker_box",
                "minecraft:blue_shulker_box", "minecraft:brown_shulker_box", "minecraft:green_shulker_box", "minecraft:red_shulker_box",
                "minecraft:black_shulker_box"
        ));
    }

    private static void add_1_12(Map<String, List<String>> map) {
        map.put("1.12", Arrays.asList(
                "minecraft:white_glazed_terracotta", "minecraft:orange_glazed_terracotta", "minecraft:magenta_glazed_terracotta", "minecraft:light_blue_glazed_terracotta",
                "minecraft:yellow_glazed_terracotta", "minecraft:lime_glazed_terracotta", "minecraft:pink_glazed_terracotta", "minecraft:gray_glazed_terracotta",
                "minecraft:light_gray_glazed_terracotta", "minecraft:cyan_glazed_terracotta", "minecraft:purple_glazed_terracotta", "minecraft:blue_glazed_terracotta",
                "minecraft:brown_glazed_terracotta", "minecraft:green_glazed_terracotta", "minecraft:red_glazed_terracotta", "minecraft:black_glazed_terracotta",
                "minecraft:white_concrete", "minecraft:orange_concrete", "minecraft:magenta_concrete", "minecraft:light_blue_concrete",
                "minecraft:yellow_concrete", "minecraft:lime_concrete", "minecraft:pink_concrete", "minecraft:gray_concrete",
                "minecraft:light_gray_concrete", "minecraft:cyan_concrete", "minecraft:purple_concrete", "minecraft:blue_concrete",
                "minecraft:brown_concrete", "minecraft:green_concrete", "minecraft:red_concrete", "minecraft:black_concrete",
                "minecraft:white_concrete_powder", "minecraft:orange_concrete_powder", "minecraft:magenta_concrete_powder", "minecraft:light_blue_concrete_powder",
                "minecraft:yellow_concrete_powder", "minecraft:lime_concrete_powder", "minecraft:pink_concrete_powder", "minecraft:gray_concrete_powder",
                "minecraft:light_gray_concrete_powder", "minecraft:cyan_concrete_powder", "minecraft:purple_concrete_powder", "minecraft:blue_concrete_powder",
                "minecraft:brown_concrete_powder", "minecraft:green_concrete_powder", "minecraft:red_concrete_powder", "minecraft:black_concrete_powder",
                "minecraft:white_bed", "minecraft:orange_bed", "minecraft:magenta_bed", "minecraft:light_blue_bed",
                "minecraft:yellow_bed", "minecraft:lime_bed", "minecraft:pink_bed", "minecraft:gray_bed",
                "minecraft:light_gray_bed", "minecraft:cyan_bed", "minecraft:purple_bed", "minecraft:blue_bed",
                "minecraft:brown_bed", "minecraft:green_bed", "minecraft:black_bed"
        ));
    }

    private static void add_1_13(Map<String, List<String>> map) {
        map.put("1.13", Arrays.asList(
                "minecraft:pumpkin", "minecraft:spruce_pressure_plate", "minecraft:birch_pressure_plate", "minecraft:jungle_pressure_plate",
                "minecraft:dark_oak_pressure_plate", "minecraft:acacia_pressure_plate", "minecraft:spruce_trapdoor", "minecraft:birch_trapdoor",
                "minecraft:jungle_trapdoor", "minecraft:acacia_trapdoor", "minecraft:dark_oak_trapdoor", "minecraft:spruce_button",
                "minecraft:birch_button", "minecraft:jungle_button", "minecraft:acacia_button", "minecraft:dark_oak_button",
                "minecraft:stripped_oak_log", "minecraft:stripped_spruce_log",
                "minecraft:stripped_birch_log", "minecraft:stripped_jungle_log", "minecraft:stripped_acacia_log", "minecraft:stripped_dark_oak_log",
                "minecraft:prismarine_slab", "minecraft:prismarine_brick_slab", "minecraft:dark_prismarine_slab", "minecraft:prismarine_stairs",
                "minecraft:prismarine_brick_stairs", "minecraft:dark_prismarine_stairs", "minecraft:dried_kelp_block", "minecraft:seagrass",
                "minecraft:turtle_egg", "minecraft:kelp",
                "minecraft:tube_coral_block", "minecraft:brain_coral_block", "minecraft:bubble_coral_block",
                "minecraft:fire_coral_block", "minecraft:horn_coral_block", "minecraft:tube_coral", "minecraft:brain_coral",
                "minecraft:bubble_coral", "minecraft:fire_coral", "minecraft:horn_coral", "minecraft:dead_tube_coral_block",
                "minecraft:dead_brain_coral_block", "minecraft:dead_bubble_coral_block", "minecraft:dead_fire_coral_block", "minecraft:dead_horn_coral_block",
                "minecraft:shulker_box", "minecraft:tube_coral_fan", "minecraft:brain_coral_fan",
                "minecraft:bubble_coral_fan", "minecraft:fire_coral_fan",
                "minecraft:horn_coral_fan", "minecraft:sea_pickle",
                "minecraft:blue_ice", "minecraft:conduit", "minecraft:stripped_oak_wood", "minecraft:stripped_spruce_wood",
                "minecraft:stripped_birch_wood", "minecraft:stripped_jungle_wood", "minecraft:stripped_acacia_wood", "minecraft:stripped_dark_oak_wood",
                "minecraft:dead_tube_coral_fan", "minecraft:dead_brain_coral_fan",
                "minecraft:dead_bubble_coral_fan", "minecraft:dead_fire_coral_fan",
                "minecraft:dead_horn_coral_fan"
        ));
    }

    private static void add_1_13_1(Map<String, List<String>> map) {
        map.put("1.13.1", Arrays.asList(
                "minecraft:dead_brain_coral", "minecraft:dead_bubble_coral", "minecraft:dead_fire_coral", "minecraft:dead_horn_coral",
                "minecraft:dead_tube_coral"
        ));
    }

    private static void add_1_14(Map<String, List<String>> map) {
        map.put("1.14", Arrays.asList(
                "minecraft:stone_slab", "minecraft:polished_granite_stairs", "minecraft:smooth_red_sandstone_stairs", "minecraft:mossy_stone_brick_stairs",
                "minecraft:polished_diorite_stairs", "minecraft:mossy_cobblestone_stairs", "minecraft:end_stone_brick_stairs", "minecraft:stone_stairs",
                "minecraft:smooth_sandstone_stairs", "minecraft:smooth_quartz_stairs", "minecraft:granite_stairs", "minecraft:andesite_stairs",
                "minecraft:red_nether_brick_stairs", "minecraft:polished_andesite_stairs", "minecraft:diorite_stairs", "minecraft:polished_granite_slab",
                "minecraft:smooth_red_sandstone_slab", "minecraft:mossy_stone_brick_slab", "minecraft:polished_diorite_slab", "minecraft:mossy_cobblestone_slab",
                "minecraft:end_stone_brick_slab", "minecraft:smooth_sandstone_slab", "minecraft:smooth_quartz_slab", "minecraft:granite_slab",
                "minecraft:andesite_slab", "minecraft:red_nether_brick_slab", "minecraft:polished_andesite_slab", "minecraft:diorite_slab",
                "minecraft:brick_wall", "minecraft:prismarine_wall", "minecraft:red_sandstone_wall", "minecraft:mossy_stone_brick_wall",
                "minecraft:granite_wall", "minecraft:stone_brick_wall", "minecraft:nether_brick_wall", "minecraft:andesite_wall",
                "minecraft:red_nether_brick_wall", "minecraft:sandstone_wall", "minecraft:end_stone_brick_wall", "minecraft:diorite_wall",
                "minecraft:spruce_sign", "minecraft:birch_sign",
                "minecraft:acacia_sign", "minecraft:jungle_sign",
                "minecraft:dark_oak_sign", "minecraft:cornflower", "minecraft:lily_of_the_valley",
                "minecraft:wither_rose", "minecraft:loom", "minecraft:bamboo",
                "minecraft:barrel", "minecraft:smoker", "minecraft:blast_furnace", "minecraft:cartography_table",
                "minecraft:fletching_table", "minecraft:grindstone", "minecraft:smithing_table", "minecraft:stonecutter",
                "minecraft:bell", "minecraft:lectern", "minecraft:scaffolding", "minecraft:lantern",
                "minecraft:jigsaw", "minecraft:sweet_berries", "minecraft:campfire", "minecraft:composter",
                "minecraft:cut_sandstone_slab", "minecraft:cut_red_sandstone_slab"
        ));
    }

    private static void add_1_15(Map<String, List<String>> map) {
        map.put("1.15", Arrays.asList(
                "minecraft:bee_nest", "minecraft:beehive", "minecraft:honey_block", "minecraft:honeycomb_block"
        ));
    }

    private static void add_1_16(Map<String, List<String>> map) {
        map.put("1.16", Arrays.asList(
                "minecraft:crimson_nylium", "minecraft:warped_nylium", "minecraft:crimson_planks", "minecraft:warped_planks",
                "minecraft:crimson_stem", "minecraft:warped_stem", "minecraft:stripped_crimson_stem", "minecraft:stripped_warped_stem",
                "minecraft:crimson_slab", "minecraft:warped_slab", "minecraft:soul_soil", "minecraft:basalt",
                "minecraft:crimson_stairs", "minecraft:warped_stairs", "minecraft:warped_wart_block", "minecraft:netherite_block",
                "minecraft:ancient_debris", "minecraft:crimson_fungus", "minecraft:warped_fungus", "minecraft:crimson_roots",
                "minecraft:warped_roots", "minecraft:nether_sprouts", "minecraft:weeping_vines",
                "minecraft:crimson_fence", "minecraft:warped_fence", "minecraft:soul_torch",
                "minecraft:crimson_sign", "minecraft:warped_sign",
                "minecraft:soul_lantern", "minecraft:shroomlight", "minecraft:crimson_pressure_plate", "minecraft:warped_pressure_plate",
                "minecraft:crimson_trapdoor", "minecraft:warped_trapdoor", "minecraft:crimson_fence_gate", "minecraft:warped_fence_gate",
                "minecraft:crimson_button", "minecraft:warped_button", "minecraft:crimson_door", "minecraft:warped_door",
                "minecraft:crying_obsidian",
                "minecraft:target", "minecraft:stripped_warped_hyphae",
                "minecraft:stripped_crimson_hyphae", "minecraft:warped_hyphae", "minecraft:crimson_hyphae", "minecraft:nether_gold_ore",
                "minecraft:twisting_vines", "minecraft:polished_basalt", "minecraft:respawn_anchor",
                "minecraft:lodestone", "minecraft:cracked_nether_bricks", "minecraft:chiseled_nether_bricks", "minecraft:quartz_bricks",
                "minecraft:blackstone", "minecraft:blackstone_slab", "minecraft:blackstone_stairs", "minecraft:blackstone_wall",
                "minecraft:gilded_blackstone", "minecraft:polished_blackstone", "minecraft:polished_blackstone_slab", "minecraft:polished_blackstone_stairs",
                "minecraft:polished_blackstone_wall", "minecraft:chiseled_polished_blackstone", "minecraft:polished_blackstone_bricks", "minecraft:polished_blackstone_brick_slab",
                "minecraft:polished_blackstone_brick_stairs", "minecraft:polished_blackstone_brick_wall", "minecraft:cracked_polished_blackstone_bricks", "minecraft:soul_campfire",
                "minecraft:polished_blackstone_button", "minecraft:polished_blackstone_pressure_plate", "minecraft:iron_chain"
        ));
    }

    private static void add_1_17(Map<String, List<String>> map) {
        map.put("1.17", Arrays.asList(
                "minecraft:calcite", "minecraft:tuff", "minecraft:copper_ore", "minecraft:tinted_glass",
                "minecraft:amethyst_block", "minecraft:budding_amethyst", "minecraft:copper_block", "minecraft:exposed_copper",
                "minecraft:weathered_copper", "minecraft:oxidized_copper", "minecraft:cut_copper", "minecraft:exposed_cut_copper",
                "minecraft:weathered_cut_copper", "minecraft:oxidized_cut_copper", "minecraft:cut_copper_stairs", "minecraft:exposed_cut_copper_stairs",
                "minecraft:weathered_cut_copper_stairs", "minecraft:oxidized_cut_copper_stairs", "minecraft:cut_copper_slab", "minecraft:exposed_cut_copper_slab",
                "minecraft:weathered_cut_copper_slab", "minecraft:oxidized_cut_copper_slab", "minecraft:waxed_copper_block", "minecraft:waxed_exposed_copper",
                "minecraft:waxed_weathered_copper", "minecraft:waxed_cut_copper", "minecraft:waxed_exposed_cut_copper", "minecraft:waxed_weathered_cut_copper",
                "minecraft:waxed_cut_copper_stairs", "minecraft:waxed_exposed_cut_copper_stairs", "minecraft:waxed_weathered_cut_copper_stairs", "minecraft:waxed_cut_copper_slab",
                "minecraft:waxed_exposed_cut_copper_slab", "minecraft:waxed_weathered_cut_copper_slab", "minecraft:candle", "minecraft:white_candle",
                "minecraft:orange_candle", "minecraft:magenta_candle", "minecraft:light_blue_candle", "minecraft:yellow_candle",
                "minecraft:lime_candle", "minecraft:pink_candle", "minecraft:gray_candle", "minecraft:light_gray_candle",
                "minecraft:cyan_candle", "minecraft:purple_candle", "minecraft:blue_candle", "minecraft:brown_candle",
                "minecraft:green_candle", "minecraft:red_candle", "minecraft:black_candle", "minecraft:small_amethyst_bud",
                "minecraft:medium_amethyst_bud", "minecraft:large_amethyst_bud", "minecraft:amethyst_cluster", "minecraft:lightning_rod",
                "minecraft:dripstone_block", "minecraft:pointed_dripstone", "minecraft:sculk_sensor", "minecraft:glow_lichen",
                "minecraft:azalea_leaves", "minecraft:flowering_azalea_leaves", "minecraft:azalea", "minecraft:flowering_azalea",
                "minecraft:spore_blossom", "minecraft:moss_carpet", "minecraft:moss_block", "minecraft:rooted_dirt",
                "minecraft:hanging_roots", "minecraft:big_dripleaf", "minecraft:small_dripleaf",
                "minecraft:deepslate", "minecraft:polished_deepslate",
                "minecraft:deepslate_bricks", "minecraft:deepslate_tiles", "minecraft:chiseled_deepslate", "minecraft:cobbled_deepslate_wall",
                "minecraft:polished_deepslate_wall", "minecraft:deepslate_tile_wall", "minecraft:deepslate_brick_wall", "minecraft:cobbled_deepslate_stairs",
                "minecraft:polished_deepslate_stairs", "minecraft:deepslate_tile_stairs", "minecraft:deepslate_brick_stairs", "minecraft:cobbled_deepslate_slab",
                "minecraft:polished_deepslate_slab", "minecraft:deepslate_tile_slab", "minecraft:deepslate_brick_slab", "minecraft:cobbled_deepslate",
                "minecraft:deepslate_gold_ore", "minecraft:deepslate_iron_ore", "minecraft:deepslate_lapis_ore", "minecraft:deepslate_diamond_ore",
                "minecraft:deepslate_redstone_ore", "minecraft:smooth_basalt", "minecraft:deepslate_copper_ore", "minecraft:deepslate_coal_ore",
                "minecraft:cracked_deepslate_bricks", "minecraft:cracked_deepslate_tiles", "minecraft:deepslate_emerald_ore", "minecraft:infested_deepslate",
                "minecraft:light", "minecraft:waxed_oxidized_copper", "minecraft:waxed_oxidized_cut_copper", "minecraft:waxed_oxidized_cut_copper_stairs",
                "minecraft:waxed_oxidized_cut_copper_slab", "minecraft:raw_iron_block", "minecraft:raw_copper_block", "minecraft:raw_gold_block",
                "minecraft:potted_azalea_bush"
        ));
    }

    private static void add_1_19(Map<String, List<String>> map) {
        map.put("1.19", Arrays.asList(
                "minecraft:reinforced_deepslate", "minecraft:sculk", "minecraft:sculk_vein", "minecraft:sculk_catalyst",
                "minecraft:sculk_shrieker", "minecraft:mud", "minecraft:mangrove_planks", "minecraft:mangrove_log",
                "minecraft:mangrove_roots", "minecraft:muddy_mangrove_roots", "minecraft:stripped_mangrove_log", "minecraft:stripped_mangrove_wood",
                "minecraft:mangrove_wood", "minecraft:mangrove_slab", "minecraft:mud_brick_slab", "minecraft:mangrove_fence",
                "minecraft:packed_mud", "minecraft:mud_bricks", "minecraft:mud_brick_stairs", "minecraft:mangrove_stairs",
                "minecraft:mangrove_fence_gate", "minecraft:mangrove_propagule", "minecraft:mangrove_leaves", "minecraft:mud_brick_wall",
                "minecraft:mangrove_sign", "minecraft:ochre_froglight",
                "minecraft:verdant_froglight", "minecraft:pearlescent_froglight", "minecraft:mangrove_button", "minecraft:mangrove_pressure_plate",
                "minecraft:mangrove_door", "minecraft:mangrove_trapdoor", "minecraft:frogspawn"
        ));
    }

    private static void add_1_19_3(Map<String, List<String>> map) {
        map.put("1.19.3", Arrays.asList(
                "minecraft:bamboo_planks", "minecraft:bamboo_mosaic", "minecraft:bamboo_stairs", "minecraft:bamboo_mosaic_stairs",
                "minecraft:bamboo_slab", "minecraft:bamboo_mosaic_slab", "minecraft:bamboo_fence", "minecraft:bamboo_fence_gate",
                "minecraft:bamboo_door", "minecraft:bamboo_trapdoor", "minecraft:chiseled_bookshelf", "minecraft:oak_hanging_sign",
                "minecraft:spruce_hanging_sign", "minecraft:birch_hanging_sign",
                "minecraft:jungle_hanging_sign", "minecraft:acacia_hanging_sign",
                "minecraft:dark_oak_hanging_sign", "minecraft:mangrove_hanging_sign",
                "minecraft:bamboo_sign", "minecraft:bamboo_hanging_sign",
                "minecraft:crimson_hanging_sign", "minecraft:warped_hanging_sign",
                "minecraft:bamboo_button", "minecraft:bamboo_pressure_plate", "minecraft:bamboo_block",
                "minecraft:stripped_bamboo_block", "minecraft:piglin_head"
        ));
    }

    private static void add_1_20(Map<String, List<String>> map) {
        map.put("1.20", Arrays.asList(
                "minecraft:cherry_log", "minecraft:cherry_wood", "minecraft:stripped_cherry_log", "minecraft:stripped_cherry_wood",
                "minecraft:cherry_planks", "minecraft:cherry_stairs", "minecraft:cherry_slab", "minecraft:cherry_fence",
                "minecraft:cherry_fence_gate", "minecraft:cherry_door", "minecraft:cherry_trapdoor", "minecraft:cherry_pressure_plate",
                "minecraft:cherry_button", "minecraft:pink_petals", "minecraft:cherry_leaves", "minecraft:cherry_sapling",
                "minecraft:torchflower", "minecraft:suspicious_sand", "minecraft:decorated_pot", "minecraft:cherry_sign",
                "minecraft:cherry_hanging_sign", "minecraft:pitcher_plant", "minecraft:pitcher_pot",
                "minecraft:sniffer_egg", "minecraft:suspicious_gravel", "minecraft:calibrated_sculk_sensor"
        ));
    }

    private static void add_1_21(Map<String, List<String>> map) {
        map.put("1.21", Arrays.asList(
                "minecraft:crafter", "minecraft:tuff_stairs", "minecraft:tuff_slab", "minecraft:tuff_wall",
                "minecraft:chiseled_tuff", "minecraft:polished_tuff", "minecraft:polished_tuff_stairs", "minecraft:polished_tuff_slab",
                "minecraft:polished_tuff_wall", "minecraft:tuff_bricks", "minecraft:tuff_brick_stairs", "minecraft:tuff_brick_slab",
                "minecraft:tuff_brick_wall", "minecraft:chiseled_tuff_bricks", "minecraft:chiseled_copper", "minecraft:copper_grate",
                "minecraft:copper_door", "minecraft:copper_trapdoor", "minecraft:copper_bulb", "minecraft:exposed_chiseled_copper",
                "minecraft:exposed_copper_grate", "minecraft:exposed_copper_door", "minecraft:exposed_copper_trapdoor", "minecraft:exposed_copper_bulb",
                "minecraft:weathered_chiseled_copper", "minecraft:weathered_copper_grate", "minecraft:weathered_copper_door", "minecraft:weathered_copper_trapdoor",
                "minecraft:weathered_copper_bulb", "minecraft:oxidized_chiseled_copper", "minecraft:oxidized_copper_grate", "minecraft:oxidized_copper_door",
                "minecraft:oxidized_copper_trapdoor", "minecraft:oxidized_copper_bulb", "minecraft:waxed_chiseled_copper", "minecraft:waxed_copper_grate",
                "minecraft:waxed_copper_door", "minecraft:waxed_copper_trapdoor", "minecraft:waxed_copper_bulb", "minecraft:waxed_exposed_chiseled_copper",
                "minecraft:waxed_exposed_copper_grate", "minecraft:waxed_exposed_copper_door", "minecraft:waxed_exposed_copper_trapdoor", "minecraft:waxed_exposed_copper_bulb",
                "minecraft:waxed_weathered_chiseled_copper", "minecraft:waxed_weathered_copper_grate", "minecraft:waxed_weathered_copper_door", "minecraft:waxed_weathered_copper_trapdoor",
                "minecraft:waxed_weathered_copper_bulb", "minecraft:waxed_oxidized_chiseled_copper", "minecraft:waxed_oxidized_copper_grate", "minecraft:waxed_oxidized_copper_door",
                "minecraft:waxed_oxidized_copper_trapdoor", "minecraft:waxed_oxidized_copper_bulb", "minecraft:trial_spawner", "minecraft:vault",
                "minecraft:heavy_core"
        ));
    }

    private static void add_1_21_4(Map<String, List<String>> map) {
        map.put("1.21.4", Arrays.asList(
                "minecraft:pale_oak_log", "minecraft:pale_oak_wood", "minecraft:stripped_pale_oak_log", "minecraft:stripped_pale_oak_wood",
                "minecraft:pale_oak_planks", "minecraft:pale_oak_stairs", "minecraft:pale_oak_slab", "minecraft:pale_oak_fence",
                "minecraft:pale_oak_fence_gate", "minecraft:pale_oak_door", "minecraft:pale_oak_trapdoor", "minecraft:pale_oak_pressure_plate",
                "minecraft:pale_oak_button", "minecraft:pale_moss_block", "minecraft:pale_moss_carpet", "minecraft:pale_hanging_moss",
                "minecraft:pale_oak_leaves", "minecraft:pale_oak_sapling", "minecraft:pale_oak_sign",
                "minecraft:pale_oak_hanging_sign", "minecraft:creaking_heart",
                "minecraft:resin_bricks", "minecraft:resin_brick_stairs", "minecraft:resin_brick_slab", "minecraft:resin_brick_wall",
                "minecraft:chiseled_resin_bricks", "minecraft:open_eyeblossom", "minecraft:closed_eyeblossom", "minecraft:resin_block",
                "minecraft:resin_clump"
        ));
    }

    private static void add_1_21_5(Map<String, List<String>> map) {
        map.put("1.21.5", Arrays.asList(
                "minecraft:wildflowers", "minecraft:leaf_litter", "minecraft:test_instance_block",
                "minecraft:test_block[mode=start]", "minecraft:test_block[mode=log]",
                "minecraft:test_block[mode=fail]", "minecraft:test_block[mode=accept]",
                "minecraft:bush", "minecraft:firefly_bush", "minecraft:cactus_flower", "minecraft:short_dry_grass",
                "minecraft:tall_dry_grass"
        ));
    }

    private static void add_1_21_6(Map<String, List<String>> map) {
        map.put("1.21.6", Arrays.asList(
                "minecraft:dried_ghast"
        ));
    }

    private static void add_1_21_9(Map<String, List<String>> map) {
        map.put("1.21.9", Arrays.asList(
                "minecraft:oak_shelf", "minecraft:spruce_shelf", "minecraft:birch_shelf", "minecraft:jungle_shelf",
                "minecraft:acacia_shelf", "minecraft:dark_oak_shelf", "minecraft:mangrove_shelf", "minecraft:cherry_shelf",
                "minecraft:pale_oak_shelf", "minecraft:bamboo_shelf", "minecraft:crimson_shelf", "minecraft:warped_shelf",
                "minecraft:copper_chest", "minecraft:exposed_copper_chest", "minecraft:weathered_copper_chest", "minecraft:oxidized_copper_chest",
                "minecraft:waxed_copper_chest", "minecraft:waxed_exposed_copper_chest", "minecraft:waxed_weathered_copper_chest", "minecraft:waxed_oxidized_copper_chest",
                "minecraft:copper_golem_statue", "minecraft:exposed_copper_golem_statue", "minecraft:weathered_copper_golem_statue", "minecraft:oxidized_copper_golem_statue",
                "minecraft:waxed_copper_golem_statue", "minecraft:waxed_exposed_copper_golem_statue", "minecraft:waxed_weathered_copper_golem_statue", "minecraft:waxed_oxidized_copper_golem_statue",
                "minecraft:exposed_lightning_rod", "minecraft:weathered_lightning_rod", "minecraft:oxidized_lightning_rod", "minecraft:waxed_lightning_rod",
                "minecraft:waxed_exposed_lightning_rod", "minecraft:waxed_weathered_lightning_rod", "minecraft:waxed_oxidized_lightning_rod", "minecraft:copper_bars",
                "minecraft:exposed_copper_bars", "minecraft:weathered_copper_bars", "minecraft:oxidized_copper_bars", "minecraft:waxed_copper_bars",
                "minecraft:waxed_exposed_copper_bars", "minecraft:waxed_weathered_copper_bars", "minecraft:waxed_oxidized_copper_bars", "minecraft:copper_chain",
                "minecraft:exposed_copper_chain", "minecraft:weathered_copper_chain", "minecraft:oxidized_copper_chain", "minecraft:waxed_copper_chain",
                "minecraft:waxed_exposed_copper_chain", "minecraft:waxed_weathered_copper_chain", "minecraft:waxed_oxidized_copper_chain", "minecraft:copper_torch",
                "minecraft:copper_lantern", "minecraft:exposed_copper_lantern", "minecraft:weathered_copper_lantern",
                "minecraft:oxidized_copper_lantern", "minecraft:waxed_copper_lantern", "minecraft:waxed_exposed_copper_lantern", "minecraft:waxed_weathered_copper_lantern",
                "minecraft:waxed_oxidized_copper_lantern"
        ));
    }

    private static void add_26_1(Map<String, List<String>> map) {
        map.put("26.1", Arrays.asList(
                "minecraft:golden_dandelion", "minecraft:potted_golden_dandelion"
        ));
    }

    private static void add_26_2(Map<String, List<String>> map) {
        map.put("26.2", Arrays.asList(
                "minecraft:cinnabar", "minecraft:cinnabar_stairs", "minecraft:cinnabar_slab", "minecraft:cinnabar_wall",
                "minecraft:polished_cinnabar", "minecraft:polished_cinnabar_stairs", "minecraft:polished_cinnabar_slab", "minecraft:polished_cinnabar_wall",
                "minecraft:cinnabar_bricks", "minecraft:cinnabar_brick_stairs", "minecraft:cinnabar_brick_slab", "minecraft:cinnabar_brick_wall",
                "minecraft:chiseled_cinnabar", "minecraft:sulfur", "minecraft:sulfur_stairs", "minecraft:sulfur_slab",
                "minecraft:sulfur_wall", "minecraft:polished_sulfur", "minecraft:polished_sulfur_stairs", "minecraft:polished_sulfur_slab",
                "minecraft:polished_sulfur_wall", "minecraft:sulfur_bricks", "minecraft:sulfur_brick_stairs", "minecraft:sulfur_brick_slab",
                "minecraft:sulfur_brick_wall", "minecraft:chiseled_sulfur", "minecraft:potent_sulfur", "minecraft:sulfur_spike"
        ));
    }

    private static void add_26_3(Map<String, List<String>> map) {
        map.put("26.3", Arrays.asList(
                "minecraft:poplar_log", "minecraft:poplar_wood", "minecraft:stripped_poplar_log", "minecraft:stripped_poplar_wood",
                "minecraft:poplar_planks", "minecraft:poplar_stairs", "minecraft:poplar_slab", "minecraft:poplar_fence",
                "minecraft:poplar_fence_gate", "minecraft:poplar_door", "minecraft:poplar_trapdoor", "minecraft:poplar_pressure_plate",
                "minecraft:poplar_button", "minecraft:white_wool_stairs", "minecraft:light_gray_wool_stairs", "minecraft:gray_wool_stairs",
                "minecraft:black_wool_stairs", "minecraft:brown_wool_stairs", "minecraft:red_wool_stairs", "minecraft:orange_wool_stairs",
                "minecraft:yellow_wool_stairs", "minecraft:lime_wool_stairs", "minecraft:green_wool_stairs", "minecraft:cyan_wool_stairs",
                "minecraft:light_blue_wool_stairs", "minecraft:blue_wool_stairs", "minecraft:purple_wool_stairs", "minecraft:magenta_wool_stairs",
                "minecraft:pink_wool_stairs", "minecraft:white_wool_slab", "minecraft:light_gray_wool_slab", "minecraft:gray_wool_slab",
                "minecraft:black_wool_slab", "minecraft:brown_wool_slab", "minecraft:red_wool_slab", "minecraft:orange_wool_slab",
                "minecraft:yellow_wool_slab", "minecraft:lime_wool_slab", "minecraft:green_wool_slab", "minecraft:cyan_wool_slab",
                "minecraft:light_blue_wool_slab", "minecraft:blue_wool_slab", "minecraft:purple_wool_slab", "minecraft:magenta_wool_slab",
                "minecraft:pink_wool_slab", "minecraft:red_poplar_leaves", "minecraft:orange_poplar_leaves", "minecraft:yellow_poplar_leaves",
                "minecraft:poplar_sapling", "minecraft:shelf_mushroom", "minecraft:red_shrub", "minecraft:potted_poplar_sapling",
                "minecraft:poplar_shelf", "minecraft:poplar_sign", "minecraft:poplar_wall_sign", "minecraft:poplar_hanging_sign",
                "minecraft:poplar_wall_hanging_sign", "minecraft:straw_bed", "minecraft:white_concrete_stairs", "minecraft:light_gray_concrete_stairs",
                "minecraft:gray_concrete_stairs", "minecraft:black_concrete_stairs", "minecraft:brown_concrete_stairs", "minecraft:red_concrete_stairs",
                "minecraft:orange_concrete_stairs", "minecraft:yellow_concrete_stairs", "minecraft:lime_concrete_stairs", "minecraft:green_concrete_stairs",
                "minecraft:cyan_concrete_stairs", "minecraft:light_blue_concrete_stairs", "minecraft:blue_concrete_stairs", "minecraft:purple_concrete_stairs",
                "minecraft:magenta_concrete_stairs", "minecraft:pink_concrete_stairs", "minecraft:white_concrete_slab", "minecraft:light_gray_concrete_slab",
                "minecraft:gray_concrete_slab", "minecraft:black_concrete_slab", "minecraft:brown_concrete_slab", "minecraft:red_concrete_slab",
                "minecraft:orange_concrete_slab", "minecraft:yellow_concrete_slab", "minecraft:lime_concrete_slab", "minecraft:green_concrete_slab",
                "minecraft:cyan_concrete_slab", "minecraft:light_blue_concrete_slab", "minecraft:blue_concrete_slab", "minecraft:purple_concrete_slab",
                "minecraft:magenta_concrete_slab", "minecraft:pink_concrete_slab"
        ));
    }

}
