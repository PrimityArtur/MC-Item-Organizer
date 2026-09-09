package com.itemorganizer.gui.widget;

import com.itemorganizer.core.model.PaletteData;
import com.itemorganizer.core.model.PaletteRow;
import com.itemorganizer.gui.dragdrop.DragAndDropManager;
import com.itemorganizer.gui.dragdrop.DragPayload;
import com.itemorganizer.gui.dragdrop.DragSource;
import com.itemorganizer.gui.util.RenderHelper;
import com.itemorganizer.gui.util.TextScaleHelper;
import com.itemorganizer.gui.viewmodel.OrganizerViewModel;
import com.itemorganizer.storage.StorageManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
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
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

// palette list widget with 9-slot rows, search, duplicate and delete options
public class PaletteListWidget implements Drawable, Element, Selectable {
    public static final int SCROLLBAR_WIDTH = 3;
    public static final int BASE_TOP_BAR_HEIGHT = 18;

    private final OrganizerViewModel viewModel;
    private int x;
    private int y;
    private int width;
    private int height;

    private final VerticalScrollbar scrollbar;
    private TextFieldWidget searchField;
    private TextFieldWidget renameField;

    // active modal states
    private String deletingPaletteId = null;
    private String renamingPaletteId = null;

    // hover state
    private int hoveredOriginalRowIndex = -1;
    private int hoveredSlotIndex = -1;
    private boolean hoveredLoadBtn = false;
    private boolean hoveredDeleteBtn = false;
    private boolean hoveredDupBtn = false;
    private boolean hoveredUpBtn = false;
    private boolean hoveredDownBtn = false;
    private boolean hoveredName = false;
    private boolean hoveredTopAddBtn = false;
    private String activeTooltip = null;
    private ItemStack hoveredStack = ItemStack.EMPTY;

    public record DisplayPalette(int originalIndex, PaletteRow row) {}

    public PaletteListWidget(OrganizerViewModel viewModel, int x, int y, int width, int height) {
        this.viewModel = viewModel;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        int listStartY = y + getTopBarHeight() + 2;
        int listHeight = height - getTopBarHeight() - 4;
        this.scrollbar = new VerticalScrollbar(x + width - SCROLLBAR_WIDTH - 2, listStartY, SCROLLBAR_WIDTH, listHeight);

        initInputs();
    }

    public int getTopBarHeight() {
        float paletteScale = viewModel.getConfig().getPaletteScale();
        float textScale = viewModel.getConfig().getTextScale();
        float factor = Math.max(paletteScale, textScale);
        return Math.max(14, Math.min(26, Math.round(BASE_TOP_BAR_HEIGHT * factor)));
    }

    public int getSearchHeight() {
        return Math.max(12, getTopBarHeight() - 4);
    }

    public int getHeaderHeight() {
        float paletteScale = viewModel.getConfig().getPaletteScale();
        float textScale = viewModel.getConfig().getTextScale();
        int btnH = getButtonSize();
        int textH = Math.round(9 * textScale);
        return Math.max(btnH + 2, Math.max(textH + 2, Math.round(10 * Math.max(paletteScale, textScale))));
    }

    public int getButtonSize() {
        float paletteScale = viewModel.getConfig().getPaletteScale();
        return Math.max(6, Math.min(16, Math.round(8 * paletteScale)));
    }

    public int getButtonGap() {
        float paletteScale = viewModel.getConfig().getPaletteScale();
        return Math.max(1, Math.round(2 * paletteScale));
    }

    private void initInputs() {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;

        float textScale = viewModel.getConfig().getTextScale();
        int searchH = getSearchHeight();
        int addBtnW = (width < 160) ? searchH : Math.round(40 * Math.min(1.3f, textScale));
        int searchW = Math.max(30, width - SCROLLBAR_WIDTH - addBtnW - 12);
        this.searchField = new TextFieldWidget(tr, x + 4, y + 2, searchW, searchH, Text.translatable("palettes.itemorganizer.search_placeholder"));
        this.searchField.setPlaceholder(Text.translatable("palettes.itemorganizer.search_placeholder"));
        this.searchField.setChangedListener(text -> scrollbar.setScrollOffset(0));
        this.searchField.setDrawsBackground(false);

        this.renameField = new TextFieldWidget(tr, x + 20, y + 20, 120, 16, Text.translatable("palettes.itemorganizer.rename.title"));
        this.renameField.setMaxLength(24);
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        int topBarH = getTopBarHeight();
        int listStartY = y + topBarH + 2;
        int listHeight = height - topBarH - 4;
        this.scrollbar.setBounds(x + width - SCROLLBAR_WIDTH - 2, listStartY, SCROLLBAR_WIDTH, listHeight);

        int searchH = getSearchHeight();
        int addBtnH = searchH;
        int addBtnW = (width < 160) ? searchH : Math.round(40 * Math.min(1.3f, viewModel.getConfig().getTextScale()));
        int addBtnX = x + width - SCROLLBAR_WIDTH - addBtnW - 4;
        int searchW = Math.max(30, addBtnX - (x + 4) - 4);
        int searchY = y + 2 + (topBarH - 4 - searchH) / 2;
        this.searchField.setX(x + 4);
        this.searchField.setY(searchY);
        this.searchField.setDimensions(searchW, searchH);
        this.searchField.setDrawsBackground(false);
    }

    public int getCardWidth() {
        int availW = Math.max(60, width - SCROLLBAR_WIDTH - 12);
        int neededForSlots = (9 * getSlotSize()) + 10;
        return Math.max(availW, neededForSlots);
    }

