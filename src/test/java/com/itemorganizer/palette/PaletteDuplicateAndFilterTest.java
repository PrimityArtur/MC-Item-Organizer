package com.itemorganizer.palette;

import com.itemorganizer.core.model.PaletteRow;
import com.itemorganizer.gui.widget.PaletteListWidget;
import com.itemorganizer.gui.widget.PaletteSearchFilterWidget;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PaletteDuplicateAndFilterTest {

    @Test
    void testDuplicatePaletteDetection() {
        PaletteRow p1 = new PaletteRow();
        p1.setId("pal_1");
        p1.setSlot(0, "minecraft:stone");
        p1.setSlot(1, "minecraft:dirt");

        PaletteRow p2 = new PaletteRow();
        p2.setId("pal_2");
        p2.setSlot(0, "minecraft:stone");
        p2.setSlot(1, "minecraft:dirt");

        PaletteRow p3 = new PaletteRow();
        p3.setId("pal_3");
        p3.setSlot(0, "minecraft:stone");
        p3.setSlot(1, "minecraft:cobblestone");

        List<PaletteRow> allRows = new ArrayList<>();
        allRows.add(p1);
        allRows.add(p2);
        allRows.add(p3);

        // p1 and p2 have identical items in identical slots -> duplicates
        assertTrue(PaletteListWidget.isDuplicatePalette(p1, allRows));
        assertTrue(PaletteListWidget.isDuplicatePalette(p2, allRows));

        // p3 has different items in slot 1 -> not duplicate
        assertFalse(PaletteListWidget.isDuplicatePalette(p3, allRows));

        // empty palette is not marked duplicate
        PaletteRow empty = new PaletteRow();
        empty.setId("empty");
        allRows.add(empty);
        assertFalse(PaletteListWidget.isDuplicatePalette(empty, allRows));
    }

    @Test
    void testPaletteSearchFilterMatching() {
        PaletteSearchFilterWidget filterWidget = new PaletteSearchFilterWidget(0, 0);

        PaletteRow row1 = new PaletteRow();
        row1.setSlot(0, "minecraft:stone");
        row1.setSlot(1, "minecraft:dirt");
        row1.setSlot(2, "minecraft:sand");

        PaletteRow row2 = new PaletteRow();
        row2.setSlot(0, "minecraft:stone");
        row2.setSlot(1, "minecraft:gravel");
        row2.setSlot(2, "minecraft:sand");

        PaletteRow row3 = new PaletteRow();
        row3.setSlot(0, "minecraft:oak_log");
        row3.setSlot(1, "minecraft:dirt");

        // empty filter matches everything
        assertTrue(filterWidget.matches(row1));
        assertTrue(filterWidget.matches(row2));
        assertTrue(filterWidget.matches(row3));

        // filter by slot 0 = stone
        filterWidget.getFilterPalette().setSlot(0, "minecraft:stone");
        assertTrue(filterWidget.matches(row1));
        assertTrue(filterWidget.matches(row2));
        assertFalse(filterWidget.matches(row3));

        // filter by slot 0 = stone AND slot 1 = dirt
        filterWidget.getFilterPalette().setSlot(1, "minecraft:dirt");
        assertTrue(filterWidget.matches(row1));
        assertFalse(filterWidget.matches(row2)); // row2 has gravel in slot 1
        assertFalse(filterWidget.matches(row3)); // row3 has oak_log in slot 0

        // clear filter resets matching
        filterWidget.clearFilter();
        assertFalse(filterWidget.isActive());
        assertTrue(filterWidget.matches(row1));
        assertTrue(filterWidget.matches(row2));
        assertTrue(filterWidget.matches(row3));
    }
}
