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
        ProfileData profile = viewModel.getActiveProfile();
        if (profile != null) {
            profile.unblockItem(itemId);
            StorageManager.getInstance().getProfileRepository().saveProfile(profile);
            viewModel.recomputeUnorganizedItems();
            SoundHelper.playBreak();
        }
    }

    @Override
    protected DragSource getDragSource() {
        return DragSource.POR_ORGANIZAR;
    }
}
