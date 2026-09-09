package com.itemorganizer.gui.widget;

import java.util.ArrayList;
import java.util.List;

import com.itemorganizer.client.ItemOrganizerClient;
import com.itemorganizer.core.model.ModConfig;
import com.itemorganizer.gui.util.RenderHelper;
import com.itemorganizer.gui.util.TextScaleHelper;
import com.itemorganizer.gui.viewmodel.OrganizerViewModel;
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
import net.minecraft.client.option.KeyBinding;
import com.itemorganizer.gui.util.SoundHelper;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

// settings widget for background color, opacity, zoom, and keybinds
public class ConfigWidget implements Drawable, Element, Selectable {
    public static final int SCROLLBAR_WIDTH = 3;

    private final OrganizerViewModel viewModel;
    private int x;
    private int y;
    private int width;
    private int height;

    private final VerticalScrollbar scrollbar;

    // hex color input field
    private TextFieldWidget hexColorField;
    private String hexErrorMessage = "";

    // rgb sliders
    private boolean draggingRed = false;
    private boolean draggingGreen = false;
    private boolean draggingBlue = false;

    // setting sliders
    private boolean draggingTransparency = false;
    private boolean draggingBlur = false;
    private boolean draggingScale = false;
    private boolean draggingItemScale = false;
    private boolean draggingTextScale = false;
    private boolean draggingSplitRatio = false;
    private boolean draggingHotbarScale = false;
    private boolean draggingHotbarItemScale = false;
    private boolean draggingPaletteScale = false;
    private boolean draggingPaletteItemScale = false;

    // key listening state
    private boolean listeningForKey = false;
    private boolean listeningForQuickAppendKey = false;
    private boolean listeningForUndoKey = false;

    // color presets
    private static final PresetColor[] PRESETS = new PresetColor[]{
            new PresetColor("Obsidian", 0x101010),
            new PresetColor("Charcoal", 0x1E1E1E),
            new PresetColor("Abyss", 0x0A1428),
            new PresetColor("Forest", 0x0E2015),
            new PresetColor("Amethyst", 0x1F102E),
            new PresetColor("Sapphire", 0x0C2340),
            new PresetColor("Gray", 0xB3B3B3)
    };

    private record PresetColor(String name, int color) {}

    public ConfigWidget(OrganizerViewModel viewModel, int x, int y, int width, int height) {
        this.viewModel = viewModel;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        int listStartY = y + 8;
        int listHeight = height - 16;
        this.scrollbar = new VerticalScrollbar(x + width - SCROLLBAR_WIDTH - 4, listStartY, SCROLLBAR_WIDTH, listHeight);

        initInputs();
    }

    private void initInputs() {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;

        int fieldX = x + 100;
        int fieldY = y + 54;
        this.hexColorField = new TextFieldWidget(tr, fieldX, fieldY, 54, 14, Text.literal("Hex"));
        this.hexColorField.setMaxLength(7);
        ModConfig cfg = viewModel.getConfig();
        this.hexColorField.setText(String.format("#%06X", cfg.getBackgroundColor() & 0x00FFFFFF));

        this.hexColorField.setChangedListener(text -> {
            String clean = text.startsWith("#") ? text.substring(1) : text;
            if (clean.length() == 6) {
                try {
                    int parsed = Integer.parseInt(clean, 16);
                    hexErrorMessage = "";
                    viewModel.updateConfig(c -> c.setBackgroundColor(parsed));
                } catch (NumberFormatException e) {
                    hexErrorMessage = Text.translatable("config.itemorganizer.error.invalid_hex").getString();
                }
            } else if (!clean.isEmpty()) {
                hexErrorMessage = Text.translatable("config.itemorganizer.error.must_be_hex").getString();
            } else {
                hexErrorMessage = "";
            }
        });
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        int listStartY = y + 8;
        int listHeight = height - 16;
        this.scrollbar.setBounds(x + width - SCROLLBAR_WIDTH - 4, listStartY, SCROLLBAR_WIDTH, listHeight);
    }

    // shortcut entry for the top help guide
    public static class ShortcutEntry {
        private final String key;
        private final String action;

        public ShortcutEntry(String key, String action) {
            this.key = key;
            this.action = action;
        }

        public String format() {
            return "§f[" + key + "] §7" + action;
        }
    }

    private String getQuickAppendKeyName(ModConfig cfg) {
        try {
            InputUtil.Key k = InputUtil.fromTranslationKey(cfg.getKeyQuickAppend());
            if (k != null) {
                return k.getLocalizedText().getString().toUpperCase();
            }
        } catch (Exception ignored) {}
        return "A";
    }

    private String getUndoKeyName(ModConfig cfg) {
        try {
            InputUtil.Key k = InputUtil.fromTranslationKey(cfg.getKeyUndo());
            if (k != null) {
                return k.getLocalizedText().getString().toUpperCase();
            }
        } catch (Exception ignored) {}
        return "Z";
    }

    private List<ShortcutEntry> getShortcutEntries(ModConfig cfg) {
        List<ShortcutEntry> list = new ArrayList<>();
        String rightClick = Text.translatable("config.itemorganizer.shortcut.right_click").getString();
        String arrows = Text.translatable("config.itemorganizer.shortcut.arrows").getString();

        list.add(new ShortcutEntry("1-9", Text.translatable("config.itemorganizer.shortcut.hotbar").getString()));
        list.add(new ShortcutEntry(rightClick, Text.translatable("config.itemorganizer.shortcut.select").getString()));
        list.add(new ShortcutEntry(rightClick + " + DEL", Text.translatable("config.itemorganizer.shortcut.remove").getString()));
        list.add(new ShortcutEntry(rightClick + " + " + arrows, Text.translatable("config.itemorganizer.shortcut.move").getString()));
        list.add(new ShortcutEntry(rightClick + " + " + rightClick, Text.translatable("config.itemorganizer.shortcut.block").getString()));
        list.add(new ShortcutEntry(getQuickAppendKeyName(cfg), Text.translatable("config.itemorganizer.shortcut.quick_append").getString()));
        list.add(new ShortcutEntry("Ctrl + " + getUndoKeyName(cfg), Text.translatable("config.itemorganizer.shortcut.undo").getString()));
        list.add(new ShortcutEntry("ESC", Text.translatable("config.itemorganizer.shortcut.exit").getString()));
        return list;
    }

    // wrap shortcut lines to fit available width
    private List<String> buildGuideLines(TextRenderer tr, ModConfig cfg, int maxWidth, float guideScale) {
        List<ShortcutEntry> entries = getShortcutEntries(cfg);
        List<String> lines = new ArrayList<>();
        if (entries.isEmpty()) return lines;

        if (tr == null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < entries.size(); i++) {
                if (i > 0) sb.append("   ");
                sb.append(entries.get(i).format());
                if (i % 2 == 1 || i == entries.size() - 1) {
                    lines.add(sb.toString());
                    sb = new StringBuilder();
                }
            }
            return lines;
        }

