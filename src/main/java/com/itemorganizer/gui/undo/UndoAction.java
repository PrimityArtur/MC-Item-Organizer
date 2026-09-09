package com.itemorganizer.gui.undo;

import com.itemorganizer.gui.viewmodel.OrganizerViewModel;
import net.minecraft.client.MinecraftClient;

// represents an undoable action executed in the organizer interface
public interface UndoAction {
    void undo(MinecraftClient client, OrganizerViewModel viewModel);
}
