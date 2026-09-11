package com.itemorganizer.gui.undo;

import com.itemorganizer.core.model.PaletteData;
import com.itemorganizer.core.model.PaletteRow;
import com.itemorganizer.gui.viewmodel.OrganizerViewModel;
import com.itemorganizer.storage.StorageManager;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

// restores all slots of a palette row to their previous or redone items
public class PaletteFullUndoAction implements UndoAction {
    private final String paletteId;
    private final List<String> previousSlots;
    private List<String> newSlots;

    public PaletteFullUndoAction(PaletteRow row) {
        this.paletteId = (row != null) ? row.getId() : "";
        this.previousSlots = (row != null && row.getSlots() != null) ? new ArrayList<>(row.getSlots()) : new ArrayList<>();
        this.newSlots = new ArrayList<>();
    }

    public PaletteFullUndoAction(String paletteId, List<String> previousSlots, List<String> newSlots) {
        this.paletteId = (paletteId != null) ? paletteId : "";
        this.previousSlots = (previousSlots != null) ? new ArrayList<>(previousSlots) : new ArrayList<>();
        this.newSlots = (newSlots != null) ? new ArrayList<>(newSlots) : new ArrayList<>();
    }

    public void setNewSlots(List<String> newSlots) {
        this.newSlots = (newSlots != null) ? new ArrayList<>(newSlots) : new ArrayList<>();
    }

    public String getPaletteId() {
        return paletteId;
    }

    public List<String> getPreviousSlots() {
        return previousSlots;
    }

    public List<String> getNewSlots() {
        return newSlots;
    }

    @Override
    public void undo(MinecraftClient client, OrganizerViewModel viewModel) {
        applySlots(viewModel, previousSlots);
    }

    @Override
    public void redo(MinecraftClient client, OrganizerViewModel viewModel) {
        applySlots(viewModel, newSlots);
    }

    private void applySlots(OrganizerViewModel viewModel, List<String> slotsToApply) {
        if (viewModel == null || slotsToApply == null) return;
        PaletteData data = viewModel.getPaletteData();
        if (data != null) {
            PaletteRow row = data.findRowById(paletteId);
            if (row != null) {
                row.setSlots(new ArrayList<>(slotsToApply));
                StorageManager.getInstance().getPaletteRepository().save(data);
                viewModel.notifyChanges();
                return;
            }
        }
        PaletteData infData = viewModel.getInfinitePaletteData();
        if (infData != null) {
            PaletteRow row = infData.findRowById(paletteId);
            if (row != null) {
                row.setSlots(new ArrayList<>(slotsToApply));
                row.updateInfiniteSlots();
                StorageManager.getInstance().getInfinitePaletteRepository().save(infData);
                viewModel.notifyChanges();
            }
        }
    }
}
