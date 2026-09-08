package com.itemorganizer.gui.dragdrop;

import net.minecraft.item.ItemStack;

// payload carrying dragged item details
public class DragPayload {
    private final String itemId;
    private final ItemStack itemStack;
    private final DragSource source;
    private final int sourceCol;
    private final int sourceRow;
    private final int sourceIndex;
    private final boolean isCopy;

    public DragPayload(String itemId, ItemStack itemStack, DragSource source, int sourceCol, int sourceRow, int sourceIndex, boolean isCopy) {
        this.itemId = itemId;
        this.itemStack = itemStack;
        this.source = source;
        this.sourceCol = sourceCol;
        this.sourceRow = sourceRow;
        this.sourceIndex = sourceIndex;
        this.isCopy = isCopy;
    }

    public static DragPayload ofGrid(String itemId, ItemStack stack, DragSource source, int col, int row, boolean isCopy) {
        return new DragPayload(itemId, stack, source, col, row, -1, isCopy);
    }

    public static DragPayload ofIndexed(String itemId, ItemStack stack, DragSource source, int index, boolean isCopy) {
        return new DragPayload(itemId, stack, source, -1, -1, index, isCopy);
    }

    public String getItemId() {
        return itemId;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public DragSource getSource() {
        return source;
    }

    public int getSourceCol() {
        return sourceCol;
    }

    public int getSourceRow() {
        return sourceRow;
    }

    public int getSourceIndex() {
        return sourceIndex;
    }

    public boolean isCopy() {
        return isCopy;
    }
}
