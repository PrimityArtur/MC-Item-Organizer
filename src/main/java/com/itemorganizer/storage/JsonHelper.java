package com.itemorganizer.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.itemorganizer.ItemOrganizer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

// json serialization and atomic file writing helper
public class JsonHelper {
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public static <T> T load(Path file, Class<T> clazz, T fallback) {
        if (!Files.exists(file)) {
            return fallback;
        }

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            T result = GSON.fromJson(reader, clazz);
            return result != null ? result : fallback;
        } catch (Exception e) {
            ItemOrganizer.LOGGER.error("failed reading json file {}: {}", file, e.getMessage());
            return fallback;
        }
    }

    public static <T> boolean saveAtomic(Path file, T data) {
        try {
            Path parent = file.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            Path tempFile = file.resolveSibling(file.getFileName().toString() + ".tmp");

            try (Writer writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }

            try {
                Files.move(tempFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception atomicEx) {
                // fallback if atomic move is not supported by the filesystem
                Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING);
            }

            return true;
        } catch (Exception e) {
            ItemOrganizer.LOGGER.error("failed saving json file atomically at {}: {}", file, e.getMessage());
            return false;
        }
    }
}
