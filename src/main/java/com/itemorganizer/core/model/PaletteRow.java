package com.itemorganizer.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

// represents a hotbar palette row with support for dynamic slot expansion
public class PaletteRow {
    public static final int SLOTS_COUNT = 9;

    private String id;
    private String name;
    private List<String> slots;

    public PaletteRow() {
        this(SLOTS_COUNT);
    }

    public PaletteRow(int slotCount) {
        int count = Math.max(1, slotCount);
        this.id = UUID.randomUUID().toString();
        this.slots = new ArrayList<>(Collections.nCopies(count, null));
    }

    public PaletteRow(String id, List<String> slots) {
        this(id, null, slots);
    }

    public PaletteRow(String id, String name, int slotCount) {
        int count = Math.max(1, slotCount);
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.name = name;
        this.slots = new ArrayList<>(Collections.nCopies(count, null));
    }

    public PaletteRow(String id, String name, List<String> slots) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.name = name;
        int count = (slots != null && !slots.isEmpty()) ? Math.max(SLOTS_COUNT, slots.size()) : SLOTS_COUNT;
        this.slots = new ArrayList<>(Collections.nCopies(count, null));
        if (slots != null) {
            for (int i = 0; i < Math.min(slots.size(), count); i++) {
                this.slots.set(i, slots.get(i));
            }
        }
    }

    public int getSlotCount() {
        return slots != null ? slots.size() : SLOTS_COUNT;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PaletteRow copy() {
        PaletteRow cloned = new PaletteRow(UUID.randomUUID().toString(), new ArrayList<>(this.slots));
        cloned.setName(this.name != null && !this.name.isEmpty() ? (this.name + " (Copy)") : null);
        return cloned;
    }

    public PaletteRow snapshot() {
        return new PaletteRow(this.id, this.name, new ArrayList<>(this.slots != null ? this.slots : Collections.emptyList()));
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getSlots() {
        return slots;
    }

    public void setSlots(List<String> slots) {
        int count = (slots != null && !slots.isEmpty()) ? Math.max(SLOTS_COUNT, slots.size()) : SLOTS_COUNT;
        this.slots = new ArrayList<>(Collections.nCopies(count, null));
        if (slots != null) {
            for (int i = 0; i < Math.min(slots.size(), count); i++) {
                this.slots.set(i, slots.get(i));
            }
        }
    }

    public String getSlot(int index) {
        if (index >= 0 && index < getSlotCount()) {
            return slots.get(index);
        }
        return null;
    }

    public void setSlot(int index, String itemId) {
        if (index >= 0 && index < getSlotCount()) {
            slots.set(index, itemId);
        }
    }

    public void clearSlot(int index) {
        if (index >= 0 && index < getSlotCount()) {
            slots.set(index, null);
        }
    }

    public void clearSlots() {
        int count = getSlotCount();
        for (int i = 0; i < count; i++) {
            slots.set(i, null);
        }
    }

    public boolean hasAnyItem() {
        if (slots == null) return false;
        for (String slot : slots) {
            if (slot != null && !slot.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasSameSlotsAs(PaletteRow other) {
        if (other == null) return false;
        if (this.getSlotCount() != other.getSlotCount()) return false;
        int count = this.getSlotCount();
        for (int i = 0; i < count; i++) {
            String a = this.getSlot(i);
            String b = other.getSlot(i);
            String normA = (a == null || a.trim().isEmpty()) ? null : a.trim();
            String normB = (b == null || b.trim().isEmpty()) ? null : b.trim();
            if (!java.util.Objects.equals(normA, normB)) {
                return false;
            }
        }
        return true;
    }

    public void expandSlots(int additionalCount) {
        if (additionalCount <= 0) return;
        if (this.slots == null) {
            this.slots = new ArrayList<>();
        }
        for (int i = 0; i < additionalCount; i++) {
            this.slots.add(null);
        }
    }

    public void shrinkSlots(int removeCount) {
        if (removeCount <= 0 || this.slots == null) return;
        int targetCount = Math.max(SLOTS_COUNT, this.slots.size() - removeCount);
        while (this.slots.size() > targetCount) {
            this.slots.remove(this.slots.size() - 1);
        }
    }

    public boolean updateInfiniteSlots() {
        int initialCount = (this.slots != null) ? this.slots.size() : 0;
        if (this.slots == null) {
            this.slots = new ArrayList<>(Collections.nCopies(SLOTS_COUNT, null));
            return true;
        }
        while (this.slots.size() < SLOTS_COUNT) {
            this.slots.add(null);
        }

        while (getSlotCount() >= SLOTS_COUNT) {
            String lastItem = getSlot(getSlotCount() - 1);
            if (lastItem != null && !lastItem.trim().isEmpty()) {
                expandSlots(SLOTS_COUNT);
            } else {
                break;
            }
        }

        while (getSlotCount() > SLOTS_COUNT) {
            int currentCount = getSlotCount();
            int lastChunkStart = currentCount - SLOTS_COUNT;
            boolean lastChunkHasItems = false;
            for (int i = lastChunkStart; i < currentCount; i++) {
                String item = getSlot(i);
                if (item != null && !item.trim().isEmpty()) {
                    lastChunkHasItems = true;
                    break;
                }
            }
            if (lastChunkHasItems) {
                break;
            }

            int triggerSlot = lastChunkStart - 1;
            String triggerItem = getSlot(triggerSlot);
            if (triggerItem == null || triggerItem.trim().isEmpty()) {
                shrinkSlots(SLOTS_COUNT);
            } else {
                break;
            }
        }
        return getSlotCount() != initialCount;
    }

    public void appendItems(List<String> itemsToAppend) {
        if (itemsToAppend == null || itemsToAppend.isEmpty()) return;

        int lastInputIdx = -1;
        for (int i = itemsToAppend.size() - 1; i >= 0; i--) {
            String it = itemsToAppend.get(i);
            if (it != null && !it.trim().isEmpty()) {
                lastInputIdx = i;
                break;
            }
        }
        if (lastInputIdx == -1) return;

        int countToPaste = lastInputIdx + 1;

        int lastOccupied = -1;
        for (int i = getSlotCount() - 1; i >= 0; i--) {
            String item = getSlot(i);
            if (item != null && !item.trim().isEmpty()) {
                lastOccupied = i;
                break;
            }
        }

        int startSlot = (lastOccupied >= 0) ? (lastOccupied + 1) : 0;
        int requiredSlots = startSlot + countToPaste;

        while (getSlotCount() < requiredSlots) {
            expandSlots(SLOTS_COUNT);
        }

        for (int i = 0; i < countToPaste; i++) {
            setSlot(startSlot + i, itemsToAppend.get(i));
        }

        updateInfiniteSlots();
    }
}