package com.itemorganizer.gui.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.TransparentBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.EmptyBlockView;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// evaluates physical and rendering properties of blocks
@SuppressWarnings("deprecation")
public class BlockPropertyHelper {
    private static final Map<String, Boolean> FULL_CUBE_CACHE = new ConcurrentHashMap<>(1000);
    private static final Map<String, Boolean> SOLID_COLLISION_CACHE = new ConcurrentHashMap<>(1000);
    private static final Map<String, Boolean> TRANSPARENT_CACHE = new ConcurrentHashMap<>(1000);
    private static final Map<String, Boolean> UNIFORM_TEXTURE_CACHE = new ConcurrentHashMap<>(1000);

    // hard blacklist of patterns that must never be evaluated in palettes
    public static final List<String> PALETTE_BLACKLIST = List.of(
            "spawner",
            "sculk",
            "head",
            "skull",
            "piston",
            "command_block",
            "structure_",
            "jigsaw",
            "barrier",
            "bedrock",
            "end_portal",
            "nether_portal",
            "infested",
            "reinforced_deepslate",
            "vault"
    );

    public static boolean isPaletteBlacklisted(String itemId) {
        if (itemId == null || itemId.isEmpty()) return true;
        String path = getPath(itemId).toLowerCase();
        for (String pattern : PALETTE_BLACKLIST) {
            if (path.contains(pattern)) return true;
        }
        return false;
    }

    private static final Set<String> NON_UNIFORM_PATTERNS = Set.of(
            "_log", "log", "_stem", "stem",
            "grass_block", "podzol", "mycelium", "dirt_path", "farmland",
            "crafting_table", "furnace", "smoker", "blast_furnace",
            "bookshelf", "chiseled_bookshelf", "loom", "smithing_table",
            "fletching_table", "cartography_table", "grindstone", "stonecutter",
            "tnt", "cake", "pumpkin", "carved_pumpkin", "jack_o_lantern", "melon",
            "cactus", "bee_nest", "beehive", "target", "lodestone", "respawn_anchor",
            "command_block", "jukebox", "daylight_detector", "piston", "sticky_piston",
            "observer", "dispenser", "dropper", "crafter", "lectern", "anvil"
    );

    public static void clearCache() {
        FULL_CUBE_CACHE.clear();
        SOLID_COLLISION_CACHE.clear();
        TRANSPARENT_CACHE.clear();
        UNIFORM_TEXTURE_CACHE.clear();
    }

