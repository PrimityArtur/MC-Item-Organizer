package com.itemorganizer.gui.widget;

import com.itemorganizer.core.model.ProfileData;
import com.itemorganizer.gui.dragdrop.DragSource;
import com.itemorganizer.gui.util.SoundHelper;
import com.itemorganizer.gui.viewmodel.OrganizerViewModel;
import com.itemorganizer.storage.StorageManager;

import java.util.List;

// item grid for unorganized items
public class UnorganizedGridWidget extends AbstractItemGridWidget {

    public UnorganizedGridWidget(OrganizerViewModel viewModel, int x, int y, int width, int height) {
        super(viewModel, x, y, width, height);
    }

    @Override
    protected List<String> getItems() {
        return viewModel.getUnorganizedItems();
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
            profile.blockItem(itemId);
            StorageManager.getInstance().getProfileRepository().saveProfile(profile);
            viewModel.recomputeUnorganizedItems();
            com.itemorganizer.gui.undo.UndoManager.getInstance().record(
                    new com.itemorganizer.gui.undo.ProfileUndoAction(before, profile.snapshot())
            );
            SoundHelper.playLock();
        }
    }

    @Override
    protected boolean handlePayloadDrop(com.itemorganizer.gui.dragdrop.DragPayload payload) {
        if (payload == null || payload.getItemId() == null) return false;
        ProfileData profile = viewModel.getActiveProfile();
        if (profile != null) {
            if (viewModel.isBlockerActive() && profile.isItemBlocked(payload.getItemId())) {
                SoundHelper.playLock();
                return false;
            }
            ProfileData before = profile.snapshot();
            boolean modified = false;
            if (payload.getSource() == DragSource.ORDENADO && !payload.isCopy() && !viewModel.isBlockerActive()) {
                profile.removeAt(payload.getSourceCol(), payload.getSourceRow());
                modified = true;
            }
            if (profile.isItemBlocked(payload.getItemId()) && !viewModel.isBlockerActive()) {
                profile.unblockItem(payload.getItemId());
                modified = true;
            }
            if (modified) {
                StorageManager.getInstance().getProfileRepository().saveProfile(profile);
                viewModel.recomputeUnorganizedItems();
                com.itemorganizer.gui.undo.UndoManager.getInstance().record(
                        new com.itemorganizer.gui.undo.ProfileUndoAction(before, profile.snapshot())
                );
            }
            SoundHelper.playClick();
            return true;
        }
        return false;
    }

    @Override
    protected DragSource getDragSource() {
        return DragSource.POR_ORGANIZAR;
    }
}