        int effectiveMaxW = Math.max(120, (int) (maxWidth / guideScale));
        StringBuilder currentLine = new StringBuilder();
        int currentLineWidth = 0;
        int sepWidth = tr.getWidth("   ");

        for (ShortcutEntry entry : entries) {
            String token = entry.format();
            int tokenW = tr.getWidth(token);

            if (currentLine.length() == 0) {
                currentLine.append(token);
                currentLineWidth = tokenW;
            } else if (currentLineWidth + sepWidth + tokenW <= effectiveMaxW) {
                currentLine.append("   ").append(token);
                currentLineWidth += sepWidth + tokenW;
            } else {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(token);
                currentLineWidth = tokenW;
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        return lines;
    }

    private int getShortcutGuideHeight(float textScale, int contentWidth) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = (client != null) ? client.textRenderer : null;
        float guideScale = Math.max(0.70f, Math.min(1.0f, textScale * 0.85f));
        int lineSpacing = Math.max(10, Math.round(11 * guideScale));
        List<String> lines = buildGuideLines(tr, viewModel.getConfig(), contentWidth, guideScale);
        if (lines.isEmpty()) return 0;
        return (lines.size() * lineSpacing) + 8;
    }

    private int calculateTotalHeight(float textScale) {
        int maxContentW = width - SCROLLBAR_WIDTH - 24;
        return getShortcutGuideHeight(textScale, maxContentW) + Math.round(520 * Math.max(1.0f, textScale));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;
        ModConfig cfg = viewModel.getConfig();
        float textScale = cfg.getTextScale();

        int listStartY = y + 8;
        int listHeight = height - 16;
        int contentX = x + 10;
        int maxContentW = width - SCROLLBAR_WIDTH - 24;
        scrollbar.updateMaxScroll(calculateTotalHeight(textScale), listHeight);

        context.enableScissor(x + 4, listStartY, x + width - 4, listStartY + listHeight);

        int scroll = (int) scrollbar.getScrollOffset();
        int sliderW = Math.min(180, maxContentW);
        int sliderH = Math.max(12, Math.round(12 * Math.max(1.0f, textScale)));
        int labelGap = Math.max(10, Math.round(8 * textScale) + 2);
        int secGap = 6;

        // shortcut guide
        float guideScale = Math.max(0.70f, Math.min(1.0f, textScale * 0.85f));
        int lineSpacing = Math.max(10, Math.round(11 * guideScale));
        List<String> guideLines = buildGuideLines(tr, cfg, maxContentW, guideScale);

        int guideStartY = listStartY + 4 - scroll;
        for (int i = 0; i < guideLines.size(); i++) {
            int lineY = guideStartY + (i * lineSpacing);
            if (lineY + lineSpacing >= listStartY && lineY <= listStartY + listHeight) {
                TextScaleHelper.drawScaledText(
                        context, tr, guideLines.get(i), contentX, lineY, 0xFFCCCCCC, false, guideScale
                );
            }
        }

        int guideTotalHeight = guideLines.isEmpty() ? 0 : (guideLines.size() * lineSpacing + 8);

        int currentColor = cfg.getBackgroundColor() & 0x00FFFFFF;
        int currentR = (currentColor >> 16) & 0xFF;
        int currentG = (currentColor >> 8) & 0xFF;
        int currentB = currentColor & 0xFF;

        // keep hex text in sync when not focused
        if (!hexColorField.isFocused() && hexErrorMessage.isEmpty()) {
            String expectedHex = String.format("#%06X", currentColor);
            if (!expectedHex.equalsIgnoreCase(hexColorField.getText())) {
                hexColorField.setText(expectedHex);
            }
        }

        // background color
        int sec1Y = guideStartY + guideTotalHeight;
        if (sec1Y + 12 >= listStartY && sec1Y <= listStartY + listHeight) {
            TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.bg_color"), contentX, sec1Y, 0xFF38BDF8, true, textScale);
        }

        int swatchX = contentX;
        int swatchY = sec1Y + labelGap;
        int swatchSize = 14;
        int chipW = 16;
        int chipH = 14;
        int presetStartX = swatchX + swatchSize + 4 + hexColorField.getWidth() + 6;
        boolean presetsInline = (presetStartX + (PRESETS.length * (chipW + 3)) <= x + width - SCROLLBAR_WIDTH - 6);

        if (swatchY + swatchSize >= listStartY && swatchY <= listStartY + listHeight) {
            int currentRgb = currentColor | 0xFF000000;
            context.fill(swatchX, swatchY, swatchX + swatchSize, swatchY + swatchSize, currentRgb);
            RenderHelper.drawBorder(context, swatchX, swatchY, swatchSize, swatchSize, 0x40FFFFFF);

            hexColorField.setY(swatchY);
            hexColorField.setX(swatchX + swatchSize + 4);
            hexColorField.renderWidget(context, mouseX, mouseY, delta);

            if (!hexErrorMessage.isEmpty()) {
                TextScaleHelper.drawScaledText(context, tr, hexErrorMessage, hexColorField.getX() + hexColorField.getWidth() + 4, swatchY + 3, 0xFFEF4444, false, textScale);
            }
        }

        // color preset chips
        for (int i = 0; i < PRESETS.length; i++) {
            PresetColor p = PRESETS[i];
            int px = presetsInline ? presetStartX + i * (chipW + 3) : contentX + i * (chipW + 4);
            int py = presetsInline ? swatchY : swatchY + swatchSize + 4;

            if (py + chipH >= listStartY && py <= listStartY + listHeight) {
                boolean hoverP = mouseX >= px && mouseX <= px + chipW && mouseY >= py && mouseY <= py + chipH;
                int bg = p.color | 0xFF000000;
                context.fill(px, py, px + chipW, py + chipH, bg);
                RenderHelper.drawBorder(context, px, py, chipW, chipH, hoverP ? 0xFF38BDF8 : 0x25FFFFFF);
            }
        }

        // rgb sliders
        int rgbStartY = presetsInline ? swatchY + swatchSize + 4 : swatchY + swatchSize + 4 + chipH + 4;
        int rgbSliderH = 11;

        int sliderRedY = rgbStartY;
        if (sliderRedY + rgbSliderH >= listStartY && sliderRedY <= listStartY + listHeight) {
            float normR = currentR / 255.0f;
            renderSlider(context, tr, contentX, sliderRedY, sliderW, rgbSliderH, normR, "R: " + currentR, mouseX, mouseY, textScale, 0x66EF4444, 0xFFEF4444);
        }

        int sliderGreenY = sliderRedY + rgbSliderH + 3;
        if (sliderGreenY + rgbSliderH >= listStartY && sliderGreenY <= listStartY + listHeight) {
            float normG = currentG / 255.0f;
            renderSlider(context, tr, contentX, sliderGreenY, sliderW, rgbSliderH, normG, "G: " + currentG, mouseX, mouseY, textScale, 0x6610B981, 0xFF34D399);
        }

        int sliderBlueY = sliderGreenY + rgbSliderH + 3;
        if (sliderBlueY + rgbSliderH >= listStartY && sliderBlueY <= listStartY + listHeight) {
            float normB = currentB / 255.0f;
            renderSlider(context, tr, contentX, sliderBlueY, sliderW, rgbSliderH, normB, "B: " + currentB, mouseX, mouseY, textScale, 0x663B82F6, 0xFF38BDF8);
        }

        // transparency slider
        int sec2Y = sliderBlueY + rgbSliderH + secGap;
        int alphaPercent = Math.round(cfg.getTransparency() * 100.0f);
        if (sec2Y + 12 >= listStartY && sec2Y <= listStartY + listHeight) {
            TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.transparency"), contentX, sec2Y, 0xFF38BDF8, true, textScale);
        }

        int slider1X = contentX;
        int slider1Y = sec2Y + labelGap;
        if (slider1Y + sliderH >= listStartY && slider1Y <= listStartY + listHeight) {
            renderSlider(context, tr, slider1X, slider1Y, sliderW, sliderH, cfg.getTransparency(), alphaPercent + "%", mouseX, mouseY, textScale, 0x4D38BDF8, 0xFF38BDF8);
        }

        // blur slider
        int secBlurY = slider1Y + sliderH + secGap;
        int blurPercent = Math.round(cfg.getBlur() * 100.0f);
        if (secBlurY + 12 >= listStartY && secBlurY <= listStartY + listHeight) {
            TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.blur"), contentX, secBlurY, 0xFF38BDF8, true, textScale);
        }

        int sliderBlurX = contentX;
        int sliderBlurY = secBlurY + labelGap;
        if (sliderBlurY + sliderH >= listStartY && sliderBlurY <= listStartY + listHeight) {
            float blurNorm = MathHelper.clamp(cfg.getBlur() / 5.0f, 0.0f, 1.0f);
            renderSlider(context, tr, sliderBlurX, sliderBlurY, sliderW, sliderH, blurNorm, blurPercent + "%", mouseX, mouseY, textScale, 0x4D38BDF8, 0xFF38BDF8);
        }

        // grid zoom slider
        int sec3Y = sliderBlurY + sliderH + secGap;
        int scalePercent = Math.round(cfg.getScale() * 100.0f);
        if (sec3Y + 12 >= listStartY && sec3Y <= listStartY + listHeight) {
            TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.grid_zoom"), contentX, sec3Y, 0xFF38BDF8, true, textScale);
        }

        int slider2X = contentX;
        int slider2Y = sec3Y + labelGap;
        if (slider2Y + sliderH >= listStartY && slider2Y <= listStartY + listHeight) {
            float normScale = (cfg.getScale() - 0.10f) / 4.90f;
            renderSlider(context, tr, slider2X, slider2Y, sliderW, sliderH, normScale, scalePercent + "% (" + String.format("%.2f", cfg.getScale()) + "x)", mouseX, mouseY, textScale, 0x4D38BDF8, 0xFF38BDF8);
        }

        // item scale slider
        int sec4Y = slider2Y + sliderH + secGap;
        int itemScalePercent = Math.round(cfg.getItemScale() * 100.0f);
        if (sec4Y + 12 >= listStartY && sec4Y <= listStartY + listHeight) {
            TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.item_scale"), contentX, sec4Y, 0xFF38BDF8, true, textScale);
        }

        int slider3X = contentX;
        int slider3Y = sec4Y + labelGap;
        if (slider3Y + sliderH >= listStartY && slider3Y <= listStartY + listHeight) {
            float normItemScale = (cfg.getItemScale() - 0.5f) / 1.0f;
            renderSlider(context, tr, slider3X, slider3Y, sliderW, sliderH, normItemScale, itemScalePercent + "% (" + String.format("%.2f", cfg.getItemScale()) + "x)", mouseX, mouseY, textScale, 0x4D38BDF8, 0xFF38BDF8);
        }

        // text scale slider
        int sec5Y = slider3Y + sliderH + secGap;
        int textScalePercent = Math.round(cfg.getTextScale() * 100.0f);
        if (sec5Y + 12 >= listStartY && sec5Y <= listStartY + listHeight) {
            TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.text_scale"), contentX, sec5Y, 0xFF38BDF8, true, textScale);
        }

        int slider4X = contentX;
        int slider4Y = sec5Y + labelGap;
        if (slider4Y + sliderH >= listStartY && slider4Y <= listStartY + listHeight) {
            float normTextScale = (cfg.getTextScale() - 0.5f) / 1.0f;
            renderSlider(context, tr, slider4X, slider4Y, sliderW, sliderH, normTextScale, textScalePercent + "% (" + String.format("%.2f", cfg.getTextScale()) + "x)", mouseX, mouseY, textScale, 0x4D38BDF8, 0xFF38BDF8);
        }

        // panel split ratio slider
        int sec6Y = slider4Y + sliderH + secGap;
        int leftPercent = Math.round(cfg.getSplitRatio() * 100.0f);
        if (sec6Y + 12 >= listStartY && sec6Y <= listStartY + listHeight) {
            TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.panel_split"), contentX, sec6Y, 0xFF38BDF8, true, textScale);
        }

        int slider5X = contentX;
        int slider5Y = sec6Y + labelGap;
        if (slider5Y + sliderH >= listStartY && slider5Y <= listStartY + listHeight) {
            float normSplit = (cfg.getSplitRatio() - 0.20f) / 0.60f;
            renderSlider(context, tr, slider5X, slider5Y, sliderW, sliderH, normSplit, leftPercent + "% / " + (100 - leftPercent) + "%", mouseX, mouseY, textScale, 0x4D38BDF8, 0xFF38BDF8);
        }

        // hotbar scale slider
        int sec7Y = slider5Y + sliderH + secGap;
        int hotbarPercent = Math.round(cfg.getHotbarScale() * 100.0f);
        if (sec7Y + 12 >= listStartY && sec7Y <= listStartY + listHeight) {
            TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.hotbar_scale"), contentX, sec7Y, 0xFF38BDF8, true, textScale);
        }

        int slider6X = contentX;
        int slider6Y = sec7Y + labelGap;
        if (slider6Y + sliderH >= listStartY && slider6Y <= listStartY + listHeight) {
            float normHotbar = (cfg.getHotbarScale() - 0.50f) / 1.50f;
            renderSlider(context, tr, slider6X, slider6Y, sliderW, sliderH, normHotbar, hotbarPercent + "% (" + String.format("%.2f", cfg.getHotbarScale()) + "x)", mouseX, mouseY, textScale, 0x4D38BDF8, 0xFF38BDF8);
        }

        // hotbar item scale slider
        int sec7bY = slider6Y + sliderH + secGap;
        int hotbarItemPercent = Math.round(cfg.getHotbarItemScale() * 100.0f);
        if (sec7bY + 12 >= listStartY && sec7bY <= listStartY + listHeight) {
            TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.hotbar_item_scale"), contentX, sec7bY, 0xFF38BDF8, true, textScale);
        }

        int slider6bX = contentX;
        int slider6bY = sec7bY + labelGap;
        if (slider6bY + sliderH >= listStartY && slider6bY <= listStartY + listHeight) {
            float normHotbarItem = (cfg.getHotbarItemScale() - 0.50f) / 1.00f;
            renderSlider(context, tr, slider6bX, slider6bY, sliderW, sliderH, normHotbarItem, hotbarItemPercent + "% (" + String.format("%.2f", cfg.getHotbarItemScale()) + "x)", mouseX, mouseY, textScale, 0x4D38BDF8, 0xFF38BDF8);
        }

        // palette scale slider
        int sec8Y = slider6bY + sliderH + secGap;
        int palettePercent = Math.round(cfg.getPaletteScale() * 100.0f);
        if (sec8Y + 12 >= listStartY && sec8Y <= listStartY + listHeight) {
            TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.palette_scale"), contentX, sec8Y, 0xFF38BDF8, true, textScale);
        }

        int slider7X = contentX;
        int slider7Y = sec8Y + labelGap;
        if (slider7Y + sliderH >= listStartY && slider7Y <= listStartY + listHeight) {
            float normPalette = (cfg.getPaletteScale() - 0.50f) / 1.50f;
            renderSlider(context, tr, slider7X, slider7Y, sliderW, sliderH, normPalette, palettePercent + "% (" + String.format("%.2f", cfg.getPaletteScale()) + "x)", mouseX, mouseY, textScale, 0x4D38BDF8, 0xFF38BDF8);
        }

        // palette item scale slider
        int sec8bY = slider7Y + sliderH + secGap;
        int paletteItemPercent = Math.round(cfg.getPaletteItemScale() * 100.0f);
        if (sec8bY + 12 >= listStartY && sec8bY <= listStartY + listHeight) {
            TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.palette_item_scale"), contentX, sec8bY, 0xFF38BDF8, true, textScale);
        }

        int slider7bX = contentX;
        int slider7bY = sec8bY + labelGap;
        if (slider7bY + sliderH >= listStartY && slider7bY <= listStartY + listHeight) {
            float normPaletteItem = (cfg.getPaletteItemScale() - 0.50f) / 1.00f;
            renderSlider(context, tr, slider7bX, slider7bY, sliderW, sliderH, normPaletteItem, paletteItemPercent + "% (" + String.format("%.2f", cfg.getPaletteItemScale()) + "x)", mouseX, mouseY, textScale, 0x4D38BDF8, 0xFF38BDF8);
        }

        // open key binding
        int sec9Y = slider7bY + sliderH + secGap;
        if (sec9Y + 12 >= listStartY && sec9Y <= listStartY + listHeight) {
            TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.open_key"), contentX, sec9Y, 0xFF38BDF8, true, textScale);
        }

        int keyBtnX = contentX;
        int keyBtnY = sec9Y + labelGap;
        int keyBtnW = Math.min(180, maxContentW);
        int keyBtnH = Math.max(15, Math.round(15 * Math.max(1.0f, textScale)));

        if (keyBtnY + keyBtnH >= listStartY && keyBtnY <= listStartY + listHeight) {
            boolean hoverKeyBtn = mouseX >= keyBtnX && mouseX <= keyBtnX + keyBtnW && mouseY >= keyBtnY && mouseY <= keyBtnY + keyBtnH;
            String keyLabel;
            int keyBg;
            int keyBorder;
            int keyTextColor;

            if (listeningForKey) {
                keyLabel = Text.translatable("config.itemorganizer.press_key").getString();
                keyBg = 0x4DF59E0B;
                keyBorder = 0xFFF59E0B;
                keyTextColor = 0xFFF59E0B;
            } else {
                KeyBinding binding = ItemOrganizerClient.getOpenKeyBinding();
                Text keyText = (binding != null) ? binding.getBoundKeyLocalizedText() : Text.literal("O");
                keyLabel = Text.translatable("config.itemorganizer.key_label", keyText.getString()).getString();
                keyBg = hoverKeyBtn ? 0x801E3A5F : 0x14FFFFFF;
                keyBorder = hoverKeyBtn ? 0xFF38BDF8 : 0x25FFFFFF;
                keyTextColor = hoverKeyBtn ? 0xFFFFFFFF : 0xFFCBD5E1;
            }

            context.fill(keyBtnX, keyBtnY, keyBtnX + keyBtnW, keyBtnY + keyBtnH, keyBg);
            RenderHelper.drawBorder(context, keyBtnX, keyBtnY, keyBtnW, keyBtnH, keyBorder);
            TextScaleHelper.drawVerticallyCenteredScaledText(context, tr, keyLabel, keyBtnX + keyBtnW / 2, keyBtnY + keyBtnH / 2, keyTextColor, textScale);
        }

        // quick append key binding
        int sec10Y = keyBtnY + keyBtnH + secGap;
        if (sec10Y + 12 >= listStartY && sec10Y <= listStartY + listHeight) {
            TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.quick_append_key"), contentX, sec10Y, 0xFF38BDF8, true, textScale);
        }

        int quickKeyBtnX = contentX;
        int quickKeyBtnY = sec10Y + labelGap;
        int quickKeyBtnW = Math.min(180, maxContentW);
        int quickKeyBtnH = Math.max(15, Math.round(15 * Math.max(1.0f, textScale)));

        if (quickKeyBtnY + quickKeyBtnH >= listStartY && quickKeyBtnY <= listStartY + listHeight) {
            boolean hoverQuickKey = mouseX >= quickKeyBtnX && mouseX <= quickKeyBtnX + quickKeyBtnW && mouseY >= quickKeyBtnY && mouseY <= quickKeyBtnY + quickKeyBtnH;
            String qkLabel;
            int qkBg;
            int qkBorder;
            int qkTextColor;

            if (listeningForQuickAppendKey) {
                qkLabel = Text.translatable("config.itemorganizer.press_key").getString();
                qkBg = 0x4DF59E0B;
                qkBorder = 0xFFF59E0B;
                qkTextColor = 0xFFF59E0B;
            } else {
                String boundKey = cfg.getKeyQuickAppend();
                InputUtil.Key k = InputUtil.fromTranslationKey(boundKey);
                String keyName = (k != null) ? k.getLocalizedText().getString() : "A";
                qkLabel = Text.translatable("config.itemorganizer.key_label", keyName).getString();
                qkBg = hoverQuickKey ? 0x801E3A5F : 0x14FFFFFF;
                qkBorder = hoverQuickKey ? 0xFF38BDF8 : 0x25FFFFFF;
                qkTextColor = hoverQuickKey ? 0xFFFFFFFF : 0xFFCBD5E1;
            }

            context.fill(quickKeyBtnX, quickKeyBtnY, quickKeyBtnX + quickKeyBtnW, quickKeyBtnY + quickKeyBtnH, qkBg);
            RenderHelper.drawBorder(context, quickKeyBtnX, quickKeyBtnY, quickKeyBtnW, quickKeyBtnH, qkBorder);
            TextScaleHelper.drawVerticallyCenteredScaledText(context, tr, qkLabel, quickKeyBtnX + quickKeyBtnW / 2, quickKeyBtnY + quickKeyBtnH / 2, qkTextColor, textScale);
        }

        // undo key binding
        int sec11Y = quickKeyBtnY + quickKeyBtnH + secGap;
        if (sec11Y + 12 >= listStartY && sec11Y <= listStartY + listHeight) {
            TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.undo_key"), contentX, sec11Y, 0xFF38BDF8, true, textScale);
        }

        int undoKeyBtnX = contentX;
        int undoKeyBtnY = sec11Y + labelGap;
        int undoKeyBtnW = Math.min(180, maxContentW);
        int undoKeyBtnH = Math.max(15, Math.round(15 * Math.max(1.0f, textScale)));

        if (undoKeyBtnY + undoKeyBtnH >= listStartY && undoKeyBtnY <= listStartY + listHeight) {
            boolean hoverUndoKey = mouseX >= undoKeyBtnX && mouseX <= undoKeyBtnX + undoKeyBtnW && mouseY >= undoKeyBtnY && mouseY <= undoKeyBtnY + undoKeyBtnH;
            String undoLabel;
            int undoBg;
            int undoBorder;
            int undoTextColor;

            if (listeningForUndoKey) {
                undoLabel = Text.translatable("config.itemorganizer.press_key").getString();
                undoBg = 0x4DF59E0B;
                undoBorder = 0xFFF59E0B;
                undoTextColor = 0xFFF59E0B;
            } else {
                String boundKey = cfg.getKeyUndo();
                InputUtil.Key k = InputUtil.fromTranslationKey(boundKey);
                String keyName = (k != null) ? k.getLocalizedText().getString().toUpperCase() : "Z";
                undoLabel = Text.translatable("config.itemorganizer.key_label_ctrl", keyName).getString();
                undoBg = hoverUndoKey ? 0x801E3A5F : 0x14FFFFFF;
                undoBorder = hoverUndoKey ? 0xFF38BDF8 : 0x25FFFFFF;
                undoTextColor = hoverUndoKey ? 0xFFFFFFFF : 0xFFCBD5E1;
            }

            context.fill(undoKeyBtnX, undoKeyBtnY, undoKeyBtnX + undoKeyBtnW, undoKeyBtnY + undoKeyBtnH, undoBg);
            RenderHelper.drawBorder(context, undoKeyBtnX, undoKeyBtnY, undoKeyBtnW, undoKeyBtnH, undoBorder);
            TextScaleHelper.drawVerticallyCenteredScaledText(context, tr, undoLabel, undoKeyBtnX + undoKeyBtnW / 2, undoKeyBtnY + undoKeyBtnH / 2, undoTextColor, textScale);
        }

        // reset defaults button
        int resetBtnY = undoKeyBtnY + undoKeyBtnH + secGap + 2;
        int resetBtnW = Math.min(180, maxContentW);
        int resetBtnH = Math.max(15, Math.round(15 * Math.max(1.0f, textScale)));

        if (resetBtnY + resetBtnH >= listStartY && resetBtnY <= listStartY + listHeight) {
            boolean hoverReset = mouseX >= keyBtnX && mouseX <= keyBtnX + resetBtnW && mouseY >= resetBtnY && mouseY <= resetBtnY + resetBtnH;
            context.fill(keyBtnX, resetBtnY, keyBtnX + resetBtnW, resetBtnY + resetBtnH, hoverReset ? 0x807F1D1D : 0x337F1D1D);
            RenderHelper.drawBorder(context, keyBtnX, resetBtnY, resetBtnW, resetBtnH, hoverReset ? 0xFFEF4444 : 0x80EF4444);
            TextScaleHelper.drawVerticallyCenteredScaledText(context, tr, Text.translatable("config.itemorganizer.reset_defaults"), keyBtnX + resetBtnW / 2, resetBtnY + resetBtnH / 2, hoverReset ? 0xFFFFFFFF : 0xFFFCA5A5, textScale);
        }

        context.disableScissor();
        scrollbar.render(context, mouseX, mouseY);
    }

