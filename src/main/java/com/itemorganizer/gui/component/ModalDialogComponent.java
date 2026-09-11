package com.itemorganizer.gui.component;

import com.itemorganizer.gui.theme.UITheme;
import com.itemorganizer.gui.util.RenderHelper;
import com.itemorganizer.gui.util.SoundHelper;
import com.itemorganizer.gui.util.TextScaleHelper;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

// configurable floating modal dialog component for confirmations and inputs
public class ModalDialogComponent {

    public enum ModalType {
        INFO(UITheme.PRIMARY, UITheme.PRIMARY_BG, UITheme.PRIMARY_HOVER_BG, UITheme.PRIMARY_BORDER, UITheme.PRIMARY_BORDER),
        SUCCESS(UITheme.SUCCESS, UITheme.SUCCESS_BG, UITheme.SUCCESS_HOVER_BG, UITheme.SUCCESS_BORDER, UITheme.SUCCESS_BORDER),
        WARNING(UITheme.WARNING, UITheme.WARNING_BG, UITheme.WARNING_HOVER_BG, UITheme.WARNING_BORDER, UITheme.WARNING_BORDER),
        DANGER(UITheme.DANGER, UITheme.DANGER_BG, UITheme.DANGER_HOVER_BG, UITheme.DANGER_BORDER, UITheme.DANGER_BORDER);

        private final int accentColor;
        private final int confirmBg;
        private final int confirmHoverBg;
        private final int confirmBorder;
        private final int confirmHoverBorder;

        ModalType(int accentColor, int confirmBg, int confirmHoverBg, int confirmBorder, int confirmHoverBorder) {
            this.accentColor = accentColor;
            this.confirmBg = confirmBg;
            this.confirmHoverBg = confirmHoverBg;
            this.confirmBorder = confirmBorder;
            this.confirmHoverBorder = confirmHoverBorder;
        }

        public int getAccentColor() {
            return accentColor;
        }

        public int getConfirmBg() {
            return confirmBg;
        }

        public int getConfirmHoverBg() {
            return confirmHoverBg;
        }

        public int getConfirmBorder() {
            return confirmBorder;
        }

        public int getConfirmHoverBorder() {
            return confirmHoverBorder;
        }
    }

    @FunctionalInterface
    public interface CustomContentRenderer {
        void render(DrawContext context, TextRenderer tr, int contentX, int contentY, int contentW, int contentH, int mouseX, int mouseY, float delta, float textScale);
    }

    @FunctionalInterface
    public interface CustomClickHandler {
        boolean mouseClicked(Click click, int contentX, int contentY, int contentW, int contentH);
    }

    @FunctionalInterface
    public interface CustomKeyHandler {
        boolean keyPressed(KeyInput input);
    }

    @FunctionalInterface
    public interface CustomCharHandler {
        boolean charTyped(CharInput input);
    }

    // geometry and bounds
    private int parentX;
    private int parentY;
    private int parentWidth;
    private int parentHeight;
    private int customX = -1;
    private int customY = -1;
    private int width;
    private int height;

    // visual attributes
    private ModalType type = ModalType.INFO;
    private int backdropColor = UITheme.BG_MODAL_BACKDROP;
    private int backgroundColor = UITheme.BG_CARD;
    private int strokeColor = 0;
    private int shadowSize = 4;
    private int shadowColor = 0x66000000;

    // text content
    private Text title;
    private int titleColor = 0;
    private Text message;
    private int messageColor = UITheme.TEXT_SECONDARY;
    private Text warning;
    private int warningColor = UITheme.TEXT_MUTED;

    // action buttons
    private boolean showConfirmButton = true;
    private Text confirmText;
    private int confirmBg = 0;
    private int confirmHoverBg = 0;
    private int confirmBorder = 0;
    private int confirmHoverBorder = 0;
    private int confirmTextColor = UITheme.TEXT_PRIMARY;
    private Runnable onConfirm;

    private boolean showCancelButton = true;
    private Text cancelText;
    private int cancelBg = 0x1AFFFFFF;
    private int cancelHoverBg = 0x33FFFFFF;
    private int cancelBorder = 0x33FFFFFF;
    private int cancelHoverBorder = 0x66FFFFFF;
    private int cancelTextColor = UITheme.TEXT_SECONDARY;
    private Runnable onCancel;

    private boolean closeOnBackdropClick = false;

