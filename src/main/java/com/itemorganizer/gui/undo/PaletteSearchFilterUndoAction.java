package com.itemorganizer.gui.undo;

import com.itemorganizer.core.model.PaletteRow;
import com.itemorganizer.gui.viewmodel.OrganizerViewModel;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PaletteSearchFilterUndoAction implements UndoAction {
    private final List<String> previousSlots;
    private final List<String> newSlots;

    public PaletteSearchFilterUndoAction(List<String> previousSlots, List<String> newSlots) {
        this.previousSlots = (previousSlots != null) ? new ArrayList<>(previousSlots) : Collections.emptyList();
        this.newSlots = (newSlots != null) ? new ArrayList<>(newSlots) : Collections.emptyList();
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

    private void applySlots(OrganizerViewModel viewModel, List<String> slots) {
        if (viewModel == null) return;
        PaletteRow filterRow = viewModel.getPaletteFilterRow();
        if (filterRow != null) {
            filterRow.setSlots(slots);
            viewModel.notifyChanges();
        }
    }
}
