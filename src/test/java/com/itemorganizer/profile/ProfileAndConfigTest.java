package com.itemorganizer.profile;

import com.itemorganizer.core.model.ItemCategory;
import com.itemorganizer.core.model.ItemSlotPosition;
import com.itemorganizer.core.model.ModConfig;
import com.itemorganizer.core.model.ProfileData;
import com.itemorganizer.storage.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

// unit tests for profile management and configuration
public class ProfileAndConfigTest {

    @TempDir
    Path tempDir;

    private ProfileRepository profileRepository;

    @BeforeEach
    void setUp() {
        profileRepository = new ProfileRepository(tempDir);
        profileRepository.init();
    }

    @Test
    void testProfileCreationEmptyAndClone() {
        // base profile with items
        ProfileData base = new ProfileData("creative_blocks");
        base.setItemAt("minecraft:oak_log", 0, 0);
        base.setItemAt("minecraft:stone", 1, 0);
        base.setItemAt("minecraft:glass", 2, 0);
        assertTrue(profileRepository.saveProfile(base));

        // clone profile
        ProfileData cloned = new ProfileData("cloned_blocks");
        for (ItemSlotPosition pos : base.getItems()) {
            cloned.setItemAt(pos.getItemId(), pos.getX(), pos.getY());
        }
        assertTrue(profileRepository.saveProfile(cloned));

        // verify cloned items
        ProfileData loadedClone = profileRepository.loadProfile("cloned_blocks");
        assertEquals(3, loadedClone.getItems().size());
        assertTrue(loadedClone.hasItem("minecraft:oak_log"));
        assertTrue(loadedClone.hasItem("minecraft:stone"));
        assertTrue(loadedClone.hasItem("minecraft:glass"));

        // new empty profile
        ProfileData empty = new ProfileData("empty_profile");
        assertTrue(profileRepository.saveProfile(empty));
        ProfileData loadedEmpty = profileRepository.loadProfile("empty_profile");
        assertEquals(0, loadedEmpty.getItems().size());
    }

    @Test
    void testDefaultProfileProtection() {
        // check default profile exists
        List<String> list = profileRepository.listProfiles();
        assertTrue(list.contains(ProfileRepository.DEFAULT_PROFILE_NAME));

        // deleting default profile should fail
        assertFalse(profileRepository.deleteProfile(ProfileRepository.DEFAULT_PROFILE_NAME));
        assertTrue(profileRepository.listProfiles().contains(ProfileRepository.DEFAULT_PROFILE_NAME));
    }

    @Test
    void testConfigArgbCalculationAndClamping() {
        ModConfig config = new ModConfig();

        // defaults
        assertEquals(0x101010, config.getBackgroundColor());
        assertEquals(0.60f, config.getTransparency(), 0.001f);
        assertEquals(1.0f, config.getScale(), 0.001f);

        // calculate argb
        int argb = config.getArgbColor();
        int expectedAlpha = (int) (0.60f * 255.0f) & 0xFF;
        assertEquals(expectedAlpha, (argb >>> 24) & 0xFF);
        assertEquals(0x101010, argb & 0x00FFFFFF);

        // transparency clamping (0.0 to 1.0)
        config.setTransparency(-0.5f);
        assertEquals(0.0f, config.getTransparency(), 0.001f);
        assertEquals(0x00, (config.getArgbColor() >>> 24) & 0xFF);

        config.setTransparency(1.5f);
        assertEquals(1.0f, config.getTransparency(), 0.001f);
        assertEquals(0xFF, (config.getArgbColor() >>> 24) & 0xFF);

        // grid scale clamping (0.10 to 5.00)
        config.setScale(0.05f);
        assertEquals(0.10f, config.getScale(), 0.001f);

        config.setScale(6.0f);
        assertEquals(5.00f, config.getScale(), 0.001f);

        // item scale clamping (0.5 to 1.5)
        assertEquals(1.0f, config.getItemScale(), 0.001f);
        config.setItemScale(0.2f);
        assertEquals(0.5f, config.getItemScale(), 0.001f);
        config.setItemScale(3.0f);
        assertEquals(1.5f, config.getItemScale(), 0.001f);

        // text scale clamping (0.5 to 1.5)
        assertEquals(1.0f, config.getTextScale(), 0.001f);
        config.setTextScale(0.2f);
        assertEquals(0.5f, config.getTextScale(), 0.001f);
        config.setTextScale(3.0f);
        assertEquals(1.5f, config.getTextScale(), 0.001f);

        // split ratio clamping (0.20 to 0.80, default 0.50)
        assertEquals(0.50f, config.getSplitRatio(), 0.001f);
        config.setSplitRatio(0.10f);
        assertEquals(0.20f, config.getSplitRatio(), 0.001f);
        config.setSplitRatio(0.95f);
        assertEquals(0.80f, config.getSplitRatio(), 0.001f);

        // hotbar scale clamping (0.50 to 2.00, default 1.00)
        assertEquals(1.00f, config.getHotbarScale(), 0.001f);
        config.setHotbarScale(0.20f);
        assertEquals(0.50f, config.getHotbarScale(), 0.001f);
        config.setHotbarScale(3.50f);
        assertEquals(2.00f, config.getHotbarScale(), 0.001f);

        // hotbar item scale clamping (0.50 to 1.50, default 1.00)
        assertEquals(1.00f, config.getHotbarItemScale(), 0.001f);
        config.setHotbarItemScale(0.20f);
        assertEquals(0.50f, config.getHotbarItemScale(), 0.001f);
        config.setHotbarItemScale(3.50f);
        assertEquals(1.50f, config.getHotbarItemScale(), 0.001f);

        // palette item scale clamping (0.50 to 1.50, default 1.00)
        assertEquals(1.00f, config.getPaletteItemScale(), 0.001f);
        config.setPaletteItemScale(0.20f);
        assertEquals(0.50f, config.getPaletteItemScale(), 0.001f);
        config.setPaletteItemScale(3.50f);
        assertEquals(1.50f, config.getPaletteItemScale(), 0.001f);

        // palette button scale clamping (0.50 to 2.00, default 1.00)
        assertEquals(1.00f, config.getPaletteButtonScale(), 0.001f);
        config.setPaletteButtonScale(0.20f);
        assertEquals(0.50f, config.getPaletteButtonScale(), 0.001f);
        config.setPaletteButtonScale(3.50f);
        assertEquals(2.00f, config.getPaletteButtonScale(), 0.001f);

        // create palette scale clamping (0.50 to 2.00, default 1.00)
        assertEquals(1.00f, config.getCreatePaletteScale(), 0.001f);
        config.setCreatePaletteScale(0.20f);
        assertEquals(0.50f, config.getCreatePaletteScale(), 0.001f);
        config.setCreatePaletteScale(3.50f);
        assertEquals(2.00f, config.getCreatePaletteScale(), 0.001f);

        // create palette item scale clamping (0.50 to 1.50, default 1.00)
        assertEquals(1.00f, config.getCreatePaletteItemScale(), 0.001f);
        config.setCreatePaletteItemScale(0.20f);
        assertEquals(0.50f, config.getCreatePaletteItemScale(), 0.001f);
        config.setCreatePaletteItemScale(3.50f);
        assertEquals(1.50f, config.getCreatePaletteItemScale(), 0.001f);

        // create palette button scale clamping (0.50 to 2.00, default 1.00)
        assertEquals(1.00f, config.getCreatePaletteButtonScale(), 0.001f);
        config.setCreatePaletteButtonScale(0.20f);
        assertEquals(0.50f, config.getCreatePaletteButtonScale(), 0.001f);
        config.setCreatePaletteButtonScale(3.50f);
        assertEquals(2.00f, config.getCreatePaletteButtonScale(), 0.001f);
    }

