package com.itemorganizer.gui.widget;

import com.itemorganizer.core.model.PaletteRow;
import com.itemorganizer.gui.component.ScrollbarComponent;
import com.itemorganizer.gui.dragdrop.DragAndDropManager;
import com.itemorganizer.gui.dragdrop.DragPayload;
import com.itemorganizer.gui.dragdrop.DragSource;
import com.itemorganizer.gui.palette.CreatePaletteState;
import com.itemorganizer.gui.theme.UITheme;
import com.itemorganizer.gui.undo.CreatePaletteUndoAction;
import com.itemorganizer.gui.undo.UndoManager;
import com.itemorganizer.gui.util.HotbarActionHelper;
import com.itemorganizer.gui.util.PaletteGenerator;
import com.itemorganizer.gui.util.PalettePlacementManager;
import com.itemorganizer.gui.util.RenderHelper;
import com.itemorganizer.gui.util.SoundHelper;
import com.itemorganizer.gui.util.TextScaleHelper;
import com.itemorganizer.gui.viewmodel.OrganizerViewModel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

// interactive widget for designing palettes with fixed controls and scrollable rows
public class CreatePaletteWidget {
    private static final int SCROLLBAR_WIDTH = 6;

    private final OrganizerViewModel viewModel;
    private int x;
    private int y;
    private int width;
    private int height;

    private final ScrollbarComponent scrollbar;

    // hover states
    private int hoveredInputSlot = -1;
    private int hoveredResultRow = -1;
    private int hoveredResultSlot = -1;
    private int hoveredPlaceBtnRow = -1;
    private int hoveredDeleteBtnRow = -1;
    private boolean hoveredClearInputBtn = false;
    private boolean hoveredDecSlots = false;
    private boolean hoveredIncSlots = false;
    private boolean hoveredAddRow = false;
    private boolean hoveredToggleCube = false;
    private boolean hoveredToggleSolid = false;
    private boolean hoveredToggleTrans = false;
    private boolean hoveredToggleUniform = false;
    private boolean hoveredToggleOres = false;
    private boolean hoveredToggleGlazed = false;
    private boolean hoveredToggleLights = false;

    private String activeTooltip = null;
    private ItemStack hoveredStack = ItemStack.EMPTY;

    // continuous shift drag tracking
    private int lastShiftResultRow = -1;
    private int lastShiftResultSlot = -1;

    public CreatePaletteWidget(OrganizerViewModel viewModel, int x, int y, int width, int height) {
        this.viewModel = viewModel;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        int listStartY = y + getTopHeaderHeight() + 2;
        int listHeight = Math.max(10, height - getTopHeaderHeight() - 4);
        this.scrollbar = new ScrollbarComponent(x + width - SCROLLBAR_WIDTH - 2, listStartY, SCROLLBAR_WIDTH, listHeight);
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        int listStartY = y + getTopHeaderHeight() + 2;
        int listHeight = Math.max(10, height - getTopHeaderHeight() - 4);
        this.scrollbar.setBounds(x + width - SCROLLBAR_WIDTH - 2, listStartY, SCROLLBAR_WIDTH, listHeight);
    }

    public int getSlotSize() {
        float scale = viewModel.getConfig().getCreatePaletteScale();
        return Math.max(12, Math.min(28, Math.round(18 * scale)));
    }

    public int getButtonSize() {
        float btnScale = viewModel.getConfig().getCreatePaletteButtonScale();
        return Math.max(8, Math.min(18, Math.round(10 * btnScale)));
    }

    public int getButtonGap() {
        float btnScale = viewModel.getConfig().getCreatePaletteButtonScale();
        return Math.max(1, Math.min(4, Math.round(2 * btnScale)));
    }

    public int getCardWidth() {
        return width - SCROLLBAR_WIDTH - 6;
    }

    public int getCardGap() {
        float paletteScale = viewModel.getConfig().getCreatePaletteScale();
        return Math.max(2, Math.min(6, Math.round(3 * paletteScale)));
    }

    public int getHeaderHeight() {
        float btnScale = viewModel.getConfig().getCreatePaletteButtonScale();
        float textScale = viewModel.getConfig().getTextScale();
        int btnH = getButtonSize();
        int textH = Math.round(9 * textScale);
        return Math.max(btnH + 2, Math.max(textH + 2, Math.round(10 * Math.max(btnScale, textScale))));
    }

    public int getTopHeaderHeight() {
        int btnH = getButtonSize();
        int ctrlBtnH = btnH + 4;
        int bannerH = 14;
        return bannerH + 3 + ctrlBtnH + 3 + ctrlBtnH + 3 + ctrlBtnH + 4;
    }

    public int getSlotsPerRow(int cardW, int slotSize) {
        int usableW = cardW - 8;
        return Math.max(1, usableW / slotSize);
    }

