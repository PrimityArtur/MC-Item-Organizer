package com.itemorganizer.gui.widget;

import com.itemorganizer.gui.dragdrop.DragAndDropManager;
import com.itemorganizer.gui.dragdrop.DragPayload;
import com.itemorganizer.gui.dragdrop.DragSource;
import com.itemorganizer.gui.util.HotbarActionHelper;
import com.itemorganizer.gui.util.RenderHelper;
import net.minecraft.client.MinecraftClient;
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
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
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

            // slot background
            boolean isHovered = (i == hoveredSlot);
            int bgColor = isHovered ? 0x8E1E293B : 0x24BDB7B7;
            int borderColor = isHovered ? 0xFF38BDF8 : 0x4DFFFFFF;

            context.fill(slotX, slotY, slotX + slotSize, slotY + slotSize, bgColor);
            RenderHelper.drawBorder(context, slotX, slotY, slotSize, slotSize, borderColor);

            // slot number (1-9) in top-left
            String slotNum = String.valueOf(i + 1);
            int numColor = isHovered ? 0xFF38BDF8 : 0xFDF2F4F8;
            com.itemorganizer.gui.util.TextScaleHelper.drawScaledText(context, client.textRenderer, slotNum, slotX + 2, slotY + 2, numColor, false, textScale);

            // current hotbar item
            if (inventory != null) {
                ItemStack stack = inventory.getStack(i);
                if (!stack.isEmpty()) {
                    float renderScale = ((float) slotSize / (float) BASE_SLOT_SIZE) * this.itemScale;
                    float cx = slotX + (slotSize - 1) / 2.0f;
                    float cy = slotY + (slotSize - 1) / 2.0f;

                    context.getMatrices().pushMatrix();
                    context.getMatrices().translate(cx, cy);
                    context.getMatrices().scale(renderScale, renderScale);

                    context.drawItem(stack, -8, -8);
                    context.drawStackOverlay(client.textRenderer, stack, -8, -8);

                    context.getMatrices().popMatrix();

                    // render tooltip on hover
                    if (isHovered) {
                        context.drawItemTooltip(client.textRenderer, stack, mouseX, mouseY);
                    }
                }
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
            if (click.button() == 0 && client.player != null) {
                // delete item with shift + left click
                boolean hasShift = HotbarActionHelper.hasShiftDown(click);
                if (hasShift) {
                    PlayerInventory inv = client.player.getInventory();
                    ItemStack stack = inv.getStack(slot);
                    if (!stack.isEmpty()) {
                        inv.setStack(slot, ItemStack.EMPTY);
                        HotbarActionHelper.assignItemToSlot(client, slot, ItemStack.EMPTY);
                        SoundHelper.playClick();
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
                    dragManager.startDrag(payload);
                    return true;
                }
            }
            return true;
        }
        return false;
    }

    public boolean keyPressed(KeyInput input) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (hoveredSlot >= 0 && hoveredSlot < SLOT_COUNT && client.player != null) {
            for (int i = 0; i < SLOT_COUNT; i++) {
                boolean matches = (client.options != null && client.options.hotbarKeys[i].matchesKey(input))
                        || input.key() == (GLFW.GLFW_KEY_1 + i)
                        || input.key() == (GLFW.GLFW_KEY_KP_1 + i);

                if (matches) {
                    if (i != hoveredSlot) {
                        PlayerInventory inv = client.player.getInventory();
                        ItemStack stackHovered = inv.getStack(hoveredSlot).copy();
                        ItemStack stackTarget = inv.getStack(i).copy();

                        inv.setStack(i, stackHovered);
                        inv.setStack(hoveredSlot, stackTarget);

                        HotbarActionHelper.assignItemToSlot(client, i, stackHovered);
                        HotbarActionHelper.assignItemToSlot(client, hoveredSlot, stackTarget);

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