    private void renderSlider(DrawContext context, TextRenderer tr, int sx, int sy, int sw, int sh, float normalizedVal, String label, int mouseX, int mouseY, float textScale, int trackColor, int thumbColor) {
        normalizedVal = MathHelper.clamp(normalizedVal, 0.0f, 1.0f);

        context.fill(sx, sy, sx + sw, sy + sh, 0x40000000);
        int fillW = (int) (sw * normalizedVal);
        if (fillW > 0) {
            context.fill(sx, sy, sx + fillW, sy + sh, trackColor);
        }
        RenderHelper.drawBorder(context, sx, sy, sw, sh, 0x25FFFFFF);

        int thumbW = 7;
        int thumbX = sx + (int) (normalizedVal * (sw - thumbW));
        boolean hoverThumb = mouseX >= thumbX && mouseX <= thumbX + thumbW && mouseY >= sy && mouseY <= sy + sh;
        context.fill(thumbX, sy, thumbX + thumbW, sy + sh, hoverThumb ? 0xFFFFFFFF : thumbColor);
        RenderHelper.drawBorder(context, thumbX, sy, thumbW, sh, hoverThumb ? 0xFF38BDF8 : 0xCCFFFFFF);

        TextScaleHelper.drawVerticallyCenteredScaledText(context, tr, label, sx + sw / 2, sy + sh / 2, 0xFFFFFFFF, textScale);
    }