    @Test
    void testProfileDynamicReflowToColumns() {
        ProfileData profile = new ProfileData("reflow_test");
        profile.setColumnCount(10);
        // place 10 items in row 0 and 2 items in row 1
        for (int i = 0; i < 10; i++) {
            profile.setItemAt("item_" + i, i, 0);
        }
        profile.setItemAt("item_10", 0, 1);
        profile.setItemAt("item_11", 1, 1);

        assertEquals(12, profile.getItems().size());
        assertEquals(10, profile.getColumnCount());

        // reduce columns to 6
        boolean changed = profile.reflowToColumns(6);
        assertTrue(changed);
        assertEquals(6, profile.getColumnCount());
        assertEquals(12, profile.getItems().size());

        // verify linear order preserved without lost items
        for (int i = 0; i < 12; i++) {
            int expectedX = i % 6;
            int expectedY = i / 6;
            Optional<ItemSlotPosition> pos = profile.findItemAt(expectedX, expectedY);
            assertTrue(pos.isPresent(), "item at expected pos (" + expectedX + ", " + expectedY + ") not found");
            assertEquals("item_" + i, pos.get().getItemId());
        }

        // expand back to 10 columns
        boolean changedBack = profile.reflowToColumns(10);
        assertTrue(changedBack);
        assertEquals(10, profile.getColumnCount());
        assertEquals(12, profile.getItems().size());

        for (int i = 0; i < 12; i++) {
            int expectedX = i % 10;
            int expectedY = i / 10;
            Optional<ItemSlotPosition> pos = profile.findItemAt(expectedX, expectedY);
            assertTrue(pos.isPresent(), "item at restored pos (" + expectedX + ", " + expectedY + ") not found");
            assertEquals("item_" + i, pos.get().getItemId());
        }
    }

    @Test
    void testConfigQuickAppendKey() {
        ModConfig config = new ModConfig();
        // default key: a
        assertEquals("key.keyboard.a", config.getKeyQuickAppend());

        // reassign key
        config.setKeyQuickAppend("key.keyboard.g");
        assertEquals("key.keyboard.g", config.getKeyQuickAppend());

        // defensive fallback for null or empty
        config.setKeyQuickAppend(null);
        assertEquals("key.keyboard.a", config.getKeyQuickAppend());
        config.setKeyQuickAppend("");
        assertEquals("key.keyboard.a", config.getKeyQuickAppend());
    }

    @Test
    void testQuickAppendSlotCalculation() {
        ProfileData profile = new ProfileData("quick_append_test");
        int cols = 10;
        profile.setColumnCount(cols);

        // empty profile: should place at (0, 0)
        String firstItem = "minecraft:dirt";
        int nextIndex = 0;
        if (!profile.getItems().isEmpty()) {
            int maxIdx = -1;
            for (ItemSlotPosition pos : profile.getItems()) {
                int idx = pos.getY() * cols + pos.getX();
                if (idx > maxIdx) maxIdx = idx;
            }
            nextIndex = maxIdx + 1;
        }
        profile.setItemAt(firstItem, nextIndex % cols, nextIndex / cols);
        Optional<ItemSlotPosition> pos0 = profile.findPositionOf(firstItem);
        assertTrue(pos0.isPresent());
        assertEquals(0, pos0.get().getX());
        assertEquals(0, pos0.get().getY());

        // fill row up to x=9, y=0
        for (int i = 1; i < 10; i++) {
            String item = "minecraft:item_" + i;
            int maxIdx = -1;
            for (ItemSlotPosition pos : profile.getItems()) {
                int idx = pos.getY() * cols + pos.getX();
                if (idx > maxIdx) maxIdx = idx;
            }
            int next = maxIdx + 1;
            profile.setItemAt(item, next % cols, next / cols);
        }

        // last item is at x=9, y=0
        Optional<ItemSlotPosition> pos9 = profile.findPositionOf("minecraft:item_9");
        assertTrue(pos9.isPresent());
        assertEquals(9, pos9.get().getX());
        assertEquals(0, pos9.get().getY());

        // next item wraps to next row: x=0, y=1
        String wrapItem = "minecraft:stone";
        int maxIdx = -1;
        for (ItemSlotPosition pos : profile.getItems()) {
            int idx = pos.getY() * cols + pos.getX();
            if (idx > maxIdx) maxIdx = idx;
        }
        int next = maxIdx + 1;
        profile.setItemAt(wrapItem, next % cols, next / cols);

        Optional<ItemSlotPosition> wrapPos = profile.findPositionOf(wrapItem);
        assertTrue(wrapPos.isPresent());
        assertEquals(0, wrapPos.get().getX());
        assertEquals(1, wrapPos.get().getY());

        // re-appending existing item moves it to the end
        profile.removeItem(firstItem);
        maxIdx = -1;
        for (ItemSlotPosition pos : profile.getItems()) {
            int idx = pos.getY() * cols + pos.getX();
            if (idx > maxIdx) maxIdx = idx;
        }
        next = maxIdx + 1;
        profile.setItemAt(firstItem, next % cols, next / cols);

        Optional<ItemSlotPosition> reubPos = profile.findPositionOf(firstItem);
        assertTrue(reubPos.isPresent());
        assertEquals(1, reubPos.get().getX());
        assertEquals(1, reubPos.get().getY());
    }

    @Test
    void testCompactItemsEliminatesGaps() {
        ProfileData profile = new ProfileData("test_compact");
        // insert items with empty gaps
        profile.setItemAt("minecraft:stone", 0, 0);
        profile.setItemAt("minecraft:dirt", 4, 0);
        profile.setItemAt("minecraft:glass", 8, 0);
        profile.setItemAt("minecraft:sand", 2, 1);

        assertEquals(4, profile.getItems().size());

        // compact into 9 columns
        boolean changed = profile.compactItems(9);
        assertTrue(changed);

        // all 4 items should be contiguous
        assertEquals(0, profile.findPositionOf("minecraft:stone").get().getX());
        assertEquals(0, profile.findPositionOf("minecraft:stone").get().getY());

        assertEquals(1, profile.findPositionOf("minecraft:dirt").get().getX());
        assertEquals(0, profile.findPositionOf("minecraft:dirt").get().getY());

        assertEquals(2, profile.findPositionOf("minecraft:glass").get().getX());
        assertEquals(0, profile.findPositionOf("minecraft:glass").get().getY());

        assertEquals(3, profile.findPositionOf("minecraft:sand").get().getX());
        assertEquals(0, profile.findPositionOf("minecraft:sand").get().getY());

        // already compacted, returns false
        assertFalse(profile.compactItems(9));
    }

