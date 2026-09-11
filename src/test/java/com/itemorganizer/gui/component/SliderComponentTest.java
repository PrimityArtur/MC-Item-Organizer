package com.itemorganizer.gui.component;

import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class SliderComponentTest {

    @Test
    void testValueGetterAndSetter() {
        AtomicReference<Double> state = new AtomicReference<>(0.5);
        SliderComponent slider = new SliderComponent(
                Text.literal("Test"),
                0.0, 1.0,
                state::get,
                state::set,
                v -> String.format("%.2f", v)
        );

        assertEquals(0.5, slider.getValue(), 0.0001);

        slider.setValue(0.75);
        assertEquals(0.75, state.get(), 0.0001);
        assertEquals(0.75, slider.getValue(), 0.0001);
    }

    @Test
    void testClamping() {
        AtomicReference<Double> state = new AtomicReference<>(5.0);
        SliderComponent slider = new SliderComponent(
                Text.literal("Clamped"),
                1.0, 10.0,
                state::get,
                state::set,
                v -> String.valueOf(v.intValue())
        );

        slider.setValue(-10.0);
        assertEquals(1.0, state.get(), 0.0001);

        slider.setValue(100.0);
        assertEquals(10.0, state.get(), 0.0001);
    }

    @Test
    void testBoundsAndHitDetection() {
        SliderComponent slider = new SliderComponent(
                10, 20, 100, 14,
                Text.literal("Bounds"),
                0.0, 1.0,
                () -> 0.0,
                v -> {},
                v -> ""
        );

        assertEquals(10, slider.getX());
        assertEquals(20, slider.getY());
        assertEquals(100, slider.getWidth());
        assertEquals(14, slider.getHeight());

        assertTrue(slider.isMouseOver(10, 20));
        assertTrue(slider.isMouseOver(110, 34));
        assertFalse(slider.isMouseOver(9, 20));
        assertFalse(slider.isMouseOver(111, 20));
        assertFalse(slider.isMouseOver(50, 19));
        assertFalse(slider.isMouseOver(50, 35));

        slider.setBounds(50, 60, 80, 20);
        assertEquals(50, slider.getX());
        assertEquals(60, slider.getY());
        assertEquals(80, slider.getWidth());
        assertEquals(20, slider.getHeight());
        assertTrue(slider.isMouseOver(60, 70));
        assertFalse(slider.isMouseOver(10, 20));
    }

    @Test
    void testDraggingState() {
        SliderComponent slider = new SliderComponent(
                Text.literal("Drag"),
                0.0, 1.0,
                () -> 0.0,
                v -> {},
                v -> ""
        );

        assertFalse(slider.isDragging());
        slider.stopDragging();
        assertFalse(slider.isDragging());
    }
}
