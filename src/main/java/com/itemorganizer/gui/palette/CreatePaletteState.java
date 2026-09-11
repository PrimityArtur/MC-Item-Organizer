package com.itemorganizer.gui.palette;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// state container for the create palette studio
public class CreatePaletteState {
    public static final int DEFAULT_SLOTS = 9;
    public static final int MIN_SLOTS = 2;
    public static final int MAX_SLOTS = 36;

    private int slotCount = DEFAULT_SLOTS;
    private final List<String> inputSlots = new ArrayList<>();
    private final List<List<String>> resultRows = new ArrayList<>();
    private final List<int[]> seedOffsets = new ArrayList<>();

    private boolean filterCube = false;
    private boolean filterSolid = false;
    private boolean filterTransparent = false;
    private boolean filterUniformTexture = false;
    private boolean filterOres = false;
    private boolean filterGlazed = false;
    private boolean filterLights = false;

    public CreatePaletteState() {
        initDefaults();
    }

    private void initDefaults() {
        inputSlots.clear();
        for (int i = 0; i < slotCount; i++) {
            inputSlots.add("");
        }

        resultRows.clear();
        seedOffsets.clear();
        addResultRowInternal();
    }

    private void addResultRowInternal() {
        List<String> row = new ArrayList<>();
        for (int i = 0; i < slotCount; i++) {
            row.add("");
        }
        int[] seeds = new int[slotCount];
        int rowIndex = seedOffsets.size();
        for (int i = 0; i < slotCount; i++) {
            seeds[i] = rowIndex;
        }
        resultRows.add(row);
        seedOffsets.add(seeds);
    }

    public int getSlotCount() {
        return slotCount;
    }

    public void setSlotCount(int count) {
        int clamped = Math.max(MIN_SLOTS, Math.min(MAX_SLOTS, count));
        if (this.slotCount == clamped) return;

        this.slotCount = clamped;
        while (inputSlots.size() < slotCount) {
            inputSlots.add("");
        }
        while (inputSlots.size() > slotCount) {
            inputSlots.remove(inputSlots.size() - 1);
        }

        for (int r = 0; r < resultRows.size(); r++) {
            List<String> row = resultRows.get(r);
            while (row.size() < slotCount) {
                row.add("");
            }
            while (row.size() > slotCount) {
                row.remove(row.size() - 1);
            }

            int[] oldSeeds = seedOffsets.get(r);
            int[] newSeeds = new int[slotCount];
            for (int i = 0; i < slotCount; i++) {
                newSeeds[i] = (i < oldSeeds.length) ? oldSeeds[i] : r;
            }
            seedOffsets.set(r, newSeeds);
        }
    }

    public List<String> getInputSlots() {
        return inputSlots;
    }

    public String getInputSlot(int index) {
        if (index < 0 || index >= inputSlots.size()) return "";
        return inputSlots.get(index);
    }

    public void setInputSlot(int index, String itemId) {
        if (index >= 0 && index < inputSlots.size()) {
            inputSlots.set(index, itemId != null ? itemId : "");
        }
    }

    public void clearInput() {
        for (int i = 0; i < inputSlots.size(); i++) {
            inputSlots.set(i, "");
        }
        for (int[] seeds : seedOffsets) {
            Arrays.fill(seeds, 0);
        }
    }

    public int getResultRowCount() {
        return resultRows.size();
    }

    public List<List<String>> getResultRows() {
        return resultRows;
    }

    public List<String> getResultRow(int index) {
        if (index < 0 || index >= resultRows.size()) return List.of();
        return resultRows.get(index);
    }

    public void setResultRow(int index, List<String> row) {
        if (index >= 0 && index < resultRows.size()) {
            List<String> current = resultRows.get(index);
            current.clear();
            current.addAll(row);
        }
    }

