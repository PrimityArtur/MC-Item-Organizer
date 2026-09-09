package com.itemorganizer.gui.component;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ScrollbarComponentTest {

    @Test
    void testScrollClampingAndOffset() {
        ScrollbarComponent scrollbar = new ScrollbarComponent(100, 10, 4, 100);

        scrollbar.updateMaxScroll(500, 100);
        assertEquals(400, scrollbar.getMaxScroll(), 0.001);
        assertEquals(0, scrollbar.getScrollOffset(), 0.001);

        scrollbar.scroll(150);
        assertEquals(150, scrollbar.getScrollOffset(), 0.001);

        scrollbar.scroll(400);
        assertEquals(400, scrollbar.getScrollOffset(), 0.001, "Scroll offset must be clamped to max scroll");

        scrollbar.scroll(-500);
        assertEquals(0, scrollbar.getScrollOffset(), 0.001, "Scroll offset must not be negative");

        // when content fits inside container
        scrollbar.updateMaxScroll(80, 100);
        assertEquals(0, scrollbar.getMaxScroll(), 0.001);
        assertEquals(0, scrollbar.getScrollOffset(), 0.001);
    }

    @Test
    void testThumbGeometryCalculations() {
        ScrollbarComponent scrollbar = new ScrollbarComponent(50, 20, 6, 200);

        // content 400, visible 200 -> maxScroll = 200
        scrollbar.updateMaxScroll(400, 200);
        assertEquals(200, scrollbar.getMaxScroll(), 0.001);

        // thumb height: 200 * (200 / 400) = 100
        assertEquals(100, scrollbar.getThumbHeight());

        // at top
        assertEquals(20, scrollbar.getThumbY());

        // at bottom (offset 200)
        scrollbar.setScrollOffset(200);
        // y + (1.0 * (height - thumbHeight)) = 20 + 100 = 120
        assertEquals(120, scrollbar.getThumbY());

        // at midpoint
        scrollbar.setScrollOffset(100);
        assertEquals(70, scrollbar.getThumbY());
    }

    @Test
    void testMouseOverAndBounds() {
        ScrollbarComponent scrollbar = new ScrollbarComponent(10, 20, 8, 150);

        assertTrue(scrollbar.isMouseOver(10, 20));
        assertTrue(scrollbar.isMouseOver(14, 100));
        assertTrue(scrollbar.isMouseOver(18, 170));

        assertFalse(scrollbar.isMouseOver(9, 50));
        assertFalse(scrollbar.isMouseOver(19, 50));
        assertFalse(scrollbar.isMouseOver(14, 19));
        assertFalse(scrollbar.isMouseOver(14, 171));

        scrollbar.setBounds(100, 50, 10, 80);
        assertTrue(scrollbar.isMouseOver(105, 90));
        assertFalse(scrollbar.isMouseOver(14, 100));
    }

    @Test
    void testMinimumThumbHeight() {
        ScrollbarComponent scrollbar = new ScrollbarComponent(0, 0, 4, 100);
        // large content: viewRatio would be very small, but min thumb is 16
        scrollbar.updateMaxScroll(10000, 100);
        assertEquals(16, scrollbar.getThumbHeight());
    }
}
