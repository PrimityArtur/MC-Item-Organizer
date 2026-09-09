package com.itemorganizer.gui.widget;

import com.itemorganizer.gui.component.ScrollbarComponent;
import com.itemorganizer.gui.dragdrop.DragAndDropManager;
import com.itemorganizer.gui.dragdrop.DragPayload;
import com.itemorganizer.gui.dragdrop.DragSource;
import com.itemorganizer.gui.theme.UITheme;
import com.itemorganizer.gui.util.HotbarActionHelper;
import com.itemorganizer.gui.util.RenderHelper;
import com.itemorganizer.gui.viewmodel.OrganizerViewModel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;

import java.util.List;

// abstract base grid widget for dynamic scrolling item lists
public abstract class AbstractItemGridWidget implements Drawable, Element, Selectable {
    public static final int BASE_SLOT_SIZE = 18;
    public static final int SCROLLBAR_WIDTH = 3;

    protected final OrganizerViewModel viewModel;
    protected int x;
    protected int y;
    protected int width;
    protected int height;

    protected final ScrollbarComponent scrollbar;
    protected int hoveredIndex = -1;
    protected ItemStack hoveredStack = ItemStack.EMPTY;

    protected long lastRightClickTime = 0;
    protected String lastRightClickItemId = null;
    protected int lastShiftIndex = -1;

    public AbstractItemGridWidget(OrganizerViewModel viewModel, int x, int y, int width, int height) {
        this.viewModel = viewModel;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.scrollbar = new ScrollbarComponent(x + width - SCROLLBAR_WIDTH - 2, y + 2, SCROLLBAR_WIDTH, height - 4);
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.scrollbar.setBounds(x + width - SCROLLBAR_WIDTH - 2, y + 2, SCROLLBAR_WIDTH, height - 4);
    }

    public int getSlotSize() {
        float scale = viewModel.getConfig().getScale();
        return Math.max(12, Math.round(BASE_SLOT_SIZE * scale));
    }

    public int getGridWidth() {
        return width - SCROLLBAR_WIDTH - 8;
    }

    public int getColumnCount() {
        int slotSize = getSlotSize();
        return Math.max(1, getGridWidth() / slotSize);
    }

    protected abstract List<String> getItems();

    protected abstract void onItemDoubleRightClick(String itemId);

    protected abstract DragSource getDragSource();

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        List<String> items = getItems();
        int slotSize = getSlotSize();
        int cols = getColumnCount();
        int totalRows = (int) Math.ceil((double) items.size() / cols);
        int totalContentHeight = totalRows * slotSize;

        scrollbar.updateMaxScroll(totalContentHeight, height);
        updateHover(mouseX, mouseY, items);

        // clip content to widget boundaries
        context.enableScissor(x, y, x + width, y + height);

        int scrollY = (int) scrollbar.getScrollOffset();
        int firstVisibleRow = Math.max(0, scrollY / slotSize);
        int lastVisibleRow = Math.min(totalRows, (scrollY + height) / slotSize + 1);

        float itemScale = viewModel.getConfig().getItemScale();

        for (int r = firstVisibleRow; r <= lastVisibleRow; r++) {
            int slotY = y + (r * slotSize) - scrollY;

            for (int c = 0; c < cols; c++) {
                int index = r * cols + c;
                if (index >= items.size()) break;

                int slotX = x + 4 + (c * slotSize);
                boolean isHovered = (index == hoveredIndex);

                String itemId = items.get(index);
                ItemStack stack = RenderHelper.getItemStack(itemId);

                RenderHelper.renderSlot(context, client.textRenderer, stack, slotX, slotY, slotSize, itemScale,
                        isHovered, 0x00000000, UITheme.PRIMARY_BG, 0x00000000, UITheme.PRIMARY);
            }
        }

        context.disableScissor();

        // render scrollbar
        scrollbar.render(context, mouseX, mouseY);

