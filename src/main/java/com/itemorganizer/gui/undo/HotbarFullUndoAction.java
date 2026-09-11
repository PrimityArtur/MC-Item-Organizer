package com.itemorganizer.gui.undo;

import com.itemorganizer.gui.util.HotbarActionHelper;
import com.itemorganizer.gui.viewmodel.OrganizerViewModel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

// restores all 9 player hotbar slots to a previous or redone state
public class HotbarFullUndoAction implements UndoAction {
    private final ItemStack[] previousStacks = new ItemStack[9];
    private final ItemStack[] newStacks = new ItemStack[9];

    public HotbarFullUndoAction(ItemStack[] previousStacks) {
        this(previousStacks, null);
    }

    public HotbarFullUndoAction(ItemStack[] previousStacks, ItemStack[] newStacks) {
        for (int i = 0; i < 9; i++) {
            this.previousStacks[i] = (previousStacks != null && i < previousStacks.length && previousStacks[i] != null)
                    ? previousStacks[i].copy() : ItemStack.EMPTY;
            this.newStacks[i] = (newStacks != null && i < newStacks.length && newStacks[i] != null)
                    ? newStacks[i].copy() : ItemStack.EMPTY;
        }
    }

    public static ItemStack[] captureCurrent(MinecraftClient client) {
        ItemStack[] stacks = new ItemStack[9];
        if (client != null && client.player != null) {
            for (int i = 0; i < 9; i++) {
                ItemStack st = client.player.getInventory().getStack(i);
                stacks[i] = (st != null) ? st.copy() : ItemStack.EMPTY;
            }
        } else {
            for (int i = 0; i < 9; i++) {
                stacks[i] = ItemStack.EMPTY;
            }
        }
        return stacks;
    }

    public static HotbarFullUndoAction capture(MinecraftClient client) {
        return new HotbarFullUndoAction(captureCurrent(client));
    }

    public ItemStack[] getPreviousStacks() {
        return previousStacks;
    }

    public ItemStack[] getNewStacks() {
        return newStacks;
    }

    public void setNewStacks(ItemStack[] newStacks) {
        for (int i = 0; i < 9; i++) {
            this.newStacks[i] = (newStacks != null && i < newStacks.length && newStacks[i] != null)
                    ? newStacks[i].copy() : ItemStack.EMPTY;
        }
    }

    @Override
    public void undo(MinecraftClient client, OrganizerViewModel viewModel) {
        if (client == null || client.player == null) return;
        for (int i = 0; i < 9; i++) {
            HotbarActionHelper.assignItemToSlot(client, i, previousStacks[i]);
        }
    }

    @Override
    public void redo(MinecraftClient client, OrganizerViewModel viewModel) {
        if (client == null || client.player == null) return;
        for (int i = 0; i < 9; i++) {
            HotbarActionHelper.assignItemToSlot(client, i, newStacks[i]);
        }
    }
}
