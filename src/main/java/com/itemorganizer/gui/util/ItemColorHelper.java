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
        try (com.google.gson.stream.JsonReader jsonReader = new com.google.gson.stream.JsonReader(reader)) {
            jsonReader.setLenient(true);
            jsonReader.beginObject();
            while (jsonReader.hasNext()) {
                String key = jsonReader.nextName().toLowerCase();
                String hex = jsonReader.nextString();
                try {
                    if (hex.startsWith("0x") || hex.startsWith("0X")) {
                        hex = hex.substring(2);
                    } else if (hex.startsWith("#")) {
                        hex = hex.substring(1);
                    }
                    int color = (int) Long.parseLong(hex, 16);
                    TEXTURE_COLORS.put(key, color);
                    if (key.startsWith("minecraft:")) {
                        TEXTURE_COLORS.put(key.substring("minecraft:".length()), color);
                    }
                } catch (Exception ignored) {
                }
            }
            jsonReader.endObject();
        } catch (Exception ignored) {
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

        if (key.contains("test_block")) {
            if (key.contains("start")) return 0x29A5D6;
            if (key.contains("log")) return 0xE8A825;
            if (key.contains("fail")) return 0xDE3226;
            if (key.contains("accept")) return 0x38BD4C;
        }

        String cleanId = key;
        int bracketIndex = cleanId.indexOf('[');
        if (bracketIndex != -1) {
            cleanId = cleanId.substring(0, bracketIndex).trim();
            Integer cleanCol = TEXTURE_COLORS.get(cleanId);
            if (cleanCol != null) {
                return cleanCol;
            }
        }

        Identifier id = Identifier.tryParse(cleanId);
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
        return com.itemorganizer.core.model.ItemCategory.getOrder(itemId);
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
            return compareItemColors(id1, id2);
        };
    }

    public static java.util.Comparator<String> getPureColorComparator() {
        return ItemColorHelper::compareItemColors;
    }

    public static int compareItemColors(String id1, String id2) {
        if (id1 == null && id2 == null) return 0;
        if (id1 == null) return 1;
        if (id2 == null) return -1;
        if (id1.equals(id2)) return 0;

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
            int cmpV = Float.compare(hsv2[2], hsv1[2]);
            if (cmpV != 0) return cmpV;

            int cmpS = Float.compare(hsv1[1], hsv2[1]);
            if (cmpS != 0) return cmpS;
        } else {
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
    }

    public static double getColorDistance(int rgb1, int rgb2) {
        int r1 = (rgb1 >> 16) & 0xFF;
        int g1 = (rgb1 >> 8) & 0xFF;
        int b1 = rgb1 & 0xFF;
        int r2 = (rgb2 >> 16) & 0xFF;
        int g2 = (rgb2 >> 8) & 0xFF;
        int b2 = rgb2 & 0xFF;
        long rmean = (r1 + r2) / 2;
        long r = r1 - r2;
        long g = g1 - g2;
        long b = b1 - b2;
        return Math.sqrt((((512 + rmean) * r * r) >> 8) + 4 * g * g + (((767 - rmean) * b * b) >> 8));
    }

    public static boolean isSimilarColor(String itemId1, String itemId2) {
        if (itemId1 == null || itemId2 == null) return false;
        if (itemId1.equalsIgnoreCase(itemId2)) return true;

        int rgb1 = getItemColor(itemId1);
        int rgb2 = getItemColor(itemId2);
        if (rgb1 == rgb2) return true;

        float[] hsv1 = rgbToHsv(rgb1);
        float[] hsv2 = rgbToHsv(rgb2);

        boolean neutral1 = isNeutral(hsv1);
        boolean neutral2 = isNeutral(hsv2);
        if (neutral1 != neutral2) return false;

        if (neutral1) {
            return Math.abs(hsv1[2] - hsv2[2]) < 0.30f;
        }

        float hueDiff = Math.abs(hsv1[0] - hsv2[0]);
        if (hueDiff > 0.5f) hueDiff = 1.0f - hueDiff;
        float hueAngleDiff = hueDiff * 360.0f;

        if (hueAngleDiff > 35.0f) {
            return false;
        }

        return getColorDistance(rgb1, rgb2) < 115.0;
    }
}