    public int getSlotSize() {
        float paletteScale = viewModel.getConfig().getPaletteScale();
        int availW = Math.max(60, width - SCROLLBAR_WIDTH - 12);
        int baseFit = Math.max(10, (availW - 10) / 9);
        int scaled = Math.round(baseFit * paletteScale);
        return Math.max(10, Math.min(36, scaled));
    }

    public int getCardHeight() {
        int slotSize = getSlotSize();
        int headerH = getHeaderHeight();
        return 2 + headerH + 2 + slotSize + 3;
    }

    public int getCardGap() {
        return 3;
    }

    private List<DisplayPalette> getFilteredPalettes(List<PaletteRow> allRows) {
        List<DisplayPalette> list = new ArrayList<>();
        if (allRows == null) return list;

        String query = searchField.getText().trim().toLowerCase();
        for (int i = 0; i < allRows.size(); i++) {
            PaletteRow row = allRows.get(i);
            if (query.isEmpty()) {
                list.add(new DisplayPalette(i, row));
                continue;
            }

            // filter by row index
            String idxStr = String.valueOf(i + 1);
            if (idxStr.contains(query) || ("#" + idxStr).contains(query)) {
                list.add(new DisplayPalette(i, row));
                continue;
            }

            // filter by custom name
            if (row.getName() != null && row.getName().toLowerCase().contains(query)) {
                list.add(new DisplayPalette(i, row));
                continue;
            }

            // filter by item names inside slots
            boolean matchedItem = false;
            for (int s = 0; s < 9; s++) {
                String itemId = row.getSlot(s);
                if (itemId != null) {
                    if (itemId.toLowerCase().contains(query)) {
                        matchedItem = true;
                        break;
                    }
                    ItemStack stack = getItemStackFromId(itemId);
                    if (!stack.isEmpty() && stack.getName().getString().toLowerCase().contains(query)) {
                        matchedItem = true;
                        break;
                    }
                }
            }
            if (matchedItem) {
                list.add(new DisplayPalette(i, row));
            }
        }
        return list;
    }

