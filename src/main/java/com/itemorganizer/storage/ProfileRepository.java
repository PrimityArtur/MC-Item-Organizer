package com.itemorganizer.storage;

import com.itemorganizer.ItemOrganizer;
import com.itemorganizer.core.model.ProfileData;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// repository for profile management in config/itemorganizer/profiles/
public class ProfileRepository {
    public static final String DEFAULT_PROFILE_NAME = "default";
    private final Path profilesDir;

    public ProfileRepository(Path baseDir) {
        this.profilesDir = baseDir.resolve("profiles");
    }

    public void init() {
        try {
            if (!Files.exists(profilesDir)) {
                Files.createDirectories(profilesDir);
            }
            ensureDefaultProfile();
        } catch (IOException e) {
            ItemOrganizer.LOGGER.error("failed initializing profiles directory: {}", e.getMessage());
        }
    }

    // create starter default.json if profiles directory is empty
    public void ensureDefaultProfile() {
        List<String> existing = listProfiles();
        if (existing.isEmpty()) {
            ProfileData defaultProfile = new ProfileData(DEFAULT_PROFILE_NAME);

            // row 0: white blocks
            defaultProfile.setItemAt("minecraft:white_concrete", 0, 0);
            defaultProfile.setItemAt("minecraft:calcite", 1, 0);
            defaultProfile.setItemAt("minecraft:diorite", 2, 0);
            defaultProfile.setItemAt("minecraft:iron_block", 3, 0);

            // row 1: stone blocks with gap at column 4
            defaultProfile.setItemAt("minecraft:stone", 0, 1);
            defaultProfile.setItemAt("minecraft:stone_bricks", 1, 1);
            defaultProfile.setItemAt("minecraft:andesite", 2, 1);
            defaultProfile.setItemAt("minecraft:cobblestone", 3, 1);
            defaultProfile.setItemAt("minecraft:cracked_stone_bricks", 5, 1);

            // row 2: deepslate
            defaultProfile.setItemAt("minecraft:deepslate", 0, 2);
            defaultProfile.setItemAt("minecraft:deepslate_bricks", 1, 2);
            defaultProfile.setItemAt("minecraft:cobbled_deepslate", 2, 2);
            defaultProfile.setItemAt("minecraft:blackstone", 4, 2);
            defaultProfile.setItemAt("minecraft:obsidian", 5, 2);

            // row 4: wood planks gradient
            defaultProfile.setItemAt("minecraft:oak_planks", 0, 4);
            defaultProfile.setItemAt("minecraft:birch_planks", 1, 4);
            defaultProfile.setItemAt("minecraft:spruce_planks", 2, 4);
            defaultProfile.setItemAt("minecraft:jungle_planks", 3, 4);
            defaultProfile.setItemAt("minecraft:acacia_planks", 4, 4);
            defaultProfile.setItemAt("minecraft:dark_oak_planks", 5, 4);
            defaultProfile.setItemAt("minecraft:mangrove_planks", 6, 4);
            defaultProfile.setItemAt("minecraft:cherry_planks", 7, 4);
            defaultProfile.setItemAt("minecraft:bamboo_planks", 8, 4);

            saveProfile(defaultProfile);
            ItemOrganizer.LOGGER.info("created default profile with starter template: {}.json", DEFAULT_PROFILE_NAME);
        }
    }

    public List<String> listProfiles() {
        List<String> names = new ArrayList<>();
        if (!Files.exists(profilesDir)) {
            return names;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(profilesDir, "*.json")) {
            for (Path entry : stream) {
                String fileName = entry.getFileName().toString();
                String profileName = fileName.substring(0, fileName.length() - 5); // remove .json
                names.add(profileName);
            }
        } catch (IOException e) {
            ItemOrganizer.LOGGER.error("failed listing profiles in {}: {}", profilesDir, e.getMessage());
        }
        return names;
    }

    public ProfileData loadProfile(String name) {
        Path file = getProfilePath(name);
        if (!Files.exists(file)) {
            return new ProfileData(name);
        }
        ProfileData profile = JsonHelper.load(file, ProfileData.class, new ProfileData(name));
        if (profile.getName() == null || profile.getName().isEmpty()) {
            profile.setName(name);
        }
        return profile;
    }

    public boolean saveProfile(ProfileData profile) {
        if (profile == null || profile.getName() == null || profile.getName().trim().isEmpty()) {
            return false;
        }
        Path file = getProfilePath(profile.getName());
        return JsonHelper.saveAtomic(file, profile);
    }

    public boolean renameProfile(String oldName, String newName) {
        if (oldName == null || newName == null || newName.trim().isEmpty()) {
            return false;
        }
        Path oldFile = getProfilePath(oldName);
        Path newFile = getProfilePath(newName);

        if (!Files.exists(oldFile) || Files.exists(newFile)) {
            return false;
        }

        ProfileData data = loadProfile(oldName);
        data.setName(newName);
        if (saveProfile(data)) {
            try {
                Files.deleteIfExists(oldFile);
                return true;
            } catch (IOException e) {
                ItemOrganizer.LOGGER.error("failed deleting old profile file after rename: {}", e.getMessage());
            }
        }
        return false;
    }

    public boolean deleteProfile(String name) {
        if (name == null || name.equalsIgnoreCase(DEFAULT_PROFILE_NAME)) {
            // avoid deleting default profile to maintain stability
            return false;
        }
        Path file = getProfilePath(name);
        try {
            return Files.deleteIfExists(file);
        } catch (IOException e) {
            ItemOrganizer.LOGGER.error("failed deleting profile {}: {}", name, e.getMessage());
            return false;
        }
    }

    private Path getProfilePath(String name) {
        // sanitize filename
        String sanitized = name.replaceAll("[\\\\/:*?\"<>|]", "_");
        return profilesDir.resolve(sanitized + ".json");
    }
}
