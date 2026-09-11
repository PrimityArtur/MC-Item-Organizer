package com.itemorganizer.gui.util;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

// helper to render text scaled by custom factor
public final class TextScaleHelper {
    private TextScaleHelper() {}

    public static void drawScaledText(DrawContext context, TextRenderer tr, Text text, int x, int y, int color, boolean shadow, float textScale) {
        if (Math.abs(textScale - 1.0f) < 0.01f) {
            context.drawText(tr, text, x, y, color, shadow);
            return;
        }
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(textScale, textScale);
        context.drawText(tr, text, 0, 0, color, shadow);
        context.getMatrices().popMatrix();
    }

    public static void drawScaledText(DrawContext context, TextRenderer tr, Text text, int x, int y, int color, float textScale) {
        drawScaledText(context, tr, text, x, y, color, false, textScale);
    }

    public static void drawCenteredScaledText(DrawContext context, TextRenderer tr, Text text, int centerX, int y, int color, float textScale) {
        if (Math.abs(textScale - 1.0f) < 0.01f) {
            context.drawCenteredTextWithShadow(tr, text, centerX, y, color);
            return;
        }
        int w = tr.getWidth(text);
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(centerX, y);
        context.getMatrices().scale(textScale, textScale);
        context.drawText(tr, text, -w / 2, 0, color, true);
        context.getMatrices().popMatrix();
    }

    public static void drawScaledText(DrawContext context, TextRenderer tr, String text, int x, int y, int color, boolean shadow, float textScale) {
        drawScaledText(context, tr, Text.literal(text), x, y, color, shadow, textScale);
    }

    public static void drawCenteredScaledText(DrawContext context, TextRenderer tr, String text, int centerX, int y, int color, float textScale) {
        drawCenteredScaledText(context, tr, Text.literal(text), centerX, y, color, textScale);
    }

    public static void drawVerticallyCenteredScaledText(DrawContext context, TextRenderer tr, Text text, int centerX, int centerY, int color, float textScale) {
        drawVerticallyCenteredScaledText(context, tr, text, centerX, centerY, color, true, textScale);
    }

    public static void drawVerticallyCenteredScaledText(DrawContext context, TextRenderer tr, String text, int centerX, int centerY, int color, float textScale) {
        drawVerticallyCenteredScaledText(context, tr, Text.literal(text), centerX, centerY, color, true, textScale);
    }

    public static void drawVerticallyCenteredScaledText(DrawContext context, TextRenderer tr, Text text, int centerX, int centerY, int color, boolean shadow, float textScale) {
        if (Math.abs(textScale - 1.0f) < 0.01f) {
            int w = tr.getWidth(text);
            context.drawText(tr, text, centerX - w / 2, centerY - 4, color, shadow);
            return;
        }
        int w = tr.getWidth(text);
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(centerX, centerY);
        context.getMatrices().scale(textScale, textScale);
        context.drawText(tr, text, -w / 2, -4, color, shadow);
        context.getMatrices().popMatrix();
    }

    public static void drawVerticallyCenteredScaledText(DrawContext context, TextRenderer tr, String text, int centerX, int centerY, int color, boolean shadow, float textScale) {
        drawVerticallyCenteredScaledText(context, tr, Text.literal(text), centerX, centerY, color, shadow, textScale);
    }

    public static void drawLeftVerticallyCenteredScaledText(DrawContext context, TextRenderer tr, Text text, int x, int centerY, int color, boolean shadow, float textScale) {
        if (Math.abs(textScale - 1.0f) < 0.01f) {
            context.drawText(tr, text, x, centerY - 4, color, shadow);
            return;
        }
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, centerY);
        context.getMatrices().scale(textScale, textScale);
        context.drawText(tr, text, 0, -4, color, shadow);
        context.getMatrices().popMatrix();
    }

    public static void drawLeftVerticallyCenteredScaledText(DrawContext context, TextRenderer tr, String text, int x, int centerY, int color, boolean shadow, float textScale) {
        drawLeftVerticallyCenteredScaledText(context, tr, Text.literal(text), x, centerY, color, shadow, textScale);
    }
}
