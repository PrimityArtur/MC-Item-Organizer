package com.itemorganizer.gui.util;

import net.minecraft.client.gui.DrawContext;

// gui render helper for immediate bordered rectangles
public final class RenderHelper {
    private RenderHelper() {}

    // draws bordered rectangle with 1px stroke using immediate fill
    public static void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0 || (color >>> 24) == 0) return;

        if (width <= 2 || height <= 2) {
            context.fill(x, y, x + width, y + height, color);
            return;
        }

        // top border
        context.fill(x, y, x + width, y + 1, color);
        // bottom border
        context.fill(x, y + height - 1, x + width, y + height, color);
        // left border
        context.fill(x, y + 1, x + 1, y + height - 1, color);
        // right border
        context.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }
}