    private void playClickSound() {
        SoundHelper.playClick();
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        int mouseX = (int) click.x();
        int mouseY = (int) click.y();

        if (scrollbar.mouseClicked(click)) {
            return true;
        }

        int listStartY = y + 8;
        int listHeight = height - 16;

        if (mouseY < listStartY || mouseY > listStartY + listHeight) {
            if (listeningForKey) {
                listeningForKey = false;
            }
            if (listeningForQuickAppendKey) {
                listeningForQuickAppendKey = false;
            }
            if (listeningForUndoKey) {
                listeningForUndoKey = false;
            }
            return false;
        }

        float textScale = viewModel.getConfig().getTextScale();
        int scroll = (int) scrollbar.getScrollOffset();
        int contentX = x + 10;
        int maxContentW = width - SCROLLBAR_WIDTH - 24;
        int sliderW = Math.min(180, maxContentW);
        int sliderH = Math.max(12, Math.round(12 * Math.max(1.0f, textScale)));
        int labelGap = Math.max(10, Math.round(8 * textScale) + 2);
        int secGap = 6;

        int guideTotalHeight = getShortcutGuideHeight(textScale, maxContentW);

        // hex input field
        int sec1Y = listStartY + 4 - scroll + guideTotalHeight;
        int swatchX = contentX;
        int swatchY = sec1Y + labelGap;
        int swatchSize = 14;
        hexColorField.setY(swatchY);
        hexColorField.setX(swatchX + swatchSize + 4);
        if (hexColorField.mouseClicked(click, bl)) {
            return true;
        }

        // color presets
        int chipW = 16;
        int chipH = 14;
        int presetStartX = swatchX + swatchSize + 4 + hexColorField.getWidth() + 6;
        boolean presetsInline = (presetStartX + (PRESETS.length * (chipW + 3)) <= x + width - SCROLLBAR_WIDTH - 6);
        for (int i = 0; i < PRESETS.length; i++) {
            PresetColor p = PRESETS[i];
            int px = presetsInline ? presetStartX + i * (chipW + 3) : contentX + i * (chipW + 4);
            int py = presetsInline ? swatchY : swatchY + swatchSize + 4;

            if (mouseX >= px && mouseX <= px + chipW && mouseY >= py && mouseY <= py + chipH) {
                viewModel.updateConfig(c -> c.setBackgroundColor(p.color));
                hexColorField.setText(String.format("#%06X", p.color & 0x00FFFFFF));
                hexErrorMessage = "";
                playClickSound();
                return true;
            }
        }

        // rgb sliders
        int rgbStartY = presetsInline ? swatchY + swatchSize + 4 : swatchY + swatchSize + 4 + chipH + 4;
        int rgbSliderH = 11;

        int sliderRedY = rgbStartY;
        if (mouseX >= contentX && mouseX <= contentX + sliderW && mouseY >= sliderRedY && mouseY <= sliderRedY + rgbSliderH) {
            draggingRed = true;
            updateRedFromMouse(mouseX, contentX, sliderW);
            return true;
        }

        int sliderGreenY = sliderRedY + rgbSliderH + 3;
        if (mouseX >= contentX && mouseX <= contentX + sliderW && mouseY >= sliderGreenY && mouseY <= sliderGreenY + rgbSliderH) {
            draggingGreen = true;
            updateGreenFromMouse(mouseX, contentX, sliderW);
            return true;
        }

        int sliderBlueY = sliderGreenY + rgbSliderH + 3;
        if (mouseX >= contentX && mouseX <= contentX + sliderW && mouseY >= sliderBlueY && mouseY <= sliderBlueY + rgbSliderH) {
            draggingBlue = true;
            updateBlueFromMouse(mouseX, contentX, sliderW);
            return true;
        }

        // transparency slider
        int sec2Y = sliderBlueY + rgbSliderH + secGap;
        int slider1X = contentX;
        int slider1Y = sec2Y + labelGap;
        if (mouseX >= slider1X && mouseX <= slider1X + sliderW && mouseY >= slider1Y && mouseY <= slider1Y + sliderH) {
            draggingTransparency = true;
            updateTransparencyFromMouse(mouseX, slider1X, sliderW);
            return true;
        }

        // blur slider
        int secBlurY = slider1Y + sliderH + secGap;
        int sliderBlurX = contentX;
        int sliderBlurY = secBlurY + labelGap;
        if (mouseX >= sliderBlurX && mouseX <= sliderBlurX + sliderW && mouseY >= sliderBlurY && mouseY <= sliderBlurY + sliderH) {
            draggingBlur = true;
            updateBlurFromMouse(mouseX, sliderBlurX, sliderW);
            return true;
        }

        // grid zoom slider
        int sec3Y = sliderBlurY + sliderH + secGap;
        int slider2X = contentX;
        int slider2Y = sec3Y + labelGap;
        if (mouseX >= slider2X && mouseX <= slider2X + sliderW && mouseY >= slider2Y && mouseY <= slider2Y + sliderH) {
            draggingScale = true;
            updateScaleFromMouse(mouseX, slider2X, sliderW);
            return true;
        }

        // item scale slider
        int sec4Y = slider2Y + sliderH + secGap;
        int slider3X = contentX;
        int slider3Y = sec4Y + labelGap;
        if (mouseX >= slider3X && mouseX <= slider3X + sliderW && mouseY >= slider3Y && mouseY <= slider3Y + sliderH) {
            draggingItemScale = true;
            updateItemScaleFromMouse(mouseX, slider3X, sliderW);
            return true;
        }

        // text scale slider
        int sec5Y = slider3Y + sliderH + secGap;
        int slider4X = contentX;
        int slider4Y = sec5Y + labelGap;
        if (mouseX >= slider4X && mouseX <= slider4X + sliderW && mouseY >= slider4Y && mouseY <= slider4Y + sliderH) {
            draggingTextScale = true;
            updateTextScaleFromMouse(mouseX, slider4X, sliderW);
            return true;
        }

        // panel split slider
        int sec6Y = slider4Y + sliderH + secGap;
        int slider5X = contentX;
        int slider5Y = sec6Y + labelGap;
        if (mouseX >= slider5X && mouseX <= slider5X + sliderW && mouseY >= slider5Y && mouseY <= slider5Y + sliderH) {
            draggingSplitRatio = true;
            updateSplitRatioFromMouse(mouseX, slider5X, sliderW);
            return true;
        }

        // hotbar scale slider
        int sec7Y = slider5Y + sliderH + secGap;
        int slider6X = contentX;
        int slider6Y = sec7Y + labelGap;
        if (mouseX >= slider6X && mouseX <= slider6X + sliderW && mouseY >= slider6Y && mouseY <= slider6Y + sliderH) {
            draggingHotbarScale = true;
            updateHotbarScaleFromMouse(mouseX, slider6X, sliderW);
            return true;
        }

        // hotbar item scale slider
        int sec7bY = slider6Y + sliderH + secGap;
        int slider6bX = contentX;
        int slider6bY = sec7bY + labelGap;
        if (mouseX >= slider6bX && mouseX <= slider6bX + sliderW && mouseY >= slider6bY && mouseY <= slider6bY + sliderH) {
            draggingHotbarItemScale = true;
            updateHotbarItemScaleFromMouse(mouseX, slider6bX, sliderW);
            return true;
        }

        // palette scale slider
        int sec8Y = slider6bY + sliderH + secGap;
        int slider7X = contentX;
        int slider7Y = sec8Y + labelGap;
        if (mouseX >= slider7X && mouseX <= slider7X + sliderW && mouseY >= slider7Y && mouseY <= slider7Y + sliderH) {
            draggingPaletteScale = true;
            updatePaletteScaleFromMouse(mouseX, slider7X, sliderW);
            return true;
        }

        // palette item scale slider
        int sec8bY = slider7Y + sliderH + secGap;
        int slider7bX = contentX;
        int slider7bY = sec8bY + labelGap;
        if (mouseX >= slider7bX && mouseX <= slider7bX + sliderW && mouseY >= slider7bY && mouseY <= slider7bY + sliderH) {
            draggingPaletteItemScale = true;
            updatePaletteItemScaleFromMouse(mouseX, slider7bX, sliderW);
            return true;
        }

        // open/close key
        int sec9Y = slider7bY + sliderH + secGap;
        int keyBtnX = contentX;
        int keyBtnY = sec9Y + labelGap;
        int keyBtnW = Math.min(180, maxContentW);
        int keyBtnH = Math.max(15, Math.round(15 * Math.max(1.0f, textScale)));
        if (mouseX >= keyBtnX && mouseX <= keyBtnX + keyBtnW && mouseY >= keyBtnY && mouseY <= keyBtnY + keyBtnH) {
            listeningForKey = !listeningForKey;
            listeningForQuickAppendKey = false;
            listeningForUndoKey = false;
            playClickSound();
            return true;
        }

        // quick append key
        int sec10Y = keyBtnY + keyBtnH + secGap;
        int quickKeyBtnX = contentX;
        int quickKeyBtnY = sec10Y + labelGap;
        int quickKeyBtnW = Math.min(180, maxContentW);
        int quickKeyBtnH = Math.max(15, Math.round(15 * Math.max(1.0f, textScale)));
        if (mouseX >= quickKeyBtnX && mouseX <= quickKeyBtnX + quickKeyBtnW && mouseY >= quickKeyBtnY && mouseY <= quickKeyBtnY + quickKeyBtnH) {
            listeningForQuickAppendKey = !listeningForQuickAppendKey;
            listeningForKey = false;
            listeningForUndoKey = false;
            playClickSound();
            return true;
        }

        // undo key
        int sec11Y = quickKeyBtnY + quickKeyBtnH + secGap;
        int undoKeyBtnX = contentX;
        int undoKeyBtnY = sec11Y + labelGap;
        int undoKeyBtnW = Math.min(180, maxContentW);
        int undoKeyBtnH = Math.max(15, Math.round(15 * Math.max(1.0f, textScale)));
        if (mouseX >= undoKeyBtnX && mouseX <= undoKeyBtnX + undoKeyBtnW && mouseY >= undoKeyBtnY && mouseY <= undoKeyBtnY + undoKeyBtnH) {
            listeningForUndoKey = !listeningForUndoKey;
            listeningForKey = false;
            listeningForQuickAppendKey = false;
            playClickSound();
            return true;
        }

        // reset defaults button
        int resetBtnY = undoKeyBtnY + undoKeyBtnH + secGap + 2;
        int resetBtnW = Math.min(180, maxContentW);
        int resetBtnH = Math.max(15, Math.round(15 * Math.max(1.0f, textScale)));
        if (mouseX >= keyBtnX && mouseX <= keyBtnX + resetBtnW && mouseY >= resetBtnY && mouseY <= resetBtnY + resetBtnH) {
            resetToDefaults();
            playClickSound();
            return true;
        }

        if (listeningForKey) {
            listeningForKey = false;
        }
        if (listeningForQuickAppendKey) {
            listeningForQuickAppendKey = false;
        }
        if (listeningForUndoKey) {
            listeningForUndoKey = false;
        }

        return false;
    }

