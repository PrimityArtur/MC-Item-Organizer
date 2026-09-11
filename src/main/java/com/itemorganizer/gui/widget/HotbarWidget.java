package com.itemorganizer.gui.widget;

import com.itemorganizer.gui.dragdrop.DragAndDropManager;
import com.itemorganizer.gui.dragdrop.DragPayload;
import com.itemorganizer.gui.dragdrop.DragSource;
import com.itemorganizer.gui.theme.UITheme;
import com.itemorganizer.gui.util.HotbarActionHelper;
import com.itemorganizer.gui.util.RenderHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.input.KeyInput;
import com.itemorganizer.gui.util.SoundHelper;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import org.lwjgl.glfw.GLFW;

// bottom widget rendering the player hotbar slots
public class HotbarWidget implements Drawable, Element, Selectable {
    public static final int SLOT_COUNT = 9;
    public static final int BASE_SLOT_SIZE = 22;
    public static final int TOTAL_WIDTH = SLOT_COUNT * BASE_SLOT_SIZE;
    public static final int TOTAL_HEIGHT = BASE_SLOT_SIZE;

    private int x;
    private int y;
    private float scale = 1.0f;
    private float itemScale = 1.0f;
    private float textScale = 1.0f;
    private int hoveredSlot = -1;
    private int lastShiftSlot = -1;

