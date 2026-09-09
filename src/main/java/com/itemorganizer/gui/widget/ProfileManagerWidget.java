package com.itemorganizer.gui.widget;

import com.itemorganizer.core.model.ItemSlotPosition;
import com.itemorganizer.core.model.ProfileData;
import com.itemorganizer.gui.component.ModalDialogComponent;
import com.itemorganizer.gui.component.ScrollbarComponent;
import com.itemorganizer.gui.theme.UITheme;
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

// widget managing custom sorting profiles with create, rename, and delete dialogs
public class ProfileManagerWidget implements Drawable, Element, Selectable {
    public static final int SCROLLBAR_WIDTH = 3;
    public static final int ROW_HEIGHT = 24;
    public static final int ROW_GAP = 3;
    public static final int BTN_DELETE_W = 18;
    public static final int BTN_RENAME_W = 18;
    public static final int BTN_LOAD_W = 44;
    public static final int BTN_H = 16;

    private final OrganizerViewModel viewModel;
    private int x;
    private int y;
    private int width;
    private int height;

    private final ScrollbarComponent scrollbar;

    // modal dialog state
    private ModalDialogComponent activeModal = null;
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
        this.scrollbar = new ScrollbarComponent(x + width - SCROLLBAR_WIDTH - 4, listStartY, SCROLLBAR_WIDTH, listHeight);

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

