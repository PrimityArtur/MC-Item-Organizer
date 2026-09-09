package com.itemorganizer.gui.widget;

import com.itemorganizer.core.model.ProfileData;
import com.itemorganizer.gui.dragdrop.DragAndDropManager;
import com.itemorganizer.gui.dragdrop.DragPayload;
import com.itemorganizer.gui.dragdrop.DragSource;
import com.itemorganizer.gui.util.HotbarActionHelper;
import com.itemorganizer.gui.util.RenderHelper;
import com.itemorganizer.gui.util.TextScaleHelper;
import com.itemorganizer.gui.viewmodel.OrganizerViewModel;
import com.itemorganizer.storage.StorageManager;
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

import java.util.Collections;
import java.util.List;

// grid widget for blocked items subtab
public class BlockedGridWidget implements Drawable, Element, Selectable {
    public static final int BASE_SLOT_SIZE = 18;
    public static final int SCROLLBAR_WIDTH = 3;

    private final OrganizerViewModel viewModel;
    private int x;
    private int y;
    private int width;
    private int height;

    private final VerticalScrollbar scrollbar;
    private int hoveredIndex = -1;
    private ItemStack hoveredStack = ItemStack.EMPTY;

    // double right-click tracking
    private long lastRightClickTime = 0;
    private String lastRightClickItemId = null;

    public BlockedGridWidget(OrganizerViewModel viewModel, int x, int y, int width, int height) {
        this.viewModel = viewModel;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

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

    private List<String> getBlockedItems() {
        ProfileData profile = viewModel.getActiveProfile();
        return (profile != null) ? profile.getBlockedItems() : Collections.emptyList();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        List<String> items = getBlockedItems();
        int slotSize = getSlotSize();
        int cols = getColumnCount();
        int totalRows = (int) Math.ceil((double) items.size() / cols);
        int totalContentHeight = totalRows * slotSize;

        scrollbar.updateMaxScroll(totalContentHeight, height);
        updateHover(mouseX, mouseY, items);

        context.enableScissor(x, y, x + width, y + height);

        if (items.isEmpty()) {
            // empty state message
            float textScale = viewModel.getConfig().getTextScale();
            int centerX = x + width / 2;
            int centerY = y + height / 2;

            TextScaleHelper.drawCenteredScaledText(
                    context, client.textRenderer, Text.translatable("blocked.itemorganizer.empty_title"), centerX, centerY - 14, 0xFF38BDF8, textScale * 1.05f
            );
            TextScaleHelper.drawCenteredScaledText(
                    context, client.textRenderer, Text.translatable("blocked.itemorganizer.empty_line1"), centerX, centerY + 2, 0xFF94A3B8, textScale * 0.85f
            );
            TextScaleHelper.drawCenteredScaledText(
                    context, client.textRenderer, Text.translatable("blocked.itemorganizer.empty_line2"), centerX, centerY + 14, 0xFF64748B, textScale * 0.85f
            );

            context.disableScissor();
            return;
        }

        int scrollY = (int) scrollbar.getScrollOffset();
        int firstVisibleRow = Math.max(0, scrollY / slotSize);
        int lastVisibleRow = Math.min(totalRows, (scrollY + height) / slotSize + 1);

        for (int r = firstVisibleRow; r <= lastVisibleRow; r++) {
            int slotY = y + (r * slotSize) - scrollY;

            for (int c = 0; c < cols; c++) {
                int index = r * cols + c;
                if (index >= items.size()) break;

                int slotX = x + 4 + (c * slotSize);
                boolean isHovered = (index == hoveredIndex);

                int slotBg = isHovered ? 0x22FFFFFF : 0x0AFFFFFF;
                int slotBorder = isHovered ? 0xFF38BDF8 : 0x1AFFFFFF;

                context.fill(slotX, slotY, slotX + slotSize, slotY + slotSize, slotBg);
                RenderHelper.drawBorder(context, slotX, slotY, slotSize, slotSize, slotBorder);

                String itemId = items.get(index);
                ItemStack stack = getItemStackFromId(itemId);

                if (!stack.isEmpty()) {
                    float itemScale = ((float) slotSize / (float) BASE_SLOT_SIZE) * 0.95f;
                    float cx = slotX + (slotSize - 1) / 2.0f;
                    float cy = slotY + (slotSize - 1) / 2.0f;

                    context.getMatrices().pushMatrix();
                    context.getMatrices().translate(cx, cy);
                    context.getMatrices().scale(itemScale, itemScale);

                    context.drawItem(stack, -8, -8);
                    context.drawStackOverlay(client.textRenderer, stack, -8, -8);

                    context.getMatrices().popMatrix();
                }

                int badgeColor = isHovered ? 0xFF38BDF8 : 0x6694A3B8;
                TextScaleHelper.drawScaledText(context, client.textRenderer, "🔒", slotX + slotSize - 7, slotY + 1, badgeColor, false, 0.55f);
            }
        }

        context.disableScissor();
        scrollbar.render(context, mouseX, mouseY);

        if (!DragAndDropManager.getInstance().isDragging() && !hoveredStack.isEmpty() && isMouseOver(mouseX, mouseY)) {
            context.drawItemTooltip(client.textRenderer, hoveredStack, mouseX, mouseY);
        }
    }

    private void updateHover(double mouseX, double mouseY, List<String> items) {
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
                    hoveredStack = getItemStackFromId(items.get(index));
                }
            }
        }
    }

    public String getHoveredItemId() {
        List<String> items = getBlockedItems();
        if (hoveredIndex >= 0 && hoveredIndex < items.size()) {
            return items.get(hoveredIndex);
        }
        return null;
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

        if (hoveredIndex >= 0 && !hoveredStack.isEmpty()) {
            List<String> items = getBlockedItems();
            if (hoveredIndex < items.size()) {
                String itemId = items.get(hoveredIndex);

                // double right click to unlock
                if (click.button() == 1) {
                    long now = System.currentTimeMillis();
                    if (now - lastRightClickTime < 350 && itemId.equals(lastRightClickItemId)) {
                        ProfileData profile = viewModel.getActiveProfile();
                        if (profile != null) {
                            profile.unblockItem(itemId);
                            StorageManager.getInstance().getProfileRepository().saveProfile(profile);
                            viewModel.recomputeUnorganizedItems();
                            SoundHelper.playTrapdoorOpen();
                        }
                        lastRightClickTime = 0;
                        lastRightClickItemId = null;
                        return true;
                    } else {
                        lastRightClickTime = now;
                        lastRightClickItemId = itemId;
                        return true;
                    }
                }

                // left click to drag or shift-click to hotbar
                if (click.button() == 0) {
                    if (HotbarActionHelper.hasShiftDown(click)) {
                        HotbarActionHelper.quickMoveToHotbar(MinecraftClient.getInstance(), hoveredStack);
                        return true;
                    }
                    DragPayload payload = DragPayload.ofIndexed(itemId, hoveredStack, DragSource.BLOQUEADO, hoveredIndex, false);
                    DragAndDropManager.getInstance().startDrag(payload);
                    return true;
                }
            }
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

        // 1-9 hotbar key assignment
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
        return isMouseOver(MinecraftClient.getInstance().mouse.getX(), MinecraftClient.getInstance().mouse.getY())
                ? SelectionType.HOVERED : SelectionType.NONE;
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {
    }
}
