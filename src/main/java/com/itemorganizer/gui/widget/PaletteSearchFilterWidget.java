package com.itemorganizer.gui.widget;

import com.itemorganizer.core.model.PaletteRow;
import com.itemorganizer.gui.dragdrop.DragAndDropManager;
import com.itemorganizer.gui.dragdrop.DragPayload;
import com.itemorganizer.gui.dragdrop.DragSource;
import com.itemorganizer.gui.util.RenderHelper;
import com.itemorganizer.gui.util.SoundHelper;
import com.itemorganizer.gui.util.TextScaleHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Objects;

// bottom widget beside hotbar for filtering palettes by slot contents
public class PaletteSearchFilterWidget implements Drawable, Element, Selectable {
    public static final int SLOT_COUNT = 9;

    private final PaletteRow filterPalette = new PaletteRow();
    private int x;
    private int y;
    private float scale = 1.0f;
    private float itemScale = 1.0f;
    private float textScale = 1.0f;
    private int hoveredSlot = -1;
    private boolean hoveredClearBtn = false;

    public PaletteSearchFilterWidget(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setBounds(int x, int y, float scale, float itemScale, float textScale) {
        this.x = x;
        this.y = y;
        this.scale = scale;
        this.itemScale = itemScale;
        this.textScale = textScale;
    }

    public int getSlotSize() {
        return Math.max(10, Math.round(HotbarWidget.BASE_SLOT_SIZE * scale));
    }

    public int getClearButtonWidth() {
        return Math.max(12, Math.round(14 * scale));
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return (SLOT_COUNT * getSlotSize()) + 4 + getClearButtonWidth();
    }

    public int getHeight() {
        return getSlotSize();
    }

    public PaletteRow getFilterPalette() {
        return filterPalette;
    }

    public boolean isActive() {
        return filterPalette.hasAnyItem();
    }

    public void clearFilter() {
        filterPalette.clearSlots();
    }

    public boolean matches(PaletteRow row) {
        if (!filterPalette.hasAnyItem()) {
            return true;
        }
        if (row == null) {
            return false;
        }
        for (int i = 0; i < SLOT_COUNT; i++) {
            String filterItem = filterPalette.getSlot(i);
            if (filterItem != null && !filterItem.trim().isEmpty()) {
                String rowItem = row.getSlot(i);
                String normFilter = filterItem.trim();
                String normRow = (rowItem != null && !rowItem.trim().isEmpty()) ? rowItem.trim() : null;
                if (!Objects.equals(normFilter, normRow)) {
                    return false;
                }
            }
        }
        return true;
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

    public boolean isHoveredClear(double mouseX, double mouseY) {
        int slotSize = getSlotSize();
        int clearW = getClearButtonWidth();
        int btnX = x + (SLOT_COUNT * slotSize) + 4;
        return mouseX >= btnX && mouseX <= btnX + clearW && mouseY >= y && mouseY <= y + slotSize;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        hoveredSlot = getSlotAt(mouseX, mouseY);
        hoveredClearBtn = isHoveredClear(mouseX, mouseY);
        int slotSize = getSlotSize();

        // header label above search palette
        Text title = Text.translatable("palettes.itemorganizer.filter_palette.title");
        int labelY = y - Math.max(9, Math.round(9 * textScale)) - 2;
        TextScaleHelper.drawScaledText(
                context, client.textRenderer, title, x + 1, labelY, 0xFF38BDF8, false, Math.min(0.72f, textScale * 0.80f)
        );

        // slots
        for (int i = 0; i < SLOT_COUNT; i++) {
            int slotX = x + i * slotSize;
            int slotY = y;
            boolean isHovered = (i == hoveredSlot);
            String itemId = filterPalette.getSlot(i);
            boolean hasItem = (itemId != null && !itemId.trim().isEmpty());

            int bgColor = hasItem
                    ? (isHovered ? 0x660284C7 : 0x330284C7)
                    : (isHovered ? 0x4D1E293B : 0x24141820);
            int borderColor = hasItem
                    ? (isHovered ? 0xFF38BDF8 : 0x8038BDF8)
                    : (isHovered ? 0x80FFFFFF : 0x33FFFFFF);

            context.fill(slotX, slotY, slotX + slotSize, slotY + slotSize, bgColor);
            RenderHelper.drawBorder(context, slotX, slotY, slotSize, slotSize, borderColor);

            String slotNum = String.valueOf(i + 1);
            int numColor = isHovered ? 0xFF38BDF8 : 0x8894A3B8;
            TextScaleHelper.drawScaledText(
                    context, client.textRenderer, slotNum, slotX + 2, slotY + 2, numColor, false, Math.min(0.65f, textScale * 0.70f)
            );

            if (hasItem) {
                ItemStack stack = getItemStackFromId(itemId);
                if (!stack.isEmpty()) {
                    float renderScale = ((float) slotSize / (float) HotbarWidget.BASE_SLOT_SIZE) * this.itemScale;
                    float cx = slotX + (slotSize - 1) / 2.0f;
                    float cy = slotY + (slotSize - 1) / 2.0f;

                    context.getMatrices().pushMatrix();
                    context.getMatrices().translate(cx, cy);
                    context.getMatrices().scale(renderScale, renderScale);

                    context.drawItem(stack, -8, -8);
                    context.drawStackOverlay(client.textRenderer, stack, -8, -8);

                    context.getMatrices().popMatrix();

                    if (isHovered && !DragAndDropManager.getInstance().isDragging()) {
                        context.drawItemTooltip(client.textRenderer, stack, mouseX, mouseY);
                    }
                }
            } else if (isHovered && !DragAndDropManager.getInstance().isDragging()) {
                context.drawTooltip(client.textRenderer, Text.translatable("palettes.itemorganizer.filter_palette.empty_slot"), mouseX, mouseY);
            }
        }

        // clear filter button
        int clearW = getClearButtonWidth();
        int btnX = x + (SLOT_COUNT * slotSize) + 4;
        int btnY = y;
        boolean hasAny = filterPalette.hasAnyItem();
        int btnBg = hoveredClearBtn ? 0x807F1D1D : (hasAny ? 0x407F1D1D : 0x1AFFFFFF);
        int btnBorder = hoveredClearBtn ? 0xFFEF4444 : (hasAny ? 0x80EF4444 : 0x33FFFFFF);

        context.fill(btnX, btnY, btnX + clearW, btnY + slotSize, btnBg);
        RenderHelper.drawBorder(context, btnX, btnY, clearW, slotSize, btnBorder);

        Text xText = Text.literal("×");
        int xColor = hoveredClearBtn ? 0xFFFFFFFF : (hasAny ? 0xFFFCA5A5 : 0xFF64748B);
        TextScaleHelper.drawCenteredScaledText(
                context, client.textRenderer, xText, btnX + clearW / 2, btnY + (slotSize - 8) / 2, xColor, textScale
        );

        if (hoveredClearBtn && !DragAndDropManager.getInstance().isDragging()) {
            context.drawTooltip(client.textRenderer, Text.translatable("palettes.itemorganizer.filter_palette.clear"), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        if (click.button() == 0 && isHoveredClear(click.x(), click.y())) {
            if (filterPalette.hasAnyItem()) {
                clearFilter();
                SoundHelper.playBreak();
            }
            return true;
        }

        int slot = getSlotAt(click.x(), click.y());
        if (slot >= 0 && slot < SLOT_COUNT) {
            // right-click: clear slot
            if (click.button() == 1) {
                if (filterPalette.getSlot(slot) != null) {
                    filterPalette.clearSlot(slot);
                    SoundHelper.playBreak();
                    return true;
                }
            }

            // left-click: drop payload or drag
            if (click.button() == 0) {
                DragAndDropManager dragManager = DragAndDropManager.getInstance();
                if (dragManager.isDragging()) {
                    DragPayload payload = dragManager.consumePayload();
                    if (payload != null && payload.getItemId() != null) {
                        filterPalette.setSlot(slot, payload.getItemId());
                        SoundHelper.playClick();
                        return true;
                    }
                } else {
                    String itemId = filterPalette.getSlot(slot);
                    if (itemId != null && !itemId.trim().isEmpty()) {
                        ItemStack stack = getItemStackFromId(itemId);
                        if (!stack.isEmpty()) {
                            DragPayload payload = DragPayload.ofIndexed(itemId, stack.copy(), DragSource.HOTBAR, slot, false);
                            dragManager.startDrag(payload);
                            return true;
                        }
                    }
                }
            }
            return true;
        }

        return false;
    }

    private ItemStack getItemStackFromId(String itemId) {
        if (itemId == null || itemId.trim().isEmpty()) {
            return ItemStack.EMPTY;
        }
        try {
            Identifier id = Identifier.of(itemId);
            Item item = Registries.ITEM.get(id);
            if (item != null && item != Items.AIR) {
                return new ItemStack(item);
            }
        } catch (Exception ignored) {
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return getSlotAt(mouseX, mouseY) >= 0 || isHoveredClear(mouseX, mouseY);
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