        boolean modalActive = activeModal != null;

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
                int delBg = hoverDel ? UITheme.DANGER_HOVER_BG : UITheme.DANGER_BG;
                int delBorder = hoverDel ? UITheme.DANGER : UITheme.DANGER_BORDER_MUTED;
                int delColor = hoverDel ? UITheme.TEXT_WHITE : 0xFFFCA5A5;
                RenderHelper.drawCard(context, delBtnX, btnY, BTN_DELETE_W, btnH, delBg, delBorder);
                RenderHelper.drawDeleteIcon(context, delBtnX + BTN_DELETE_W / 2.0f, btnY + btnH / 2.0f, textScale, delColor);
                if (hoverDel) hoveredTooltip = Text.translatable("profiles.itemorganizer.tooltip.delete");
            } else {
                RenderHelper.drawCard(context, delBtnX, btnY, BTN_DELETE_W, btnH, 0x0DFFFFFF, 0x14FFFFFF);
                RenderHelper.drawDeleteIcon(context, delBtnX + BTN_DELETE_W / 2.0f, btnY + btnH / 2.0f, textScale, UITheme.TEXT_HINT);
            }

            // rename button
            int renBtnX = delBtnX - BTN_RENAME_W - 3;
            boolean hoverRen = !modalActive && !isDefault && mouseX >= renBtnX && mouseX <= renBtnX + BTN_RENAME_W && mouseY >= btnY && mouseY <= btnY + btnH;
            if (!isDefault) {
                int renBg = hoverRen ? UITheme.PRIMARY_HOVER_BG : UITheme.PRIMARY_BG;
                int renBorder = hoverRen ? UITheme.PRIMARY : UITheme.PRIMARY_BORDER_MUTED;
                int renColor = hoverRen ? UITheme.TEXT_WHITE : 0xFFBAE6FD;
                RenderHelper.drawCard(context, renBtnX, btnY, BTN_RENAME_W, btnH, renBg, renBorder);
                RenderHelper.drawEditIcon(context, renBtnX + BTN_RENAME_W / 2.0f, btnY + btnH / 2.0f, textScale, renColor);
                if (hoverRen) hoveredTooltip = Text.translatable("profiles.itemorganizer.tooltip.rename");
            } else {
                RenderHelper.drawCard(context, renBtnX, btnY, BTN_RENAME_W, btnH, 0x0DFFFFFF, 0x14FFFFFF);
                RenderHelper.drawEditIcon(context, renBtnX + BTN_RENAME_W / 2.0f, btnY + btnH / 2.0f, textScale, UITheme.TEXT_HINT);
            }

            // load button
            int loadBtnX = renBtnX - BTN_LOAD_W - 3;
            boolean hoverLoad = !modalActive && !isActive && mouseX >= loadBtnX && mouseX <= loadBtnX + BTN_LOAD_W && mouseY >= btnY && mouseY <= btnY + btnH;
            if (!isActive) {
                RenderHelper.drawButton(context, tr, loadBtnX, btnY, BTN_LOAD_W, btnH,
                        Text.translatable("profiles.itemorganizer.load"), hoverLoad,
                        UITheme.SUCCESS_BG, UITheme.SUCCESS_HOVER_BG, UITheme.SUCCESS_BORDER_MUTED, UITheme.SUCCESS,
                        0xFFA7F3D0, UITheme.TEXT_WHITE, textScale);
                if (hoverLoad) hoveredTooltip = Text.translatable("profiles.itemorganizer.tooltip.load");
            } else {
                RenderHelper.drawCard(context, loadBtnX, btnY, BTN_LOAD_W, btnH, 0x1A10B981, 0x4034D399);
                TextScaleHelper.drawVerticallyCenteredScaledText(context, tr, Text.translatable("profiles.itemorganizer.loaded"), loadBtnX + BTN_LOAD_W / 2, btnY + btnH / 2, 0x8034D399, textScale);
            }
        }

        context.disableScissor();

        scrollbar.render(context, mouseX, mouseY);

        // modal rendering
        if (activeModal != null) {
            activeModal.updateParentBounds(x, y, width, height);
            activeModal.render(context, tr, mouseX, mouseY, delta, textScale);
        }

        // tooltip rendering
        if (hoveredTooltip != null && !modalActive) {
            context.drawTooltip(tr, hoveredTooltip, mouseX, mouseY);
        }
    }

    private void openCreateModal() {
        cloneActive = false;
        createErrorMessage = "";
        createNameField.setText("");
        createNameField.setFocused(true);

        activeModal = ModalDialogComponent.builder()
                .parentBounds(x, y, width, height)
                .size(Math.min(220, width - 20), 120)
                .type(ModalDialogComponent.ModalType.SUCCESS)
                .title(Text.translatable("profiles.itemorganizer.create.title"))
                .customContent((ctx, tr1, cx, cy, cw, ch, mx, my, dt, ts) -> {
                    createNameField.setX(cx);
                    createNameField.setY(cy);
                    createNameField.setWidth(cw);
                    ctx.fill(createNameField.getX() - 1, createNameField.getY() - 1, createNameField.getX() + createNameField.getWidth() + 1, createNameField.getY() + createNameField.getHeight() + 1, UITheme.BG_INPUT);
                    RenderHelper.drawBorder(ctx, createNameField.getX() - 1, createNameField.getY() - 1, createNameField.getWidth() + 2, createNameField.getHeight() + 2, UITheme.BORDER_SUBTLE);
                    createNameField.renderWidget(ctx, mx, my, dt);

                    int toggleY = cy + 24;
                    int toggleH = 16;
                    boolean hoverToggle = mx >= cx && mx <= cx + cw && my >= toggleY && my <= toggleY + toggleH;
                    Text modeText = cloneActive ? Text.translatable("profiles.itemorganizer.create.clone") : Text.translatable("profiles.itemorganizer.create.empty");
                    ctx.fill(cx, toggleY, cx + cw, toggleY + toggleH, hoverToggle ? 0x401E293B : 0x2A1E293B);
                    RenderHelper.drawBorder(ctx, cx, toggleY, cw, toggleH, hoverToggle ? UITheme.PRIMARY_BORDER : 0x4D38BDF8);
                    TextScaleHelper.drawCenteredScaledText(ctx, tr1, modeText, cx + cw / 2, toggleY + 4, UITheme.TEXT_PRIMARY, ts);

                    if (!createErrorMessage.isEmpty()) {
                        TextScaleHelper.drawCenteredScaledText(ctx, tr1, createErrorMessage, cx + cw / 2, toggleY + 20, UITheme.DANGER, ts);
                    }
                })
                .customClickHandler((click1, cx, cy, cw, ch) -> {
                    if (createNameField.mouseClicked(click1, false)) {
                        return true;
                    }
                    int toggleY = cy + 24;
                    int toggleH = 16;
                    if (click1.button() == 0 && click1.x() >= cx && click1.x() <= cx + cw && click1.y() >= toggleY && click1.y() <= toggleY + toggleH) {
                        cloneActive = !cloneActive;
                        playClickSound();
                        return true;
                    }
                    return false;
                })
                .customKeyHandler(input -> createNameField.keyPressed(input))
                .customCharHandler(input -> createNameField.charTyped(input))
                .confirmButton(Text.translatable("profiles.itemorganizer.create.btn"), this::executeCreate)
                .cancelButton(Text.translatable("button.itemorganizer.cancel"), () -> activeModal = null)
                .build();
    }

    private void openRenameModal(String profileName) {
        renamingProfileName = profileName;
        renameErrorMessage = "";
        renameField.setText(profileName);
        renameField.setFocused(true);

        activeModal = ModalDialogComponent.builder()
                .parentBounds(x, y, width, height)
                .size(Math.min(220, width - 20), 100)
                .type(ModalDialogComponent.ModalType.INFO)
                .title(Text.translatable("profiles.itemorganizer.rename.title", profileName))
                .customContent((ctx, tr1, cx, cy, cw, ch, mx, my, dt, ts) -> {
                    renameField.setX(cx);
                    renameField.setY(cy);
                    renameField.setWidth(cw);
                    ctx.fill(renameField.getX() - 1, renameField.getY() - 1, renameField.getX() + renameField.getWidth() + 1, renameField.getY() + renameField.getHeight() + 1, UITheme.BG_INPUT);
                    RenderHelper.drawBorder(ctx, renameField.getX() - 1, renameField.getY() - 1, renameField.getWidth() + 2, renameField.getHeight() + 2, UITheme.BORDER_SUBTLE);
                    renameField.renderWidget(ctx, mx, my, dt);

                    if (!renameErrorMessage.isEmpty()) {
                        TextScaleHelper.drawCenteredScaledText(ctx, tr1, renameErrorMessage, cx + cw / 2, cy + 22, UITheme.DANGER, ts);
                    }
                })
                .customClickHandler((click1, cx, cy, cw, ch) -> renameField.mouseClicked(click1, false))
                .customKeyHandler(input -> renameField.keyPressed(input))
                .customCharHandler(input -> renameField.charTyped(input))
                .confirmButton(Text.translatable("profiles.itemorganizer.save"), this::executeRename)
                .cancelButton(Text.translatable("button.itemorganizer.cancel"), () -> {
                    renamingProfileName = null;
                    activeModal = null;
                })
                .build();
    }

    private void openDeleteModal(String profileName) {
        deletingProfileName = profileName;

        activeModal = ModalDialogComponent.builder()
                .parentBounds(x, y, width, height)
                .size(Math.min(220, width - 20), 95)
                .type(ModalDialogComponent.ModalType.DANGER)
                .title(Text.translatable("profiles.itemorganizer.delete.title"))
                .message(Text.literal("\"" + profileName + "\"\n").append(Text.translatable("profiles.itemorganizer.delete.warning")))
                .confirmButton(Text.translatable("profiles.itemorganizer.delete.btn"), this::executeDelete)
                .cancelButton(Text.translatable("button.itemorganizer.cancel"), () -> {
                    deletingProfileName = null;
                    activeModal = null;
                })
                .build();
    }

    private void playClickSound() {
        SoundHelper.playClick();
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        if (activeModal != null) {
            return activeModal.mouseClicked(click);
        }

        int mouseX = (int) click.x();
        int mouseY = (int) click.y();

        float textScale = viewModel.getConfig().getTextScale();
        int addBtnW = Math.max(86, Math.round(86 * Math.max(1.0f, textScale)));
        int addBtnH = Math.max(18, Math.round(18 * Math.max(1.0f, textScale)));
        int addBtnX = x + width - addBtnW - 10;
        int addBtnY = y + 10;

        if (click.button() == 0 && mouseX >= addBtnX && mouseX <= addBtnX + addBtnW && mouseY >= addBtnY && mouseY <= addBtnY + addBtnH) {
            openCreateModal();
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
                openDeleteModal(profileName);
                playClickSound();
                return true;
            }

            // rename
            int renBtnX = delBtnX - BTN_RENAME_W - 3;
            if (!isDefault && mouseX >= renBtnX && mouseX <= renBtnX + BTN_RENAME_W && mouseY >= btnY && mouseY <= btnY + BTN_H) {
                openRenameModal(profileName);
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
            activeModal = null;
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
            activeModal = null;
            return;
        }
        if (viewModel.getAvailableProfiles().contains(newName)) {
            renameErrorMessage = Text.translatable("profiles.itemorganizer.error.exists").getString();
            return;
        }

        boolean ok = viewModel.renameProfile(renamingProfileName, newName);
        if (ok) {
            renamingProfileName = null;
            activeModal = null;
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
            activeModal = null;
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
        if (activeModal != null) {
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
        if (activeModal != null) {
            return activeModal.keyPressed(input);
        }
        return false;
    }

    public boolean charTyped(CharInput input) {
        if (activeModal != null) {
            return activeModal.charTyped(input);
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
        return activeModal != null;
    }
}
