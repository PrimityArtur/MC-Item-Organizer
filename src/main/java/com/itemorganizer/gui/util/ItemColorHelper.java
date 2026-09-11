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
import java.util.concurrent.ConcurrentHashMap;

// visual color and gradient helper for items
public class ItemColorHelper {
    public static final String[] CATEGORY_NAMES = {
            "Blancos",
            "Gris",
            "Negro",
            "Rojo",
            "Naranja / Maderas / Tierras",
            "Amarillo / Ocres",
            "Verde",
            "Cyan",
            "Azul",
            "Púrpura",
            "Magenta",
            "Rosa"
    };

    public static final class ColorSortKey {
        public final int category;
        public final double s;
        public final long roundS;
        public final double chroma;
        public final double lightness;

        public ColorSortKey(int category, double s, double chroma, double lightness) {
            this.category = category;
            this.s = s;
            this.roundS = Math.round(s * 1000.0);
            this.chroma = chroma;
            this.lightness = lightness;
        }
    }

    private static final Map<String, ColorSortKey> KEY_CACHE = new ConcurrentHashMap<>(2500);
    private static final Map<Integer, double[][]> BEZIER_SAMPLES = new HashMap<>();
    private static final int NUM_SAMPLES = 128;
    private static final Map<String, Integer> TEXTURE_COLORS = new HashMap<>(2500);

    static {
        Map<Integer, int[]> chromaticAnchors = Map.of(
                3, new int[]{0x2D0C12, 0xDD1B1B, 0xD85A32},
                4, new int[]{0xB84C2A, 0xFF6E00, 0xE6B032},
                5, new int[]{0xE6B032, 0xFFEB0A, 0xC8E81A},
                6, new int[]{0xB8E820, 0x22B828, 0x165848},
                7, new int[]{0x165848, 0x00C8C8, 0x58B4E8},
                8, new int[]{0x58B4E8, 0x1E48D8, 0x2A1A68},
                9, new int[]{0x2A1A68, 0x8820B8, 0x9C1878},
                10, new int[]{0x9C1878, 0xD81098, 0xE03878},
                11, new int[]{0xE03878, 0xF26898, 0xF8D4DC}
        );

        for (Map.Entry<Integer, int[]> entry : chromaticAnchors.entrySet()) {
            int catId = entry.getKey();
            int[] hexes = entry.getValue();
            double[] p0 = srgbToOklab(hexes[0]);
            double[] p1 = srgbToOklab(hexes[1]);
            double[] p2 = srgbToOklab(hexes[2]);

            double[][] samples = new double[NUM_SAMPLES][4];
            for (int i = 0; i < NUM_SAMPLES; i++) {
                double s = (double) i / (NUM_SAMPLES - 1);
                double w0 = (1.0 - s) * (1.0 - s);
                double w1 = 2.0 * (1.0 - s) * s;
                double w2 = s * s;
                double bL = w0 * p0[0] + w1 * p1[0] + w2 * p2[0];
                double ba = w0 * p0[1] + w1 * p1[1] + w2 * p2[1];
                double bb = w0 * p0[2] + w1 * p1[2] + w2 * p2[2];
                samples[i] = new double[]{s, bL, ba, bb};
            }
            BEZIER_SAMPLES.put(catId, samples);
        }

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
        clearColorCache();
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
                    } else {
                        TEXTURE_COLORS.put("minecraft:" + key, color);
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
        clearColorCache();
    }

    public static Map<String, Integer> getTextureColors() {
        return java.util.Collections.unmodifiableMap(TEXTURE_COLORS);
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

    public static ColorSortKey getColorSortKey(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return new ColorSortKey(1, -0.5, 0.0, 0.5);
        }
        return KEY_CACHE.computeIfAbsent(itemId.toLowerCase(), id -> {
            int rgb = getItemColor(id);
            double[] oklab = srgbToOklab(rgb);
            double L = oklab[0];
            double a = oklab[1];
            double b = oklab[2];
            double C = Math.hypot(a, b);
            int cat = classifyColor(L, a, b);
            double s = (cat <= 2) ? -L : projectOntoBezier(L, a, b, cat);
            return new ColorSortKey(cat, s, C, L);
        });
    }

