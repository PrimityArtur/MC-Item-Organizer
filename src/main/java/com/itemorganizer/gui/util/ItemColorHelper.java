package com.itemorganizer.gui.util;

import com.google.gson.reflect.TypeToken;
import com.itemorganizer.storage.JsonHelper;
import com.itemorganizer.storage.StorageManager;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.EmptyBlockView;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

// visual color and gradient helper for items
public class ItemColorHelper {
    private static final Map<String, Integer> TEXTURE_COLORS = new HashMap<>(2500);

    static {
        try {
            net.minecraft.SharedConstants.createGameVersion();
            net.minecraft.Bootstrap.initialize();
        } catch (Throwable ignored) {
        }
        loadDefaultAndConfigColors();
    }

    public static synchronized void loadDefaultAndConfigColors() {
        // 1. load bundled resource
        try (InputStream is = ItemColorHelper.class.getResourceAsStream("/assets/item-organizer/texture_colors.json")) {
            if (is != null) {
                loadColorsFromReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {
        }

        // 2. load local config file
        try {
            java.nio.file.Path configFile = StorageManager.getInstance().getBaseDir().resolve("texture_colors.json");
            if (java.nio.file.Files.exists(configFile)) {
                try (Reader reader = java.nio.file.Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                    loadColorsFromReader(reader);
                }
            } else if (!TEXTURE_COLORS.isEmpty()) {
                // save initial copy to config for user customization
                TextureColorAnalyzer.saveColorsToConfigFile(TEXTURE_COLORS);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void loadColorsFromReader(Reader reader) {
        Type mapType = new TypeToken<Map<String, String>>() {}.getType();
        Map<String, String> rawMap = JsonHelper.GSON.fromJson(reader, mapType);
        if (rawMap != null) {
            for (Map.Entry<String, String> entry : rawMap.entrySet()) {
                try {
                    String hex = entry.getValue();
                    if (hex.startsWith("0x") || hex.startsWith("0X")) {
                        hex = hex.substring(2);
                    } else if (hex.startsWith("#")) {
                        hex = hex.substring(1);
                    }
                    int color = (int) Long.parseLong(hex, 16);
                    String key = entry.getKey().toLowerCase();
                    TEXTURE_COLORS.put(key, color);
                    if (key.startsWith("minecraft:")) {
                        TEXTURE_COLORS.put(key.substring("minecraft:".length()), color);
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    public static boolean hasExactTextureColor(String itemId) {
        if (itemId == null || itemId.isEmpty()) return false;
        String key = itemId.toLowerCase();
        if (TEXTURE_COLORS.containsKey(key)) return true;
        if (key.startsWith("minecraft:")) {
            return TEXTURE_COLORS.containsKey(key.substring("minecraft:".length()));
        }
        return false;
    }

    public static void registerColors(Map<String, Integer> colors) {
        if (colors == null || colors.isEmpty()) return;
        synchronized (TEXTURE_COLORS) {
            for (Map.Entry<String, Integer> entry : colors.entrySet()) {
                String k = entry.getKey().toLowerCase();
                TEXTURE_COLORS.put(k, entry.getValue());
                if (k.startsWith("minecraft:")) {
                    TEXTURE_COLORS.put(k.substring("minecraft:".length()), entry.getValue());
                }
            }
        }
    }

    // get visual color from texture, dye, or map color fallback
    public static int getItemColor(String itemId) {
        if (itemId == null || itemId.isEmpty()) return 0x888888;
        String key = itemId.toLowerCase();
        Integer col = TEXTURE_COLORS.get(key);
        if (col != null) {
            return col;
        }

        Identifier id = Identifier.tryParse(itemId);
        if (id != null) {
            Integer colPath = TEXTURE_COLORS.get(id.getPath());
            if (colPath != null) {
                return colPath;
            }

            try {
                Item item = Registries.ITEM.get(id);
                if (item != null) {
                    if (item instanceof DyeItem dyeItem) {
                        int c = dyeItem.getColor().getEntityColor();
                        TEXTURE_COLORS.put(key, c);
                        TEXTURE_COLORS.put(id.getPath(), c);
                        return c;
                    }

                    if (item instanceof BlockItem blockItem) {
                        Block block = blockItem.getBlock();
                        MapColor mc = block.getDefaultMapColor();
                        if (mc != null && mc.color != 0) {
                            TEXTURE_COLORS.put(key, mc.color);
                            TEXTURE_COLORS.put(id.getPath(), mc.color);
                            return mc.color;
                        }
                    }
                }
            } catch (Throwable ignored) {
                // environment without initialized registries (e.g. pure unit tests)
            }
        }

        return 0x888888;
    }


    // returns category order index for sorting
    public static int getItemCategoryOrder(String itemId) {
        if (itemId == null || itemId.isEmpty()) return 1;
        Identifier id = Identifier.tryParse(itemId);
        String path = (id != null ? id.getPath() : itemId).toLowerCase();

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

        // 2. glass blocks
        if (!path.contains("pane") && !path.contains("plane") && !path.equals("spyglass") && !path.contains("bottle")) {
            if (block instanceof net.minecraft.block.StainedGlassBlock ||
                    path.endsWith("glass") || path.endsWith("_glass") || path.contains("stained_glass")) {
                if (!(block instanceof net.minecraft.block.PaneBlock)) {
                    return 2;
                }
            }
        }

        // 3. glass panes
        if (!path.contains("bars") && !path.endsWith("_bars") && !path.equals("bars")) {
            if (path.endsWith("glass_pane") || path.endsWith("glass_plane") || path.endsWith("_pane") || path.contains("glass_pane") ||
                    (path.contains("glass") && (path.contains("pane") || path.contains("plane")))) {
                return 3;
            }
            if (block instanceof net.minecraft.block.PaneBlock) {
                return 3;
            }
        }

        // 4. bars
        if (path.endsWith("_bars") || path.equals("bars") || path.contains("bars")) {
            return 4;
        }

        // 5. chains
        if (path.endsWith("chain") || path.endsWith("chains") || path.equals("chain")) {
            return 5;
        }

        // 9. fence gates
        if (block instanceof net.minecraft.block.FenceGateBlock ||
                path.endsWith("_fence_gate") || path.endsWith("_gate") || path.equals("fence_gate") || path.equals("gate")) {
            return 9;
        }

        // 6. fences
        if (block instanceof net.minecraft.block.FenceBlock ||
                path.endsWith("_fence") || path.equals("fence")) {
            return 6;
        }

        // 7. walls
        if (block instanceof net.minecraft.block.WallBlock ||
                ((path.endsWith("_wall") || path.equals("wall")) && !path.contains("torch") && !path.contains("sign") && !path.contains("banner") && !path.contains("fan"))) {
            return 7;
        }

        // 8. slabs
        if (block instanceof net.minecraft.block.SlabBlock ||
                path.endsWith("_slab") || path.equals("slab")) {
            return 8;
        }

        // 10. stairs
        if (block instanceof net.minecraft.block.StairsBlock ||
                path.endsWith("_stairs") || path.equals("stairs") || path.equals("ladder")) {
            return 10;
        }

        // 11. saplings
        if (block instanceof net.minecraft.block.SaplingBlock ||
                path.endsWith("_sapling") || path.equals("sapling") || path.endsWith("_propagule") || path.equals("mangrove_propagule")) {
            return 11;
        }
        // 12. leaves
        if (block instanceof net.minecraft.block.LeavesBlock ||
                path.endsWith("_leaves") || path.equals("leaves")) {
            return 12;
        }
        // 13. carpets
        if (block instanceof net.minecraft.block.CarpetBlock ||
                path.endsWith("_carpet") || path.equals("carpet")) {
            return 13;
        }
        // 14. lanterns
        if (block instanceof net.minecraft.block.LanternBlock ||
                path.endsWith("lantern") || path.equals("lantern")) {
            return 14;
        }
        // 15. shelves and bookshelves
        if (path.endsWith("bookshelf") || path.endsWith("shelf") || path.contains("shelf")) {
            return 15;
        }
        // 16. signs and hanging signs
        if (block instanceof net.minecraft.block.AbstractSignBlock ||
                path.endsWith("_sign") || path.equals("sign") || path.endsWith("_hanging_sign") || path.equals("hanging_sign") || path.contains("hanging_sign")) {
            return 16;
        }
        // 18. trapdoors
        if (block instanceof net.minecraft.block.TrapdoorBlock ||
                path.endsWith("_trapdoor") || path.equals("trapdoor")) {
            return 18;
        }
        // 17. doors
        if (block instanceof net.minecraft.block.DoorBlock ||
                path.endsWith("_door") || path.equals("door")) {
            return 17;
        }
        // 19. pressure plates
        if (block instanceof net.minecraft.block.PressurePlateBlock ||
                path.endsWith("_pressure_plate") || path.equals("pressure_plate")) {
            return 19;
        }
        // 20. beds
        if (block instanceof net.minecraft.block.BedBlock ||
                ((path.endsWith("_bed") || path.equals("bed")) && !path.contains("bedrock"))) {
            return 20;
        }
        // 21. boats and rafts
        if (item instanceof net.minecraft.item.BoatItem ||
                path.endsWith("_boat") || path.equals("boat") || path.endsWith("_raft") || path.equals("raft")) {
            return 21;
        }
        // 22. buttons
        if (block instanceof net.minecraft.block.ButtonBlock ||
                path.endsWith("_button") || path.equals("button")) {
            return 22;
        }
        // 23. dyes
        if (item instanceof net.minecraft.item.DyeItem ||
                path.endsWith("_dye") || path.equals("dye")) {
            return 23;
        }
        // 47. golems
        if (path.contains("golem")) {
            return 47;
        }
        // 24. spawn eggs
        if (item instanceof net.minecraft.item.SpawnEggItem ||
                path.endsWith("_egg") || path.equals("egg")) {
            return 24;
        }
        // 25. candles
        if (block instanceof net.minecraft.block.CandleBlock ||
                path.endsWith("_candle") || path.equals("candle") || path.contains("candle")) {
            return 25;
        }
        // 26. banners
        if (block instanceof net.minecraft.block.BannerBlock ||
                path.endsWith("_banner") || path.equals("banner") || path.contains("banner")) {
            return 26;
        }
        // 27. corals
        if (!path.contains("coral_block") && !(block instanceof net.minecraft.block.CoralBlockBlock)) {
            if (block instanceof net.minecraft.block.CoralBlock ||
                    block instanceof net.minecraft.block.CoralFanBlock ||
                    block instanceof net.minecraft.block.CoralWallFanBlock ||
                    path.contains("coral")) {
                return 27;
            }
        }
        // 28. harness and saddles
        if (path.contains("harness") || path.equals("saddle") || path.equals("lead")) {
            return 28;
        }
        // 29. tools
        if (isTool(path, item)) {
            return 29;
        }
        // 30. armor
        if (isArmor(path, item)) {
            return 30;
        }
        // 31. rods
        if ((path.endsWith("_rod") || path.equals("rod")) && !path.equals("fishing_rod") && !path.endsWith("_on_a_stick")) {
            return 31;
        }
        // 32. bundles
        if ((item != null && item.getComponents().contains(net.minecraft.component.DataComponentTypes.BUNDLE_CONTENTS)) ||
                path.endsWith("_bundle") || path.equals("bundle") || path.contains("bundle")) {
            return 32;
        }
        // 33. books
        if ((item != null && (item.getComponents().contains(net.minecraft.component.DataComponentTypes.WRITTEN_BOOK_CONTENT) ||
                item.getComponents().contains(net.minecraft.component.DataComponentTypes.WRITABLE_BOOK_CONTENT))) ||
                ((path.endsWith("book") || path.contains("book")) && !path.contains("shelf"))) {
            return 33;
        }
        // 44. sherds
        if (isSherd(path, item)) {
            return 44;
        }
        // 34. heads and skulls
        if ((path.endsWith("_head") || path.equals("head") || path.endsWith("_skull") || path.equals("skull") || path.contains("head") || path.contains("skull")) && !path.contains("banner")) {
            return 34;
        }
        // 35. trims and templates
        if (path.contains("trim") || path.contains("smithing_template")) {
            return 35;
        }
        // 36. shulker boxes and shells
        if (block instanceof net.minecraft.block.ShulkerBoxBlock || path.contains("shulker")) {
            return 36;
        }
        // 37. scraps
        if (path.contains("scrap") || path.contains("scrape")) {
            return 37;
        }
        // 38. buckets
        if (item instanceof net.minecraft.item.BucketItem || path.endsWith("bucket") || path.contains("bucket")) {
            return 38;
        }
        // 39. potions
        if ((item != null && item.getComponents().contains(net.minecraft.component.DataComponentTypes.POTION_CONTENTS)) || path.contains("potion")) {
            return 39;
        }
        // 40. arrows
        if (item instanceof net.minecraft.item.ArrowItem || path.endsWith("arrow") || path.contains("arrow")) {
            return 40;
        }
        // 41. music discs
        if ((item != null && item.getComponents().contains(net.minecraft.component.DataComponentTypes.JUKEBOX_PLAYABLE)) ||
                path.contains("music_disc") || path.contains("disc")) {
            return 41;
        }
        // 42. rails
        if (block instanceof net.minecraft.block.AbstractRailBlock || path.endsWith("rail") || path.contains("rail")) {
            return 42;
        }
        // 43. anvils
        if (block instanceof net.minecraft.block.AnvilBlock || path.endsWith("anvil") || path.contains("anvil")) {
            return 43;
        }

        // 46. minecarts
        if (path.contains("minecart")) {
            return 46;
        }

        // 45. chests
        if (!path.contains("chestplate") && !path.contains("minecart") && (path.contains("chest") || path.contains("chess"))) {
            return 45;
        }

        // 48. non-cube items and partial blocks
        if (!isFullCubeBlock(item, block, path)) {
            return 48;
        }

        // 1. standard full cube blocks
        return 1;
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

    public static float[] rgbToHsv(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255.0f;
        float g = ((rgb >> 8) & 0xFF) / 255.0f;
        float b = (rgb & 0xFF) / 255.0f;

        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        float h = 0.0f;
        if (delta > 0.0001f) {
            if (max == r) {
                h = (g - b) / delta;
                if (h < 0) h += 6.0f;
            } else if (max == g) {
                h = ((b - r) / delta) + 2.0f;
            } else {
                h = ((r - g) / delta) + 4.0f;
            }
            h /= 6.0f; // 0.0 to 1.0
        }

        float s = (max <= 0.0001f) ? 0.0f : (delta / max);
        float v = max;

        return new float[]{h, s, v};
    }

    // check if hsv color is neutral (white, gray, black)
    public static boolean isNeutral(float[] hsv) {
        float s = hsv[1];
        float v = hsv[2];
        return (s < 0.15f) || (v < 0.14f) || (s < 0.22f && v < 0.25f);
    }

    // continuous gradient comparator based on visual texture color
    public static java.util.Comparator<String> getColorComparator() {
        return (id1, id2) -> {
            if (id1 == null && id2 == null) return 0;
            if (id1 == null) return 1;
            if (id2 == null) return -1;
            if (id1.equals(id2)) return 0;

            // 1. primary: category
            int cat1 = getItemCategoryOrder(id1);
            int cat2 = getItemCategoryOrder(id2);
            if (cat1 != cat2) {
                return Integer.compare(cat1, cat2);
            }

            // 2. secondary: visual texture color
            int rgb1 = getItemColor(id1);
            int rgb2 = getItemColor(id2);

            float[] hsv1 = rgbToHsv(rgb1);
            float[] hsv2 = rgbToHsv(rgb2);

            boolean neutral1 = isNeutral(hsv1);
            boolean neutral2 = isNeutral(hsv2);

            if (neutral1 != neutral2) {
                return neutral1 ? -1 : 1;
            }

            if (neutral1) {
                // neutrals: light to dark
                int cmpV = Float.compare(hsv2[2], hsv1[2]);
                if (cmpV != 0) return cmpV;

                int cmpS = Float.compare(hsv1[1], hsv2[1]);
                if (cmpS != 0) return cmpS;
            } else {
                // chromatic: continuous rainbow starting with red at 345 deg
                float shiftedH1 = (hsv1[0] * 360.0f - 345.0f + 360.0f) % 360.0f;
                float shiftedH2 = (hsv2[0] * 360.0f - 345.0f + 360.0f) % 360.0f;

                int cmpH = Float.compare(shiftedH1, shiftedH2);
                if (cmpH != 0) return cmpH;

                int cmpV = Float.compare(hsv2[2], hsv1[2]);
                if (cmpV != 0) return cmpV;

                int cmpS = Float.compare(hsv2[1], hsv1[1]);
                if (cmpS != 0) return cmpS;
            }

            return id1.compareTo(id2);
        };
    }
}