    public int getCardHeight(int slotCount) {
        int cardW = getCardWidth();
        int slotSize = getSlotSize();
        int slotsPerRow = getSlotsPerRow(cardW, slotSize);
        int lines = (slotCount + slotsPerRow - 1) / slotsPerRow;
        int slotGap = 1;
        int slotsH = lines * slotSize + Math.max(0, lines - 1) * slotGap;
        return 2 + getHeaderHeight() + 2 + slotsH + 4;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;
        CreatePaletteState state = viewModel.getCreatePaletteState();
        if (state == null) return;

        int topHeaderH = getTopHeaderHeight();
        int listStartY = y + topHeaderH + 2;
        int listHeight = Math.max(10, height - topHeaderH - 4);
        int totalContentH = calculateTotalContentHeight(state);

        scrollbar.setBounds(x + width - SCROLLBAR_WIDTH - 2, listStartY, SCROLLBAR_WIDTH, listHeight);
        scrollbar.updateMaxScroll(totalContentH, listHeight);

        updateHover(mouseX, mouseY, state);

        float textScale = viewModel.getConfig().getTextScale();
        float itemScale = viewModel.getConfig().getCreatePaletteItemScale();
        int cardX = x + 3;
        int cardW = getCardWidth();
        int cardGap = getCardGap();
        int slotSize = getSlotSize();
        int btnW = getButtonSize();
        int btnH = getButtonSize();
        int btnGap = getButtonGap();
        float iconScale = Math.max(0.6f, Math.min(1.6f, ((float) btnW / 8.0f) * textScale));

        // 1. fixed top header: banner and controls
        renderTopHeader(context, tr, cardX, cardW, textScale, btnH, state);

        // 2. scrollbar
        scrollbar.render(context, mouseX, mouseY);

        // 3. scrollable list of cards
        context.enableScissor(x, listStartY, x + width, listStartY + listHeight);

        int scrollY = (int) scrollbar.getScrollOffset();
        int curY = listStartY + 2 - scrollY;

        // input card
        int inputCardH = getCardHeight(state.getSlotCount());
        if (curY + inputCardH >= listStartY && curY <= listStartY + listHeight) {
            boolean isInputHover = (mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= curY && mouseY <= curY + inputCardH);
            RenderHelper.drawCard(context, cardX, curY, cardW, inputCardH, isInputHover ? 0x22FFFFFF : 0x1A000000, isInputHover ? 0x44FFFFFF : 0x22FFFFFF);

            int inHeaderY = curY + 2;
            int inHeaderH = getHeaderHeight();
            int inBtnY = inHeaderY + (inHeaderH - btnH) / 2;

            TextScaleHelper.drawLeftVerticallyCenteredScaledText(
                    context, tr, "Entrada", cardX + 6, inHeaderY + inHeaderH / 2,
                    0xFFCBD5E1, false, textScale
            );

            // clear input button
            int btnClearX = cardX + cardW - 4 - btnW;
            RenderHelper.drawCard(context, btnClearX, inBtnY, btnW, btnH, hoveredClearInputBtn ? 0x807F1D1D : 0x407F1D1D, hoveredClearInputBtn ? 0xFFEF4444 : 0x80EF4444);
            RenderHelper.drawDeleteIcon(context, btnClearX + (btnW - 1) / 2.0f, inBtnY + (btnH - 1) / 2.0f, iconScale, hoveredClearInputBtn ? 0xFFFFFFFF : 0xFFFCA5A5);

            // input slots
            int inSlotsY = inHeaderY + inHeaderH + 2;
            int slotCount = state.getSlotCount();
            int slotsPerRow = getSlotsPerRow(cardW, slotSize);
            int slotGap = 1;
            int slotsStartX = cardX + 4;

            for (int s = 0; s < slotCount; s++) {
                int line = s / slotsPerRow;
                int col = s % slotsPerRow;
                int slotX = slotsStartX + (col * slotSize);
                int slotY = inSlotsY + (line * (slotSize + slotGap));
                boolean isSlotHover = (hoveredInputSlot == s);

                String itemId = state.getInputSlot(s);
                ItemStack stack = RenderHelper.getItemStack(itemId);
                RenderHelper.renderSlot(context, tr, stack, slotX, slotY, slotSize, itemScale,
                        isSlotHover, 0x1A000000, 0x3338BDF8, 0x22FFFFFF, UITheme.PRIMARY);

                if (stack.isEmpty()) {
                    TextScaleHelper.drawVerticallyCenteredScaledText(
                            context, tr, String.valueOf(s + 1),
                            slotX + slotSize / 2, slotY + slotSize / 2,
                            0x44FFFFFF, false, textScale * 0.7f
                    );
                }
            }
        }
        curY += inputCardH + cardGap;

        // result row cards
        int resCardH = getCardHeight(state.getSlotCount());
        for (int r = 0; r < state.getResultRowCount(); r++) {
            if (curY + resCardH >= listStartY && curY <= listStartY + listHeight) {
                boolean isRowHover = (hoveredResultRow == r);
                RenderHelper.drawCard(context, cardX, curY, cardW, resCardH, isRowHover ? 0x22FFFFFF : 0x1A000000, isRowHover ? 0x44FFFFFF : 0x22FFFFFF);

                int resHeaderY = curY + 2;
                int resHeaderH = getHeaderHeight();
                int resBtnY = resHeaderY + (resHeaderH - btnH) / 2;

                int btnDelX = cardX + cardW - 4 - btnW;
                int btnPlaceX = btnDelX - btnGap - btnW;

                // place in world button
                boolean hoverPlace = (hoveredPlaceBtnRow == r);
                RenderHelper.drawCard(context, btnPlaceX, resBtnY, btnW, btnH, hoverPlace ? 0x80581C87 : 0x40581C87, hoverPlace ? 0xFFA855F7 : 0x80A855F7);
                RenderHelper.drawPlaceWorldIcon(context, btnPlaceX + (btnW - 1) / 2.0f, resBtnY + (btnH - 1) / 2.0f, iconScale, hoverPlace ? 0xFFFFFFFF : 0xFFE9D5FF);

                // delete row button
                boolean hoverDel = (hoveredDeleteBtnRow == r);
                RenderHelper.drawCard(context, btnDelX, resBtnY, btnW, btnH, hoverDel ? 0x807F1D1D : 0x407F1D1D, hoverDel ? 0xFFEF4444 : 0x80EF4444);
                RenderHelper.drawDeleteIcon(context, btnDelX + (btnW - 1) / 2.0f, resBtnY + (btnH - 1) / 2.0f, iconScale, hoverDel ? 0xFFFFFFFF : 0xFFFCA5A5);

                // slots strip
                int resSlotsY = resHeaderY + resHeaderH + 2;
                List<String> row = state.getResultRow(r);
                int slotCount = state.getSlotCount();
                int slotsPerRow = getSlotsPerRow(cardW, slotSize);
                int slotGap = 1;
                int slotsStartX = cardX + 4;

                for (int s = 0; s < slotCount; s++) {
                    int line = s / slotsPerRow;
                    int col = s % slotsPerRow;
                    int slotX = slotsStartX + (col * slotSize);
                    int slotY = resSlotsY + (line * (slotSize + slotGap));
                    boolean isSlotHover = (hoveredResultRow == r && hoveredResultSlot == s);

                    String itemId = (s < row.size()) ? row.get(s) : "";
                    ItemStack stack = RenderHelper.getItemStack(itemId);
                    boolean isAnchor = !state.getInputSlot(s).isEmpty();

                    int borderCol = isAnchor ? 0x66A855F7 : 0x22FFFFFF;
                    int hoverBorderCol = isAnchor ? 0xFFA855F7 : 0xFF38BDF8;

                    RenderHelper.renderSlot(context, tr, stack, slotX, slotY, slotSize, itemScale,
                            isSlotHover, 0x1A000000, 0x3338BDF8, borderCol, hoverBorderCol);
                }
            }
            curY += resCardH + cardGap;
        }

        context.disableScissor();

        renderTooltips(context, tr, mouseX, mouseY);
    }