    private int calculateTotalContentHeight(int filteredCount) {
        return (filteredCount * (getCardHeight() + getCardGap())) + 8;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;
        PaletteData data = viewModel.getPaletteData();
        if (data == null) return;

        List<PaletteRow> allRows = data.getRows();
        List<DisplayPalette> displayList = getFilteredPalettes(allRows);

        float textScale = viewModel.getConfig().getTextScale();
        float itemScale = viewModel.getConfig().getPaletteItemScale();

        // top bar
        renderTopBar(context, tr, mouseX, mouseY, delta, textScale);

        // list area
        int topBarH = getTopBarHeight();
        int listStartY = y + topBarH + 2;
        int listHeight = height - topBarH - 4;
        int totalContentHeight = calculateTotalContentHeight(displayList.size());
        scrollbar.setBounds(x + width - SCROLLBAR_WIDTH - 2, listStartY, SCROLLBAR_WIDTH, listHeight);
        scrollbar.updateMaxScroll(totalContentHeight, listHeight);

        updateHover(mouseX, mouseY, displayList, allRows.size());

        context.enableScissor(x, listStartY, x + width, listStartY + listHeight);

        int scrollY = (int) scrollbar.getScrollOffset();
        int cardX = x + 4;
        int cardW = getCardWidth();
        int slotSize = getSlotSize();
        int cardH = getCardHeight();
        int cardGap = getCardGap();
        int totalSlotsW = 9 * slotSize;
        int slotsStartX = cardX + Math.max(4, (cardW - totalSlotsW) / 2);
        float effectiveScale = ((float) slotSize / 16.0f) * itemScale;

        int currentY = listStartY + 3 - scrollY;

        if (displayList.isEmpty()) {
            Text emptyMsg = allRows.isEmpty()
                    ? Text.translatable("palettes.itemorganizer.empty")
                    : Text.translatable("palettes.itemorganizer.no_results");
            TextScaleHelper.drawCenteredScaledText(
                    context, tr, emptyMsg, x + width / 2, listStartY + 20, 0xFF888888, textScale
            );
        }

        for (int i = 0; i < displayList.size(); i++) {
            DisplayPalette dp = displayList.get(i);
            PaletteRow row = dp.row();
            int origIdx = dp.originalIndex();

            if (currentY + cardH >= listStartY && currentY <= listStartY + listHeight) {
                boolean isCardHover = (deletingPaletteId == null && renamingPaletteId == null && origIdx == hoveredOriginalRowIndex);

                // card background
                context.fill(cardX, currentY, cardX + cardW, currentY + cardH, isCardHover ? 0x22FFFFFF : 0x00000000);
                RenderHelper.drawBorder(context, cardX, currentY, cardW, cardH, isCardHover ? 0x44FFFFFF : 0x00000000);

                // card header
                int headerY = currentY + 2;
                int headerH = getHeaderHeight();
                int btnW = getButtonSize();
                int btnH = getButtonSize();
                int btnGap = getButtonGap();
                int btnY = headerY + (headerH - btnH) / 2;

                int btnDeleteX = cardX + cardW - 3 - btnW;
                int btnLoadX = btnDeleteX - btnGap - btnW;
                int btnDupX = btnLoadX - btnGap - btnW;

                boolean showReorder = (cardW >= 140 && searchField.getText().trim().isEmpty());
                int btnDownX = showReorder ? btnDupX - btnGap - btnW : btnDupX;
                int btnUpX = showReorder ? btnDownX - btnGap - btnW : btnDownX;
                int leftmostBtnX = showReorder ? btnUpX : btnDupX;

                String title = "#" + (origIdx + 1);
                if (row.getName() != null && !row.getName().isEmpty()) {
                    title += " " + row.getName();
                }
                int maxTitleW = Math.max(20, leftmostBtnX - cardX - 6);
                String trimmedTitle = tr.trimToWidth(title, (int) (maxTitleW / Math.min(1.0f, textScale)));
                boolean isNameHover = (isCardHover && hoveredName);
                int titleColor = isNameHover ? 0xFF38BDF8 : 0xFFCBD5E1;

                int textY = headerY + Math.max(0, (headerH - Math.round(9 * textScale)) / 2);
                TextScaleHelper.drawScaledText(
                        context, tr, trimmedTitle, cardX + 4, textY, titleColor, false, textScale
                );

                float iconScale = Math.max(0.6f, Math.min(1.6f, ((float) btnW / 8.0f) * textScale));

                float btnCenterY = btnY + (btnH - 1) / 2.0f;

                // reorder buttons
                if (showReorder) {
                    if (origIdx > 0) {
                        boolean hoverUp = (isCardHover && hoveredUpBtn);
                        context.fill(btnUpX, btnY, btnUpX + btnW, btnY + btnH, hoverUp ? 0x801E293B : 0x401E293B);
                        RenderHelper.drawBorder(context, btnUpX, btnY, btnW, btnH, hoverUp ? 0xFF38BDF8 : 0x4038BDF8);
                        drawArrowUpIcon(context, btnUpX + (btnW - 1) / 2.0f, btnCenterY, iconScale, hoverUp ? 0xFFFFFFFF : 0xFF94A3B8);
                    }
                    if (origIdx < allRows.size() - 1) {
                        boolean hoverDown = (isCardHover && hoveredDownBtn);
                        context.fill(btnDownX, btnY, btnDownX + btnW, btnY + btnH, hoverDown ? 0x801E293B : 0x401E293B);
                        RenderHelper.drawBorder(context, btnDownX, btnY, btnW, btnH, hoverDown ? 0xFF38BDF8 : 0x4038BDF8);
                        drawArrowDownIcon(context, btnDownX + (btnW - 1) / 2.0f, btnCenterY, iconScale, hoverDown ? 0xFFFFFFFF : 0xFF94A3B8);
                    }
                }

                // duplicate button (blue, copy sheets icon)
                boolean hoverDup = (isCardHover && hoveredDupBtn);
                context.fill(btnDupX, btnY, btnDupX + btnW, btnY + btnH, hoverDup ? 0x801E3A5F : 0x401E3A5F);
                RenderHelper.drawBorder(context, btnDupX, btnY, btnW, btnH, hoverDup ? 0xFF38BDF8 : 0x8038BDF8);
                drawDuplicateIcon(context, btnDupX + (btnW - 1) / 2.0f, btnCenterY, iconScale, hoverDup ? 0xFFFFFFFF : 0xFFBAE6FD);

                // load button (green, arrow down into hotbar tray icon)
                boolean hoverLoad = (isCardHover && hoveredLoadBtn);
                context.fill(btnLoadX, btnY, btnLoadX + btnW, btnY + btnH, hoverLoad ? 0x80065F46 : 0x40065F46);
                RenderHelper.drawBorder(context, btnLoadX, btnY, btnW, btnH, hoverLoad ? 0xFF34D399 : 0x8034D399);
                drawLoadHotbarIcon(context, btnLoadX + (btnW - 1) / 2.0f, btnCenterY, iconScale, hoverLoad ? 0xFFFFFFFF : 0xFFA7F3D0);

                // delete button (red, diagonal cross icon)
                boolean hoverDel = (isCardHover && hoveredDeleteBtn);
                context.fill(btnDeleteX, btnY, btnDeleteX + btnW, btnY + btnH, hoverDel ? 0x807F1D1D : 0x407F1D1D);
                RenderHelper.drawBorder(context, btnDeleteX, btnY, btnW, btnH, hoverDel ? 0xFFEF4444 : 0x80EF4444);
                drawDeleteIcon(context, btnDeleteX + (btnW - 1) / 2.0f, btnCenterY, iconScale, hoverDel ? 0xFFFFFFFF : 0xFFFCA5A5);

                // 9 slot continuous strip
                int slotsY = headerY + headerH + 2;
                for (int s = 0; s < 9; s++) {
                    int slotX = slotsStartX + (s * slotSize);
                    boolean isSlotHover = (isCardHover && hoveredSlotIndex == s);

                    context.fill(slotX, slotsY, slotX + slotSize, slotsY + slotSize, isSlotHover ? 0x3338BDF8 : 0x1A000000);
                    RenderHelper.drawBorder(context, slotX, slotsY, slotSize, slotSize, isSlotHover ? 0xFF38BDF8 : 0x22FFFFFF);

                    String itemId = row.getSlot(s);
                    if (itemId != null) {
                        ItemStack stack = getItemStackFromId(itemId);
                        if (!stack.isEmpty()) {
                            float cx = slotX + (slotSize - 1) / 2.0f;
                            float cy = slotsY + (slotSize - 1) / 2.0f;

                            context.getMatrices().pushMatrix();
                            context.getMatrices().translate(cx, cy);
                            context.getMatrices().scale(effectiveScale, effectiveScale);

                            context.drawItem(stack, -8, -8);
                            context.drawStackOverlay(tr, stack, -8, -8);

                            context.getMatrices().popMatrix();
                        }
                    }
                }
            }
            currentY += cardH + cardGap;
        }

        context.disableScissor();

        scrollbar.render(context, mouseX, mouseY);

        // delete confirmation modal
        if (deletingPaletteId != null) {
            renderDeleteModal(context, tr, mouseX, mouseY);
        }

        // rename modal
        if (renamingPaletteId != null) {
            renderRenameModal(context, tr, mouseX, mouseY);
        }

        renderTooltips(context, tr, mouseX, mouseY);
    }