    public static void clearColorCache() {
        if (KEY_CACHE != null) {
            KEY_CACHE.clear();
        }
    }

    public static double[] srgbToOklab(int rgb) {
        int rInt = (rgb >> 16) & 0xFF;
        int gInt = (rgb >> 8) & 0xFF;
        int bInt = rgb & 0xFF;

        double r = pivotSrgb(rInt);
        double g = pivotSrgb(gInt);
        double b = pivotSrgb(bInt);

        double l_ = 0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b;
        double m_ = 0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b;
        double s_ = 0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b;

        double lRoot = l_ > 0.0 ? Math.cbrt(l_) : 0.0;
        double mRoot = m_ > 0.0 ? Math.cbrt(m_) : 0.0;
        double sRoot = s_ > 0.0 ? Math.cbrt(s_) : 0.0;

        double L = 0.2104542553 * lRoot + 0.7936177850 * mRoot - 0.0040720468 * sRoot;
        double a = 1.9779984951 * lRoot - 2.4285922050 * mRoot + 0.4505937099 * sRoot;
        double bVal = 0.0259040371 * lRoot + 0.7827717662 * mRoot - 0.8086757660 * sRoot;

        return new double[]{L, a, bVal};
    }

    public static double[] oklabToOklch(double L, double a, double b) {
        double C = Math.hypot(a, b);
        double h = Math.atan2(b, a);
        return new double[]{L, C, h};
    }

    public static double[] oklchToOklab(double L, double C, double h) {
        double a = C * Math.cos(h);
        double b = C * Math.sin(h);
        return new double[]{L, a, b};
    }

    private static double pivotSrgb(int c) {
        double v = c / 255.0;
        return v <= 0.04045 ? (v / 12.92) : Math.pow((v + 0.055) / 1.055, 2.4);
    }

    public static int classifyColor(double L, double a, double b) {
        double C = Math.hypot(a, b);
        double h = (Math.toDegrees(Math.atan2(b, a)) % 360.0 + 360.0) % 360.0;

        // 1. negro
        if (L <= 0.20) return 2;
        if (L <= 0.23 && C < 0.050) return 2;

        // 2. blanco
        if (L >= 0.85 && C < 0.040) return 0;
        if (L >= 0.90 && C < 0.065) return 0;

        // 3. gris
        if (C < 0.028) return 1;

        // 4. rosa
        boolean isPink = false;
        if (h >= 325.0 || h < 30.0) {
            if (L >= 0.50 && b <= 0.055 && C >= 0.030) {
                isPink = true;
            } else if (L >= 0.62 && C < 0.15) {
                isPink = true;
            }
        }
        if (isPink) return 11;

        // 5. familias cromaticas
        if (345.0 <= h || h < 36.0) return 3;
        else if (h < 78.0) return 4;
        else if (h < 112.0) return 5;
        else if (h < 178.0) return 6;
        else if (h < 218.0) return 7;
        else if (h < 282.0) return 8;
        else if (h < 318.0) return 9;
        else return 10;
    }

    public static double projectOntoBezier(double L, double a, double b, int catId) {
        double[][] samples = BEZIER_SAMPLES.get(catId);
        if (samples == null) return 0.0;

        double bestS = 0.0;
        double minDistSq = Double.POSITIVE_INFINITY;
        for (double[] pt : samples) {
            double dL = L - pt[1];
            double da = a - pt[2];
            double db = b - pt[3];
            double distSq = dL * dL + da * da + db * db;
            if (distSq < minDistSq) {
                minDistSq = distSq;
                bestS = pt[0];
            }
        }
        return bestS;
    }

