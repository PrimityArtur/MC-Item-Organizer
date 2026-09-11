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
    private SliderComponent paletteButtonScaleSlider;
    private SliderComponent createPaletteScaleSlider;
    private SliderComponent createPaletteItemScaleSlider;
    private SliderComponent createPaletteButtonScaleSlider;

    // key listening state
    private boolean listeningForKey = false;
    private boolean listeningForQuickAppendKey = false;
    private boolean listeningForUndoKey = false;
    private boolean listeningForRedoKey = false;

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

        paletteButtonScaleSlider = new SliderComponent(null, 0.50, 2.00,
                () -> (double) viewModel.getConfig().getPaletteButtonScale(),
                v -> viewModel.updateConfig(c -> c.setPaletteButtonScale(Math.round(v.floatValue() * 100.0f) / 100.0f)),
                v -> Math.round(v * 100.0) + "% (" + String.format("%.2f", v) + "x)");
        paletteButtonScaleSlider.setColors(0x40000000, 0x4D38BDF8, 0x25FFFFFF, 0xFF38BDF8);

        createPaletteScaleSlider = new SliderComponent(null, 0.50, 2.00,
                () -> (double) viewModel.getConfig().getCreatePaletteScale(),
                v -> viewModel.updateConfig(c -> c.setCreatePaletteScale(Math.round(v.floatValue() * 100.0f) / 100.0f)),
                v -> Math.round(v * 100.0) + "% (" + String.format("%.2f", v) + "x)");
        createPaletteScaleSlider.setColors(0x40000000, 0x4D38BDF8, 0x25FFFFFF, 0xFF38BDF8);

        createPaletteItemScaleSlider = new SliderComponent(null, 0.50, 1.50,
                () -> (double) viewModel.getConfig().getCreatePaletteItemScale(),
                v -> viewModel.updateConfig(c -> c.setCreatePaletteItemScale(Math.round(v.floatValue() * 100.0f) / 100.0f)),
                v -> Math.round(v * 100.0) + "% (" + String.format("%.2f", v) + "x)");
        createPaletteItemScaleSlider.setColors(0x40000000, 0x4D38BDF8, 0x25FFFFFF, 0xFF38BDF8);

        createPaletteButtonScaleSlider = new SliderComponent(null, 0.50, 2.00,
                () -> (double) viewModel.getConfig().getCreatePaletteButtonScale(),
                v -> viewModel.updateConfig(c -> c.setCreatePaletteButtonScale(Math.round(v.floatValue() * 100.0f) / 100.0f)),
                v -> Math.round(v * 100.0) + "% (" + String.format("%.2f", v) + "x)");
        createPaletteButtonScaleSlider.setColors(0x40000000, 0x4D38BDF8, 0x25FFFFFF, 0xFF38BDF8);

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
        allSliders.add(paletteButtonScaleSlider);
        allSliders.add(createPaletteScaleSlider);
        allSliders.add(createPaletteItemScaleSlider);
        allSliders.add(createPaletteButtonScaleSlider);
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

    private void renderBlockCard(DrawContext context, TextRenderer tr, int cardX, int blockY, int cardW, int blockH, Text title, float textScale) {
        int headerH = Math.max(14, Math.round(14 * textScale));
        context.fill(cardX, blockY, cardX + cardW, blockY + blockH, 0x1A0A0E17);
        RenderHelper.drawBorder(context, cardX, blockY, cardW, blockH, 0x2638BDF8);
        context.fill(cardX, blockY, cardX + cardW, blockY + headerH, 0x2A1E293B);
        context.fill(cardX, blockY + headerH, cardX + cardW, blockY + headerH + 1, 0x2638BDF8);
        TextScaleHelper.drawScaledText(context, tr, title, cardX + 6, blockY + 3, 0xFF38BDF8, true, textScale);
    }

    private int getBlock1Height(float textScale, int maxContentW) {
        int guideH = getShortcutGuideHeight(textScale, maxContentW);
        if (guideH <= 0) return 0;
        int headerH = Math.max(14, Math.round(14 * textScale));
        return headerH + 6 + guideH;
    }

    private int getBlock2Height(float textScale, int cardW) {
        int headerH = Math.max(14, Math.round(14 * textScale));
        int labelGap = Math.max(10, Math.round(8 * textScale) + 2);
        int sliderH = Math.max(12, Math.round(12 * Math.max(1.0f, textScale)));
        int rgbSliderH = 11;
        int swatchSize = 14;
        int chipW = 16;
        int chipH = 14;

        int presetStartX = (x + 14) + swatchSize + 4 + 54 + 6;
        boolean presetsInline = (presetStartX + (PRESETS.length * (chipW + 3)) <= (x + 6) + cardW - 6);
        int presetRowH = presetsInline ? 0 : chipH + 4;

        int bgH = 12 + labelGap + swatchSize + 4 + presetRowH + (3 * (rgbSliderH + 3)) + 6;
        int transpH = 12 + labelGap + sliderH + 6;
        int blurH = 12 + labelGap + sliderH + 6;
        int splitH = 12 + labelGap + sliderH + 6;
        int textH = 12 + labelGap + sliderH + 6;

        return headerH + 6 + bgH + transpH + blurH + splitH + textH + 4;
    }

    private int getBlock3Height(float textScale) {
        int headerH = Math.max(14, Math.round(14 * textScale));
        int labelGap = Math.max(10, Math.round(8 * textScale) + 2);
        int sliderH = Math.max(12, Math.round(12 * Math.max(1.0f, textScale)));
        int itemH = 12 + labelGap + sliderH + 6;
        return headerH + 6 + (2 * itemH) + 4;
    }

    private int getBlock4Height(float textScale) {
        int headerH = Math.max(14, Math.round(14 * textScale));
        int labelGap = Math.max(10, Math.round(8 * textScale) + 2);
        int sliderH = Math.max(12, Math.round(12 * Math.max(1.0f, textScale)));
        int itemH = 12 + labelGap + sliderH + 6;
        return headerH + 6 + (2 * itemH) + 4;
    }

    private int getBlock5Height(float textScale) {
        int headerH = Math.max(14, Math.round(14 * textScale));
        int labelGap = Math.max(10, Math.round(8 * textScale) + 2);
        int sliderH = Math.max(12, Math.round(12 * Math.max(1.0f, textScale)));
        int itemH = 12 + labelGap + sliderH + 6;
        return headerH + 6 + (3 * itemH) + 4;
    }

    private int getBlockCreatePaletteHeight(float textScale) {
        int headerH = Math.max(14, Math.round(14 * textScale));
        int labelGap = Math.max(10, Math.round(8 * textScale) + 2);
        int sliderH = Math.max(12, Math.round(12 * Math.max(1.0f, textScale)));
        int itemH = 12 + labelGap + sliderH + 6;
        return headerH + 6 + (3 * itemH) + 4;
    }

    private int getBlock6Height(float textScale) {
        int headerH = Math.max(14, Math.round(14 * textScale));
        int labelGap = Math.max(10, Math.round(8 * textScale) + 2);
        int keyBtnH = Math.max(15, Math.round(15 * Math.max(1.0f, textScale)));
        int itemH = 12 + labelGap + keyBtnH + 6;
        int resetBtnH = keyBtnH + 6;
        return headerH + 6 + (4 * itemH) + resetBtnH + 4;
    }

    private int calculateTotalHeight(float textScale) {
        int cardW = width - SCROLLBAR_WIDTH - 16;
        int contentW = cardW - 16;
        int b1 = getBlock1Height(textScale, contentW);
        int b2 = getBlock2Height(textScale, cardW);
        int b3 = getBlock3Height(textScale);
        int b4 = getBlock4Height(textScale);
        int b5 = getBlock5Height(textScale);
        int bCreatePal = getBlockCreatePaletteHeight(textScale);
        int b6 = getBlock6Height(textScale);
        int gap = 8;
        return (b1 > 0 ? b1 + gap : 0) + b2 + gap + b3 + gap + b4 + gap + b5 + gap + bCreatePal + gap + b6 + 16;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;
        ModConfig cfg = viewModel.getConfig();
        float textScale = cfg.getTextScale();

        int listStartY = y + 8;
        int listHeight = height - 16;
        scrollbar.updateMaxScroll(calculateTotalHeight(textScale), listHeight);

        context.enableScissor(x + 4, listStartY, x + width - 4, listStartY + listHeight);

        int scroll = (int) scrollbar.getScrollOffset();
        int cardX = x + 6;
        int cardW = width - SCROLLBAR_WIDTH - 16;
        int contentX = cardX + 8;
        int contentW = cardW - 16;
        int sliderW = Math.min(contentW, 180);
        int sliderH = Math.max(12, Math.round(12 * Math.max(1.0f, textScale)));
        int labelGap = Math.max(10, Math.round(8 * textScale) + 2);
        int secGap = 6;
        int headerH = Math.max(14, Math.round(14 * textScale));
        int blockGap = 8;

        int b1H = getBlock1Height(textScale, contentW);
        int b2H = getBlock2Height(textScale, cardW);
        int b3H = getBlock3Height(textScale);
        int b4H = getBlock4Height(textScale);
        int b5H = getBlock5Height(textScale);
        int bCreatePalH = getBlockCreatePaletteHeight(textScale);
        int b6H = getBlock6Height(textScale);

        int b1Y = listStartY + 4 - scroll;
        int b2Y = b1Y + (b1H > 0 ? b1H + blockGap : 0);
        int b3Y = b2Y + b2H + blockGap;
        int b4Y = b3Y + b3H + blockGap;
        int b5Y = b4Y + b4H + blockGap;
        int bCreatePalY = b5Y + b5H + blockGap;
        int b6Y = bCreatePalY + bCreatePalH + blockGap;

        // BLOCK 1: Shortcuts guide
        if (b1H > 0 && b1Y + b1H >= listStartY && b1Y <= listStartY + listHeight) {
            renderBlockCard(context, tr, cardX, b1Y, cardW, b1H, Text.translatable("config.itemorganizer.group.guide"), textScale);
            float guideScale = Math.max(0.70f, Math.min(1.0f, textScale * 0.85f));
            int lineSpacing = Math.max(10, Math.round(11 * guideScale));
            List<String> guideLines = buildGuideLines(tr, cfg, contentW, guideScale);
            int guideStartY = b1Y + headerH + 4;
            for (int i = 0; i < guideLines.size(); i++) {
                int lineY = guideStartY + (i * lineSpacing);
                if (lineY + lineSpacing >= listStartY && lineY <= listStartY + listHeight) {
                    TextScaleHelper.drawScaledText(context, tr, guideLines.get(i), contentX, lineY, 0xFFCCCCCC, false, guideScale);
                }
            }
        }

        // BLOCK 2: General (Color, Transparency, Blur, Panel Split, Text Scale)
        if (b2Y + b2H >= listStartY && b2Y <= listStartY + listHeight) {
            renderBlockCard(context, tr, cardX, b2Y, cardW, b2H, Text.translatable("config.itemorganizer.group.general"), textScale);
            int curY = b2Y + headerH + 4;

            int currentColor = cfg.getBackgroundColor() & 0x00FFFFFF;
            if (!hexColorField.isFocused() && hexErrorMessage.isEmpty()) {
                String expectedHex = String.format("#%06X", currentColor);
                if (!expectedHex.equalsIgnoreCase(hexColorField.getText())) {
                    hexColorField.setText(expectedHex);
                }
            }

            if (curY + 12 >= listStartY && curY <= listStartY + listHeight) {
                TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.bg_color"), contentX, curY, 0xFF38BDF8, true, textScale);
            }

            int swatchX = contentX;
            int swatchY = curY + labelGap;
            int swatchSize = 14;
            int chipW = 16;
            int chipH = 14;
            int presetStartX = swatchX + swatchSize + 4 + hexColorField.getWidth() + 6;
            boolean presetsInline = (presetStartX + (PRESETS.length * (chipW + 3)) <= cardX + cardW - 6);

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
            int slider1Y = sec2Y + labelGap;
            if (slider1Y + sliderH >= listStartY && slider1Y <= listStartY + listHeight) {
                transparencySlider.setBounds(contentX, slider1Y, sliderW, sliderH);
                transparencySlider.render(context, tr, mouseX, mouseY, textScale);
            }

            // blur slider
            int secBlurY = slider1Y + sliderH + secGap;
            if (secBlurY + 12 >= listStartY && secBlurY <= listStartY + listHeight) {
                TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.blur"), contentX, secBlurY, 0xFF38BDF8, true, textScale);
            }
            int sliderBlurY = secBlurY + labelGap;
            if (sliderBlurY + sliderH >= listStartY && sliderBlurY <= listStartY + listHeight) {
                blurSlider.setBounds(contentX, sliderBlurY, sliderW, sliderH);
                blurSlider.render(context, tr, mouseX, mouseY, textScale);
            }

            // panel split ratio slider
            int secSplitY = sliderBlurY + sliderH + secGap;
            if (secSplitY + 12 >= listStartY && secSplitY <= listStartY + listHeight) {
                TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.panel_split"), contentX, secSplitY, 0xFF38BDF8, true, textScale);
            }
            int sliderSplitY = secSplitY + labelGap;
            if (sliderSplitY + sliderH >= listStartY && sliderSplitY <= listStartY + listHeight) {
                splitRatioSlider.setBounds(contentX, sliderSplitY, sliderW, sliderH);
                splitRatioSlider.render(context, tr, mouseX, mouseY, textScale);
            }

            // text scale slider
            int secTextY = sliderSplitY + sliderH + secGap;
            if (secTextY + 12 >= listStartY && secTextY <= listStartY + listHeight) {
                TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.text_scale"), contentX, secTextY, 0xFF38BDF8, true, textScale);
            }
            int sliderTextY = secTextY + labelGap;
            if (sliderTextY + sliderH >= listStartY && sliderTextY <= listStartY + listHeight) {
                textScaleSlider.setBounds(contentX, sliderTextY, sliderW, sliderH);
                textScaleSlider.render(context, tr, mouseX, mouseY, textScale);
            }
        }

        // BLOCK 3: Organized / Unorganized / By Version (Grid Zoom, Item Scale)
        if (b3Y + b3H >= listStartY && b3Y <= listStartY + listHeight) {
            renderBlockCard(context, tr, cardX, b3Y, cardW, b3H, Text.translatable("config.itemorganizer.group.grid_views"), textScale);
            int curY = b3Y + headerH + 4;

            // grid zoom slider
            if (curY + 12 >= listStartY && curY <= listStartY + listHeight) {
                TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.grid_zoom"), contentX, curY, 0xFF38BDF8, true, textScale);
            }
            int sliderZoomY = curY + labelGap;
            if (sliderZoomY + sliderH >= listStartY && sliderZoomY <= listStartY + listHeight) {
                scaleSlider.setBounds(contentX, sliderZoomY, sliderW, sliderH);
                scaleSlider.render(context, tr, mouseX, mouseY, textScale);
            }

            // item scale slider
            int secItemY = sliderZoomY + sliderH + secGap;
            if (secItemY + 12 >= listStartY && secItemY <= listStartY + listHeight) {
                TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.item_scale"), contentX, secItemY, 0xFF38BDF8, true, textScale);
            }
            int sliderItemY = secItemY + labelGap;
            if (sliderItemY + sliderH >= listStartY && sliderItemY <= listStartY + listHeight) {
                itemScaleSlider.setBounds(contentX, sliderItemY, sliderW, sliderH);
                itemScaleSlider.render(context, tr, mouseX, mouseY, textScale);
            }
        }

        // BLOCK 4: Hotbar (Hotbar Scale, Hotbar Item Scale)
        if (b4Y + b4H >= listStartY && b4Y <= listStartY + listHeight) {
            renderBlockCard(context, tr, cardX, b4Y, cardW, b4H, Text.translatable("config.itemorganizer.group.hotbar"), textScale);
            int curY = b4Y + headerH + 4;

            // hotbar scale slider
            if (curY + 12 >= listStartY && curY <= listStartY + listHeight) {
                TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.hotbar_scale"), contentX, curY, 0xFF38BDF8, true, textScale);
            }
            int sliderHotbarY = curY + labelGap;
            if (sliderHotbarY + sliderH >= listStartY && sliderHotbarY <= listStartY + listHeight) {
                hotbarScaleSlider.setBounds(contentX, sliderHotbarY, sliderW, sliderH);
                hotbarScaleSlider.render(context, tr, mouseX, mouseY, textScale);
            }

            // hotbar item scale slider
            int secHotbarItemY = sliderHotbarY + sliderH + secGap;
            if (secHotbarItemY + 12 >= listStartY && secHotbarItemY <= listStartY + listHeight) {
                TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.hotbar_item_scale"), contentX, secHotbarItemY, 0xFF38BDF8, true, textScale);
            }
            int sliderHotbarItemY = secHotbarItemY + labelGap;
            if (sliderHotbarItemY + sliderH >= listStartY && sliderHotbarItemY <= listStartY + listHeight) {
                hotbarItemScaleSlider.setBounds(contentX, sliderHotbarItemY, sliderW, sliderH);
                hotbarItemScaleSlider.render(context, tr, mouseX, mouseY, textScale);
            }
        }

        // BLOCK 5: Palette (Palette Scale, Palette Item Scale)
        if (b5Y + b5H >= listStartY && b5Y <= listStartY + listHeight) {
            renderBlockCard(context, tr, cardX, b5Y, cardW, b5H, Text.translatable("config.itemorganizer.group.palette"), textScale);
            int curY = b5Y + headerH + 4;

            // palette scale slider
            if (curY + 12 >= listStartY && curY <= listStartY + listHeight) {
                TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.palette_scale"), contentX, curY, 0xFF38BDF8, true, textScale);
            }
            int sliderPaletteY = curY + labelGap;
            if (sliderPaletteY + sliderH >= listStartY && sliderPaletteY <= listStartY + listHeight) {
                paletteScaleSlider.setBounds(contentX, sliderPaletteY, sliderW, sliderH);
                paletteScaleSlider.render(context, tr, mouseX, mouseY, textScale);
            }

            // palette item scale slider
            int secPaletteItemY = sliderPaletteY + sliderH + secGap;
            if (secPaletteItemY + 12 >= listStartY && secPaletteItemY <= listStartY + listHeight) {
                TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.palette_item_scale"), contentX, secPaletteItemY, 0xFF38BDF8, true, textScale);
            }
            int sliderPaletteItemY = secPaletteItemY + labelGap;
            if (sliderPaletteItemY + sliderH >= listStartY && sliderPaletteItemY <= listStartY + listHeight) {
                paletteItemScaleSlider.setBounds(contentX, sliderPaletteItemY, sliderW, sliderH);
                paletteItemScaleSlider.render(context, tr, mouseX, mouseY, textScale);
            }

            // palette button scale slider
            int secPaletteButtonY = sliderPaletteItemY + sliderH + secGap;
            if (secPaletteButtonY + 12 >= listStartY && secPaletteButtonY <= listStartY + listHeight) {
                TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.palette_button_scale"), contentX, secPaletteButtonY, 0xFF38BDF8, true, textScale);
            }
            int sliderPaletteButtonY = secPaletteButtonY + labelGap;
            if (sliderPaletteButtonY + sliderH >= listStartY && sliderPaletteButtonY <= listStartY + listHeight) {
                paletteButtonScaleSlider.setBounds(contentX, sliderPaletteButtonY, sliderW, sliderH);
                paletteButtonScaleSlider.render(context, tr, mouseX, mouseY, textScale);
            }
        }

        // BLOCK: Create Palette (Create Palette Scale, Create Palette Item Scale, Create Palette Button Scale)
        if (bCreatePalY + bCreatePalH >= listStartY && bCreatePalY <= listStartY + listHeight) {
            renderBlockCard(context, tr, cardX, bCreatePalY, cardW, bCreatePalH, Text.translatable("config.itemorganizer.group.create_palette"), textScale);
            int curY = bCreatePalY + headerH + 4;

            // create palette scale slider
            if (curY + 12 >= listStartY && curY <= listStartY + listHeight) {
                TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.create_palette_scale"), contentX, curY, 0xFF38BDF8, true, textScale);
            }
            int sliderScaleY = curY + labelGap;
            if (sliderScaleY + sliderH >= listStartY && sliderScaleY <= listStartY + listHeight) {
                createPaletteScaleSlider.setBounds(contentX, sliderScaleY, sliderW, sliderH);
                createPaletteScaleSlider.render(context, tr, mouseX, mouseY, textScale);
            }

            // create palette item scale slider
            int secItemY = sliderScaleY + sliderH + secGap;
            if (secItemY + 12 >= listStartY && secItemY <= listStartY + listHeight) {
                TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.create_palette_item_scale"), contentX, secItemY, 0xFF38BDF8, true, textScale);
            }
            int sliderItemY = secItemY + labelGap;
            if (sliderItemY + sliderH >= listStartY && sliderItemY <= listStartY + listHeight) {
                createPaletteItemScaleSlider.setBounds(contentX, sliderItemY, sliderW, sliderH);
                createPaletteItemScaleSlider.render(context, tr, mouseX, mouseY, textScale);
            }

            // create palette button scale slider
            int secButtonY = sliderItemY + sliderH + secGap;
            if (secButtonY + 12 >= listStartY && secButtonY <= listStartY + listHeight) {
                TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.create_palette_button_scale"), contentX, secButtonY, 0xFF38BDF8, true, textScale);
            }
            int sliderButtonY = secButtonY + labelGap;
            if (sliderButtonY + sliderH >= listStartY && sliderButtonY <= listStartY + listHeight) {
                createPaletteButtonScaleSlider.setBounds(contentX, sliderButtonY, sliderW, sliderH);
                createPaletteButtonScaleSlider.render(context, tr, mouseX, mouseY, textScale);
            }
        }

        // BLOCK 6: Keybindings and Actions
        if (b6Y + b6H >= listStartY && b6Y <= listStartY + listHeight) {
            renderBlockCard(context, tr, cardX, b6Y, cardW, b6H, Text.translatable("config.itemorganizer.group.keybindings"), textScale);
            int curY = b6Y + headerH + 4;
            int keyBtnW = Math.min(180, contentW);
            int keyBtnH = Math.max(15, Math.round(15 * Math.max(1.0f, textScale)));

            // open key binding
            if (curY + 12 >= listStartY && curY <= listStartY + listHeight) {
                TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.open_key"), contentX, curY, 0xFF38BDF8, true, textScale);
            }
            int keyBtnY = curY + labelGap;
            if (keyBtnY + keyBtnH >= listStartY && keyBtnY <= listStartY + listHeight) {
                boolean hoverKeyBtn = (activeModal == null && mouseX >= contentX && mouseX <= contentX + keyBtnW && mouseY >= keyBtnY && mouseY <= keyBtnY + keyBtnH);
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

                RenderHelper.drawButton(context, tr, contentX, keyBtnY, keyBtnW, keyBtnH, keyLabel, hoverKeyBtn,
                        keyBg, keyHoverBg, keyBorder, keyHoverBorder, keyTextColor, keyHoverTextColor, textScale);
            }

            // quick append key binding
            int sec10Y = keyBtnY + keyBtnH + secGap;
            if (sec10Y + 12 >= listStartY && sec10Y <= listStartY + listHeight) {
                TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.quick_append_key"), contentX, sec10Y, UITheme.PRIMARY, true, textScale);
            }
            int quickKeyBtnY = sec10Y + labelGap;
            if (quickKeyBtnY + keyBtnH >= listStartY && quickKeyBtnY <= listStartY + listHeight) {
                boolean hoverQuickKey = (activeModal == null && mouseX >= contentX && mouseX <= contentX + keyBtnW && mouseY >= quickKeyBtnY && mouseY <= quickKeyBtnY + keyBtnH);
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

                RenderHelper.drawButton(context, tr, contentX, quickKeyBtnY, keyBtnW, keyBtnH, qkLabel, hoverQuickKey,
                        qkBg, qkHoverBg, qkBorder, qkHoverBorder, qkTextColor, qkHoverTextColor, textScale);
            }

            // undo key binding
            int sec11Y = quickKeyBtnY + keyBtnH + secGap;
            if (sec11Y + 12 >= listStartY && sec11Y <= listStartY + listHeight) {
                TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.undo_key"), contentX, sec11Y, UITheme.PRIMARY, true, textScale);
            }
            int undoKeyBtnY = sec11Y + labelGap;
            if (undoKeyBtnY + keyBtnH >= listStartY && undoKeyBtnY <= listStartY + listHeight) {
                boolean hoverUndoKey = (activeModal == null && mouseX >= contentX && mouseX <= contentX + keyBtnW && mouseY >= undoKeyBtnY && mouseY <= undoKeyBtnY + keyBtnH);
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

                RenderHelper.drawButton(context, tr, contentX, undoKeyBtnY, keyBtnW, keyBtnH, undoLabel, hoverUndoKey,
                        undoBg, undoHoverBg, undoBorder, undoHoverBorder, undoTextColor, undoHoverTextColor, textScale);
            }

            // redo key binding
            int sec12Y = undoKeyBtnY + keyBtnH + secGap;
            if (sec12Y + 12 >= listStartY && sec12Y <= listStartY + listHeight) {
                TextScaleHelper.drawScaledText(context, tr, Text.translatable("config.itemorganizer.redo_key"), contentX, sec12Y, UITheme.PRIMARY, true, textScale);
            }
            int redoKeyBtnY = sec12Y + labelGap;
            if (redoKeyBtnY + keyBtnH >= listStartY && redoKeyBtnY <= listStartY + listHeight) {
                boolean hoverRedoKey = (activeModal == null && mouseX >= contentX && mouseX <= contentX + keyBtnW && mouseY >= redoKeyBtnY && mouseY <= redoKeyBtnY + keyBtnH);
                Text redoLabel;
                int redoBg, redoHoverBg, redoBorder, redoHoverBorder, redoTextColor, redoHoverTextColor;

                if (listeningForRedoKey) {
                    redoLabel = Text.translatable("config.itemorganizer.press_key");
                    redoBg = UITheme.WARNING_BG;
                    redoHoverBg = UITheme.WARNING_HOVER_BG;
                    redoBorder = UITheme.WARNING_BORDER;
                    redoHoverBorder = UITheme.WARNING_BORDER;
                    redoTextColor = UITheme.WARNING;
                    redoHoverTextColor = UITheme.TEXT_WHITE;
                } else {
                    String boundKey = cfg.getKeyRedo();
                    InputUtil.Key k = InputUtil.fromTranslationKey(boundKey);
                    String keyName = (k != null) ? k.getLocalizedText().getString().toUpperCase() : "Y";
                    redoLabel = Text.translatable("config.itemorganizer.key_label_ctrl", keyName);
                    redoBg = UITheme.BG_SURFACE_HOVER;
                    redoHoverBg = UITheme.PRIMARY_BG;
                    redoBorder = UITheme.BORDER_MUTED;
                    redoHoverBorder = UITheme.PRIMARY;
                    redoTextColor = UITheme.TEXT_SECONDARY;
                    redoHoverTextColor = UITheme.TEXT_WHITE;
                }

                RenderHelper.drawButton(context, tr, contentX, redoKeyBtnY, keyBtnW, keyBtnH, redoLabel, hoverRedoKey,
                        redoBg, redoHoverBg, redoBorder, redoHoverBorder, redoTextColor, redoHoverTextColor, textScale);
            }

            // reset defaults button
            int resetBtnY = redoKeyBtnY + keyBtnH + secGap + 2;
            int resetBtnH = keyBtnH;
            if (resetBtnY + resetBtnH >= listStartY && resetBtnY <= listStartY + listHeight) {
                boolean hoverReset = (activeModal == null && mouseX >= contentX && mouseX <= contentX + keyBtnW && mouseY >= resetBtnY && mouseY <= resetBtnY + resetBtnH);
                RenderHelper.drawButton(context, tr, contentX, resetBtnY, keyBtnW, resetBtnH,
                        Text.translatable("config.itemorganizer.reset_defaults"), hoverReset,
                        UITheme.DANGER_BG, UITheme.DANGER_HOVER_BG, UITheme.DANGER_BORDER_MUTED, UITheme.DANGER,
                        0xFFFCA5A5, UITheme.TEXT_WHITE, textScale);
            }
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
            if (listeningForRedoKey) {
                listeningForRedoKey = false;
            }
            return false;
        }

        float textScale = viewModel.getConfig().getTextScale();
        int scroll = (int) scrollbar.getScrollOffset();
        int cardX = x + 6;
        int cardW = width - SCROLLBAR_WIDTH - 16;
        int contentX = cardX + 8;
        int contentW = cardW - 16;
        int sliderW = Math.min(contentW, 180);
        int sliderH = Math.max(12, Math.round(12 * Math.max(1.0f, textScale)));
        int labelGap = Math.max(10, Math.round(8 * textScale) + 2);
        int secGap = 6;
        int headerH = Math.max(14, Math.round(14 * textScale));
        int blockGap = 8;

        int b1H = getBlock1Height(textScale, contentW);
        int b2H = getBlock2Height(textScale, cardW);
        int b3H = getBlock3Height(textScale);
        int b4H = getBlock4Height(textScale);
        int b5H = getBlock5Height(textScale);
        int b6H = getBlock6Height(textScale);

        int b1Y = listStartY + 4 - scroll;
        int b2Y = b1Y + (b1H > 0 ? b1H + blockGap : 0);
        int b3Y = b2Y + b2H + blockGap;
        int b4Y = b3Y + b3H + blockGap;
        int b5Y = b4Y + b4H + blockGap;
        int b6Y = b5Y + b5H + blockGap;

        // Block 2: hex input and color presets
        int b2ContentY = b2Y + headerH + 4;
        int swatchX = contentX;
        int swatchY = b2ContentY + labelGap;
        int swatchSize = 14;
        hexColorField.setY(swatchY);
        hexColorField.setX(swatchX + swatchSize + 4);
        if (hexColorField.mouseClicked(click, bl)) {
            return true;
        }

        int chipW = 16;
        int chipH = 14;
        int presetStartX = swatchX + swatchSize + 4 + hexColorField.getWidth() + 6;
        boolean presetsInline = (presetStartX + (PRESETS.length * (chipW + 3)) <= cardX + cardW - 6);
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

        // sliders across all blocks
        for (SliderComponent s : allSliders) {
            if (s.mouseClicked(click)) {
                return true;
            }
        }

        // Block 6 buttons
        int b6ContentY = b6Y + headerH + 4;
        int keyBtnW = Math.min(180, contentW);
        int keyBtnH = Math.max(15, Math.round(15 * Math.max(1.0f, textScale)));

        int keyBtnY = b6ContentY + labelGap;
        if (mouseX >= contentX && mouseX <= contentX + keyBtnW && mouseY >= keyBtnY && mouseY <= keyBtnY + keyBtnH) {
            listeningForKey = !listeningForKey;
            listeningForQuickAppendKey = false;
            listeningForUndoKey = false;
            listeningForRedoKey = false;
            playClickSound();
            return true;
        }

        int quickKeyBtnY = keyBtnY + keyBtnH + secGap + labelGap;
        if (mouseX >= contentX && mouseX <= contentX + keyBtnW && mouseY >= quickKeyBtnY && mouseY <= quickKeyBtnY + keyBtnH) {
            listeningForQuickAppendKey = !listeningForQuickAppendKey;
            listeningForKey = false;
            listeningForUndoKey = false;
            listeningForRedoKey = false;
            playClickSound();
            return true;
        }

        int undoKeyBtnY = quickKeyBtnY + keyBtnH + secGap + labelGap;
        if (mouseX >= contentX && mouseX <= contentX + keyBtnW && mouseY >= undoKeyBtnY && mouseY <= undoKeyBtnY + keyBtnH) {
            listeningForUndoKey = !listeningForUndoKey;
            listeningForKey = false;
            listeningForQuickAppendKey = false;
            listeningForRedoKey = false;
            playClickSound();
            return true;
        }

        int redoKeyBtnY = undoKeyBtnY + keyBtnH + secGap + labelGap;
        if (mouseX >= contentX && mouseX <= contentX + keyBtnW && mouseY >= redoKeyBtnY && mouseY <= redoKeyBtnY + keyBtnH) {
            listeningForRedoKey = !listeningForRedoKey;
            listeningForKey = false;
            listeningForQuickAppendKey = false;
            listeningForUndoKey = false;
            playClickSound();
            return true;
        }

        int resetBtnY = redoKeyBtnY + keyBtnH + secGap + 2;
        int resetBtnH = keyBtnH;
        if (mouseX >= contentX && mouseX <= contentX + keyBtnW && mouseY >= resetBtnY && mouseY <= resetBtnY + resetBtnH) {
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
        if (listeningForRedoKey) {
            listeningForRedoKey = false;
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
            c.setPaletteButtonScale(1.00f);
            c.setCreatePaletteScale(1.00f);
            c.setCreatePaletteItemScale(1.00f);
            c.setCreatePaletteButtonScale(1.00f);
            c.setKeyOpenClose("key.keyboard.o");
            c.setKeyQuickAppend("key.keyboard.a");
            c.setKeyUndo("key.keyboard.z");
            c.setKeyRedo("key.keyboard.y");
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

        if (listeningForRedoKey) {
            if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
                listeningForRedoKey = false;
                playClickSound();
                return true;
            }

            InputUtil.Key newKey = InputUtil.fromKeyCode(input);
            if (newKey != null) {
                String translationKey = newKey.getTranslationKey();
                viewModel.updateConfig(c -> c.setKeyRedo(translationKey));
            }

            listeningForRedoKey = false;
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
        return activeModal != null || (hexColorField != null && hexColorField.isFocused()) || listeningForKey || listeningForQuickAppendKey || listeningForUndoKey || listeningForRedoKey;
    }

    public double getScrollOffset() {
        return scrollbar.getScrollOffset();
    }

    public void setScrollOffset(double offset) {
        scrollbar.setScrollOffset(offset);
    }
}
