package com.itemorganizer.gui.undo;

import com.itemorganizer.gui.util.HotbarActionHelper;
import com.itemorganizer.gui.viewmodel.OrganizerViewModel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

// restores a single player hotbar slot to its previous stack or re-applies new stack
public class HotbarSlotUndoAction implements UndoAction {
    private final int slot;
    private final ItemStack previousStack;
    private final ItemStack newStack;

    public HotbarSlotUndoAction(int slot, ItemStack previousStack) {
        this(slot, previousStack, ItemStack.EMPTY);
    }

    public HotbarSlotUndoAction(int slot, ItemStack previousStack, ItemStack newStack) {
        this.slot = slot;
        this.previousStack = (previousStack != null) ? previousStack.copy() : ItemStack.EMPTY;
        this.newStack = (newStack != null) ? newStack.copy() : ItemStack.EMPTY;
    }

    public int getSlot() {
        return slot;
    }

    public ItemStack getPreviousStack() {
        return previousStack;
    }

    public ItemStack getNewStack() {
        return newStack;
    }

    @Override
    public void undo(MinecraftClient client, OrganizerViewModel viewModel) {
        HotbarActionHelper.assignItemToSlot(client, slot, previousStack);
    }

    @Override
    public void redo(MinecraftClient client, OrganizerViewModel viewModel) {
        HotbarActionHelper.assignItemToSlot(client, slot, newStack);
    }
}
