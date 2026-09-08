package com.itemorganizer.gui.widget;

import com.itemorganizer.core.model.ItemSlotPosition;
import com.itemorganizer.core.model.ProfileData;
import com.itemorganizer.gui.dragdrop.DragAndDropManager;
import com.itemorganizer.gui.dragdrop.DragPayload;
import com.itemorganizer.gui.dragdrop.DragSource;
import com.itemorganizer.gui.util.RenderHelper;
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
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;

// main organized item grid with drag-and-drop, ripple insert, selection, and keyboard navigation
public class OrderedGridWidget implements Drawable, Element, Selectable {
    public static final int BASE_SLOT_SIZE = 18;
    public static final int SCROLLBAR_WIDTH = 3;
    public static final int SCROLLBAR_MARGIN = 2;

    private final OrganizerViewModel viewModel;
    private int x;
    private int y;
    private int width;
    private int height;

    private final VerticalScrollbar scrollbar;
    private int hoveredCol = -1;
    private int hoveredRow = -1;
    private ItemStack hoveredStack = ItemStack.EMPTY;

    // selected slot via right click
    private int selectedCol = -1;
    private int selectedRow = -1;

    // double click tracking for lock
    private long lastRightClickTime = 0;
    private int lastRightClickCol = -1;
    private int lastRightClickRow = -1;

    private long lastLeftClickTime = 0;
    private int lastLeftClickCol = -1;
    private int lastLeftClickRow = -1;

    public OrderedGridWidget(OrganizerViewModel viewModel, int x, int y, int width, int height) {
        this.viewModel = viewModel;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        // scrollbar on the left side
        this.scrollbar = new VerticalScrollbar(x + 2, y + 2, SCROLLBAR_WIDTH, height - 4);
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.scrollbar.setBounds(x + 2, y + 2, SCROLLBAR_WIDTH, height - 4);
        reflowIfNeeded();
    }

    // reflow items if grid column count changed due to resize or zoom
    public void reflowIfNeeded() {
        ProfileData profile = viewModel.getActiveProfile();
        if (profile != null) {
            int currentCols = getColumnCount();
            if (currentCols > 0 && profile.reflowToColumns(currentCols)) {
                StorageManager.getInstance().getProfileRepository().saveProfile(profile);
            }
        }
    }

    public int getSlotSize() {
        float scale = viewModel.getConfig().getScale();
        return Math.max(2, Math.round(BASE_SLOT_SIZE * scale));
    }

    public int getGridStartX() {
        return x + SCROLLBAR_WIDTH + SCROLLBAR_MARGIN + 2;
    }

    public int getGridWidth() {
        return (x + width) - getGridStartX();
    }

    public int getColumnCount() {
        int slotSize = getSlotSize();
        return Math.max(1, getGridWidth() / slotSize);
    }

