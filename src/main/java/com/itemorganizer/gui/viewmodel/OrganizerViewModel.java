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
    private final StorageManager storageManager;

    private ModConfig config;
    private ProfileData activeProfile;
    private PaletteData paletteData;
    private VersionCatalog versionCatalog;

    private LeftTab activeLeftTab = LeftTab.ORDENADO;
    private OrdenadoSubTab activeOrdenadoSubTab = OrdenadoSubTab.ORGANIZADO;
    private RightTab activeRightTab = RightTab.PALETAS;
    private boolean blockerActive = false;

    private final List<String> unorganizedItems = new ArrayList<>();
    private final List<Runnable> changeListeners = new ArrayList<>();

    public OrganizerViewModel() {
        this.storageManager = StorageManager.getInstance();
        loadInitialData();
    }

    public void loadInitialData() {
        this.config = storageManager.getConfigRepository().getConfig();
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

        for (Item item : Registries.ITEM) {
            if (item == Items.AIR) continue;
            Identifier id = Registries.ITEM.getId(item);
            String idStr = id.toString();
            if (!organizedSet.contains(idStr) && !blockedSet.contains(idStr)) {
                unorganizedItems.add(idStr);
            }
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
        notifyChanges();
    }

    public void toggleBlocker() {
        this.blockerActive = !this.blockerActive;
        notifyChanges();
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
}