    private void updateRedFromMouse(int mouseX, int sx, int sw) {
        float norm = MathHelper.clamp((float) (mouseX - sx) / (float) sw, 0.0f, 1.0f);
        int r = Math.round(norm * 255.0f);
        ModConfig cfg = viewModel.getConfig();
        int current = cfg.getBackgroundColor();
        int newColor = (r << 16) | (current & 0x00FFFF);
        viewModel.updateConfig(c -> c.setBackgroundColor(newColor));
        if (!hexColorField.isFocused()) {
            hexColorField.setText(String.format("#%06X", newColor));
        }
    }

    private void updateGreenFromMouse(int mouseX, int sx, int sw) {
        float norm = MathHelper.clamp((float) (mouseX - sx) / (float) sw, 0.0f, 1.0f);
        int g = Math.round(norm * 255.0f);
        ModConfig cfg = viewModel.getConfig();
        int current = cfg.getBackgroundColor();
        int newColor = (current & 0xFF00FF) | (g << 8);
        viewModel.updateConfig(c -> c.setBackgroundColor(newColor));
        if (!hexColorField.isFocused()) {
            hexColorField.setText(String.format("#%06X", newColor));
        }
    }

    private void updateBlueFromMouse(int mouseX, int sx, int sw) {
        float norm = MathHelper.clamp((float) (mouseX - sx) / (float) sw, 0.0f, 1.0f);
        int b = Math.round(norm * 255.0f);
        ModConfig cfg = viewModel.getConfig();
        int current = cfg.getBackgroundColor();
        int newColor = (current & 0xFFFF00) | b;
        viewModel.updateConfig(c -> c.setBackgroundColor(newColor));
        if (!hexColorField.isFocused()) {
            hexColorField.setText(String.format("#%06X", newColor));
        }
    }