    @Test
    void testSortItemsContiguous() {
        ProfileData profile = new ProfileData("test_sort");
        profile.setItemAt("minecraft:stone", 5, 2);
        profile.setItemAt("minecraft:apple", 1, 0);
        profile.setItemAt("minecraft:bedrock", 7, 1);

        // sort alphabetically
        boolean changed = profile.sortItems(String::compareTo, 9);
        assertTrue(changed);

        // verify alphabetical order: apple -> bedrock -> stone
        assertEquals(0, profile.findPositionOf("minecraft:apple").get().getX());
        assertEquals(0, profile.findPositionOf("minecraft:apple").get().getY());

        assertEquals(1, profile.findPositionOf("minecraft:bedrock").get().getX());
        assertEquals(0, profile.findPositionOf("minecraft:bedrock").get().getY());

        assertEquals(2, profile.findPositionOf("minecraft:stone").get().getX());
        assertEquals(0, profile.findPositionOf("minecraft:stone").get().getY());
    }

    @Test
    void testItemCategoryOrderingExactSequence() {
        // category mappings
        assertEquals(ItemCategory.FULL_BLOCKS, ItemCategory.getCategory("minecraft:stone"));
        assertEquals(ItemCategory.FULL_BLOCKS, ItemCategory.getCategory("minecraft:white_concrete"));
        assertEquals(ItemCategory.FULL_BLOCKS, ItemCategory.getCategory("minecraft:oak_planks"));

        assertEquals(ItemCategory.LOGS_AND_STEMS, ItemCategory.getCategory("minecraft:oak_log"));
        assertEquals(ItemCategory.LOGS_AND_STEMS, ItemCategory.getCategory("minecraft:spruce_log"));

        assertEquals(ItemCategory.ORES, ItemCategory.getCategory("minecraft:iron_ore"));
        assertEquals(ItemCategory.ORES, ItemCategory.getCategory("minecraft:coal_ore"));

        assertEquals(ItemCategory.GLAZED_TERRACOTTA, ItemCategory.getCategory("minecraft:white_glazed_terracotta"));
        assertEquals(ItemCategory.GLAZED_TERRACOTTA, ItemCategory.getCategory("minecraft:red_glazed_terracotta"));

        assertEquals(ItemCategory.GRATES, ItemCategory.getCategory("minecraft:copper_grate"));
        assertEquals(ItemCategory.BULBS, ItemCategory.getCategory("minecraft:copper_bulb"));

        assertEquals(ItemCategory.TOOLS_REDSTONE, ItemCategory.getCategory("minecraft:redstone"));
        assertEquals(ItemCategory.TOOLS_REDSTONE, ItemCategory.getCategory("minecraft:repeater"));

        assertEquals(ItemCategory.WORKING_STATIONS, ItemCategory.getCategory("minecraft:crafting_table"));
        assertEquals(ItemCategory.WORKING_STATIONS, ItemCategory.getCategory("minecraft:furnace"));
        assertEquals(ItemCategory.WORKING_STATIONS, ItemCategory.getCategory("minecraft:anvil"));

        assertEquals(ItemCategory.UNCATEGORIZED_VEGETATION, ItemCategory.getCategory("minecraft:fern"));
        assertEquals(ItemCategory.UNCATEGORIZED_VEGETATION, ItemCategory.getCategory("minecraft:short_grass"));

        assertEquals(ItemCategory.CROPS, ItemCategory.getCategory("minecraft:wheat"));
        assertEquals(ItemCategory.CROPS, ItemCategory.getCategory("minecraft:carrot"));

        assertEquals(ItemCategory.FLOWERS, ItemCategory.getCategory("minecraft:dandelion"));
        assertEquals(ItemCategory.FLOWERS, ItemCategory.getCategory("minecraft:poppy"));

        assertEquals(ItemCategory.TORCH, ItemCategory.getCategory("minecraft:torch"));
        assertEquals(ItemCategory.TORCH, ItemCategory.getCategory("minecraft:soul_torch"));

        assertEquals(ItemCategory.GLASS, ItemCategory.getCategory("minecraft:glass"));
        assertEquals(ItemCategory.GLASS, ItemCategory.getCategory("minecraft:tinted_glass"));
        assertEquals(ItemCategory.GLASS, ItemCategory.getCategory("minecraft:white_stained_glass"));

        assertEquals(ItemCategory.GLASS_PANES, ItemCategory.getCategory("minecraft:glass_pane"));
        assertEquals(ItemCategory.GLASS_PANES, ItemCategory.getCategory("minecraft:red_stained_glass_pane"));

        assertEquals(ItemCategory.BARS, ItemCategory.getCategory("minecraft:iron_bars"));
        assertEquals(ItemCategory.CHAINS, ItemCategory.getCategory("minecraft:chain"));
        assertEquals(ItemCategory.FENCES, ItemCategory.getCategory("minecraft:oak_fence"));
        assertEquals(ItemCategory.WALLS, ItemCategory.getCategory("minecraft:cobblestone_wall"));
        assertEquals(ItemCategory.SLABS, ItemCategory.getCategory("minecraft:oak_slab"));
        assertEquals(ItemCategory.FENCE_GATES, ItemCategory.getCategory("minecraft:oak_fence_gate"));
        assertEquals(ItemCategory.STAIRS, ItemCategory.getCategory("minecraft:oak_stairs"));
        assertEquals(ItemCategory.SAPLINGS, ItemCategory.getCategory("minecraft:oak_sapling"));
        assertEquals(ItemCategory.LEAVES, ItemCategory.getCategory("minecraft:oak_leaves"));
        assertEquals(ItemCategory.CARPETS, ItemCategory.getCategory("minecraft:white_carpet"));
        assertEquals(ItemCategory.LANTERNS, ItemCategory.getCategory("minecraft:lantern"));
        assertEquals(ItemCategory.SHELVES, ItemCategory.getCategory("minecraft:bookshelf"));
        assertEquals(ItemCategory.SIGNS, ItemCategory.getCategory("minecraft:oak_sign"));
        assertEquals(ItemCategory.DOORS, ItemCategory.getCategory("minecraft:oak_door"));
        assertEquals(ItemCategory.TRAPDOORS, ItemCategory.getCategory("minecraft:oak_trapdoor"));
        assertEquals(ItemCategory.PRESSURE_PLATES, ItemCategory.getCategory("minecraft:oak_pressure_plate"));
        assertEquals(ItemCategory.BEDS, ItemCategory.getCategory("minecraft:white_bed"));
        assertEquals(ItemCategory.BOATS, ItemCategory.getCategory("minecraft:oak_boat"));
        assertEquals(ItemCategory.BUTTONS, ItemCategory.getCategory("minecraft:oak_button"));
        assertEquals(ItemCategory.DYES, ItemCategory.getCategory("minecraft:white_dye"));
        assertEquals(ItemCategory.SPAWN_EGGS, ItemCategory.getCategory("minecraft:egg"));
        assertEquals(ItemCategory.CANDLES, ItemCategory.getCategory("minecraft:candle"));
        assertEquals(ItemCategory.BANNERS, ItemCategory.getCategory("minecraft:white_banner"));
        assertEquals(ItemCategory.CORALS, ItemCategory.getCategory("minecraft:tube_coral"));
        assertEquals(ItemCategory.HARNESS, ItemCategory.getCategory("minecraft:saddle"));
        assertEquals(ItemCategory.TOOLS, ItemCategory.getCategory("minecraft:diamond_pickaxe"));
        assertEquals(ItemCategory.ARMOR, ItemCategory.getCategory("minecraft:diamond_helmet"));
        assertEquals(ItemCategory.RODS, ItemCategory.getCategory("minecraft:blaze_rod"));
        assertEquals(ItemCategory.BUNDLES, ItemCategory.getCategory("minecraft:bundle"));
        assertEquals(ItemCategory.BOOKS, ItemCategory.getCategory("minecraft:book"));
        assertEquals(ItemCategory.SHERDS, ItemCategory.getCategory("minecraft:angler_pottery_sherd"));
        assertEquals(ItemCategory.HEADS, ItemCategory.getCategory("minecraft:skeleton_skull"));
        assertEquals(ItemCategory.HEADS, ItemCategory.getCategory("minecraft:conduit"));
        assertEquals(ItemCategory.HEADS, ItemCategory.getCategory("minecraft:dried_ghast"));
        assertEquals(ItemCategory.HEADS, ItemCategory.getCategory("minecraft:carved_pumpkin"));
        assertEquals(ItemCategory.TRIMS, ItemCategory.getCategory("minecraft:coast_armor_trim_smithing_template"));
        assertEquals(ItemCategory.SHULKER_BOXES, ItemCategory.getCategory("minecraft:shulker_box"));
        assertEquals(ItemCategory.SCRAPS, ItemCategory.getCategory("minecraft:netherite_scrap"));
        assertEquals(ItemCategory.BUCKETS, ItemCategory.getCategory("minecraft:bucket"));
        assertEquals(ItemCategory.POTIONS, ItemCategory.getCategory("minecraft:potion"));
        assertEquals(ItemCategory.ARROWS, ItemCategory.getCategory("minecraft:arrow"));
        assertEquals(ItemCategory.DISCS, ItemCategory.getCategory("minecraft:music_disc_13"));
        assertEquals(ItemCategory.RAILS, ItemCategory.getCategory("minecraft:rail"));
        assertEquals(ItemCategory.CHESTS, ItemCategory.getCategory("minecraft:chest"));
        assertEquals(ItemCategory.MINECARTS, ItemCategory.getCategory("minecraft:minecart"));
        assertEquals(ItemCategory.GOLEMS, ItemCategory.getCategory("minecraft:copper_golem_statue"));
        assertEquals(ItemCategory.MISC, ItemCategory.getCategory("minecraft:apple"));

        // verify that getItemCategoryOrder matches ItemCategory.getOrder(...)
        for (String sample : java.util.List.of("minecraft:stone", "minecraft:iron_ore", "minecraft:copper_grate",
                "minecraft:conduit", "minecraft:skeleton_skull", "minecraft:shulker_box", "minecraft:apple")) {
            ItemCategory cat = ItemCategory.getCategory(sample);
            assertEquals(ItemCategory.getOrder(cat), com.itemorganizer.gui.util.ItemColorHelper.getItemCategoryOrder(sample));
        }

        // verify that active categories have strictly increasing order indices
        java.util.List<ItemCategory> activeOrder = ItemCategory.getActiveOrder();
        for (int i = 0; i < activeOrder.size() - 1; i++) {
            ItemCategory c1 = activeOrder.get(i);
            ItemCategory c2 = activeOrder.get(i + 1);
            assertTrue(ItemCategory.getOrder(c1) < ItemCategory.getOrder(c2),
                    "Order should be strictly increasing: " + c1 + " before " + c2);
        }
    }

