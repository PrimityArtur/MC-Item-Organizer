package com.itemorganizer.storage;

import com.itemorganizer.core.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StorageTest {

    @TempDir
    Path tempDir;

    private ConfigRepository configRepository;
    private ProfileRepository profileRepository;
    private PaletteRepository paletteRepository;
    private VersionCatalogRepository versionCatalogRepository;

    @BeforeEach
    void setUp() {
        configRepository = new ConfigRepository(tempDir);
        profileRepository = new ProfileRepository(tempDir);
        paletteRepository = new PaletteRepository(tempDir);
        versionCatalogRepository = new VersionCatalogRepository(tempDir);
    }

    @Test
    void testConfigPersistence() {
        ModConfig config = configRepository.load();
        assertNotNull(config);
        assertEquals(0x101010, config.getBackgroundColor());
        assertEquals(0.60f, config.getTransparency(), 0.001f);

        // modify values
        config.setBackgroundColor(0xFF0000);
        config.setTransparency(0.85f);
        config.setScale(1.25f);
        config.setKeyOpenClose("key.keyboard.k");
        assertTrue(configRepository.save(config));

        // reload and verify
        ConfigRepository reloaded = new ConfigRepository(tempDir);
        ModConfig loaded = reloaded.load();
        assertEquals(0xFF0000, loaded.getBackgroundColor());
        assertEquals(0.85f, loaded.getTransparency(), 0.001f);
        assertEquals(1.25f, loaded.getScale(), 0.001f);
        assertEquals("key.keyboard.k", loaded.getKeyOpenClose());
    }

    @Test
    void testProfileLifecycleAndUniqueConstraint() {
        profileRepository.init();

        // verify default.json is created if empty
        List<String> profiles = profileRepository.listProfiles();
        assertTrue(profiles.contains("default"));

        // create new profile
        ProfileData profile = new ProfileData("test_palette");
        profile.setItemAt("minecraft:diamond_block", 0, 0);
        profile.setItemAt("minecraft:gold_block", 1, 0);
        profile.setItemAt("minecraft:iron_block", 2, 0);

        // relocate item without duplicates
        profile.setItemAt("minecraft:diamond_block", 5, 5);
        assertEquals(3, profile.getItems().size());
        assertTrue(profile.findItemAt(5, 5).isPresent());
        assertFalse(profile.findItemAt(0, 0).isPresent());

        // save profile
        assertTrue(profileRepository.saveProfile(profile));

        // load profile
        ProfileData loaded = profileRepository.loadProfile("test_palette");
        assertEquals("test_palette", loaded.getName());
        assertEquals(3, loaded.getItems().size());
        assertTrue(loaded.hasItem("minecraft:gold_block"));

        // rename profile
        assertTrue(profileRepository.renameProfile("test_palette", "renamed_palette"));
        assertFalse(profileRepository.listProfiles().contains("test_palette"));
        assertTrue(profileRepository.listProfiles().contains("renamed_palette"));

        // delete profile
        assertTrue(profileRepository.deleteProfile("renamed_palette"));
        assertFalse(profileRepository.listProfiles().contains("renamed_palette"));
    }

    @Test
    void testPaletteRowsAndSlotConstraint() {
        PaletteData data = paletteRepository.load();
        assertNotNull(data);
        assertFalse(data.getRows().isEmpty());

        PaletteRow row = data.getRows().get(0);
        assertEquals(9, row.getSlots().size());

        row.setSlot(0, "minecraft:stone");
        row.setSlot(8, "minecraft:oak_log");
        assertEquals("minecraft:stone", row.getSlot(0));
        assertEquals("minecraft:oak_log", row.getSlot(8));

        // clear slot
        row.clearSlot(0);
        assertNull(row.getSlot(0));

        // add second row
        PaletteRow newRow = new PaletteRow();
        newRow.setSlot(4, "minecraft:beacon");
        data.addRow(newRow);
        assertTrue(paletteRepository.save(data));

        PaletteData loaded = new PaletteRepository(tempDir).load();
        assertEquals(2, loaded.getRows().size());
        assertEquals("minecraft:beacon", loaded.getRows().get(1).getSlot(4));
    }

    @Test
    void testPaletteDuplicationNamingAndReordering() {
        PaletteData data = new PaletteData();
        PaletteRow r1 = new PaletteRow();
        r1.setName("Madera");
        r1.setSlot(0, "minecraft:oak_planks");

        PaletteRow r2 = new PaletteRow();
        r2.setName("Piedra");
        r2.setSlot(0, "minecraft:stone");

        data.addRow(r1);
        data.addRow(r2);

        // test duplication
        PaletteRow r1Copy = data.duplicateRow(0);
        assertNotNull(r1Copy);
        assertEquals(3, data.getRows().size());
        assertEquals("Madera (Copy)", r1Copy.getName());
        assertEquals("minecraft:oak_planks", r1Copy.getSlot(0));

        // test moveDown
        assertTrue(data.moveDown(0));
        assertEquals("Madera (Copy)", data.getRows().get(0).getName());
        assertEquals("Madera", data.getRows().get(1).getName());

        // test moveUp
        assertTrue(data.moveUp(1));
        assertEquals("Madera", data.getRows().get(0).getName());

        // test persistence of name
        assertTrue(paletteRepository.save(data));
        PaletteData reloaded = paletteRepository.load();
        assertEquals(3, reloaded.getRows().size());
        assertEquals("Madera", reloaded.getRows().get(0).getName());
        assertEquals("Madera (Copy)", reloaded.getRows().get(1).getName());
        assertEquals("Piedra", reloaded.getRows().get(2).getName());
    }

    @Test
    void testVersionCatalogTemplate() {
        VersionCatalog catalog = versionCatalogRepository.load();
        assertNotNull(catalog);
        assertFalse(catalog.getVersions().isEmpty());

        // verify key versions in catalog
        assertTrue(catalog.getVersions().containsKey("1.8"));
        assertTrue(catalog.getVersions().containsKey("Beta 1.9 Prerelease"));
        assertTrue(catalog.getVersions().containsKey("1.14"));
        assertTrue(catalog.getVersions().containsKey("1.21"));
        assertTrue(catalog.getVersions().containsKey("1.21.4"));
        assertTrue(catalog.getVersions().containsKey("26.3"));

        // verify representative blocks
        assertTrue(catalog.getItemsForVersion("1.8").contains("minecraft:stone"));
        assertTrue(catalog.getItemsForVersion("1.8").contains("minecraft:chest"));
        assertTrue(catalog.getItemsForVersion("1.21").contains("minecraft:crafter"));
        assertTrue(catalog.getItemsForVersion("1.21.4").contains("minecraft:pale_oak_log"));
        assertTrue(catalog.getItemsForVersion("26.3").contains("minecraft:poplar_log"));

        // verify catalog volume (> 1000 blocks)
        int totalBlocks = catalog.getVersions().values().stream().mapToInt(List::size).sum();
        assertTrue(totalBlocks > 1000, "catalog should contain over 1000 official blocks by version, has: " + totalBlocks);

        // add new version
        catalog.addVersion("1.22", Arrays.asList("minecraft:custom_block"));
        assertTrue(versionCatalogRepository.save(catalog));

        VersionCatalog loaded = new VersionCatalogRepository(tempDir).load();
        assertTrue(loaded.getVersions().containsKey("1.22"));
        assertTrue(loaded.getItemsForVersion("1.22").contains("minecraft:custom_block"));
    }
}