    private void renderTopBar(DrawContext context, TextRenderer tr, int mouseX, int mouseY, float delta, float textScale) {
        float paletteScale = viewModel.getConfig().getPaletteScale();
        int topBarH = getTopBarHeight();
        int searchH = getSearchHeight();
        int searchY = y + 2 + (topBarH - 4 - searchH) / 2;

        int addBtnH = searchH;
        int addBtnW = (width < 160) ? searchH : Math.round(40 * Math.min(1.3f, textScale));
        int addBtnX = x + width - SCROLLBAR_WIDTH - addBtnW - 4;
        int addBtnY = searchY;

        int searchX = x + 4;
        int searchW = Math.max(30, addBtnX - searchX - 4);

        searchField.setX(searchX);
        searchField.setY(searchY);
        searchField.setDimensions(searchW, searchH);
        searchField.setDrawsBackground(false);

        // search box background and border
        context.fill(searchX, searchY, searchX + searchW, searchY + searchH, 0xE60A0E17);
        int borderColor = searchField.isFocused() ? 0xFF38BDF8 : 0x40FFFFFF;
        RenderHelper.drawBorder(context, searchX, searchY, searchW, searchH, borderColor);

        // render search field with text scale
        context.getMatrices().pushMatrix();
        float textOriginX = searchX + 4;
        float textOriginY = searchY + (searchH / 2.0f);
        context.getMatrices().translate(textOriginX, textOriginY);
        context.getMatrices().scale(textScale, textScale);
        context.getMatrices().translate(-textOriginX, -textOriginY);

        searchField.renderWidget(context, mouseX, mouseY, delta);
        context.getMatrices().popMatrix();

        // clear button
        if (!searchField.getText().isEmpty()) {
            int clearBtnSize = Math.max(8, Math.round(10 * paletteScale));
            int clearBtnX = searchX + searchW - clearBtnSize - 3;
            int clearBtnY = searchY + (searchH - clearBtnSize) / 2;
            boolean hoverClear = mouseX >= clearBtnX && mouseX <= clearBtnX + clearBtnSize && mouseY >= clearBtnY && mouseY <= clearBtnY + clearBtnSize;
            TextScaleHelper.drawCenteredScaledText(context, tr, "×", clearBtnX + clearBtnSize / 2, clearBtnY, hoverClear ? 0xFFFF6666 : 0xFFAAAAAA, textScale);
        }

        // add palette button
        boolean hoverAdd = (deletingPaletteId == null && renamingPaletteId == null && hoveredTopAddBtn);
        int addBg = hoverAdd ? 0x66065F46 : 0x33065F46;
        int addBorder = hoverAdd ? 0xFF34D399 : 0x8034D399;
        context.fill(addBtnX, addBtnY, addBtnX + addBtnW, addBtnY + addBtnH, addBg);
        RenderHelper.drawBorder(context, addBtnX, addBtnY, addBtnW, addBtnH, addBorder);

        float iconScale = Math.max(0.7f, Math.min(1.5f, ((float) addBtnH / 14.0f) * textScale));
        if (width < 160) {
            drawPlusIcon(context, addBtnX + (addBtnW - 1) / 2.0f, addBtnY + (addBtnH - 1) / 2.0f, iconScale, hoverAdd ? 0xFFFFFFFF : 0xFFE2E8F0);
        } else {
            TextScaleHelper.drawVerticallyCenteredScaledText(
                    context, tr, Text.translatable("palettes.itemorganizer.new"), addBtnX + addBtnW / 2, addBtnY + addBtnH / 2, hoverAdd ? 0xFFFFFFFF : 0xFFE2E8F0, textScale
            );
        }
    }

    private void drawDuplicateIcon(DrawContext context, float cx, float cy, float scale, int color) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(cx, cy);
        context.getMatrices().scale(scale, scale);

        // back sheet (top-right, 5x5 overall bounds [-2..2])
        context.fill(0, -2, 3, -1, color);
        context.fill(2, -1, 3, 1, color);

        // front sheet (bottom-left)
        context.fill(-2, -1, 2, 0, color);
        context.fill(-2, 2, 2, 3, color);
        context.fill(-2, 0, -1, 2, color);
        context.fill(1, 0, 2, 2, color);