    public int getMaxRow() {
        ProfileData profile = viewModel.getActiveProfile();
        int maxRow = 0;
        if (profile != null) {
            for (ItemSlotPosition pos : profile.getItems()) {
                if (pos.getY() > maxRow) {
                    maxRow = pos.getY();
                }
            }
        }
        return Math.max(16, maxRow + 8);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        reflowIfNeeded();
        MinecraftClient client = MinecraftClient.getInstance();
        int slotSize = getSlotSize();
        int cols = getColumnCount();
        int totalRows = getMaxRow();
        int totalContentHeight = totalRows * slotSize;

        scrollbar.updateMaxScroll(totalContentHeight, height);
        updateHover(mouseX, mouseY);

        context.enableScissor(x, y, x + width, y + height);

        int gridStartX = getGridStartX();
        int scrollY = (int) scrollbar.getScrollOffset();

        ProfileData profile = viewModel.getActiveProfile();

        // viewport culling
        int firstVisibleRow = Math.max(0, scrollY / slotSize);
        int lastVisibleRow = Math.min(totalRows, (scrollY + height) / slotSize + 1);

        for (int r = firstVisibleRow; r <= lastVisibleRow; r++) {
            int slotY = y + (r * slotSize) - scrollY;

            for (int c = 0; c < cols; c++) {
                int slotX = gridStartX + (c * slotSize);

                boolean isHovered = (c == hoveredCol && r == hoveredRow);
                boolean isSelected = (c == selectedCol && r == selectedRow);

                int slotBg = isSelected ? 0x4D38BDF8 : (isHovered ? 0x22FFFFFF : 0x00000000);
                int slotBorder = isSelected ? 0xFF38BDF8 : (isHovered ? 0x66FFFFFF : 0x00000000);

                context.fill(slotX, slotY, slotX + slotSize, slotY + slotSize, slotBg);
                RenderHelper.drawBorder(context, slotX, slotY, slotSize, slotSize, slotBorder);

                if (profile != null) {
                    Optional<ItemSlotPosition> posOpt = profile.findItemAt(c, r);
                    if (posOpt.isPresent()) {
                        String itemId = posOpt.get().getItemId();
                        ItemStack stack = getItemStackFromId(itemId);
                        if (!stack.isEmpty()) {
                            float itemScale = ((float) slotSize / (float) BASE_SLOT_SIZE) * viewModel.getConfig().getItemScale();
                            float cx = slotX + (slotSize - 1) / 2.0f;
                            float cy = slotY + (slotSize - 1) / 2.0f;

                            context.getMatrices().pushMatrix();
                            context.getMatrices().translate(cx, cy);
                            context.getMatrices().scale(itemScale, itemScale);

                            context.drawItem(stack, -8, -8);
                            context.drawStackOverlay(client.textRenderer, stack, -8, -8);

                            context.getMatrices().popMatrix();
                        }
                    }
                }
            }
        }

        context.disableScissor();

        scrollbar.render(context, mouseX, mouseY);

        if (!DragAndDropManager.getInstance().isDragging() && !hoveredStack.isEmpty() && isMouseOver(mouseX, mouseY)) {
            context.drawItemTooltip(client.textRenderer, hoveredStack, mouseX, mouseY);
        }
    }