    private void updateTransparencyFromMouse(int mouseX, int sx, int sw) {
        float norm = MathHelper.clamp((float) (mouseX - sx) / (float) sw, 0.0f, 1.0f);
        viewModel.updateConfig(c -> c.setTransparency(norm));
    }

    private void updateBlurFromMouse(int mouseX, int sx, int sw) {
        float norm = MathHelper.clamp((float) (mouseX - sx) / (float) sw, 0.0f, 1.0f);
        float blur = norm * 5.0f;
        blur = Math.round(blur * 100.0f) / 100.0f;
        final float finalBlur = blur;
        viewModel.updateConfig(c -> c.setBlur(finalBlur));
    }

    private void updateScaleFromMouse(int mouseX, int sx, int sw) {
        float norm = MathHelper.clamp((float) (mouseX - sx) / (float) sw, 0.0f, 1.0f);
        float scale = 0.10f + (norm * 4.90f);
        scale = Math.round(scale * 100.0f) / 100.0f;
        final float finalScale = scale;
        viewModel.updateConfig(c -> c.setScale(finalScale));
    }

    private void updateItemScaleFromMouse(int mouseX, int sx, int sw) {
        float norm = MathHelper.clamp((float) (mouseX - sx) / (float) sw, 0.0f, 1.0f);
        float scale = 0.5f + (norm * 1.0f);
        scale = Math.round(scale * 100.0f) / 100.0f;
        final float finalScale = scale;
        viewModel.updateConfig(c -> c.setItemScale(finalScale));
    }

