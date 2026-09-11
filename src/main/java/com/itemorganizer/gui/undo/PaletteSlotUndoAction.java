package com.itemorganizer.gui.undo;

import com.itemorganizer.core.model.PaletteData;
import com.itemorganizer.core.model.PaletteRow;
import com.itemorganizer.gui.viewmodel.OrganizerViewModel;
import com.itemorganizer.storage.StorageManager;
import net.minecraft.client.MinecraftClient;

// restores a single slot in a palette row to its previous or redone item id
public class PaletteSlotUndoAction implements UndoAction {
    private final String paletteId;
    private final int slot;
    private final String previousItemId;
    private final String newItemId;

    public PaletteSlotUndoAction(String paletteId, int slot, String previousItemId) {
        this(paletteId, slot, previousItemId, null);
    }

    public PaletteSlotUndoAction(String paletteId, int slot, String previousItemId, String newItemId) {
        this.paletteId = paletteId;
        this.slot = slot;
        this.previousItemId = previousItemId;
        this.newItemId = newItemId;
    }

    public String getPaletteId() {
        return paletteId;
    }

    public int getSlot() {
        return slot;
    }

    public String getPreviousItemId() {
        return previousItemId;
    }

    public String getNewItemId() {
        return newItemId;
    }

    @Override
    public void undo(MinecraftClient client, OrganizerViewModel viewModel) {
        applySlot(viewModel, previousItemId);
    }

    @Override
    public void redo(MinecraftClient client, OrganizerViewModel viewModel) {
        applySlot(viewModel, newItemId);
    }

    private void applySlot(OrganizerViewModel viewModel, String itemId) {
        if (viewModel == null) return;
        PaletteData data = viewModel.getPaletteData();
        if (data != null) {
            PaletteRow row = data.findRowById(paletteId);
            if (row != null && slot >= 0 && slot < row.getSlotCount()) {
                row.setSlot(slot, itemId);
                StorageManager.getInstance().getPaletteRepository().save(data);
                viewModel.notifyChanges();
                return;
            }
        }
        PaletteData infData = viewModel.getInfinitePaletteData();
        if (infData != null) {
            PaletteRow row = infData.findRowById(paletteId);
            if (row != null && slot >= 0) {
                row.setSlot(slot, itemId);
                row.updateInfiniteSlots();
                StorageManager.getInstance().getInfinitePaletteRepository().save(infData);
                viewModel.notifyChanges();
            }
        }
    }
}
