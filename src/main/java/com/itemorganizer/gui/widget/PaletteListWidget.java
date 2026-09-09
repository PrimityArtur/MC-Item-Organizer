package com.itemorganizer.gui.widget;

import com.itemorganizer.core.model.PaletteData;
import com.itemorganizer.core.model.PaletteRow;
import com.itemorganizer.gui.component.ModalDialogComponent;
import com.itemorganizer.gui.component.ScrollbarComponent;
import com.itemorganizer.gui.dragdrop.DragAndDropManager;
import com.itemorganizer.gui.dragdrop.DragPayload;
import com.itemorganizer.gui.dragdrop.DragSource;
import com.itemorganizer.gui.theme.UITheme;
import com.itemorganizer.gui.util.HotbarActionHelper;
import com.itemorganizer.gui.util.RenderHelper;
import com.itemorganizer.gui.util.SoundHelper;
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
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

// palette list widget managing custom 9-slot item groups
public class PaletteListWidget implements Drawable, Element, Selectable {
    public static final int SCROLLBAR_WIDTH = 3;
    public static final int BASE_SLOT_SIZE = 18;
    public static final int BASE_ROW_HEIGHT = 38;
    public static final int BASE_TOP_BAR_HEIGHT = 16;
    public static final int BASE_ROW_BTN_SIZE = 12;

    private final OrganizerViewModel viewModel;
    private int x;
    private int y;
    private int width;
    private int height;

    private final ScrollbarComponent scrollbar;
    private TextFieldWidget searchField;
    private TextFieldWidget renameField;
    private PaletteSearchFilterWidget searchFilterWidget;

    // active modal states
    private ModalDialogComponent activeModal = null;
    private String deletingPaletteId = null;
    private String renamingPaletteId = null;
    private String pastingHotbarPaletteId = null;

    // hover state
    private int hoveredOriginalRowIndex = -1;
    private int hoveredSlotIndex = -1;
    private boolean hoveredLoadBtn = false;
    private boolean hoveredPlaceWorldBtn = false;
    private boolean hoveredPasteHotbarBtn = false;
    private boolean hoveredDeleteBtn = false;
    private boolean hoveredDupBtn = false;
    private boolean hoveredUpBtn = false;
    private boolean hoveredDownBtn = false;
    private boolean hoveredName = false;
    private boolean hoveredTopAddBtn = false;
    private boolean hoveredSortBtn = false;
    private boolean colorSortActive = false;
    private String activeTooltip = null;
    private ItemStack hoveredStack = ItemStack.EMPTY;

    // continuous shift drag tracking
    private int lastShiftPaletteRow = -1;
    private int lastShiftPaletteSlot = -1;

    private final boolean infiniteMode;

    public record DisplayPalette(int originalIndex, PaletteRow row) {}

    public PaletteListWidget(OrganizerViewModel viewModel, int x, int y, int width, int height) {
        this(viewModel, x, y, width, height, false);
    }

    public PaletteListWidget(OrganizerViewModel viewModel, int x, int y, int width, int height, boolean infiniteMode) {
        this.viewModel = viewModel;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.infiniteMode = infiniteMode;

        int listStartY = y + getTopBarHeight() + 2;
        int listHeight = height - getTopBarHeight() - 4;
        this.scrollbar = new ScrollbarComponent(x + width - SCROLLBAR_WIDTH - 2, listStartY, SCROLLBAR_WIDTH, listHeight);

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
        float btnScale = viewModel.getConfig().getPaletteButtonScale();
        float textScale = viewModel.getConfig().getTextScale();
        int btnH = getButtonSize();
        int textH = Math.round(9 * textScale);
        return Math.max(btnH + 2, Math.max(textH + 2, Math.round(10 * Math.max(btnScale, textScale))));
    }

    public int getButtonSize() {
        float btnScale = viewModel.getConfig().getPaletteButtonScale();
        return Math.max(6, Math.min(22, Math.round(8 * btnScale)));
    }

    public int getButtonGap() {
        float btnScale = viewModel.getConfig().getPaletteButtonScale();
        return Math.max(1, Math.round(2 * btnScale));
    }

    private void initInputs() {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;

        float textScale = viewModel.getConfig().getTextScale();
        int searchH = getSearchHeight();
        int addBtnW = (width < 160) ? searchH : Math.round(40 * Math.min(1.3f, textScale));
        int sortBtnW = searchH;
        int searchW = Math.max(30, width - SCROLLBAR_WIDTH - addBtnW - sortBtnW - 15);
        this.searchField = new TextFieldWidget(tr, x + 4, y + 2, searchW, searchH, Text.translatable("palettes.itemorganizer.search_placeholder"));
        this.searchField.setPlaceholder(Text.translatable("palettes.itemorganizer.search_placeholder"));
        String initialQuery = infiniteMode ? viewModel.getInfinitePaletteSearchQuery() : viewModel.getPaletteSearchQuery();
        if (initialQuery != null && !initialQuery.isEmpty()) {
            this.searchField.setText(initialQuery);
        }
        this.searchField.setChangedListener(text -> {
            String current = infiniteMode ? viewModel.getInfinitePaletteSearchQuery() : viewModel.getPaletteSearchQuery();
            if (!Objects.equals(text, current)) {
                if (infiniteMode) {
                    viewModel.setInfinitePaletteSearchQuery(text);
                } else {
                    viewModel.setPaletteSearchQuery(text);
                }
                scrollbar.setScrollOffset(0);
            }
        });
        this.searchField.setDrawsBackground(false);

        this.renameField = new TextFieldWidget(tr, x + 20, y + 20, 120, 16, Text.translatable("palettes.itemorganizer.rename.title"));
        this.renameField.setMaxLength(24);
    }