    private void updateTextScaleFromMouse(int mouseX, int sx, int sw) {
        float norm = MathHelper.clamp((float) (mouseX - sx) / (float) sw, 0.0f, 1.0f);
        float scale = 0.5f + (norm * 1.0f);
        scale = Math.round(scale * 100.0f) / 100.0f;
        final float finalScale = scale;
        viewModel.updateConfig(c -> c.setTextScale(finalScale));
    }

    private void updateSplitRatioFromMouse(int mouseX, int sx, int sw) {
        float norm = MathHelper.clamp((float) (mouseX - sx) / (float) sw, 0.0f, 1.0f);
        float ratio = 0.20f + (norm * 0.60f);
        ratio = Math.round(ratio * 100.0f) / 100.0f;
        final float finalRatio = ratio;
        viewModel.updateConfig(c -> c.setSplitRatio(finalRatio));
    }

    private void updateHotbarScaleFromMouse(int mouseX, int sx, int sw) {
        float norm = MathHelper.clamp((float) (mouseX - sx) / (float) sw, 0.0f, 1.0f);
        float scale = 0.50f + (norm * 1.50f);
        scale = Math.round(scale * 100.0f) / 100.0f;
        final float finalScale = scale;
        viewModel.updateConfig(c -> c.setHotbarScale(finalScale));
    }