    public HotbarWidget(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setBounds(int x, int y, float scale, float textScale) {
        setBounds(x, y, scale, 1.0f, textScale);
    }

    public void setBounds(int x, int y, float scale, float itemScale, float textScale) {
        this.x = x;
        this.y = y;
        this.scale = scale;
        this.itemScale = itemScale;
        this.textScale = textScale;
    }

    public int getSlotSize() {
        return Math.max(10, Math.round(BASE_SLOT_SIZE * scale));
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return SLOT_COUNT * getSlotSize();
    }

    public int getHeight() {
        return getSlotSize();
    }

    public int getSlotAt(double mouseX, double mouseY) {
        int slotSize = getSlotSize();
        int totalWidth = SLOT_COUNT * slotSize;
        if (mouseY >= y && mouseY < y + slotSize) {
            int relativeX = (int) mouseX - x;
            if (relativeX >= 0 && relativeX < totalWidth) {
                return relativeX / slotSize;
            }
        }
        return -1;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerInventory inventory = client.player != null ? client.player.getInventory() : null;
        hoveredSlot = getSlotAt(mouseX, mouseY);
        int slotSize = getSlotSize();

        for (int i = 0; i < SLOT_COUNT; i++) {
            int slotX = x + i * slotSize;
            int slotY = y;

            boolean isHovered = (i == hoveredSlot);
            int bgColor = isHovered ? 0x8E1E293B : 0x24BDB7B7;
            int borderColor = isHovered ? UITheme.PRIMARY : 0x4DFFFFFF;
            int badgeColor = isHovered ? UITheme.PRIMARY : 0xFDF2F4F8;

            ItemStack stack = (inventory != null) ? inventory.getStack(i) : ItemStack.EMPTY;

            RenderHelper.renderSlotWithBadge(context, client.textRenderer, stack, String.valueOf(i + 1),
                    slotX, slotY, slotSize, this.itemScale, textScale, isHovered,
                    bgColor, bgColor, borderColor, borderColor, badgeColor, badgeColor);

            if (isHovered && !stack.isEmpty()) {
                context.drawItemTooltip(client.textRenderer, stack, mouseX, mouseY);
            }
        }
    }

    public int getHoveredSlot() {
        return hoveredSlot;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean bl) {
        int slot = getSlotAt(click.x(), click.y());
        if (slot >= 0) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (click.button() == 0 && client.player != null && client.player.isCreative()) {
                // delete item with shift + left click
                boolean hasShift = HotbarActionHelper.hasShiftDown(click);
                if (hasShift) {
                    PlayerInventory inv = client.player.getInventory();
                    ItemStack stack = inv.getStack(slot);
                    if (!stack.isEmpty()) {
                        com.itemorganizer.gui.undo.UndoManager.getInstance().record(
                                new com.itemorganizer.gui.undo.HotbarSlotUndoAction(slot, stack.copy(), ItemStack.EMPTY)
                        );
                        inv.setStack(slot, ItemStack.EMPTY);
                        HotbarActionHelper.assignItemToSlot(client, slot, ItemStack.EMPTY);
                        SoundHelper.playClick();
                        lastShiftSlot = slot;
                        return true;
                    }
                }

                // drop dragged payload if currently dragging
                DragAndDropManager dragManager = DragAndDropManager.getInstance();
                if (dragManager.isDragging()) {
                    DragPayload payload = dragManager.getActivePayload();
                    if (payload != null && HotbarActionHelper.dropPayloadToSlot(client, payload, slot)) {
                        dragManager.consumePayload();
                        return true;
                    }
                }

                // otherwise drag item from this hotbar slot
                ItemStack stack = client.player.getInventory().getStack(slot);
                if (!stack.isEmpty()) {
                    String itemId = Registries.ITEM.getId(stack.getItem()).toString();
                    DragPayload payload = DragPayload.ofIndexed(itemId, stack.copy(), DragSource.HOTBAR, slot, false);
                    dragManager.startDrag(payload, click.x(), click.y());
                    return true;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (click.button() == 0 && HotbarActionHelper.hasShiftDown(click)) {
            int slot = getSlotAt(click.x(), click.y());
            if (slot >= 0 && slot < SLOT_COUNT && slot != lastShiftSlot) {
                lastShiftSlot = slot;
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null && client.player.isCreative()) {
                    PlayerInventory inv = client.player.getInventory();
                    ItemStack stack = inv.getStack(slot);
                    if (!stack.isEmpty()) {
                        com.itemorganizer.gui.undo.UndoManager.getInstance().record(
                                new com.itemorganizer.gui.undo.HotbarSlotUndoAction(slot, stack.copy(), ItemStack.EMPTY)
                        );
                        inv.setStack(slot, ItemStack.EMPTY);
                        HotbarActionHelper.assignItemToSlot(client, slot, ItemStack.EMPTY);
                        SoundHelper.playClick();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(Click click) {
        lastShiftSlot = -1;
        return false;
    }

    public boolean keyPressed(KeyInput input) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (hoveredSlot >= 0 && hoveredSlot < SLOT_COUNT && client.player != null && client.player.isCreative()) {
            for (int i = 0; i < SLOT_COUNT; i++) {
                boolean matches = (client.options != null && client.options.hotbarKeys[i].matchesKey(input))
                        || input.key() == (GLFW.GLFW_KEY_1 + i)
                        || input.key() == (GLFW.GLFW_KEY_KP_1 + i);

                if (matches) {
                    if (i != hoveredSlot) {
                        PlayerInventory inv = client.player.getInventory();
                        ItemStack stackHovered = inv.getStack(hoveredSlot).copy();
                        ItemStack stackTarget = inv.getStack(i).copy();

                        com.itemorganizer.gui.undo.HotbarFullUndoAction undoAction =
                                com.itemorganizer.gui.undo.HotbarFullUndoAction.capture(client);
                        inv.setStack(i, stackHovered);
                        inv.setStack(hoveredSlot, stackTarget);

                        HotbarActionHelper.assignItemToSlot(client, i, stackHovered);
                        HotbarActionHelper.assignItemToSlot(client, hoveredSlot, stackTarget);

                        undoAction.setNewStacks(com.itemorganizer.gui.undo.HotbarFullUndoAction.captureCurrent(client));
                        com.itemorganizer.gui.undo.UndoManager.getInstance().record(undoAction);

                        SoundHelper.playClick();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return getSlotAt(mouseX, mouseY) >= 0;
    }

    @Override
    public void setFocused(boolean focused) {
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public SelectionType getType() {
        return SelectionType.NONE;
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {
    }
}
