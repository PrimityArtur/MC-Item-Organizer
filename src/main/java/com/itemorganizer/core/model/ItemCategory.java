package com.itemorganizer.core.model;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.EmptyBlockView;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

// categorization system for items and blocks in the organizer
public enum ItemCategory {
    FULL_BLOCKS("Full Blocks"),
    LOGS_AND_STEMS("Logs & Stems"),
    ORES("Ores"),
    GLAZED_TERRACOTTA("Glazed Terracotta"),
    GRATES("Grates"),
    BULBS("Bulbs"),
    TOOLS_REDSTONE("Redstone Tools"),
    WORKING_STATIONS("Working Stations"),
    UNCATEGORIZED_VEGETATION("Uncategorized Vegetation"),
    CROPS("Crops"),
    FLOWERS("Flowers"),
    TORCH("Torches"),
    GLASS("Glass"),
    GLASS_PANES("Glass Panes"),
    BARS("Bars"),
    CHAINS("Chains"),
    FENCES("Fences"),
    WALLS("Walls"),
    SLABS("Slabs"),
    FENCE_GATES("Fence Gates"),
    STAIRS("Stairs"),
    SAPLINGS("Saplings"),
    LEAVES("Leaves"),
    CARPETS("Carpets"),
    LANTERNS("Lanterns & Lights"),
    SHELVES("Shelves & Bookshelves"),
    SIGNS("Signs & Hanging Signs"),
    DOORS("Doors"),
    TRAPDOORS("Trapdoors"),
    PRESSURE_PLATES("Pressure Plates"),
    BEDS("Beds"),
    BOATS("Boats & Rafts"),
    BUTTONS("Buttons"),
    DYES("Dyes"),
    SPAWN_EGGS("Spawn Eggs"),
    CANDLES("Candles"),
    BANNERS("Banners"),
    CORALS("Corals"),
    HARNESS("Harness & Saddles"),
    TOOLS("Tools & Weapons"),
    ARMOR("Armor"),
    RODS("Rods"),
    BUNDLES("Bundles"),
    BOOKS("Books"),
    SHERDS("Pottery Sherds"),
    HEADS("Heads & Skulls"),
    TRIMS("Trims & Templates"),
    SHULKER_BOXES("Shulker Boxes"),
    SCRAPS("Scraps"),
    BUCKETS("Buckets"),
    POTIONS("Potions"),
    ARROWS("Arrows"),
    DISCS("Music Discs"),
    RAILS("Rails"),
    CHESTS("Chests"),
    MINECARTS("Minecarts"),
    GOLEMS("Golem Statues"),
    MISC("Miscellaneous");

    private final String displayName;

    ItemCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    // =========================================================================
    // CATEGORY DISPLAY ORDER
    //
    // TO CHANGE THE ORDER OF GROUPS:
    // Simply rearrange the entries in the list below!
    // The organizer will sort items group by group following this exact list.
    // =========================================================================
    public static final List<ItemCategory> DEFAULT_ORDER = List.of(
        FULL_BLOCKS,
        LOGS_AND_STEMS,
        ORES,
        GLAZED_TERRACOTTA,
        GRATES,
        BULBS,
        TOOLS_REDSTONE,
        WORKING_STATIONS,
        UNCATEGORIZED_VEGETATION,
        CROPS,
        FLOWERS,
        TORCH,
        GLASS,
        GLASS_PANES,
        BARS,
        CHAINS,
        FENCES,
        WALLS,
        SLABS,
        FENCE_GATES,
        STAIRS,
        SAPLINGS,
        LEAVES,
        CARPETS,
        LANTERNS,
        SHELVES,
        SIGNS,
        DOORS,
        TRAPDOORS,
        PRESSURE_PLATES,
        BEDS,
        BOATS,
        BUTTONS,
        DYES,
        SPAWN_EGGS,
        CANDLES,
        BANNERS,
        CORALS,
        HARNESS,
        TOOLS,
        ARMOR,
        RODS,
        BUNDLES,
        BOOKS,
        SHERDS,
        HEADS,
        TRIMS,
        SHULKER_BOXES,
        SCRAPS,
        BUCKETS,
        POTIONS,
        ARROWS,
        DISCS,
        RAILS,
        CHESTS,
        MINECARTS,
        GOLEMS,
        MISC
    );