        // tooltip
        if (!DragAndDropManager.getInstance().isDragging() && !hoveredStack.isEmpty() && isMouseOver(mouseX, mouseY)) {
            context.drawItemTooltip(client.textRenderer, hoveredStack, mouseX, mouseY);
        }
    }

    protected void updateHover(double mouseX, double mouseY, List<String> items) {
        hoveredIndex = -1;
        hoveredStack = ItemStack.EMPTY;

        int gridWidth = getGridWidth();
        if (mouseX >= x + 4 && mouseX < x + 4 + gridWidth && mouseY >= y && mouseY < y + height) {
            int slotSize = getSlotSize();
            int relX = (int) mouseX - (x + 4);
            int relY = (int) mouseY - y + (int) scrollbar.getScrollOffset();

            int col = relX / slotSize;
            int row = relY / slotSize;
            int cols = getColumnCount();

            if (col >= 0 && col < cols && row >= 0) {
                int index = row * cols + col;
                if (index >= 0 && index < items.size()) {
                    hoveredIndex = index;
                    hoveredStack = RenderHelper.getItemStack(items.get(index));
                }
            }
        }
    }

    public String getHoveredItemId() {
        List<String> items = getItems();
        if (hoveredIndex >= 0 && hoveredIndex < items.size()) {
            return items.get(hoveredIndex);
        }
        return null;
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        if (scrollbar.mouseClicked(click)) {
            return true;
        }

        List<String> items = getItems();
        if (hoveredIndex >= 0 && hoveredIndex < items.size()) {
            String itemId = items.get(hoveredIndex);

            // right click: double click action
            if (click.button() == 1) {
                long now = System.currentTimeMillis();
                if (now - lastRightClickTime < 350 && itemId.equals(lastRightClickItemId)) {
                    onItemDoubleRightClick(itemId);
                    lastRightClickTime = 0;
                    lastRightClickItemId = null;
                } else {
                    lastRightClickTime = now;
                    lastRightClickItemId = itemId;
                }
                return true;
            }

            // left click: shift-click to hotbar or start drag
            if (click.button() == 0) {
                ItemStack stack = RenderHelper.getItemStack(itemId);
                if (!stack.isEmpty()) {
                    if (HotbarActionHelper.hasShiftDown(click)) {
                        lastShiftIndex = hoveredIndex;
                        HotbarActionHelper.quickMoveToHotbar(MinecraftClient.getInstance(), stack);
                        return true;
                    }
                    DragPayload payload = DragPayload.ofIndexed(itemId, stack, getDragSource(), hoveredIndex, true);
                    DragAndDropManager.getInstance().startDrag(payload);
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(Click click) {
        lastShiftIndex = -1;
        return scrollbar.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (scrollbar.mouseDragged(click, deltaX, deltaY)) {
            return true;
        }

        if (HotbarActionHelper.hasShiftDown(click)) {
            List<String> items = getItems();
            updateHover(click.x(), click.y(), items);
            if (hoveredIndex >= 0 && hoveredIndex < items.size() && hoveredIndex != lastShiftIndex) {
                lastShiftIndex = hoveredIndex;
                String itemId = items.get(hoveredIndex);
                ItemStack stack = RenderHelper.getItemStack(itemId);
                if (!stack.isEmpty()) {
                    HotbarActionHelper.quickMoveToHotbar(MinecraftClient.getInstance(), stack);
                }
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isMouseOver(mouseX, mouseY)) {
            return scrollbar.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        MinecraftClient client = MinecraftClient.getInstance();
        return HotbarActionHelper.handleHotbarKeyPress(client, input, hoveredStack);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    @Override
    public void setFocused(boolean focused) {}

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public SelectionType getType() {
        return SelectionType.NONE;
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {}

    public double getScrollOffset() {
        return scrollbar.getScrollOffset();
    }

    public void setScrollOffset(double offset) {
        scrollbar.setScrollOffset(offset);
    }
}
