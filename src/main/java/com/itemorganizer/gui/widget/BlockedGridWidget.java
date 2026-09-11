package com.itemorganizer.gui.widget;

import com.itemorganizer.core.model.ProfileData;
import com.itemorganizer.gui.dragdrop.DragSource;
import com.itemorganizer.gui.util.SoundHelper;
import com.itemorganizer.gui.viewmodel.OrganizerViewModel;
import com.itemorganizer.storage.StorageManager;

import java.util.Collections;
import java.util.List;

// grid widget for blocked items subtab
public class BlockedGridWidget extends AbstractItemGridWidget {

    public BlockedGridWidget(OrganizerViewModel viewModel, int x, int y, int width, int height) {
        super(viewModel, x, y, width, height);
    }

    @Override
    protected List<String> getItems() {
        ProfileData profile = viewModel.getActiveProfile();
        return (profile != null) ? profile.getBlockedItems() : Collections.emptyList();
    }

    @Override
    protected void onItemDoubleRightClick(String itemId) {
        if (viewModel.isBlockerActive()) {
            SoundHelper.playLock();
            return;
        }
        ProfileData profile = viewModel.getActiveProfile();
        if (profile != null) {
            ProfileData before = profile.snapshot();
            profile.unblockItem(itemId);
            StorageManager.getInstance().getProfileRepository().saveProfile(profile);
            viewModel.recomputeUnorganizedItems();
            com.itemorganizer.gui.undo.UndoManager.getInstance().record(
                    new com.itemorganizer.gui.undo.ProfileUndoAction(before, profile.snapshot())
            );
            SoundHelper.playBreak();
        }
    }

    @Override
    protected boolean handlePayloadDrop(com.itemorganizer.gui.dragdrop.DragPayload payload) {
        if (payload == null || payload.getItemId() == null) return false;
        if (viewModel.isBlockerActive()) {
            SoundHelper.playLock();
            return false;
        }
        ProfileData profile = viewModel.getActiveProfile();
        if (profile != null) {
            ProfileData before = profile.snapshot();
            if (payload.getSource() == DragSource.ORDENADO && !payload.isCopy()) {
                profile.removeAt(payload.getSourceCol(), payload.getSourceRow());
            }
            profile.blockItem(payload.getItemId());
            StorageManager.getInstance().getProfileRepository().saveProfile(profile);
            viewModel.recomputeUnorganizedItems();
            com.itemorganizer.gui.undo.UndoManager.getInstance().record(
                    new com.itemorganizer.gui.undo.ProfileUndoAction(before, profile.snapshot())
            );
            SoundHelper.playLock();
            return true;
        }
        return false;
    }

    @Override
    protected DragSource getDragSource() {
        return DragSource.BLOQUEADO;
    }
}