    // custom content extensions
    private CustomContentRenderer customRenderer;
    private CustomClickHandler customClickHandler;
    private CustomKeyHandler customKeyHandler;
    private CustomCharHandler customCharHandler;

    private ModalDialogComponent() {}

    public static Builder builder() {
        return new Builder();
    }

    public int getX() {
        if (customX >= 0) {
            return customX;
        }
        int effectiveParentW = parentWidth > 0 ? parentWidth : width;
        return parentX + (effectiveParentW - width) / 2;
    }

    public int getY() {
        if (customY >= 0) {
            return customY;
        }
        int effectiveParentH = parentHeight > 0 ? parentHeight : height;
        return parentY + (effectiveParentH - height) / 2;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void updateParentBounds(int px, int py, int pw, int ph) {
        this.parentX = px;
        this.parentY = py;
        this.parentWidth = pw;
        this.parentHeight = ph;
    }

    public ModalType getType() {
        return type;
    }

    public Text getTitle() {
        return title;
    }

    public Text getMessage() {
        return message;
    }

    public Text getWarning() {
        return warning;
    }

    public Text getConfirmText() {
        return confirmText;
    }

    public Text getCancelText() {
        return cancelText;
    }

    public boolean isCloseOnBackdropClick() {
        return closeOnBackdropClick;
    }

    public int getBackdropColor() {
        return backdropColor;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public int getStrokeColor() {
        return strokeColor;
    }

    public int getShadowSize() {
        return shadowSize;
    }

    public int getShadowColor() {
        return shadowColor;
    }

    public void triggerConfirm() {
        executeConfirm();
    }

    public void triggerCancel() {
        executeCancel();
    }

    public void render(DrawContext context, TextRenderer tr, int mouseX, int mouseY, float delta, float textScale) {
        int modalX = getX();
        int modalY = getY();

        // draw backdrop overlay covering parent area
        if ((backdropColor >>> 24) != 0) {
            int bgW = parentWidth > 0 ? parentWidth : width;
            int bgH = parentHeight > 0 ? parentHeight : height;
            context.fill(parentX, parentY, parentX + bgW, parentY + bgH, backdropColor);
        }

        // draw card with drop shadow
        int effectiveBorder = strokeColor != 0 ? strokeColor : type.getAccentColor();
        RenderHelper.drawCardWithShadow(context, modalX, modalY, width, height, backgroundColor, effectiveBorder, shadowSize, shadowColor);

        int currentY = modalY + 8;

        // title
        if (title != null) {
            int effTitleColor = titleColor != 0 ? titleColor : effectiveBorder;
            TextScaleHelper.drawCenteredScaledText(context, tr, title, modalX + width / 2, currentY, effTitleColor, textScale);
            currentY += Math.round(14 * textScale);
        }

        // message
        if (message != null) {
            TextScaleHelper.drawCenteredScaledText(context, tr, message, modalX + width / 2, currentY, messageColor, textScale * 0.92f);
            currentY += Math.round(14 * textScale);
        }

        // optional warning note
        if (warning != null) {
            TextScaleHelper.drawCenteredScaledText(context, tr, warning, modalX + width / 2, currentY, warningColor, textScale * 0.85f);
            currentY += Math.round(12 * textScale);
        }

        // custom content slot
        if (customRenderer != null) {
            int contentX = modalX + 10;
            int contentW = width - 20;
            int btnAreaHeight = (showConfirmButton || showCancelButton) ? 26 : 4;
            int contentH = (modalY + height - btnAreaHeight) - currentY;
            if (contentH > 0) {
                customRenderer.render(context, tr, contentX, currentY, contentW, contentH, mouseX, mouseY, delta, textScale);
            }
        }

        // action buttons
        renderButtons(context, tr, modalX, modalY, mouseX, mouseY, textScale);
    }

    private void renderButtons(DrawContext context, TextRenderer tr, int modalX, int modalY, int mouseX, int mouseY, float textScale) {
        if (!showConfirmButton && !showCancelButton) return;

        int btnH = 18;
        int btnY = modalY + height - btnH - 6;

        if (showConfirmButton && showCancelButton) {
            int btnW = (width - 28) / 2;
            int confirmX = modalX + 10;
            int cancelX = confirmX + btnW + 8;

            boolean hoverConfirm = mouseX >= confirmX && mouseX <= confirmX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
            int effConfirmBg = confirmBg != 0 ? confirmBg : type.getConfirmBg();
            int effConfirmHoverBg = confirmHoverBg != 0 ? confirmHoverBg : type.getConfirmHoverBg();
            int effConfirmBorder = confirmBorder != 0 ? confirmBorder : type.getConfirmBorder();
            int effConfirmHoverBorder = confirmHoverBorder != 0 ? confirmHoverBorder : type.getConfirmHoverBorder();

            RenderHelper.drawButton(context, tr, confirmX, btnY, btnW, btnH, confirmText, hoverConfirm,
                    effConfirmBg, effConfirmHoverBg, effConfirmBorder, effConfirmHoverBorder,
                    confirmTextColor, UITheme.TEXT_PRIMARY, textScale);

            boolean hoverCancel = mouseX >= cancelX && mouseX <= cancelX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
            RenderHelper.drawButton(context, tr, cancelX, btnY, btnW, btnH, cancelText, hoverCancel,
                    cancelBg, cancelHoverBg, cancelBorder, cancelHoverBorder,
                    cancelTextColor, UITheme.TEXT_PRIMARY, textScale);
        } else if (showConfirmButton) {
            int btnW = width - 20;
            int confirmX = modalX + 10;
            boolean hoverConfirm = mouseX >= confirmX && mouseX <= confirmX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
            int effConfirmBg = confirmBg != 0 ? confirmBg : type.getConfirmBg();
            int effConfirmHoverBg = confirmHoverBg != 0 ? confirmHoverBg : type.getConfirmHoverBg();
            int effConfirmBorder = confirmBorder != 0 ? confirmBorder : type.getConfirmBorder();
            int effConfirmHoverBorder = confirmHoverBorder != 0 ? confirmHoverBorder : type.getConfirmHoverBorder();

            RenderHelper.drawButton(context, tr, confirmX, btnY, btnW, btnH, confirmText, hoverConfirm,
                    effConfirmBg, effConfirmHoverBg, effConfirmBorder, effConfirmHoverBorder,
                    confirmTextColor, UITheme.TEXT_PRIMARY, textScale);
        } else {
            int btnW = width - 20;
            int cancelX = modalX + 10;
            boolean hoverCancel = mouseX >= cancelX && mouseX <= cancelX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
            RenderHelper.drawButton(context, tr, cancelX, btnY, btnW, btnH, cancelText, hoverCancel,
                    cancelBg, cancelHoverBg, cancelBorder, cancelHoverBorder,
                    cancelTextColor, UITheme.TEXT_PRIMARY, textScale);
        }
    }

    public boolean mouseClicked(Click click) {
        int modalX = getX();
        int modalY = getY();
        double mx = click.x();
        double my = click.y();

        // custom click handling inside content area
        if (customClickHandler != null) {
            int contentX = modalX + 10;
            int contentW = width - 20;
            int btnAreaHeight = (showConfirmButton || showCancelButton) ? 26 : 4;
            int contentY = modalY + 28;
            int contentH = (modalY + height - btnAreaHeight) - contentY;
            if (customClickHandler.mouseClicked(click, contentX, contentY, contentW, contentH)) {
                return true;
            }
        }

        if (click.button() == 0) {
            int btnH = 18;
            int btnY = modalY + height - btnH - 6;

            if (showConfirmButton && showCancelButton) {
                int btnW = (width - 28) / 2;
                int confirmX = modalX + 10;
                int cancelX = confirmX + btnW + 8;

                if (mx >= confirmX && mx <= confirmX + btnW && my >= btnY && my <= btnY + btnH) {
                    executeConfirm();
                    return true;
                }
                if (mx >= cancelX && mx <= cancelX + btnW && my >= btnY && my <= btnY + btnH) {
                    executeCancel();
                    return true;
                }
            } else if (showConfirmButton) {
                int btnW = width - 20;
                int confirmX = modalX + 10;
                if (mx >= confirmX && mx <= confirmX + btnW && my >= btnY && my <= btnY + btnH) {
                    executeConfirm();
                    return true;
                }
            } else if (showCancelButton) {
                int btnW = width - 20;
                int cancelX = modalX + 10;
                if (mx >= cancelX && mx <= cancelX + btnW && my >= btnY && my <= btnY + btnH) {
                    executeCancel();
                    return true;
                }
            }

            // backdrop click
            boolean insideCard = mx >= modalX && mx <= modalX + width && my >= modalY && my <= modalY + height;
            if (!insideCard && closeOnBackdropClick) {
                executeCancel();
                return true;
            }
        }

        // absorb all clicks so background elements are not activated while modal is open
        return true;
    }

    public boolean keyPressed(KeyInput input) {
        if (customKeyHandler != null && customKeyHandler.keyPressed(input)) {
            return true;
        }

        if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER) {
            if (showConfirmButton && onConfirm != null) {
                executeConfirm();
                return true;
            }
        }

        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            executeCancel();
            return true;
        }

        return true;
    }

