package com.itemorganizer.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// profile data holding items and blocked list
public class ProfileData {
    private String name;
    private int columnCount = 0;
    private List<ItemSlotPosition> items = new ArrayList<>();
    private List<String> blockedItems = new ArrayList<>();

    public ProfileData() {
    }

    public ProfileData(String name) {
        this.name = name;
        this.items = new ArrayList<>();
        this.blockedItems = new ArrayList<>();
    }

    public ProfileData(String name, List<ItemSlotPosition> items) {
        this.name = name;
        this.items = items != null ? items : new ArrayList<>();
        this.blockedItems = new ArrayList<>();
    }

    public ProfileData(String name, ProfileData source) {
        this.name = name;
        if (source != null) {
            this.columnCount = source.getColumnCount();
            this.items = new ArrayList<>();
            for (ItemSlotPosition pos : source.getItems()) {
                this.items.add(new ItemSlotPosition(pos.getItemId(), pos.getX(), pos.getY()));
            }
            this.blockedItems = new ArrayList<>(source.getBlockedItems());
        } else {
            this.items = new ArrayList<>();
            this.blockedItems = new ArrayList<>();
        }
    }

    public void copyFrom(ProfileData source) {
        if (source == null) return;
        this.columnCount = source.getColumnCount();
        this.items = new ArrayList<>();
        for (ItemSlotPosition pos : source.getItems()) {
            this.items.add(new ItemSlotPosition(pos.getItemId(), pos.getX(), pos.getY()));
        }
        this.blockedItems = new ArrayList<>(source.getBlockedItems());
    }

    public ProfileData snapshot() {
        return new ProfileData(this.name, this);
    }

    public List<String> getBlockedItems() {
        if (blockedItems == null) {
            blockedItems = new ArrayList<>();
        }
        return blockedItems;
    }

    public void setBlockedItems(List<String> blockedItems) {
        this.blockedItems = (blockedItems != null) ? new ArrayList<>(blockedItems) : new ArrayList<>();
    }

    public boolean isItemBlocked(String itemId) {
        if (itemId == null || itemId.isEmpty()) return false;
        return getBlockedItems().contains(itemId);
    }

    public boolean blockItem(String itemId) {
        if (itemId == null || itemId.isEmpty()) return false;
        if (!getBlockedItems().contains(itemId)) {
            return getBlockedItems().add(itemId);
        }
        return false;
    }

    public boolean unblockItem(String itemId) {
        if (itemId == null || itemId.isEmpty()) return false;
        return getBlockedItems().remove(itemId);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getColumnCount() {
        return columnCount;
    }

    public void setColumnCount(int columnCount) {
        this.columnCount = columnCount;
    }

    // reflow item coordinates to match new column count
    public boolean reflowToColumns(int newCols) {
        if (newCols <= 0) return false;
        int oldCols = this.columnCount;
        if (oldCols <= 0) {
            int maxCol = items.stream().mapToInt(ItemSlotPosition::getX).max().orElse(-1);
            oldCols = Math.max(maxCol + 1, newCols);
        }
        if (oldCols == newCols) {
            this.columnCount = newCols;
            return false;
        }

        for (ItemSlotPosition item : items) {
            int oldIdx = item.getY() * oldCols + item.getX();
            item.setX(oldIdx % newCols);
            item.setY(oldIdx / newCols);
        }
        this.columnCount = newCols;
        return true;
    }

    public List<ItemSlotPosition> getItems() {
        return items;
    }

    public void setItems(List<ItemSlotPosition> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public boolean hasItem(String itemId) {
        if (itemId == null) return false;
        return items.stream().anyMatch(pos -> itemId.equals(pos.getItemId()));
    }

    public Optional<ItemSlotPosition> findItemAt(int x, int y) {
        return items.stream()
                .filter(pos -> pos.getX() == x && pos.getY() == y)
                .findFirst();
    }

    public Optional<ItemSlotPosition> findPositionOf(String itemId) {
        if (itemId == null) return Optional.empty();
        return items.stream()
                .filter(pos -> itemId.equals(pos.getItemId()))
                .findFirst();
    }

    public void setItemAt(String itemId, int x, int y) {
        // remove duplicate item elsewhere in grid
        items.removeIf(pos -> pos.getItemId().equals(itemId));
        // replace target slot if occupied
        items.removeIf(pos -> pos.getX() == x && pos.getY() == y);
        items.add(new ItemSlotPosition(itemId, x, y));
    }

    public boolean removeItem(String itemId) {
        if (itemId == null) return false;
        return items.removeIf(pos -> itemId.equals(pos.getItemId()));
    }

    public boolean removeAt(int x, int y) {
        return items.removeIf(pos -> pos.getX() == x && pos.getY() == y);
    }

    // compact all items removing gaps starting from top-left
    public boolean compactItems(int cols) {
        if (items.isEmpty() || cols <= 0) return false;

        int effectiveCols = this.columnCount > 0 ? this.columnCount : cols;
        items.sort(java.util.Comparator.comparingInt(p -> p.getY() * effectiveCols + p.getX()));

        boolean changed = false;
        for (int i = 0; i < items.size(); i++) {
            ItemSlotPosition pos = items.get(i);
            int newX = i % cols;
            int newY = i / cols;
            if (pos.getX() != newX || pos.getY() != newY) {
                pos.setX(newX);
                pos.setY(newY);
                changed = true;
            }
        }
        this.columnCount = cols;
        return changed;
    }

    // sort all items by comparator and pack them row by row
    public boolean sortItems(java.util.Comparator<String> comparator, int cols) {
        if (items.isEmpty() || cols <= 0) return false;

        items.sort((p1, p2) -> {
            if (p1 == p2) return 0;
            if (p1 == null) return 1;
            if (p2 == null) return -1;
            return comparator.compare(p1.getItemId(), p2.getItemId());
        });

        for (int i = 0; i < items.size(); i++) {
            ItemSlotPosition pos = items.get(i);
            if (pos != null) {
                pos.setX(i % cols);
                pos.setY(i / cols);
            }
        }
        this.columnCount = cols;
        return true;
    }
}