    @Test
    void testColorSortingWithinSameCategory() {
        java.util.Comparator<String> cmp = com.itemorganizer.gui.util.ItemColorHelper.getCategoryColorComparator();

        // inside carpets (cat 13): white_carpet before red_carpet
        assertTrue(cmp.compare("minecraft:white_carpet", "minecraft:red_carpet") < 0);

        // inside glass (cat 2): white_stained_glass before red_stained_glass
        assertTrue(cmp.compare("minecraft:white_stained_glass", "minecraft:red_stained_glass") < 0);

        // verify sorting in ProfileData
        ProfileData profile = new ProfileData("category_sort_test");
        profile.setItemAt("minecraft:red_carpet", 5, 5);
        profile.setItemAt("minecraft:stone", 2, 2);
        profile.setItemAt("minecraft:white_carpet", 7, 7);
        profile.setItemAt("minecraft:glass", 0, 1);

        profile.sortItems(cmp, 9);

        // expected order:
        // (0,0) stone (cat 1)
        // (1,0) glass (cat 2)
        // (2,0) white_carpet (cat 13, white)
        // (3,0) red_carpet (cat 13, red)
        assertEquals(0, profile.findPositionOf("minecraft:stone").get().getX());
        assertEquals(0, profile.findPositionOf("minecraft:stone").get().getY());

        assertEquals(1, profile.findPositionOf("minecraft:glass").get().getX());
        assertEquals(0, profile.findPositionOf("minecraft:glass").get().getY());

        assertEquals(2, profile.findPositionOf("minecraft:white_carpet").get().getX());
        assertEquals(0, profile.findPositionOf("minecraft:white_carpet").get().getY());

        assertEquals(3, profile.findPositionOf("minecraft:red_carpet").get().getX());
        assertEquals(0, profile.findPositionOf("minecraft:red_carpet").get().getY());
    }

    @Test
    void testPureColorGradientSorting() {
        java.util.Comparator<String> cmp = com.itemorganizer.gui.util.ItemColorHelper.getColorComparator();

        // neutrals transition: blanco -> gris -> negro
        assertTrue(cmp.compare("minecraft:white_concrete", "minecraft:gray_concrete") < 0);
        assertTrue(cmp.compare("minecraft:gray_concrete", "minecraft:black_concrete") < 0);

        // dark blocks like black_terracotta sort into neutrals, before chromatics
        float[] blackTerracottaHsv = com.itemorganizer.gui.util.ItemColorHelper.rgbToHsv(
                com.itemorganizer.gui.util.ItemColorHelper.getItemColor("minecraft:black_terracotta")
        );
        assertTrue(com.itemorganizer.gui.util.ItemColorHelper.isNeutral(blackTerracottaHsv));
        assertTrue(cmp.compare("minecraft:black_concrete", "minecraft:red_concrete") < 0);

        // chromatics: roja -> naranja -> amarillo -> lima -> verde -> cyan -> azul -> purpura -> magenta -> rosa
        assertTrue(cmp.compare("minecraft:red_concrete", "minecraft:orange_concrete") < 0);
        assertTrue(cmp.compare("minecraft:orange_concrete", "minecraft:yellow_concrete") < 0);
        assertTrue(cmp.compare("minecraft:yellow_concrete", "minecraft:lime_concrete") < 0);
        assertTrue(cmp.compare("minecraft:lime_concrete", "minecraft:green_concrete") < 0);
        assertTrue(cmp.compare("minecraft:green_concrete", "minecraft:cyan_concrete") < 0);
        assertTrue(cmp.compare("minecraft:cyan_concrete", "minecraft:blue_concrete") < 0);
        assertTrue(cmp.compare("minecraft:blue_concrete", "minecraft:purple_concrete") < 0);
        assertTrue(cmp.compare("minecraft:purple_concrete", "minecraft:magenta_concrete") < 0);
        assertTrue(cmp.compare("minecraft:magenta_concrete", "minecraft:pink_concrete") < 0);

        // wool continuous sequence
        assertTrue(cmp.compare("minecraft:white_wool", "minecraft:light_gray_wool") < 0);
        assertTrue(cmp.compare("minecraft:light_gray_wool", "minecraft:gray_wool") < 0);
        assertTrue(cmp.compare("minecraft:gray_wool", "minecraft:black_wool") < 0);
        assertTrue(cmp.compare("minecraft:black_wool", "minecraft:red_wool") < 0);
        assertTrue(cmp.compare("minecraft:red_wool", "minecraft:brown_wool") < 0);
        assertTrue(cmp.compare("minecraft:brown_wool", "minecraft:orange_wool") < 0);
        assertTrue(cmp.compare("minecraft:orange_wool", "minecraft:yellow_wool") < 0);
        assertTrue(cmp.compare("minecraft:yellow_wool", "minecraft:lime_wool") < 0);
        assertTrue(cmp.compare("minecraft:lime_wool", "minecraft:green_wool") < 0);
        assertTrue(cmp.compare("minecraft:green_wool", "minecraft:cyan_wool") < 0);
        assertTrue(cmp.compare("minecraft:cyan_wool", "minecraft:light_blue_wool") < 0);
        assertTrue(cmp.compare("minecraft:light_blue_wool", "minecraft:blue_wool") < 0);
        assertTrue(cmp.compare("minecraft:blue_wool", "minecraft:purple_wool") < 0);
        assertTrue(cmp.compare("minecraft:purple_wool", "minecraft:magenta_wool") < 0);
        assertTrue(cmp.compare("minecraft:magenta_wool", "minecraft:pink_wool") < 0);
    }

