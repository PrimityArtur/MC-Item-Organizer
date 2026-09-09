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