    private void renderTopHeader(DrawContext context, TextRenderer tr, int cardX, int cardW,
                                 float textScale, int btnH, CreatePaletteState state) {
        int curY = y + 2;

        // banner
        int bannerH = 14;
        RenderHelper.drawCard(context, cardX, curY, cardW, bannerH, 0x1A000000, 0x33FFFFFF);
        TextScaleHelper.drawLeftVerticallyCenteredScaledText(
                context, tr, "ℹ Clic derecho a un slot de resultado para variar",
                cardX + 6, curY + bannerH / 2, 0xFFCBD5E1, false, Math.min(0.85f, textScale)
        );
        curY += bannerH + 3;

        // controls row: stepper and + Fila
        int ctrlBtnH = btnH + 4;
        int decX = cardX;
        int decW = ctrlBtnH;
        int countTxtW = Math.max(46, Math.round(50 * Math.min(1.1f, textScale)));
        int countTxtX = decX + decW + 2;
        int incX = countTxtX + countTxtW + 2;
        int incW = ctrlBtnH;
        int addRowX = incX + incW + 6;
        int addRowW = Math.max(38, Math.round(42 * Math.min(1.1f, textScale)));

        RenderHelper.drawCard(context, decX, curY, decW, ctrlBtnH, hoveredDecSlots ? 0x801E293B : 0x401E293B, hoveredDecSlots ? 0xFF38BDF8 : 0x4038BDF8);
        TextScaleHelper.drawVerticallyCenteredScaledText(context, tr, "-", decX + decW / 2, curY + ctrlBtnH / 2, 0xFFE2E8F0, false, textScale);

        TextScaleHelper.drawVerticallyCenteredScaledText(
                context, tr, state.getSlotCount() + " Slots",
                countTxtX + countTxtW / 2, curY + ctrlBtnH / 2, 0xFFE2E8F0, false, textScale
        );

        RenderHelper.drawCard(context, incX, curY, incW, ctrlBtnH, hoveredIncSlots ? 0x801E293B : 0x401E293B, hoveredIncSlots ? 0xFF38BDF8 : 0x4038BDF8);
        TextScaleHelper.drawVerticallyCenteredScaledText(context, tr, "+", incX + incW / 2, curY + ctrlBtnH / 2, 0xFFE2E8F0, false, textScale);

        RenderHelper.drawCard(context, addRowX, curY, addRowW, ctrlBtnH, hoveredAddRow ? 0x80065F46 : 0x40065F46, hoveredAddRow ? 0xFF34D399 : 0x8034D399);
        TextScaleHelper.drawVerticallyCenteredScaledText(context, tr, "+ Fila", addRowX + addRowW / 2, curY + ctrlBtnH / 2, hoveredAddRow ? 0xFFFFFFFF : 0xFFA7F3D0, false, textScale);

        curY += ctrlBtnH + 3;

        // toggles row 1
        int toggleGap = 2;
        int toggleW = Math.max(28, (cardW - (3 * toggleGap)) / 4);
        int cubeX = cardX;
        int solidX = cubeX + toggleW + toggleGap;
        int transX = solidX + toggleW + toggleGap;
        int uniformX = transX + toggleW + toggleGap;

        renderToggle(context, tr, cubeX, curY, toggleW, ctrlBtnH, "1x1x1", state.isFilterCube(), hoveredToggleCube, textScale);
        renderToggle(context, tr, solidX, curY, toggleW, ctrlBtnH, "Sólido", state.isFilterSolid(), hoveredToggleSolid, textScale);
        renderToggle(context, tr, transX, curY, toggleW, ctrlBtnH, "Transp.", state.isFilterTransparent(), hoveredToggleTrans, textScale);
        renderToggle(context, tr, uniformX, curY, toggleW, ctrlBtnH, "Textura", state.isFilterUniformTexture(), hoveredToggleUniform, textScale);

        curY += ctrlBtnH + 3;

        // toggles row 2: Minerales, Esmaltado, Luces
        int toggleW2 = Math.max(36, (cardW - (2 * toggleGap)) / 3);
        int oresX = cardX;
        int glazedX = oresX + toggleW2 + toggleGap;
        int lightsX = glazedX + toggleW2 + toggleGap;

        renderToggle(context, tr, oresX, curY, toggleW2, ctrlBtnH, "Minerales", state.isFilterOres(), hoveredToggleOres, textScale);
        renderToggle(context, tr, glazedX, curY, toggleW2, ctrlBtnH, "Esmaltado", state.isFilterGlazed(), hoveredToggleGlazed, textScale);
        renderToggle(context, tr, lightsX, curY, toggleW2, ctrlBtnH, "Luces", state.isFilterLights(), hoveredToggleLights, textScale);
    }

