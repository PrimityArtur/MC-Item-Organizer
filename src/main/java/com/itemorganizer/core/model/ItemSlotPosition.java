package com.itemorganizer.core.model;

import java.util.Objects;

// slot position for an item in the organized grid
public class ItemSlotPosition {
    private String itemId;
    private int x;
    private int y;

    public ItemSlotPosition() {
    }

    public ItemSlotPosition(String itemId, int x, int y) {
        this.itemId = itemId;
        this.x = x;
        this.y = y;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemSlotPosition that)) return false;
        return x == that.x && y == that.y && Objects.equals(itemId, that.itemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId, x, y);
    }

    @Override
    public String toString() {
        return "ItemSlotPosition{" +
                "itemId='" + itemId + '\'' +
                ", x=" + x +
                ", y=" + y +
                '}';
    }
}
