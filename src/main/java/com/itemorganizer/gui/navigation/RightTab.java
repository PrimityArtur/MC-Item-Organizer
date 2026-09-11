package com.itemorganizer.gui.navigation;

import net.minecraft.text.Text;

// navigation tabs on the right side
public enum RightTab {
    PALETAS("tab.itemorganizer.palettes", "Palettes"),
    INF_PALETAS("tab.itemorganizer.inf_palette", "Inf. Palette"),
    POR_ORGANIZAR("tab.itemorganizer.unorganized", "Unorganized"),
    POR_VERSION("tab.itemorganizer.version", "By Version");

    private final String translationKey;
    private final String label;

    RightTab(String translationKey, String label) {
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
