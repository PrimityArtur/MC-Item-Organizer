package com.itemorganizer.gui.util;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// unified gui renderer for shapes, shadows, items, slots, and vector icons
public final class RenderHelper {
    private static final Map<String, ItemStack> ITEM_CACHE = new ConcurrentHashMap<>();

    private RenderHelper() {}

    // resolves item identifier string to itemstack with caching
    public static ItemStack getItemStack(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return ItemStack.EMPTY;
        }
        String key = itemId.trim();
        ItemStack cached = ITEM_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            String baseId = key;
            String properties = null;
            int bracketStart = key.indexOf('[');
            int bracketEnd = key.lastIndexOf(']');
            if (bracketStart != -1 && bracketEnd > bracketStart) {
                baseId = key.substring(0, bracketStart).trim();
                properties = key.substring(bracketStart + 1, bracketEnd).trim();
            }

            Identifier id = Identifier.tryParse(baseId);
            if (id != null) {
                Item item = Registries.ITEM.get(id);
                if (item != null && item != Items.AIR) {
                    ItemStack stack = new ItemStack(item);
                    if (properties != null) {
                        applyPropertiesToStack(stack, item, properties);
                    }
                    ITEM_CACHE.put(key, stack);
                    return stack;
                }
            }
        } catch (Throwable ignored) {
        }
        ITEM_CACHE.put(key, ItemStack.EMPTY);
        return ItemStack.EMPTY;
    }

    private static void applyPropertiesToStack(ItemStack stack, Item item, String properties) {
        try {
            if (item == Items.TEST_BLOCK) {
                for (net.minecraft.block.enums.TestBlockMode mode : net.minecraft.block.enums.TestBlockMode.values()) {
                    if (properties.contains(mode.asString())) {
                        net.minecraft.block.TestBlock.applyBlockStateToStack(stack, mode);
                        return;
                    }
                }
            }
            if (item == Items.LIGHT) {
                for (int level = 15; level >= 0; level--) {
                    if (properties.contains(String.valueOf(level))) {
                        net.minecraft.block.LightBlock.addNbtForLevel(stack, level);
                        return;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static void clearItemCache() {
        ITEM_CACHE.clear();
    }

    // draws bordered rectangle with 1px stroke using immediate fill
    public static void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0 || (color >>> 24) == 0) return;

        if (width <= 2 || height <= 2) {
            context.fill(x, y, x + width, y + height, color);
            return;
        }

        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y + 1, x + 1, y + height - 1, color);
        context.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    // draws card with background fill and border
    public static void drawCard(DrawContext context, int x, int y, int width, int height, int bgColor, int borderColor) {
        if (width <= 0 || height <= 0) return;
        if ((bgColor >>> 24) != 0) {
            context.fill(x, y, x + width, y + height, bgColor);
        }
        if ((borderColor >>> 24) != 0) {
            drawBorder(context, x, y, width, height, borderColor);
        }
    }

    // draws soft multi-layered drop shadow around bounds
    public static void drawShadow(DrawContext context, int x, int y, int width, int height, int shadowSize, int shadowColor) {
        if (shadowSize <= 0 || (shadowColor >>> 24) == 0) return;
        int baseAlpha = (shadowColor >>> 24) & 0xFF;
        int rgb = shadowColor & 0x00FFFFFF;

        for (int i = 1; i <= shadowSize; i++) {
            float factor = (float) (shadowSize - i + 1) / (shadowSize + 1);
            int layerAlpha = Math.round(baseAlpha * factor * factor);
            if (layerAlpha <= 0) continue;
            int layerColor = (layerAlpha << 24) | rgb;

            context.fill(x - i, y - i, x + width + i, y - i + 1, layerColor);
            context.fill(x - i, y + height + i - 1, x + width + i, y + height + i, layerColor);
            context.fill(x - i, y - i + 1, x - i + 1, y + height + i - 1, layerColor);
            context.fill(x + width + i - 1, y - i + 1, x + width + i, y + height + i - 1, layerColor);
        }
    }

    // draws card with drop shadow
    public static void drawCardWithShadow(DrawContext context, int x, int y, int width, int height, int bgColor, int borderColor, int shadowSize, int shadowColor) {
        drawShadow(context, x, y, width, height, shadowSize, shadowColor);
        drawCard(context, x, y, width, height, bgColor, borderColor);
    }

    // draws interactive button with normal and hover states
    public static void drawButton(DrawContext context, TextRenderer tr, int x, int y, int width, int height, Text label, boolean hovered, int normalBg, int hoverBg, int normalBorder, int hoverBorder, int normalText, int hoverText, float textScale) {
        int bg = hovered ? hoverBg : normalBg;
        int border = hovered ? hoverBorder : normalBorder;
        int text = hovered ? hoverText : normalText;

        drawCard(context, x, y, width, height, bg, border);
        if (label != null && tr != null) {
            TextScaleHelper.drawCenteredScaledText(context, tr, label, x + width / 2, y + (height - Math.round(8 * textScale)) / 2, text, textScale);
        }
    }

    // renders scaled itemstack centered inside slot boundaries
    public static void renderScaledItem(DrawContext context, TextRenderer tr, ItemStack stack, int slotX, int slotY, int slotSize, float itemScale) {
        if (stack == null || stack.isEmpty()) return;

        float effectiveScale = ((float) slotSize / 18.0f) * itemScale;
        float cx = slotX + (slotSize - 1) / 2.0f;
        float cy = slotY + (slotSize - 1) / 2.0f;

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(cx, cy);
        context.getMatrices().scale(effectiveScale, effectiveScale);

        context.drawItem(stack, -8, -8);
        if (tr != null) {
            context.drawStackOverlay(tr, stack, -8, -8);
        }

        context.getMatrices().popMatrix();
    }

    // renders complete slot card with background, border, and scaled item
    public static void renderSlot(DrawContext context, TextRenderer tr, ItemStack stack, int slotX, int slotY, int slotSize, float itemScale, boolean isHovered, int normalBg, int hoverBg, int normalBorder, int hoverBorder) {
        int bg = isHovered ? hoverBg : normalBg;
        int border = isHovered ? hoverBorder : normalBorder;
        drawCard(context, slotX, slotY, slotSize, slotSize, bg, border);

        if (stack != null && !stack.isEmpty()) {
            renderScaledItem(context, tr, stack, slotX, slotY, slotSize, itemScale);
        }
    }

    // renders slot card with top-left badge
    public static void renderSlotWithBadge(DrawContext context, TextRenderer tr, ItemStack stack, String badge, int slotX, int slotY, int slotSize, float itemScale, float textScale, boolean isHovered, int normalBg, int hoverBg, int normalBorder, int hoverBorder, int normalBadgeColor, int hoverBadgeColor) {
        renderSlot(context, tr, stack, slotX, slotY, slotSize, itemScale, isHovered, normalBg, hoverBg, normalBorder, hoverBorder);

        if (badge != null && !badge.isEmpty() && tr != null) {
            int badgeColor = isHovered ? hoverBadgeColor : normalBadgeColor;
            TextScaleHelper.drawScaledText(context, tr, badge, slotX + 2, slotY + 2, badgeColor, false, textScale);
        }
    }

    // vector icon: cross / delete
    public static void drawDeleteIcon(DrawContext context, float cx, float cy, float scale, int color) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(cx, cy);
        context.getMatrices().scale(scale, scale);

        context.fill(-2, -2, -1, -1, color);
        context.fill(2, -2, 3, -1, color);
        context.fill(-1, -1, 0, 0, color);
        context.fill(1, -1, 2, 0, color);
        context.fill(0, 0, 1, 1, color);
        context.fill(-1, 1, 0, 2, color);
        context.fill(1, 1, 2, 2, color);
        context.fill(-2, 2, -1, 3, color);
        context.fill(2, 2, 3, 3, color);

        context.getMatrices().popMatrix();
    }

    // vector icon: plus / add
    public static void drawPlusIcon(DrawContext context, float cx, float cy, float scale, int color) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(cx, cy);
        context.getMatrices().scale(scale, scale);

        context.fill(-2, 0, 3, 1, color);
        context.fill(0, -2, 1, 3, color);

        context.getMatrices().popMatrix();
    }

    // vector icon: pencil / edit
    public static void drawEditIcon(DrawContext context, float cx, float cy, float scale, int color) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(cx, cy);
        context.getMatrices().scale(scale, scale);

        context.fill(1, -2, 2, -1, color);
        context.fill(0, -1, 1, 0, color);
        context.fill(-1, 0, 0, 1, color);
        context.fill(-2, 1, -1, 2, color);
        context.fill(-2, 2, -1, 3, color);

        context.getMatrices().popMatrix();
    }

    // vector icon: arrow up
    public static void drawArrowUpIcon(DrawContext context, float cx, float cy, float scale, int color) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(cx, cy);
        context.getMatrices().scale(scale, scale);

        context.fill(0, -1, 1, 0, color);
        context.fill(-1, 0, 2, 1, color);
        context.fill(-2, 1, 3, 2, color);

        context.getMatrices().popMatrix();
    }

    // vector icon: arrow down
    public static void drawArrowDownIcon(DrawContext context, float cx, float cy, float scale, int color) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(cx, cy);
        context.getMatrices().scale(scale, scale);

        context.fill(-2, -1, 3, 0, color);
        context.fill(-1, 0, 2, 1, color);
        context.fill(0, 1, 1, 2, color);

        context.getMatrices().popMatrix();
    }

    // vector icon: duplicate
    public static void drawDuplicateIcon(DrawContext context, float cx, float cy, float scale, int color) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(cx, cy);
        context.getMatrices().scale(scale, scale);

        context.fill(0, -3, 3, -2, color);
        context.fill(2, -2, 3, 1, color);
        context.fill(-3, -1, 1, 0, color);
        context.fill(-3, 2, 1, 3, color);
        context.fill(-3, 0, -2, 2, color);
        context.fill(0, 0, 1, 2, color);

        context.getMatrices().popMatrix();
    }

    // vector icon: load into hotbar
    public static void drawLoadHotbarIcon(DrawContext context, float cx, float cy, float scale, int color) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(cx, cy);
        context.getMatrices().scale(scale, scale);

        context.fill(0, -2, 1, 0, color);
        context.fill(-1, -1, 2, 0, color);
        context.fill(0, 0, 1, 1, color);
        context.fill(-2, 2, 3, 3, color);
        context.fill(-2, 1, -1, 2, color);
        context.fill(2, 1, 3, 2, color);

        context.getMatrices().popMatrix();
    }

    // vector icon: paste hotbar to palette
    public static void drawPasteHotbarIcon(DrawContext context, float cx, float cy, float scale, int color) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(cx, cy);
        context.getMatrices().scale(scale, scale);

        context.fill(0, -2, 1, -1, color);
        context.fill(-1, -1, 2, 0, color);
        context.fill(0, 0, 1, 1, color);
        context.fill(-2, 2, 3, 3, color);
        context.fill(-2, 1, -1, 2, color);
        context.fill(2, 1, 3, 2, color);

        context.getMatrices().popMatrix();
    }

    // vector icon: place palette in world
    public static void drawPlaceWorldIcon(DrawContext context, float cx, float cy, float scale, int color) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(cx, cy);
        context.getMatrices().scale(scale, scale);

        context.fill(0, -3, 1, -2, color);
        context.fill(-2, -2, 3, -1, color);
        context.fill(-3, -1, 4, 0, color);
        context.fill(-3, 0, 0, 3, color);
        context.fill(1, 0, 4, 3, color);
        context.fill(-1, 3, 2, 4, color);

        context.getMatrices().popMatrix();
    }

    // vector icon: sort by column color (three vertical column bars)
    public static void drawColorSortIcon(DrawContext context, float cx, float cy, float scale, boolean active) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(cx, cy);
        context.getMatrices().scale(scale, scale);

        int c1 = active ? 0xFFEF4444 : 0xFFA8A29E;
        int c2 = active ? 0xFF10B981 : 0xFF78716C;
        int c3 = active ? 0xFF38BDF8 : 0xFF57534E;

        context.fill(-3, -3, -1, 3, c1);
        context.fill(-1, -1, 1, 3, c2);
        context.fill(1, 1, 3, 3, c3);

        context.getMatrices().popMatrix();
    }
}
