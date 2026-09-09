package com.itemorganizer.gui.component;

import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class ModalDialogComponentTest {

    @Test
    void testCenteringCalculation() {
        ModalDialogComponent modal = ModalDialogComponent.builder()
                .parentBounds(0, 0, 400, 300)
                .size(200, 100)
                .title(Text.literal("Centering Test"))
                .build();

        assertEquals(200, modal.getWidth());
        assertEquals(100, modal.getHeight());
        assertEquals(100, modal.getX()); // (400 - 200) / 2
        assertEquals(100, modal.getY()); // (300 - 100) / 2

        modal.updateParentBounds(0, 0, 800, 600);
        assertEquals(300, modal.getX()); // (800 - 200) / 2
        assertEquals(250, modal.getY()); // (600 - 100) / 2
    }

    @Test
    void testCustomPosition() {
        ModalDialogComponent modal = ModalDialogComponent.builder()
                .position(75, 85)
                .size(150, 90)
                .title(Text.literal("Custom Pos"))
                .build();

        assertEquals(75, modal.getX());
        assertEquals(85, modal.getY());
        assertEquals(150, modal.getWidth());
        assertEquals(90, modal.getHeight());
    }

    @Test
    void testConfirmAndCancelCallbacks() {
        AtomicBoolean confirmed = new AtomicBoolean(false);
        AtomicBoolean cancelled = new AtomicBoolean(false);

        ModalDialogComponent modal = ModalDialogComponent.builder()
                .parentBounds(0, 0, 500, 500)
                .size(220, 120)
                .type(ModalDialogComponent.ModalType.DANGER)
                .title(Text.literal("Delete"))
                .message(Text.literal("Are you sure?"))
                .confirmButton(Text.literal("Yes"), () -> confirmed.set(true))
                .cancelButton(Text.literal("No"), () -> cancelled.set(true))
                .build();

        assertEquals(ModalDialogComponent.ModalType.DANGER, modal.getType());
        assertEquals("Delete", modal.getTitle().getString());
        assertEquals("Are you sure?", modal.getMessage().getString());
        assertEquals("Yes", modal.getConfirmText().getString());
        assertEquals("No", modal.getCancelText().getString());

        modal.triggerConfirm();
        assertTrue(confirmed.get());
        assertFalse(cancelled.get());

        modal.triggerCancel();
        assertTrue(cancelled.get());
    }

    @Test
    void testVisualCustomizations() {
        ModalDialogComponent modal = ModalDialogComponent.builder()
                .parentBounds(0, 0, 300, 300)
                .size(180, 80)
                .backgroundColor(0xFF112233)
                .strokeColor(0xFFAABBCC)
                .shadow(6, 0x88000000)
                .backdropColor(0x99000000)
                .closeOnBackdropClick(true)
                .build();

        assertEquals(0xFF112233, modal.getBackgroundColor());
        assertEquals(0xFFAABBCC, modal.getStrokeColor());
        assertEquals(6, modal.getShadowSize());
        assertEquals(0x88000000, modal.getShadowColor());
        assertEquals(0x99000000, modal.getBackdropColor());
        assertTrue(modal.isCloseOnBackdropClick());
    }
}
