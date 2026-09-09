package com.itemorganizer.gui.undo;

import com.itemorganizer.gui.util.HotbarActionHelper;
import com.itemorganizer.gui.viewmodel.OrganizerViewModel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

// restores a single player hotbar slot to its previous stack
public class HotbarSlotUndoAction implements UndoAction {
    private final int slot;
    private final ItemStack previousStack;

    public HotbarSlotUndoAction(int slot, ItemStack previousStack) {
        this.slot = slot;
        this.previousStack = (previousStack != null) ? previousStack.copy() : ItemStack.EMPTY;
    }

    public int getSlot() {
        return slot;
    }

    public ItemStack getPreviousStack() {
        return previousStack;
    }

    @Override
    public void undo(MinecraftClient client, OrganizerViewModel viewModel) {
        HotbarActionHelper.assignItemToSlot(client, slot, previousStack);
    }
}
