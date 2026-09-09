package com.itemorganizer.gui.theme;

// design tokens and color constants for gui components
public final class UITheme {
    private UITheme() {}

    // background and surface fills
    public static final int BG_DARKEST = 0xFF0B0F19;
    public static final int BG_CARD = 0xFF141820;
    public static final int BG_MODAL_BACKDROP = 0xDD0B0F19;
    public static final int BG_MODAL_BACKDROP_LIGHT = 0x99000000;
    public static final int BG_HOVER = 0x22FFFFFF;
    public static final int BG_ACTIVE = 0x3338BDF8;
    public static final int BG_SURFACE = 0x08FFFFFF;
    public static final int BG_SURFACE_HOVER = 0x14FFFFFF;
    public static final int BG_INPUT = 0xFF0B0F19;

    // borders and dividers
    public static final int BORDER_SUBTLE = 0x33FFFFFF;
    public static final int BORDER_MUTED = 0x1AFFFFFF;
    public static final int BORDER_HOVER = 0x66FFFFFF;
    public static final int BORDER_FOCUS = 0xFF38BDF8;

    // primary theme (sky blue)
    public static final int PRIMARY = 0xFF38BDF8;
    public static final int PRIMARY_BG = 0x401E3A5F;
    public static final int PRIMARY_HOVER_BG = 0x801E3A5F;
    public static final int PRIMARY_BORDER = 0xFF38BDF8;
    public static final int PRIMARY_BORDER_MUTED = 0x8038BDF8;

    // success theme (emerald green)
    public static final int SUCCESS = 0xFF34D399;
    public static final int SUCCESS_BG = 0x40065F46;
    public static final int SUCCESS_HOVER_BG = 0x80065F46;
    public static final int SUCCESS_BORDER = 0xFF34D399;
    public static final int SUCCESS_BORDER_MUTED = 0x8034D399;

    // warning theme (amber)
    public static final int WARNING = 0xFFF59E0B;
    public static final int WARNING_BG = 0x4078350F;
    public static final int WARNING_HOVER_BG = 0x8078350F;
    public static final int WARNING_BORDER = 0xFFF59E0B;
    public static final int WARNING_BORDER_MUTED = 0x80F59E0B;

    // danger theme (red)
    public static final int DANGER = 0xFFEF4444;
    public static final int DANGER_BG = 0x407F1D1D;
    public static final int DANGER_HOVER_BG = 0x807F1D1D;
    public static final int DANGER_BORDER = 0xFFEF4444;
    public static final int DANGER_BORDER_MUTED = 0x80EF4444;

    // typography
    public static final int TEXT_WHITE = 0xFFFFFFFF;
    public static final int TEXT_PRIMARY = 0xFFFFFFFF;
    public static final int TEXT_SECONDARY = 0xFFCBD5E1;
    public static final int TEXT_MUTED = 0xFF94A3B8;
    public static final int TEXT_HINT = 0xFF64748B;

    // scrollbars
    public static final int SCROLLBAR_TRACK = 0x18000000;
    public static final int SCROLLBAR_THUMB = 0x4DFFFFFF;
    public static final int SCROLLBAR_THUMB_HOVER = 0xCC38BDF8;

    // helper for modifying color alpha value
    public static int withAlpha(int argb, float alpha) {
        int a = Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f);
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    public static int withAlphaInt(int argb, int alpha255) {
        int a = Math.max(0, Math.min(255, alpha255));
        return (a << 24) | (argb & 0x00FFFFFF);
    }
}