    // checks if the item is a full 1x1x1 cube block
    public static boolean isFullCube(String itemId) {
        if (itemId == null || itemId.isEmpty()) return false;
        return FULL_CUBE_CACHE.computeIfAbsent(itemId.toLowerCase(), idStr -> {
            String path = getPath(idStr);
            if (isNonBlockPath(path)) return false;

            Identifier id = Identifier.tryParse(idStr);
            if (id != null) {
                try {
                    Item item = Registries.ITEM.get(id);
                    if (!(item instanceof BlockItem bi)) return false;
                    Block block = bi.getBlock();
                    BlockState state = block.getDefaultState();
                    VoxelShape shape = state.getOutlineShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN);
                    return Block.isShapeFullCube(shape);
                } catch (Throwable ignored) {
                }
            }

            return isLikelyFullCube(path);
        });
    }

    // checks if the block is solid and collidable
    public static boolean isSolidCollision(String itemId) {
        if (itemId == null || itemId.isEmpty()) return false;
        return SOLID_COLLISION_CACHE.computeIfAbsent(itemId.toLowerCase(), idStr -> {
            String path = getPath(idStr);
            if (isNonBlockPath(path)) return false;

            Identifier id = Identifier.tryParse(idStr);
            if (id != null) {
                try {
                    Item item = Registries.ITEM.get(id);
                    if (!(item instanceof BlockItem bi)) return false;
                    Block block = bi.getBlock();
                    BlockState state = block.getDefaultState();
                    VoxelShape shape = state.getCollisionShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN);
                    return !shape.isEmpty() && state.blocksMovement();
                } catch (Throwable ignored) {
                }
            }

            if (path.contains("air") || path.contains("water") || path.contains("lava") ||
                    path.contains("void") || path.contains("light") || path.contains("flower") ||
                    path.contains("sapling") || path.contains("torch") || path.contains("sign") ||
                    path.contains("banner") || path.contains("rail") || path.contains("wire")) {
                return false;
            }
            return true;
        });
    }

    // checks if the block is transparent or translucent
    public static boolean isTransparent(String itemId) {
        if (itemId == null || itemId.isEmpty()) return false;
        return TRANSPARENT_CACHE.computeIfAbsent(itemId.toLowerCase(), idStr -> {
            String path = getPath(idStr);
            if (isNonBlockPath(path)) return false;

            Identifier id = Identifier.tryParse(idStr);
            if (id != null) {
                try {
                    Item item = Registries.ITEM.get(id);
                    if (!(item instanceof BlockItem bi)) return false;
                    Block block = bi.getBlock();
                    BlockState state = block.getDefaultState();
                    if (!state.isOpaque() || block instanceof TransparentBlock) {
                        return true;
                    }
                } catch (Throwable ignored) {
                }
            }

            return path.contains("glass") || path.contains("ice") || path.contains("slime") ||
                    path.contains("honey") || path.contains("translucent") || path.contains("barrier") ||
                    path.contains("leaves");
        });
    }

    // checks if the block has the exact same texture across all 6 faces
    public static boolean hasUniformTexture(String itemId) {
        if (itemId == null || itemId.isEmpty()) return false;
        return UNIFORM_TEXTURE_CACHE.computeIfAbsent(itemId.toLowerCase(), idStr -> {
            String path = getPath(idStr);
            if (isNonBlockPath(path)) return false;

            for (String nonUniform : NON_UNIFORM_PATTERNS) {
                if (path.equals(nonUniform) || path.endsWith(nonUniform) || path.contains(nonUniform)) {
                    return false;
                }
            }

            Identifier id = Identifier.tryParse(idStr);
            if (id != null) {
                try {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client != null && client.getResourceManager() != null) {
                        Identifier modelResId = Identifier.of(id.getNamespace(), "models/block/" + path + ".json");
                        var opt = client.getResourceManager().getResource(modelResId);
                        if (opt.isPresent()) {
                            try (java.io.Reader r = opt.get().getReader()) {
                                com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseReader(r).getAsJsonObject();
                                if (obj.has("parent")) {
                                    String parent = obj.get("parent").getAsString();
                                    if (parent.endsWith("cube_all") || parent.endsWith("cube")) {
                                        return true;
                                    }
                                    if (parent.endsWith("cube_column") || parent.endsWith("cube_bottom_top") ||
                                            parent.endsWith("orientable")) {
                                        return false;
                                    }
                                }
                                if (obj.has("textures")) {
                                    com.google.gson.JsonObject tex = obj.getAsJsonObject("textures");
                                    if (tex.has("all") && tex.size() == 1) {
                                        return true;
                                    }
                                    if (tex.has("top") && tex.has("side")) {
                                        return tex.get("top").equals(tex.get("side"));
                                    }
                                }
                            }
                        }
                    }
                } catch (Throwable ignored) {
                }
            }

            if ((path.endsWith("_wood") || path.endsWith("_hyphae") || path.contains("wood") || path.contains("hyphae")) &&
                    !path.contains("log") && !path.contains("stem") && !isNonBlockPath(path)) {
                return true;
            }

            return path.contains("concrete") || path.contains("wool") || path.contains("terracotta") ||
                    path.contains("glass") || path.contains("planks") || path.equals("stone") || path.contains("granite") ||
                    path.contains("diorite") || path.contains("andesite") || path.contains("bricks") ||
                    path.contains("deepslate") || path.contains("prismarine") || path.contains("obsidian");
        });
    }

    private static String getPath(String itemId) {
        String clean = itemId;
        int bracket = clean.indexOf('[');
        if (bracket != -1) clean = clean.substring(0, bracket).trim();
        int colon = clean.indexOf(':');
        return colon != -1 ? clean.substring(colon + 1) : clean;
    }

    private static boolean isNonBlockPath(String path) {
        return path.contains("_sword") || path.contains("_pickaxe") || path.contains("_axe") ||
                path.contains("_shovel") || path.contains("_hoe") || path.contains("_helmet") ||
                path.contains("_chestplate") || path.contains("_leggings") || path.contains("_boots") ||
                path.contains("_ingot") || path.contains("_nugget") || path.startsWith("raw_") ||
                path.contains("_dust") || path.contains("_shard") || path.contains("_trim") ||
                path.contains("apple") || path.contains("_seeds") || path.contains("bucket") ||
                path.contains("potion") || path.contains("egg") || path.contains("_dye") ||
                path.equals("stick") || path.equals("feather") || path.equals("string") ||
                path.equals("bone") || path.equals("leather") || path.contains("pattern") ||
                path.contains("template") || path.contains("disc") || path.contains("book");
    }

    private static boolean isLikelyFullCube(String path) {
        if (path.contains("air") || path.contains("void") || path.contains("water") || path.contains("lava") ||
                path.contains("slab") || path.contains("stairs") || path.contains("fence") ||
                path.contains("wall") || path.contains("pane") || path.contains("door") ||
                path.contains("trapdoor") || path.contains("button") || path.contains("plate") ||
                path.contains("carpet") || path.contains("torch") || path.contains("lantern") ||
                path.contains("chain") || path.contains("bars") || path.contains("sign") ||
                path.contains("banner") || path.contains("bed") || path.contains("candle") ||
                path.contains("rail") || path.contains("rod") || path.contains("flower") ||
                path.contains("sapling") || path.contains("head") || path.contains("skull")) {
            return false;
        }
        return true;
    }

    // checks if the block is an ore
    public static boolean isOre(String itemId) {
        if (itemId == null || itemId.isEmpty()) return false;
        String path = getPath(itemId).toLowerCase();
        return path.endsWith("_ore") || path.contains("_ore_") || path.equals("ancient_debris");
    }

    // checks if the block is a glazed terracotta block
    public static boolean isGlazedTerracotta(String itemId) {
        if (itemId == null || itemId.isEmpty()) return false;
        String path = getPath(itemId).toLowerCase();
        return path.contains("glazed_terracotta");
    }

    // checks if the block is a light emitting block
    public static boolean isLightBlock(String itemId) {
        if (itemId == null || itemId.isEmpty()) return false;
        Identifier id = Identifier.tryParse(itemId);
        if (id != null) {
            try {
                Item item = Registries.ITEM.get(id);
                if (item instanceof BlockItem bi) {
                    if (bi.getBlock().getDefaultState().getLuminance() > 0) {
                        return true;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        String path = getPath(itemId).toLowerCase();
        return path.contains("lantern") || path.contains("glowstone") || path.contains("sea_lantern") ||
                path.contains("shroomlight") || path.contains("froglight") || path.contains("magma") ||
                path.contains("crying_obsidian") || path.contains("jack_o_lantern") || path.contains("campfire") ||
                path.contains("torch") || path.contains("candle") || path.contains("beacon") ||
                path.contains("conduit") || path.equals("light") || path.startsWith("light_") ||
                path.contains("end_rod") || path.contains("glow_lichen");
    }

    // checks if the item is a placeable building block (excluding technical/utility/debug blocks)
    public static boolean isBuildingBlock(String itemId) {
        if (itemId == null || itemId.isEmpty()) return false;
        String path = getPath(itemId).toLowerCase();
        if (isNonBlockPath(path)) return false;
        if (isPaletteBlacklisted(path)) return false;
        Identifier id = Identifier.tryParse(itemId);
        if (id != null) {
            try {
                Item item = Registries.ITEM.get(id);
                return item instanceof BlockItem;
            } catch (Throwable ignored) {
            }
        }
        return true;
    }
}
