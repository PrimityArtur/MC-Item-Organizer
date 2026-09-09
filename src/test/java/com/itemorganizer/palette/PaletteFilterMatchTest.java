package com.itemorganizer.palette;

import com.itemorganizer.core.model.PaletteRow;
import com.itemorganizer.gui.util.PaletteColumnColorComparator;
import com.itemorganizer.gui.widget.PaletteSearchFilterWidget;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PaletteFilterMatchTest {

    @Test
    void testExactSlotMatch() {
        PaletteSearchFilterWidget filterWidget = new PaletteSearchFilterWidget(0, 0);
        filterWidget.getFilterPalette().setSlot(0, "minecraft:stone");
        filterWidget.getFilterPalette().setSlot(2, "minecraft:dirt");

        PaletteRow exactRow = new PaletteRow(9);
        exactRow.setSlot(0, "minecraft:stone");
        exactRow.setSlot(1, "minecraft:oak_planks");
        exactRow.setSlot(2, "minecraft:dirt");

        PaletteRow misplacedRow = new PaletteRow(9);
        misplacedRow.setSlot(1, "minecraft:stone");
        misplacedRow.setSlot(2, "minecraft:dirt");

        PaletteSearchFilterWidget.FilterMatchResult res1 = filterWidget.evaluateMatch(exactRow);
        assertEquals(PaletteSearchFilterWidget.MatchTier.EXACT, res1.tier());
        assertTrue(res1.isMatch());

        PaletteSearchFilterWidget.FilterMatchResult res2 = filterWidget.evaluateMatch(misplacedRow);
        assertNotEquals(PaletteSearchFilterWidget.MatchTier.EXACT, res2.tier());
    }

    @Test
    void testItemMatchAnywhere() {
        PaletteSearchFilterWidget filterWidget = new PaletteSearchFilterWidget(0, 0);
        filterWidget.getFilterPalette().setSlot(0, "minecraft:stone");

        PaletteRow exactRow = new PaletteRow(9);
        exactRow.setSlot(0, "minecraft:stone");

        PaletteRow itemRow = new PaletteRow(9);
        itemRow.setSlot(5, "minecraft:stone");

        PaletteRow noneRow = new PaletteRow(9);
        noneRow.setSlot(0, "minecraft:dirt");

        assertEquals(PaletteSearchFilterWidget.MatchTier.EXACT, filterWidget.evaluateMatch(exactRow).tier());
        assertEquals(PaletteSearchFilterWidget.MatchTier.ITEM_MATCH, filterWidget.evaluateMatch(itemRow).tier());
    }

    @Test
    void testColorMatchWhenItemDiffers() {
        PaletteSearchFilterWidget filterWidget = new PaletteSearchFilterWidget(0, 0);
        filterWidget.getFilterPalette().setSlot(0, "minecraft:red_wool");

        PaletteRow exactRow = new PaletteRow(9);
        exactRow.setSlot(0, "minecraft:red_wool");

        PaletteRow itemRow = new PaletteRow(9);
        itemRow.setSlot(4, "minecraft:red_wool");

        PaletteRow colorRow = new PaletteRow(9);
        colorRow.setSlot(2, "minecraft:red_concrete");

        PaletteRow blueRow = new PaletteRow(9);
        blueRow.setSlot(0, "minecraft:blue_wool");

        assertEquals(PaletteSearchFilterWidget.MatchTier.EXACT, filterWidget.evaluateMatch(exactRow).tier());
        assertEquals(PaletteSearchFilterWidget.MatchTier.ITEM_MATCH, filterWidget.evaluateMatch(itemRow).tier());
        assertEquals(PaletteSearchFilterWidget.MatchTier.COLOR_MATCH, filterWidget.evaluateMatch(colorRow).tier());
        assertEquals(PaletteSearchFilterWidget.MatchTier.NONE, filterWidget.evaluateMatch(blueRow).tier());
    }

    @Test
    void testMultiItemFilterMatches() {
        PaletteSearchFilterWidget filterWidget = new PaletteSearchFilterWidget(0, 0);
        filterWidget.getFilterPalette().setSlot(0, "minecraft:poppy");
        filterWidget.getFilterPalette().setSlot(1, "minecraft:dandelion");

        PaletteRow exactRow = new PaletteRow(9);
        exactRow.setSlot(0, "minecraft:poppy");
        exactRow.setSlot(1, "minecraft:dandelion");

        PaletteRow fullItemRow = new PaletteRow(9);
        fullItemRow.setSlot(3, "minecraft:dandelion");
        fullItemRow.setSlot(6, "minecraft:poppy");

        PaletteRow partialItemRow = new PaletteRow(9);
        partialItemRow.setSlot(0, "minecraft:poppy");
        partialItemRow.setSlot(1, "minecraft:dirt");

        PaletteRow mixItemAndColorRow = new PaletteRow(9);
        mixItemAndColorRow.setSlot(0, "minecraft:poppy");
        mixItemAndColorRow.setSlot(4, "minecraft:yellow_concrete");

        PaletteRow colorRow = new PaletteRow(9);
        colorRow.setSlot(0, "minecraft:red_concrete");
        colorRow.setSlot(1, "minecraft:yellow_concrete");

        PaletteRow unrelatedRow = new PaletteRow(9);
        unrelatedRow.setSlot(0, "minecraft:blue_concrete");
        unrelatedRow.setSlot(1, "minecraft:obsidian");

        assertEquals(PaletteSearchFilterWidget.MatchTier.EXACT, filterWidget.evaluateMatch(exactRow).tier());
        assertEquals(PaletteSearchFilterWidget.MatchTier.ITEM_MATCH, filterWidget.evaluateMatch(fullItemRow).tier());
        assertEquals(PaletteSearchFilterWidget.MatchTier.NONE, filterWidget.evaluateMatch(partialItemRow).tier());
        assertEquals(PaletteSearchFilterWidget.MatchTier.COLOR_MATCH, filterWidget.evaluateMatch(mixItemAndColorRow).tier());
        assertEquals(PaletteSearchFilterWidget.MatchTier.COLOR_MATCH, filterWidget.evaluateMatch(colorRow).tier());
        assertEquals(PaletteSearchFilterWidget.MatchTier.NONE, filterWidget.evaluateMatch(unrelatedRow).tier());
    }

    @Test
    void testInfinitePaletteSlotFilterCompatibility() {
        PaletteSearchFilterWidget filterWidget = new PaletteSearchFilterWidget(0, 0);
        filterWidget.getFilterPalette().setSlot(0, "minecraft:stone");
        filterWidget.getFilterPalette().setSlot(8, "minecraft:cobblestone");

        PaletteRow row18 = new PaletteRow(9);
        row18.expandSlots(9);
        assertEquals(18, row18.getSlotCount());
        row18.setSlot(0, "minecraft:stone");
        row18.setSlot(8, "minecraft:cobblestone");
        assertEquals(PaletteSearchFilterWidget.MatchTier.EXACT, filterWidget.evaluateMatch(row18).tier());

        PaletteRow row27 = new PaletteRow(9);
        row27.expandSlots(18);
        assertEquals(27, row27.getSlotCount());
        row27.setSlot(12, "minecraft:stone");
        row27.setSlot(20, "minecraft:cobblestone");
        assertEquals(PaletteSearchFilterWidget.MatchTier.ITEM_MATCH, filterWidget.evaluateMatch(row27).tier());
    }

    @Test
    void testPaletteColumnColorComparator() {
        PaletteRow rWhite = new PaletteRow(9);
        rWhite.setSlot(0, "minecraft:white_concrete");

        PaletteRow rRed = new PaletteRow(9);
        rRed.setSlot(0, "minecraft:red_concrete");

        PaletteRow rBlue = new PaletteRow(9);
        rBlue.setSlot(0, "minecraft:blue_concrete");

        PaletteRow rEmpty = new PaletteRow(9);

        List<PaletteRow> rows = new ArrayList<>(List.of(rBlue, rEmpty, rWhite, rRed));
        rows.sort(PaletteColumnColorComparator.getInstance());

        // Neutrals first (white), then chromatic continuous rainbow (red before blue), then nulls last
        assertSame(rWhite, rows.get(0));
        assertSame(rRed, rows.get(1));
        assertSame(rBlue, rows.get(2));
        assertSame(rEmpty, rows.get(3));
    }

    @Test
    void testTieBreakingAcrossColumns() {
        PaletteRow rRedA = new PaletteRow(9);
        rRedA.setSlot(0, "minecraft:red_concrete");
        rRedA.setSlot(1, "minecraft:white_concrete");

        PaletteRow rRedB = new PaletteRow(9);
        rRedB.setSlot(0, "minecraft:red_concrete");
        rRedB.setSlot(1, "minecraft:blue_concrete");

        List<PaletteRow> rows = new ArrayList<>(List.of(rRedB, rRedA));
        rows.sort(PaletteColumnColorComparator.getInstance());

        // Both have red in slot 0, slot 1 breaks tie: white comes before blue
        assertSame(rRedA, rows.get(0));
        assertSame(rRedB, rows.get(1));
    }
}