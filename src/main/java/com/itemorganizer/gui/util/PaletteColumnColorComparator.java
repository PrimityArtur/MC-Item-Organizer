package com.itemorganizer.gui.util;

import com.itemorganizer.core.model.PaletteRow;

import java.util.Comparator;

// compares palette rows column-by-column by their visual item color
public class PaletteColumnColorComparator implements Comparator<PaletteRow> {
    private static final PaletteColumnColorComparator INSTANCE = new PaletteColumnColorComparator();

    public static PaletteColumnColorComparator getInstance() {
        return INSTANCE;
    }

    @Override
    public int compare(PaletteRow a, PaletteRow b) {
        if (a == b) return 0;
        if (a == null) return 1;
        if (b == null) return -1;

        int maxSlots = Math.max(a.getSlotCount(), b.getSlotCount());
        for (int i = 0; i < maxSlots; i++) {
            String itemA = (i < a.getSlotCount()) ? a.getSlot(i) : null;
            String itemB = (i < b.getSlotCount()) ? b.getSlot(i) : null;

            if (itemA != null && itemA.trim().isEmpty()) itemA = null;
            if (itemB != null && itemB.trim().isEmpty()) itemB = null;

            int cmp = ItemColorHelper.compareItemColors(itemA, itemB);
            if (cmp != 0) {
                return cmp;
            }
        }

        if (a.getName() != null && b.getName() != null) {
            int nameCmp = a.getName().compareToIgnoreCase(b.getName());
            if (nameCmp != 0) return nameCmp;
        } else if (a.getName() != null) {
            return -1;
        } else if (b.getName() != null) {
            return 1;
        }

        if (a.getId() != null && b.getId() != null) {
            return a.getId().compareTo(b.getId());
        }
        return 0;
    }
}