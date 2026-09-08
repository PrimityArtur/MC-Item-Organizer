package com.itemorganizer.gui.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.itemorganizer.ItemOrganizer;
import com.itemorganizer.storage.JsonHelper;
import com.itemorganizer.storage.StorageManager;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

// texture analyzer extracting prominent colors from textures
public class TextureColorAnalyzer {
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

    // biome tint fallbacks for grayscale textures
    private static final Map<String, Integer> HARDCODED_BIOME_TINTS = new HashMap<>();

    static {
        HARDCODED_BIOME_TINTS.put("grass_block", 0x5C802A);
        HARDCODED_BIOME_TINTS.put("short_grass", 0x5C802A);
        HARDCODED_BIOME_TINTS.put("tall_grass", 0x5C802A);
        HARDCODED_BIOME_TINTS.put("fern", 0x5C802A);
        HARDCODED_BIOME_TINTS.put("large_fern", 0x5C802A);
        HARDCODED_BIOME_TINTS.put("sugar_cane", 0x88BB67);
        HARDCODED_BIOME_TINTS.put("lily_pad", 0x208030);
        HARDCODED_BIOME_TINTS.put("vine", 0x4A6B22);
        HARDCODED_BIOME_TINTS.put("oak_leaves", 0x4A6B22);
        HARDCODED_BIOME_TINTS.put("jungle_leaves", 0x4A6B22);
        HARDCODED_BIOME_TINTS.put("acacia_leaves", 0x4A6B22);
        HARDCODED_BIOME_TINTS.put("dark_oak_leaves", 0x4A6B22);
        HARDCODED_BIOME_TINTS.put("mangrove_leaves", 0x4A6B22);
        HARDCODED_BIOME_TINTS.put("water", 0x3F76E4);
        HARDCODED_BIOME_TINTS.put("water_bucket", 0x3F76E4);
        HARDCODED_BIOME_TINTS.put("redstone", 0xE61414);
        HARDCODED_BIOME_TINTS.put("redstone_wire", 0xE61414);
    }

    public static boolean isRunning() {
        return RUNNING.get();
    }

    // background async scan to analyze textures for items without registered colors
    public static CompletableFuture<Integer> analyzeMissingItemsAsync(boolean forceAll, Consumer<Integer> onComplete) {
        if (RUNNING.compareAndSet(false, true)) {
            return CompletableFuture.supplyAsync(() -> {
                int resolvedCount = 0;
                try {
                    resolvedCount = analyzeMissingItemsInternal(forceAll);
                } catch (Throwable t) {
                    ItemOrganizer.LOGGER.error("error during background texture analysis: ", t);
                } finally {
                    RUNNING.set(false);
                    if (onComplete != null) {
                        try {
                            onComplete.accept(resolvedCount);
                        } catch (Throwable ignored) {
                        }
                    }
                }
                return resolvedCount;
            });
        }
        return CompletableFuture.completedFuture(0);
    }

    private static int analyzeMissingItemsInternal(boolean forceAll) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return 0;
        ResourceManager resourceManager = client.getResourceManager();
        if (resourceManager == null) return 0;

        List<Identifier> itemsToAnalyze = new ArrayList<>();
        try {
            for (Identifier id : Registries.ITEM.getIds()) {
                if (forceAll || !ItemColorHelper.hasExactTextureColor(id.toString())) {
                    itemsToAnalyze.add(id);
                }
            }
        } catch (Throwable t) {
            ItemOrganizer.LOGGER.warn("could not retrieve item ids from registry: {}", t.getMessage());
            return 0;
        }

        if (itemsToAnalyze.isEmpty()) {
            return 0;
        }

        ItemOrganizer.LOGGER.info("analyzing textures for {} missing items...", itemsToAnalyze.size());

        Map<String, Integer> newlyDiscovered = new HashMap<>();
        for (Identifier id : itemsToAnalyze) {
            try {
                int color = analyzeSingleItem(resourceManager, id);
                if (color != 0) {
                    newlyDiscovered.put(id.toString(), color);
                    newlyDiscovered.put(id.getPath(), color);
                }
            } catch (Exception ignored) {
            }
        }