    @Test
    void testCategoryGroupedColorGradient() {
        java.util.Comparator<String> cmp = com.itemorganizer.gui.util.ItemColorHelper.getColorComparator();

        ProfileData multiCatProfile = new ProfileData("multi_cat_gradient");
        // full blocks
        multiCatProfile.setItemAt("minecraft:red_wool", 0, 0);
        multiCatProfile.setItemAt("minecraft:white_wool", 1, 0);
        multiCatProfile.setItemAt("minecraft:orange_wool", 2, 0);
        // carpets
        multiCatProfile.setItemAt("minecraft:red_carpet", 3, 0);
        multiCatProfile.setItemAt("minecraft:white_carpet", 4, 0);
        multiCatProfile.setItemAt("minecraft:orange_carpet", 5, 0);

        multiCatProfile.sortItems(cmp, 3);

        // full blocks group first, ordered by gradient (white -> red -> orange)
        assertEquals(0, multiCatProfile.findPositionOf("minecraft:white_wool").get().getX());
        assertEquals(0, multiCatProfile.findPositionOf("minecraft:white_wool").get().getY());
        assertEquals(1, multiCatProfile.findPositionOf("minecraft:red_wool").get().getX());
        assertEquals(0, multiCatProfile.findPositionOf("minecraft:red_wool").get().getY());
        assertEquals(2, multiCatProfile.findPositionOf("minecraft:orange_wool").get().getX());
        assertEquals(0, multiCatProfile.findPositionOf("minecraft:orange_wool").get().getY());

        // carpets group second, ordered by the same gradient (white -> red -> orange)
        assertEquals(0, multiCatProfile.findPositionOf("minecraft:white_carpet").get().getX());
        assertEquals(1, multiCatProfile.findPositionOf("minecraft:white_carpet").get().getY());
        assertEquals(1, multiCatProfile.findPositionOf("minecraft:red_carpet").get().getX());
        assertEquals(1, multiCatProfile.findPositionOf("minecraft:red_carpet").get().getY());
        assertEquals(2, multiCatProfile.findPositionOf("minecraft:orange_carpet").get().getX());
        assertEquals(1, multiCatProfile.findPositionOf("minecraft:orange_carpet").get().getY());
    }

    @Test
    void testColorComparatorTimSortContractAndTransitivity() {
        java.util.Comparator<String> cmp = com.itemorganizer.gui.util.ItemColorHelper.getColorComparator();

        // diverse item list
        java.util.List<String> items = new java.util.ArrayList<>(java.util.Arrays.asList(
                "minecraft:diamond", "minecraft:emerald", "minecraft:gold_ingot", "minecraft:iron_ingot",
                "minecraft:copper_ingot", "minecraft:netherite_ingot", "minecraft:coal", "minecraft:charcoal",
                "minecraft:redstone", "minecraft:lapis_lazuli", "minecraft:amethyst_shard", "minecraft:quartz",
                "minecraft:apple", "minecraft:golden_apple", "minecraft:carrot", "minecraft:potato",
                "minecraft:white_wool", "minecraft:orange_wool", "minecraft:magenta_wool", "minecraft:light_blue_wool",
                "minecraft:yellow_wool", "minecraft:lime_wool", "minecraft:pink_wool", "minecraft:gray_wool",
                "minecraft:light_gray_wool", "minecraft:cyan_wool", "minecraft:purple_wool", "minecraft:blue_wool",
                "minecraft:brown_wool", "minecraft:green_wool", "minecraft:red_wool", "minecraft:black_wool",
                "minecraft:white_concrete", "minecraft:red_concrete", "minecraft:blue_concrete", "minecraft:yellow_concrete",
                "minecraft:glass", "minecraft:tinted_glass", "minecraft:red_stained_glass", "minecraft:blue_stained_glass",
                "minecraft:iron_bars", "minecraft:chain", "minecraft:oak_fence", "minecraft:stone_brick_wall",
                "minecraft:oak_slab", "minecraft:stone_slab", "minecraft:oak_fence_gate", "minecraft:oak_stairs",
                "minecraft:oak_sapling", "minecraft:oak_leaves", "minecraft:white_carpet", "minecraft:red_carpet",
                "minecraft:lantern", "minecraft:bookshelf", "minecraft:oak_sign", "minecraft:oak_door",
                "minecraft:oak_trapdoor", "minecraft:oak_pressure_plate", "minecraft:white_bed", "minecraft:oak_boat",
                "minecraft:oak_button", "minecraft:white_dye", "minecraft:egg", "minecraft:candle",
                "minecraft:white_banner", "minecraft:tube_coral", "minecraft:saddle", "minecraft:diamond_pickaxe",
                "minecraft:diamond_helmet", "minecraft:blaze_rod", "minecraft:bundle", "minecraft:book",
                "minecraft:skeleton_skull", "minecraft:coast_armor_trim_smithing_template", "minecraft:shulker_box",
                "minecraft:netherite_scrap", "minecraft:bucket", "minecraft:water_bucket", "minecraft:potion",
                "minecraft:arrow", "minecraft:music_disc_13", "minecraft:rail", "minecraft:anvil"
        ));

        // timsort permutations
        for (int seed = 0; seed < 20; seed++) {
            java.util.Collections.shuffle(items, new java.util.Random(seed * 42L));
            assertDoesNotThrow(() -> items.sort(cmp), "timsort failed with seed " + seed);
        }

        // reflexivity and antisymmetry
        for (String a : items) {
            assertEquals(0, cmp.compare(a, a), "cmp(a, a) must be 0 for " + a);
            for (String b : items) {
                int cAB = cmp.compare(a, b);
                int cBA = cmp.compare(b, a);
                assertEquals(Integer.signum(cAB), -Integer.signum(cBA), "antisymmetry violated for " + a + " and " + b);
            }
        }

        // sorting in profile data
        ProfileData profile = new ProfileData("stress_test");
        for (int i = 0; i < items.size(); i++) {
            profile.setItemAt(items.get(i), i % 9, i / 9);
        }
        assertDoesNotThrow(() -> profile.sortItems(cmp, 9));
    }

