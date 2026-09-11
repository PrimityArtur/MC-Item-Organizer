package com.itemorganizer.gui.util;

import com.itemorganizer.core.model.ItemCategory;
import com.itemorganizer.core.model.VersionCatalog;
import com.itemorganizer.storage.VersionCatalogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestBlockItemTest {

    @Test
    void testTestBlockAndOperatorCategorization() {
        assertEquals(ItemCategory.COMMAND_BLOCKS, ItemCategory.getCategory("minecraft:test_block[mode=start]"));
        assertEquals(ItemCategory.COMMAND_BLOCKS, ItemCategory.getCategory("minecraft:test_block[mode=log]"));
        assertEquals(ItemCategory.COMMAND_BLOCKS, ItemCategory.getCategory("minecraft:test_block[mode=fail]"));
        assertEquals(ItemCategory.COMMAND_BLOCKS, ItemCategory.getCategory("minecraft:test_block[mode=accept]"));
        assertEquals(ItemCategory.COMMAND_BLOCKS, ItemCategory.getCategory("minecraft:test_instance_block"));
        assertEquals(ItemCategory.COMMAND_BLOCKS, ItemCategory.getCategory("minecraft:light[level=15]"));
        assertEquals(ItemCategory.COMMAND_BLOCKS, ItemCategory.getCategory("minecraft:light"));
        assertEquals(ItemCategory.COMMAND_BLOCKS, ItemCategory.getCategory("minecraft:debug_stick"));
    }

    @Test
    void testTestBlockVariationColors() {
        assertEquals(0x29A5D6, ItemColorHelper.getItemColor("minecraft:test_block[mode=start]"));
        assertEquals(0xE8A825, ItemColorHelper.getItemColor("minecraft:test_block[mode=log]"));
        assertEquals(0xDE3226, ItemColorHelper.getItemColor("minecraft:test_block[mode=fail]"));
        assertEquals(0x38BD4C, ItemColorHelper.getItemColor("minecraft:test_block[mode=accept]"));
    }

    @Test
    void testTestInstanceBlockColor() {
        ItemColorHelper.loadDefaultAndConfigColors();
        assertEquals(0x7E7876, ItemColorHelper.getItemColor("minecraft:test_instance_block"));
        assertEquals(0xA12723, ItemColorHelper.getItemColor("minecraft:red_wool"));
    }

    @Test
    void testVersionCatalogContainsTestBlockVariations(@TempDir Path tempDir) {
        VersionCatalogRepository repo = new VersionCatalogRepository(tempDir);
        VersionCatalog catalog = repo.load();

        List<String> list21_5 = catalog.getItemsForVersion("1.21.5");
        assertNotNull(list21_5);
        assertTrue(list21_5.contains("minecraft:test_block[mode=start]"));
        assertTrue(list21_5.contains("minecraft:test_block[mode=log]"));
        assertTrue(list21_5.contains("minecraft:test_block[mode=fail]"));
        assertTrue(list21_5.contains("minecraft:test_block[mode=accept]"));
        assertTrue(list21_5.contains("minecraft:test_instance_block"));
    }
}
