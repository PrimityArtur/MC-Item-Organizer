package com.itemorganizer.palette;

import com.itemorganizer.core.model.PaletteData;
import com.itemorganizer.core.model.PaletteRow;
import com.itemorganizer.gui.undo.PaletteAddDeleteUndoAction;
import com.itemorganizer.gui.undo.UndoManager;
import com.itemorganizer.storage.StorageManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PaletteInsertStartTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        StorageManager storage = new StorageManager(tempDir);
        StorageManager.setInstanceForTesting(storage);
        storage.init();
        UndoManager.getInstance().clear();
    }

    @Test
    void testInsertPaletteAtStartStandard() {
        PaletteData data = new PaletteData();
        PaletteRow row1 = new PaletteRow("id1", "Palette 1", 9);
        PaletteRow row2 = new PaletteRow("id2", "Palette 2", 9);
        data.addRow(row1);
        data.addRow(row2);

        assertEquals(2, data.getRows().size());
        assertEquals("id1", data.getRows().get(0).getId());

        PaletteRow newRow = new PaletteRow("id_new", "New Top Palette", 9);
        data.insertRow(0, newRow);

        assertEquals(3, data.getRows().size());
        assertEquals("id_new", data.getRows().get(0).getId());
        assertEquals("id1", data.getRows().get(1).getId());
        assertEquals("id2", data.getRows().get(2).getId());
    }

    @Test
    void testInsertPaletteAtStartInfinite() {
        PaletteData infData = new PaletteData();
        PaletteRow inf1 = new PaletteRow("inf1", List.of("minecraft:stone"));
        PaletteRow inf2 = new PaletteRow("inf2", List.of("minecraft:dirt"));
        infData.addRow(inf1);
        infData.addRow(inf2);

        PaletteRow newTop = new PaletteRow("inf_new", "Infinite New", 9);
        infData.insertRow(0, newTop);

        assertEquals(3, infData.getRows().size());
        assertEquals("inf_new", infData.getRows().get(0).getId());
        assertEquals("inf1", infData.getRows().get(1).getId());
    }

    @Test
    void testUndoRedoInsertAtStart() {
        PaletteData data = StorageManager.getInstance().getPaletteRepository().getData();
        data.getRows().clear();
        PaletteRow row1 = new PaletteRow("p1", "P1", 9);
        data.addRow(row1);
        StorageManager.getInstance().getPaletteRepository().save(data);

        PaletteRow newRow = new PaletteRow("top_palette", "Top Palette", 9);
        data.insertRow(0, newRow);
        StorageManager.getInstance().getPaletteRepository().save(data);

        PaletteAddDeleteUndoAction action = new PaletteAddDeleteUndoAction(newRow, 0, true, false);
        UndoManager.getInstance().record(action);

        assertEquals(2, data.getRows().size());
        assertEquals("top_palette", data.getRows().get(0).getId());

        com.itemorganizer.gui.viewmodel.OrganizerViewModel vm = new com.itemorganizer.gui.viewmodel.OrganizerViewModel(null, false);
        UndoManager.getInstance().undo(null, vm);

        assertEquals(1, data.getRows().size());
        assertEquals("p1", data.getRows().get(0).getId());

        UndoManager.getInstance().redo(null, vm);

        assertEquals(2, data.getRows().size());
        assertEquals("top_palette", data.getRows().get(0).getId());
    }
}
