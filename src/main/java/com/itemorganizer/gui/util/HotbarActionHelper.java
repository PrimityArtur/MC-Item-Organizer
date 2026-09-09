package com.itemorganizer.gui.util;

import com.itemorganizer.gui.dragdrop.DragPayload;
import com.itemorganizer.gui.dragdrop.DragSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

// utility helper for player hotbar interaction, slot finding, and keybindings
public final class HotbarActionHelper {

    private HotbarActionHelper() {
    }

    // finds the first empty hotbar slot (indices 0 to 8), or -1 if full
    public static int findFirstEmptySlot(PlayerInventory inventory) {
        if (inventory == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (inventory.getStack(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    // assigns an item to a specific hotbar slot and synchronizes with server
    public static void assignItemToSlot(MinecraftClient client, int slot, ItemStack stack) {
        if (client == null || client.player == null || !client.player.isCreative() || slot < 0 || slot >= 9) {
            return;
        }

        ItemStack giveStack = (stack == null || stack.isEmpty()) ? ItemStack.EMPTY : stack.copy();
        if (!giveStack.isEmpty()) {
            giveStack.setCount(1);
        }
        client.player.getInventory().setStack(slot, giveStack);
        if (client.player.playerScreenHandler != null) {
            try {
                client.player.playerScreenHandler.getSlot(36 + slot).setStack(giveStack);
            } catch (Exception ignored) {
            }
        }

        if (client.interactionManager != null) {
            client.interactionManager.clickCreativeStack(giveStack, 36 + slot);
        } else if (client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(36 + slot, giveStack));
        }
    }

    // checks whether the specified item is already present in the player hotbar (0..8)
    public static boolean isItemInHotbar(PlayerInventory inventory, ItemStack stack) {
        if (inventory == null || stack == null || stack.isEmpty()) {
            return false;
        }
        for (int i = 0; i < 9; i++) {
            ItemStack slotStack = inventory.getStack(i);
            if (!slotStack.isEmpty() && slotStack.getItem() == stack.getItem()) {
                return true;
            }
        }
        return false;
    }

    // attempts to insert an item into the first available hotbar slot
    public static boolean quickMoveToHotbar(MinecraftClient client, ItemStack stack) {
        if (client == null || client.player == null || !client.player.isCreative() || stack == null || stack.isEmpty()) {
            return false;
        }

        PlayerInventory inventory = client.player.getInventory();
        if (isItemInHotbar(inventory, stack)) {
            return false;
        }

        int emptySlot = findFirstEmptySlot(inventory);
        if (emptySlot < 0) {
            return false;
        }

        ItemStack previousStack = inventory.getStack(emptySlot).copy();
        com.itemorganizer.gui.undo.UndoManager.getInstance().record(
                new com.itemorganizer.gui.undo.HotbarSlotUndoAction(emptySlot, previousStack)
        );

        assignItemToSlot(client, emptySlot, stack);
        SoundHelper.playClick();
        return true;
    }

    // drops a dragged payload into the specified hotbar slot, or swaps if source is hotbar
    public static boolean dropPayloadToSlot(MinecraftClient client, DragPayload payload, int targetSlot) {
        if (client == null || client.player == null || !client.player.isCreative() || payload == null || targetSlot < 0 || targetSlot >= 9) {
            return false;
        }

        if (payload.getSource() == DragSource.HOTBAR) {
            int sourceSlot = payload.getSourceIndex();
            if (sourceSlot >= 0 && sourceSlot < 9) {
                if (sourceSlot != targetSlot) {
                    com.itemorganizer.gui.undo.UndoManager.getInstance().record(
                            com.itemorganizer.gui.undo.HotbarFullUndoAction.capture(client)
                    );

                    PlayerInventory inv = client.player.getInventory();
                    ItemStack sourceStack = inv.getStack(sourceSlot).copy();
                    ItemStack targetStack = inv.getStack(targetSlot).copy();
                    inv.setStack(targetSlot, sourceStack);
                    inv.setStack(sourceSlot, targetStack);

                    assignItemToSlot(client, targetSlot, sourceStack);
                    assignItemToSlot(client, sourceSlot, targetStack);
                    SoundHelper.playClick();
                }
                return true;
            }
            return false;
        }

        ItemStack stack = payload.getItemStack();
        if (stack == null || stack.isEmpty()) {
            if (payload.getItemId() != null) {
                Identifier id = Identifier.tryParse(payload.getItemId());
                if (id != null) {
                    Item item = Registries.ITEM.get(id);
                    if (item != null && item != Items.AIR) {
                        stack = new ItemStack(item);
                    }
                }
            }
        }

        if (stack != null && !stack.isEmpty()) {
            ItemStack previous = client.player.getInventory().getStack(targetSlot).copy();
            com.itemorganizer.gui.undo.UndoManager.getInstance().record(
                    new com.itemorganizer.gui.undo.HotbarSlotUndoAction(targetSlot, previous)
            );

            assignItemToSlot(client, targetSlot, stack);
            SoundHelper.playClick();
            return true;
        }

        return false;
    }

    // handles 1-9 hotbar key assignments when hovering over an item
    public static boolean handleHotbarKeyPress(MinecraftClient client, KeyInput input, ItemStack hoveredStack) {
        if (client == null || client.player == null || hoveredStack == null || hoveredStack.isEmpty()) {
            return false;
        }

        for (int i = 0; i < 9; i++) {
            boolean matches = (client.options != null && client.options.hotbarKeys[i].matchesKey(input))
                    || input.key() == (GLFW.GLFW_KEY_1 + i)
                    || input.key() == (GLFW.GLFW_KEY_KP_1 + i);

            if (matches) {
                ItemStack previous = client.player.getInventory().getStack(i).copy();
                com.itemorganizer.gui.undo.UndoManager.getInstance().record(
                        new com.itemorganizer.gui.undo.HotbarSlotUndoAction(i, previous)
                );
                assignItemToSlot(client, i, hoveredStack);
                SoundHelper.playClick();
                return true;
            }
        }
        return false;
    }

    // checks whether the shift key is currently pressed
    public static boolean hasShiftDown(Click click) {
        if (click != null && click.hasShift()) {
            return true;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getWindow() != null) {
            return InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                    || InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
        }
        return false;
    }

    // checks whether the control key is currently pressed
    public static boolean hasControlDown() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getWindow() != null) {
            return InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL)
                    || InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL)
                    || InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_LEFT_SUPER)
                    || InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_RIGHT_SUPER);
        }
        return false;
    }
}