    // groups by item category first, then sorts by color gradient
    public static java.util.Comparator<String> getColorComparator() {
        return (id1, id2) -> {
            if (id1 == null && id2 == null) return 0;
            if (id1 == null) return 1;
            if (id2 == null) return -1;
            if (id1.equals(id2)) return 0;

            int cat1 = getItemCategoryOrder(id1);
            int cat2 = getItemCategoryOrder(id2);
            if (cat1 != cat2) {
                return Integer.compare(cat1, cat2);
            }

            return compareItemColors(id1, id2);
        };
    }

    public static java.util.Comparator<String> getCategoryColorComparator() {
        return getColorComparator();
    }

    public static java.util.Comparator<String> getPureColorComparator() {
        return ItemColorHelper::compareItemColors;
    }

    public static int compareItemColors(String id1, String id2) {
        if (id1 == null && id2 == null) return 0;
        if (id1 == null) return 1;
        if (id2 == null) return -1;
        if (id1.equals(id2)) return 0;

        ColorSortKey k1 = getColorSortKey(id1);
        ColorSortKey k2 = getColorSortKey(id2);

        if (k1.category != k2.category) {
            return Integer.compare(k1.category, k2.category);
        }

        if (k1.category <= 2) {
            int cmpS = Double.compare(k1.s, k2.s);
            if (cmpS != 0) return cmpS;
        } else {
            int cmpRound = Long.compare(k1.roundS, k2.roundS);
            if (cmpRound != 0) return cmpRound;
        }

        int cmpC = Double.compare(k2.chroma, k1.chroma);
        if (cmpC != 0) return cmpC;

        int cmpL = Double.compare(k2.lightness, k1.lightness);
        if (cmpL != 0) return cmpL;

        return id1.compareTo(id2);
    }

    public static boolean isNeutral(float[] hsv) {
        return getColorBand(hsv) <= 2;
    }

    public static boolean isNeutral(String itemId) {
        return getColorSortKey(itemId).category <= 2;
    }

    public static int getColorBand(float[] hsv) {
        float h = hsv[0] * 360.0f;
        float s = hsv[1];
        float v = hsv[2];

        if (v < 0.18f || (s < 0.30f && v < 0.22f)) {
            return 2;
        }
        if (s < 0.16f && v >= 0.75f) {
            return 0;
        }
        if (s < 0.20f || (s < 0.35f && v < 0.30f)) {
            return 1;
        }

        if (h >= 345.0f || h < 15.0f) {
            return 3;
        }
        if (h < 38.0f) {
            return 4;
        }
        if (h < 68.0f) {
            return 5;
        }
        if (h < 105.0f) {
            return v >= 0.55f ? 6 : 7;
        }
        if (h < 165.0f) {
            return 7;
        }
        if (h < 195.0f) {
            return 8;
        }
        if (h < 255.0f) {
            return 9;
        }
        if (h < 285.0f) {
            return 10;
        }
        if (h < 325.0f) {
            return 11;
        }
        return 12;
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

        ColorSortKey k1 = getColorSortKey(itemId1);
        ColorSortKey k2 = getColorSortKey(itemId2);

        int rgb1 = getItemColor(itemId1);
        int rgb2 = getItemColor(itemId2);
        if (rgb1 == rgb2) return true;

        double[] ok1 = srgbToOklab(rgb1);
        double[] ok2 = srgbToOklab(rgb2);
        double dL = ok1[0] - ok2[0];
        double da = ok1[1] - ok2[1];
        double db = ok1[2] - ok2[2];
        double deltaE = Math.sqrt(dL * dL + da * da + db * db);

        if (deltaE < 0.16) return true;
        if (k1.category == k2.category && Math.abs(k1.s - k2.s) < 0.12 && Math.abs(k1.lightness - k2.lightness) < 0.20) {
            return true;
        }

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
