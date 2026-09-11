package com.itemorganizer.gui.undo;

import com.itemorganizer.core.model.PaletteData;
import com.itemorganizer.core.model.PaletteRow;
import com.itemorganizer.gui.viewmodel.OrganizerViewModel;
import com.itemorganizer.storage.StorageManager;
import net.minecraft.client.MinecraftClient;

public class PaletteAddDeleteUndoAction implements UndoAction {
    private final PaletteRow row;
    private final int index;
    private final boolean isAdd;
    private final boolean infiniteMode;

    public PaletteAddDeleteUndoAction(PaletteRow row, int index, boolean isAdd, boolean infiniteMode) {
        this.row = (row != null) ? row.snapshot() : null;
        this.index = index;
        this.isAdd = isAdd;
        this.infiniteMode = infiniteMode;
    }

    public PaletteRow getRow() {
        return row;
    }

    public int getIndex() {
        return index;
    }

    public boolean isAdd() {
        return isAdd;
    }

    public boolean isInfiniteMode() {
        return infiniteMode;
    }

    @Override
    public void undo(MinecraftClient client, OrganizerViewModel viewModel) {
        if (row == null) return;
        PaletteData data = null;
        if (viewModel != null) {
            data = infiniteMode ? viewModel.getInfinitePaletteData() : viewModel.getPaletteData();
        }
        if (data == null) {
            data = infiniteMode
                    ? StorageManager.getInstance().getInfinitePaletteRepository().getData()
                    : StorageManager.getInstance().getPaletteRepository().getData();
        }
        if (data == null) return;

        if (isAdd) {
            data.removeRowById(row.getId());
        } else {
            data.insertRow(index, row.snapshot());
        }
        saveAndNotify(viewModel, data);
    }

    @Override
    public void redo(MinecraftClient client, OrganizerViewModel viewModel) {
        if (row == null) return;
        PaletteData data = null;
        if (viewModel != null) {
            data = infiniteMode ? viewModel.getInfinitePaletteData() : viewModel.getPaletteData();
        }
        if (data == null) {
            data = infiniteMode
                    ? StorageManager.getInstance().getInfinitePaletteRepository().getData()
                    : StorageManager.getInstance().getPaletteRepository().getData();
        }
        if (data == null) return;

        if (isAdd) {
            data.insertRow(index, row.snapshot());
        } else {
            data.removeRowById(row.getId());
        }
        saveAndNotify(viewModel, data);
    }

    private void saveAndNotify(OrganizerViewModel viewModel, PaletteData data) {
        if (infiniteMode) {
            StorageManager.getInstance().getInfinitePaletteRepository().save(data);
        } else {
            StorageManager.getInstance().getPaletteRepository().save(data);
        }
        if (viewModel != null) {
            viewModel.notifyChanges();
        }
    }
}
