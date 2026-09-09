package com.itemorganizer.gui.undo;

import com.itemorganizer.gui.util.SoundHelper;
import com.itemorganizer.gui.viewmodel.OrganizerViewModel;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayDeque;
import java.util.Deque;

// session-scoped in-memory undo manager for player interface edits
public class UndoManager {
    private static final UndoManager INSTANCE = new UndoManager();
    private static final int MAX_HISTORY = 100;

    private final Deque<UndoAction> undoStack = new ArrayDeque<>();

    private UndoManager() {
    }

    public static UndoManager getInstance() {
        return INSTANCE;
    }

    public void record(UndoAction action) {
        if (action == null) return;
        if (undoStack.size() >= MAX_HISTORY) {
            undoStack.removeLast();
        }
        undoStack.push(action);
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean undo(MinecraftClient client, OrganizerViewModel viewModel) {
        if (undoStack.isEmpty()) {
            return false;
        }

        UndoAction action = undoStack.pop();
        action.undo(client, viewModel);
        if (client != null) {
            SoundHelper.playClick();
        }
        return true;
    }

    public void clear() {
        undoStack.clear();
    }

    public int getHistorySize() {
        return undoStack.size();
    }
}
