package com.itemorganizer.gui.undo;

import com.itemorganizer.gui.palette.CreatePaletteState;
import com.itemorganizer.gui.viewmodel.OrganizerViewModel;
import net.minecraft.client.MinecraftClient;

// records snapshot changes in the create palette studio
public class CreatePaletteUndoAction implements UndoAction {
    private final CreatePaletteState before;
    private final CreatePaletteState after;

    public CreatePaletteUndoAction(CreatePaletteState before, CreatePaletteState after) {
        this.before = before != null ? before.copy() : new CreatePaletteState();
        this.after = after != null ? after.copy() : new CreatePaletteState();
    }

    public CreatePaletteState getBefore() {
        return before;
    }

    public CreatePaletteState getAfter() {
        return after;
    }

    @Override
    public void undo(MinecraftClient client, OrganizerViewModel viewModel) {
        if (viewModel != null && viewModel.getCreatePaletteState() != null) {
            viewModel.getCreatePaletteState().restoreFrom(before);
        }
    }

    @Override
    public void redo(MinecraftClient client, OrganizerViewModel viewModel) {
        if (viewModel != null && viewModel.getCreatePaletteState() != null) {
            viewModel.getCreatePaletteState().restoreFrom(after);
        }
    }
}
