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
    private final Deque<UndoAction> redoStack = new ArrayDeque<>();

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
        redoStack.clear();
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public boolean undo(MinecraftClient client, OrganizerViewModel viewModel) {
        if (undoStack.isEmpty()) {
            return false;
        }

        UndoAction action = undoStack.pop();
        action.undo(client, viewModel);
        if (redoStack.size() >= MAX_HISTORY) {
            redoStack.removeLast();
        }
        redoStack.push(action);
        if (client != null) {
            SoundHelper.playClick();
        }
        return true;
    }

    public boolean redo(MinecraftClient client, OrganizerViewModel viewModel) {
        if (redoStack.isEmpty()) {
            return false;
        }

        UndoAction action = redoStack.pop();
        action.redo(client, viewModel);
        if (undoStack.size() >= MAX_HISTORY) {
            undoStack.removeLast();
        }
        undoStack.push(action);
        if (client != null) {
            SoundHelper.playClick();
        }
        return true;
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }

    public int getHistorySize() {
        return undoStack.size();
    }

    public int getRedoHistorySize() {
        return redoStack.size();
    }
}