    public PaletteData getPaletteData() {
        return infiniteMode ? viewModel.getInfinitePaletteData() : viewModel.getPaletteData();
    }

    public void savePaletteData(PaletteData data) {
        if (data == null) return;
        if (infiniteMode) {
            StorageManager.getInstance().getInfinitePaletteRepository().save(data);
        } else {
            StorageManager.getInstance().getPaletteRepository().save(data);
        }
    }

    public boolean isInfiniteMode() {
        return infiniteMode;
    }

    public String getSearchText() {
        return searchField != null ? searchField.getText() : "";
    }

    public void setSearchText(String text) {
        if (searchField != null) {
            String clean = (text != null) ? text : "";
            if (!Objects.equals(searchField.getText(), clean)) {
                searchField.setText(clean);
            }
        }
    }

    public double getScrollOffset() {
        return scrollbar.getScrollOffset();
    }

    public void setScrollOffset(double offset) {
        scrollbar.setScrollOffset(offset);
    }

    public boolean isColorSortActive() {
        return colorSortActive;
    }

    public void setColorSortActive(boolean colorSortActive) {
        this.colorSortActive = colorSortActive;
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
        int sortBtnW = searchH;
        int sortBtnX = addBtnX - sortBtnW - 3;
        int searchW = Math.max(30, sortBtnX - (x + 4) - 4);
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

    public int getSlotsPerRow(int cardW, int slotSize) {
        int availW = Math.max(slotSize, cardW - 8);
        return Math.max(1, availW / slotSize);
    }

    public int getCardHeight() {
        return getCardHeight(null);
    }

    public int getCardHeight(PaletteRow row) {
        int slotSize = getSlotSize();
        int headerH = getHeaderHeight();
        if (!infiniteMode || row == null) {
            return 2 + headerH + 2 + slotSize + 3;
        }
        int cardW = getCardWidth();
        int slotsPerRow = getSlotsPerRow(cardW, slotSize);
        int totalLines = Math.max(1, (row.getSlotCount() + slotsPerRow - 1) / slotsPerRow);
        int slotGap = 1;
        return 2 + headerH + 2 + (totalLines * slotSize) + ((totalLines - 1) * slotGap) + 3;
    }

    public int getCardGap() {
        return 3;
    }

    private List<DisplayPalette> getFilteredPalettes(List<PaletteRow> allRows) {
        if (allRows == null) return new ArrayList<>();

        String query = searchField.getText().trim().toLowerCase();
        boolean filterActive = (searchFilterWidget != null && searchFilterWidget.isActive());

        List<DisplayPalette> exactList = new ArrayList<>();
        List<DisplayPalette> itemList = new ArrayList<>();
        List<DisplayPalette> colorList = new ArrayList<>();
        List<DisplayPalette> standardList = new ArrayList<>();

        for (int i = 0; i < allRows.size(); i++) {
            PaletteRow row = allRows.get(i);

            // check text query if present
            if (!query.isEmpty()) {
                boolean matchesText = false;
                String idxStr = String.valueOf(i + 1);
                if (idxStr.contains(query) || ("#" + idxStr).contains(query)) {
                    matchesText = true;
                } else if (row.getName() != null && row.getName().toLowerCase().contains(query)) {
                    matchesText = true;
                } else {
                    int count = row.getSlotCount();
                    for (int s = 0; s < count; s++) {
                        String itemId = row.getSlot(s);
                        if (itemId != null) {
                            if (itemId.toLowerCase().contains(query)) {
                                matchesText = true;
                                break;
                            }
                            ItemStack stack = getItemStackFromId(itemId);
                            if (!stack.isEmpty() && stack.getName().getString().toLowerCase().contains(query)) {
                                matchesText = true;
                                break;
                            }
                        }
                    }
                }
                if (!matchesText) {
                    continue;
                }
            }

            if (filterActive) {
                PaletteSearchFilterWidget.FilterMatchResult matchResult = searchFilterWidget.evaluateMatch(row);
                if (matchResult.isMatch()) {
                    DisplayPalette dp = new DisplayPalette(i, row);
                    if (matchResult.tier() == PaletteSearchFilterWidget.MatchTier.EXACT) {
                        exactList.add(dp);
                    } else if (matchResult.tier() == PaletteSearchFilterWidget.MatchTier.ITEM_MATCH) {
                        itemList.add(dp);
                    } else if (matchResult.tier() == PaletteSearchFilterWidget.MatchTier.COLOR_MATCH) {
                        colorList.add(dp);
                    }
                }
            } else {
                standardList.add(new DisplayPalette(i, row));
            }
        }

        com.itemorganizer.gui.util.PaletteColumnColorComparator colorComparator = com.itemorganizer.gui.util.PaletteColumnColorComparator.getInstance();

        if (filterActive) {
            if (colorSortActive) {
                exactList.sort((d1, d2) -> colorComparator.compare(d1.row(), d2.row()));
                itemList.sort((d1, d2) -> colorComparator.compare(d1.row(), d2.row()));
                colorList.sort((d1, d2) -> colorComparator.compare(d1.row(), d2.row()));
            }
            List<DisplayPalette> combined = new ArrayList<>(exactList.size() + itemList.size() + colorList.size());
            combined.addAll(exactList);
            combined.addAll(itemList);
            combined.addAll(colorList);
            return combined;
        } else {
            if (colorSortActive) {
                standardList.sort((d1, d2) -> colorComparator.compare(d1.row(), d2.row()));
            }
            return standardList;
        }
    }

    public int calculateTotalContentHeight(int count) {
        if (count <= 0) return 0;
        return (count * getCardHeight()) + ((count - 1) * getCardGap()) + 8;
    }

    private int calculateTotalContentHeight(List<DisplayPalette> displayList) {
        int total = 8;
        int cardGap = getCardGap();
        for (DisplayPalette dp : displayList) {
            total += getCardHeight(dp.row()) + cardGap;
        }
        return total;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;
        PaletteData data = getPaletteData();
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
        int totalContentHeight = calculateTotalContentHeight(displayList);
        scrollbar.setBounds(x + width - SCROLLBAR_WIDTH - 2, listStartY, SCROLLBAR_WIDTH, listHeight);
        scrollbar.updateMaxScroll(totalContentHeight, listHeight);

        updateHover(mouseX, mouseY, displayList, allRows.size());

        context.enableScissor(x, listStartY, x + width, listStartY + listHeight);

        int scrollY = (int) scrollbar.getScrollOffset();
        int cardX = x + 4;
        int cardW = getCardWidth();
        int slotSize = getSlotSize();
        int cardGap = getCardGap();
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
            int cardH = getCardHeight(row);

            if (currentY + cardH >= listStartY && currentY <= listStartY + listHeight) {
                boolean isCardHover = (activeModal == null && origIdx == hoveredOriginalRowIndex);
                boolean isDuplicate = isDuplicatePalette(row, allRows);

                // card background
                int cardBg = isDuplicate ? (isCardHover ? 0x4CEF4444 : 0x22EF4444) : (isCardHover ? 0x22FFFFFF : 0x00000000);
                int cardBorder = isDuplicate ? 0xFFEF4444 : (isCardHover ? 0x44FFFFFF : 0x00000000);
                context.fill(cardX, currentY, cardX + cardW, currentY + cardH, cardBg);
                RenderHelper.drawBorder(context, cardX, currentY, cardW, cardH, cardBorder);

                // card header
                int headerY = currentY + 2;
                int headerH = getHeaderHeight();
                int btnW = getButtonSize();
                int btnH = getButtonSize();
                int btnGap = getButtonGap();
                int btnY = headerY + (headerH - btnH) / 2;

                int btnDeleteX = cardX + cardW - 3 - btnW;
                int btnPlaceWorldX = btnDeleteX - btnGap - btnW;
                int nextBtnX = btnPlaceWorldX;

                int btnLoadX = -1;
                if (!infiniteMode) {
                    btnLoadX = nextBtnX - btnGap - btnW;
                    nextBtnX = btnLoadX;
                }
                int btnPasteHotbarX = nextBtnX - btnGap - btnW;
                nextBtnX = btnPasteHotbarX;

                int btnDupX = nextBtnX - btnGap - btnW;
                boolean showReorder = (cardW >= 170 && searchField.getText().trim().isEmpty() && !colorSortActive);
                int btnDownX = showReorder ? btnDupX - btnGap - btnW : btnDupX;
                int btnUpX = showReorder ? btnDownX - btnGap - btnW : btnDownX;
                int leftmostBtnX = showReorder ? btnUpX : btnDupX;

                String title = "#" + (origIdx + 1);
                if (isDuplicate) {
                    title += " " + Text.translatable("palettes.itemorganizer.duplicate_warning").getString();
                } else if (row.getName() != null && !row.getName().isEmpty()) {
                    title += " " + row.getName();
                }
                int maxTitleW = Math.max(20, leftmostBtnX - cardX - 6);
                String trimmedTitle = tr.trimToWidth(title, (int) (maxTitleW / Math.min(1.0f, textScale)));
                boolean isNameHover = (isCardHover && hoveredName);
                int titleColor = isDuplicate ? 0xFFEF4444 : (isNameHover ? 0xFF38BDF8 : 0xFFCBD5E1);

                int textY = headerY + Math.max(0, (headerH - Math.round(9 * textScale)) / 2);
                TextScaleHelper.drawScaledText(
                        context, tr, trimmedTitle, cardX + 4, textY, titleColor, false, textScale
                );

                float iconScale = Math.max(0.6f, Math.min(1.6f, ((float) btnW / 8.0f) * textScale));
                float btnCenterY = btnY + (btnH - 1) / 2.0f;

                boolean hoverLoad = (isCardHover && hoveredLoadBtn);
                boolean hoverPlaceWorld = (isCardHover && hoveredPlaceWorldBtn);
                boolean hoverPasteHotbar = (isCardHover && hoveredPasteHotbarBtn);
                boolean hoverDel = (isCardHover && hoveredDeleteBtn);
                boolean hoverDup = (isCardHover && hoveredDupBtn);
                boolean hoverUp = (isCardHover && hoveredUpBtn);
                boolean hoverDown = (isCardHover && hoveredDownBtn);

                if (showReorder) {
                    // up button
                    if (origIdx > 0) {
                        context.fill(btnUpX, btnY, btnUpX + btnW, btnY + btnH, hoverUp ? 0x801E293B  : 0x401E293B);
                        RenderHelper.drawBorder(context, btnUpX, btnY, btnW, btnH, hoverUp ? 0xFF38BDF8  : 0x4038BDF8);
                        RenderHelper.drawArrowUpIcon(context, btnUpX + (btnW - 1) / 2.0f, btnCenterY, iconScale, hoverUp ? 0xFFFFFFFF  : 0xFF94A3B8);
                    }
                    // down button
                    if (origIdx < allRows.size() - 1) {
                        context.fill(btnDownX, btnY, btnDownX + btnW, btnY + btnH, hoverDown ? 0x801E293B  : 0x401E293B);
                        RenderHelper.drawBorder(context, btnDownX, btnY, btnW, btnH, hoverDown ? 0xFF38BDF8 : 0x4038BDF8);
                        RenderHelper.drawArrowDownIcon(context, btnDownX + (btnW - 1) / 2.0f, btnCenterY, iconScale, hoverDown ? 0xFFFFFFFF : 0xFF94A3B8);
                    }
                }

                // duplicate button
                context.fill(btnDupX, btnY, btnDupX + btnW, btnY + btnH, hoverDup ? 0x801E3A5F : 0x401E3A5F);
                RenderHelper.drawBorder(context, btnDupX, btnY, btnW, btnH, hoverDup ? 0xFF38BDF8 : 0x8038BDF8);
                RenderHelper.drawDuplicateIcon(context, btnDupX + (btnW - 1) / 2.0f, btnCenterY, iconScale, hoverDup ?  0xFFFFFFFF : 0xFFBAE6FD);

                // place in world button
                context.fill(btnPlaceWorldX, btnY, btnPlaceWorldX + btnW, btnY + btnH, hoverPlaceWorld ? 0x80581C87 : 0x40581C87);
                RenderHelper.drawBorder(context, btnPlaceWorldX, btnY, btnW, btnH, hoverPlaceWorld ? 0xFFA855F7 : 0x80A855F7);
                RenderHelper.drawPlaceWorldIcon(context, btnPlaceWorldX + (btnW - 1) / 2.0f, btnCenterY, iconScale, hoverPlaceWorld ? 0xFFFFFFFF : 0xFFE9D5FF);

                // paste hotbar button
                context.fill(btnPasteHotbarX, btnY, btnPasteHotbarX + btnW, btnY + btnH, hoverPasteHotbar ? 0x8078350F : 0x4078350F);
                RenderHelper.drawBorder(context, btnPasteHotbarX, btnY, btnW, btnH, hoverPasteHotbar ? 0xFFF59E0B : 0x80F59E0B);
                RenderHelper.drawPasteHotbarIcon(context, btnPasteHotbarX + (btnW - 1) / 2.0f, btnCenterY, iconScale, hoverPasteHotbar ? 0xFFFFFFFF : 0xFFFDE68A);

                if (!infiniteMode) {
                    // load button
                    context.fill(btnLoadX, btnY, btnLoadX + btnW, btnY + btnH, hoverLoad ? 0x80065F46 : 0x40065F46);
                    RenderHelper.drawBorder(context, btnLoadX, btnY, btnW, btnH, hoverLoad ? 0xFF34D399 : 0x8034D399);
                    RenderHelper.drawLoadHotbarIcon(context, btnLoadX + (btnW - 1) / 2.0f, btnCenterY, iconScale, hoverLoad ? 0xFFFFFFFF : 0xFFA7F3D0);
                }

                // delete button
                context.fill(btnDeleteX, btnY, btnDeleteX + btnW, btnY + btnH, hoverDel ? 0x807F1D1D : 0x407F1D1D);
                RenderHelper.drawBorder(context, btnDeleteX, btnY, btnW, btnH, hoverDel ? 0xFFEF4444 : 0x80EF4444);
                RenderHelper.drawDeleteIcon(context, btnDeleteX + (btnW - 1) / 2.0f, btnCenterY, iconScale, hoverDel ? 0xFFFFFFFF : 0xFFFCA5A5);

                // continuous slot strip with wrapping in infinite mode
                int slotsY = headerY + headerH + 2;
                int slotCount = row.getSlotCount();
                int slotsPerRow = infiniteMode ? getSlotsPerRow(cardW, slotSize) : slotCount;
                int slotGap = 1;
                int slotsStartX = infiniteMode ? (cardX + 4) : (cardX + Math.max(4, (cardW - (slotCount * slotSize)) / 2));

                for (int s = 0; s < slotCount; s++) {
                    int line = infiniteMode ? (s / slotsPerRow) : 0;
                    int col = infiniteMode ? (s % slotsPerRow) : s;
                    int slotX = slotsStartX + (col * slotSize);
                    int slotY = slotsY + (line * (slotSize + slotGap));
                    boolean isSlotHover = (isCardHover && hoveredSlotIndex == s);

                    String itemId = row.getSlot(s);
                    ItemStack stack = RenderHelper.getItemStack(itemId);
                    RenderHelper.renderSlot(context, tr, stack, slotX, slotY, slotSize, itemScale,
                            isSlotHover, 0x1A000000, 0x3338BDF8, 0x22FFFFFF, UITheme.PRIMARY);
                }
            }
            currentY += cardH + cardGap;
        }

        context.disableScissor();

        scrollbar.render(context, mouseX, mouseY);

        // floating modal rendering
        if (activeModal != null) {
            activeModal.updateParentBounds(x, y, width, height);
            activeModal.render(context, tr, mouseX, mouseY, delta, textScale);
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

        int sortBtnW = searchH;
        int sortBtnH = searchH;
        int sortBtnX = addBtnX - sortBtnW - 3;
        int sortBtnY = searchY;

        int searchX = x + 4;
        int searchW = Math.max(30, sortBtnX - searchX - 4);

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

        // sort by column color toggle button
        boolean hoverSort = (activeModal == null && hoveredSortBtn);
        int sortBg = colorSortActive
                ? (hoverSort ? 0x800284C7 : 0x500284C7)
                : (hoverSort ? 0x601E293B : 0x331E293B);
        int sortBorder = colorSortActive
                ? (hoverSort ? 0xFF38BDF8 : 0xCC38BDF8)
                : (hoverSort ? 0xFF94A3B8 : 0x5094A3B8);
        context.fill(sortBtnX, sortBtnY, sortBtnX + sortBtnW, sortBtnY + sortBtnH, sortBg);
        RenderHelper.drawBorder(context, sortBtnX, sortBtnY, sortBtnW, sortBtnH, sortBorder);

        float sortIconScale = Math.max(0.7f, Math.min(1.5f, ((float) sortBtnH / 14.0f) * textScale));
        RenderHelper.drawColorSortIcon(context, sortBtnX + (sortBtnW - 1) / 2.0f, sortBtnY + (sortBtnH - 1) / 2.0f, sortIconScale, colorSortActive);

        // add palette button
        boolean hoverAdd = (activeModal == null && hoveredTopAddBtn);
        int addBg = hoverAdd ? 0x66065F46 : 0x33065F46;
        int addBorder = hoverAdd ? 0xFF34D399 : 0x8034D399;
        context.fill(addBtnX, addBtnY, addBtnX + addBtnW, addBtnY + addBtnH, addBg);
        RenderHelper.drawBorder(context, addBtnX, addBtnY, addBtnW, addBtnH, addBorder);

        float iconScale = Math.max(0.7f, Math.min(1.5f, ((float) addBtnH / 14.0f) * textScale));
        if (width < 160) {
            RenderHelper.drawPlusIcon(context, addBtnX + (addBtnW - 1) / 2.0f, addBtnY + (addBtnH - 1) / 2.0f, iconScale, hoverAdd ? 0xFFFFFFFF : 0xFFE2E8F0);
        } else {
            TextScaleHelper.drawVerticallyCenteredScaledText(
                    context, tr, Text.translatable("palettes.itemorganizer.new"), addBtnX + addBtnW / 2, addBtnY + addBtnH / 2, hoverAdd ? 0xFFFFFFFF : 0xFFE2E8F0, textScale
            );
        }
    }

    private void renderTooltips(DrawContext context, TextRenderer tr, int mouseX, int mouseY) {
        if (activeModal != null || DragAndDropManager.getInstance().isDragging() || !isMouseOver(mouseX, mouseY)) {
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
        hoveredPlaceWorldBtn = false;
        hoveredPasteHotbarBtn = false;
        hoveredDeleteBtn = false;
        hoveredDupBtn = false;
        hoveredUpBtn = false;
        hoveredDownBtn = false;
        hoveredName = false;
        hoveredTopAddBtn = false;
        hoveredSortBtn = false;
        activeTooltip = null;
        hoveredStack = ItemStack.EMPTY;

        if (activeModal != null || !isMouseOver(mouseX, mouseY)) return;

        int topBarH = getTopBarHeight();
        int searchH = getSearchHeight();
        int addBtnH = searchH;
        float textScale = viewModel.getConfig().getTextScale();
        int addBtnW = (width < 160) ? searchH : Math.round(40 * Math.min(1.3f, textScale));
        int addBtnX = x + width - SCROLLBAR_WIDTH - addBtnW - 4;
        int addBtnY = y + 2 + (topBarH - 4 - addBtnH) / 2;

        int sortBtnW = searchH;
        int sortBtnH = searchH;
        int sortBtnX = addBtnX - sortBtnW - 3;
        int sortBtnY = addBtnY;
        if (mouseX >= sortBtnX && mouseX <= sortBtnX + sortBtnW && mouseY >= sortBtnY && mouseY <= sortBtnY + sortBtnH) {
            hoveredSortBtn = true;
            activeTooltip = colorSortActive
                    ? Text.translatable("palettes.itemorganizer.tooltip.sort_column_color_active").getString()
                    : Text.translatable("palettes.itemorganizer.tooltip.sort_column_color").getString();
            return;
        }

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
        int cardGap = getCardGap();

        int currentY = listStartY + 3 - scrollY;

        for (int i = 0; i < displayList.size(); i++) {
            DisplayPalette dp = displayList.get(i);
            PaletteRow row = dp.row();
            int origIdx = dp.originalIndex();
            int cardH = getCardHeight(row);

            if (mouseY >= currentY && mouseY < currentY + cardH && mouseX >= cardX && mouseX <= cardX + cardW) {
                hoveredOriginalRowIndex = origIdx;

                int headerY = currentY + 2;
                int headerH = getHeaderHeight();
                int btnW = getButtonSize();
                int btnH = getButtonSize();
                int btnGap = getButtonGap();
                int btnY = headerY + (headerH - btnH) / 2;

                int btnDeleteX = cardX + cardW - 3 - btnW;
                int btnPlaceWorldX = btnDeleteX - btnGap - btnW;
                int nextBtnX = btnPlaceWorldX;

                int btnLoadX = -1;
                if (!infiniteMode) {
                    btnLoadX = nextBtnX - btnGap - btnW;
                    nextBtnX = btnLoadX;
                }
                int btnPasteHotbarX = nextBtnX - btnGap - btnW;
                nextBtnX = btnPasteHotbarX;

                int btnDupX = nextBtnX - btnGap - btnW;
                boolean showReorder = (cardW >= 170 && searchField.getText().trim().isEmpty() && !colorSortActive);
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
                    if (!infiniteMode && mouseX >= btnLoadX && mouseX <= btnLoadX + btnW) {
                        hoveredLoadBtn = true;
                        activeTooltip = Text.translatable("palettes.itemorganizer.tooltip.load_hotbar").getString();
                        return;
                    }
                    if (mouseX >= btnPlaceWorldX && mouseX <= btnPlaceWorldX + btnW) {
                        hoveredPlaceWorldBtn = true;
                        activeTooltip = Text.translatable("palettes.itemorganizer.tooltip.place_world").getString();
                        return;
                    }
                    if (mouseX >= btnPasteHotbarX && mouseX <= btnPasteHotbarX + btnW) {
                        hoveredPasteHotbarBtn = true;
                        activeTooltip = Text.translatable("palettes.itemorganizer.tooltip.paste_hotbar").getString();
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

                // check slots
                int slotsY = headerY + headerH + 2;
                int slotCount = row.getSlotCount();
                int slotsPerRow = infiniteMode ? getSlotsPerRow(cardW, slotSize) : slotCount;
                int slotGap = 1;
                int slotsStartX = infiniteMode ? (cardX + 4) : (cardX + Math.max(4, (cardW - (slotCount * slotSize)) / 2));

                if (infiniteMode) {
                    int totalLines = Math.max(1, (slotCount + slotsPerRow - 1) / slotsPerRow);
                    int totalSlotsHeight = (totalLines * slotSize) + ((totalLines - 1) * slotGap);
                    if (mouseY >= slotsY && mouseY < slotsY + totalSlotsHeight && mouseX >= slotsStartX) {
                        int relY = (int) mouseY - slotsY;
                        int line = relY / (slotSize + slotGap);
                        int lineRem = relY % (slotSize + slotGap);
                        if (lineRem < slotSize) {
                            int relX = (int) mouseX - slotsStartX;
                            int col = relX / slotSize;
                            if (col >= 0 && col < slotsPerRow) {
                                int s = (line * slotsPerRow) + col;
                                if (s >= 0 && s < slotCount) {
                                    hoveredSlotIndex = s;
                                    String itemId = row.getSlot(s);
                                    if (itemId != null) {
                                        hoveredStack = getItemStackFromId(itemId);
                                    }
                                    return;
                                }
                            }
                        }
                    }
                } else {
                    if (mouseY >= slotsY && mouseY < slotsY + slotSize) {
                        for (int s = 0; s < slotCount; s++) {
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
                }
                return;
            }
            currentY += cardH + cardGap;
        }
    }

    public ItemStack getItemStackFromId(String itemId) {
        return RenderHelper.getItemStack(itemId);
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        if (activeModal != null) {
            return activeModal.mouseClicked(click);
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

        if (click.button() == 0 && hoveredSortBtn) {
            colorSortActive = !colorSortActive;
            playClickSound();
            return true;
        }

        if (click.button() == 0 && hoveredTopAddBtn) {
            PaletteData data = getPaletteData();
            if (data != null) {
                PaletteRow newRow = new PaletteRow(9);
                data.addRow(newRow);
                savePaletteData(data);
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
        PaletteData data = getPaletteData();
        if (data == null) return false;
        List<PaletteRow> allRows = data.getRows();

        if (hoveredOriginalRowIndex >= 0 && hoveredOriginalRowIndex < allRows.size()) {
            PaletteRow row = allRows.get(hoveredOriginalRowIndex);

            if (!infiniteMode && click.button() == 0 && hoveredLoadBtn) {
                loadRowToHotbar(row);
                return true;
            }

            if (click.button() == 0 && hoveredPlaceWorldBtn) {
                placePaletteInWorld(row);
                return true;
            }

            if (click.button() == 0 && hoveredPasteHotbarBtn) {
                pastingHotbarPaletteId = row.getId();
                if (infiniteMode) {
                    executePasteHotbar();
                    return true;
                } else {
                    activeModal = ModalDialogComponent.builder()
                            .parentBounds(x, y, width, height)
                            .size(Math.min(220, width - 20), 85)
                            .type(ModalDialogComponent.ModalType.WARNING)
                            .title(Text.translatable("palettes.itemorganizer.paste_hotbar.title"))
                            .message(Text.translatable("palettes.itemorganizer.paste_hotbar.warning"))
                            .confirmButton(Text.translatable("palettes.itemorganizer.paste_hotbar.confirm"), this::executePasteHotbar)
                            .cancelButton(Text.translatable("button.itemorganizer.cancel"), () -> {
                                pastingHotbarPaletteId = null;
                                activeModal = null;
                            })
                            .build();
                    playClickSound();
                    return true;
                }
            }

            if (click.button() == 0 && hoveredDeleteBtn) {
                deletingPaletteId = row.getId();
                activeModal = ModalDialogComponent.builder()
                        .parentBounds(x, y, width, height)
                        .size(Math.min(220, width - 20), 85)
                        .type(ModalDialogComponent.ModalType.DANGER)
                        .title(Text.translatable("palettes.itemorganizer.delete.title"))
                        .message(Text.translatable("palettes.itemorganizer.delete.warning"))
                        .confirmButton(Text.translatable("profiles.itemorganizer.delete.btn"), this::executeDeletePalette)
                        .cancelButton(Text.translatable("button.itemorganizer.cancel"), () -> {
                            deletingPaletteId = null;
                            activeModal = null;
                        })
                        .build();
                playClickSound();
                return true;
            }

            if (click.button() == 0 && hoveredDupBtn) {
                data.duplicateRow(hoveredOriginalRowIndex);
                savePaletteData(data);
                playClickSound();
                return true;
            }

            if (click.button() == 0 && hoveredUpBtn) {
                if (data.moveUp(hoveredOriginalRowIndex)) {
                    savePaletteData(data);
                    playClickSound();
                }
                return true;
            }

            if (click.button() == 0 && hoveredDownBtn) {
                if (data.moveDown(hoveredOriginalRowIndex)) {
                    savePaletteData(data);
                    playClickSound();
                }
                return true;
            }

            if (click.button() == 0 && hoveredName) {
                renamingPaletteId = row.getId();
                renameField.setText(row.getName() != null ? row.getName() : "");
                renameField.setFocused(true);
                activeModal = ModalDialogComponent.builder()
                        .parentBounds(x, y, width, height)
                        .size(Math.min(220, width - 20), 80)
                        .type(ModalDialogComponent.ModalType.INFO)
                        .title(Text.translatable("palettes.itemorganizer.rename.title"))
                        .customContent((ctx, tr1, cx, cy, cw, ch, mx, my, dt, ts) -> {
                            renameField.setX(cx + 2);
                            renameField.setY(cy);
                            renameField.setWidth(cw - 4);
                            ctx.fill(renameField.getX() - 1, renameField.getY() - 1, renameField.getX() + renameField.getWidth() + 1, renameField.getY() + renameField.getHeight() + 1, UITheme.BG_INPUT);
                            RenderHelper.drawBorder(ctx, renameField.getX() - 1, renameField.getY() - 1, renameField.getWidth() + 2, renameField.getHeight() + 2, UITheme.BORDER_SUBTLE);
                            renameField.renderWidget(ctx, mx, my, 0);
                        })
                        .customClickHandler((click1, cx, cy, cw, ch) -> renameField.mouseClicked(click1, false))
                        .customKeyHandler(input -> renameField.keyPressed(input))
                        .customCharHandler(input -> renameField.charTyped(input))
                        .confirmButton(Text.translatable("profiles.itemorganizer.save"), this::applyRename)
                        .cancelButton(Text.translatable("button.itemorganizer.cancel"), () -> {
                            renamingPaletteId = null;
                            activeModal = null;
                        })
                        .build();
                playClickSound();
                return true;
            }

            // slot interaction
            if (hoveredSlotIndex >= 0 && hoveredSlotIndex < row.getSlotCount()) {
                // right click: clear slot
                if (click.button() == 1) {
                    String previous = row.getSlot(hoveredSlotIndex);
                    if (previous != null) {
                        com.itemorganizer.gui.undo.UndoManager.getInstance().record(
                                new com.itemorganizer.gui.undo.PaletteSlotUndoAction(row.getId(), hoveredSlotIndex, previous)
                        );
                    }
                    row.clearSlot(hoveredSlotIndex);
                    if (infiniteMode) {
                        row.updateInfiniteSlots();
                    }
                    savePaletteData(data);
                    SoundHelper.playBreak();
                    return true;
                }

                // left click: shift-click to hotbar or drag
                if (click.button() == 0) {
                    String itemId = row.getSlot(hoveredSlotIndex);
                    if (itemId != null) {
                        ItemStack stack = getItemStackFromId(itemId);
                        if (!stack.isEmpty()) {
                            if (HotbarActionHelper.hasShiftDown(click)) {
                                lastShiftPaletteRow = hoveredOriginalRowIndex;
                                lastShiftPaletteSlot = hoveredSlotIndex;
                                HotbarActionHelper.quickMoveToHotbar(MinecraftClient.getInstance(), stack);
                                return true;
                            }
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
        PaletteData data = getPaletteData();
        if (data != null) {
            for (PaletteRow row : data.getRows()) {
                if (renamingPaletteId.equals(row.getId())) {
                    String newName = renameField.getText().trim();
                    row.setName(newName.isEmpty() ? null : newName);
                    savePaletteData(data);
                    break;
                }
            }
        }
        renamingPaletteId = null;
        activeModal = null;
        playClickSound();
    }

    private void executeDeletePalette() {
        if (deletingPaletteId == null) return;
        PaletteData data = getPaletteData();
        if (data != null) {
            data.removeRowById(deletingPaletteId);
            savePaletteData(data);
        }
        deletingPaletteId = null;
        activeModal = null;
        playClickSound();
    }

    private void executePasteHotbar() {
        if (pastingHotbarPaletteId == null) return;
        PaletteData data = getPaletteData();
        if (data != null) {
            PaletteRow row = data.findRowById(pastingHotbarPaletteId);
            if (row != null) {
                com.itemorganizer.gui.undo.UndoManager.getInstance().record(
                        new com.itemorganizer.gui.undo.PaletteFullUndoAction(row)
                );
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    if (infiniteMode) {
                        List<String> hotbarItems = new ArrayList<>();
                        for (int s = 0; s < 9; s++) {
                            ItemStack st = client.player.getInventory().getStack(s);
                            if (st != null && !st.isEmpty()) {
                                Identifier id = Registries.ITEM.getId(st.getItem());
                                hotbarItems.add(id != null ? id.toString() : null);
                            } else {
                                hotbarItems.add(null);
                            }
                        }
                        row.appendItems(hotbarItems);
                    } else {
                        for (int s = 0; s < 9; s++) {
                            ItemStack st = client.player.getInventory().getStack(s);
                            if (st != null && !st.isEmpty()) {
                                Identifier id = Registries.ITEM.getId(st.getItem());
                                row.setSlot(s, id != null ? id.toString() : null);
                            } else {
                                row.setSlot(s, null);
                            }
                        }
                    }
                    savePaletteData(data);
                }
            }
        }
        pastingHotbarPaletteId = null;
        activeModal = null;
        playClickSound();
    }

    // copy the 9 slots to player hotbar
    private void loadRowToHotbar(PaletteRow row) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || !client.player.isCreative()) return;

        com.itemorganizer.gui.undo.UndoManager.getInstance().record(
                com.itemorganizer.gui.undo.HotbarFullUndoAction.capture(client)
        );

        for (int i = 0; i < 9; i++) {
            String itemId = row.getSlot(i);
            ItemStack stack = ItemStack.EMPTY;
            if (itemId != null && !itemId.trim().isEmpty()) {
                stack = getItemStackFromId(itemId);
                if (!stack.isEmpty()) {
                    stack = new ItemStack(stack.getItem(), 1);
                }
            }
            HotbarActionHelper.assignItemToSlot(client, i, stack);
        }

        SoundHelper.playClick();
    }

    // place palette blocks horizontally in the world starting below the player and teleport player 1 block to the left
    public void placePaletteInWorld(PaletteRow row) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null || row == null) return;

        BlockPos startPos = client.player.getBlockPos().down();
        Direction facing = client.player.getHorizontalFacing();
        MinecraftServer server = client.getServer();

        int slotCount = row.getSlotCount();
        for (int i = 0; i < slotCount; i++) {
            String itemId = row.getSlot(i);
            if (itemId == null || itemId.trim().isEmpty()) continue;

            Identifier id = Identifier.tryParse(itemId.trim());
            if (id == null) continue;

            Item item = Registries.ITEM.get(id);
            if (item instanceof BlockItem blockItem) {
                BlockPos targetPos = startPos.offset(facing, i);
                BlockState state = blockItem.getBlock().getDefaultState();

                if (server != null) {
                    RegistryKey<World> key = client.world.getRegistryKey();
                    server.execute(() -> {
                        ServerWorld serverWorld = server.getWorld(key);
                        if (serverWorld != null) {
                            serverWorld.setBlockState(targetPos, state, Block.NOTIFY_ALL);
                        }
                    });
                } else if (client.getNetworkHandler() != null) {
                    client.getNetworkHandler().sendChatCommand(
                            String.format(java.util.Locale.ROOT, "setblock %d %d %d %s",
                                    targetPos.getX(), targetPos.getY(), targetPos.getZ(), itemId.trim())
                    );
                }
                client.world.setBlockState(targetPos, state, Block.NOTIFY_ALL);
            }
        }

        // teleport player 1 block to their left (counter-clockwise relative to facing direction)
        Direction leftDir = facing.rotateYCounterclockwise();
        int dx = leftDir.getOffsetX();
        int dz = leftDir.getOffsetZ();
        double newX = client.player.getX() + dx;
        double newY = client.player.getY();
        double newZ = client.player.getZ() + dz;

        if (server != null) {
            server.execute(() -> {
                ServerPlayerEntity serverPlayer = server.getPlayerManager().getPlayer(client.player.getUuid());
                if (serverPlayer != null) {
                    serverPlayer.requestTeleport(newX, newY, newZ);
                }
            });
        } else if (client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendChatCommand(
                    String.format(java.util.Locale.ROOT, "tp @s ~%d ~ ~%d", dx, dz)
            );
        }
        client.player.setPosition(newX, newY, newZ);

        SoundHelper.playChime();
    }

    @Override
    public boolean mouseReleased(Click click) {
        lastShiftPaletteRow = -1;
        lastShiftPaletteSlot = -1;
        if (scrollbar.mouseReleased(click)) {
            return true;
        }

        DragAndDropManager dragManager = DragAndDropManager.getInstance();
        if (dragManager.isDragging()) {
            PaletteData data = getPaletteData();
            if (data != null) {
                updateHover(click.x(), click.y(), getFilteredPalettes(data.getRows()), data.getRows().size());
                if (hoveredOriginalRowIndex >= 0 && hoveredSlotIndex >= 0 && hoveredOriginalRowIndex < data.getRows().size()) {
                    PaletteRow row = data.getRows().get(hoveredOriginalRowIndex);
                    DragPayload payload = dragManager.getActivePayload();
                    String previous = row.getSlot(hoveredSlotIndex);
                    com.itemorganizer.gui.undo.UndoManager.getInstance().record(
                            new com.itemorganizer.gui.undo.PaletteSlotUndoAction(row.getId(), hoveredSlotIndex, previous)
                    );
                    row.setSlot(hoveredSlotIndex, payload.getItemId());
                    if (infiniteMode) {
                        row.updateInfiniteSlots();
                    }
                    savePaletteData(data);
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

        if (click.button() == 0 && HotbarActionHelper.hasShiftDown(click)) {
            PaletteData data = getPaletteData();
            if (data != null) {
                updateHover(click.x(), click.y(), getFilteredPalettes(data.getRows()), data.getRows().size());
                if (hoveredOriginalRowIndex >= 0 && hoveredSlotIndex >= 0 && hoveredOriginalRowIndex < data.getRows().size()) {
                    if (hoveredOriginalRowIndex != lastShiftPaletteRow || hoveredSlotIndex != lastShiftPaletteSlot) {
                        lastShiftPaletteRow = hoveredOriginalRowIndex;
                        lastShiftPaletteSlot = hoveredSlotIndex;
                        PaletteRow row = data.getRows().get(hoveredOriginalRowIndex);
                        String itemId = row.getSlot(hoveredSlotIndex);
                        if (itemId != null) {
                            ItemStack stack = getItemStackFromId(itemId);
                            if (!stack.isEmpty()) {
                                HotbarActionHelper.quickMoveToHotbar(MinecraftClient.getInstance(), stack);
                            }
                        }
                    }
                    return true;
                }
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
        if (activeModal != null) {
            return activeModal.keyPressed(input);
        }

        if (searchField.isFocused()) {
            if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
                searchField.setFocused(false);
                return true;
            }
            return searchField.keyPressed(input);
        }

        // 1-9 hotbar keys
        MinecraftClient client = MinecraftClient.getInstance();
        if (HotbarActionHelper.handleHotbarKeyPress(client, input, hoveredStack)) {
            return true;
        }
        return false;
    }

    public boolean charTyped(CharInput input) {
        if (activeModal != null) {
            return activeModal.charTyped(input);
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

    public boolean isEditingOrSearching() {
        return (searchField != null && searchField.isFocused()) || activeModal != null;
    }

    public static boolean isDuplicatePalette(PaletteRow row, List<PaletteRow> allRows) {
        if (row == null || !row.hasAnyItem() || allRows == null) {
            return false;
        }
        for (PaletteRow other : allRows) {
            if (other != null && other != row && !java.util.Objects.equals(other.getId(), row.getId())) {
                if (row.hasSameSlotsAs(other)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setSearchFilterWidget(PaletteSearchFilterWidget searchFilterWidget) {
        this.searchFilterWidget = searchFilterWidget;
    }

    public PaletteSearchFilterWidget getSearchFilterWidget() {
        return searchFilterWidget;
    }

    private void playClickSound() {
        SoundHelper.playClick();
    }
}
