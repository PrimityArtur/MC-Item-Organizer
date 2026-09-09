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
        ProfileData profile = viewModel.getActiveProfile();
        if (profile != null) {
            profile.blockItem(itemId);
            StorageManager.getInstance().getProfileRepository().saveProfile(profile);
            viewModel.recomputeUnorganizedItems();
            SoundHelper.playLock();
        }
    }

    @Override
    protected DragSource getDragSource() {
        return DragSource.POR_ORGANIZAR;
    }
}