        if (!newlyDiscovered.isEmpty()) {
            ItemColorHelper.registerColors(newlyDiscovered);
            saveColorsToConfigFile(newlyDiscovered);
            ItemOrganizer.LOGGER.info("analyzed and saved {} new texture colors", newlyDiscovered.size() / 2);
        }

        return newlyDiscovered.size() / 2;
    }

    // analyze single item texture using minecraft resource manager
    public static int analyzeSingleItem(ResourceManager resourceManager, Identifier id) {
        String path = id.getPath();

        // 1. check dynamic biome tint
        Integer biomeTint = HARDCODED_BIOME_TINTS.get(path);
        if (biomeTint != null) {
            return biomeTint;
        }

        // 2. dye items use official dye color
        try {
            Item item = Registries.ITEM.get(id);
            if (item instanceof DyeItem dye) {
                return dye.getColor().getEntityColor();
            }
        } catch (Throwable ignored) {
        }

        // 3. resolve texture using model definitions
        Identifier textureId = resolveTextureIdentifier(resourceManager, id);
        if (textureId != null) {
            Optional<Resource> res = resourceManager.getResource(textureId);
            if (res.isPresent()) {
                try (InputStream is = res.get().getInputStream()) {
                    int c = extractProminentColor(is);
                    if (c != 0) {
                        return c;
                    }
                } catch (Exception ignored) {
                }
            }
        }

        // 4. block map color fallback
        try {
            Item item = Registries.ITEM.get(id);
            if (item instanceof BlockItem blockItem) {
                Block block = blockItem.getBlock();
                MapColor mc = block.getDefaultMapColor();
                if (mc != null && mc.color != 0) {
                    return mc.color;
                }
            }
        } catch (Throwable ignored) {
        }

        return 0x888888;
    }

    // resolve png texture identifier for item id from models
    public static Identifier resolveTextureIdentifier(ResourceManager rm, Identifier id) {
        String ns = id.getNamespace();
        String path = id.getPath();

        // 1. direct item texture: textures/item/<path>.png
        Identifier candItem = Identifier.of(ns, "textures/item/" + path + ".png");
        if (rm.getResource(candItem).isPresent()) return candItem;

        // 2. direct block texture: textures/block/<path>.png
        Identifier candBlock = Identifier.of(ns, "textures/block/" + path + ".png");
        if (rm.getResource(candBlock).isPresent()) return candBlock;

        // 3. 1.21 item model definition: assets/<ns>/items/<path>.json
        Identifier itemDef = Identifier.of(ns, "items/" + path + ".json");
        Optional<Resource> itemDefRes = rm.getResource(itemDef);
        if (itemDefRes.isPresent()) {
            Identifier tex = extractTextureFromItemDefJson(rm, itemDefRes.get());
            if (tex != null && rm.getResource(tex).isPresent()) return tex;
        }

        // 4. legacy item model: assets/<ns>/models/item/<path>.json
        Identifier itemModel = Identifier.of(ns, "models/item/" + path + ".json");
        Optional<Resource> itemModelRes = rm.getResource(itemModel);
        if (itemModelRes.isPresent()) {
            Identifier tex = extractTextureFromModelJson(rm, itemModelRes.get(), 0);
            if (tex != null && rm.getResource(tex).isPresent()) return tex;
        }

        // 5. block model: assets/<ns>/models/block/<path>.json
        Identifier blockModel = Identifier.of(ns, "models/block/" + path + ".json");
        Optional<Resource> blockModelRes = rm.getResource(blockModel);
        if (blockModelRes.isPresent()) {
            Identifier tex = extractTextureFromModelJson(rm, blockModelRes.get(), 0);
            if (tex != null && rm.getResource(tex).isPresent()) return tex;
        }

        return null;
    }

    private static Identifier extractTextureFromItemDefJson(ResourceManager rm, Resource res) {
        try (Reader r = new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(r).getAsJsonObject();
            if (json.has("model")) {
                JsonObject modelObj = json.getAsJsonObject("model");
                if (modelObj.has("model")) {
                    String modelPath = modelObj.get("model").getAsString();
                    return resolveModelPathToTexture(rm, modelPath);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Identifier extractTextureFromModelJson(ResourceManager rm, Resource res, int depth) {
        if (depth > 4) return null;
        try (Reader r = new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(r).getAsJsonObject();
            if (json.has("textures")) {
                JsonObject textures = json.getAsJsonObject("textures");
                String[] priorityKeys = new String[]{"layer0", "all", "texture", "top", "front", "side", "wall", "bottom", "particle"};
                for (String k : priorityKeys) {
                    if (textures.has(k)) {
                        String val = textures.get(k).getAsString();
                        Identifier tex = parseTextureRef(val);
                        if (tex != null && rm.getResource(tex).isPresent()) return tex;
                    }
                }
                for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
                    String val = entry.getValue().getAsString();
                    if (!val.startsWith("#")) {
                        Identifier tex = parseTextureRef(val);
                        if (tex != null && rm.getResource(tex).isPresent()) return tex;
                    }
                }
            }

            if (json.has("parent")) {
                String parent = json.get("parent").getAsString();
                Identifier parentId = Identifier.tryParse(parent.contains(":") ? parent : "minecraft:" + parent);
                if (parentId != null) {
                    Identifier parentResId = Identifier.of(parentId.getNamespace(), "models/" + parentId.getPath() + ".json");
                    Optional<Resource> pRes = rm.getResource(parentResId);
                    if (pRes.isPresent()) {
                        return extractTextureFromModelJson(rm, pRes.get(), depth + 1);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Identifier resolveModelPathToTexture(ResourceManager rm, String modelPath) {
        Identifier modelId = Identifier.tryParse(modelPath.contains(":") ? modelPath : "minecraft:" + modelPath);
        if (modelId == null) return null;

        Identifier modelResId = Identifier.of(modelId.getNamespace(), "models/" + modelId.getPath() + ".json");
        Optional<Resource> res = rm.getResource(modelResId);
        if (res.isPresent()) {
            return extractTextureFromModelJson(rm, res.get(), 0);
        }
        return null;
    }

    private static Identifier parseTextureRef(String val) {
        if (val == null || val.isEmpty() || val.startsWith("#")) return null;
        Identifier id = Identifier.tryParse(val.contains(":") ? val : "minecraft:" + val);
        if (id == null) return null;
        return Identifier.of(id.getNamespace(), "textures/" + id.getPath() + ".png");
    }

    // extract prominent visual color from png texture
    public static int extractProminentColor(InputStream is) {
        return extractProminentColor(is, null);
    }

    // cluster-based prominent color extraction ignoring alpha < 128
    public static int extractProminentColor(InputStream is, String unusedItemPath) {
        try {
            BufferedImage img = ImageIO.read(is);
            if (img == null) return 0x888888;

            int w = img.getWidth();
            int h = img.getHeight();

            // take first square frame if vertical animated strip
            if (h > w) {
                img = img.getSubimage(0, 0, w, w);
                h = w;
            }

            // 1. collect visible pixels ignoring alpha < 128
            List<float[]> visiblePixels = new ArrayList<>(w * h);
            float maxS = 0.0f;

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int argb = img.getRGB(x, y);
                    int a = (argb >> 24) & 0xFF;
                    if (a < 128) continue; // ignore transparent pixels

                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;

                    float[] hsv = ItemColorHelper.rgbToHsv((r << 16) | (g << 8) | b);
                    if (hsv[1] > maxS) {
                        maxS = hsv[1];
                    }
                    // r, g, b, h, s, v, y
                    visiblePixels.add(new float[]{r, g, b, hsv[0], hsv[1], hsv[2], (float) y});
                }
            }

            if (visiblePixels.isEmpty()) return 0x888888;

            // 2. purely achromatic fallback
            if (maxS < 0.18f) {
                double rSum = 0, gSum = 0, bSum = 0;
                for (float[] p : visiblePixels) {
                    rSum += p[0];
                    gSum += p[1];
                    bSum += p[2];
                }
                int rAvg = (int) Math.round(rSum / visiblePixels.size());
                int gAvg = (int) Math.round(gSum / visiblePixels.size());
                int bAvg = (int) Math.round(bSum / visiblePixels.size());
                return (rAvg << 16) | (gAvg << 8) | bAvg;
            }

            // 3. group chromatic pixels into 12 hue sectors by 2 brightness tiers
            Map<Integer, List<float[]>> clusters = new HashMap<>();
            for (float[] p : visiblePixels) {
                float s = p[4];
                float v = p[5];
                if (v < 0.14f) continue; // ignore dark outline edges

                int key;
                if (s < 0.18f) {
                    key = -1; // neutral
                } else {
                    int hueSector = ((int) Math.floor(p[3] * 12.0f)) % 12;
                    int vTier = v >= 0.50f ? 1 : 0;
                    key = hueSector * 2 + vTier;
                }
                clusters.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
            }

            // 4. score clusters by chromatic salience (s^2 * v)
            List<float[]> bestCluster = null;
            double bestScore = -1.0;

            for (Map.Entry<Integer, List<float[]>> entry : clusters.entrySet()) {
                int key = entry.getKey();
                List<float[]> clusterPixels = entry.getValue();

                double sSum = 0, vSum = 0, ySum = 0;
                for (float[] p : clusterPixels) {
                    sSum += p[4];
                    vSum += p[5];
                    ySum += p[6];
                }
                double avgS = sSum / clusterPixels.size();
                double avgV = vSum / clusterPixels.size();
                double avgY = ySum / clusterPixels.size();
                double posFactor = 1.0 + (1.0 - (avgY / (double) h)) * 0.8;

                double score;
                if (key == -1) {
                    // neutral: bright white gets higher visual salience
                    double whiteSalience = (avgV >= 0.80) ? (0.2 + 3.0 * (avgV * avgV)) : 0.15;
                    score = clusterPixels.size() * whiteSalience * posFactor;
                } else {
                    // chromatic: quadratic saturation salience
                    score = clusterPixels.size() * (0.1 + 8.0 * (avgS * avgS * avgV)) * posFactor;
                }

                if (score > bestScore) {
                    bestScore = score;
                    bestCluster = clusterPixels;
                }
            }

            if (bestCluster == null || bestCluster.isEmpty()) {
                bestCluster = visiblePixels;
            }

            double rAcc = 0, gAcc = 0, bAcc = 0;
            for (float[] p : bestCluster) {
                rAcc += p[0];
                gAcc += p[1];
                bAcc += p[2];
            }

            int finalR = Math.min(255, Math.max(0, (int) Math.round(rAcc / bestCluster.size())));
            int finalG = Math.min(255, Math.max(0, (int) Math.round(gAcc / bestCluster.size())));
            int finalB = Math.min(255, Math.max(0, (int) Math.round(bAcc / bestCluster.size())));

            return (finalR << 16) | (finalG << 8) | finalB;
        } catch (Exception e) {
            return 0x888888;
        }
    }

    // saves colors map into config/itemorganizer/texture_colors.json
    public static synchronized void saveColorsToConfigFile(Map<String, Integer> additionalColors) {
        try {
            Path baseDir = StorageManager.getInstance().getBaseDir();
            if (!Files.exists(baseDir)) {
                Files.createDirectories(baseDir);
            }
            Path configFile = baseDir.resolve("texture_colors.json");

            Map<String, String> currentMap = new TreeMap<>();
            if (Files.exists(configFile)) {
                try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                    Type mapType = new TypeToken<Map<String, String>>() {}.getType();
                    Map<String, String> existing = JsonHelper.GSON.fromJson(reader, mapType);
                    if (existing != null) {
                        currentMap.putAll(existing);
                    }
                } catch (Exception ignored) {
                }
            }

            for (Map.Entry<String, Integer> entry : additionalColors.entrySet()) {
                String hex = String.format("0x%06X", entry.getValue() & 0xFFFFFF);
                currentMap.put(entry.getKey(), hex);
            }

            try (Writer writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                JsonHelper.GSON.toJson(currentMap, writer);
            }
        } catch (Exception e) {
            ItemOrganizer.LOGGER.error("error saving texture_colors.json in config: ", e);
        }
    }
}