    private void updateHover(double mouseX, double mouseY) {
        hoveredCol = -1;
        hoveredRow = -1;
        hoveredStack = ItemStack.EMPTY;

        int gridStartX = getGridStartX();
        if (mouseX >= gridStartX && mouseX < x + width && mouseY >= y && mouseY <= y + height) {
            int slotSize = getSlotSize();
            int relX = (int) mouseX - gridStartX;
            int relY = (int) mouseY - y + (int) scrollbar.getScrollOffset();

            int col = relX / slotSize;
            int row = relY / slotSize;

            if (col >= 0 && col < getColumnCount()) {
                hoveredCol = col;
                hoveredRow = row;

                ProfileData profile = viewModel.getActiveProfile();
                if (profile != null) {
                    profile.findItemAt(col, row).ifPresent(pos -> {
                        hoveredStack = getItemStackFromId(pos.getItemId());
                    });
                }
            }
        }
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

        if (isMouseOver(click.x(), click.y())) {
            int gridStartX = getGridStartX();
            if (click.x() >= gridStartX) {
                int slotSize = getSlotSize();
                int relX = (int) click.x() - gridStartX;
                int relY = (int) click.y() - y + (int) scrollbar.getScrollOffset();
                int col = relX / slotSize;
                int row = relY / slotSize;

                if (col >= 0 && col < getColumnCount()) {
                    ProfileData profile = viewModel.getActiveProfile();
                    long now = System.currentTimeMillis();

                    // right click: select slot or double click to lock
                    if (click.button() == 1) {
                        if (profile != null && profile.findItemAt(col, row).isPresent()) {
                            if (now - lastRightClickTime < 350 && col == lastRightClickCol && row == lastRightClickRow) {
                                Optional<ItemSlotPosition> slotItem = profile.findItemAt(col, row);
                                if (slotItem.isPresent()) {
                                    String itemId = slotItem.get().getItemId();
                                    profile.removeAt(col, row);
                                    profile.blockItem(itemId);
                                    StorageManager.getInstance().getProfileRepository().saveProfile(profile);
                                    viewModel.recomputeUnorganizedItems();
                                    selectedCol = -1;
                                    selectedRow = -1;
                                    SoundHelper.playTrapdoorClose();
                                }
                                lastRightClickTime = 0;
                                return true;
                            }

                            selectedCol = col;
                            selectedRow = row;
                            lastRightClickTime = now;
                            lastRightClickCol = col;
                            lastRightClickRow = row;
                            SoundHelper.playClick();
                        } else {
                            selectedCol = -1;
                            selectedRow = -1;
                            lastRightClickTime = 0;
                        }
                        return true;
                    }

                    // left click: drag or double click to lock
                    if (click.button() == 0 && profile != null) {
                        Optional<ItemSlotPosition> slotItem = profile.findItemAt(col, row);
                        if (slotItem.isPresent()) {
                            String itemId = slotItem.get().getItemId();

                            if (now - lastLeftClickTime < 350 && col == lastLeftClickCol && row == lastLeftClickRow) {
                                profile.removeAt(col, row);
                                profile.blockItem(itemId);
                                StorageManager.getInstance().getProfileRepository().saveProfile(profile);
                                viewModel.recomputeUnorganizedItems();
                                selectedCol = -1;
                                selectedRow = -1;
                                DragAndDropManager.getInstance().consumePayload();
                                SoundHelper.playTrapdoorClose();
                                lastLeftClickTime = 0;
                                return true;
                            }

                            lastLeftClickTime = now;
                            lastLeftClickCol = col;
                            lastLeftClickRow = row;

                            ItemStack stack = getItemStackFromId(itemId);
                            if (!stack.isEmpty()) {
                                boolean isCopy = viewModel.isBlockerActive();
                                DragPayload payload = DragPayload.ofGrid(itemId, stack, DragSource.ORDENADO, col, row, isCopy);
                                DragAndDropManager.getInstance().startDrag(payload);
                                selectedCol = -1;
                                selectedRow = -1;
                                return true;
                            }
                        }
                    }
                }
            }
        }

        if (selectedCol >= 0) {
            selectedCol = -1;
            selectedRow = -1;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (scrollbar.mouseReleased(click)) {
            return true;
        }

        DragAndDropManager dragManager = DragAndDropManager.getInstance();
        if (dragManager.isDragging()) {
            if (isMouseOver(click.x(), click.y())) {
                if (!viewModel.isBlockerActive() && hoveredCol >= 0 && hoveredRow >= 0) {
                    DragPayload payload = dragManager.getActivePayload();
                    ProfileData profile = viewModel.getActiveProfile();
                    if (profile != null) {
                        if (payload.getSource() == DragSource.ORDENADO && !payload.isCopy()) {
                            profile.removeAt(payload.getSourceCol(), payload.getSourceRow());
                        } else if (payload.getSource() == DragSource.POR_VERSION) {
                            profile.removeItem(payload.getItemId());
                        }

                        insertWithRippleWrap(profile, payload.getItemId(), hoveredCol, hoveredRow);

                        StorageManager.getInstance().getProfileRepository().saveProfile(profile);
                        viewModel.recomputeUnorganizedItems();
                        SoundHelper.playClick();
                    }
                }
                dragManager.consumePayload();
                return true;
            }
            return false;
        }

        return false;
    }

    // insert item shifting subsequent items to the right and wrapping across rows
    public void insertWithRippleWrap(ProfileData profile, String itemId, int targetCol, int targetRow) {
        int cols = getColumnCount();
        int targetIdx = targetRow * cols + targetCol;

        // place directly if slot is empty
        if (profile.findItemAt(targetCol, targetRow).isEmpty()) {
            profile.setItemAt(itemId, targetCol, targetRow);
            return;
        }

        // find next empty slot
        int emptyIdx = targetIdx + 1;
        while (true) {
            int checkCol = emptyIdx % cols;
            int checkRow = emptyIdx / cols;
            if (profile.findItemAt(checkCol, checkRow).isEmpty()) {
                break;
            }
            emptyIdx++;
        }

        // shift items forward
        for (int i = emptyIdx - 1; i >= targetIdx; i--) {
            int fromCol = i % cols;
            int fromRow = i / cols;
            int toCol = (i + 1) % cols;
            int toRow = (i + 1) / cols;

            Optional<ItemSlotPosition> movingItem = profile.findItemAt(fromCol, fromRow);
            if (movingItem.isPresent()) {
                String id = movingItem.get().getItemId();
                profile.removeAt(fromCol, fromRow);
                profile.setItemAt(id, toCol, toRow);
            }
        }

        profile.setItemAt(itemId, targetCol, targetRow);
        profile.setColumnCount(cols);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        MinecraftClient client = MinecraftClient.getInstance();

        // 1-9 hotbar key assignment
        if (!hoveredStack.isEmpty() && client.player != null) {
            for (int i = 0; i < 9; i++) {
                if (client.options.hotbarKeys[i].matchesKey(input)) {
                    if (client.player.isCreative()) {
                        ItemStack giveStack = new ItemStack(hoveredStack.getItem(), 1);
                        client.player.getInventory().setStack(i, giveStack);
                        if (client.getNetworkHandler() != null) {
                            client.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(36 + i, giveStack));
                        }
                        SoundHelper.playClick();
                        return true;
                    }
                }
            }
        }

        // deselect with ESC
        if (selectedCol >= 0 && input.key() == GLFW.GLFW_KEY_ESCAPE) {
            selectedCol = -1;
            selectedRow = -1;
            return true;
        }

        // delete with DEL or Backspace
        if (selectedCol >= 0 && !viewModel.isBlockerActive()) {
            if (input.key() == GLFW.GLFW_KEY_DELETE || input.key() == GLFW.GLFW_KEY_BACKSPACE) {
                return deleteSelection();
            }

            // arrow key movement
            if (input.key() == GLFW.GLFW_KEY_LEFT) {
                return moveSelection(-1, 0);
            } else if (input.key() == GLFW.GLFW_KEY_RIGHT) {
                return moveSelection(1, 0);
            } else if (input.key() == GLFW.GLFW_KEY_UP) {
                return moveSelection(0, -1);
            } else if (input.key() == GLFW.GLFW_KEY_DOWN) {
                return moveSelection(0, 1);
            }
        }

        return false;
    }