    private void renderToggle(DrawContext context, TextRenderer tr, int bx, int by, int bw, int bh,
                              String label, boolean active, boolean hover, float textScale) {
        int bg = active ? (hover ? 0x80065F46 : 0x50065F46) : (hover ? 0x40334155 : 0x20334155);
        int border = active ? 0xFF34D399 : (hover ? 0xFF64748B : 0x50475569);

        RenderHelper.drawCard(context, bx, by, bw, bh, bg, border);
        int textCol = active ? 0xFFFFFFFF : (hover ? 0xFFCBD5E1 : 0xFF94A3B8);
        TextScaleHelper.drawVerticallyCenteredScaledText(context, tr, label, bx + bw / 2, by + bh / 2, textCol, false, textScale);
    }

    private void updateHover(double mouseX, double mouseY, CreatePaletteState state) {
        hoveredInputSlot = -1;
        hoveredResultRow = -1;
        hoveredResultSlot = -1;
        hoveredPlaceBtnRow = -1;
        hoveredDeleteBtnRow = -1;
        hoveredClearInputBtn = false;
        hoveredDecSlots = false;
        hoveredIncSlots = false;
        hoveredAddRow = false;
        hoveredToggleCube = false;
        hoveredToggleSolid = false;
        hoveredToggleTrans = false;
        hoveredToggleUniform = false;
        hoveredToggleOres = false;
        hoveredToggleGlazed = false;
        hoveredToggleLights = false;
        hoveredStack = ItemStack.EMPTY;
        activeTooltip = null;

        if (!isMouseOver(mouseX, mouseY) || state == null) return;

        float textScale = viewModel.getConfig().getTextScale();
        int cardX = x + 3;
        int cardW = getCardWidth();
        int cardGap = getCardGap();
        int slotSize = getSlotSize();
        int btnW = getButtonSize();
        int btnH = getButtonSize();
        int btnGap = getButtonGap();

        int topHeaderH = getTopHeaderHeight();
        int listStartY = y + topHeaderH + 2;
        int listHeight = Math.max(10, height - topHeaderH - 4);

        // check top fixed header hover
        if (mouseY < listStartY) {
            int curY = y + 2;
            int bannerH = 14;
            curY += bannerH + 3;

            int ctrlBtnH = btnH + 4;
            int decX = cardX;
            int decW = ctrlBtnH;
            int countTxtW = Math.max(46, Math.round(50 * Math.min(1.1f, textScale)));
            int countTxtX = decX + decW + 2;
            int incX = countTxtX + countTxtW + 2;
            int incW = ctrlBtnH;
            int addRowX = incX + incW + 6;
            int addRowW = Math.max(38, Math.round(42 * Math.min(1.1f, textScale)));

            if (isHovered(mouseX, mouseY, decX, curY, decW, ctrlBtnH)) {
                hoveredDecSlots = true;
                activeTooltip = "Reducir número de slots";
                return;
            }
            if (isHovered(mouseX, mouseY, incX, curY, incW, ctrlBtnH)) {
                hoveredIncSlots = true;
                activeTooltip = "Aumentar número de slots";
                return;
            }
            if (isHovered(mouseX, mouseY, addRowX, curY, addRowW, ctrlBtnH)) {
                hoveredAddRow = true;
                activeTooltip = "Añadir fila de resultado";
                return;
            }

            curY += ctrlBtnH + 3;

            int toggleGap = 2;
            int toggleW = Math.max(28, (cardW - (3 * toggleGap)) / 4);
            int cubeX = cardX;
            int solidX = cubeX + toggleW + toggleGap;
            int transX = solidX + toggleW + toggleGap;
            int uniformX = transX + toggleW + toggleGap;

            if (isHovered(mouseX, mouseY, cubeX, curY, toggleW, ctrlBtnH)) {
                hoveredToggleCube = true;
                activeTooltip = "Filtro: Bloque 1x1x1 cubo";
                return;
            }
            if (isHovered(mouseX, mouseY, solidX, curY, toggleW, ctrlBtnH)) {
                hoveredToggleSolid = true;
                activeTooltip = "Filtro: Bloque sólido con colisión";
                return;
            }
            if (isHovered(mouseX, mouseY, transX, curY, toggleW, ctrlBtnH)) {
                hoveredToggleTrans = true;
                activeTooltip = "Filtro: Bloque transparente";
                return;
            }
            if (isHovered(mouseX, mouseY, uniformX, curY, toggleW, ctrlBtnH)) {
                hoveredToggleUniform = true;
                activeTooltip = "Filtro: Textura uniforme en todas las caras";
                return;
            }

            curY += ctrlBtnH + 3;

            int toggleW2 = Math.max(36, (cardW - (2 * toggleGap)) / 3);
            int oresX = cardX;
            int glazedX = oresX + toggleW2 + toggleGap;
            int lightsX = glazedX + toggleW2 + toggleGap;

            if (isHovered(mouseX, mouseY, oresX, curY, toggleW2, ctrlBtnH)) {
                hoveredToggleOres = true;
                activeTooltip = "Filtro: Incluir bloques de minerales";
                return;
            }
            if (isHovered(mouseX, mouseY, glazedX, curY, toggleW2, ctrlBtnH)) {
                hoveredToggleGlazed = true;
                activeTooltip = "Filtro: Incluir terracota esmaltada";
                return;
            }
            if (isHovered(mouseX, mouseY, lightsX, curY, toggleW2, ctrlBtnH)) {
                hoveredToggleLights = true;
                activeTooltip = "Filtro: Incluir bloques luminosos";
                return;
            }
            return;
        }

        // check scrollable list hover
        if (mouseY > listStartY + listHeight) return;

        int scrollY = (int) scrollbar.getScrollOffset();
        int curY = listStartY + 2 - scrollY;

        // input card
        int inputCardH = getCardHeight(state.getSlotCount());
        int inHeaderY = curY + 2;
        int inHeaderH = getHeaderHeight();
        int inBtnY = inHeaderY + (inHeaderH - btnH) / 2;
        int btnClearX = cardX + cardW - 4 - btnW;

        if (isHovered(mouseX, mouseY, btnClearX, inBtnY, btnW, btnH)) {
            hoveredClearInputBtn = true;
            activeTooltip = "Limpiar paleta de entrada";
            return;
        }

        int inSlotsY = inHeaderY + inHeaderH + 2;
        int slotCount = state.getSlotCount();
        int slotsPerRow = getSlotsPerRow(cardW, slotSize);
        int slotGap = 1;
        int slotsStartX = cardX + 4;

        for (int s = 0; s < slotCount; s++) {
            int line = s / slotsPerRow;
            int col = s % slotsPerRow;
            int slotX = slotsStartX + (col * slotSize);
            int slotY = inSlotsY + (line * (slotSize + slotGap));

            if (isHovered(mouseX, mouseY, slotX, slotY, slotSize, slotSize)) {
                hoveredInputSlot = s;
                String itemId = state.getInputSlot(s);
                if (!itemId.isEmpty()) {
                    ItemStack stack = RenderHelper.getItemStack(itemId);
                    if (!stack.isEmpty()) {
                        hoveredStack = stack;
                        activeTooltip = stack.getName().getString() + " (Entrada #" + (s + 1) + ")";
                    }
                }
                return;
            }
        }

        curY += inputCardH + cardGap;

        // result cards
        int resCardH = getCardHeight(state.getSlotCount());
        for (int r = 0; r < state.getResultRowCount(); r++) {
            int resHeaderY = curY + 2;
            int resHeaderH = getHeaderHeight();
            int resBtnY = resHeaderY + (resHeaderH - btnH) / 2;
            int btnDelX = cardX + cardW - 4 - btnW;
            int btnPlaceX = btnDelX - btnGap - btnW;

            if (isHovered(mouseX, mouseY, btnPlaceX, resBtnY, btnW, btnH)) {
                hoveredPlaceBtnRow = r;
                activeTooltip = Text.translatable("palettes.itemorganizer.tooltip.place_world").getString();
                return;
            }
            if (isHovered(mouseX, mouseY, btnDelX, resBtnY, btnW, btnH)) {
                hoveredDeleteBtnRow = r;
                activeTooltip = Text.translatable("palettes.itemorganizer.tooltip.delete").getString();
                return;
            }

            int resSlotsY = resHeaderY + resHeaderH + 2;
            List<String> row = state.getResultRow(r);

            for (int s = 0; s < slotCount; s++) {
                int line = s / slotsPerRow;
                int col = s % slotsPerRow;
                int slotX = slotsStartX + (col * slotSize);
                int slotY = resSlotsY + (line * (slotSize + slotGap));

                if (isHovered(mouseX, mouseY, slotX, slotY, slotSize, slotSize)) {
                    hoveredResultRow = r;
                    hoveredResultSlot = s;
                    String itemId = (s < row.size()) ? row.get(s) : "";
                    if (!itemId.isEmpty()) {
                        ItemStack stack = RenderHelper.getItemStack(itemId);
                        if (!stack.isEmpty()) {
                            hoveredStack = stack;
                            boolean isAnchor = !state.getInputSlot(s).isEmpty();
                            activeTooltip = stack.getName().getString() + (isAnchor ? " [Ancla fija]" : " [Clic der.: variar]");
                        }
                    }
                    return;
                }
            }

            if (isHovered(mouseX, mouseY, cardX, curY, cardW, resCardH)) {
                hoveredResultRow = r;
            }

            curY += resCardH + cardGap;
        }
    }

