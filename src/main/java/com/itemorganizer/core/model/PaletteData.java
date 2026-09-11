package com.itemorganizer.core.model;

import java.util.ArrayList;
import java.util.List;

// container for hotbar palette rows saved in palettes.json
public class PaletteData {
    private List<PaletteRow> rows = new ArrayList<>();

    public PaletteData() {
    }

    public PaletteData(List<PaletteRow> rows) {
        this.rows = rows != null ? rows : new ArrayList<>();
    }

    public List<PaletteRow> getRows() {
        return rows;
    }

    public void setRows(List<PaletteRow> rows) {
        this.rows = rows != null ? rows : new ArrayList<>();
    }

    public void addRow(PaletteRow row) {
        if (row != null) {
            rows.add(row);
        }
    }

    public void insertRow(int index, PaletteRow row) {
        if (row != null) {
            int target = Math.max(0, Math.min(rows.size(), index));
            rows.add(target, row);
        }
    }

    public boolean removeRowById(String id) {
        if (id == null) return false;
        return rows.removeIf(row -> id.equals(row.getId()));
    }

    public PaletteRow findRowById(String id) {
        if (id == null) return null;
        for (PaletteRow row : rows) {
            if (id.equals(row.getId())) {
                return row;
            }
        }
        return null;
    }

    public PaletteRow duplicateRow(int index) {
        if (index >= 0 && index < rows.size()) {
            PaletteRow copy = rows.get(index).copy();
            rows.add(index + 1, copy);
            return copy;
        }
        return null;
    }

    public boolean moveUp(int index) {
        if (index > 0 && index < rows.size()) {
            PaletteRow row = rows.remove(index);
            rows.add(index - 1, row);
            return true;
        }
        return false;
    }

    public boolean moveDown(int index) {
        if (index >= 0 && index < rows.size() - 1) {
            PaletteRow row = rows.remove(index);
            rows.add(index + 1, row);
            return true;
        }
        return false;
    }
}
