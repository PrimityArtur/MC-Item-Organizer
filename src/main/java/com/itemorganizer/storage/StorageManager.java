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
    private final VersionCatalogRepository versionCatalogRepository;

    private StorageManager() {
        this.baseDir = FabricLoader.getInstance().getConfigDir().resolve("itemorganizer");
        this.configRepository = new ConfigRepository(baseDir);
        this.profileRepository = new ProfileRepository(baseDir);
        this.paletteRepository = new PaletteRepository(baseDir);
        this.versionCatalogRepository = new VersionCatalogRepository(baseDir);
    }

    public static synchronized StorageManager getInstance() {
        if (instance == null) {
            instance = new StorageManager();
        }
        return instance;
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
            versionCatalogRepository.load();
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

    public VersionCatalogRepository getVersionCatalogRepository() {
        return versionCatalogRepository;
    }
}
