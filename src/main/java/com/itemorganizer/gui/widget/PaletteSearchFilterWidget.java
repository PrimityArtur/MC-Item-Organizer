package com.itemorganizer.gui.widget;

import com.itemorganizer.core.model.PaletteRow;
import com.itemorganizer.gui.dragdrop.DragAndDropManager;
import com.itemorganizer.gui.dragdrop.DragPayload;
import com.itemorganizer.gui.dragdrop.DragSource;
import com.itemorganizer.gui.theme.UITheme;
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
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.Objects;

// bottom widget beside hotbar for filtering palettes by slot contents
public class PaletteSearchFilterWidget implements Drawable, Element, Selectable {
    public static final int SLOT_COUNT = 9;

    private final PaletteRow filterPalette;
    private int x;
    private int y;
    private float scale = 1.0f;
    private float itemScale = 1.0f;
    private float textScale = 1.0f;
    private int hoveredSlot = -1;
    private boolean hoveredClearBtn = false;

    public PaletteSearchFilterWidget(int x, int y) {
        this(x, y, new PaletteRow());
    }

    public PaletteSearchFilterWidget(int x, int y, PaletteRow filterPalette) {
        this.x = x;
        this.y = y;
        this.filterPalette = (filterPalette != null) ? filterPalette : new PaletteRow();
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

    public enum MatchTier {
        EXACT(1),
        ITEM_MATCH(2),
        COLOR_MATCH(3),
        NONE(99);

        private final int priority;

        MatchTier(int priority) {
            this.priority = priority;
        }

        public int getPriority() {
            return priority;
        }
    }

    public record FilterMatchResult(MatchTier tier, int matchScore, int totalFilterCount) {
        public boolean isMatch() {
            return tier != MatchTier.NONE;
        }
    }

    public FilterMatchResult evaluateMatch(PaletteRow row) {
        java.util.List<Integer> activeSlots = new java.util.ArrayList<>();
        java.util.List<String> activeFilterItems = new java.util.ArrayList<>();
        for (int i = 0; i < SLOT_COUNT; i++) {
            String item = filterPalette.getSlot(i);
            if (item != null && !item.trim().isEmpty()) {
                activeSlots.add(i);
                activeFilterItems.add(item.trim());
            }
        }

        int totalFilter = activeFilterItems.size();
        if (totalFilter == 0) {
            return new FilterMatchResult(MatchTier.EXACT, 0, 0);
        }
        if (row == null) {
            return new FilterMatchResult(MatchTier.NONE, 0, totalFilter);
        }

        // 1. exact slot match
        boolean exact = true;
        for (int idx = 0; idx < activeSlots.size(); idx++) {
            int slot = activeSlots.get(idx);
            String filterItem = activeFilterItems.get(idx);
            String rowItem = (slot < row.getSlotCount()) ? row.getSlot(slot) : null;
            String normRow = (rowItem != null && !rowItem.trim().isEmpty()) ? rowItem.trim() : null;
            if (!filterItem.equalsIgnoreCase(normRow)) {
                exact = false;
                break;
            }
        }
        if (exact) {
            return new FilterMatchResult(MatchTier.EXACT, totalFilter, totalFilter);
        }

        // collect non-empty row items
        java.util.Set<String> rowItems = new java.util.HashSet<>();
        for (int i = 0; i < row.getSlotCount(); i++) {
            String it = row.getSlot(i);
            if (it != null && !it.trim().isEmpty()) {
                rowItems.add(it.trim().toLowerCase());
            }
        }

        if (rowItems.isEmpty()) {
            return new FilterMatchResult(MatchTier.NONE, 0, totalFilter);
        }

        // 2. item matches anywhere in row
        int itemMatchCount = 0;
        for (String filterItem : activeFilterItems) {
            if (rowItems.contains(filterItem.toLowerCase())) {
                itemMatchCount++;
            }
        }

        if (itemMatchCount == totalFilter) {
            return new FilterMatchResult(MatchTier.ITEM_MATCH, itemMatchCount, totalFilter);
        }

        // 3. color matches
        int colorMatchCount = 0;
        for (String filterItem : activeFilterItems) {
            boolean matched = false;
            for (String rowItem : rowItems) {
                if (com.itemorganizer.gui.util.ItemColorHelper.isSimilarColor(filterItem, rowItem)) {
                    matched = true;
                    break;
                }
            }
            if (matched) {
                colorMatchCount++;
            }
        }

        if (colorMatchCount == totalFilter) {
            return new FilterMatchResult(MatchTier.COLOR_MATCH, colorMatchCount, totalFilter);
        }

        return new FilterMatchResult(MatchTier.NONE, 0, totalFilter);
    }

    public boolean matches(PaletteRow row) {
        if (!filterPalette.hasAnyItem()) {
            return true;
        }
        return evaluateMatch(row).tier() == MatchTier.EXACT;
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
                context, client.textRenderer, title, x + 1, labelY, 0xFF38BDF8, false, textScale
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
                    ? (isHovered ? UITheme.PRIMARY : 0x8038BDF8)
                    : (isHovered ? 0x80FFFFFF : 0x33FFFFFF);
            int badgeColor = isHovered ? UITheme.PRIMARY : 0x8894A3B8;

            ItemStack stack = hasItem ? RenderHelper.getItemStack(itemId) : ItemStack.EMPTY;
            RenderHelper.renderSlotWithBadge(context, client.textRenderer, stack, String.valueOf(i + 1),
                    slotX, slotY, slotSize, this.itemScale, textScale, isHovered,
                    bgColor, bgColor, borderColor, borderColor, badgeColor, badgeColor);

            if (isHovered && !DragAndDropManager.getInstance().isDragging()) {
                if (!stack.isEmpty()) {
                    context.drawItemTooltip(client.textRenderer, stack, mouseX, mouseY);
                } else {
                    context.drawTooltip(client.textRenderer, Text.translatable("palettes.itemorganizer.filter_palette.empty_slot"), mouseX, mouseY);
                }
            }
        }

        // clear filter button
        int clearW = getClearButtonWidth();
        int btnX = x + (SLOT_COUNT * slotSize) + 4;
        int btnY = y;
        boolean hasAny = filterPalette.hasAnyItem();
        int btnBg = hoveredClearBtn ? UITheme.DANGER_HOVER_BG : (hasAny ? UITheme.DANGER_BG : UITheme.BG_SURFACE_HOVER);
        int btnBorder = hoveredClearBtn ? UITheme.DANGER : (hasAny ? UITheme.DANGER_BORDER_MUTED : UITheme.BORDER_SUBTLE);
        int xColor = hoveredClearBtn ? UITheme.TEXT_WHITE : (hasAny ? 0xFFFCA5A5 : UITheme.TEXT_HINT);

        RenderHelper.drawCard(context, btnX, btnY, clearW, slotSize, btnBg, btnBorder);
        float iconScale = Math.max(0.6f, Math.min(1.4f, ((float) clearW / 14.0f) * textScale));
        RenderHelper.drawDeleteIcon(context, btnX + (clearW - 1) / 2.0f, btnY + (slotSize - 1) / 2.0f, iconScale, xColor);

        if (hoveredClearBtn && !DragAndDropManager.getInstance().isDragging()) {
            context.drawTooltip(client.textRenderer, Text.translatable("palettes.itemorganizer.filter_palette.clear"), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        if (click.button() == 0 && isHoveredClear(click.x(), click.y())) {
            if (filterPalette.hasAnyItem()) {
                java.util.List<String> before = new java.util.ArrayList<>(filterPalette.getSlots());
                clearFilter();
                java.util.List<String> after = new java.util.ArrayList<>(filterPalette.getSlots());
                com.itemorganizer.gui.undo.UndoManager.getInstance().record(
                        new com.itemorganizer.gui.undo.PaletteSearchFilterUndoAction(before, after)
                );
                SoundHelper.playBreak();
            }
            return true;
        }

        int slot = getSlotAt(click.x(), click.y());
        if (slot >= 0 && slot < SLOT_COUNT) {
            // right-click: clear slot
            if (click.button() == 1) {
                if (filterPalette.getSlot(slot) != null) {
                    java.util.List<String> before = new java.util.ArrayList<>(filterPalette.getSlots());
                    filterPalette.clearSlot(slot);
                    java.util.List<String> after = new java.util.ArrayList<>(filterPalette.getSlots());
                    com.itemorganizer.gui.undo.UndoManager.getInstance().record(
                            new com.itemorganizer.gui.undo.PaletteSearchFilterUndoAction(before, after)
                    );
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
                        java.util.List<String> before = new java.util.ArrayList<>(filterPalette.getSlots());
                        filterPalette.setSlot(slot, payload.getItemId());
                        java.util.List<String> after = new java.util.ArrayList<>(filterPalette.getSlots());
                        com.itemorganizer.gui.undo.UndoManager.getInstance().record(
                                new com.itemorganizer.gui.undo.PaletteSearchFilterUndoAction(before, after)
                        );
                        SoundHelper.playClick();
                        return true;
                    }
                } else {
                    String itemId = filterPalette.getSlot(slot);
                    if (itemId != null && !itemId.trim().isEmpty()) {
                        ItemStack stack = RenderHelper.getItemStack(itemId);
                        if (!stack.isEmpty()) {
                            DragPayload payload = DragPayload.ofIndexed(itemId, stack.copy(), DragSource.HOTBAR, slot, false);
                            dragManager.startDrag(payload, click.x(), click.y());
                            return true;
                        }
                    }
                }
            }
            return true;
        }

        return false;
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
