package com.itemorganizer.storage;

import com.itemorganizer.core.model.ModConfig;

import java.nio.file.Files;
import java.nio.file.Path;

// repository for global config.json
public class ConfigRepository {
    private final Path configFile;
    private ModConfig cachedConfig;

    public ConfigRepository(Path baseDir) {
        this.configFile = baseDir.resolve("config.json");
    }

    public ModConfig load() {
        if (!Files.exists(configFile)) {
            cachedConfig = new ModConfig();
            save(cachedConfig);
            return cachedConfig;
        }
        cachedConfig = JsonHelper.load(configFile, ModConfig.class, new ModConfig());
        return cachedConfig;
    }

    public boolean save(ModConfig config) {
        if (config == null) return false;
        this.cachedConfig = config;
        return JsonHelper.saveAtomic(configFile, config);
    }

    public ModConfig getConfig() {
        if (cachedConfig == null) {
            return load();
        }
        return cachedConfig;
    }
}
