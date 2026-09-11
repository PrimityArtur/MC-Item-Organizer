package com.itemorganizer.gui.undo;

import com.itemorganizer.core.model.ProfileData;
import com.itemorganizer.gui.viewmodel.OrganizerViewModel;
import com.itemorganizer.storage.StorageManager;
import net.minecraft.client.MinecraftClient;

// undo and redo action for profile item layout and blocked list modifications
public class ProfileUndoAction implements UndoAction {
    private final String profileName;
    private final ProfileData previousState;
    private final ProfileData newState;

    public ProfileUndoAction(ProfileData previousState, ProfileData newState) {
        this.profileName = (previousState != null) ? previousState.getName()
                : (newState != null ? newState.getName() : "default");
        this.previousState = (previousState != null) ? previousState.snapshot() : null;
        this.newState = (newState != null) ? newState.snapshot() : null;
    }

    public String getProfileName() {
        return profileName;
    }

    public ProfileData getPreviousState() {
        return previousState;
    }

    public ProfileData getNewState() {
        return newState;
    }

    @Override
    public void undo(MinecraftClient client, OrganizerViewModel viewModel) {
        applyState(viewModel, previousState);
    }

    @Override
    public void redo(MinecraftClient client, OrganizerViewModel viewModel) {
        applyState(viewModel, newState);
    }

    private void applyState(OrganizerViewModel viewModel, ProfileData state) {
        if (viewModel == null || state == null) return;
        ProfileData active = viewModel.getActiveProfile();
        if (active != null && active.getName() != null && active.getName().equals(profileName)) {
            active.copyFrom(state);
            StorageManager.getInstance().getProfileRepository().saveProfile(active);
            viewModel.recomputeUnorganizedItems();
            viewModel.notifyChanges();
        }
    }
}
