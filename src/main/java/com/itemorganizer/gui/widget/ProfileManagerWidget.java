package com.itemorganizer.gui.widget;

import com.itemorganizer.core.model.ItemSlotPosition;
import com.itemorganizer.core.model.ProfileData;
import com.itemorganizer.gui.util.RenderHelper;
import com.itemorganizer.gui.util.TextScaleHelper;
import com.itemorganizer.gui.viewmodel.OrganizerViewModel;
import com.itemorganizer.storage.ProfileRepository;
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
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

// widget for managing profiles (create, clone, rename, delete)
public class ProfileManagerWidget implements Drawable, Element, Selectable {
    public static final int SCROLLBAR_WIDTH = 3;
    public static final int ROW_HEIGHT = 24;
    public static final int ROW_GAP = 3;
    public static final int BTN_DELETE_W = 16;
    public static final int BTN_RENAME_W = 16;
    public static final int BTN_LOAD_W = 44;
    public static final int BTN_H = 16;

    private final OrganizerViewModel viewModel;
    private int x;
    private int y;
    private int width;
    private int height;

    private final VerticalScrollbar scrollbar;

    // modal dialog state
    private boolean isCreating = false;
    private boolean cloneActive = false;
    private TextFieldWidget createNameField;
    private String createErrorMessage = "";

    private String renamingProfileName = null;
    private TextFieldWidget renameField;
    private String renameErrorMessage = "";

    private String deletingProfileName = null;

    // tooltip
    private Text hoveredTooltip = null;

    public ProfileManagerWidget(OrganizerViewModel viewModel, int x, int y, int width, int height) {
        this.viewModel = viewModel;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        int listStartY = y + 40;
        int listHeight = height - 44;
        this.scrollbar = new VerticalScrollbar(x + width - SCROLLBAR_WIDTH - 4, listStartY, SCROLLBAR_WIDTH, listHeight);

        initTextFields();
    }