    private boolean moveSelection(int deltaCol, int deltaRow) {
        if (selectedCol < 0 || selectedRow < 0) return false;
        int cols = getColumnCount();
        int targetCol = selectedCol + deltaCol;
        int targetRow = selectedRow + deltaRow;

        if (targetCol < 0 || targetCol >= cols || targetRow < 0) {
            return false;
        }

        ProfileData profile = viewModel.getActiveProfile();
        if (profile == null) return false;

        Optional<ItemSlotPosition> currentItem = profile.findItemAt(selectedCol, selectedRow);
        if (currentItem.isEmpty()) return false;
        String currentId = currentItem.get().getItemId();

        Optional<ItemSlotPosition> targetItem = profile.findItemAt(targetCol, targetRow);

        if (targetItem.isPresent()) {
            // swap
            String targetId = targetItem.get().getItemId();
            profile.removeAt(selectedCol, selectedRow);
            profile.removeAt(targetCol, targetRow);
            profile.setItemAt(currentId, targetCol, targetRow);
            profile.setItemAt(targetId, selectedCol, selectedRow);
        } else {
            // move to empty slot
            profile.removeAt(selectedCol, selectedRow);
            profile.setItemAt(currentId, targetCol, targetRow);
        }

        selectedCol = targetCol;
        selectedRow = targetRow;

        ensureRowVisible(selectedRow);

        StorageManager.getInstance().getProfileRepository().saveProfile(profile);
        SoundHelper.playClick();
        return true;
    }

    private boolean deleteSelection() {
        if (selectedCol < 0 || selectedRow < 0) return false;
        ProfileData profile = viewModel.getActiveProfile();
        if (profile != null) {
            if (profile.removeAt(selectedCol, selectedRow)) {
                selectedCol = -1;
                selectedRow = -1;
                StorageManager.getInstance().getProfileRepository().saveProfile(profile);
                viewModel.recomputeUnorganizedItems();
                SoundHelper.playBreak();
                return true;
            }
        }
        return false;
    }

    private void ensureRowVisible(int row) {
        int slotSize = getSlotSize();
        int itemY = row * slotSize;
        double currentScroll = scrollbar.getScrollOffset();

        if (itemY < currentScroll) {
            scrollbar.setScrollOffset(itemY);
        } else if (itemY + slotSize > currentScroll + height) {
            scrollbar.setScrollOffset(itemY + slotSize - height);
        }
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

    public int getHoveredCol() {
        return hoveredCol;
    }

    public int getHoveredRow() {
        return hoveredRow;
    }

    public ItemStack getHoveredStack() {
        return hoveredStack;
    }

    public int getSelectedCol() {
        return selectedCol;
    }

    public int getSelectedRow() {
        return selectedRow;
    }
}
