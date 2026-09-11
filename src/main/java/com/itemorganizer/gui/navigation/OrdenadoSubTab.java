package com.itemorganizer.gui.navigation;

import net.minecraft.text.Text;

// subtabs inside organized view
public enum OrdenadoSubTab {
    ORGANIZADO("subtab.itemorganizer.organized", "Organized"),
    BLOQUEADO("subtab.itemorganizer.blocked", "Blocked"),
    CREAR_PALETA("subtab.itemorganizer.create_palette", "Crear Paleta");

    private final String translationKey;
    private final String label;

    OrdenadoSubTab(String translationKey, String label) {
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