    private void renderTooltips(DrawContext context, TextRenderer tr, int mouseX, int mouseY) {
        if (DragAndDropManager.getInstance().isDragging() || !isMouseOver(mouseX, mouseY)) {
            return;
        }

        if (!hoveredStack.isEmpty()) {
            context.drawItemTooltip(tr, hoveredStack, mouseX, mouseY);
        } else if (activeTooltip != null && !activeTooltip.isEmpty()) {
            context.drawTooltip(tr, Text.literal(activeTooltip), mouseX, mouseY);
        }
    }

    public int calculateTotalContentHeight(CreatePaletteState state) {
        if (state == null) return 0;
        int slotCount = state.getSlotCount();
        int inputH = getCardHeight(slotCount);
        int cardGap = getCardGap();
        int resH = state.getResultRowCount() * (getCardHeight(slotCount) + cardGap);
        return inputH + cardGap + resH + 8;
    }

    public boolean mouseClicked(Click click) {
        CreatePaletteState state = viewModel.getCreatePaletteState();
        if (state == null) return false;

        if (scrollbar.mouseClicked(click)) {
            return true;
        }

        int button = click.button();
        updateHover(click.x(), click.y(), state);

        if (button == 0) {
            if (hoveredDecSlots) {
                CreatePaletteState before = state.copy();
                state.setSlotCount(state.getSlotCount() - 1);
                PaletteGenerator.generate(state);
                recordUndo(before, state);
                playClickSound();
                return true;
            }
            if (hoveredIncSlots) {
                CreatePaletteState before = state.copy();
                state.setSlotCount(state.getSlotCount() + 1);
                PaletteGenerator.generate(state);
                recordUndo(before, state);
                playClickSound();
                return true;
            }
            if (hoveredAddRow) {
                CreatePaletteState before = state.copy();
                state.addResultRow();
                PaletteGenerator.generate(state);
                recordUndo(before, state);
                playClickSound();
                return true;
            }
            if (hoveredToggleCube) {
                CreatePaletteState before = state.copy();
                state.setFilterCube(!state.isFilterCube());
                PaletteGenerator.clearCache();
                PaletteGenerator.generate(state);
                recordUndo(before, state);
                playClickSound();
                return true;
            }
            if (hoveredToggleSolid) {
                CreatePaletteState before = state.copy();
                state.setFilterSolid(!state.isFilterSolid());
                PaletteGenerator.clearCache();
                PaletteGenerator.generate(state);
                recordUndo(before, state);
                playClickSound();
                return true;
            }
            if (hoveredToggleTrans) {
                CreatePaletteState before = state.copy();
                state.setFilterTransparent(!state.isFilterTransparent());
                PaletteGenerator.clearCache();
                PaletteGenerator.generate(state);
                recordUndo(before, state);
                playClickSound();
                return true;
            }
            if (hoveredToggleUniform) {
                CreatePaletteState before = state.copy();
                state.setFilterUniformTexture(!state.isFilterUniformTexture());
                PaletteGenerator.clearCache();
                PaletteGenerator.generate(state);
                recordUndo(before, state);
                playClickSound();
                return true;
            }
            if (hoveredToggleOres) {
                CreatePaletteState before = state.copy();
                state.setFilterOres(!state.isFilterOres());
                PaletteGenerator.clearCache();
                PaletteGenerator.generate(state);
                recordUndo(before, state);
                playClickSound();
                return true;
            }
            if (hoveredToggleGlazed) {
                CreatePaletteState before = state.copy();
                state.setFilterGlazed(!state.isFilterGlazed());
                PaletteGenerator.clearCache();
                PaletteGenerator.generate(state);
                recordUndo(before, state);
                playClickSound();
                return true;
            }
            if (hoveredToggleLights) {
                CreatePaletteState before = state.copy();
                state.setFilterLights(!state.isFilterLights());
                PaletteGenerator.clearCache();
                PaletteGenerator.generate(state);
                recordUndo(before, state);
                playClickSound();
                return true;
            }
            if (hoveredClearInputBtn) {
                CreatePaletteState before = state.copy();
                state.clearInput();
                PaletteGenerator.generate(state);
                recordUndo(before, state);
                playClickSound();
                return true;
            }
            if (hoveredPlaceBtnRow >= 0) {
                executePlaceInWorld(state, hoveredPlaceBtnRow);
                return true;
            }
            if (hoveredDeleteBtnRow >= 0) {
                executeDeleteRow(state, hoveredDeleteBtnRow);
                return true;
            }
        }

        // input slot interaction
        if (hoveredInputSlot != -1) {
            DragAndDropManager dnd = DragAndDropManager.getInstance();
            if (dnd.isDragging()) {
                DragPayload activePayload = dnd.getActivePayload();
                String draggedId = (activePayload != null) ? activePayload.getItemId() : null;
                if (draggedId != null && !draggedId.isEmpty()) {
                    CreatePaletteState before = state.copy();
                    state.setInputSlot(hoveredInputSlot, draggedId);
                    PaletteGenerator.generate(state);
                    recordUndo(before, state);
                    playClickSound();
                    return true;
                }
            } else if (button == 1 || (button == 0 && HotbarActionHelper.hasShiftDown(click))) {
                CreatePaletteState before = state.copy();
                state.setInputSlot(hoveredInputSlot, "");
                PaletteGenerator.generate(state);
                recordUndo(before, state);
                playClickSound();
                return true;
            } else if (button == 0) {
                String currentId = state.getInputSlot(hoveredInputSlot);
                if (!currentId.isEmpty()) {
                    ItemStack stack = RenderHelper.getItemStack(currentId);
                    if (!stack.isEmpty()) {
                        DragPayload payload = DragPayload.ofIndexed(currentId, stack, DragSource.ORDENADO, hoveredInputSlot, false);
                        dnd.startDrag(payload, click.x(), click.y());
                        playClickSound();
                        return true;
                    }
                }
            }
        }

        // result slot interaction
        if (hoveredResultRow != -1 && hoveredResultSlot != -1) {
            if (button == 1) {
                CreatePaletteState before = state.copy();
                state.cycleSeed(hoveredResultRow, hoveredResultSlot);
                PaletteGenerator.generate(state);
                recordUndo(before, state);
                playClickSound();
                return true;
            } else if (button == 0) {
                if (HotbarActionHelper.hasShiftDown(click)) {
                    lastShiftResultRow = hoveredResultRow;
                    lastShiftResultSlot = hoveredResultSlot;
                    List<String> row = state.getResultRow(hoveredResultRow);
                    if (hoveredResultSlot < row.size()) {
                        String id = row.get(hoveredResultSlot);
                        if (!id.isEmpty()) {
                            ItemStack stack = RenderHelper.getItemStack(id);
                            if (!stack.isEmpty()) {
                                HotbarActionHelper.quickMoveToHotbar(MinecraftClient.getInstance(), stack);
                                return true;
                            }
                        }
                    }
                    return false;
                }

                List<String> row = state.getResultRow(hoveredResultRow);
                if (hoveredResultSlot < row.size()) {
                    String id = row.get(hoveredResultSlot);
                    if (!id.isEmpty()) {
                        ItemStack stack = RenderHelper.getItemStack(id);
                        if (!stack.isEmpty()) {
                            DragPayload payload = DragPayload.ofIndexed(id, stack, DragSource.ORDENADO, hoveredResultSlot, false);
                            DragAndDropManager.getInstance().startDrag(payload, click.x(), click.y());
                            playClickSound();
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public boolean mouseReleased(Click click) {
        lastShiftResultRow = -1;
        lastShiftResultSlot = -1;

        if (scrollbar.mouseReleased(click)) {
            return true;
        }

        DragAndDropManager dnd = DragAndDropManager.getInstance();
        if (dnd.isDragging() && dnd.isDraggedBeyondThreshold()) {
            CreatePaletteState state = viewModel.getCreatePaletteState();
            if (state != null) {
                updateHover(click.x(), click.y(), state);
                if (hoveredInputSlot >= 0) {
                    DragPayload payload = dnd.consumePayload();
                    if (payload != null && payload.getItemId() != null && !payload.getItemId().isEmpty()) {
                        CreatePaletteState before = state.copy();
                        state.setInputSlot(hoveredInputSlot, payload.getItemId());
                        PaletteGenerator.generate(state);
                        recordUndo(before, state);
                        playClickSound();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (scrollbar.mouseDragged(click, deltaX, deltaY)) {
            return true;
        }

        if (click.button() == 0 && HotbarActionHelper.hasShiftDown(click)) {
            CreatePaletteState state = viewModel.getCreatePaletteState();
            if (state != null) {
                updateHover(click.x(), click.y(), state);
                if (hoveredResultRow >= 0 && hoveredResultSlot >= 0) {
                    if (hoveredResultRow != lastShiftResultRow || hoveredResultSlot != lastShiftResultSlot) {
                        lastShiftResultRow = hoveredResultRow;
                        lastShiftResultSlot = hoveredResultSlot;
                        List<String> row = state.getResultRow(hoveredResultRow);
                        if (hoveredResultSlot < row.size()) {
                            String id = row.get(hoveredResultSlot);
                            if (!id.isEmpty()) {
                                ItemStack stack = RenderHelper.getItemStack(id);
                                if (!stack.isEmpty()) {
                                    HotbarActionHelper.quickMoveToHotbar(MinecraftClient.getInstance(), stack);
                                }
                            }
                        }
                    }
                    return true;
                }
            }
        }

        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isMouseOver(mouseX, mouseY)) {
            return scrollbar.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        return false;
    }

    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        int key = input.key();
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9) {
            return handleHotbarKey(key - GLFW.GLFW_KEY_1);
        }
        if (key >= GLFW.GLFW_KEY_KP_1 && key <= GLFW.GLFW_KEY_KP_9) {
            return handleHotbarKey(key - GLFW.GLFW_KEY_KP_1);
        }
        return false;
    }

    public boolean handleHotbarKey(int slotIndex) {
        if (hoveredResultRow != -1 && hoveredResultSlot != -1) {
            CreatePaletteState state = viewModel.getCreatePaletteState();
            if (state != null) {
                List<String> row = state.getResultRow(hoveredResultRow);
                if (hoveredResultSlot < row.size()) {
                    String id = row.get(hoveredResultSlot);
                    if (!id.isEmpty()) {
                        ItemStack stack = RenderHelper.getItemStack(id);
                        if (!stack.isEmpty()) {
                            HotbarActionHelper.assignItemToSlot(MinecraftClient.getInstance(), slotIndex, stack);
                            playClickSound();
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private void executePlaceInWorld(CreatePaletteState state, int rowIndex) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;

        List<String> toPlace = state.getResultRow(rowIndex);
        if (toPlace.isEmpty() || toPlace.stream().allMatch(String::isEmpty)) {
            return;
        }

        PaletteRow row = new PaletteRow("Paleta Generada", toPlace);
        PalettePlacementManager.getInstance().placePalette(client, row);
        playClickSound();
    }

    private void executeDeleteRow(CreatePaletteState state, int rowIndex) {
        CreatePaletteState before = state.copy();
        state.removeResultRow(rowIndex);
        PaletteGenerator.generate(state);
        recordUndo(before, state);
        playClickSound();
    }

    private void recordUndo(CreatePaletteState before, CreatePaletteState after) {
        UndoManager.getInstance().record(new CreatePaletteUndoAction(before, after));
    }

    public double getScrollOffset() {
        return scrollbar.getScrollOffset();
    }

    public void setScrollOffset(double offset) {
        scrollbar.setScrollOffset(offset);
    }

    private boolean isHovered(double mx, double my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    private void playClickSound() {
        SoundHelper.playClick();
    }
}
