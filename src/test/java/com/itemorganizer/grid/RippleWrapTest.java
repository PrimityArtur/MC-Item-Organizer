package com.itemorganizer.grid;

import com.itemorganizer.core.model.ItemSlotPosition;
import com.itemorganizer.core.model.ProfileData;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class RippleWrapTest {

    // simulate ripple wrap insertion logic with row wrapping
    private void simulateRippleWrap(ProfileData profile, int cols, String itemId, int targetCol, int targetRow) {
        int targetIdx = targetRow * cols + targetCol;

        if (profile.findItemAt(targetCol, targetRow).isEmpty()) {
            profile.setItemAt(itemId, targetCol, targetRow);
            return;
        }

        int emptyIdx = targetIdx + 1;
        while (true) {
            int checkCol = emptyIdx % cols;
            int checkRow = emptyIdx / cols;
            if (profile.findItemAt(checkCol, checkRow).isEmpty()) {
                break;
            }
            emptyIdx++;
        }

        for (int i = emptyIdx - 1; i >= targetIdx; i--) {
            int fromCol = i % cols;
            int fromRow = i / cols;
            int toCol = (i + 1) % cols;
            int toRow = (i + 1) / cols;

            Optional<ItemSlotPosition> movingItem = profile.findItemAt(fromCol, fromRow);
            if (movingItem.isPresent()) {
                String id = movingItem.get().getItemId();
                profile.removeAt(fromCol, fromRow);
                profile.setItemAt(id, toCol, toRow);
            }
        }

        profile.setItemAt(itemId, targetCol, targetRow);
    }

    @Test
    void testDirectPlacementOnEmptySlot() {
        ProfileData profile = new ProfileData("test");
        simulateRippleWrap(profile, 5, "minecraft:stone", 2, 0);

        assertTrue(profile.findItemAt(2, 0).isPresent());
        assertEquals("minecraft:stone", profile.findItemAt(2, 0).get().getItemId());
        assertEquals(1, profile.getItems().size());
    }

    @Test
    void testRippleShiftToRightUntilEmptySlot() {
        ProfileData profile = new ProfileData("test");
        int cols = 5;

        // row 0: stone (0,0), dirt (1,0), grass (2,0), empty (3,0), gold (4,0)
        profile.setItemAt("minecraft:stone", 0, 0);
        profile.setItemAt("minecraft:dirt", 1, 0);
        profile.setItemAt("minecraft:grass_block", 2, 0);
        profile.setItemAt("minecraft:gold_block", 4, 0);

        // insert diamond_block at col 1 between stone and dirt
        simulateRippleWrap(profile, cols, "minecraft:diamond_block", 1, 0);

        // expected:
        // col 0: stone (intact)
        // col 1: diamond_block (new)
        // col 2: dirt (shifted)
        // col 3: grass_block (shifted to empty gap)
        // col 4: gold_block (intact)
        assertEquals("minecraft:stone", profile.findItemAt(0, 0).get().getItemId());
        assertEquals("minecraft:diamond_block", profile.findItemAt(1, 0).get().getItemId());
        assertEquals("minecraft:dirt", profile.findItemAt(2, 0).get().getItemId());
        assertEquals("minecraft:grass_block", profile.findItemAt(3, 0).get().getItemId());
        assertEquals("minecraft:gold_block", profile.findItemAt(4, 0).get().getItemId());
        assertEquals(5, profile.getItems().size());
    }

    @Test
    void testRippleWrapToNextRowWhenRowIsFull() {
        ProfileData profile = new ProfileData("test");
        int cols = 3; // 3 columns grid

        // row 0 full: (0,0)=A, (1,0)=B, (2,0)=C
        // row 1: (0,1)=empty
        profile.setItemAt("A", 0, 0);
        profile.setItemAt("B", 1, 0);
        profile.setItemAt("C", 2, 0);

        // insert X at col 1 row 0
        simulateRippleWrap(profile, cols, "X", 1, 0);

        // expected:
        // (0,0)=A
        // (1,0)=X
        // (2,0)=B (shifted)
        // (0,1)=C (wrapped to next row col 0)
        assertEquals("A", profile.findItemAt(0, 0).get().getItemId());
        assertEquals("X", profile.findItemAt(1, 0).get().getItemId());
        assertEquals("B", profile.findItemAt(2, 0).get().getItemId());
        assertEquals("C", profile.findItemAt(0, 1).get().getItemId());
        assertEquals(4, profile.getItems().size());
    }
}