    private void initTextFields() {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;

        int modalW = Math.min(220, width - 20);
        int modalX = x + (width - modalW) / 2;
        int modalY = y + (height - 110) / 2;

        this.createNameField = new TextFieldWidget(textRenderer, modalX + 10, modalY + 30, modalW - 20, 18, Text.translatable("profiles.itemorganizer.create.title"));
        this.createNameField.setMaxLength(24);
        this.createNameField.setPlaceholder(Text.translatable("profiles.itemorganizer.create.placeholder"));

        this.renameField = new TextFieldWidget(textRenderer, modalX + 10, modalY + 32, modalW - 20, 18, Text.translatable("profiles.itemorganizer.rename.title", ""));
        this.renameField.setMaxLength(24);
        this.renameField.setPlaceholder(Text.translatable("profiles.itemorganizer.rename.placeholder"));
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        int listStartY = y + 40;
        int listHeight = height - 44;
        this.scrollbar.setBounds(x + width - SCROLLBAR_WIDTH - 4, listStartY, SCROLLBAR_WIDTH, listHeight);

        int modalW = Math.min(220, width - 20);
        int modalX = x + (width - modalW) / 2;
        int modalY = y + (height - 110) / 2;
        if (createNameField != null) {
            createNameField.setX(modalX + 10);
            createNameField.setY(modalY + 30);
            createNameField.setWidth(modalW - 20);
        }
        if (renameField != null) {
            renameField.setX(modalX + 10);
            renameField.setY(modalY + 32);
            renameField.setWidth(modalW - 20);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;
        hoveredTooltip = null;

        List<String> profiles = viewModel.getAvailableProfiles();
        ProfileData activeProfile = viewModel.getActiveProfile();
        String activeName = (activeProfile != null) ? activeProfile.getName() : "";

        float textScale = viewModel.getConfig().getTextScale();

        // header
        int headerY = y + 8;
        TextScaleHelper.drawScaledText(context, tr, Text.translatable("profiles.itemorganizer.title"), x + 8, headerY, 0xFF38BDF8, true, textScale);

        // active profile subtitle
        Text activeLabel = Text.translatable("profiles.itemorganizer.active");
        TextScaleHelper.drawScaledText(context, tr, activeLabel, x + 8, headerY + 14, 0xFF94A3B8, false, textScale);
        int activoLabelW = (int) (tr.getWidth(activeLabel) * textScale);
        TextScaleHelper.drawScaledText(context, tr, activeName, x + 8 + activoLabelW, headerY + 14, 0xFF34D399, true, textScale);

        boolean modalActive = isCreating || renamingProfileName != null || deletingProfileName != null;

        // new profile button
        int addBtnW = Math.max(86, Math.round(86 * Math.max(1.0f, textScale)));
        int addBtnH = Math.max(18, Math.round(18 * Math.max(1.0f, textScale)));
        int addBtnX = x + width - addBtnW - 10;
        int addBtnY = y + 10;
        boolean hoverAdd = !modalActive && mouseX >= addBtnX && mouseX <= addBtnX + addBtnW && mouseY >= addBtnY && mouseY <= addBtnY + addBtnH;

        context.fill(addBtnX, addBtnY, addBtnX + addBtnW, addBtnY + addBtnH, hoverAdd ? 0x66065F46 : 0x33065F46);
        RenderHelper.drawBorder(context, addBtnX, addBtnY, addBtnW, addBtnH, hoverAdd ? 0xFF34D399 : 0x8034D399);
        TextScaleHelper.drawVerticallyCenteredScaledText(context, tr, Text.translatable("profiles.itemorganizer.new"), addBtnX + addBtnW / 2, addBtnY + addBtnH / 2, hoverAdd ? 0xFFFFFFFF : 0xFFE2E8F0, textScale);

        if (hoverAdd) {
            hoveredTooltip = Text.translatable("profiles.itemorganizer.tooltip.new");
        }

        // profile list
        int rowHeight = Math.max(24, Math.round(ROW_HEIGHT * Math.max(1.0f, textScale)));
        int btnH = Math.max(16, Math.round(BTN_H * Math.max(1.0f, textScale)));
        int listStartY = y + addBtnH + 18;
        int listHeight = height - (addBtnH + 22);
        int totalContentH = profiles.size() * (rowHeight + ROW_GAP);
        scrollbar.setBounds(x + width - SCROLLBAR_WIDTH - 4, listStartY, SCROLLBAR_WIDTH, listHeight);
        scrollbar.updateMaxScroll(totalContentH, listHeight);

        context.enableScissor(x + 4, listStartY, x + width - 4, listStartY + listHeight);

        int rowStartX = x + 8;
        int rowW = width - SCROLLBAR_WIDTH - 20;

        for (int i = 0; i < profiles.size(); i++) {
            String profileName = profiles.get(i);
            boolean isActive = profileName.equals(activeName);
            boolean isDefault = profileName.equalsIgnoreCase(ProfileRepository.DEFAULT_PROFILE_NAME);

            int rowY = listStartY + (i * (rowHeight + ROW_GAP)) - (int) scrollbar.getScrollOffset();

            // viewport culling
            if (rowY + rowHeight < listStartY || rowY > listStartY + listHeight) {
                continue;
            }

            boolean hoverRow = !modalActive && mouseX >= rowStartX && mouseX <= rowStartX + rowW && mouseY >= rowY && mouseY <= rowY + rowHeight;

            // row background
            int rowBg = isActive ? 0x2238BDF8 : (hoverRow ? 0x14FFFFFF : 0x08FFFFFF);
            int rowBorder = isActive ? 0xFF38BDF8 : (hoverRow ? 0x4038BDF8 : 0x18FFFFFF);
            context.fill(rowStartX, rowY, rowStartX + rowW, rowY + rowHeight, rowBg);
            RenderHelper.drawBorder(context, rowStartX, rowY, rowW, rowHeight, rowBorder);

            // indicator icon and name
            int textY = rowY + (rowHeight - Math.round(8 * textScale)) / 2;
            if (isActive) {
                TextScaleHelper.drawVerticallyCenteredScaledText(context, tr, "●", rowStartX + 10, rowY + rowHeight / 2, 0xFF38BDF8, textScale);
                TextScaleHelper.drawScaledText(context, tr, profileName, rowStartX + 20, textY, 0xFFFFFFFF, true, textScale);
            } else {
                TextScaleHelper.drawVerticallyCenteredScaledText(context, tr, "○", rowStartX + 10, rowY + rowHeight / 2, 0x6694A3B8, textScale);
                TextScaleHelper.drawScaledText(context, tr, profileName, rowStartX + 20, textY, 0xFFCBD5E1, false, textScale);
            }

            // row buttons
            int btnY = rowY + (rowHeight - btnH) / 2;
            int rightX = rowStartX + rowW - 4;

            // delete button
            int delBtnX = rightX - BTN_DELETE_W;
            boolean hoverDel = !modalActive && !isDefault && mouseX >= delBtnX && mouseX <= delBtnX + BTN_DELETE_W && mouseY >= btnY && mouseY <= btnY + btnH;
            if (!isDefault) {
                context.fill(delBtnX, btnY, delBtnX + BTN_DELETE_W, btnY + btnH, hoverDel ? 0x807F1D1D : 0x407F1D1D);
                RenderHelper.drawBorder(context, delBtnX, btnY, BTN_DELETE_W, btnH, hoverDel ? 0xFFEF4444 : 0x80EF4444);
                TextScaleHelper.drawVerticallyCenteredScaledText(context, tr, "✕", delBtnX + BTN_DELETE_W / 2, btnY + btnH / 2, hoverDel ? 0xFFFFFFFF : 0xFFFCA5A5, textScale);
                if (hoverDel) hoveredTooltip = Text.translatable("profiles.itemorganizer.tooltip.delete");
            } else {
                context.fill(delBtnX, btnY, delBtnX + BTN_DELETE_W, btnY + btnH, 0x0DFFFFFF);
                RenderHelper.drawBorder(context, delBtnX, btnY, BTN_DELETE_W, btnH, 0x14FFFFFF);
                TextScaleHelper.drawVerticallyCenteredScaledText(context, tr, "✕", delBtnX + BTN_DELETE_W / 2, btnY + btnH / 2, 0x4094A3B8, textScale);
            }

            // rename button
            int renBtnX = delBtnX - BTN_RENAME_W - 3;
            boolean hoverRen = !modalActive && !isDefault && mouseX >= renBtnX && mouseX <= renBtnX + BTN_RENAME_W && mouseY >= btnY && mouseY <= btnY + btnH;
            if (!isDefault) {
                context.fill(renBtnX, btnY, renBtnX + BTN_RENAME_W, btnY + btnH, hoverRen ? 0x801E3A5F : 0x401E3A5F);
                RenderHelper.drawBorder(context, renBtnX, btnY, BTN_RENAME_W, btnH, hoverRen ? 0xFF38BDF8 : 0x8038BDF8);
                TextScaleHelper.drawVerticallyCenteredScaledText(context, tr, "✎", renBtnX + BTN_RENAME_W / 2, btnY + btnH / 2, hoverRen ? 0xFFFFFFFF : 0xFFBAE6FD, textScale);
                if (hoverRen) hoveredTooltip = Text.translatable("profiles.itemorganizer.tooltip.rename");
            } else {
                context.fill(renBtnX, btnY, renBtnX + BTN_RENAME_W, btnY + btnH, 0x0DFFFFFF);
                RenderHelper.drawBorder(context, renBtnX, btnY, BTN_RENAME_W, btnH, 0x14FFFFFF);
                TextScaleHelper.drawVerticallyCenteredScaledText(context, tr, "✎", renBtnX + BTN_RENAME_W / 2, btnY + btnH / 2, 0x4094A3B8, textScale);
            }

            // load button
            int loadBtnX = renBtnX - BTN_LOAD_W - 3;
            boolean hoverLoad = !modalActive && !isActive && mouseX >= loadBtnX && mouseX <= loadBtnX + BTN_LOAD_W && mouseY >= btnY && mouseY <= btnY + btnH;
            if (!isActive) {
                context.fill(loadBtnX, btnY, loadBtnX + BTN_LOAD_W, btnY + btnH, hoverLoad ? 0x80065F46 : 0x40065F46);
                RenderHelper.drawBorder(context, loadBtnX, btnY, BTN_LOAD_W, btnH, hoverLoad ? 0xFF34D399 : 0x8034D399);
                TextScaleHelper.drawVerticallyCenteredScaledText(context, tr, Text.translatable("profiles.itemorganizer.load"), loadBtnX + BTN_LOAD_W / 2, btnY + btnH / 2, hoverLoad ? 0xFFFFFFFF : 0xFFA7F3D0, textScale);
                if (hoverLoad) hoveredTooltip = Text.translatable("profiles.itemorganizer.tooltip.load");
            } else {
                context.fill(loadBtnX, btnY, loadBtnX + BTN_LOAD_W, btnY + btnH, 0x1A10B981);
                RenderHelper.drawBorder(context, loadBtnX, btnY, BTN_LOAD_W, btnH, 0x4034D399);
                TextScaleHelper.drawVerticallyCenteredScaledText(context, tr, Text.translatable("profiles.itemorganizer.loaded"), loadBtnX + BTN_LOAD_W / 2, btnY + btnH / 2, 0x8034D399, textScale);
            }
        }

        context.disableScissor();

        scrollbar.render(context, mouseX, mouseY);

        // modal rendering
        if (isCreating) {
            renderCreateModal(context, tr, mouseX, mouseY, delta);
        } else if (renamingProfileName != null) {
            renderRenameModal(context, tr, mouseX, mouseY, delta);
        } else if (deletingProfileName != null) {
            renderDeleteModal(context, tr, mouseX, mouseY, delta);
        }

        // tooltip rendering
        if (hoveredTooltip != null && !modalActive) {
            context.drawTooltip(tr, hoveredTooltip, mouseX, mouseY);
        }
    }

    private void renderCreateModal(DrawContext context, TextRenderer tr, int mouseX, int mouseY, float delta) {
        float textScale = viewModel.getConfig().getTextScale();
        int modalW = Math.min(220, width - 20);
        int modalH = 120;
        int modalX = x + (width - modalW) / 2;
        int modalY = y + (height - modalH) / 2;

        context.fill(x, y, x + width, y + height, 0xDD0B0F19);
        context.fill(modalX, modalY, modalX + modalW, modalY + modalH, 0xFF141820);
        RenderHelper.drawBorder(context, modalX, modalY, modalW, modalH, 0xFF34D399);

        TextScaleHelper.drawCenteredScaledText(context, tr, Text.translatable("profiles.itemorganizer.create.title"), modalX + modalW / 2, modalY + 8, 0xFF34D399, textScale);

        context.fill(createNameField.getX() - 1, createNameField.getY() - 1, createNameField.getX() + createNameField.getWidth() + 1, createNameField.getY() + createNameField.getHeight() + 1, 0xFF0B0F19);
        RenderHelper.drawBorder(context, createNameField.getX() - 1, createNameField.getY() - 1, createNameField.getWidth() + 2, createNameField.getHeight() + 2, 0x33FFFFFF);
        createNameField.renderWidget(context, mouseX, mouseY, delta);

        // mode toggle
        int toggleY = modalY + 54;
        int toggleH = 16;
        int toggleW = modalW - 20;
        int toggleX = modalX + 10;
        boolean hoverToggle = mouseX >= toggleX && mouseX <= toggleX + toggleW && mouseY >= toggleY && mouseY <= toggleY + toggleH;
        Text modeText = cloneActive ? Text.translatable("profiles.itemorganizer.create.clone") : Text.translatable("profiles.itemorganizer.create.empty");

        context.fill(toggleX, toggleY, toggleX + toggleW, toggleY + toggleH, hoverToggle ? 0x401E293B : 0x2A1E293B);
        RenderHelper.drawBorder(context, toggleX, toggleY, toggleW, toggleH, hoverToggle ? 0xFF38BDF8 : 0x4D38BDF8);
        TextScaleHelper.drawCenteredScaledText(context, tr, modeText, toggleX + toggleW / 2, toggleY + 4, 0xFFE2E8F0, textScale);

        if (!createErrorMessage.isEmpty()) {
            TextScaleHelper.drawCenteredScaledText(context, tr, createErrorMessage, modalX + modalW / 2, modalY + 74, 0xFFEF4444, textScale);
        }

        int btnW = (modalW - 28) / 2;
        int btnH = 18;
        int btnY = modalY + modalH - 24;

        int createBtnX = modalX + 10;
        boolean hoverCreate = mouseX >= createBtnX && mouseX <= createBtnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        context.fill(createBtnX, btnY, createBtnX + btnW, btnY + btnH, hoverCreate ? 0x80065F46 : 0x40065F46);
        RenderHelper.drawBorder(context, createBtnX, btnY, btnW, btnH, hoverCreate ? 0xFF34D399 : 0x8034D399);
        TextScaleHelper.drawCenteredScaledText(context, tr, Text.translatable("profiles.itemorganizer.create.btn"), createBtnX + btnW / 2, btnY + 5, 0xFFFFFFFF, textScale);

        int cancelBtnX = createBtnX + btnW + 8;
        boolean hoverCancel = mouseX >= cancelBtnX && mouseX <= cancelBtnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        context.fill(cancelBtnX, btnY, cancelBtnX + btnW, btnY + btnH, hoverCancel ? 0x33FFFFFF : 0x1AFFFFFF);
        RenderHelper.drawBorder(context, cancelBtnX, btnY, btnW, btnH, hoverCancel ? 0x66FFFFFF : 0x33FFFFFF);
        TextScaleHelper.drawCenteredScaledText(context, tr, Text.translatable("button.itemorganizer.cancel"), cancelBtnX + btnW / 2, btnY + 5, 0xFFE2E8F0, textScale);
    }

    private void renderRenameModal(DrawContext context, TextRenderer tr, int mouseX, int mouseY, float delta) {
        float textScale = viewModel.getConfig().getTextScale();
        int modalW = Math.min(220, width - 20);
        int modalH = 100;
        int modalX = x + (width - modalW) / 2;
        int modalY = y + (height - modalH) / 2;

        context.fill(x, y, x + width, y + height, 0xDD0B0F19);
        context.fill(modalX, modalY, modalX + modalW, modalY + modalH, 0xFF141820);
        RenderHelper.drawBorder(context, modalX, modalY, modalW, modalH, 0xFF38BDF8);

        Text title = Text.translatable("profiles.itemorganizer.rename.title", renamingProfileName);
        TextScaleHelper.drawCenteredScaledText(context, tr, title, modalX + modalW / 2, modalY + 8, 0xFF38BDF8, textScale);

        context.fill(renameField.getX() - 1, renameField.getY() - 1, renameField.getX() + renameField.getWidth() + 1, renameField.getY() + renameField.getHeight() + 1, 0xFF0B0F19);
        RenderHelper.drawBorder(context, renameField.getX() - 1, renameField.getY() - 1, renameField.getWidth() + 2, renameField.getHeight() + 2, 0x33FFFFFF);
        renameField.renderWidget(context, mouseX, mouseY, delta);

        if (!renameErrorMessage.isEmpty()) {
            TextScaleHelper.drawCenteredScaledText(context, tr, renameErrorMessage, modalX + modalW / 2, modalY + 54, 0xFFEF4444, textScale);
        }

        int btnW = (modalW - 28) / 2;
        int btnH = 18;
        int btnY = modalY + modalH - 24;

        int saveBtnX = modalX + 10;
        boolean hoverSave = mouseX >= saveBtnX && mouseX <= saveBtnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        context.fill(saveBtnX, btnY, saveBtnX + btnW, btnY + btnH, hoverSave ? 0x801E3A5F : 0x401E3A5F);
        RenderHelper.drawBorder(context, saveBtnX, btnY, btnW, btnH, hoverSave ? 0xFF38BDF8 : 0x8038BDF8);
        TextScaleHelper.drawCenteredScaledText(context, tr, Text.translatable("profiles.itemorganizer.save"), saveBtnX + btnW / 2, btnY + 5, 0xFFFFFFFF, textScale);

        int cancelBtnX = saveBtnX + btnW + 8;
        boolean hoverCancel = mouseX >= cancelBtnX && mouseX <= cancelBtnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        context.fill(cancelBtnX, btnY, cancelBtnX + btnW, btnY + btnH, hoverCancel ? 0x33FFFFFF : 0x1AFFFFFF);
        RenderHelper.drawBorder(context, cancelBtnX, btnY, btnW, btnH, hoverCancel ? 0x66FFFFFF : 0x33FFFFFF);
        TextScaleHelper.drawCenteredScaledText(context, tr, Text.translatable("button.itemorganizer.cancel"), cancelBtnX + btnW / 2, btnY + 5, 0xFFE2E8F0, textScale);
    }

    private void renderDeleteModal(DrawContext context, TextRenderer tr, int mouseX, int mouseY, float delta) {
        float textScale = viewModel.getConfig().getTextScale();
        int modalW = Math.min(220, width - 20);
        int modalH = 95;
        int modalX = x + (width - modalW) / 2;
        int modalY = y + (height - modalH) / 2;

        context.fill(x, y, x + width, y + height, 0xDD0B0F19);
        context.fill(modalX, modalY, modalX + modalW, modalY + modalH, 0xFF141820);
        RenderHelper.drawBorder(context, modalX, modalY, modalW, modalH, 0xFFEF4444);

        TextScaleHelper.drawCenteredScaledText(context, tr, Text.translatable("profiles.itemorganizer.delete.title"), modalX + modalW / 2, modalY + 8, 0xFFEF4444, textScale);
        TextScaleHelper.drawCenteredScaledText(context, tr, "\"" + deletingProfileName + "\"", modalX + modalW / 2, modalY + 24, 0xFFFFFFFF, textScale);
        TextScaleHelper.drawCenteredScaledText(context, tr, Text.translatable("profiles.itemorganizer.delete.warning"), modalX + modalW / 2, modalY + 38, 0xFF94A3B8, textScale);

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

    private void playClickSound() {
        SoundHelper.playClick();
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        int mouseX = (int) click.x();
        int mouseY = (int) click.y();

        if (isCreating) {
            int modalW = Math.min(220, width - 20);
            int modalH = 120;
            int modalX = x + (width - modalW) / 2;
            int modalY = y + (height - modalH) / 2;

            if (createNameField.mouseClicked(click, bl)) {
                return true;
            }

            int toggleY = modalY + 54;
            int toggleH = 16;
            int toggleW = modalW - 20;
            int toggleX = modalX + 10;
            if (mouseX >= toggleX && mouseX <= toggleX + toggleW && mouseY >= toggleY && mouseY <= toggleY + toggleH) {
                cloneActive = !cloneActive;
                playClickSound();
                return true;
            }

            int btnW = (modalW - 28) / 2;
            int btnH = 18;
            int btnY = modalY + modalH - 24;

            int createBtnX = modalX + 10;
            if (mouseX >= createBtnX && mouseX <= createBtnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                executeCreate();
                return true;
            }

            int cancelBtnX = createBtnX + btnW + 8;
            if (mouseX >= cancelBtnX && mouseX <= cancelBtnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                isCreating = false;
                playClickSound();
                return true;
            }

            return true;
        }

        if (renamingProfileName != null) {
            int modalW = Math.min(220, width - 20);
            int modalH = 100;
            int modalX = x + (width - modalW) / 2;
            int modalY = y + (height - modalH) / 2;

            if (renameField.mouseClicked(click, bl)) {
                return true;
            }

            int btnW = (modalW - 28) / 2;
            int btnH = 18;
            int btnY = modalY + modalH - 24;

            int saveBtnX = modalX + 10;
            if (mouseX >= saveBtnX && mouseX <= saveBtnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                executeRename();
                return true;
            }

            int cancelBtnX = saveBtnX + btnW + 8;
            if (mouseX >= cancelBtnX && mouseX <= cancelBtnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                renamingProfileName = null;
                playClickSound();
                return true;
            }

            return true;
        }

        if (deletingProfileName != null) {
            int modalW = Math.min(220, width - 20);
            int modalH = 95;
            int modalX = x + (width - modalW) / 2;
            int modalY = y + (height - modalH) / 2;

            int btnW = (modalW - 28) / 2;
            int btnH = 18;
            int btnY = modalY + modalH - 24;

            int delBtnX = modalX + 10;
            if (mouseX >= delBtnX && mouseX <= delBtnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                executeDelete();
                return true;
            }

            int cancelBtnX = delBtnX + btnW + 8;
            if (mouseX >= cancelBtnX && mouseX <= cancelBtnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                deletingProfileName = null;
                playClickSound();
                return true;
            }

            return true;
        }

        float textScale = viewModel.getConfig().getTextScale();
        int addBtnW = Math.max(86, Math.round(86 * Math.max(1.0f, textScale)));
        int addBtnH = Math.max(18, Math.round(18 * Math.max(1.0f, textScale)));
        int addBtnX = x + width - addBtnW - 10;
        int addBtnY = y + 10;

        if (mouseX >= addBtnX && mouseX <= addBtnX + addBtnW && mouseY >= addBtnY && mouseY <= addBtnY + addBtnH) {
            isCreating = true;
            cloneActive = false;
            createErrorMessage = "";
            createNameField.setText("");
            createNameField.setFocused(true);
            playClickSound();
            return true;
        }

        if (scrollbar.mouseClicked(click)) {
            return true;
        }

        List<String> profiles = viewModel.getAvailableProfiles();
        ProfileData activeProfile = viewModel.getActiveProfile();
        String activeName = (activeProfile != null) ? activeProfile.getName() : "";

        int rowHeight = Math.max(24, Math.round(ROW_HEIGHT * Math.max(1.0f, textScale)));
        int btnH = Math.max(16, Math.round(BTN_H * Math.max(1.0f, textScale)));
        int listStartY = y + addBtnH + 18;
        int rowStartX = x + 8;
        int rowW = width - SCROLLBAR_WIDTH - 20;

        for (int i = 0; i < profiles.size(); i++) {
            String profileName = profiles.get(i);
            boolean isActive = profileName.equals(activeName);
            boolean isDefault = profileName.equalsIgnoreCase(ProfileRepository.DEFAULT_PROFILE_NAME);

            int rowY = listStartY + (i * (rowHeight + ROW_GAP)) - (int) scrollbar.getScrollOffset();
            int btnY = rowY + (rowHeight - btnH) / 2;
            int rightX = rowStartX + rowW - 4;

            // delete
            int delBtnX = rightX - BTN_DELETE_W;
            if (!isDefault && mouseX >= delBtnX && mouseX <= delBtnX + BTN_DELETE_W && mouseY >= btnY && mouseY <= btnY + BTN_H) {
                deletingProfileName = profileName;
                playClickSound();
                return true;
            }

            // rename
            int renBtnX = delBtnX - BTN_RENAME_W - 3;
            if (!isDefault && mouseX >= renBtnX && mouseX <= renBtnX + BTN_RENAME_W && mouseY >= btnY && mouseY <= btnY + BTN_H) {
                renamingProfileName = profileName;
                renameErrorMessage = "";
                renameField.setText(profileName);
                renameField.setFocused(true);
                playClickSound();
                return true;
            }

            // load
            int loadBtnX = renBtnX - BTN_LOAD_W - 3;
            if (!isActive && mouseX >= loadBtnX && mouseX <= loadBtnX + BTN_LOAD_W && mouseY >= btnY && mouseY <= btnY + BTN_H) {
                viewModel.loadProfileByName(profileName);
                playClickSound();
                return true;
            }
        }

        return false;
    }

    private void executeCreate() {
        String name = createNameField.getText().trim();
        if (name.isEmpty()) {
            createErrorMessage = Text.translatable("profiles.itemorganizer.error.empty").getString();
            return;
        }
        if (!name.matches("[a-zA-Z0-9_\\-\\s]+")) {
            createErrorMessage = Text.translatable("profiles.itemorganizer.error.invalid_chars").getString();
            return;
        }
        if (viewModel.getAvailableProfiles().contains(name)) {
            createErrorMessage = Text.translatable("profiles.itemorganizer.error.exists").getString();
            return;
        }

        boolean ok = viewModel.createNewProfile(name, cloneActive);
        if (ok) {
            isCreating = false;
            createErrorMessage = "";
            playClickSound();
        } else {
            createErrorMessage = Text.translatable("profiles.itemorganizer.error.save").getString();
        }
    }

    private void executeRename() {
        String newName = renameField.getText().trim();
        if (newName.isEmpty()) {
            renameErrorMessage = Text.translatable("profiles.itemorganizer.error.empty").getString();
            return;
        }
        if (!newName.matches("[a-zA-Z0-9_\\-\\s]+")) {
            renameErrorMessage = Text.translatable("profiles.itemorganizer.error.invalid_chars").getString();
            return;
        }
        if (newName.equals(renamingProfileName)) {
            renamingProfileName = null;
            return;
        }
        if (viewModel.getAvailableProfiles().contains(newName)) {
            renameErrorMessage = Text.translatable("profiles.itemorganizer.error.exists").getString();
            return;
        }

        boolean ok = viewModel.renameProfile(renamingProfileName, newName);
        if (ok) {
            renamingProfileName = null;
            renameErrorMessage = "";
            playClickSound();
        } else {
            renameErrorMessage = Text.translatable("profiles.itemorganizer.error.rename").getString();
        }
    }

    private void executeDelete() {
        if (deletingProfileName != null) {
            viewModel.deleteProfileByName(deletingProfileName);
            deletingProfileName = null;
            playClickSound();
        }
    }

    @Override
    public boolean mouseReleased(Click click) {
        scrollbar.mouseReleased(click);
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (scrollbar.mouseDragged(click, deltaX, deltaY)) {
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isCreating || renamingProfileName != null || deletingProfileName != null) {
            return false;
        }
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            scrollbar.scroll(-verticalAmount * 18);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (isCreating) {
            if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
                isCreating = false;
                playClickSound();
                return true;
            }
            if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER) {
                executeCreate();
                return true;
            }
            return createNameField.keyPressed(input);
        }

        if (renamingProfileName != null) {
            if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
                renamingProfileName = null;
                playClickSound();
                return true;
            }
            if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER) {
                executeRename();
                return true;
            }
            return renameField.keyPressed(input);
        }

        if (deletingProfileName != null) {
            if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
                deletingProfileName = null;
                playClickSound();
                return true;
            }
            if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER) {
                executeDelete();
                return true;
            }
            return true;
        }

        return false;
    }

    public boolean charTyped(CharInput input) {
        if (isCreating) {
            return createNameField.charTyped(input);
        }
        if (renamingProfileName != null) {
            return renameField.charTyped(input);
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
        return isCreating || renamingProfileName != null;
    }
}
