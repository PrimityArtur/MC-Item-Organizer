package com.itemorganizer.gui.widget;

import com.itemorganizer.gui.util.RenderHelper;
import com.itemorganizer.gui.util.SoundHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.function.Supplier;

// navigation tab button with selected and hovered states
public class TabButtonWidget extends ClickableWidget {
    @FunctionalInterface
    public interface PressAction {
        void onPress(TabButtonWidget button);
    }

    private final PressAction onPress;
    private final Supplier<Boolean> isSelectedSupplier;
    private final boolean isToggleButton;

    public TabButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress, Supplier<Boolean> isSelectedSupplier) {
        this(x, y, width, height, message, onPress, isSelectedSupplier, false);
    }

    public TabButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress, Supplier<Boolean> isSelectedSupplier, boolean isToggleButton) {
        super(x, y, width, height, message);
        this.onPress = onPress;
        this.isSelectedSupplier = isSelectedSupplier;
        this.isToggleButton = isToggleButton;
    }

    public void setBounds(int x, int y, int width, int height) {
        setX(x);
        setY(y);
        setWidth(width);
        setHeight(height);
    }

    @Override
    public void onClick(Click click, boolean bl) {
        SoundHelper.playClick();
        if (this.onPress != null) {
            this.onPress.onPress(this);
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean selected = isSelectedSupplier != null && Boolean.TRUE.equals(isSelectedSupplier.get());
        boolean hovered = isHovered();

        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        int bgColor;
        int textColor;
        int borderColor;
        int indicatorColor = 0;

        if (isToggleButton && selected) {
            // active toggle
            bgColor = hovered ? 0x55991B1B : 0x447F1D1D;
            textColor = 0xFFFCA5A5;
            borderColor = hovered ? 0xFFFF6666 : 0xFFDC2626;
            indicatorColor = 0xFFEF4444;
        } else if (selected) {
            // active tab
            bgColor = hovered ? 0x4A1E293B : 0x381E293B;
            textColor = 0xFFFFFFFF;
            borderColor = hovered ? 0x6638BDF8 : 0x4038BDF8;
            indicatorColor = 0xFF38BDF8;
        } else {
            // inactive tab
            bgColor = hovered ? 0x2AFFFFFF : 0x12FFFFFF;
            textColor = hovered ? 0xFFFFFFFF : 0xFF94A3B8;
            borderColor = hovered ? 0x33FFFFFF : 0x1AFFFFFF;
        }

        // button background
        context.fill(x, y, x + w, y + h, bgColor);

        // border
        RenderHelper.drawBorder(context, x, y, w, h, borderColor);

        // active indicator line
        if (indicatorColor != 0) {
            context.fill(x + 2, y + h - 2, x + w - 2, y + h, indicatorColor);
        }

        // vertically centered scaled text
        MinecraftClient client = MinecraftClient.getInstance();
        float textScale = com.itemorganizer.client.ItemOrganizerClient.getSharedViewModel() != null ?
                com.itemorganizer.client.ItemOrganizerClient.getSharedViewModel().getConfig().getTextScale() : 1.0f;
        int centerX = x + w / 2;
        int centerY = y + (h - (indicatorColor != 0 ? 1 : 0)) / 2;
        com.itemorganizer.gui.util.TextScaleHelper.drawVerticallyCenteredScaledText(context, client.textRenderer, getMessage(), centerX, centerY, textColor, true, textScale);
    }
}
