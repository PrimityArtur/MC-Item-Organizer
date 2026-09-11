package com.itemorganizer.storage;

import com.itemorganizer.ItemOrganizer;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// central storage manager for the mod
// base location: .minecraft/config/itemorganizer/
public class StorageManager {
    private static StorageManager instance;

    private final Path baseDir;
    private final ConfigRepository configRepository;
    private final ProfileRepository profileRepository;
    private final PaletteRepository paletteRepository;
    private final PaletteRepository infinitePaletteRepository;
    private final VersionCatalogRepository versionCatalogRepository;

    public StorageManager(Path baseDir) {
        if (baseDir != null) {
            this.baseDir = baseDir;
        } else {
            Path dir = null;
            try {
                if (FabricLoader.getInstance() != null) {
                    dir = FabricLoader.getInstance().getConfigDir().resolve("itemorganizer");
                }
            } catch (Throwable ignored) {
            }
            this.baseDir = (dir != null) ? dir : Path.of("build", "tmp", "test-config", "itemorganizer");
        }
        this.configRepository = new ConfigRepository(this.baseDir);
        this.profileRepository = new ProfileRepository(this.baseDir);
        this.paletteRepository = new PaletteRepository(this.baseDir);
        this.infinitePaletteRepository = new PaletteRepository(this.baseDir, "palette_inf.json");
        this.versionCatalogRepository = new VersionCatalogRepository(this.baseDir);
    }

    private StorageManager() {
        this(null);
    }

    public static synchronized StorageManager getInstance() {
        if (instance == null) {
            instance = new StorageManager();
        }
        return instance;
    }

    public static synchronized void setInstanceForTesting(StorageManager testInstance) {
        instance = testInstance;
    }

    public void init() {
        try {
            if (!Files.exists(baseDir)) {
                Files.createDirectories(baseDir);
            }
            // initialize repositories ensuring default files
            configRepository.load();
            profileRepository.init();
            paletteRepository.load();
            infinitePaletteRepository.load();
            versionCatalogRepository.load();

            // initialize and ensure category order file
            Path categoryOrderFile = baseDir.resolve("category_order.json");
            if (Files.exists(categoryOrderFile)) {
                com.itemorganizer.core.model.ItemCategory.loadOrderFromFile(categoryOrderFile);
            } else {
                com.itemorganizer.core.model.ItemCategory.saveOrderToFile(categoryOrderFile);
            }

            ItemOrganizer.LOGGER.info("ItemOrganizer StorageManager initialized at {}", baseDir);
        } catch (IOException e) {
            ItemOrganizer.LOGGER.error("failed initializing StorageManager: {}", e.getMessage());
        }
    }

    public Path getBaseDir() {
        return baseDir;
    }

    public ConfigRepository getConfigRepository() {
        return configRepository;
    }

    public ProfileRepository getProfileRepository() {
        return profileRepository;
    }

    public PaletteRepository getPaletteRepository() {
        return paletteRepository;
    }

    public PaletteRepository getInfinitePaletteRepository() {
        return infinitePaletteRepository;
    }

    public VersionCatalogRepository getVersionCatalogRepository() {
        return versionCatalogRepository;
    }
}
