package com.itemorganizer.core.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// catalog of items indexed by minecraft version
public class VersionCatalog {
    private Map<String, List<String>> versions = new LinkedHashMap<>();

    public VersionCatalog() {
    }

    public VersionCatalog(Map<String, List<String>> versions) {
        this.versions = versions != null ? new LinkedHashMap<>(versions) : new LinkedHashMap<>();
    }

    public Map<String, List<String>> getVersions() {
        return versions;
    }

    public void setVersions(Map<String, List<String>> versions) {
        this.versions = versions != null ? new LinkedHashMap<>(versions) : new LinkedHashMap<>();
    }

    public List<String> getItemsForVersion(String version) {
        return versions.getOrDefault(version, new ArrayList<>());
    }

    public void addVersion(String version, List<String> items) {
        versions.put(version, items != null ? new ArrayList<>(items) : new ArrayList<>());
    }
}
