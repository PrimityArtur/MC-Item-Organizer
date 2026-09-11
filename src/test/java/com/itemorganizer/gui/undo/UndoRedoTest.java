package com.itemorganizer.gui.undo;

import com.itemorganizer.core.model.ModConfig;
import com.itemorganizer.core.model.PaletteData;
import com.itemorganizer.core.model.PaletteRow;
import com.itemorganizer.core.model.ProfileData;
import com.itemorganizer.gui.viewmodel.OrganizerViewModel;
import com.itemorganizer.storage.StorageManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class UndoRedoTest {

    @TempDir
    Path tempDir;

    private OrganizerViewModel viewModel;

    @BeforeEach
    void setUp() {
        StorageManager storage = new StorageManager(tempDir);
        StorageManager.setInstanceForTesting(storage);
        storage.init();

        ProfileData defaultProfile = new ProfileData("default");
        defaultProfile.setItemAt("minecraft:dirt", 0, 0);
        defaultProfile.setItemAt("minecraft:stone", 1, 0);
        storage.getProfileRepository().saveProfile(defaultProfile);

        PaletteData paletteData = new PaletteData();
        PaletteRow row1 = new PaletteRow("row1", List.of("minecraft:oak_planks", "minecraft:birch_planks"));
        paletteData.addRow(row1);
        storage.getPaletteRepository().save(paletteData);

        PaletteData infData = new PaletteData();
        PaletteRow infRow1 = new PaletteRow("infRow1", List.of("minecraft:glass"));
        infData.addRow(infRow1);
        storage.getInfinitePaletteRepository().save(infData);

        ModConfig config = new ModConfig();
        storage.getConfigRepository().save(config);

        viewModel = new OrganizerViewModel();
        UndoManager.getInstance().clear();
    }

    @Test
    void testUndoManagerBasicFlow() {
        UndoManager manager = UndoManager.getInstance();
        assertFalse(manager.canUndo());
        assertFalse(manager.canRedo());

        List<String> trace = new ArrayList<>();
        UndoAction action1 = new UndoAction() {
            @Override
            public void undo(net.minecraft.client.MinecraftClient client, OrganizerViewModel vm) {
                trace.add("undo1");
            }

            @Override
            public void redo(net.minecraft.client.MinecraftClient client, OrganizerViewModel vm) {
                trace.add("redo1");
            }
        };

        manager.record(action1);
        assertTrue(manager.canUndo());
        assertFalse(manager.canRedo());

        manager.undo(null, viewModel);
        assertEquals(List.of("undo1"), trace);
        assertFalse(manager.canUndo());
        assertTrue(manager.canRedo());

        manager.redo(null, viewModel);
        assertEquals(List.of("undo1", "redo1"), trace);
        assertTrue(manager.canUndo());
        assertFalse(manager.canRedo());
    }

    @Test
    void testRecordingClearsRedoStack() {
        UndoManager manager = UndoManager.getInstance();
        manager.record(new UndoAction() {
            @Override
            public void undo(net.minecraft.client.MinecraftClient client, OrganizerViewModel vm) {}
        });
        manager.undo(null, viewModel);
        assertTrue(manager.canRedo());

        manager.record(new UndoAction() {
            @Override
            public void undo(net.minecraft.client.MinecraftClient client, OrganizerViewModel vm) {}
        });
        assertFalse(manager.canRedo());
    }

    @Test
    void testProfileUndoAction() {
        ProfileData profile = viewModel.getActiveProfile();
        assertNotNull(profile);
        ProfileData before = profile.snapshot();

        profile.setItemAt("minecraft:diamond_block", 2, 0);
        profile.blockItem("minecraft:bedrock");
        StorageManager.getInstance().getProfileRepository().saveProfile(profile);
        ProfileData after = profile.snapshot();

        ProfileUndoAction action = new ProfileUndoAction(before, after);
        UndoManager.getInstance().record(action);

        assertTrue(profile.hasItem("minecraft:diamond_block"));
        assertTrue(profile.isItemBlocked("minecraft:bedrock"));

        UndoManager.getInstance().undo(null, viewModel);
        ProfileData activeAfterUndo = viewModel.getActiveProfile();
        assertFalse(activeAfterUndo.hasItem("minecraft:diamond_block"));
        assertFalse(activeAfterUndo.isItemBlocked("minecraft:bedrock"));
        assertTrue(activeAfterUndo.hasItem("minecraft:dirt"));
        assertTrue(activeAfterUndo.hasItem("minecraft:stone"));

        UndoManager.getInstance().redo(null, viewModel);
        ProfileData activeAfterRedo = viewModel.getActiveProfile();
        assertTrue(activeAfterRedo.hasItem("minecraft:diamond_block"));
        assertTrue(activeAfterRedo.isItemBlocked("minecraft:bedrock"));
    }

    @Test
    void testPaletteSearchFilterUndoAction() {
        PaletteRow filterRow = viewModel.getPaletteFilterRow();
        assertNotNull(filterRow);
        List<String> before = new ArrayList<>(filterRow.getSlots());

        filterRow.setSlot(0, "minecraft:redstone");
        filterRow.setSlot(1, "minecraft:emerald");
        List<String> after = new ArrayList<>(filterRow.getSlots());

        PaletteSearchFilterUndoAction action = new PaletteSearchFilterUndoAction(before, after);
        UndoManager.getInstance().record(action);

        assertEquals("minecraft:redstone", filterRow.getSlot(0));
        assertEquals("minecraft:emerald", filterRow.getSlot(1));

        UndoManager.getInstance().undo(null, viewModel);
        assertNull(filterRow.getSlot(0));
        assertNull(filterRow.getSlot(1));

        UndoManager.getInstance().redo(null, viewModel);
        assertEquals("minecraft:redstone", filterRow.getSlot(0));
        assertEquals("minecraft:emerald", filterRow.getSlot(1));
    }

    @Test
    void testPaletteAddDeleteUndoActionForAdd() {
        PaletteData paletteData = viewModel.getPaletteData();
        assertEquals(1, paletteData.getRows().size());

        PaletteRow newRow = new PaletteRow("newRow1", List.of("minecraft:gold_ingot"));
        int index = paletteData.getRows().size();
        paletteData.addRow(newRow);
        StorageManager.getInstance().getPaletteRepository().save(paletteData);

        PaletteAddDeleteUndoAction action = new PaletteAddDeleteUndoAction(newRow, index, true, false);
        UndoManager.getInstance().record(action);
        assertEquals(2, paletteData.getRows().size());

        UndoManager.getInstance().undo(null, viewModel);
        assertEquals(1, paletteData.getRows().size());
        assertNull(paletteData.findRowById("newRow1"));

        UndoManager.getInstance().redo(null, viewModel);
        assertEquals(2, paletteData.getRows().size());
        assertNotNull(paletteData.findRowById("newRow1"));
        assertEquals("minecraft:gold_ingot", paletteData.findRowById("newRow1").getSlot(0));
    }

    @Test
    void testPaletteAddDeleteUndoActionForDelete() {
        PaletteData paletteData = viewModel.getPaletteData();
        PaletteRow row1 = paletteData.getRows().get(0);
        PaletteRow snapshot = row1.snapshot();
        int index = 0;

        paletteData.removeRowById(row1.getId());
        StorageManager.getInstance().getPaletteRepository().save(paletteData);
        assertEquals(0, paletteData.getRows().size());

        PaletteAddDeleteUndoAction action = new PaletteAddDeleteUndoAction(snapshot, index, false, false);
        UndoManager.getInstance().record(action);

        UndoManager.getInstance().undo(null, viewModel);
        assertEquals(1, paletteData.getRows().size());
        assertEquals("row1", paletteData.getRows().get(0).getId());

        UndoManager.getInstance().redo(null, viewModel);
        assertEquals(0, paletteData.getRows().size());
    }

    @Test
    void testPaletteSlotUndoAction() {
        PaletteData paletteData = viewModel.getPaletteData();
        PaletteRow row1 = paletteData.findRowById("row1");
        assertNotNull(row1);

        String previous = row1.getSlot(0);
        String next = "minecraft:sand";

        row1.setSlot(0, next);
        StorageManager.getInstance().getPaletteRepository().save(paletteData);

        PaletteSlotUndoAction action = new PaletteSlotUndoAction("row1", 0, previous, next);
        UndoManager.getInstance().record(action);

        assertEquals("minecraft:sand", row1.getSlot(0));

        UndoManager.getInstance().undo(null, viewModel);
        assertEquals("minecraft:oak_planks", row1.getSlot(0));

        UndoManager.getInstance().redo(null, viewModel);
        assertEquals("minecraft:sand", row1.getSlot(0));
    }

    @Test
    void testPaletteFullUndoAction() {
        PaletteData paletteData = viewModel.getPaletteData();
        PaletteRow row1 = paletteData.findRowById("row1");
        assertNotNull(row1);

        List<String> previous = new ArrayList<>(row1.getSlots());
        List<String> next = List.of("minecraft:iron_block", "minecraft:gold_block");

        row1.setSlots(next);
        StorageManager.getInstance().getPaletteRepository().save(paletteData);

        PaletteFullUndoAction action = new PaletteFullUndoAction("row1", previous, next);
        UndoManager.getInstance().record(action);

        assertEquals("minecraft:iron_block", row1.getSlot(0));

        UndoManager.getInstance().undo(null, viewModel);
        assertEquals("minecraft:oak_planks", row1.getSlot(0));

        UndoManager.getInstance().redo(null, viewModel);
        assertEquals("minecraft:iron_block", row1.getSlot(0));
    }

    @Test
    void testKeyRedoConfig() {
        ModConfig config = new ModConfig();
        assertEquals("key.keyboard.y", config.getKeyRedo());

        config.setKeyRedo("key.keyboard.r");
        assertEquals("key.keyboard.r", config.getKeyRedo());

        config.setKeyRedo(null);
        assertEquals("key.keyboard.y", config.getKeyRedo());
    }
}