    public boolean charTyped(CharInput input) {
        if (customCharHandler != null) {
            return customCharHandler.charTyped(input);
        }
        return false;
    }

    private void executeConfirm() {
        if (onConfirm != null) {
            onConfirm.run();
        }
    }

    private void executeCancel() {
        SoundHelper.playClick();
        if (onCancel != null) {
            onCancel.run();
        }
    }

    // builder class for modal creation
    public static class Builder {
        private final ModalDialogComponent modal = new ModalDialogComponent();

        public Builder parentBounds(int px, int py, int pw, int ph) {
            modal.parentX = px;
            modal.parentY = py;
            modal.parentWidth = pw;
            modal.parentHeight = ph;
            return this;
        }

        public Builder position(int x, int y) {
            modal.customX = x;
            modal.customY = y;
            return this;
        }

        public Builder size(int width, int height) {
            modal.width = width;
            modal.height = height;
            return this;
        }

        public Builder type(ModalType type) {
            modal.type = Objects.requireNonNull(type);
            return this;
        }

        public Builder backdropColor(int color) {
            modal.backdropColor = color;
            return this;
        }

        public Builder backgroundColor(int color) {
            modal.backgroundColor = color;
            return this;
        }

        public Builder strokeColor(int color) {
            modal.strokeColor = color;
            return this;
        }