    private void updateHotbarItemScaleFromMouse(int mouseX, int sx, int sw) {
        float norm = MathHelper.clamp((float) (mouseX - sx) / (float) sw, 0.0f, 1.0f);
        float scale = 0.50f + (norm * 1.00f);
        scale = Math.round(scale * 100.0f) / 100.0f;
        final float finalScale = scale;
        viewModel.updateConfig(c -> c.setHotbarItemScale(finalScale));
    }

    private void updatePaletteScaleFromMouse(int mouseX, int sx, int sw) {
        float norm = MathHelper.clamp((float) (mouseX - sx) / (float) sw, 0.0f, 1.0f);
        float scale = 0.50f + (norm * 1.50f);
        scale = Math.round(scale * 100.0f) / 100.0f;
        final float finalScale = scale;
        viewModel.updateConfig(c -> c.setPaletteScale(finalScale));
    }

    private void updatePaletteItemScaleFromMouse(int mouseX, int sx, int sw) {
        float norm = MathHelper.clamp((float) (mouseX - sx) / (float) sw, 0.0f, 1.0f);
        float scale = 0.50f + (norm * 1.00f);
        scale = Math.round(scale * 100.0f) / 100.0f;
        final float finalScale = scale;
        viewModel.updateConfig(c -> c.setPaletteItemScale(finalScale));
    }

    private void resetToDefaults() {
        viewModel.updateConfig(c -> {
            c.setBackgroundColor(0x101010);
            c.setTransparency(0.60f);
            c.setBlur(2.50f);
            c.setScale(1.0f);
            c.setItemScale(1.0f);
            c.setTextScale(1.0f);
            c.setSplitRatio(0.50f);
            c.setHotbarScale(1.00f);
            c.setHotbarItemScale(1.00f);
            c.setPaletteScale(1.00f);
            c.setPaletteItemScale(1.00f);
            c.setKeyOpenClose("key.keyboard.o");
            c.setKeyQuickAppend("key.keyboard.a");
            c.setKeyUndo("key.keyboard.z");
        });
        hexColorField.setText("#101010");
        hexErrorMessage = "";

        KeyBinding binding = ItemOrganizerClient.getOpenKeyBinding();
        if (binding != null) {
            binding.setBoundKey(InputUtil.fromKeyCode(new KeyInput(GLFW.GLFW_KEY_O, 0, 0)));
            KeyBinding.updateKeysByCode();
            MinecraftClient.getInstance().options.write();
        }
    }

    @Override
    public boolean mouseReleased(Click click) {
        draggingRed = false;
        draggingGreen = false;
        draggingBlue = false;
        draggingTransparency = false;
        draggingBlur = false;
        draggingScale = false;
        draggingItemScale = false;
        draggingTextScale = false;
        draggingSplitRatio = false;
        draggingHotbarScale = false;
        draggingHotbarItemScale = false;
        draggingPaletteScale = false;
        draggingPaletteItemScale = false;
        scrollbar.mouseReleased(click);
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (scrollbar.mouseDragged(click, deltaX, deltaY)) {
            return true;
        }

        int mouseX = (int) click.x();
        int contentX = x + 10;
        int maxContentW = width - SCROLLBAR_WIDTH - 24;
        int sliderW = Math.min(180, maxContentW);

        if (draggingRed) {
            updateRedFromMouse(mouseX, contentX, sliderW);
            return true;
        }
        if (draggingGreen) {
            updateGreenFromMouse(mouseX, contentX, sliderW);
            return true;
        }
        if (draggingBlue) {
            updateBlueFromMouse(mouseX, contentX, sliderW);
            return true;
        }
        if (draggingTransparency) {
            updateTransparencyFromMouse(mouseX, contentX, sliderW);
            return true;
        }
        if (draggingBlur) {
            updateBlurFromMouse(mouseX, contentX, sliderW);
            return true;
        }
        if (draggingScale) {
            updateScaleFromMouse(mouseX, contentX, sliderW);
            return true;
        }
        if (draggingItemScale) {
            updateItemScaleFromMouse(mouseX, contentX, sliderW);
            return true;
        }
        if (draggingTextScale) {
            updateTextScaleFromMouse(mouseX, contentX, sliderW);
            return true;
        }
        if (draggingSplitRatio) {
            updateSplitRatioFromMouse(mouseX, contentX, sliderW);
            return true;
        }
        if (draggingHotbarScale) {
            updateHotbarScaleFromMouse(mouseX, contentX, sliderW);
            return true;
        }
        if (draggingHotbarItemScale) {
            updateHotbarItemScaleFromMouse(mouseX, contentX, sliderW);
            return true;
        }
        if (draggingPaletteScale) {
            updatePaletteScaleFromMouse(mouseX, contentX, sliderW);
            return true;
        }
        if (draggingPaletteItemScale) {
            updatePaletteItemScaleFromMouse(mouseX, contentX, sliderW);
            return true;
        }

        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            scrollbar.scroll(-verticalAmount * 18);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (listeningForKey) {
            if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
                listeningForKey = false;
                playClickSound();
                return true;
            }

            InputUtil.Key newKey = InputUtil.fromKeyCode(input);
            KeyBinding binding = ItemOrganizerClient.getOpenKeyBinding();
            if (binding != null && newKey != null) {
                binding.setBoundKey(newKey);
                KeyBinding.updateKeysByCode();
                MinecraftClient.getInstance().options.write();

                String translationKey = newKey.getTranslationKey();
                viewModel.updateConfig(c -> c.setKeyOpenClose(translationKey));
            }

            listeningForKey = false;
            playClickSound();
            return true;
        }

        if (listeningForQuickAppendKey) {
            if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
                listeningForQuickAppendKey = false;
                playClickSound();
                return true;
            }

            InputUtil.Key newKey = InputUtil.fromKeyCode(input);
            if (newKey != null) {
                String translationKey = newKey.getTranslationKey();
                viewModel.updateConfig(c -> c.setKeyQuickAppend(translationKey));
            }

            listeningForQuickAppendKey = false;
            playClickSound();
            return true;
        }

        if (listeningForUndoKey) {
            if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
                listeningForUndoKey = false;
                playClickSound();
                return true;
            }

            InputUtil.Key newKey = InputUtil.fromKeyCode(input);
            if (newKey != null) {
                String translationKey = newKey.getTranslationKey();
                viewModel.updateConfig(c -> c.setKeyUndo(translationKey));
            }

            listeningForUndoKey = false;
            playClickSound();
            return true;
        }

        if (hexColorField.isFocused()) {
            return hexColorField.keyPressed(input);
        }

        return false;
    }

    public boolean charTyped(CharInput input) {
        if (hexColorField.isFocused()) {
            return hexColorField.charTyped(input);
        }
        return false;
    }

    @Override
    public void setFocused(boolean focused) {
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {
    }

    @Override
    public SelectionType getType() {
        return SelectionType.NONE;
    }
}