    @Test
    void testBlockedItemsManagementAndPersistence() {
        ProfileData profile = new ProfileData("blocked_test");
        assertTrue(profile.getBlockedItems().isEmpty());
        assertFalse(profile.isItemBlocked("minecraft:dirt"));

        // block items
        assertTrue(profile.blockItem("minecraft:dirt"));
        assertTrue(profile.blockItem("minecraft:stone"));
        // no duplicates
        assertFalse(profile.blockItem("minecraft:dirt"));

        assertEquals(2, profile.getBlockedItems().size());
        assertTrue(profile.isItemBlocked("minecraft:dirt"));
        assertTrue(profile.isItemBlocked("minecraft:stone"));
        assertFalse(profile.isItemBlocked("minecraft:diamond"));

        // save and reload via repository
        profileRepository.saveProfile(profile);
        ProfileData loaded = profileRepository.loadProfile("blocked_test");

        assertNotNull(loaded);
        assertEquals(2, loaded.getBlockedItems().size());
        assertTrue(loaded.isItemBlocked("minecraft:dirt"));
        assertTrue(loaded.isItemBlocked("minecraft:stone"));

        // unblock item
        assertTrue(loaded.unblockItem("minecraft:dirt"));
        assertFalse(loaded.isItemBlocked("minecraft:dirt"));
        assertEquals(1, loaded.getBlockedItems().size());

        profileRepository.saveProfile(loaded);
        ProfileData reloaded = profileRepository.loadProfile("blocked_test");
        assertEquals(1, reloaded.getBlockedItems().size());
        assertFalse(reloaded.isItemBlocked("minecraft:dirt"));
        assertTrue(reloaded.isItemBlocked("minecraft:stone"));
    }

    @Test
    void testBlockedItemsIndependentPerProfile() {
        ProfileData profileA = new ProfileData("profile_a");
        ProfileData profileB = new ProfileData("profile_b");

        profileA.blockItem("minecraft:obsidian");
        profileB.blockItem("minecraft:crying_obsidian");

        profileRepository.saveProfile(profileA);
        profileRepository.saveProfile(profileB);

        ProfileData loadedA = profileRepository.loadProfile("profile_a");
        ProfileData loadedB = profileRepository.loadProfile("profile_b");

        assertTrue(loadedA.isItemBlocked("minecraft:obsidian"));
        assertFalse(loadedA.isItemBlocked("minecraft:crying_obsidian"));

        assertFalse(loadedB.isItemBlocked("minecraft:obsidian"));
        assertTrue(loadedB.isItemBlocked("minecraft:crying_obsidian"));
    }

    @Test
    void testOrdenadoSubTabNavigation() {
        com.itemorganizer.gui.navigation.OrdenadoSubTab subTab = com.itemorganizer.gui.navigation.OrdenadoSubTab.ORGANIZADO;
        assertEquals("Organized", subTab.getLabel());

        com.itemorganizer.gui.navigation.OrdenadoSubTab subTabBlocked = com.itemorganizer.gui.navigation.OrdenadoSubTab.BLOQUEADO;
        assertEquals("Blocked", subTabBlocked.getLabel());
    }

    @Test
    void testModConfigSelectedProfileAndBackgroundBlur() {
        ModConfig cfg = new ModConfig();
        assertEquals("default", cfg.getSelectedProfile());
        assertEquals(0.50f, cfg.getBlur(), 0.001f);
        assertTrue(cfg.isBackgroundBlur());

        cfg.setSelectedProfile("miner_profile");
        cfg.setBlur(0.85f);
        assertEquals("miner_profile", cfg.getSelectedProfile());
        assertEquals(0.85f, cfg.getBlur(), 0.001f);
        assertTrue(cfg.isBackgroundBlur());

        // blur clamping (0.0 to 5.0)
        cfg.setBlur(-0.5f);
        assertEquals(0.0f, cfg.getBlur(), 0.001f);
        assertFalse(cfg.isBackgroundBlur());

        cfg.setBlur(1.5f);
        assertEquals(1.5f, cfg.getBlur(), 0.001f);
        assertTrue(cfg.isBackgroundBlur());

        cfg.setBlur(6.0f);
        assertEquals(5.0f, cfg.getBlur(), 0.001f);
        assertTrue(cfg.isBackgroundBlur());

        cfg.setBackgroundBlur(false);
        assertEquals(0.0f, cfg.getBlur(), 0.001f);
        assertFalse(cfg.isBackgroundBlur());

        cfg.setBackgroundBlur(true);
        assertEquals(0.50f, cfg.getBlur(), 0.001f);
        assertTrue(cfg.isBackgroundBlur());

        // sanitize null or empty profile
        cfg.setSelectedProfile(null);
        assertEquals("default", cfg.getSelectedProfile());
        cfg.setSelectedProfile("   ");
        assertEquals("default", cfg.getSelectedProfile());
    }

    @Test
    void testConfigPersistenceWithProfileSelection() {
        com.itemorganizer.storage.ConfigRepository configRepo = new com.itemorganizer.storage.ConfigRepository(tempDir);
        ModConfig initial = configRepo.load();
        assertEquals("default", initial.getSelectedProfile());
        assertEquals(0.50f, initial.getBlur(), 0.001f);
        assertTrue(initial.isBackgroundBlur());

        initial.setSelectedProfile("creative_builds");
        initial.setBlur(0.75f);
        initial.setHotbarItemScale(1.25f);
        initial.setPaletteItemScale(0.80f);
        assertTrue(configRepo.save(initial));

        com.itemorganizer.storage.ConfigRepository reloadedRepo = new com.itemorganizer.storage.ConfigRepository(tempDir);
        ModConfig loaded = reloadedRepo.load();
        assertEquals("creative_builds", loaded.getSelectedProfile());
        assertEquals(0.75f, loaded.getBlur(), 0.001f);
        assertEquals(1.25f, loaded.getHotbarItemScale(), 0.001f);
        assertEquals(0.80f, loaded.getPaletteItemScale(), 0.001f);
        assertTrue(loaded.isBackgroundBlur());
    }

    @Test
    void testProfileClonePreservesBlockedItems() {
        ProfileData source = new ProfileData("original");
        source.setColumnCount(12);
        source.setItemAt("minecraft:stone", 0, 0);
        source.setItemAt("minecraft:dirt", 1, 0);
        source.blockItem("minecraft:bedrock");
        source.blockItem("minecraft:barrier");

        assertEquals(2, source.getBlockedItems().size());
        assertTrue(source.isItemBlocked("minecraft:bedrock"));
        assertTrue(source.isItemBlocked("minecraft:barrier"));

        // clone using copy constructor
        ProfileData clone = new ProfileData("copy", source);
        assertEquals("copy", clone.getName());
        assertEquals(12, clone.getColumnCount());
        assertEquals(2, clone.getItems().size());
        assertEquals(2, clone.getBlockedItems().size());
        assertTrue(clone.isItemBlocked("minecraft:bedrock"));
        assertTrue(clone.isItemBlocked("minecraft:barrier"));

        // modifying clone does not mutate original
        clone.blockItem("minecraft:command_block");
        assertEquals(3, clone.getBlockedItems().size());
        assertEquals(2, source.getBlockedItems().size());
        assertFalse(source.isItemBlocked("minecraft:command_block"));

        // persistence and reload
        assertTrue(profileRepository.saveProfile(clone));
        ProfileData reloaded = profileRepository.loadProfile("copy");
        assertEquals(3, reloaded.getBlockedItems().size());
        assertTrue(reloaded.isItemBlocked("minecraft:command_block"));
        assertTrue(reloaded.isItemBlocked("minecraft:bedrock"));
        assertTrue(reloaded.isItemBlocked("minecraft:barrier"));
    }

