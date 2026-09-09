package com.itemorganizer.gui.component;

import com.itemorganizer.gui.theme.UITheme;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

// reusable vertical scrollbar component for scrollable containers
public class ScrollbarComponent {
    private int x;
    private int y;
    private int width;
    private int height;

    private double scrollOffset = 0;
    private double maxScroll = 0;
    private boolean isDragging = false;
    private double dragStartY = 0;
    private double dragStartScroll = 0;

    public ScrollbarComponent(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void updateMaxScroll(double contentHeight, double visibleHeight) {
        this.maxScroll = Math.max(0, contentHeight - visibleHeight);
        this.scrollOffset = MathHelper.clamp(this.scrollOffset, 0, this.maxScroll);
    }

    public double getScrollOffset() {
        return scrollOffset;
    }

    public void setScrollOffset(double offset) {
        this.scrollOffset = MathHelper.clamp(offset, 0, maxScroll);
    }

    public double getMaxScroll() {
        return maxScroll;
    }

    public boolean isDragging() {
        return isDragging;
    }

    public void scroll(double amount) {
        setScrollOffset(this.scrollOffset + amount);
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public int getThumbHeight() {
        if (maxScroll <= 0) return height;
        double viewRatio = (double) height / (height + maxScroll);
        return Math.max(16, (int) (height * viewRatio));
    }

    public int getThumbY() {
        if (maxScroll <= 0) return y;
        int thumbHeight = getThumbHeight();
        double scrollRatio = scrollOffset / maxScroll;
        return y + (int) (scrollRatio * (height - thumbHeight));
    }

    public void render(DrawContext context, int mouseX, int mouseY) {
        if (maxScroll <= 0) return;

        // scrollbar track
        context.fill(x, y, x + width, y + height, UITheme.SCROLLBAR_TRACK);

        // scrollbar thumb
        int thumbY = getThumbY();
        int thumbHeight = getThumbHeight();
        boolean hovered = isMouseOver(mouseX, mouseY) || isDragging;
        int thumbColor = hovered ? UITheme.SCROLLBAR_THUMB_HOVER : UITheme.SCROLLBAR_THUMB;

        context.fill(x, thumbY, x + width, thumbY + thumbHeight, thumbColor);
    }

    public boolean mouseClicked(Click click) {
        if (maxScroll <= 0) return false;

        if (isMouseOver(click.x(), click.y())) {
            int thumbY = getThumbY();
            int thumbHeight = getThumbHeight();

            if (click.y() >= thumbY && click.y() <= thumbY + thumbHeight) {
                // start thumb drag
                isDragging = true;
                dragStartY = click.y();
                dragStartScroll = scrollOffset;
            } else {
                // track jump click
                double ratio = (click.y() - y - (thumbHeight / 2.0)) / (height - thumbHeight);
                setScrollOffset(ratio * maxScroll);
                isDragging = true;
                dragStartY = click.y();
                dragStartScroll = scrollOffset;
            }
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
        if (isDragging && maxScroll > 0) {
            int thumbHeight = getThumbHeight();
            int trackAvailable = height - thumbHeight;
            if (trackAvailable > 0) {
                double deltaMove = click.y() - dragStartY;
                double scrollDelta = (deltaMove / trackAvailable) * maxScroll;
                setScrollOffset(dragStartScroll + scrollDelta);
            }
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (maxScroll > 0) {
            scroll(-verticalAmount * 24.0);
            return true;
        }
        return false;
    }
}
