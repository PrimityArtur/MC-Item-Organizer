package com.itemorganizer.gui.undo;

import com.itemorganizer.core.model.PaletteData;
import com.itemorganizer.core.model.PaletteRow;
import com.itemorganizer.gui.viewmodel.OrganizerViewModel;
import com.itemorganizer.storage.StorageManager;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

// restores all slots of a palette row to their previous items
public class PaletteFullUndoAction implements UndoAction {
    private final String paletteId;
    private final List<String> previousSlots;

    public PaletteFullUndoAction(PaletteRow row) {
        this.paletteId = (row != null) ? row.getId() : "";
        this.previousSlots = (row != null && row.getSlots() != null) ? new ArrayList<>(row.getSlots()) : new ArrayList<>();
    }

    public String getPaletteId() {
        return paletteId;
    }

    public List<String> getPreviousSlots() {
        return previousSlots;
    }

    @Override
    public void undo(MinecraftClient client, OrganizerViewModel viewModel) {
        if (viewModel == null) return;
        PaletteData data = viewModel.getPaletteData();
        if (data != null) {
            PaletteRow row = data.findRowById(paletteId);
            if (row != null) {
                row.setSlots(new ArrayList<>(previousSlots));
                StorageManager.getInstance().getPaletteRepository().save(data);
                return;
            }
        }
        PaletteData infData = viewModel.getInfinitePaletteData();
        if (infData != null) {
            PaletteRow row = infData.findRowById(paletteId);
            if (row != null) {
                row.setSlots(new ArrayList<>(previousSlots));
                row.updateInfiniteSlots();
                StorageManager.getInstance().getInfinitePaletteRepository().save(infData);
            }
        }
    }
}
