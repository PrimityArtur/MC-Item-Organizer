package com.itemorganizer.storage;

import com.itemorganizer.core.model.PaletteData;
import com.itemorganizer.core.model.PaletteRow;

import java.nio.file.Files;
import java.nio.file.Path;

// repository for hotbar palettes in paletas.json
public class PaletteRepository {
    private final Path paletteFile;
    private PaletteData cachedData;

    public PaletteRepository(Path baseDir) {
        this(baseDir, "paletas.json");
    }

    public PaletteRepository(Path baseDir, String fileName) {
        this.paletteFile = baseDir.resolve(fileName);
    }

    public PaletteData load() {
        if (!Files.exists(paletteFile)) {
            cachedData = new PaletteData();
            // initial default empty row
            cachedData.addRow(new PaletteRow());
            save(cachedData);
            return cachedData;
        }
        cachedData = JsonHelper.load(paletteFile, PaletteData.class, new PaletteData());
        if (cachedData.getRows().isEmpty()) {
            cachedData.addRow(new PaletteRow());
        }
        return cachedData;
    }

    public boolean save(PaletteData data) {
        if (data == null) return false;
        this.cachedData = data;
        return JsonHelper.saveAtomic(paletteFile, data);
    }

    public PaletteData getData() {
        if (cachedData == null) {
            return load();
        }
        return cachedData;
    }
}
