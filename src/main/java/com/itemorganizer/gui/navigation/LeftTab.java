package com.itemorganizer.gui.navigation;

import net.minecraft.text.Text;

// navigation tabs on the left side
public enum LeftTab {
    ORDENADO("tab.itemorganizer.ordered", "Organized"),
    PERFILES("tab.itemorganizer.profiles", "Profiles"),
    CONFIG("tab.itemorganizer.config", "Config");

    private final String translationKey;
    private final String label;

    LeftTab(String translationKey, String label) {
        this.translationKey = translationKey;
        this.label = label;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public Text getText() {
        return Text.translatable(translationKey);
    }

    public String getLabel() {
        return label;
    }
}