    private static List<ItemCategory> activeOrder = new ArrayList<>(DEFAULT_ORDER);
    private static final Map<ItemCategory, Integer> ORDER_MAP = new EnumMap<>(ItemCategory.class);

    static {
        refreshOrderMap();
    }

    private static synchronized void refreshOrderMap() {
        ORDER_MAP.clear();
        for (int i = 0; i < activeOrder.size(); i++) {
            ORDER_MAP.put(activeOrder.get(i), i + 1);
        }
        // ensure any omitted category still gets an order value
        for (ItemCategory cat : values()) {
            ORDER_MAP.putIfAbsent(cat, activeOrder.size() + 1);
        }
    }

    public static synchronized void setOrder(List<ItemCategory> newOrder) {
        if (newOrder != null && !newOrder.isEmpty()) {
            activeOrder = new ArrayList<>(newOrder);
            refreshOrderMap();
        }
    }

    public static synchronized void resetToDefaultOrder() {
        activeOrder = new ArrayList<>(DEFAULT_ORDER);
        refreshOrderMap();
    }

    public static synchronized List<ItemCategory> getActiveOrder() {
        return Collections.unmodifiableList(activeOrder);
    }

    // load custom category order from json array in config
    public static synchronized void loadOrderFromFile(Path file) {
        if (file == null || !Files.exists(file)) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            StringBuilder sb = new StringBuilder();
            for (String l : lines) sb.append(l.trim());
            String content = sb.toString();

            if (content.startsWith("[") && content.endsWith("]")) {
                content = content.substring(1, content.length() - 1);
                String[] parts = content.split(",");
                List<ItemCategory> custom = new ArrayList<>();
                for (String part : parts) {
                    String clean = part.replace("\"", "").replace("'", "").trim();
                    if (!clean.isEmpty()) {
                        try {
                            custom.add(ItemCategory.valueOf(clean.toUpperCase(Locale.ROOT)));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
                if (!custom.isEmpty()) {
                    setOrder(custom);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    // save current category order to file
    public static synchronized void saveOrderToFile(Path file) {
        if (file == null) return;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            for (int i = 0; i < activeOrder.size(); i++) {
                sb.append("  \"").append(activeOrder.get(i).name()).append("\"");
                if (i < activeOrder.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("]\n");
            Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
        } catch (Throwable ignored) {
        }
    }

    // returns order index for category
    public static int getOrder(ItemCategory category) {
        if (category == null) return activeOrder.size() + 1;
        Integer order = ORDER_MAP.get(category);
        return order != null ? order : activeOrder.size() + 1;
    }

    // returns order index directly from item id
    public static int getOrder(String itemId) {
        return getOrder(getCategory(itemId));
    }

    // identifies item category based on id, item, and block tags
    public static ItemCategory getCategory(String itemId) {
        if (itemId == null || itemId.isEmpty()) return MISC;
        Identifier id = Identifier.tryParse(itemId);
        String path = (id != null ? id.getPath() : itemId).toLowerCase(Locale.ROOT);

        Block block = null;
        Item item = null;
        try {
            if (id != null) {
                item = Registries.ITEM.get(id);
                if (item instanceof BlockItem bi) {
                    block = bi.getBlock();
                }
            }
        } catch (Throwable ignored) {
        }

        // 1. Glazed Terracotta
        if (path.contains("glazed_terracotta") || path.contains("glazed")) {
            return GLAZED_TERRACOTTA;
        }

        // 2. Ores
        if (path.endsWith("_ore") || path.contains("_ore") || path.equals("ancient_debris") || path.equals("gilded_blackstone")) {
            return ORES;
        }

        // 3. Grates
        if (path.contains("grate")) {
            return GRATES;
        }

        // 4. Bulbs
        if (path.contains("bulb")) {
            return BULBS;
        }

        // 5. Torches
        if ((path.endsWith("torch") || path.contains("torch")) && !path.contains("torchflower")) {
            return TORCH;
        }

        // 6. Redstone Tools & Components
        if (path.equals("redstone") || path.equals("redstone_wire") || path.equals("repeater") ||
                path.equals("comparator") || path.equals("lever") || path.equals("tripwire_hook") ||
                path.equals("daylight_detector") || path.equals("piston") || path.equals("sticky_piston") ||
                path.equals("dispenser") || path.equals("dropper") || path.equals("observer") ||
                path.equals("hopper") || path.equals("tnt") || path.equals("target") ||
                path.equals("redstone_lamp") || path.equals("sculk_sensor") || path.equals("calibrated_sculk_sensor") ||
                path.contains("lightning_rod")) {
            return TOOLS_REDSTONE;
        }

        // 7. Working Stations
        if (path.equals("crafting_table") || path.equals("furnace") || path.equals("blast_furnace") ||
                path.equals("smoker") || path.equals("stonecutter") || path.equals("smithing_table") ||
                path.equals("cartography_table") || path.equals("fletching_table") || path.equals("loom") ||
                path.equals("grindstone") || path.equals("lectern") || path.equals("enchanting_table") ||
                path.equals("brewing_stand") || path.equals("cauldron") || path.equals("composter") ||
                path.equals("barrel") || path.equals("jukebox") || path.equals("note_block") ||
                path.equals("sculk_shrieker") || path.equals("bell") || path.equals("crafter") ||
                path.endsWith("anvil")) {
            return WORKING_STATIONS;
        }

        // 8. Crops
        if (path.equals("wheat") || path.equals("wheat_seeds") || path.equals("carrot") || path.equals("carrots") ||
                path.equals("potato") || path.equals("potatoes") || path.equals("beetroot") || path.equals("beetroots") ||
                path.equals("beetroot_seeds") || path.equals("melon_seeds") || path.equals("pumpkin_seeds") ||
                path.equals("nether_wart") || path.equals("sweet_berries") || path.equals("glow_berries") ||
                path.equals("cocoa_beans") || path.equals("pitcher_pod") || path.equals("torchflower_seeds") ||
                path.equals("melon_slice")) {
            return CROPS;
        }

        // 9. Flowers
        if (isFlower(path, block, item)) {
            return FLOWERS;
        }

        // 10. Logs & Stems
        if (path.endsWith("_log") || path.equals("log") || path.endsWith("_stem") || path.equals("stem") ||
                path.endsWith("_wood") || path.endsWith("_hyphae") || (path.contains("bamboo_block") && !path.contains("mosaic"))) {
            return LOGS_AND_STEMS;
        }

        // 11. Glass Panes
        if (!path.contains("bars") && !path.endsWith("_bars") && !path.equals("bars")) {
            if (path.endsWith("glass_pane") || path.endsWith("glass_plane") || path.endsWith("_pane") || path.contains("glass_pane") ||
                    (path.contains("glass") && (path.contains("pane") || path.contains("plane")))) {
                return GLASS_PANES;
            }
            if (block instanceof net.minecraft.block.PaneBlock) {
                return GLASS_PANES;
            }
        }

        // 12. Glass Blocks
        if (!path.contains("pane") && !path.contains("plane") && !path.equals("spyglass") && !path.contains("bottle")) {
            if (block instanceof net.minecraft.block.StainedGlassBlock ||
                    path.endsWith("glass") || path.endsWith("_glass") || path.contains("stained_glass")) {
                if (!(block instanceof net.minecraft.block.PaneBlock)) {
                    return GLASS;
                }
            }
        }

        // 13. Bars
        if (path.endsWith("_bars") || path.equals("bars") || path.contains("bars")) {
            return BARS;
        }

        // 14. Chains
        if (path.endsWith("chain") || path.endsWith("chains") || path.equals("chain")) {
            return CHAINS;
        }

        // 15. Fence Gates
        if (block instanceof net.minecraft.block.FenceGateBlock ||
                path.endsWith("_fence_gate") || path.endsWith("_gate") || path.equals("fence_gate") || path.equals("gate")) {
            return FENCE_GATES;
        }

        // 16. Fences
        if (block instanceof net.minecraft.block.FenceBlock ||
                path.endsWith("_fence") || path.equals("fence")) {
            return FENCES;
        }

        // 17. Walls
        if (block instanceof net.minecraft.block.WallBlock ||
                ((path.endsWith("_wall") || path.equals("wall")) && !path.contains("torch") && !path.contains("sign") && !path.contains("banner") && !path.contains("fan"))) {
            return WALLS;
        }

        // 18. Slabs
        if (block instanceof net.minecraft.block.SlabBlock ||
                path.endsWith("_slab") || path.equals("slab")) {
            return SLABS;
        }

        // 19. Stairs & Ladders
        if (block instanceof net.minecraft.block.StairsBlock ||
                path.endsWith("_stairs") || path.equals("stairs") || path.equals("ladder") || path.equals("scaffolding")) {
            return STAIRS;
        }

        // 20. Saplings & Propagules
        if (block instanceof net.minecraft.block.SaplingBlock ||
                path.endsWith("_sapling") || path.equals("sapling") || path.endsWith("_propagule") || path.equals("mangrove_propagule")) {
            return SAPLINGS;
        }

        // 21. Leaves
        if (block instanceof net.minecraft.block.LeavesBlock ||
                path.endsWith("_leaves") || path.equals("leaves")) {
            return LEAVES;
        }

        // 22. Carpets
        if (block instanceof net.minecraft.block.CarpetBlock ||
                path.endsWith("_carpet") || path.equals("carpet")) {
            return CARPETS;
        }

        // 23. Lanterns & Light Sources
        if (block instanceof net.minecraft.block.LanternBlock ||
                path.endsWith("lantern") || path.equals("lantern") || path.equals("campfire") ||
                path.equals("soul_campfire") || path.equals("sea_lantern") || path.equals("beacon") ||
                path.equals("glowstone") || path.equals("shroomlight") || path.equals("jack_o_lantern") ||
                path.contains("froglight")) {
            return LANTERNS;
        }

        // 24. Shelves & Bookshelves
        if (path.endsWith("bookshelf") || path.endsWith("shelf") || path.contains("shelf")) {
            return SHELVES;
        }

        // 25. Signs & Hanging Signs
        if (block instanceof net.minecraft.block.AbstractSignBlock ||
                path.endsWith("_sign") || path.equals("sign") || path.endsWith("_hanging_sign") || path.equals("hanging_sign") || path.contains("hanging_sign")) {
            return SIGNS;
        }

        // 26. Trapdoors
        if (block instanceof net.minecraft.block.TrapdoorBlock ||
                path.endsWith("_trapdoor") || path.equals("trapdoor")) {
            return TRAPDOORS;
        }

        // 27. Doors
        if (block instanceof net.minecraft.block.DoorBlock ||
                path.endsWith("_door") || path.equals("door")) {
            return DOORS;
        }

        // 28. Pressure Plates
        if (block instanceof net.minecraft.block.PressurePlateBlock ||
                path.endsWith("_pressure_plate") || path.equals("pressure_plate")) {
            return PRESSURE_PLATES;
        }

        // 29. Beds
        if (block instanceof net.minecraft.block.BedBlock ||
                ((path.endsWith("_bed") || path.equals("bed")) && !path.contains("bedrock"))) {
            return BEDS;
        }

        // 30. Boats & Rafts
        if (item instanceof net.minecraft.item.BoatItem ||
                path.endsWith("_boat") || path.equals("boat") || path.endsWith("_raft") || path.equals("raft")) {
            return BOATS;
        }

        // 31. Buttons
        if (block instanceof net.minecraft.block.ButtonBlock ||
                path.endsWith("_button") || path.equals("button")) {
            return BUTTONS;
        }

        // 32. Dyes
        if (item instanceof net.minecraft.item.DyeItem ||
                path.endsWith("_dye") || path.equals("dye")) {
            return DYES;
        }

        // 33. Golems & Statues
        if (path.contains("golem") && !path.contains("spawn_egg") && !path.endsWith("_egg")) {
            return GOLEMS;
        }

        // 34. Spawn Eggs & Eggs
        if (item instanceof net.minecraft.item.SpawnEggItem ||
                path.endsWith("_egg") || path.equals("egg")) {
            return SPAWN_EGGS;
        }

        // 35. Candles
        if (block instanceof net.minecraft.block.CandleBlock ||
                path.endsWith("_candle") || path.equals("candle") || path.contains("candle")) {
            return CANDLES;
        }

        // 36. Banners
        if (block instanceof net.minecraft.block.BannerBlock ||
                path.endsWith("_banner") || path.equals("banner") || path.contains("banner")) {
            return BANNERS;
        }

        // 37. Corals
        if (!path.contains("coral_block") && !(block instanceof net.minecraft.block.CoralBlockBlock)) {
            if (block instanceof net.minecraft.block.CoralBlock ||
                    block instanceof net.minecraft.block.CoralFanBlock ||
                    block instanceof net.minecraft.block.CoralWallFanBlock ||
                    path.contains("coral")) {
                return CORALS;
            }
        }

        // 38. Harness & Saddles
        if (path.contains("harness") || path.equals("saddle") || path.equals("lead")) {
            return HARNESS;
        }

        // 39. Tools & Weapons
        if (isTool(path, item)) {
            return TOOLS;
        }

        // 40. Armor
        if (isArmor(path, item)) {
            return ARMOR;
        }

        // 41. Rods
        if ((path.endsWith("_rod") || path.equals("rod")) && !path.equals("fishing_rod") && !path.endsWith("_on_a_stick")) {
            return RODS;
        }

        // 42. Bundles
        if ((item != null && item.getComponents().contains(net.minecraft.component.DataComponentTypes.BUNDLE_CONTENTS)) ||
                path.endsWith("_bundle") || path.equals("bundle") || path.contains("bundle")) {
            return BUNDLES;
        }

        // 43. Books
        if ((item != null && (item.getComponents().contains(net.minecraft.component.DataComponentTypes.WRITTEN_BOOK_CONTENT) ||
                item.getComponents().contains(net.minecraft.component.DataComponentTypes.WRITABLE_BOOK_CONTENT))) ||
                ((path.endsWith("book") || path.contains("book")) && !path.contains("shelf"))) {
            return BOOKS;
        }

        // 44. Pottery Sherds & Pots
        if (isSherd(path, item) || path.equals("decorated_pot") || path.equals("flower_pot")) {
            return SHERDS;
        }

        // 45. Heads & Skulls
        if ((path.endsWith("_head") || path.equals("head") || path.endsWith("_skull") || path.equals("skull") || path.contains("head") || path.contains("skull")) && !path.contains("banner")) {
            return HEADS;
        }

        // 46. Trims & Templates
        if (path.contains("trim") || path.contains("smithing_template")) {
            return TRIMS;
        }

        // 47. Shulker Boxes
        if (block instanceof net.minecraft.block.ShulkerBoxBlock || path.contains("shulker")) {
            return SHULKER_BOXES;
        }

        // 48. Scraps
        if (path.contains("scrap") || path.contains("scrape")) {
            return SCRAPS;
        }

        // 49. Buckets
        if (item instanceof net.minecraft.item.BucketItem || path.endsWith("bucket") || path.contains("bucket")) {
            return BUCKETS;
        }

        // 50. Potions
        if ((item != null && item.getComponents().contains(net.minecraft.component.DataComponentTypes.POTION_CONTENTS)) || path.contains("potion")) {
            return POTIONS;
        }

        // 51. Arrows
        if (item instanceof net.minecraft.item.ArrowItem || path.endsWith("arrow") || path.contains("arrow")) {
            return ARROWS;
        }

        // 52. Music Discs
        if ((item != null && item.getComponents().contains(net.minecraft.component.DataComponentTypes.JUKEBOX_PLAYABLE)) ||
                path.contains("music_disc") || path.contains("disc")) {
            return DISCS;
        }

        // 53. Rails
        if (block instanceof net.minecraft.block.AbstractRailBlock || path.endsWith("rail") || path.contains("rail")) {
            return RAILS;
        }

        // 54. Chests
        if (!path.contains("chestplate") && !path.contains("minecart") && (path.contains("chest") || path.contains("chess"))) {
            return CHESTS;
        }

        // 55. Minecarts
        if (path.contains("minecart")) {
            return MINECARTS;
        }

        // 56. Uncategorized Vegetation
        if (isUncategorizedVegetation(path, block)) {
            return UNCATEGORIZED_VEGETATION;
        }

        // 57. Full Cube Blocks
        if (isFullCubeBlock(item, block, path)) {
            return FULL_BLOCKS;
        }

        return MISC;
    }

    private static boolean isFlower(String path, Block block, Item item) {
        if (path.endsWith("_tulip") || path.contains("eyeblossom") || path.equals("poppy") ||
                path.equals("dandelion") || path.equals("blue_orchid") || path.equals("allium") ||
                path.equals("azure_bluet") || path.equals("oxeye_daisy") || path.equals("cornflower") ||
                path.equals("lily_of_the_valley") || path.equals("wither_rose") || path.equals("torchflower") ||
                path.equals("pitcher_plant") || path.equals("sunflower") || path.equals("lilac") ||
                path.equals("rose_bush") || path.equals("peony") || path.equals("pink_petals") ||
                path.equals("wildflowers") || path.equals("cactus_flower") || path.equals("spore_blossom") ||
                path.equals("chorus_flower")) {
            return true;
        }
        try {
            if (item != null && item.getDefaultStack().isIn(net.minecraft.registry.tag.ItemTags.FLOWERS)) {
                return true;
            }
            if (block instanceof net.minecraft.block.FlowerBlock || block instanceof net.minecraft.block.TallFlowerBlock) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean isUncategorizedVegetation(String path, Block block) {
        if (path.contains("planks") || path.contains("mosaic") || path.endsWith("_block") || path.contains("wool") || path.contains("concrete")) {
            return false;
        }
        return path.equals("short_grass") || path.equals("tall_grass") || path.equals("short_dry_grass") ||
                path.equals("tall_dry_grass") || path.equals("fern") || path.equals("large_fern") ||
                path.equals("bush") || path.equals("dead_bush") || path.equals("firefly_bush") ||
                path.equals("sugar_cane") || path.equals("bamboo") || path.equals("seagrass") ||
                path.equals("kelp") || path.equals("weeping_vines") || path.equals("twisting_vines") ||
                path.equals("vine") || path.equals("nether_sprouts") || path.equals("warped_roots") ||
                path.equals("crimson_roots") || path.equals("hanging_roots") || path.equals("pale_hanging_moss") ||
                path.equals("leaf_litter") || path.equals("resin_clump") || path.equals("lily_pad") ||
                path.equals("sea_pickle") || path.equals("cactus") || path.equals("big_dripleaf") ||
                path.equals("small_dripleaf") || path.equals("brown_mushroom") || path.equals("red_mushroom") ||
                path.equals("crimson_fungus") || path.equals("warped_fungus") || path.equals("glow_lichen") ||
                path.equals("sculk_vein") || path.equals("chorus_plant") || path.equals("mangrove_roots") ||
                path.equals("azalea") || path.equals("flowering_azalea") || path.equals("frogspawn");
    }

    private static boolean isTool(String path, Item item) {
        if (item != null) {
            try {
                if (item.getComponents().contains(net.minecraft.component.DataComponentTypes.TOOL) ||
                        item instanceof net.minecraft.item.RangedWeaponItem ||
                        item.getDefaultStack().isIn(net.minecraft.registry.tag.ItemTags.SWORDS) ||
                        item.getDefaultStack().isIn(net.minecraft.registry.tag.ItemTags.AXES) ||
                        item.getDefaultStack().isIn(net.minecraft.registry.tag.ItemTags.PICKAXES) ||
                        item.getDefaultStack().isIn(net.minecraft.registry.tag.ItemTags.SHOVELS) ||
                        item.getDefaultStack().isIn(net.minecraft.registry.tag.ItemTags.HOES)) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }
        return path.endsWith("_pickaxe") || path.equals("pickaxe") ||
                path.endsWith("_axe") || path.equals("axe") ||
                path.endsWith("_shovel") || path.equals("shovel") ||
                path.endsWith("_hoe") || path.equals("hoe") ||
                path.endsWith("_sword") || path.equals("sword") ||
                path.equals("bow") || path.equals("crossbow") ||
                path.equals("trident") || path.equals("mace") ||
                path.equals("shield") ||
                path.equals("fishing_rod") || path.equals("flint_and_steel") ||
                path.equals("shears") || path.equals("brush") ||
                path.equals("spyglass") || path.endsWith("_on_a_stick") ||
                path.contains("wrench") || path.contains("hammer");
    }

    private static boolean isArmor(String path, Item item) {
        if (item != null) {
            try {
                if (item.getComponents().contains(net.minecraft.component.DataComponentTypes.EQUIPPABLE) ||
                        item.getDefaultStack().isIn(net.minecraft.registry.tag.ItemTags.HEAD_ARMOR) ||
                        item.getDefaultStack().isIn(net.minecraft.registry.tag.ItemTags.CHEST_ARMOR) ||
                        item.getDefaultStack().isIn(net.minecraft.registry.tag.ItemTags.LEG_ARMOR) ||
                        item.getDefaultStack().isIn(net.minecraft.registry.tag.ItemTags.FOOT_ARMOR)) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }
        return path.endsWith("_helmet") || path.equals("helmet") ||
                path.endsWith("_chestplate") || path.equals("chestplate") ||
                path.endsWith("_leggings") || path.equals("leggings") ||
                path.endsWith("_boots") || path.equals("boots") ||
                path.equals("elytra") ||
                path.endsWith("_horse_armor") || path.equals("wolf_armor");
    }

    private static boolean isSherd(String path, Item item) {
        if (item != null) {
            try {
                if (item.getDefaultStack().isIn(net.minecraft.registry.tag.ItemTags.DECORATED_POT_SHERDS)) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }
        return path.endsWith("_sherd") || path.endsWith("_sherds") || path.equals("sherd") || path.contains("sherd");
    }

    private static boolean isFullCubeBlock(Item item, Block block, String path) {
        if (item != null) {
            if (!(item instanceof BlockItem)) {
                return false; // inventory item (food, ingots, dusts, etc.)
            }
            if (block != null) {
                try {
                    VoxelShape shape = block.getDefaultState().getOutlineShape(
                            EmptyBlockView.INSTANCE,
                            BlockPos.ORIGIN
                    );
                    return Block.isShapeFullCube(shape);
                } catch (Throwable ignored) {
                }
            }
            return true;
        }

        // fallback for unit test environments
        if (path.contains("apple") || path.contains("diamond") || path.contains("raw_") ||
                path.endsWith("_ingot") || path.endsWith("_nugget") || path.endsWith("_dust") ||
                path.contains("feather") || path.contains("string") || path.contains("flint") ||
                path.contains("chest") || path.contains("hopper") || path.contains("cauldron") ||
                path.contains("bottle") || path.contains("pearl") || path.contains("powder")) {
            return false;
        }
        return true;
    }
}