package com.itemorganizer.palette;

import com.itemorganizer.core.model.PaletteRow;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PaletteInfiniteSlotLogicTest {

    @Test
    void testInitialSlotCount() {
        PaletteRow row = new PaletteRow();
        assertEquals(9, row.getSlotCount());
        assertFalse(row.updateInfiniteSlots());
        assertEquals(9, row.getSlotCount());
    }

    @Test
    void testExpansionWhenLastSlotOccupied() {
        PaletteRow row = new PaletteRow();
        assertEquals(9, row.getSlotCount());

        // placing item before the last slot does not expand
        row.setSlot(0, "minecraft:stone");
        row.setSlot(7, "minecraft:dirt");
        assertFalse(row.updateInfiniteSlots());
        assertEquals(9, row.getSlotCount());

        // placing item in the last slot (index 8) expands by +9
        row.setSlot(8, "minecraft:cobblestone");
        assertTrue(row.updateInfiniteSlots());
        assertEquals(18, row.getSlotCount());
        assertEquals("minecraft:cobblestone", row.getSlot(8));
        assertNull(row.getSlot(9));
        assertNull(row.getSlot(17));

        // placing item in the new last slot (index 17) expands again by +9
        row.setSlot(17, "minecraft:oak_planks");
        assertTrue(row.updateInfiniteSlots());
        assertEquals(27, row.getSlotCount());
        assertEquals("minecraft:oak_planks", row.getSlot(17));
        assertNull(row.getSlot(26));
    }

    @Test
    void testContractionWhenTriggerSlotClearedAndNewSlotsEmpty() {
        PaletteRow row = new PaletteRow();
        row.setSlot(8, "minecraft:stone");
        assertTrue(row.updateInfiniteSlots());
        assertEquals(18, row.getSlotCount());

        row.setSlot(17, "minecraft:dirt");
        assertTrue(row.updateInfiniteSlots());
        assertEquals(27, row.getSlotCount());

        // clear slot 17 when slots 18..26 are empty -> contracts to 18
        row.setSlot(17, null);
        assertTrue(row.updateInfiniteSlots());
        assertEquals(18, row.getSlotCount());

        // clear slot 8 when slots 9..17 are empty -> contracts to minimum 9
        row.setSlot(8, null);
        assertTrue(row.updateInfiniteSlots());
        assertEquals(9, row.getSlotCount());

        // cannot contract below 9
        assertFalse(row.updateInfiniteSlots());
        assertEquals(9, row.getSlotCount());
    }

    @Test
    void testNoContractionWhenAddedSlotsHaveItems() {
        PaletteRow row = new PaletteRow();
        row.setSlot(8, "minecraft:stone");
        row.updateInfiniteSlots();
        assertEquals(18, row.getSlotCount());

        // place item in slot 12 (in the second chunk)
        row.setSlot(12, "minecraft:iron_block");

        // clear slot 8: should not contract because slots 9..17 are not empty
        row.setSlot(8, null);
        assertFalse(row.updateInfiniteSlots());
        assertEquals(18, row.getSlotCount());

        // once slot 12 is also cleared and slot 8 is empty, contraction occurs
        row.setSlot(12, null);
        assertTrue(row.updateInfiniteSlots());
        assertEquals(9, row.getSlotCount());
    }

    @Test
    void testExpandAndShrinkDirectMethods() {
        PaletteRow row = new PaletteRow(9);
        row.expandSlots(9);
        assertEquals(18, row.getSlotCount());

        row.expandSlots(9);
        assertEquals(27, row.getSlotCount());

        row.shrinkSlots(9);
        assertEquals(18, row.getSlotCount());

        row.shrinkSlots(9);
        assertEquals(9, row.getSlotCount());

        // shrinking below 9 remains at minimum 9
        row.shrinkSlots(9);
        assertEquals(9, row.getSlotCount());
    }

    @Test
    void testAppendItemsToEmptyPalette() {
        PaletteRow row = new PaletteRow();
        assertEquals(9, row.getSlotCount());

        row.appendItems(java.util.List.of("minecraft:stone", "minecraft:dirt", "minecraft:oak_planks"));
        assertEquals(9, row.getSlotCount());
        assertEquals("minecraft:stone", row.getSlot(0));
        assertEquals("minecraft:dirt", row.getSlot(1));
        assertEquals("minecraft:oak_planks", row.getSlot(2));
        assertNull(row.getSlot(3));
    }

    @Test
    void testAppendItemsOverBoundaryExpandsPalette() {
        PaletteRow row = new PaletteRow();
        for (int i = 0; i < 6; i++) {
            row.setSlot(i, "minecraft:stone_" + i);
        }
        assertEquals(9, row.getSlotCount());

        java.util.List<String> hotbar = java.util.List.of(
                "minecraft:apple", "minecraft:bread", "minecraft:torch",
                "minecraft:arrow", "minecraft:bow", "minecraft:diamond"
        );
        row.appendItems(hotbar);

        assertEquals(18, row.getSlotCount());
        for (int i = 0; i < 6; i++) {
            assertEquals("minecraft:stone_" + i, row.getSlot(i));
        }
        assertEquals("minecraft:apple", row.getSlot(6));
        assertEquals("minecraft:bread", row.getSlot(7));
        assertEquals("minecraft:torch", row.getSlot(8));
        assertEquals("minecraft:arrow", row.getSlot(9));
        assertEquals("minecraft:bow", row.getSlot(10));
        assertEquals("minecraft:diamond", row.getSlot(11));
        assertNull(row.getSlot(12));
        assertNull(row.getSlot(17));
    }

    @Test
    void testAppendItemsTrailingNulls() {
        PaletteRow row = new PaletteRow();
        row.setSlot(0, "minecraft:stone");
        row.setSlot(1, "minecraft:dirt");

        java.util.List<String> hotbar = new java.util.ArrayList<>(java.util.Collections.nCopies(9, null));
        hotbar.set(0, "minecraft:iron_ingot");
        hotbar.set(1, "minecraft:gold_ingot");

        row.appendItems(hotbar);
        assertEquals(9, row.getSlotCount());
        assertEquals("minecraft:stone", row.getSlot(0));
        assertEquals("minecraft:dirt", row.getSlot(1));
        assertEquals("minecraft:iron_ingot", row.getSlot(2));
        assertEquals("minecraft:gold_ingot", row.getSlot(3));
        assertNull(row.getSlot(4));
    }

    @Test
    void testAppendItemsEmptyOrNullNoOp() {
        PaletteRow row = new PaletteRow();
        row.setSlot(0, "minecraft:stone");

        row.appendItems(null);
        assertEquals(9, row.getSlotCount());
        assertEquals("minecraft:stone", row.getSlot(0));

        row.appendItems(java.util.Collections.emptyList());
        assertEquals(9, row.getSlotCount());

        java.util.List<String> allNull = new java.util.ArrayList<>(java.util.Collections.nCopies(9, null));
        row.appendItems(allNull);
        assertEquals(9, row.getSlotCount());
        assertEquals("minecraft:stone", row.getSlot(0));
        assertNull(row.getSlot(1));
    }
}
