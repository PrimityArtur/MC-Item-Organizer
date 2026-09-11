package com.itemorganizer.palette;

import com.itemorganizer.gui.palette.CreatePaletteState;
import com.itemorganizer.gui.undo.CreatePaletteUndoAction;
import com.itemorganizer.gui.undo.UndoManager;
import com.itemorganizer.gui.util.BlockPropertyHelper;
import com.itemorganizer.gui.util.PaletteGenerator;
import com.itemorganizer.gui.viewmodel.OrganizerViewModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CreatePaletteTest {

    @BeforeEach
    void setUp() {
        UndoManager.getInstance().clear();
        PaletteGenerator.clearCache();
        BlockPropertyHelper.clearCache();
    }

    @Test
    void testInitialStateDefaults() {
        CreatePaletteState state = new CreatePaletteState();
        assertEquals(9, state.getSlotCount());
        assertEquals(1, state.getResultRowCount());
        assertEquals(9, state.getInputSlots().size());
        assertEquals(9, state.getResultRow(0).size());
        assertFalse(state.isFilterCube());
        assertFalse(state.isFilterSolid());
        assertFalse(state.isFilterTransparent());
        assertFalse(state.isFilterUniformTexture());
        assertFalse(state.isFilterOres());
        assertFalse(state.isFilterGlazed());
        assertFalse(state.isFilterLights());
    }

    @Test
    void testSlotCountBoundsAndResizing() {
        CreatePaletteState state = new CreatePaletteState();
        state.setSlotCount(5);
        assertEquals(5, state.getSlotCount());
        assertEquals(5, state.getInputSlots().size());
        assertEquals(5, state.getResultRow(0).size());

        state.setSlotCount(1);
        assertEquals(2, state.getSlotCount());

        state.setSlotCount(50);
        assertEquals(36, state.getSlotCount());
    }

    @Test
    void testDeepCopyIntegrity() {
        CreatePaletteState state = new CreatePaletteState();
        state.setInputSlot(0, "minecraft:white_concrete");
        state.setInputSlot(8, "minecraft:black_concrete");
        state.setFilterCube(true);
        state.setFilterOres(true);
        state.setFilterGlazed(true);
        state.setFilterLights(true);
        state.cycleSeed(0, 4);

        CreatePaletteState copy = state.copy();
        assertEquals(state.getSlotCount(), copy.getSlotCount());
        assertEquals("minecraft:white_concrete", copy.getInputSlot(0));
        assertEquals("minecraft:black_concrete", copy.getInputSlot(8));
        assertTrue(copy.isFilterCube());
        assertTrue(copy.isFilterOres());
        assertTrue(copy.isFilterGlazed());
        assertTrue(copy.isFilterLights());
        assertEquals(1, copy.getSeedOffsets(0)[4]);

        copy.setInputSlot(0, "minecraft:red_concrete");
        assertEquals("minecraft:white_concrete", state.getInputSlot(0));
        assertEquals("minecraft:red_concrete", copy.getInputSlot(0));
    }

    @Test
    void testPaletteGeneratorEmptyInput() {
        CreatePaletteState state = new CreatePaletteState();
        PaletteGenerator.generate(state);

        List<String> row = state.getResultRow(0);
        assertEquals(9, row.size());
        for (String slot : row) {
            assertEquals("", slot);
        }
    }

    @Test
    void testPaletteGeneratorSingleAnchor() {
        CreatePaletteState state = new CreatePaletteState();
        state.setInputSlot(0, "minecraft:white_concrete");
        PaletteGenerator.generate(state);

        List<String> row = state.getResultRow(0);
        assertEquals(9, row.size());
        assertEquals("minecraft:white_concrete", row.get(0));
        for (int i = 1; i < 9; i++) {
            assertFalse(row.get(i).isEmpty());
        }
    }

    @Test
    void testPaletteGeneratorTwoAnchorsInterpolation() {
        CreatePaletteState state = new CreatePaletteState();
        state.setInputSlot(0, "minecraft:white_concrete");
        state.setInputSlot(8, "minecraft:black_concrete");
        PaletteGenerator.generate(state);

        List<String> row = state.getResultRow(0);
        assertEquals(9, row.size());
        assertEquals("minecraft:white_concrete", row.get(0));
        assertEquals("minecraft:black_concrete", row.get(8));

        for (int i = 0; i < 9; i++) {
            assertNotNull(row.get(i));
            assertFalse(row.get(i).isEmpty(), "Slot " + i + " should not be empty");
        }
    }

    @Test
    void testCandidateSeedCycling() {
        CreatePaletteState state = new CreatePaletteState();
        state.setInputSlot(0, "minecraft:white_concrete");
        state.setInputSlot(8, "minecraft:black_concrete");
        PaletteGenerator.generate(state);

        String initialMid = state.getResultRow(0).get(4);
        state.cycleSeed(0, 4);
        PaletteGenerator.generate(state);
        String secondMid = state.getResultRow(0).get(4);

        assertNotNull(secondMid);
        assertFalse(secondMid.isEmpty());
    }

    @Test
    void testMultiRowGeneration() {
        CreatePaletteState state = new CreatePaletteState();
        state.setInputSlot(0, "minecraft:red_concrete");
        state.setInputSlot(8, "minecraft:blue_concrete");
        state.addResultRow();
        assertEquals(2, state.getResultRowCount());

        state.cycleSeed(1, 4);
        PaletteGenerator.generate(state);

        List<String> row1 = state.getResultRow(0);
        List<String> row2 = state.getResultRow(1);
        assertEquals(9, row1.size());
        assertEquals(9, row2.size());
        assertEquals("minecraft:red_concrete", row1.get(0));
        assertEquals("minecraft:red_concrete", row2.get(0));
    }

    @Test
    void testToggleFiltering() {
        CreatePaletteState state = new CreatePaletteState();
        state.setInputSlot(0, "minecraft:glass");
        state.setInputSlot(8, "minecraft:white_stained_glass");
        state.setFilterTransparent(true);
        PaletteGenerator.generate(state);

        List<String> row = state.getResultRow(0);
        for (String id : row) {
            assertTrue(BlockPropertyHelper.isTransparent(id), "Expected transparent block for " + id);
        }
    }

    @Test
    void testTransparentExcludedWhenToggleInactive() {
        CreatePaletteState state = new CreatePaletteState();
        state.setInputSlot(0, "minecraft:white_concrete");
        state.setInputSlot(8, "minecraft:blue_concrete");
        assertFalse(state.isFilterTransparent());
        PaletteGenerator.generate(state);

        List<String> row = state.getResultRow(0);
        for (String id : row) {
            assertFalse(BlockPropertyHelper.isTransparent(id), "Transparent block " + id + " must not appear when filterTransparent is false");
        }
    }

    @Test
    void testOresExcludedWhenToggleInactive() {
        CreatePaletteState state = new CreatePaletteState();
        state.setInputSlot(0, "minecraft:stone");
        state.setInputSlot(8, "minecraft:deepslate");
        assertFalse(state.isFilterOres());
        PaletteGenerator.generate(state);

        List<String> row = state.getResultRow(0);
        for (String id : row) {
            assertFalse(BlockPropertyHelper.isOre(id), "Ore block " + id + " must not appear when filterOres is false");
        }
    }

    @Test
    void testGlazedExcludedWhenToggleInactive() {
        CreatePaletteState state = new CreatePaletteState();
        state.setInputSlot(0, "minecraft:orange_terracotta");
        state.setInputSlot(8, "minecraft:red_terracotta");
        assertFalse(state.isFilterGlazed());
        PaletteGenerator.generate(state);

        List<String> row = state.getResultRow(0);
        for (String id : row) {
            assertFalse(BlockPropertyHelper.isGlazedTerracotta(id), "Glazed terracotta " + id + " must not appear when filterGlazed is false");
        }
    }

    @Test
    void testLightsExcludedWhenToggleInactive() {
        CreatePaletteState state = new CreatePaletteState();
        state.setInputSlot(0, "minecraft:yellow_concrete");
        state.setInputSlot(8, "minecraft:orange_concrete");
        assertFalse(state.isFilterLights());
        PaletteGenerator.generate(state);

        List<String> row = state.getResultRow(0);
        for (String id : row) {
            assertFalse(BlockPropertyHelper.isLightBlock(id), "Light block " + id + " must not appear when filterLights is false");
        }
    }

    @Test
    void testSmoothSandToGreenConcreteGradient() {
        CreatePaletteState state = new CreatePaletteState();
        state.setSlotCount(17);
        state.setInputSlot(0, "minecraft:sand");
        state.setInputSlot(16, "minecraft:green_concrete");
        state.setFilterCube(true);
        PaletteGenerator.generate(state);

        List<String> row = state.getResultRow(0);
        assertEquals(17, row.size());
        assertEquals("minecraft:sand", row.get(0));
        assertEquals("minecraft:green_concrete", row.get(16));

        for (int i = 0; i < 17; i++) {
            String item = row.get(i);
            assertNotNull(item);
            assertFalse(item.isEmpty(), "Slot " + i + " must not be empty");
            assertFalse(item.contains("diorite"), "Slot " + i + " (" + item + ") should not be diorite");
            assertFalse(item.contains("cherry_planks"), "Slot " + i + " (" + item + ") should not be cherry planks");
            assertFalse(BlockPropertyHelper.isOre(item), "Slot " + i + " (" + item + ") should not be an ore");
            assertFalse(BlockPropertyHelper.isLightBlock(item), "Slot " + i + " (" + item + ") should not be a light block");
        }
    }

    @Test
    void testBlockPropertyHelperEvaluation() {
        assertTrue(BlockPropertyHelper.isFullCube("minecraft:stone"));
        assertFalse(BlockPropertyHelper.isFullCube("minecraft:air"));

        assertTrue(BlockPropertyHelper.isSolidCollision("minecraft:stone"));
        assertFalse(BlockPropertyHelper.isSolidCollision("minecraft:air"));

        assertTrue(BlockPropertyHelper.isTransparent("minecraft:glass"));
        assertFalse(BlockPropertyHelper.isTransparent("minecraft:stone"));

        assertTrue(BlockPropertyHelper.hasUniformTexture("minecraft:stone"));
        assertFalse(BlockPropertyHelper.hasUniformTexture("minecraft:crafting_table"));

        assertTrue(BlockPropertyHelper.isOre("minecraft:iron_ore"));
        assertTrue(BlockPropertyHelper.isOre("minecraft:deepslate_diamond_ore"));
        assertTrue(BlockPropertyHelper.isOre("minecraft:ancient_debris"));
        assertFalse(BlockPropertyHelper.isOre("minecraft:iron_block"));

        assertTrue(BlockPropertyHelper.isGlazedTerracotta("minecraft:orange_glazed_terracotta"));
        assertFalse(BlockPropertyHelper.isGlazedTerracotta("minecraft:orange_terracotta"));

        assertTrue(BlockPropertyHelper.isLightBlock("minecraft:sea_lantern"));
        assertTrue(BlockPropertyHelper.isLightBlock("minecraft:glowstone"));
        assertFalse(BlockPropertyHelper.isLightBlock("minecraft:stone"));

        assertTrue(BlockPropertyHelper.isBuildingBlock("minecraft:stone"));
        assertFalse(BlockPropertyHelper.isBuildingBlock("minecraft:diamond_sword"));
        assertFalse(BlockPropertyHelper.isBuildingBlock("minecraft:command_block"));
        assertFalse(BlockPropertyHelper.isBuildingBlock("minecraft:sculk_sensor"));
        assertFalse(BlockPropertyHelper.isBuildingBlock("minecraft:piston"));
        assertFalse(BlockPropertyHelper.isBuildingBlock("minecraft:player_head"));
        assertFalse(BlockPropertyHelper.isBuildingBlock("minecraft:spawner"));

        // wood and hyphae uniform texture verification
        assertTrue(BlockPropertyHelper.hasUniformTexture("minecraft:oak_wood"));
        assertTrue(BlockPropertyHelper.hasUniformTexture("minecraft:crimson_hyphae"));
        assertTrue(BlockPropertyHelper.hasUniformTexture("minecraft:stripped_warped_hyphae"));
        assertFalse(BlockPropertyHelper.hasUniformTexture("minecraft:oak_log"));
        assertFalse(BlockPropertyHelper.hasUniformTexture("minecraft:crimson_stem"));
    }

    @Test
    void testPaletteBlacklistExcludesSpecialBlocks() {
        CreatePaletteState state = new CreatePaletteState();
        state.setSlotCount(18);
        state.setInputSlot(0, "minecraft:sand");
        state.setInputSlot(8, "minecraft:stone");
        state.setInputSlot(17, "minecraft:light_blue_wool");
        // even with no toggles active, blacklisted items must never appear
        PaletteGenerator.generate(state);

        for (int r = 0; r < state.getResultRowCount(); r++) {
            List<String> row = state.getResultRow(r);
            for (int s = 0; s < row.size(); s++) {
                String id = row.get(s);
                assertFalse(id.contains("spawner"), "Row " + r + " slot " + s + " contains spawner: " + id);
                assertFalse(id.contains("sculk"), "Row " + r + " slot " + s + " contains sculk: " + id);
                assertFalse(id.contains("head"), "Row " + r + " slot " + s + " contains head: " + id);
                assertFalse(id.contains("skull"), "Row " + r + " slot " + s + " contains skull: " + id);
                assertFalse(id.contains("piston"), "Row " + r + " slot " + s + " contains piston: " + id);
            }
        }
    }

    @Test
    void testAntiRepetitionPreventsExcessivePlateaus() {
        CreatePaletteState state = new CreatePaletteState();
        state.setSlotCount(18);
        state.setInputSlot(0, "minecraft:sand");
        state.setInputSlot(8, "minecraft:stone");
        state.setInputSlot(17, "minecraft:light_blue_wool");
        PaletteGenerator.generate(state);

        List<String> row = state.getResultRow(0);
        int consecutive = 1;
        String prev = "";
        for (int i = 0; i < row.size(); i++) {
            String item = row.get(i);
            assertNotNull(item);
            assertFalse(item.isEmpty());
            if (item.equals(prev)) {
                consecutive++;
                assertTrue(consecutive <= 2, "Item " + item + " repeated more than twice consecutively at slot " + i);
            } else {
                consecutive = 1;
                prev = item;
            }
        }
    }

    @Test
    void testUndoRedoWorkflow() {
        OrganizerViewModel vm = new OrganizerViewModel();
        CreatePaletteState state = vm.getCreatePaletteState();
        assertEquals(9, state.getSlotCount());

        CreatePaletteState before = state.copy();
        state.setInputSlot(0, "minecraft:stone");
        PaletteGenerator.generate(state);
        UndoManager.getInstance().record(new CreatePaletteUndoAction(before, state));

        assertTrue(UndoManager.getInstance().canUndo());
        assertFalse(UndoManager.getInstance().canRedo());
        assertEquals("minecraft:stone", state.getInputSlot(0));

        UndoManager.getInstance().undo(null, vm);
        assertEquals("", state.getInputSlot(0));
        assertTrue(UndoManager.getInstance().canRedo());

        UndoManager.getInstance().redo(null, vm);
        assertEquals("minecraft:stone", state.getInputSlot(0));
    }

    @Test
    void testRemoveResultRowByIndex() {
        CreatePaletteState state = new CreatePaletteState();
        state.addResultRow();
        state.addResultRow();
        assertEquals(3, state.getResultRowCount());

        state.getResultRow(0).set(0, "minecraft:stone");
        state.getResultRow(1).set(0, "minecraft:dirt");
        state.getResultRow(2).set(0, "minecraft:sand");

        state.removeResultRow(1);
        assertEquals(2, state.getResultRowCount());
        assertEquals("minecraft:stone", state.getResultRow(0).get(0));
        assertEquals("minecraft:sand", state.getResultRow(1).get(0));

        state.removeResultRow(0);
        assertEquals(1, state.getResultRowCount());
        assertEquals("minecraft:sand", state.getResultRow(0).get(0));

        state.removeResultRow(0);
        assertEquals(1, state.getResultRowCount());
        assertEquals("", state.getResultRow(0).get(0));
    }

    @Test
    void testUnlimitedResultRows() {
        CreatePaletteState state = new CreatePaletteState();
        assertEquals(1, state.getResultRowCount());

        for (int i = 0; i < 15; i++) {
            state.addResultRow();
        }

        assertEquals(16, state.getResultRowCount());
        for (int r = 0; r < 16; r++) {
            assertEquals(9, state.getResultRow(r).size());
        }
    }
}
