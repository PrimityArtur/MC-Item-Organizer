package com.itemorganizer.gui.dragdrop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DragAndDropClickTest {

    private DragAndDropManager dragManager;

    @BeforeEach
    void setUp() {
        dragManager = DragAndDropManager.getInstance();
        dragManager.cancelDrag();
    }

    @Test
    void testClickToPickRetainsPayload() {
        DragPayload payload = new DragPayload("minecraft:apple", null, DragSource.ORDENADO, 0, 0, -1, false);
        dragManager.startDrag(payload, 50.0, 50.0);

        assertTrue(dragManager.isDragging());
        assertTrue(dragManager.isMouseButtonHeld());
        assertFalse(dragManager.isDraggedBeyondThreshold());

        // small movement below drag threshold
        dragManager.onMouseDrag(51.0, 51.0);
        assertFalse(dragManager.isDraggedBeyondThreshold());

        // mouse released without dragging
        dragManager.onMouseRelease();
        assertFalse(dragManager.isMouseButtonHeld());
        assertFalse(dragManager.isDraggedBeyondThreshold());
        assertTrue(dragManager.isDragging());
        assertSame(payload, dragManager.getActivePayload());

        // consume when placed
        DragPayload consumed = dragManager.consumePayload();
        assertSame(payload, consumed);
        assertFalse(dragManager.isDragging());
        assertNull(dragManager.getActivePayload());
    }

    @Test
    void testDragBeyondThreshold() {
        DragPayload payload = new DragPayload("minecraft:diamond", null, DragSource.PALETAS, -1, -1, 2, true);
        dragManager.startDrag(payload, 100.0, 100.0);

        dragManager.onMouseDrag(102.0, 101.0);
        assertFalse(dragManager.isDraggedBeyondThreshold());

        dragManager.onMouseDrag(110.0, 100.0);
        assertTrue(dragManager.isDraggedBeyondThreshold());

        dragManager.onMouseRelease();
        assertFalse(dragManager.isMouseButtonHeld());
        assertTrue(dragManager.isDraggedBeyondThreshold());

        dragManager.cancelDrag();
        assertFalse(dragManager.isDragging());
        assertFalse(dragManager.isDraggedBeyondThreshold());
    }

    @Test
    void testStartHoldingForFloatingItem() {
        DragPayload payload = new DragPayload("minecraft:stone", null, DragSource.POR_ORGANIZAR, -1, -1, 5, true);
        dragManager.startDrag(payload, 10.0, 10.0);
        dragManager.onMouseRelease();

        // start secondary hold
        dragManager.startHolding(80.0, 80.0);
        assertTrue(dragManager.isMouseButtonHeld());
        assertFalse(dragManager.isDraggedBeyondThreshold());

        dragManager.onMouseDrag(95.0, 95.0);
        assertTrue(dragManager.isDraggedBeyondThreshold());

        dragManager.consumePayload();
        assertFalse(dragManager.isDragging());
        assertFalse(dragManager.isMouseButtonHeld());
        assertFalse(dragManager.isDraggedBeyondThreshold());
    }
}
