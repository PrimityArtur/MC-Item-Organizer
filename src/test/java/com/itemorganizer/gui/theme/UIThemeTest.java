package com.itemorganizer.gui.theme;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UIThemeTest {

    @Test
    void testColorConstantsNonZero() {
        assertNotEquals(0, UITheme.PRIMARY);
        assertNotEquals(0, UITheme.SUCCESS);
        assertNotEquals(0, UITheme.WARNING);
        assertNotEquals(0, UITheme.DANGER);
        assertNotEquals(0, UITheme.BG_CARD);
        assertNotEquals(0, UITheme.BG_MODAL_BACKDROP);
        assertNotEquals(0, UITheme.TEXT_PRIMARY);
        assertNotEquals(0, UITheme.TEXT_SECONDARY);
    }

    @Test
    void testWithAlphaFullyOpaque() {
        int color = 0x00123456;
        int result = UITheme.withAlpha(color, 1.0f);
        assertEquals(0xFF123456, result);
    }

    @Test
    void testWithAlphaFullyTransparent() {
        int color = 0xFF123456;
        int result = UITheme.withAlpha(color, 0.0f);
        assertEquals(0x00123456, result);
    }

    @Test
    void testWithAlphaClamping() {
        int color = 0xFFABCDEF;
        int belowZero = UITheme.withAlpha(color, -0.5f);
        assertEquals(0x00ABCDEF, belowZero);

        int aboveOne = UITheme.withAlpha(color, 1.5f);
        assertEquals(0xFFABCDEF, aboveOne);
    }

    @Test
    void testWithAlphaHalf() {
        int color = 0xFFFFFFFF;
        int half = UITheme.withAlpha(color, 0.5f);
        int alpha = (half >>> 24) & 0xFF;
        assertEquals(128, alpha);
        assertEquals(0x00FFFFFF, half & 0x00FFFFFF);
    }
}
