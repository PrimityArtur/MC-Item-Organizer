package com.itemorganizer.gui.component;

import com.itemorganizer.gui.theme.UITheme;
import com.itemorganizer.gui.util.RenderHelper;
import com.itemorganizer.gui.util.TextScaleHelper;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

// reusable slider component for numeric configuration values
public class SliderComponent {
    private int x;
    private int y;
    private int width;
    private int height;

    private Text label;
    private double min;
    private double max;
    private Supplier<Double> valueGetter;
    private Consumer<Double> valueSetter;
    private Function<Double, String> valueFormatter;

    private int trackColor = 0x40000000;
    private int activeTrackColor = UITheme.PRIMARY;
    private int borderColor = UITheme.BORDER_SUBTLE;
    private int thumbColor = UITheme.TEXT_PRIMARY;

    private boolean isDragging = false;

    public SliderComponent(Text label, double min, double max,
                           Supplier<Double> valueGetter, Consumer<Double> valueSetter,
                           Function<Double, String> valueFormatter) {
        this(0, 0, 100, 14, label, min, max, valueGetter, valueSetter, valueFormatter);
    }

    public SliderComponent(int x, int y, int width, int height,
                           Text label, double min, double max,
                           Supplier<Double> valueGetter, Consumer<Double> valueSetter,
                           Function<Double, String> valueFormatter) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.label = label;
        this.min = min;
        this.max = max;
        this.valueGetter = valueGetter;
        this.valueSetter = valueSetter;
        this.valueFormatter = valueFormatter;
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setColors(int trackColor, int activeTrackColor, int borderColor, int thumbColor) {
        this.trackColor = trackColor;
        this.activeTrackColor = activeTrackColor;
        this.borderColor = borderColor;
        this.thumbColor = thumbColor;
    }

    public double getValue() {
        return valueGetter != null ? valueGetter.get() : min;
    }

    public void setValue(double val) {
        double clamped = MathHelper.clamp(val, min, max);
        if (valueSetter != null) {
            valueSetter.accept(clamped);
        }
    }

    public boolean isDragging() {
        return isDragging;
    }

    public void stopDragging() {
        this.isDragging = false;
    }

    public void render(DrawContext context, TextRenderer tr, int mouseX, int mouseY, float textScale) {
        double val = getValue();
        double ratio = (max > min) ? (val - min) / (max - min) : 0;
        ratio = MathHelper.clamp(ratio, 0.0, 1.0);

        // draw track background and border
        RenderHelper.drawCard(context, x, y, width, height, trackColor, borderColor);

        // filled active progress portion
        int fillW = (int) Math.round(width * ratio);
        if (fillW > 0) {
            context.fill(x + 1, y + 1, x + fillW, y + height - 1, activeTrackColor);
        }

        // thumb indicator
        int thumbW = 6;
        int thumbX = MathHelper.clamp(x + fillW - (thumbW / 2), x + 1, x + width - thumbW - 1);
        boolean hover = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        int effThumbColor = (hover || isDragging) ? UITheme.TEXT_WHITE : thumbColor;
        context.fill(thumbX, y + 1, thumbX + thumbW, y + height - 1, effThumbColor);
        RenderHelper.drawBorder(context, thumbX, y + 1, thumbW, height - 2, (hover || isDragging) ? UITheme.PRIMARY_BORDER : UITheme.BORDER_SUBTLE);

        // label and formatted value text
        if (tr != null) {
            String valStr = (valueFormatter != null) ? valueFormatter.apply(val) : String.format("%.2f", val);
            Text fullText;
            if (label != null && !label.getString().isEmpty()) {
                fullText = Text.literal("").append(label).append(": ").append(valStr);
            } else {
                fullText = Text.literal(valStr);
            }
            TextScaleHelper.drawVerticallyCenteredScaledText(context, tr, fullText, x + width / 2, y + height / 2, UITheme.TEXT_WHITE, textScale);
        }
    }

    public boolean mouseClicked(Click click) {
        if (click.button() == 0 && isMouseOver(click.x(), click.y())) {
            isDragging = true;
            updateFromMouse(click.x());
            return true;
        }
        return false;
    }

    public boolean mouseReleased(Click click) {
        if (isDragging) {
            isDragging = false;
            return true;
        }
        return false;
    }

    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (isDragging) {
            updateFromMouse(click.x());
            return true;
        }
        return false;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private void updateFromMouse(double mouseX) {
        double relativeX = mouseX - x;
        double ratio = (width > 0) ? relativeX / (double) width : 0;
        ratio = MathHelper.clamp(ratio, 0.0, 1.0);
        double newVal = min + ratio * (max - min);
        setValue(newVal);
    }
}