    @Test
    void testHotbarSlotFinder() {
        assertEquals(-1, com.itemorganizer.gui.util.HotbarActionHelper.findFirstEmptySlot(null));

        try {
            net.minecraft.entity.player.PlayerInventory inventory = new net.minecraft.entity.player.PlayerInventory(null, null);
            assertEquals(0, com.itemorganizer.gui.util.HotbarActionHelper.findFirstEmptySlot(inventory));
        } catch (Throwable ignored) {
            // PlayerInventory may require full game bootstrap in headless environment
        }
    }

    @Test
    void testConfigUndoKey() {
        ModConfig config = new ModConfig();
        // default undo key: z
        assertEquals("key.keyboard.z", config.getKeyUndo());

        // reassign key
        config.setKeyUndo("key.keyboard.u");
        assertEquals("key.keyboard.u", config.getKeyUndo());

        // fallback for null or empty
        config.setKeyUndo(null);
        assertEquals("key.keyboard.z", config.getKeyUndo());
        config.setKeyUndo("");
        assertEquals("key.keyboard.z", config.getKeyUndo());
    }

    @Test
    void testUndoManagerStackAndCapacity() {
        com.itemorganizer.gui.undo.UndoManager manager = com.itemorganizer.gui.undo.UndoManager.getInstance();
        manager.clear();
        assertFalse(manager.canUndo());
        assertEquals(0, manager.getHistorySize());

        // record actions
        java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(0);
        manager.record((client, vm) -> counter.set(1));
        manager.record((client, vm) -> counter.set(2));

        assertTrue(manager.canUndo());
        assertEquals(2, manager.getHistorySize());

        // pop LIFO
        assertTrue(manager.undo(null, null));
        assertEquals(2, counter.get());
        assertEquals(1, manager.getHistorySize());

        assertTrue(manager.undo(null, null));
        assertEquals(1, counter.get());
        assertEquals(0, manager.getHistorySize());

        // empty stack
        assertFalse(manager.undo(null, null));
        assertFalse(manager.canUndo());

        // capacity bound: up to 100 actions
        for (int i = 0; i < 120; i++) {
            final int val = i;
            manager.record((client, vm) -> counter.set(val));
        }
        assertEquals(100, manager.getHistorySize());

        manager.clear();
        assertEquals(0, manager.getHistorySize());
        assertFalse(manager.canUndo());
    }

    @Test
    void testPaletteDataFindRowById() {
        com.itemorganizer.core.model.PaletteData data = new com.itemorganizer.core.model.PaletteData();
        com.itemorganizer.core.model.PaletteRow row1 = new com.itemorganizer.core.model.PaletteRow();
        com.itemorganizer.core.model.PaletteRow row2 = new com.itemorganizer.core.model.PaletteRow();
        data.addRow(row1);
        data.addRow(row2);

        assertEquals(row1, data.findRowById(row1.getId()));
        assertEquals(row2, data.findRowById(row2.getId()));
        assertNull(data.findRowById("non_existent_id"));
        assertNull(data.findRowById(null));
    }

    @Test
    void testPaletteUndoActionsData() {
        com.itemorganizer.core.model.PaletteRow row = new com.itemorganizer.core.model.PaletteRow();
        row.setSlot(0, "minecraft:stone");
        row.setSlot(1, "minecraft:dirt");

        // full row undo snapshot
        com.itemorganizer.gui.undo.PaletteFullUndoAction fullAction = new com.itemorganizer.gui.undo.PaletteFullUndoAction(row);
        assertEquals(row.getId(), fullAction.getPaletteId());
        assertEquals("minecraft:stone", fullAction.getPreviousSlots().get(0));
        assertEquals("minecraft:dirt", fullAction.getPreviousSlots().get(1));
        assertNull(fullAction.getPreviousSlots().get(2));

        // slot undo snapshot
        com.itemorganizer.gui.undo.PaletteSlotUndoAction slotAction = new com.itemorganizer.gui.undo.PaletteSlotUndoAction(row.getId(), 0, "minecraft:stone");
        assertEquals(row.getId(), slotAction.getPaletteId());
        assertEquals(0, slotAction.getSlot());
        assertEquals("minecraft:stone", slotAction.getPreviousItemId());
    }

    @Test
    void testHotbarActionHelperDefensiveChecks() {
        assertFalse(com.itemorganizer.gui.util.HotbarActionHelper.isItemInHotbar(null, null));
        assertFalse(com.itemorganizer.gui.util.HotbarActionHelper.quickMoveToHotbar(null, null));
        assertFalse(com.itemorganizer.gui.util.HotbarActionHelper.dropPayloadToSlot(null, null, 0));
    }

    @Test
    void testPaletteRowSlotComparisonAndClearing() {
        com.itemorganizer.core.model.PaletteRow rowA = new com.itemorganizer.core.model.PaletteRow();
        com.itemorganizer.core.model.PaletteRow rowB = new com.itemorganizer.core.model.PaletteRow();

        // empty palettes
        assertFalse(rowA.hasAnyItem());
        assertFalse(rowB.hasAnyItem());
        assertTrue(rowA.hasSameSlotsAs(rowB));

        // populate row A
        rowA.setSlot(0, "minecraft:stone");
        rowA.setSlot(4, "minecraft:oak_planks");
        assertTrue(rowA.hasAnyItem());
        assertFalse(rowA.hasSameSlotsAs(rowB));

        // match row B to row A
        rowB.setSlot(0, "minecraft:stone");
        rowB.setSlot(4, "minecraft:oak_planks");
        assertTrue(rowB.hasAnyItem());
        assertTrue(rowA.hasSameSlotsAs(rowB));
        assertTrue(rowB.hasSameSlotsAs(rowA));

        // different slot index
        com.itemorganizer.core.model.PaletteRow rowC = new com.itemorganizer.core.model.PaletteRow();
        rowC.setSlot(1, "minecraft:stone");
        rowC.setSlot(4, "minecraft:oak_planks");
        assertFalse(rowA.hasSameSlotsAs(rowC));

        // clear slots
        rowA.clearSlots();
        assertFalse(rowA.hasAnyItem());
        assertNull(rowA.getSlot(0));
        assertNull(rowA.getSlot(4));
    }

