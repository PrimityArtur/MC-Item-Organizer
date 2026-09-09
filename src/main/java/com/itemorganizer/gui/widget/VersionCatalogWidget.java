package com.itemorganizer.gui.widget;

import com.itemorganizer.core.model.VersionCatalog;
import com.itemorganizer.gui.dragdrop.DragAndDropManager;
import com.itemorganizer.gui.dragdrop.DragPayload;
import com.itemorganizer.gui.dragdrop.DragSource;
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
import com.itemorganizer.gui.util.SoundHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;

// widget grouped by historical minecraft versions
public class VersionCatalogWidget implements Drawable, Element, Selectable {
    public static final int BASE_SLOT_SIZE = 18;
    public static final int SCROLLBAR_WIDTH = 3;
    public static final int HEADER_HEIGHT = 16;
    public static final int GROUP_MARGIN = 8;

    private final OrganizerViewModel viewModel;
    private int x;
    private int y;
    private int width;
    private int height;

    private final VerticalScrollbar scrollbar;
    private String hoveredItemId = null;
    private ItemStack hoveredStack = ItemStack.EMPTY;

    public VersionCatalogWidget(OrganizerViewModel viewModel, int x, int y, int width, int height) {
        this.viewModel = viewModel;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        // scrollbar on right edge
        this.scrollbar = new VerticalScrollbar(x + width - SCROLLBAR_WIDTH - 2, y + 2, SCROLLBAR_WIDTH, height - 4);
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

    public int getHeaderHeight() {
        float textScale = viewModel.getConfig().getTextScale();
        return Math.max(16, Math.round(HEADER_HEIGHT * textScale));
    }

    private int calculateTotalContentHeight() {
        VersionCatalog catalog = viewModel.getVersionCatalog();
        if (catalog == null) return 0;

        int totalH = 0;
        int slotSize = getSlotSize();
        int cols = getColumnCount();
        int headerHeight = getHeaderHeight();

        for (Map.Entry<String, List<String>> entry : catalog.getVersions().entrySet()) {
            totalH += headerHeight;
            List<String> items = entry.getValue();
            int rows = (int) Math.ceil((double) items.size() / cols);
            totalH += (rows * slotSize) + GROUP_MARGIN;
        }
        return totalH;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        VersionCatalog catalog = viewModel.getVersionCatalog();
        if (catalog == null) return;

        int slotSize = getSlotSize();
        int cols = getColumnCount();
        int totalContentHeight = calculateTotalContentHeight();

        scrollbar.updateMaxScroll(totalContentHeight, height);
        updateHover(mouseX, mouseY, catalog);

        // scissor clipping
        context.enableScissor(x, y, x + width, y + height);

        int scrollY = (int) scrollbar.getScrollOffset();
        int currentY = y - scrollY;
        int headerHeight = getHeaderHeight();
        float textScale = viewModel.getConfig().getTextScale();

        for (Map.Entry<String, List<String>> entry : catalog.getVersions().entrySet()) {
            String version = entry.getKey();
            List<String> items = entry.getValue();
            int rows = (int) Math.ceil((double) items.size() / cols);
            int groupHeight = headerHeight + (rows * slotSize) + GROUP_MARGIN;

            // only draw groups intersecting view bounds
            if (currentY + groupHeight >= y && currentY <= y + height) {
                // version header
                int titleY = currentY + (headerHeight - Math.round(8 * textScale)) / 2;
                com.itemorganizer.gui.util.TextScaleHelper.drawScaledText(context, client.textRenderer, Text.literal(version), x + 4, titleY, 0xFF38BDF8, true, textScale);
                int titleWidth = client.textRenderer.getWidth("Minecraft " + version);
                int lineStartX = x + 10 + titleWidth;
                int lineEndX = x + width - SCROLLBAR_WIDTH - 12;
                if (lineEndX > lineStartX) {
                    context.fill(lineStartX, titleY + 4, lineEndX, titleY + 5, 0x2538BDF8);
                }

                int itemsStartY = currentY + headerHeight;

                for (int i = 0; i < items.size(); i++) {
                    int c = i % cols;
                    int r = i / cols;

                    int slotX = x + 4 + (c * slotSize);
                    int slotY = itemsStartY + (r * slotSize);

                    // row culling
                    if (slotY + slotSize >= y && slotY <= y + height) {
                        String itemId = items.get(i);
                        boolean isHovered = itemId.equals(hoveredItemId) && isMouseOver(mouseX, mouseY);

                        // slot background
                        int bgColor = isHovered ? 0x301E293B : 0x00000000;
                        context.fill(slotX, slotY, slotX + slotSize, slotY + slotSize, bgColor);

                        // slot border
                        int borderColor = isHovered ? 0xFF38BDF8 : 0x00000000;
                        RenderHelper.drawBorder(context, slotX, slotY, slotSize, slotSize, borderColor);

                        ItemStack stack = getItemStackFromId(itemId);
                        if (!stack.isEmpty()) {
                            float effectiveScale = ((float) slotSize / 18.0f) * viewModel.getConfig().getItemScale();
                            float cx = slotX + (slotSize - 1) / 2.0f;
                            float cy = slotY + (slotSize - 1) / 2.0f;

                            context.getMatrices().pushMatrix();
                            context.getMatrices().translate(cx, cy);
                            context.getMatrices().scale(effectiveScale, effectiveScale);

                            context.drawItem(stack, -8, -8);
                            context.drawStackOverlay(client.textRenderer, stack, -8, -8);

                            context.getMatrices().popMatrix();
                        }
                    }
                }
            }

            currentY += groupHeight;
        }

        context.disableScissor();

        // render scrollbar
        scrollbar.render(context, mouseX, mouseY);

        // tooltip for hovered item
        if (!DragAndDropManager.getInstance().isDragging() && !hoveredStack.isEmpty() && isMouseOver(mouseX, mouseY)) {
            context.drawItemTooltip(client.textRenderer, hoveredStack, mouseX, mouseY);
        }
    }

    private void updateHover(double mouseX, double mouseY, VersionCatalog catalog) {
        hoveredItemId = null;
        hoveredStack = ItemStack.EMPTY;

        int gridWidth = getGridWidth();
        if (mouseX < x + 4 || mouseX >= x + 4 + gridWidth || mouseY < y || mouseY >= y + height) {
            return;
        }

        int slotSize = getSlotSize();
        int cols = getColumnCount();
        int scrollY = (int) scrollbar.getScrollOffset();
        int currentY = y - scrollY;
        int headerHeight = getHeaderHeight();

        for (Map.Entry<String, List<String>> entry : catalog.getVersions().entrySet()) {
            List<String> items = entry.getValue();
            int rows = (int) Math.ceil((double) items.size() / cols);
            int groupHeight = headerHeight + (rows * slotSize) + GROUP_MARGIN;

            int itemsStartY = currentY + headerHeight;
            if (mouseY >= itemsStartY && mouseY < itemsStartY + (rows * slotSize)) {
                int relX = (int) mouseX - (x + 4);
                int relY = (int) mouseY - itemsStartY;

                int col = relX / slotSize;
                int row = relY / slotSize;

                if (col >= 0 && col < cols && row >= 0) {
                    int index = row * cols + col;
                    if (index >= 0 && index < items.size()) {
                        hoveredItemId = items.get(index);
                        hoveredStack = getItemStackFromId(hoveredItemId);
                        return;
                    }
                }
            }

            currentY += groupHeight;
        }
    }

    public String getHoveredItemId() {
        return hoveredItemId;
    }

    public ItemStack getItemStackFromId(String itemId) {
        if (itemId == null || itemId.isEmpty()) return ItemStack.EMPTY;
        Identifier id = Identifier.tryParse(itemId);
        if (id != null) {
            Item item = Registries.ITEM.get(id);
            if (item != null && item != Items.AIR) {
                return new ItemStack(item);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        if (scrollbar.mouseClicked(click)) {
            return true;
        }

        // start drag or shift-click with left click (catalog items copy by default)
        if (click.button() == 0 && hoveredItemId != null && !hoveredStack.isEmpty()) {
            if (HotbarActionHelper.hasShiftDown(click)) {
                HotbarActionHelper.quickMoveToHotbar(MinecraftClient.getInstance(), hoveredStack);
                return true;
            }
            DragPayload payload = DragPayload.ofIndexed(hoveredItemId, hoveredStack, DragSource.POR_VERSION, 0, true);
            DragAndDropManager.getInstance().startDrag(payload);
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (scrollbar.mouseReleased(click)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (scrollbar.mouseDragged(click, deltaX, deltaY)) {
            return true;
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

        // 1-9 hotbar keys
        if (HotbarActionHelper.handleHotbarKeyPress(client, input, hoveredStack)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
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
