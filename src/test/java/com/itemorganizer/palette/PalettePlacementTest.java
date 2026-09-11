package com.itemorganizer.palette;

import com.itemorganizer.gui.util.PalettePlacementManager;
import net.minecraft.block.Block;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PalettePlacementTest {

    @Test
    void testCommandFeedbackDetectionTranslatable() {
        assertTrue(PalettePlacementManager.isCommandFeedbackMessage(
                Text.translatable("commands.setblock.success", 10, 64, 20)
        ));
        assertTrue(PalettePlacementManager.isCommandFeedbackMessage(
                Text.translatable("commands.teleport.success.entity.single", "Steve", 10, 64, 20)
        ));
        assertTrue(PalettePlacementManager.isCommandFeedbackMessage(
                Text.translatable("worldedit.set.success", 1)
        ));
        assertTrue(PalettePlacementManager.isCommandFeedbackMessage(
                Text.translatable("fawe.set.success", 1)
        ));
    }

    @Test
    void testCommandFeedbackDetectionRawString() {
        assertTrue(PalettePlacementManager.isCommandFeedbackMessage(
                Text.literal("Changed the block at 100, 64, 200")
        ));
        assertTrue(PalettePlacementManager.isCommandFeedbackMessage(
                Text.literal("Set the block at 100, 64, 200")
        ));
        assertTrue(PalettePlacementManager.isCommandFeedbackMessage(
                Text.literal("Se ha colocado el bloque en 100, 64, 200")
        ));
        assertTrue(PalettePlacementManager.isCommandFeedbackMessage(
                Text.literal("Bloque colocado en 100, 64, 200")
        ));
        assertTrue(PalettePlacementManager.isCommandFeedbackMessage(
                Text.literal("First position set to (100, 64, 200).")
        ));
        assertTrue(PalettePlacementManager.isCommandFeedbackMessage(
                Text.literal("Second position set to (100, 64, 200).")
        ));
        assertTrue(PalettePlacementManager.isCommandFeedbackMessage(
                Text.literal("Operation completed (1 blocks affected).")
        ));
        assertTrue(PalettePlacementManager.isCommandFeedbackMessage(
                Text.literal("Operación completada (9 bloques afectados).")
        ));
        assertTrue(PalettePlacementManager.isCommandFeedbackMessage(
                Text.literal("Teleported Steve to 100.5, 64.0, 200.5")
        ));
        assertTrue(PalettePlacementManager.isCommandFeedbackMessage(
                Text.literal("Se ha teletransportado a Steve a 100.5, 64.0, 200.5")
        ));
    }

    @Test
    void testNonFeedbackMessagesNotSuppressed() {
        assertFalse(PalettePlacementManager.isCommandFeedbackMessage(null));
        assertFalse(PalettePlacementManager.isCommandFeedbackMessage(Text.literal("")));
        assertFalse(PalettePlacementManager.isCommandFeedbackMessage(Text.literal("Hello world!")));
        assertFalse(PalettePlacementManager.isCommandFeedbackMessage(Text.literal("<Steve> Awesome palette!")));
        assertFalse(PalettePlacementManager.isCommandFeedbackMessage(Text.literal("Alex joined the game")));
        assertFalse(PalettePlacementManager.isCommandFeedbackMessage(Text.literal("Steve fell out of the world")));
        assertFalse(PalettePlacementManager.isCommandFeedbackMessage(Text.translatable("chat.type.text", "Alex", "Hi")));
    }

    @Test
    void testSuppressionWindow() throws InterruptedException {
        PalettePlacementManager manager = PalettePlacementManager.getInstance();
        manager.startSuppression(150);
        assertTrue(manager.isSuppressingChat());

        Thread.sleep(175);
        assertFalse(manager.isSuppressingChat());
    }

    @Test
    void testSingleplayerFlagsDoNotNotifyNeighbors() {
        int flags = PalettePlacementManager.SINGLEPLAYER_BLOCK_FLAGS;

        // NOTIFY_NEIGHBORS must be excluded to avoid triggering physics or falling sand
        assertEquals(0, flags & Block.NOTIFY_NEIGHBORS);

        // listeners, force state, skip drops, skip block added callbacks, and moved flags must be enabled
        assertNotEquals(0, flags & Block.NOTIFY_LISTENERS);
        assertNotEquals(0, flags & Block.FORCE_STATE);
        assertNotEquals(0, flags & Block.SKIP_DROPS);
        assertNotEquals(0, flags & Block.MOVED);
        assertNotEquals(0, flags & Block.SKIP_BLOCK_ADDED_CALLBACK);
    }

    @Test
    void testBlockStateResolution() {
        assertNull(PalettePlacementManager.getBlockStateForPlacement(null));
        assertNull(PalettePlacementManager.getBlockStateForPlacement(""));
        assertNull(PalettePlacementManager.getBlockStateForPlacement("   "));
        assertNull(PalettePlacementManager.getBlockStateForPlacement("invalid:item_that_does_not_exist"));
    }
}
