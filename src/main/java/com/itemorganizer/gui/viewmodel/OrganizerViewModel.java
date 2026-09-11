package com.itemorganizer.gui.viewmodel;

import com.itemorganizer.core.model.ItemSlotPosition;
import com.itemorganizer.core.model.ModConfig;
import com.itemorganizer.core.model.PaletteData;
import com.itemorganizer.core.model.ProfileData;
import com.itemorganizer.core.model.VersionCatalog;
import com.itemorganizer.gui.navigation.LeftTab;
import com.itemorganizer.gui.navigation.OrdenadoSubTab;
import com.itemorganizer.gui.navigation.RightTab;
import com.itemorganizer.storage.ProfileRepository;
import com.itemorganizer.storage.StorageManager;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// central viewmodel holding state and coordinating tabs
public class OrganizerViewModel {
    public static final String AREA_ORGANIZED = "organized";
    public static final String AREA_PROFILES = "profiles";
    public static final String AREA_CONFIG = "config";
    public static final String AREA_BLOCKER = "blocker";
    public static final String AREA_PALETTES = "palettes";
    public static final String AREA_INF_PALETTE = "inf_palette";
    public static final String AREA_UNORGANIZED = "unorganized";
    public static final String AREA_VERSION = "by version";

    private final StorageManager storageManager;

    private ModConfig config;
    private ProfileData activeProfile;
    private PaletteData paletteData;
    private PaletteData infinitePaletteData;
    private VersionCatalog versionCatalog;

    private LeftTab activeLeftTab = LeftTab.ORDENADO;
    private OrdenadoSubTab activeOrdenadoSubTab = OrdenadoSubTab.ORGANIZADO;
    private RightTab activeRightTab = RightTab.PALETAS;
    private boolean blockerActive = false;

    private final List<String> unorganizedItems = new ArrayList<>();
    private final List<Runnable> changeListeners = new ArrayList<>();

    private final java.util.Map<String, Double> sessionScrollOffsets = new java.util.HashMap<>();
    private String paletteSearchQuery = "";
    private String infinitePaletteSearchQuery = "";
    private final com.itemorganizer.core.model.PaletteRow paletteFilterRow = new com.itemorganizer.core.model.PaletteRow();

    public OrganizerViewModel() {
        this(StorageManager.getInstance(), true);
    }

    public OrganizerViewModel(StorageManager storageManager, boolean loadData) {
        this.storageManager = storageManager;
        if (loadData && storageManager != null) {
            loadInitialData();
        }
    }

    public void loadInitialData() {
        this.config = storageManager.getConfigRepository().getConfig();
        this.blockerActive = (config != null) && config.isBlockerActive();
        String savedProfile = (config != null) ? config.getSelectedProfile() : ProfileRepository.DEFAULT_PROFILE_NAME;
        List<String> available = storageManager.getProfileRepository().listProfiles();
        String profileToLoad = (savedProfile != null && available.contains(savedProfile))
                ? savedProfile
                : ProfileRepository.DEFAULT_PROFILE_NAME;
        this.activeProfile = storageManager.getProfileRepository().loadProfile(profileToLoad);
        if (config != null && !profileToLoad.equals(savedProfile)) {
            config.setSelectedProfile(profileToLoad);
            storageManager.getConfigRepository().save(config);
        }
        this.paletteData = storageManager.getPaletteRepository().getData();
        this.infinitePaletteData = storageManager.getInfinitePaletteRepository().getData();
        this.versionCatalog = storageManager.getVersionCatalogRepository().getCatalog();
        recomputeUnorganizedItems();
    }

