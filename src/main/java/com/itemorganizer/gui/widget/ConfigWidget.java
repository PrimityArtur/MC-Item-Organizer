package com.itemorganizer.gui.widget;

import java.util.ArrayList;
import java.util.List;

import com.itemorganizer.client.ItemOrganizerClient;
import com.itemorganizer.core.model.ModConfig;
import com.itemorganizer.gui.component.ModalDialogComponent;
import com.itemorganizer.gui.component.ScrollbarComponent;
import com.itemorganizer.gui.component.SliderComponent;
import com.itemorganizer.gui.theme.UITheme;
import com.itemorganizer.gui.util.RenderHelper;
import com.itemorganizer.gui.util.SoundHelper;
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
import net.minecraft.client.util.InputUtil;
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

    private final ScrollbarComponent scrollbar;
    private ModalDialogComponent activeModal = null;

    // hex color input field
    private TextFieldWidget hexColorField;
    private String hexErrorMessage = "";

    // sliders
    private final List<SliderComponent> allSliders = new ArrayList<>();
    private SliderComponent redSlider;
    private SliderComponent greenSlider;
    private SliderComponent blueSlider;
    private SliderComponent transparencySlider;
    private SliderComponent blurSlider;
    private SliderComponent scaleSlider;
    private SliderComponent itemScaleSlider;
    private SliderComponent textScaleSlider;
    private SliderComponent splitRatioSlider;
    private SliderComponent hotbarScaleSlider;
    private SliderComponent hotbarItemScaleSlider;
    private SliderComponent paletteScaleSlider;
    private SliderComponent paletteItemScaleSlider;

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
        this.scrollbar = new ScrollbarComponent(x + width - SCROLLBAR_WIDTH - 4, listStartY, SCROLLBAR_WIDTH, listHeight);

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

        initSliders();
    }

    private void initSliders() {
        redSlider = new SliderComponent(Text.literal("R"), 0.0, 255.0,
                () -> (double) ((viewModel.getConfig().getBackgroundColor() >> 16) & 0xFF),
                v -> {
                    int current = viewModel.getConfig().getBackgroundColor();
                    int newColor = (v.intValue() << 16) | (current & 0x00FFFF);
                    viewModel.updateConfig(c -> c.setBackgroundColor(newColor));
                    if (!hexColorField.isFocused()) hexColorField.setText(String.format("#%06X", newColor));
                },
                v -> String.valueOf(v.intValue()));
        redSlider.setColors(0x40000000, 0x66EF4444, 0x25FFFFFF, 0xFFEF4444);

        greenSlider = new SliderComponent(Text.literal("G"), 0.0, 255.0,
                () -> (double) ((viewModel.getConfig().getBackgroundColor() >> 8) & 0xFF),
                v -> {
                    int current = viewModel.getConfig().getBackgroundColor();
                    int newColor = (current & 0xFF00FF) | (v.intValue() << 8);
                    viewModel.updateConfig(c -> c.setBackgroundColor(newColor));
                    if (!hexColorField.isFocused()) hexColorField.setText(String.format("#%06X", newColor));
                },
                v -> String.valueOf(v.intValue()));
        greenSlider.setColors(0x40000000, 0x6610B981, 0x25FFFFFF, 0xFF34D399);

        blueSlider = new SliderComponent(Text.literal("B"), 0.0, 255.0,
                () -> (double) (viewModel.getConfig().getBackgroundColor() & 0xFF),
                v -> {
                    int current = viewModel.getConfig().getBackgroundColor();
                    int newColor = (current & 0xFFFF00) | v.intValue();
                    viewModel.updateConfig(c -> c.setBackgroundColor(newColor));
                    if (!hexColorField.isFocused()) hexColorField.setText(String.format("#%06X", newColor));
                },
                v -> String.valueOf(v.intValue()));
        blueSlider.setColors(0x40000000, 0x663B82F6, 0x25FFFFFF, 0xFF38BDF8);

        transparencySlider = new SliderComponent(null, 0.10, 1.0,
                () -> (double) viewModel.getConfig().getTransparency(),
                v -> viewModel.updateConfig(c -> c.setTransparency(v.floatValue())),
                v -> Math.round(v * 100.0) + "%");
        transparencySlider.setColors(0x40000000, 0x4D38BDF8, 0x25FFFFFF, 0xFF38BDF8);

        blurSlider = new SliderComponent(null, 0.0, 5.0,
                () -> (double) viewModel.getConfig().getBlur(),
                v -> viewModel.updateConfig(c -> c.setBlur(Math.round(v.floatValue() * 100.0f) / 100.0f)),
                v -> Math.round((v / 5.0) * 100.0) + "%");
        blurSlider.setColors(0x40000000, 0x4D38BDF8, 0x25FFFFFF, 0xFF38BDF8);

        scaleSlider = new SliderComponent(null, 0.10, 5.00,
                () -> (double) viewModel.getConfig().getScale(),
                v -> viewModel.updateConfig(c -> c.setScale(Math.round(v.floatValue() * 100.0f) / 100.0f)),
                v -> Math.round(v * 100.0) + "% (" + String.format("%.2f", v) + "x)");
        scaleSlider.setColors(0x40000000, 0x4D38BDF8, 0x25FFFFFF, 0xFF38BDF8);

        itemScaleSlider = new SliderComponent(null, 0.50, 1.50,
                () -> (double) viewModel.getConfig().getItemScale(),
                v -> viewModel.updateConfig(c -> c.setItemScale(Math.round(v.floatValue() * 100.0f) / 100.0f)),
                v -> Math.round(v * 100.0) + "% (" + String.format("%.2f", v) + "x)");
        itemScaleSlider.setColors(0x40000000, 0x4D38BDF8, 0x25FFFFFF, 0xFF38BDF8);

        textScaleSlider = new SliderComponent(null, 0.50, 1.50,
                () -> (double) viewModel.getConfig().getTextScale(),
                v -> viewModel.updateConfig(c -> c.setTextScale(Math.round(v.floatValue() * 100.0f) / 100.0f)),
                v -> Math.round(v * 100.0) + "% (" + String.format("%.2f", v) + "x)");
        textScaleSlider.setColors(0x40000000, 0x4D38BDF8, 0x25FFFFFF, 0xFF38BDF8);

        splitRatioSlider = new SliderComponent(null, 0.20, 0.80,
                () -> (double) viewModel.getConfig().getSplitRatio(),
                v -> viewModel.updateConfig(c -> c.setSplitRatio(Math.round(v.floatValue() * 100.0f) / 100.0f)),
                v -> Math.round(v * 100.0) + "% / " + (100 - Math.round(v * 100.0)) + "%");
        splitRatioSlider.setColors(0x40000000, 0x4D38BDF8, 0x25FFFFFF, 0xFF38BDF8);

        hotbarScaleSlider = new SliderComponent(null, 0.50, 2.00,
                () -> (double) viewModel.getConfig().getHotbarScale(),
                v -> viewModel.updateConfig(c -> c.setHotbarScale(Math.round(v.floatValue() * 100.0f) / 100.0f)),
                v -> Math.round(v * 100.0) + "% (" + String.format("%.2f", v) + "x)");
        hotbarScaleSlider.setColors(0x40000000, 0x4D38BDF8, 0x25FFFFFF, 0xFF38BDF8);

        hotbarItemScaleSlider = new SliderComponent(null, 0.50, 1.50,
                () -> (double) viewModel.getConfig().getHotbarItemScale(),
                v -> viewModel.updateConfig(c -> c.setHotbarItemScale(Math.round(v.floatValue() * 100.0f) / 100.0f)),
                v -> Math.round(v * 100.0) + "% (" + String.format("%.2f", v) + "x)");
        hotbarItemScaleSlider.setColors(0x40000000, 0x4D38BDF8, 0x25FFFFFF, 0xFF38BDF8);

        paletteScaleSlider = new SliderComponent(null, 0.50, 2.00,
                () -> (double) viewModel.getConfig().getPaletteScale(),
                v -> viewModel.updateConfig(c -> c.setPaletteScale(Math.round(v.floatValue() * 100.0f) / 100.0f)),
                v -> Math.round(v * 100.0) + "% (" + String.format("%.2f", v) + "x)");
        paletteScaleSlider.setColors(0x40000000, 0x4D38BDF8, 0x25FFFFFF, 0xFF38BDF8);

        paletteItemScaleSlider = new SliderComponent(null, 0.50, 1.50,
                () -> (double) viewModel.getConfig().getPaletteItemScale(),
                v -> viewModel.updateConfig(c -> c.setPaletteItemScale(Math.round(v.floatValue() * 100.0f) / 100.0f)),
                v -> Math.round(v * 100.0) + "% (" + String.format("%.2f", v) + "x)");
        paletteItemScaleSlider.setColors(0x40000000, 0x4D38BDF8, 0x25FFFFFF, 0xFF38BDF8);

        allSliders.clear();
        allSliders.add(redSlider);
        allSliders.add(greenSlider);
        allSliders.add(blueSlider);
        allSliders.add(transparencySlider);
        allSliders.add(blurSlider);
        allSliders.add(scaleSlider);
        allSliders.add(itemScaleSlider);
        allSliders.add(textScaleSlider);
        allSliders.add(splitRatioSlider);
        allSliders.add(hotbarScaleSlider);
        allSliders.add(hotbarItemScaleSlider);
        allSliders.add(paletteScaleSlider);
        allSliders.add(paletteItemScaleSlider);
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
            redSlider.setBounds(contentX, sliderRedY, sliderW, rgbSliderH);
            redSlider.render(context, tr, mouseX, mouseY, textScale);
        }

        int sliderGreenY = sliderRedY + rgbSliderH + 3;
        if (sliderGreenY + rgbSliderH >= listStartY && sliderGreenY <= listStartY + listHeight) {
            greenSlider.setBounds(contentX, sliderGreenY, sliderW, rgbSliderH);
            greenSlider.render(context, tr, mouseX, mouseY, textScale);
        }

        int sliderBlueY = sliderGreenY + rgbSliderH + 3;
        if (sliderBlueY + rgbSliderH >= listStartY && sliderBlueY <= listStartY + listHeight) {
            blueSlider.setBounds(contentX, sliderBlueY, sliderW, rgbSliderH);
            blueSlider.render(context, tr, mouseX, mouseY, textScale);
        }

        // transparency slider
        int sec2Y = sliderBlueY + rgbSliderH + secGap;
        if (sec2Y + 12 >= listStartY && sec2Y <= listStartY + listHeight) {
            TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.transparency"), contentX, sec2Y, 0xFF38BDF8, true, textScale);
        }

        int slider1X = contentX;
        int slider1Y = sec2Y + labelGap;
        if (slider1Y + sliderH >= listStartY && slider1Y <= listStartY + listHeight) {
            transparencySlider.setBounds(slider1X, slider1Y, sliderW, sliderH);
            transparencySlider.render(context, tr, mouseX, mouseY, textScale);
        }

        // blur slider
        int secBlurY = slider1Y + sliderH + secGap;
        if (secBlurY + 12 >= listStartY && secBlurY <= listStartY + listHeight) {
            TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.blur"), contentX, secBlurY, 0xFF38BDF8, true, textScale);
        }

        int sliderBlurX = contentX;
        int sliderBlurY = secBlurY + labelGap;
        if (sliderBlurY + sliderH >= listStartY && sliderBlurY <= listStartY + listHeight) {
            blurSlider.setBounds(sliderBlurX, sliderBlurY, sliderW, sliderH);
            blurSlider.render(context, tr, mouseX, mouseY, textScale);
        }

        // grid zoom slider
        int sec3Y = sliderBlurY + sliderH + secGap;
        if (sec3Y + 12 >= listStartY && sec3Y <= listStartY + listHeight) {
            TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.grid_zoom"), contentX, sec3Y, 0xFF38BDF8, true, textScale);
        }

        int slider2X = contentX;
        int slider2Y = sec3Y + labelGap;
        if (slider2Y + sliderH >= listStartY && slider2Y <= listStartY + listHeight) {
            scaleSlider.setBounds(slider2X, slider2Y, sliderW, sliderH);
            scaleSlider.render(context, tr, mouseX, mouseY, textScale);
        }

        // item scale slider
        int sec4Y = slider2Y + sliderH + secGap;
        if (sec4Y + 12 >= listStartY && sec4Y <= listStartY + listHeight) {
            TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.item_scale"), contentX, sec4Y, 0xFF38BDF8, true, textScale);
        }

        int slider3X = contentX;
        int slider3Y = sec4Y + labelGap;
        if (slider3Y + sliderH >= listStartY && slider3Y <= listStartY + listHeight) {
            itemScaleSlider.setBounds(slider3X, slider3Y, sliderW, sliderH);
            itemScaleSlider.render(context, tr, mouseX, mouseY, textScale);
        }

        // text scale slider
        int sec5Y = slider3Y + sliderH + secGap;
        if (sec5Y + 12 >= listStartY && sec5Y <= listStartY + listHeight) {
            TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.text_scale"), contentX, sec5Y, 0xFF38BDF8, true, textScale);
        }

        int slider4X = contentX;
        int slider4Y = sec5Y + labelGap;
        if (slider4Y + sliderH >= listStartY && slider4Y <= listStartY + listHeight) {
            textScaleSlider.setBounds(slider4X, slider4Y, sliderW, sliderH);
            textScaleSlider.render(context, tr, mouseX, mouseY, textScale);
        }

        // panel split ratio slider
        int sec6Y = slider4Y + sliderH + secGap;
        if (sec6Y + 12 >= listStartY && sec6Y <= listStartY + listHeight) {
            TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.panel_split"), contentX, sec6Y, 0xFF38BDF8, true, textScale);
        }

        int slider5X = contentX;
        int slider5Y = sec6Y + labelGap;
        if (slider5Y + sliderH >= listStartY && slider5Y <= listStartY + listHeight) {
            splitRatioSlider.setBounds(slider5X, slider5Y, sliderW, sliderH);
            splitRatioSlider.render(context, tr, mouseX, mouseY, textScale);
        }

        // hotbar scale slider
        int sec7Y = slider5Y + sliderH + secGap;
        if (sec7Y + 12 >= listStartY && sec7Y <= listStartY + listHeight) {
            TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.hotbar_scale"), contentX, sec7Y, 0xFF38BDF8, true, textScale);
        }

        int slider6X = contentX;
        int slider6Y = sec7Y + labelGap;
        if (slider6Y + sliderH >= listStartY && slider6Y <= listStartY + listHeight) {
            hotbarScaleSlider.setBounds(slider6X, slider6Y, sliderW, sliderH);
            hotbarScaleSlider.render(context, tr, mouseX, mouseY, textScale);
        }

        // hotbar item scale slider
        int sec7bY = slider6Y + sliderH + secGap;
        if (sec7bY + 12 >= listStartY && sec7bY <= listStartY + listHeight) {
            TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.hotbar_item_scale"), contentX, sec7bY, 0xFF38BDF8, true, textScale);
        }

        int slider6bX = contentX;
        int slider6bY = sec7bY + labelGap;
        if (slider6bY + sliderH >= listStartY && slider6bY <= listStartY + listHeight) {
            hotbarItemScaleSlider.setBounds(slider6bX, slider6bY, sliderW, sliderH);
            hotbarItemScaleSlider.render(context, tr, mouseX, mouseY, textScale);
        }

        // palette scale slider
        int sec8Y = slider6bY + sliderH + secGap;
        if (sec8Y + 12 >= listStartY && sec8Y <= listStartY + listHeight) {
            TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.palette_scale"), contentX, sec8Y, 0xFF38BDF8, true, textScale);
        }

        int slider7X = contentX;
        int slider7Y = sec8Y + labelGap;
        if (slider7Y + sliderH >= listStartY && slider7Y <= listStartY + listHeight) {
            paletteScaleSlider.setBounds(slider7X, slider7Y, sliderW, sliderH);
            paletteScaleSlider.render(context, tr, mouseX, mouseY, textScale);
        }

        // palette item scale slider
        int sec8bY = slider7Y + sliderH + secGap;
        if (sec8bY + 12 >= listStartY && sec8bY <= listStartY + listHeight) {
            TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.palette_item_scale"), contentX, sec8bY, 0xFF38BDF8, true, textScale);
        }

        int slider7bX = contentX;
        int slider7bY = sec8bY + labelGap;
        if (slider7bY + sliderH >= listStartY && slider7bY <= listStartY + listHeight) {
            paletteItemScaleSlider.setBounds(slider7bX, slider7bY, sliderW, sliderH);
            paletteItemScaleSlider.render(context, tr, mouseX, mouseY, textScale);
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
            boolean hoverKeyBtn = (activeModal == null && mouseX >= keyBtnX && mouseX <= keyBtnX + keyBtnW && mouseY >= keyBtnY && mouseY <= keyBtnY + keyBtnH);
            Text keyLabel;
            int keyBg, keyHoverBg, keyBorder, keyHoverBorder, keyTextColor, keyHoverTextColor;

            if (listeningForKey) {
                keyLabel = Text.translatable("config.itemorganizer.press_key");
                keyBg = UITheme.WARNING_BG;
                keyHoverBg = UITheme.WARNING_HOVER_BG;
                keyBorder = UITheme.WARNING_BORDER;
                keyHoverBorder = UITheme.WARNING_BORDER;
                keyTextColor = UITheme.WARNING;
                keyHoverTextColor = UITheme.TEXT_WHITE;
            } else {
                KeyBinding binding = ItemOrganizerClient.getOpenKeyBinding();
                Text keyText = (binding != null) ? binding.getBoundKeyLocalizedText() : Text.literal("O");
                keyLabel = Text.translatable("config.itemorganizer.key_label", keyText.getString());
                keyBg = UITheme.BG_SURFACE_HOVER;
                keyHoverBg = UITheme.PRIMARY_BG;
                keyBorder = UITheme.BORDER_MUTED;
                keyHoverBorder = UITheme.PRIMARY;
                keyTextColor = UITheme.TEXT_SECONDARY;
                keyHoverTextColor = UITheme.TEXT_WHITE;
            }

            RenderHelper.drawButton(context, tr, keyBtnX, keyBtnY, keyBtnW, keyBtnH, keyLabel, hoverKeyBtn,
                    keyBg, keyHoverBg, keyBorder, keyHoverBorder, keyTextColor, keyHoverTextColor, textScale);
        }

        // quick append key binding
        int sec10Y = keyBtnY + keyBtnH + secGap;
        if (sec10Y + 12 >= listStartY && sec10Y <= listStartY + listHeight) {
            TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.quick_append_key"), contentX, sec10Y, UITheme.PRIMARY, true, textScale);
        }

        int quickKeyBtnX = contentX;
        int quickKeyBtnY = sec10Y + labelGap;
        int quickKeyBtnW = Math.min(180, maxContentW);
        int quickKeyBtnH = Math.max(15, Math.round(15 * Math.max(1.0f, textScale)));

        if (quickKeyBtnY + quickKeyBtnH >= listStartY && quickKeyBtnY <= listStartY + listHeight) {
            boolean hoverQuickKey = (activeModal == null && mouseX >= quickKeyBtnX && mouseX <= quickKeyBtnX + quickKeyBtnW && mouseY >= quickKeyBtnY && mouseY <= quickKeyBtnY + quickKeyBtnH);
            Text qkLabel;
            int qkBg, qkHoverBg, qkBorder, qkHoverBorder, qkTextColor, qkHoverTextColor;

            if (listeningForQuickAppendKey) {
                qkLabel = Text.translatable("config.itemorganizer.press_key");
                qkBg = UITheme.WARNING_BG;
                qkHoverBg = UITheme.WARNING_HOVER_BG;
                qkBorder = UITheme.WARNING_BORDER;
                qkHoverBorder = UITheme.WARNING_BORDER;
                qkTextColor = UITheme.WARNING;
                qkHoverTextColor = UITheme.TEXT_WHITE;
            } else {
                String boundKey = cfg.getKeyQuickAppend();
                InputUtil.Key k = InputUtil.fromTranslationKey(boundKey);
                String keyName = (k != null) ? k.getLocalizedText().getString() : "A";
                qkLabel = Text.translatable("config.itemorganizer.key_label", keyName);
                qkBg = UITheme.BG_SURFACE_HOVER;
                qkHoverBg = UITheme.PRIMARY_BG;
                qkBorder = UITheme.BORDER_MUTED;
                qkHoverBorder = UITheme.PRIMARY;
                qkTextColor = UITheme.TEXT_SECONDARY;
                qkHoverTextColor = UITheme.TEXT_WHITE;
            }

            RenderHelper.drawButton(context, tr, quickKeyBtnX, quickKeyBtnY, quickKeyBtnW, quickKeyBtnH, qkLabel, hoverQuickKey,
                    qkBg, qkHoverBg, qkBorder, qkHoverBorder, qkTextColor, qkHoverTextColor, textScale);
        }

        // undo key binding
        int sec11Y = quickKeyBtnY + quickKeyBtnH + secGap;
        if (sec11Y + 12 >= listStartY && sec11Y <= listStartY + listHeight) {
            TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.undo_key"), contentX, sec11Y, UITheme.PRIMARY, true, textScale);
        }

        int undoKeyBtnX = contentX;
        int undoKeyBtnY = sec11Y + labelGap;
        int undoKeyBtnW = Math.min(180, maxContentW);
        int undoKeyBtnH = Math.max(15, Math.round(15 * Math.max(1.0f, textScale)));

        if (undoKeyBtnY + undoKeyBtnH >= listStartY && undoKeyBtnY <= listStartY + listHeight) {
            boolean hoverUndoKey = (activeModal == null && mouseX >= undoKeyBtnX && mouseX <= undoKeyBtnX + undoKeyBtnW && mouseY >= undoKeyBtnY && mouseY <= undoKeyBtnY + undoKeyBtnH);
            Text undoLabel;
            int undoBg, undoHoverBg, undoBorder, undoHoverBorder, undoTextColor, undoHoverTextColor;

            if (listeningForUndoKey) {
                undoLabel = Text.translatable("config.itemorganizer.press_key");
                undoBg = UITheme.WARNING_BG;
                undoHoverBg = UITheme.WARNING_HOVER_BG;
                undoBorder = UITheme.WARNING_BORDER;
                undoHoverBorder = UITheme.WARNING_BORDER;
                undoTextColor = UITheme.WARNING;
                undoHoverTextColor = UITheme.TEXT_WHITE;
            } else {
                String boundKey = cfg.getKeyUndo();
                InputUtil.Key k = InputUtil.fromTranslationKey(boundKey);
                String keyName = (k != null) ? k.getLocalizedText().getString().toUpperCase() : "Z";
                undoLabel = Text.translatable("config.itemorganizer.key_label_ctrl", keyName);
                undoBg = UITheme.BG_SURFACE_HOVER;
                undoHoverBg = UITheme.PRIMARY_BG;
                undoBorder = UITheme.BORDER_MUTED;
                undoHoverBorder = UITheme.PRIMARY;
                undoTextColor = UITheme.TEXT_SECONDARY;
                undoHoverTextColor = UITheme.TEXT_WHITE;
            }

            RenderHelper.drawButton(context, tr, undoKeyBtnX, undoKeyBtnY, undoKeyBtnW, undoKeyBtnH, undoLabel, hoverUndoKey,
                    undoBg, undoHoverBg, undoBorder, undoHoverBorder, undoTextColor, undoHoverTextColor, textScale);
        }

        // reset defaults button
        int resetBtnY = undoKeyBtnY + undoKeyBtnH + secGap + 2;
        int resetBtnW = Math.min(180, maxContentW);
        int resetBtnH = Math.max(15, Math.round(15 * Math.max(1.0f, textScale)));

        if (resetBtnY + resetBtnH >= listStartY && resetBtnY <= listStartY + listHeight) {
            boolean hoverReset = (activeModal == null && mouseX >= keyBtnX && mouseX <= keyBtnX + resetBtnW && mouseY >= resetBtnY && mouseY <= resetBtnY + resetBtnH);
            RenderHelper.drawButton(context, tr, keyBtnX, resetBtnY, resetBtnW, resetBtnH,
                    Text.translatable("config.itemorganizer.reset_defaults"), hoverReset,
                    UITheme.DANGER_BG, UITheme.DANGER_HOVER_BG, UITheme.DANGER_BORDER_MUTED, UITheme.DANGER,
                    0xFFFCA5A5, UITheme.TEXT_WHITE, textScale);
        }

        context.disableScissor();
        scrollbar.render(context, mouseX, mouseY);

        // confirmation modal rendering
        if (activeModal != null) {
            activeModal.updateParentBounds(x, y, width, height);
            activeModal.render(context, tr, mouseX, mouseY, delta, textScale);
        }
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

        // sliders
        for (SliderComponent s : allSliders) {
            if (s.mouseClicked(click)) {
                return true;
            }
        }

        // open/close key
        int sec9Y = paletteItemScaleSlider.getY() + paletteItemScaleSlider.getHeight() + secGap;
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
            activeModal = ModalDialogComponent.builder()
                    .parentBounds(x, y, width, height)
                    .size(Math.min(240, width - 20), 96)
                    .type(ModalDialogComponent.ModalType.DANGER)
                    .title(Text.translatable("config.itemorganizer.reset_defaults.confirm_title"))
                    .message(Text.translatable("config.itemorganizer.reset_defaults.confirm_desc"))
                    .warning(Text.translatable("config.itemorganizer.reset_defaults.warning"))
                    .confirmButton(Text.translatable("button.itemorganizer.confirm"), () -> {
                        resetToDefaults();
                        activeModal = null;
                    })
                    .cancelButton(Text.translatable("button.itemorganizer.cancel"), () -> activeModal = null)
                    .closeOnBackdropClick(true)
                    .build();
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
        for (SliderComponent s : allSliders) {
            s.stopDragging();
        }
        scrollbar.mouseReleased(click);
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (scrollbar.mouseDragged(click, deltaX, deltaY)) {
            return true;
        }

        for (SliderComponent s : allSliders) {
            if (s.mouseDragged(click, deltaX, deltaY)) {
                return true;
            }
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
        if (activeModal != null) {
            return activeModal.keyPressed(input);
        }

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
        if (activeModal != null) {
            return activeModal.charTyped(input);
        }

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

    public boolean isEditingOrSearching() {
        return activeModal != null || (hexColorField != null && hexColorField.isFocused()) || listeningForKey || listeningForQuickAppendKey || listeningForUndoKey;
    }
}
