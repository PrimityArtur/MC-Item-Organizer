package com.itemorganizer.profile;

import com.itemorganizer.core.model.ItemSlotPosition;
import com.itemorganizer.core.model.ModConfig;
import com.itemorganizer.core.model.ProfileData;
import com.itemorganizer.storage.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
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
        // category sequence checks: full blocks, logs&stem, ores, glazed, grate, bulb, tools redstone, working stations, etc.
        assertEquals(com.itemorganizer.core.model.ItemCategory.FULL_BLOCKS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:stone"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.FULL_BLOCKS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:white_concrete"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.FULL_BLOCKS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:oak_planks"));

        // requested new sections
        assertEquals(com.itemorganizer.core.model.ItemCategory.LOGS_AND_STEMS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:oak_log"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.LOGS_AND_STEMS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:stripped_oak_log"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.LOGS_AND_STEMS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:warped_stem"));

        assertEquals(com.itemorganizer.core.model.ItemCategory.ORES, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:iron_ore"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.ORES, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:deepslate_diamond_ore"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.ORES, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:ancient_debris"));

        assertEquals(com.itemorganizer.core.model.ItemCategory.GLAZED_TERRACOTTA, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:white_glazed_terracotta"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.GLAZED_TERRACOTTA, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:magenta_glazed_terracotta"));

        assertEquals(com.itemorganizer.core.model.ItemCategory.GRATES, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:copper_grate"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.GRATES, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:waxed_exposed_copper_grate"));

        assertEquals(com.itemorganizer.core.model.ItemCategory.BULBS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:copper_bulb"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.BULBS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:waxed_oxidized_copper_bulb"));

        assertEquals(com.itemorganizer.core.model.ItemCategory.TOOLS_REDSTONE, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:redstone"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.TOOLS_REDSTONE, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:repeater"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.TOOLS_REDSTONE, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:comparator"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.TOOLS_REDSTONE, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:piston"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.TOOLS_REDSTONE, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:observer"));

        assertEquals(com.itemorganizer.core.model.ItemCategory.WORKING_STATIONS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:crafting_table"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.WORKING_STATIONS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:furnace"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.WORKING_STATIONS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:stonecutter"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.WORKING_STATIONS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:anvil"));

        assertEquals(com.itemorganizer.core.model.ItemCategory.UNCATEGORIZED_VEGETATION, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:fern"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.UNCATEGORIZED_VEGETATION, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:short_grass"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.UNCATEGORIZED_VEGETATION, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:vine"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.UNCATEGORIZED_VEGETATION, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:kelp"));

        assertEquals(com.itemorganizer.core.model.ItemCategory.CROPS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:wheat"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.CROPS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:carrot"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.CROPS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:potato"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.CROPS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:pumpkin_seeds"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.CROPS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:nether_wart"));

        assertEquals(com.itemorganizer.core.model.ItemCategory.FLOWERS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:dandelion"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.FLOWERS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:poppy"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.FLOWERS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:blue_orchid"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.FLOWERS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:sunflower"));

        assertEquals(com.itemorganizer.core.model.ItemCategory.TORCH, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:torch"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.TORCH, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:soul_torch"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.TORCH, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:copper_torch"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.TORCH, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:redstone_torch"));

        // additional standard categories
        assertEquals(com.itemorganizer.core.model.ItemCategory.GLASS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:glass"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.GLASS_PANES, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:glass_pane"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.BARS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:iron_bars"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.CHAINS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:chain"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.FENCES, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:oak_fence"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.WALLS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:cobblestone_wall"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.SLABS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:oak_slab"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.FENCE_GATES, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:oak_fence_gate"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.STAIRS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:oak_stairs"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.SAPLINGS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:oak_sapling"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.LEAVES, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:oak_leaves"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.CARPETS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:white_carpet"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.LANTERNS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:lantern"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.SHELVES, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:bookshelf"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.SIGNS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:oak_sign"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.DOORS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:oak_door"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.TRAPDOORS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:oak_trapdoor"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.PRESSURE_PLATES, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:oak_pressure_plate"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.BEDS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:white_bed"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.BOATS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:oak_boat"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.BUTTONS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:oak_button"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.DYES, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:white_dye"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.SPAWN_EGGS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:egg"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.CANDLES, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:candle"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.BANNERS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:white_banner"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.CORALS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:tube_coral"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.HARNESS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:saddle"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.TOOLS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:diamond_pickaxe"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.ARMOR, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:diamond_helmet"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.RODS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:blaze_rod"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.BUNDLES, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:bundle"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.BOOKS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:book"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.SHERDS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:angler_pottery_sherd"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.HEADS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:skeleton_skull"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.TRIMS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:netherite_upgrade_smithing_template"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.SHULKER_BOXES, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:shulker_box"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.CHESTS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:chest"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.MINECARTS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:minecart"));

        // sequence order verification (Full Blocks < Logs < Ores < Glazed < Grates < Bulbs < ToolsRedstone < WorkingStations < Veg < Crops < Flowers < Torch)
        int orderFullBlocks = com.itemorganizer.core.model.ItemCategory.getOrder(com.itemorganizer.core.model.ItemCategory.FULL_BLOCKS);
        int orderLogs = com.itemorganizer.core.model.ItemCategory.getOrder(com.itemorganizer.core.model.ItemCategory.LOGS_AND_STEMS);
        int orderOres = com.itemorganizer.core.model.ItemCategory.getOrder(com.itemorganizer.core.model.ItemCategory.ORES);
        int orderGlazed = com.itemorganizer.core.model.ItemCategory.getOrder(com.itemorganizer.core.model.ItemCategory.GLAZED_TERRACOTTA);
        int orderGrates = com.itemorganizer.core.model.ItemCategory.getOrder(com.itemorganizer.core.model.ItemCategory.GRATES);
        int orderBulbs = com.itemorganizer.core.model.ItemCategory.getOrder(com.itemorganizer.core.model.ItemCategory.BULBS);
        int orderRedstone = com.itemorganizer.core.model.ItemCategory.getOrder(com.itemorganizer.core.model.ItemCategory.TOOLS_REDSTONE);
        int orderWorkstations = com.itemorganizer.core.model.ItemCategory.getOrder(com.itemorganizer.core.model.ItemCategory.WORKING_STATIONS);
        int orderVeg = com.itemorganizer.core.model.ItemCategory.getOrder(com.itemorganizer.core.model.ItemCategory.UNCATEGORIZED_VEGETATION);
        int orderCrops = com.itemorganizer.core.model.ItemCategory.getOrder(com.itemorganizer.core.model.ItemCategory.CROPS);
        int orderFlowers = com.itemorganizer.core.model.ItemCategory.getOrder(com.itemorganizer.core.model.ItemCategory.FLOWERS);
        int orderTorch = com.itemorganizer.core.model.ItemCategory.getOrder(com.itemorganizer.core.model.ItemCategory.TORCH);

        assertTrue(orderFullBlocks < orderLogs);
        assertTrue(orderLogs < orderOres);
        assertTrue(orderOres < orderGlazed);
        assertTrue(orderGlazed < orderGrates);
        assertTrue(orderGrates < orderBulbs);
        assertTrue(orderBulbs < orderRedstone);
        assertTrue(orderRedstone < orderWorkstations);
        assertTrue(orderWorkstations < orderVeg);
        assertTrue(orderVeg < orderCrops);
        assertTrue(orderCrops < orderFlowers);
        assertTrue(orderFlowers < orderTorch);

        // test dynamic reordering capability
        List<com.itemorganizer.core.model.ItemCategory> customOrder = new ArrayList<>(com.itemorganizer.core.model.ItemCategory.DEFAULT_ORDER);
        customOrder.remove(com.itemorganizer.core.model.ItemCategory.TORCH);
        customOrder.add(0, com.itemorganizer.core.model.ItemCategory.TORCH); // move Torches to top
        com.itemorganizer.core.model.ItemCategory.setOrder(customOrder);

        assertEquals(1, com.itemorganizer.core.model.ItemCategory.getOrder(com.itemorganizer.core.model.ItemCategory.TORCH));
        assertEquals(1, com.itemorganizer.gui.util.ItemColorHelper.getItemCategoryOrder("minecraft:torch"));
        assertTrue(com.itemorganizer.gui.util.ItemColorHelper.getItemCategoryOrder("minecraft:stone") > 1);

        // reset back to default
        com.itemorganizer.core.model.ItemCategory.resetToDefaultOrder();
        assertEquals(com.itemorganizer.core.model.ItemCategory.SHERDS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:skull_pottery_sherd"));

        // verify strict relative order with comparator
        java.util.Comparator<String> cmp = com.itemorganizer.gui.util.ItemColorHelper.getColorComparator();

        String[] categorySamples = new String[]{
                "minecraft:stone",                          // 1. full blocks
                "minecraft:oak_log",                        // 2. logs&stem
                "minecraft:iron_ore",                       // 3. ores
                "minecraft:white_glazed_terracotta",        // 4. glazed
                "minecraft:copper_grate",                   // 5. grate
                "minecraft:copper_bulb",                    // 6. bulb
                "minecraft:redstone",                       // 7. tools redstone
                "minecraft:crafting_table",                 // 8. workstations
                "minecraft:fern",                           // 9. veg
                "minecraft:wheat",                          // 10. crops
                "minecraft:dandelion",                      // 11. flowers
                "minecraft:torch",                          // 12. torch
                "minecraft:glass",                          // 13. glass
                "minecraft:glass_pane",                     // 14. glass_pane
                "minecraft:iron_bars",                      // 15. bars
                "minecraft:chain",                          // 16. chain
                "minecraft:oak_fence",                      // 17. fence
                "minecraft:cobblestone_wall",               // 18. wall
                "minecraft:oak_slab",                       // 19. slab
                "minecraft:oak_fence_gate",                 // 20. fence_gate
                "minecraft:oak_stairs",                     // 21. stairs
                "minecraft:oak_sapling",                    // 22. saplings
                "minecraft:oak_leaves",                     // 23. leaves
                "minecraft:white_carpet",                   // 24. carpets
                "minecraft:lantern",                        // 25. lantern
                "minecraft:bookshelf",                      // 26. shelf
                "minecraft:oak_sign",                       // 27. sign
                "minecraft:oak_door",                       // 28. door
                "minecraft:oak_trapdoor",                   // 29. trapdoor
                "minecraft:oak_pressure_plate",             // 30. pressure_plate
                "minecraft:white_bed",                      // 31. bed
                "minecraft:oak_boat",                       // 32. boat
                "minecraft:oak_button",                     // 33. button
                "minecraft:white_dye",                      // 34. dye
                "minecraft:egg",                            // 35. egg
                "minecraft:candle",                         // 36. candle
                "minecraft:white_banner",                   // 37. banner
                "minecraft:tube_coral",                     // 38. coral
                "minecraft:saddle",                         // 39. harness
                "minecraft:diamond_pickaxe",                // 40. tools
                "minecraft:diamond_helmet",                 // 41. armor
                "minecraft:blaze_rod",                      // 42. rod
                "minecraft:bundle",                         // 43. bundle
                "minecraft:book",                           // 44. book
                "minecraft:angler_pottery_sherd",           // 45. sherd
                "minecraft:skeleton_skull",                 // 46. heads
                "minecraft:coast_armor_trim_smithing_template", // 47. trim
                "minecraft:shulker_box",                    // 48. shulker
                "minecraft:netherite_scrap",                // 49. scrap
                "minecraft:bucket",                         // 50. bucket
                "minecraft:potion",                         // 51. potion
                "minecraft:arrow",                          // 52. arrow
                "minecraft:music_disc_13",                  // 53. disc
                "minecraft:rail",                           // 54. rail
                "minecraft:chest",                          // 55. chest
                "minecraft:minecart",                       // 56. minecart
                "minecraft:copper_golem_statue",            // 57. golem
                "minecraft:apple"                           // 58. misc
        };

        for (int i = 0; i < categorySamples.length - 1; i++) {
            assertTrue(cmp.compare(categorySamples[i], categorySamples[i + 1]) < 0,
                    "order mismatch: " + categorySamples[i] + " should precede " + categorySamples[i + 1]);
        }
    }

    @Test
    void testColorSortingWithinSameCategory() {
        java.util.Comparator<String> cmp = com.itemorganizer.gui.util.ItemColorHelper.getColorComparator();

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
        assertTrue(configRepo.save(initial));

        com.itemorganizer.storage.ConfigRepository reloadedRepo = new com.itemorganizer.storage.ConfigRepository(tempDir);
        ModConfig loaded = reloadedRepo.load();
        assertEquals("creative_builds", loaded.getSelectedProfile());
        assertEquals(0.75f, loaded.getBlur(), 0.001f);
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
}

