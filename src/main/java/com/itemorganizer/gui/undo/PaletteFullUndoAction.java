package com.itemorganizer.gui.undo;

import com.itemorganizer.core.model.PaletteData;
import com.itemorganizer.core.model.PaletteRow;
import com.itemorganizer.gui.viewmodel.OrganizerViewModel;
import com.itemorganizer.storage.StorageManager;
import net.minecraft.client.MinecraftClient;

// restores all 9 slots of a palette row to their previous items
public class PaletteFullUndoAction implements UndoAction {
    private final String paletteId;
    private final String[] previousSlots = new String[9];

    public PaletteFullUndoAction(PaletteRow row) {
        this.paletteId = (row != null) ? row.getId() : "";
        for (int i = 0; i < 9; i++) {
            this.previousSlots[i] = (row != null) ? row.getSlot(i) : null;
        }
    }

    public String getPaletteId() {
        return paletteId;
    }

    public String[] getPreviousSlots() {
        return previousSlots;
    }

    @Override
    public void undo(MinecraftClient client, OrganizerViewModel viewModel) {
        if (viewModel == null) return;
        PaletteData data = viewModel.getPaletteData();
        if (data != null) {
            PaletteRow row = data.findRowById(paletteId);
            if (row != null) {
                for (int i = 0; i < 9; i++) {
                    row.setSlot(i, previousSlots[i]);
                }
                StorageManager.getInstance().getPaletteRepository().save(data);
            }
        }
    }
}