        context.getMatrices().popMatrix();
    }

    private void drawLoadHotbarIcon(DrawContext context, float cx, float cy, float scale, int color) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(cx, cy);
        context.getMatrices().scale(scale, scale);

        // arrow down shaft (centered at x=0)
        context.fill(0, -2, 1, 0, color);
        // arrow down wings
        context.fill(-1, -1, 2, 0, color);
        context.fill(0, 0, 1, 1, color);
        // hotbar tray (5x5 overall bounds [-2..2])
        context.fill(-2, 2, 3, 3, color);
        context.fill(-2, 1, -1, 2, color);
        context.fill(2, 1, 3, 2, color);

        context.getMatrices().popMatrix();
    }

    private void drawDeleteIcon(DrawContext context, float cx, float cy, float scale, int color) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(cx, cy);
        context.getMatrices().scale(scale, scale);

        // crisp diagonal cross (5x5 overall bounds [-2..2])
        context.fill(-2, -2, -1, -1, color);
        context.fill(2, -2, 3, -1, color);
        context.fill(-1, -1, 0, 0, color);
        context.fill(1, -1, 2, 0, color);
        context.fill(0, 0, 1, 1, color);
        context.fill(-1, 1, 0, 2, color);
        context.fill(1, 1, 2, 2, color);
        context.fill(-2, 2, -1, 3, color);
        context.fill(2, 2, 3, 3, color);

        context.getMatrices().popMatrix();
    }

    private void drawArrowUpIcon(DrawContext context, float cx, float cy, float scale, int color) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(cx, cy);
        context.getMatrices().scale(scale, scale);

        // arrow up triangle centered vertically in [-1..1]
        context.fill(0, -1, 1, 0, color);
        context.fill(-1, 0, 2, 1, color);
        context.fill(-2, 1, 3, 2, color);

        context.getMatrices().popMatrix();
    }

    private void drawArrowDownIcon(DrawContext context, float cx, float cy, float scale, int color) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(cx, cy);
        context.getMatrices().scale(scale, scale);

        // arrow down triangle centered vertically in [-1..1]
        context.fill(-2, -1, 3, 0, color);
        context.fill(-1, 0, 2, 1, color);
        context.fill(0, 1, 1, 2, color);

        context.getMatrices().popMatrix();
    }

    private void drawPlusIcon(DrawContext context, float cx, float cy, float scale, int color) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(cx, cy);
        context.getMatrices().scale(scale, scale);

        // plus cross centered in [-2..2]
        context.fill(-2, 0, 3, 1, color);
        context.fill(0, -2, 1, 3, color);

        context.getMatrices().popMatrix();
    }

    private void renderDeleteModal(DrawContext context, TextRenderer tr, int mouseX, int mouseY) {
        float textScale = viewModel.getConfig().getTextScale();
        int modalW = Math.min(220, width - 20);
        int modalH = 85;
        int modalX = x + (width - modalW) / 2;
        int modalY = y + (height - modalH) / 2;

        context.fill(x, y, x + width, y + height, 0xDD0B0F19);
        context.fill(modalX, modalY, modalX + modalW, modalY + modalH, 0xFF141820);
        RenderHelper.drawBorder(context, modalX, modalY, modalW, modalH, 0xFFEF4444);

        TextScaleHelper.drawCenteredScaledText(context, tr, Text.translatable("palettes.itemorganizer.delete.title"), modalX + modalW / 2, modalY + 8, 0xFFEF4444, textScale);
        TextScaleHelper.drawCenteredScaledText(context, tr, Text.translatable("palettes.itemorganizer.delete.warning"), modalX + modalW / 2, modalY + 24, 0xFF94A3B8, textScale);

        int btnW = (modalW - 28) / 2;
        int btnH = 18;
        int btnY = modalY + modalH - 24;

        int delBtnX = modalX + 10;
        boolean hoverDel = mouseX >= delBtnX && mouseX <= delBtnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        context.fill(delBtnX, btnY, delBtnX + btnW, btnY + btnH, hoverDel ? 0x807F1D1D : 0x407F1D1D);
        RenderHelper.drawBorder(context, delBtnX, btnY, btnW, btnH, hoverDel ? 0xFFEF4444 : 0x80EF4444);
        TextScaleHelper.drawCenteredScaledText(context, tr, Text.translatable("profiles.itemorganizer.delete.btn"), delBtnX + btnW / 2, btnY + 5, 0xFFFFFFFF, textScale);

        int cancelBtnX = delBtnX + btnW + 8;
        boolean hoverCancel = mouseX >= cancelBtnX && mouseX <= cancelBtnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        context.fill(cancelBtnX, btnY, cancelBtnX + btnW, btnY + btnH, hoverCancel ? 0x33FFFFFF : 0x1AFFFFFF);
        RenderHelper.drawBorder(context, cancelBtnX, btnY, btnW, btnH, hoverCancel ? 0x66FFFFFF : 0x33FFFFFF);
        TextScaleHelper.drawCenteredScaledText(context, tr, Text.translatable("button.itemorganizer.cancel"), cancelBtnX + btnW / 2, btnY + 5, 0xFFE2E8F0, textScale);
    }

    private void renderRenameModal(DrawContext context, TextRenderer tr, int mouseX, int mouseY) {
        float textScale = viewModel.getConfig().getTextScale();
        int modalW = Math.min(220, width - 20);
        int modalH = 80;
        int modalX = x + (width - modalW) / 2;
        int modalY = y + (height - modalH) / 2;

        context.fill(x, y, x + width, y + height, 0xDD0B0F19);
        context.fill(modalX, modalY, modalX + modalW, modalY + modalH, 0xFF141820);
        RenderHelper.drawBorder(context, modalX, modalY, modalW, modalH, 0xFF38BDF8);

        TextScaleHelper.drawCenteredScaledText(context, tr, Text.translatable("palettes.itemorganizer.rename.title"), modalX + modalW / 2, modalY + 8, 0xFF38BDF8, textScale);

        renameField.setX(modalX + 12);
        renameField.setY(modalY + 24);
        renameField.setWidth(modalW - 24);
        context.fill(renameField.getX() - 1, renameField.getY() - 1, renameField.getX() + renameField.getWidth() + 1, renameField.getY() + renameField.getHeight() + 1, 0xFF0B0F19);
        RenderHelper.drawBorder(context, renameField.getX() - 1, renameField.getY() - 1, renameField.getWidth() + 2, renameField.getHeight() + 2, 0x33FFFFFF);
        renameField.renderWidget(context, mouseX, mouseY, 0);

        int btnW = (modalW - 28) / 2;
        int btnH = 16;
        int btnY = modalY + modalH - 22;

        int saveBtnX = modalX + 10;
        boolean hoverSave = mouseX >= saveBtnX && mouseX <= saveBtnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        context.fill(saveBtnX, btnY, saveBtnX + btnW, btnY + btnH, hoverSave ? 0x801E3A5F : 0x401E3A5F);
        RenderHelper.drawBorder(context, saveBtnX, btnY, btnW, btnH, hoverSave ? 0xFF38BDF8 : 0x8038BDF8);
        TextScaleHelper.drawCenteredScaledText(context, tr, Text.translatable("profiles.itemorganizer.save"), saveBtnX + btnW / 2, btnY + 4, 0xFFFFFFFF, textScale);

        int cancelBtnX = saveBtnX + btnW + 8;
        boolean hoverCancel = mouseX >= cancelBtnX && mouseX <= cancelBtnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        context.fill(cancelBtnX, btnY, cancelBtnX + btnW, btnY + btnH, hoverCancel ? 0x33FFFFFF : 0x1AFFFFFF);
        RenderHelper.drawBorder(context, cancelBtnX, btnY, btnW, btnH, hoverCancel ? 0x66FFFFFF : 0x33FFFFFF);
        TextScaleHelper.drawCenteredScaledText(context, tr, Text.translatable("button.itemorganizer.cancel"), cancelBtnX + btnW / 2, btnY + 4, 0xFFE2E8F0, textScale);
    }

    private void renderTooltips(DrawContext context, TextRenderer tr, int mouseX, int mouseY) {
        if (deletingPaletteId != null || renamingPaletteId != null || DragAndDropManager.getInstance().isDragging() || !isMouseOver(mouseX, mouseY)) {
            return;
        }

        if (!hoveredStack.isEmpty()) {
            context.drawItemTooltip(tr, hoveredStack, mouseX, mouseY);
        } else if (activeTooltip != null && !activeTooltip.isEmpty()) {
            context.drawTooltip(tr, Text.literal(activeTooltip), mouseX, mouseY);
        }
    }

    private void updateHover(double mouseX, double mouseY, List<DisplayPalette> displayList, int totalRowCount) {
        hoveredOriginalRowIndex = -1;
        hoveredSlotIndex = -1;
        hoveredLoadBtn = false;
        hoveredDeleteBtn = false;
        hoveredDupBtn = false;
        hoveredUpBtn = false;
        hoveredDownBtn = false;
        hoveredName = false;
        hoveredTopAddBtn = false;
        activeTooltip = null;
        hoveredStack = ItemStack.EMPTY;

        if (deletingPaletteId != null || renamingPaletteId != null || !isMouseOver(mouseX, mouseY)) return;

        int topBarH = getTopBarHeight();
        int searchH = getSearchHeight();
        int addBtnH = searchH;
        float textScale = viewModel.getConfig().getTextScale();
        int addBtnW = (width < 160) ? searchH : Math.round(40 * Math.min(1.3f, textScale));
        int addBtnX = x + width - SCROLLBAR_WIDTH - addBtnW - 4;
        int addBtnY = y + 2 + (topBarH - 4 - addBtnH) / 2;
        if (mouseX >= addBtnX && mouseX <= addBtnX + addBtnW && mouseY >= addBtnY && mouseY <= addBtnY + addBtnH) {
            hoveredTopAddBtn = true;
            activeTooltip = Text.translatable("palettes.itemorganizer.tooltip.new").getString();
            return;
        }

        int listStartY = y + topBarH + 2;
        int listHeight = height - topBarH - 4;
        if (mouseY < listStartY || mouseY > listStartY + listHeight) return;

        int scrollY = (int) scrollbar.getScrollOffset();
        int cardX = x + 4;
        int cardW = getCardWidth();
        int slotSize = getSlotSize();
        int cardH = getCardHeight();
        int cardGap = getCardGap();
        int totalSlotsW = 9 * slotSize;
        int slotsStartX = cardX + Math.max(4, (cardW - totalSlotsW) / 2);

        int currentY = listStartY + 3 - scrollY;

        for (int i = 0; i < displayList.size(); i++) {
            DisplayPalette dp = displayList.get(i);
            PaletteRow row = dp.row();
            int origIdx = dp.originalIndex();

            if (mouseY >= currentY && mouseY < currentY + cardH && mouseX >= cardX && mouseX <= cardX + cardW) {
                hoveredOriginalRowIndex = origIdx;

                int headerY = currentY + 2;
                int headerH = getHeaderHeight();
                int btnW = getButtonSize();
                int btnH = getButtonSize();
                int btnGap = getButtonGap();
                int btnY = headerY + (headerH - btnH) / 2;

                int btnDeleteX = cardX + cardW - 3 - btnW;
                int btnLoadX = btnDeleteX - btnGap - btnW;
                int btnDupX = btnLoadX - btnGap - btnW;

                boolean showReorder = (cardW >= 140 && searchField.getText().trim().isEmpty());
                int btnDownX = showReorder ? btnDupX - btnGap - btnW : btnDupX;
                int btnUpX = showReorder ? btnDownX - btnGap - btnW : btnDownX;
                int leftmostBtnX = showReorder ? btnUpX : btnDupX;

                // check action buttons in header
                if (mouseY >= btnY && mouseY <= btnY + btnH) {
                    if (mouseX >= btnDeleteX && mouseX <= btnDeleteX + btnW) {
                        hoveredDeleteBtn = true;
                        activeTooltip = Text.translatable("palettes.itemorganizer.tooltip.delete").getString();
                        return;
                    }
                    if (mouseX >= btnLoadX && mouseX <= btnLoadX + btnW) {
                        hoveredLoadBtn = true;
                        activeTooltip = Text.translatable("palettes.itemorganizer.tooltip.load_hotbar").getString();
                        return;
                    }
                    if (mouseX >= btnDupX && mouseX <= btnDupX + btnW) {
                        hoveredDupBtn = true;
                        activeTooltip = Text.translatable("palettes.itemorganizer.tooltip.duplicate").getString();
                        return;
                    }
                    if (showReorder) {
                        if (origIdx > 0 && mouseX >= btnUpX && mouseX <= btnUpX + btnW) {
                            hoveredUpBtn = true;
                            activeTooltip = Text.translatable("palettes.itemorganizer.tooltip.move_up").getString();
                            return;
                        }
                        if (origIdx < totalRowCount - 1 && mouseX >= btnDownX && mouseX <= btnDownX + btnW) {
                            hoveredDownBtn = true;
                            activeTooltip = Text.translatable("palettes.itemorganizer.tooltip.move_down").getString();
                            return;
                        }
                    }
                }

                if (mouseY >= headerY && mouseY <= headerY + headerH && mouseX >= cardX + 4 && mouseX < leftmostBtnX - 4) {
                    hoveredName = true;
                    activeTooltip = Text.translatable("palettes.itemorganizer.tooltip.rename").getString();
                    return;
                }

                // check 9 slots
                int slotsY = headerY + headerH + 2;
                if (mouseY >= slotsY && mouseY < slotsY + slotSize) {
                    for (int s = 0; s < 9; s++) {
                        int slotX = slotsStartX + (s * slotSize);
                        if (mouseX >= slotX && mouseX < slotX + slotSize) {
                            hoveredSlotIndex = s;
                            String itemId = row.getSlot(s);
                            if (itemId != null) {
                                hoveredStack = getItemStackFromId(itemId);
                            }
                            return;
                        }
                    }
                }
                return;
            }
            currentY += cardH + cardGap;
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
        // delete modal
        if (deletingPaletteId != null) {
            int modalW = Math.min(220, width - 20);
            int modalH = 85;
            int modalX = x + (width - modalW) / 2;
            int modalY = y + (height - modalH) / 2;
            int btnW = (modalW - 28) / 2;
            int btnH = 18;
            int btnY = modalY + modalH - 24;

            int delBtnX = modalX + 10;
            if (click.button() == 0 && click.x() >= delBtnX && click.x() <= delBtnX + btnW && click.y() >= btnY && click.y() <= btnY + btnH) {
                PaletteData data = viewModel.getPaletteData();
                if (data != null) {
                    data.removeRowById(deletingPaletteId);
                    StorageManager.getInstance().getPaletteRepository().save(data);
                }
                deletingPaletteId = null;
                playClickSound();
                return true;
            }

            int cancelBtnX = delBtnX + btnW + 8;
            if (click.button() == 0 && click.x() >= cancelBtnX && click.x() <= cancelBtnX + btnW && click.y() >= btnY && click.y() <= btnY + btnH) {
                deletingPaletteId = null;
                playClickSound();
                return true;
            }
            return true;
        }

        // rename modal
        if (renamingPaletteId != null) {
            int modalW = Math.min(220, width - 20);
            int modalH = 80;
            int modalX = x + (width - modalW) / 2;
            int modalY = y + (height - modalH) / 2;

            boolean clickOnRenameField = (click.button() == 0 && click.x() >= renameField.getX() && click.x() <= renameField.getX() + renameField.getWidth()
                    && click.y() >= renameField.getY() && click.y() <= renameField.getY() + renameField.getHeight());
            renameField.setFocused(clickOnRenameField);
            if (clickOnRenameField) {
                renameField.mouseClicked(click, bl);
                return true;
            }

            int btnW = (modalW - 28) / 2;
            int btnH = 16;
            int btnY = modalY + modalH - 22;

            int saveBtnX = modalX + 10;
            if (click.button() == 0 && click.x() >= saveBtnX && click.x() <= saveBtnX + btnW && click.y() >= btnY && click.y() <= btnY + btnH) {
                applyRename();
                return true;
            }

            int cancelBtnX = saveBtnX + btnW + 8;
            if (click.button() == 0 && click.x() >= cancelBtnX && click.x() <= cancelBtnX + btnW && click.y() >= btnY && click.y() <= btnY + btnH) {
                renamingPaletteId = null;
                playClickSound();
                return true;
            }
            return true;
        }

        // search bar and add button
        if (!searchField.getText().isEmpty()) {
            float paletteScale = viewModel.getConfig().getPaletteScale();
            int clearBtnSize = Math.max(8, Math.round(10 * paletteScale));
            int clearBtnX = searchField.getX() + searchField.getWidth() - clearBtnSize - 3;
            int clearBtnY = searchField.getY() + (searchField.getHeight() - clearBtnSize) / 2;
            if (click.button() == 0 && click.x() >= clearBtnX && click.x() <= clearBtnX + clearBtnSize && click.y() >= clearBtnY && click.y() <= clearBtnY + clearBtnSize) {
                searchField.setText("");
                searchField.setFocused(true);
                playClickSound();
                return true;
            }
        }

        boolean clickOnSearch = (click.button() == 0 && click.x() >= searchField.getX() && click.x() <= searchField.getX() + searchField.getWidth()
                && click.y() >= searchField.getY() && click.y() <= searchField.getY() + searchField.getHeight());
        searchField.setFocused(clickOnSearch);
        if (clickOnSearch) {
            float textScale = viewModel.getConfig().getTextScale();
            double textOriginX = searchField.getX() + 4;
            double localX = click.x() - textOriginX;
            double transformedX = textOriginX + (localX / textScale);
            Click transformedClick = new Click(transformedX, click.y(), click.buttonInfo());
            searchField.mouseClicked(transformedClick, bl);
            return true;
        }

        if (click.button() == 0 && hoveredTopAddBtn) {
            PaletteData data = viewModel.getPaletteData();
            if (data != null) {
                PaletteRow newRow = new PaletteRow();
                data.addRow(newRow);
                StorageManager.getInstance().getPaletteRepository().save(data);
                searchField.setText("");
                playClickSound();
            }
            return true;
        }

        // scrollbar
        if (scrollbar.mouseClicked(click)) {
            return true;
        }

        // clicks inside cards
        PaletteData data = viewModel.getPaletteData();
        if (data == null) return false;
        List<PaletteRow> allRows = data.getRows();

        if (hoveredOriginalRowIndex >= 0 && hoveredOriginalRowIndex < allRows.size()) {
            PaletteRow row = allRows.get(hoveredOriginalRowIndex);

            if (click.button() == 0 && hoveredLoadBtn) {
                loadRowToHotbar(row);
                return true;
            }

            if (click.button() == 0 && hoveredDeleteBtn) {
                deletingPaletteId = row.getId();
                playClickSound();
                return true;
            }

            if (click.button() == 0 && hoveredDupBtn) {
                data.duplicateRow(hoveredOriginalRowIndex);
                StorageManager.getInstance().getPaletteRepository().save(data);
                playClickSound();
                return true;
            }

            if (click.button() == 0 && hoveredUpBtn) {
                if (data.moveUp(hoveredOriginalRowIndex)) {
                    StorageManager.getInstance().getPaletteRepository().save(data);
                    playClickSound();
                }
                return true;
            }

            if (click.button() == 0 && hoveredDownBtn) {
                if (data.moveDown(hoveredOriginalRowIndex)) {
                    StorageManager.getInstance().getPaletteRepository().save(data);
                    playClickSound();
                }
                return true;
            }

            if (click.button() == 0 && hoveredName) {
                renamingPaletteId = row.getId();
                renameField.setText(row.getName() != null ? row.getName() : "");
                renameField.setFocused(true);
                playClickSound();
                return true;
            }

            // slot interaction
            if (hoveredSlotIndex >= 0 && hoveredSlotIndex < 9) {
                // right click: clear slot
                if (click.button() == 1) {
                    row.clearSlot(hoveredSlotIndex);
                    StorageManager.getInstance().getPaletteRepository().save(data);
                    SoundHelper.playBreak();
                    return true;
                }

                // left click: drag
                if (click.button() == 0) {
                    String itemId = row.getSlot(hoveredSlotIndex);
                    if (itemId != null) {
                        ItemStack stack = getItemStackFromId(itemId);
                        if (!stack.isEmpty()) {
                            DragPayload payload = DragPayload.ofIndexed(itemId, stack, DragSource.PALETAS, hoveredSlotIndex, true);
                            DragAndDropManager.getInstance().startDrag(payload);
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private void applyRename() {
        if (renamingPaletteId == null) return;
        PaletteData data = viewModel.getPaletteData();
        if (data != null) {
            for (PaletteRow row : data.getRows()) {
                if (renamingPaletteId.equals(row.getId())) {
                    String newName = renameField.getText().trim();
                    row.setName(newName.isEmpty() ? null : newName);
                    StorageManager.getInstance().getPaletteRepository().save(data);
                    break;
                }
            }
        }
        renamingPaletteId = null;
        playClickSound();
    }

    // copy the 9 slots to player hotbar
    private void loadRowToHotbar(PaletteRow row) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !client.player.isCreative()) return;

        for (int i = 0; i < 9; i++) {
            String itemId = row.getSlot(i);
            ItemStack stack = ItemStack.EMPTY;
            if (itemId != null) {
                stack = getItemStackFromId(itemId);
                if (!stack.isEmpty()) {
                    stack = new ItemStack(stack.getItem(), 1);
                }
            }

            client.player.getInventory().setStack(i, stack);
            if (client.getNetworkHandler() != null) {
                client.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(36 + i, stack));
            }
        }

        SoundHelper.playClick();
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (scrollbar.mouseReleased(click)) {
            return true;
        }

        DragAndDropManager dragManager = DragAndDropManager.getInstance();
        if (dragManager.isDragging()) {
            PaletteData data = viewModel.getPaletteData();
            if (data != null) {
                updateHover(click.x(), click.y(), getFilteredPalettes(data.getRows()), data.getRows().size());
                if (hoveredOriginalRowIndex >= 0 && hoveredSlotIndex >= 0 && hoveredOriginalRowIndex < data.getRows().size()) {
                    PaletteRow row = data.getRows().get(hoveredOriginalRowIndex);
                    DragPayload payload = dragManager.getActivePayload();
                    row.setSlot(hoveredSlotIndex, payload.getItemId());

                    StorageManager.getInstance().getPaletteRepository().save(data);
                    SoundHelper.playClick();
                    dragManager.consumePayload();
                    return true;
                }
            }
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
        if (renamingPaletteId != null) {
            if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER) {
                applyRename();
                return true;
            }
            if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
                renamingPaletteId = null;
                playClickSound();
                return true;
            }
            return renameField.keyPressed(input);
        }

        if (deletingPaletteId != null) {
            if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
                deletingPaletteId = null;
                playClickSound();
                return true;
            }
            return true;
        }

        if (searchField.isFocused()) {
            if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
                searchField.setFocused(false);
                return true;
            }
            return searchField.keyPressed(input);
        }

        // 1-9 hotbar keys in creative
        MinecraftClient client = MinecraftClient.getInstance();
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
        return false;
    }

    public boolean charTyped(CharInput input) {
        if (renamingPaletteId != null) {
            return renameField.charTyped(input);
        }
        if (searchField.isFocused()) {
            return searchField.charTyped(input);
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

    private void playClickSound() {
        SoundHelper.playClick();
    }
}
