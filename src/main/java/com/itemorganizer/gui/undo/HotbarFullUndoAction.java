package com.itemorganizer.gui.undo;

import com.itemorganizer.gui.util.HotbarActionHelper;
import com.itemorganizer.gui.viewmodel.OrganizerViewModel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

// restores all 9 player hotbar slots to a previous state
public class HotbarFullUndoAction implements UndoAction {
    private final ItemStack[] previousStacks = new ItemStack[9];

    public HotbarFullUndoAction(ItemStack[] stacks) {
        for (int i = 0; i < 9; i++) {
            this.previousStacks[i] = (stacks != null && i < stacks.length && stacks[i] != null) ? stacks[i].copy() : ItemStack.EMPTY;
        }
    }

    public static HotbarFullUndoAction capture(MinecraftClient client) {
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
        return new HotbarFullUndoAction(stacks);
    }

    public ItemStack[] getPreviousStacks() {
        return previousStacks;
    }

    @Override
    public void undo(MinecraftClient client, OrganizerViewModel viewModel) {
        if (client == null || client.player == null) return;
        for (int i = 0; i < 9; i++) {
            HotbarActionHelper.assignItemToSlot(client, i, previousStacks[i]);
        }
    }
}