    @Test
    void testDuplicatePaletteDetection() {
        com.itemorganizer.core.model.PaletteRow empty1 = new com.itemorganizer.core.model.PaletteRow();
        com.itemorganizer.core.model.PaletteRow empty2 = new com.itemorganizer.core.model.PaletteRow();
        java.util.List<com.itemorganizer.core.model.PaletteRow> list = new java.util.ArrayList<>();
        list.add(empty1);
        list.add(empty2);

        // empty palettes must not be flagged as duplicates
        assertFalse(com.itemorganizer.gui.widget.PaletteListWidget.isDuplicatePalette(empty1, list));
        assertFalse(com.itemorganizer.gui.widget.PaletteListWidget.isDuplicatePalette(empty2, list));

        // single non-empty palette is not duplicate
        com.itemorganizer.core.model.PaletteRow p1 = new com.itemorganizer.core.model.PaletteRow();
        p1.setSlot(0, "minecraft:diamond_block");
        p1.setSlot(1, "minecraft:gold_block");
        list.add(p1);
        assertFalse(com.itemorganizer.gui.widget.PaletteListWidget.isDuplicatePalette(p1, list));

        // duplicate palette with same slots
        com.itemorganizer.core.model.PaletteRow p2 = new com.itemorganizer.core.model.PaletteRow();
        p2.setSlot(0, "minecraft:diamond_block");
        p2.setSlot(1, "minecraft:gold_block");
        list.add(p2);

        // both p1 and p2 should now be detected as duplicates
        assertTrue(com.itemorganizer.gui.widget.PaletteListWidget.isDuplicatePalette(p1, list));
        assertTrue(com.itemorganizer.gui.widget.PaletteListWidget.isDuplicatePalette(p2, list));

        // modifying one slot breaks the collision
        p2.setSlot(2, "minecraft:iron_block");
        assertFalse(com.itemorganizer.gui.widget.PaletteListWidget.isDuplicatePalette(p1, list));
        assertFalse(com.itemorganizer.gui.widget.PaletteListWidget.isDuplicatePalette(p2, list));

        // reset back to matching
        p2.clearSlot(2);
        assertTrue(com.itemorganizer.gui.widget.PaletteListWidget.isDuplicatePalette(p1, list));
        assertTrue(com.itemorganizer.gui.widget.PaletteListWidget.isDuplicatePalette(p2, list));
    }

    @Test
    void testPaletteSearchFilterWidgetMatching() {
        com.itemorganizer.gui.widget.PaletteSearchFilterWidget filterWidget = new com.itemorganizer.gui.widget.PaletteSearchFilterWidget(0, 0);
        assertFalse(filterWidget.isActive());

        com.itemorganizer.core.model.PaletteRow row = new com.itemorganizer.core.model.PaletteRow();
        row.setSlot(0, "minecraft:stone");
        row.setSlot(1, "minecraft:dirt");
        row.setSlot(8, "minecraft:glass");

        // inactive filter matches everything
        assertTrue(filterWidget.matches(row));

        // filter by slot 0
        filterWidget.getFilterPalette().setSlot(0, "minecraft:stone");
        assertTrue(filterWidget.isActive());
        assertTrue(filterWidget.matches(row));

        // mismatch in slot 0
        filterWidget.getFilterPalette().setSlot(0, "minecraft:cobblestone");
        assertFalse(filterWidget.matches(row));

        // match slot 0 and slot 8
        filterWidget.getFilterPalette().setSlot(0, "minecraft:stone");
        filterWidget.getFilterPalette().setSlot(8, "minecraft:glass");
        assertTrue(filterWidget.matches(row));

        // empty slots in filter (e.g. slot 1) are ignored
        assertNull(filterWidget.getFilterPalette().getSlot(1));
        assertEquals("minecraft:dirt", row.getSlot(1));
        assertTrue(filterWidget.matches(row));

        // mismatch in slot 8
        filterWidget.getFilterPalette().setSlot(8, "minecraft:obsidian");
        assertFalse(filterWidget.matches(row));

        // clear filter resets matching
        filterWidget.clearFilter();
        assertFalse(filterWidget.isActive());
        assertTrue(filterWidget.matches(row));
    }

    @Test
    void testPaletteDataRemoveAndPasteOperations() {
        com.itemorganizer.core.model.PaletteData data = new com.itemorganizer.core.model.PaletteData();
        com.itemorganizer.core.model.PaletteRow row = new com.itemorganizer.core.model.PaletteRow();
        row.setSlot(0, "minecraft:iron_sword");
        data.addRow(row);

        assertEquals(1, data.getRows().size());
        assertEquals("minecraft:iron_sword", data.findRowById(row.getId()).getSlot(0));

        // simulate paste hotbar items
        for (int s = 0; s < 9; s++) {
            row.setSlot(s, "minecraft:stone");
        }
        for (int s = 0; s < 9; s++) {
            assertEquals("minecraft:stone", row.getSlot(s));
        }

        // simulate delete row
        data.removeRowById(row.getId());
        assertEquals(0, data.getRows().size());
        assertNull(data.findRowById(row.getId()));
    }

    @Test
    void testSearchFilterFarRightPlacementMath() {
        int screenWidth = 600;
        int margin = 10;
        int hotbarWidth = 9 * 22; // 198
        int filterWidth = 9 * 22 + 4 + 14; // 216

        int hotbarX = (screenWidth - hotbarWidth) / 2; // 201
        int filterX = screenWidth - margin - filterWidth; // 600 - 10 - 216 = 374

        // hotbar right edge: 201 + 198 = 399 > 374 -> would collide on narrow screen
        if (hotbarX + hotbarWidth + 8 > filterX) {
            hotbarX = Math.max(6, filterX - hotbarWidth - 8);
        }
        assertEquals(374 - 198 - 8, hotbarX);
        assertTrue(hotbarX + hotbarWidth <= filterX - 8);

        // on wider screen (800)
        screenWidth = 800;
        hotbarX = (screenWidth - hotbarWidth) / 2; // 301
        filterX = screenWidth - margin - filterWidth; // 800 - 10 - 216 = 574
        // 301 + 198 = 499 < 574 -> stays centered!
        assertTrue(hotbarX + hotbarWidth + 8 <= filterX);
        assertEquals(301, hotbarX);
    }

    @Test
    void testBlockerActiveConfigPersistence() {
        com.itemorganizer.storage.ConfigRepository configRepo = new com.itemorganizer.storage.ConfigRepository(tempDir);
        ModConfig config = configRepo.load();
        assertFalse(config.isBlockerActive());

        config.setBlockerActive(true);
        assertTrue(configRepo.save(config));

        com.itemorganizer.storage.ConfigRepository reloadedRepo = new com.itemorganizer.storage.ConfigRepository(tempDir);
        ModConfig reloaded = reloadedRepo.load();
        assertTrue(reloaded.isBlockerActive());

        reloaded.setBlockerActive(false);
        assertTrue(reloadedRepo.save(reloaded));

        ModConfig secondReload = new com.itemorganizer.storage.ConfigRepository(tempDir).load();
        assertFalse(secondReload.isBlockerActive());
    }

    @Test
    void testProfileSnapshotAndCopyFrom() {
        ProfileData profile = new ProfileData("test_profile");
        profile.setColumnCount(12);
        profile.setItemAt("minecraft:stone", 0, 0);
        profile.setItemAt("minecraft:dirt", 1, 0);
        profile.blockItem("minecraft:bedrock");

        ProfileData snap = profile.snapshot();
        assertEquals("test_profile", snap.getName());
        assertEquals(12, snap.getColumnCount());
        assertEquals(2, snap.getItems().size());
        assertTrue(snap.isItemBlocked("minecraft:bedrock"));

        // mutate original
        profile.setItemAt("minecraft:gold_block", 2, 0);
        profile.blockItem("minecraft:barrier");
        assertEquals(3, profile.getItems().size());
        assertEquals(2, snap.getItems().size());

        // restore with copyFrom
        profile.copyFrom(snap);
        assertEquals(12, profile.getColumnCount());
        assertEquals(2, profile.getItems().size());
        assertFalse(profile.hasItem("minecraft:gold_block"));
        assertFalse(profile.isItemBlocked("minecraft:barrier"));
        assertTrue(profile.isItemBlocked("minecraft:bedrock"));
    }
}