    // dynamically recomputes all registry items not in active profile nor blocked
    public void recomputeUnorganizedItems() {
        unorganizedItems.clear();
        Set<String> organizedSet = new HashSet<>();
        Set<String> blockedSet = new HashSet<>();
        if (activeProfile != null) {
            activeProfile.getItems().forEach(pos -> organizedSet.add(pos.getItemId()));
            blockedSet.addAll(activeProfile.getBlockedItems());
        }

        try {
            for (Item item : Registries.ITEM) {
                if (item == Items.AIR) continue;
                Identifier id = Registries.ITEM.getId(item);
                String idStr = id.toString();
                if (item == Items.TEST_BLOCK) {
                    for (net.minecraft.block.enums.TestBlockMode mode : net.minecraft.block.enums.TestBlockMode.values()) {
                        String varId = "minecraft:test_block[mode=" + mode.asString() + "]";
                        if (!organizedSet.contains(varId) && !blockedSet.contains(varId)) {
                            unorganizedItems.add(varId);
                        }
                    }
                    continue;
                }

                if (item == Items.LIGHT) {
                    for (int lvl = 15; lvl >= 0; lvl--) {
                        String varId = "minecraft:light[level=" + lvl + "]";
                        if (!organizedSet.contains(varId) && !blockedSet.contains(varId)) {
                            unorganizedItems.add(varId);
                        }
                    }
                    continue;
                }

                if (!organizedSet.contains(idStr) && !blockedSet.contains(idStr)) {
                    unorganizedItems.add(idStr);
                }
            }
        } catch (Throwable ignored) {
        }
        notifyChanges();
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    public void notifyChanges() {
        for (Runnable listener : changeListeners) {
            listener.run();
        }
    }

    // getters and setters

    public ModConfig getConfig() {
        return config;
    }

    public ProfileData getActiveProfile() {
        return activeProfile;
    }

    public void setActiveProfile(ProfileData activeProfile) {
        this.activeProfile = activeProfile;
        if (activeProfile != null && config != null) {
            config.setSelectedProfile(activeProfile.getName());
            storageManager.getConfigRepository().save(config);
        }
        recomputeUnorganizedItems();
    }

    public PaletteData getPaletteData() {
        return paletteData;
    }

    public VersionCatalog getVersionCatalog() {
        return versionCatalog;
    }

    public LeftTab getActiveLeftTab() {
        return activeLeftTab;
    }

    public void setActiveLeftTab(LeftTab tab) {
        this.activeLeftTab = tab;
        notifyChanges();
    }

    public OrdenadoSubTab getActiveOrdenadoSubTab() {
        return activeOrdenadoSubTab;
    }

    public void setActiveOrdenadoSubTab(OrdenadoSubTab tab) {
        this.activeOrdenadoSubTab = tab;
        notifyChanges();
    }

    public RightTab getActiveRightTab() {
        return activeRightTab;
    }

    public void setActiveRightTab(RightTab tab) {
        this.activeRightTab = tab;
        notifyChanges();
    }

    public boolean isBlockerActive() {
        return blockerActive;
    }

    public void setBlockerActive(boolean blockerActive) {
        this.blockerActive = blockerActive;
        if (config != null && storageManager != null && storageManager.getConfigRepository() != null) {
            config.setBlockerActive(blockerActive);
            storageManager.getConfigRepository().save(config);
        }
        notifyChanges();
    }

    public void toggleBlocker() {
        setBlockerActive(!this.blockerActive);
    }

    public List<String> getUnorganizedItems() {
        return unorganizedItems;
    }

    // profile management methods

    public List<String> getAvailableProfiles() {
        return storageManager.getProfileRepository().listProfiles();
    }

    public boolean loadProfileByName(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        ProfileData loaded = storageManager.getProfileRepository().loadProfile(name);
        if (loaded != null) {
            this.activeProfile = loaded;
            if (config != null) {
                config.setSelectedProfile(loaded.getName());
                storageManager.getConfigRepository().save(config);
            }
            recomputeUnorganizedItems();
            return true;
        }
        return false;
    }

    public boolean createNewProfile(String name, boolean cloneActive) {
        if (name == null || name.trim().isEmpty()) return false;
        String cleanName = name.trim();
        List<String> existing = getAvailableProfiles();
        if (existing.contains(cleanName)) return false;

        ProfileData newProfile = (cloneActive && activeProfile != null)
                ? new ProfileData(cleanName, activeProfile)
                : new ProfileData(cleanName);

        boolean saved = storageManager.getProfileRepository().saveProfile(newProfile);
        if (saved) {
            this.activeProfile = newProfile;
            if (config != null) {
                config.setSelectedProfile(newProfile.getName());
                storageManager.getConfigRepository().save(config);
            }
            recomputeUnorganizedItems();
        }
        return saved;
    }

    public boolean renameProfile(String oldName, String newName) {
        if (oldName == null || newName == null || newName.trim().isEmpty()) return false;
        String cleanNew = newName.trim();
        boolean renamed = storageManager.getProfileRepository().renameProfile(oldName, cleanNew);
        if (renamed) {
            if (activeProfile != null && activeProfile.getName().equals(oldName)) {
                activeProfile.setName(cleanNew);
                if (config != null) {
                    config.setSelectedProfile(cleanNew);
                    storageManager.getConfigRepository().save(config);
                }
            }
            notifyChanges();
        }
        return renamed;
    }

    public boolean deleteProfileByName(String name) {
        if (name == null || name.equalsIgnoreCase(ProfileRepository.DEFAULT_PROFILE_NAME)) {
            return false;
        }

        boolean wasActive = activeProfile != null && activeProfile.getName().equals(name);
        if (wasActive) {
            loadProfileByName(ProfileRepository.DEFAULT_PROFILE_NAME);
        }

        boolean deleted = storageManager.getProfileRepository().deleteProfile(name);
        if (deleted) {
            notifyChanges();
        }
        return deleted;
    }

    // config methods

    public void updateConfig(java.util.function.Consumer<ModConfig> updater) {
        if (updater != null && config != null) {
            updater.accept(config);
            storageManager.getConfigRepository().save(config);
            notifyChanges();
        }
    }

    public double getScrollOffset(String area) {
        return sessionScrollOffsets.getOrDefault(area, 0.0);
    }

    public void setScrollOffset(String area, double offset) {
        sessionScrollOffsets.put(area, Math.max(0.0, offset));
    }

    public String getPaletteSearchQuery() {
        return paletteSearchQuery;
    }

    public void setPaletteSearchQuery(String query) {
        this.paletteSearchQuery = (query != null) ? query : "";
    }

    public com.itemorganizer.core.model.PaletteRow getPaletteFilterRow() {
        return paletteFilterRow;
    }

    public PaletteData getInfinitePaletteData() {
        return infinitePaletteData;
    }

    public String getInfinitePaletteSearchQuery() {
        return infinitePaletteSearchQuery;
    }

    public void setInfinitePaletteSearchQuery(String query) {
        this.infinitePaletteSearchQuery = (query != null) ? query : "";
    }
}

