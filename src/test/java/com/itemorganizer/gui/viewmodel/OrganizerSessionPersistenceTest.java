package com.itemorganizer.gui.viewmodel;

import com.itemorganizer.core.model.PaletteRow;
import com.itemorganizer.gui.component.ScrollbarComponent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OrganizerSessionPersistenceTest {

    @Test
    void testSessionScrollOffsetsAcrossAreas() {
        OrganizerViewModel viewModel = new OrganizerViewModel(null, false);

        String[] areas = {
                OrganizerViewModel.AREA_ORGANIZED,
                OrganizerViewModel.AREA_PROFILES,
                OrganizerViewModel.AREA_CONFIG,
                OrganizerViewModel.AREA_BLOCKER,
                OrganizerViewModel.AREA_PALETTES,
                OrganizerViewModel.AREA_INF_PALETTE,
                OrganizerViewModel.AREA_UNORGANIZED,
                OrganizerViewModel.AREA_VERSION
        };

        // default offset for uninitialized area
        for (String area : areas) {
            assertEquals(0.0, viewModel.getScrollOffset(area), 0.001);
        }
        assertEquals(0.0, viewModel.getScrollOffset("unknown_area"), 0.001);

        // set distinct offsets
        for (int i = 0; i < areas.length; i++) {
            double expected = (i + 1) * 35.5;
            viewModel.setScrollOffset(areas[i], expected);
            assertEquals(expected, viewModel.getScrollOffset(areas[i]), 0.001);
        }

        // clamp negative offsets to zero
        viewModel.setScrollOffset(OrganizerViewModel.AREA_CONFIG, -42.0);
        assertEquals(0.0, viewModel.getScrollOffset(OrganizerViewModel.AREA_CONFIG), 0.001);
    }

    @Test
    void testPaletteSearchQueryPersistence() {
        OrganizerViewModel viewModel = new OrganizerViewModel(null, false);

        assertEquals("", viewModel.getPaletteSearchQuery());

        viewModel.setPaletteSearchQuery("stone");
        assertEquals("stone", viewModel.getPaletteSearchQuery());

        viewModel.setPaletteSearchQuery(null);
        assertEquals("", viewModel.getPaletteSearchQuery());
    }

    @Test
    void testInfinitePaletteSearchQueryPersistence() {
        OrganizerViewModel viewModel = new OrganizerViewModel(null, false);

        assertEquals("", viewModel.getInfinitePaletteSearchQuery());

        viewModel.setInfinitePaletteSearchQuery("diorite");
        assertEquals("diorite", viewModel.getInfinitePaletteSearchQuery());

        viewModel.setInfinitePaletteSearchQuery(null);
        assertEquals("", viewModel.getInfinitePaletteSearchQuery());
    }

    @Test
    void testPaletteFilterRowPersistence() {
        OrganizerViewModel viewModel = new OrganizerViewModel(null, false);

        PaletteRow filterRow = viewModel.getPaletteFilterRow();
        assertNotNull(filterRow);

        filterRow.setSlot(0, "minecraft:oak_planks");
        assertSame(filterRow, viewModel.getPaletteFilterRow());
        assertEquals("minecraft:oak_planks", viewModel.getPaletteFilterRow().getSlot(0));
    }

    @Test
    void testDynamicPaletteRowSizing() {
        PaletteRow defaultRow = new PaletteRow();
        assertEquals(9, defaultRow.getSlotCount());

        PaletteRow dynamicRow = new PaletteRow(18);
        assertEquals(18, dynamicRow.getSlotCount());
        for (int i = 0; i < 18; i++) {
            assertNull(dynamicRow.getSlot(i));
        }

        PaletteRow namedRow = new PaletteRow("custom_id", "Custom Palette", 12);
        assertEquals("custom_id", namedRow.getId());
        assertEquals("Custom Palette", namedRow.getName());
        assertEquals(12, namedRow.getSlotCount());

        namedRow.setSlot(11, "minecraft:diamond_block");
        assertEquals("minecraft:diamond_block", namedRow.getSlot(11));

        // out of bounds index safety
        assertNull(namedRow.getSlot(-1));
        assertNull(namedRow.getSlot(12));
        namedRow.setSlot(-1, "minecraft:stone");
        namedRow.setSlot(99, "minecraft:stone");
    }

    @Test
    void testPaletteRowSlotComparison() {
        PaletteRow rowA = new PaletteRow(9);
        rowA.setSlot(0, "minecraft:stone");
        rowA.setSlot(1, "minecraft:granite");

        PaletteRow rowB = new PaletteRow(9);
        rowB.setSlot(0, "minecraft:stone");
        rowB.setSlot(1, "minecraft:granite");

        assertTrue(rowA.hasSameSlotsAs(rowB));
        assertTrue(rowA.hasAnyItem());

        PaletteRow rowC = new PaletteRow(10);
        rowC.setSlot(0, "minecraft:stone");
        rowC.setSlot(1, "minecraft:granite");

        assertFalse(rowA.hasSameSlotsAs(rowC));
    }

    @Test
    void testScrollbarOffsetPreservationBeforeLayout() {
        ScrollbarComponent scrollbar = new ScrollbarComponent(0, 0, 4, 100);

        // restored offset before container calculates maxScroll
        scrollbar.setScrollOffset(125.0);
        assertEquals(125.0, scrollbar.getScrollOffset(), 0.001);

        // clamped once layout updates max scroll
        scrollbar.updateMaxScroll(300, 100);
        assertEquals(200.0, scrollbar.getMaxScroll(), 0.001);
        assertEquals(125.0, scrollbar.getScrollOffset(), 0.001);

        // clamped if content is smaller than offset
        scrollbar.updateMaxScroll(150, 100);
        assertEquals(50.0, scrollbar.getMaxScroll(), 0.001);
        assertEquals(50.0, scrollbar.getScrollOffset(), 0.001);
    }
}
