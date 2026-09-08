package com.itemorganizer.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

// represents a 9-slot hotbar palette row
public class PaletteRow {
    public static final int SLOTS_COUNT = 9;

    private String id;
    private String name;
    private List<String> slots;

    public PaletteRow() {
        this.id = UUID.randomUUID().toString();
        this.slots = new ArrayList<>(Collections.nCopies(SLOTS_COUNT, null));
    }

    public PaletteRow(String id, List<String> slots) {
        this(id, null, slots);
    }

    public PaletteRow(String id, String name, List<String> slots) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.name = name;
        this.slots = new ArrayList<>(Collections.nCopies(SLOTS_COUNT, null));
        if (slots != null) {
            for (int i = 0; i < Math.min(slots.size(), SLOTS_COUNT); i++) {
                this.slots.set(i, slots.get(i));
            }
        }
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
        this.slots = new ArrayList<>(Collections.nCopies(SLOTS_COUNT, null));
        if (slots != null) {
            for (int i = 0; i < Math.min(slots.size(), SLOTS_COUNT); i++) {
                this.slots.set(i, slots.get(i));
            }
        }
    }

    public String getSlot(int index) {
        if (index >= 0 && index < SLOTS_COUNT) {
            return slots.get(index);
        }
        return null;
    }

    public void setSlot(int index, String itemId) {
        if (index >= 0 && index < SLOTS_COUNT) {
            slots.set(index, itemId);
        }
    }

    public void clearSlot(int index) {
        if (index >= 0 && index < SLOTS_COUNT) {
            slots.set(index, null);
        }
    }
}