        public Builder shadow(int size, int color) {
            modal.shadowSize = size;
            modal.shadowColor = color;
            return this;
        }

        public Builder title(Text title) {
            modal.title = title;
            return this;
        }

        public Builder titleColor(int color) {
            modal.titleColor = color;
            return this;
        }

        public Builder message(Text message) {
            modal.message = message;
            return this;
        }

        public Builder messageColor(int color) {
            modal.messageColor = color;
            return this;
        }

        public Builder warning(Text warning) {
            modal.warning = warning;
            return this;
        }

        public Builder warningColor(int color) {
            modal.warningColor = color;
            return this;
        }

        public Builder confirmButton(Text text, Runnable onConfirm) {
            modal.showConfirmButton = true;
            modal.confirmText = text;
            modal.onConfirm = onConfirm;
            return this;
        }

        public Builder confirmColors(int normalBg, int hoverBg, int normalBorder, int hoverBorder) {
            modal.confirmBg = normalBg;
            modal.confirmHoverBg = hoverBg;
            modal.confirmBorder = normalBorder;
            modal.confirmHoverBorder = hoverBorder;
            return this;
        }

        public Builder cancelButton(Text text, Runnable onCancel) {
            modal.showCancelButton = true;
            modal.cancelText = text;
            modal.onCancel = onCancel;
            return this;
        }

        public Builder closeOnBackdropClick(boolean close) {
            modal.closeOnBackdropClick = close;
            return this;
        }

        public Builder customContent(CustomContentRenderer renderer) {
            modal.customRenderer = renderer;
            return this;
        }

        public Builder customClickHandler(CustomClickHandler handler) {
            modal.customClickHandler = handler;
            return this;
        }

        public Builder customKeyHandler(CustomKeyHandler handler) {
            modal.customKeyHandler = handler;
            return this;
        }

        public Builder customCharHandler(CustomCharHandler handler) {
            modal.customCharHandler = handler;
            return this;
        }

        public ModalDialogComponent build() {
            if (modal.confirmText == null) {
                modal.confirmText = Text.translatable("button.itemorganizer.confirm");
            }
            if (modal.cancelText == null) {
                modal.cancelText = Text.translatable("button.itemorganizer.cancel");
            }
            if (modal.width <= 0) {
                modal.width = 240;
            }
            if (modal.height <= 0) {
                modal.height = 88;
            }
            return modal;
        }
    }
}