    public int[] getSeedOffsets(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= seedOffsets.size()) return new int[slotCount];
        return seedOffsets.get(rowIndex);
    }

    public void cycleSeed(int rowIndex, int slotIndex) {
        if (rowIndex >= 0 && rowIndex < seedOffsets.size()) {
            int[] seeds = seedOffsets.get(rowIndex);
            if (slotIndex >= 0 && slotIndex < seeds.length) {
                seeds[slotIndex]++;
            }
        }
    }

    public void addResultRow() {
        addResultRowInternal();
    }

    public void removeResultRow(int index) {
        if (resultRows.size() > 1 && index >= 0 && index < resultRows.size()) {
            resultRows.remove(index);
            seedOffsets.remove(index);
        } else if (resultRows.size() == 1 && index == 0) {
            List<String> row0 = resultRows.get(0);
            for (int i = 0; i < row0.size(); i++) {
                row0.set(i, "");
            }
            int[] seeds = seedOffsets.get(0);
            for (int i = 0; i < seeds.length; i++) {
                seeds[i] = 0;
            }
        }
    }

    public void removeResultRow() {
        removeResultRow(resultRows.size() - 1);
    }

    public boolean isFilterCube() {
        return filterCube;
    }

    public void setFilterCube(boolean filterCube) {
        this.filterCube = filterCube;
    }

    public boolean isFilterSolid() {
        return filterSolid;
    }

    public void setFilterSolid(boolean filterSolid) {
        this.filterSolid = filterSolid;
    }

    public boolean isFilterTransparent() {
        return filterTransparent;
    }

    public void setFilterTransparent(boolean filterTransparent) {
        this.filterTransparent = filterTransparent;
    }

    public boolean isFilterUniformTexture() {
        return filterUniformTexture;
    }

    public void setFilterUniformTexture(boolean filterUniformTexture) {
        this.filterUniformTexture = filterUniformTexture;
    }

    public boolean isFilterOres() {
        return filterOres;
    }

    public void setFilterOres(boolean filterOres) {
        this.filterOres = filterOres;
    }

    public boolean isFilterGlazed() {
        return filterGlazed;
    }

    public void setFilterGlazed(boolean filterGlazed) {
        this.filterGlazed = filterGlazed;
    }

    public boolean isFilterLights() {
        return filterLights;
    }

    public void setFilterLights(boolean filterLights) {
        this.filterLights = filterLights;
    }

    public CreatePaletteState copy() {
        CreatePaletteState copy = new CreatePaletteState();
        copy.slotCount = this.slotCount;

        copy.inputSlots.clear();
        copy.inputSlots.addAll(this.inputSlots);

        copy.resultRows.clear();
        copy.seedOffsets.clear();
        for (int r = 0; r < this.resultRows.size(); r++) {
            copy.resultRows.add(new ArrayList<>(this.resultRows.get(r)));
            copy.seedOffsets.add(Arrays.copyOf(this.seedOffsets.get(r), this.slotCount));
        }

        copy.filterCube = this.filterCube;
        copy.filterSolid = this.filterSolid;
        copy.filterTransparent = this.filterTransparent;
        copy.filterUniformTexture = this.filterUniformTexture;
        copy.filterOres = this.filterOres;
        copy.filterGlazed = this.filterGlazed;
        copy.filterLights = this.filterLights;

        return copy;
    }

    public void restoreFrom(CreatePaletteState source) {
        if (source == null) return;
        this.slotCount = source.slotCount;

        this.inputSlots.clear();
        this.inputSlots.addAll(source.inputSlots);

        this.resultRows.clear();
        this.seedOffsets.clear();
        for (int r = 0; r < source.resultRows.size(); r++) {
            this.resultRows.add(new ArrayList<>(source.resultRows.get(r)));
            this.seedOffsets.add(Arrays.copyOf(source.seedOffsets.get(r), source.slotCount));
        }

        this.filterCube = source.filterCube;
        this.filterSolid = source.filterSolid;
        this.filterTransparent = source.filterTransparent;
        this.filterUniformTexture = source.filterUniformTexture;
        this.filterOres = source.filterOres;
        this.filterGlazed = source.filterGlazed;
        this.filterLights = source.filterLights;
    }
}
