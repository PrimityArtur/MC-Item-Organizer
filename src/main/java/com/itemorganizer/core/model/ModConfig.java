package com.itemorganizer.core.model;

// visual and control configuration for the mod
public class ModConfig {
    private int backgroundColor = 0x101010; // default background rgb
    private float transparency = 0.60f;     // alpha 0.0 to 1.0
    private float scale = 1.0f;             // grid zoom scale (0.10 to 5.00)
    private float itemScale = 1.0f;         // item render scale (0.5 to 1.5)
    private float textScale = 1.0f;         // text render scale (0.5 to 1.5)
    private float splitRatio = 0.50f;       // left vs right split ratio (0.20 to 0.80)
    private float hotbarScale = 1.0f;       // hotbar size scale (0.5 to 2.0)
    private float paletteScale = 1.0f;      // palette size scale (0.5 to 2.0)
    private float paletteItemScale = 1.0f;  // palette item render scale (0.5 to 1.5)
    private String keyOpenClose = "key.keyboard.o"; // toggle key
    private String keyQuickAppend = "key.keyboard.a"; // quick append key
    private String selectedProfile = "default"; // active profile name
    private float blur = 0.50f; // background blur intensity (0.0 to 5.0)

    public ModConfig() {
    }

    public ModConfig(int backgroundColor, float transparency, float scale, float itemScale, float textScale, String keyOpenClose) {
        this.backgroundColor = backgroundColor;
        this.transparency = transparency;
        this.scale = scale;
        this.itemScale = itemScale;
        this.textScale = textScale;
        this.keyOpenClose = keyOpenClose;
    }

    public ModConfig(int backgroundColor, float transparency, float scale, float itemScale, float textScale, float splitRatio, float hotbarScale, String keyOpenClose) {
        this.backgroundColor = backgroundColor;
        this.transparency = transparency;
        this.scale = scale;
        this.itemScale = itemScale;
        this.textScale = textScale;
        this.splitRatio = splitRatio;
        this.hotbarScale = hotbarScale;
        this.keyOpenClose = keyOpenClose;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public float getTransparency() {
        return transparency;
    }

    public void setTransparency(float transparency) {
        this.transparency = Math.max(0.0f, Math.min(1.0f, transparency));
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = Math.max(0.10f, Math.min(5.00f, scale));
    }

    public float getSplitRatio() {
        return splitRatio;
    }

    public void setSplitRatio(float splitRatio) {
        this.splitRatio = Math.max(0.20f, Math.min(0.80f, splitRatio));
    }

    public float getHotbarScale() {
        return hotbarScale;
    }

    public void setHotbarScale(float hotbarScale) {
        this.hotbarScale = Math.max(0.50f, Math.min(2.00f, hotbarScale));
    }

    public float getPaletteScale() {
        if (paletteScale < 0.50f) paletteScale = 1.0f;
        return paletteScale;
    }

    public void setPaletteScale(float paletteScale) {
        this.paletteScale = Math.max(0.50f, Math.min(2.00f, paletteScale));
    }

    public float getPaletteItemScale() {
        if (paletteItemScale < 0.10f) paletteItemScale = 1.0f;
        return paletteItemScale;
    }

    public void setPaletteItemScale(float paletteItemScale) {
        this.paletteItemScale = Math.max(0.50f, Math.min(1.50f, paletteItemScale));
    }

    public float getItemScale() {
        return itemScale;
    }

    public void setItemScale(float itemScale) {
        this.itemScale = Math.max(0.5f, Math.min(1.5f, itemScale));
    }

    public float getTextScale() {
        return textScale;
    }

    public void setTextScale(float textScale) {
        this.textScale = Math.max(0.5f, Math.min(1.5f, textScale));
    }

    public String getKeyOpenClose() {
        return keyOpenClose;
    }

    public void setKeyOpenClose(String keyOpenClose) {
        this.keyOpenClose = keyOpenClose;
    }

    public String getKeyQuickAppend() {
        return (keyQuickAppend != null && !keyQuickAppend.isEmpty()) ? keyQuickAppend : "key.keyboard.a";
    }

    public void setKeyQuickAppend(String keyQuickAppend) {
        this.keyQuickAppend = keyQuickAppend;
    }

    public String getSelectedProfile() {
        return (selectedProfile != null && !selectedProfile.trim().isEmpty()) ? selectedProfile : "default";
    }

    public void setSelectedProfile(String selectedProfile) {
        this.selectedProfile = (selectedProfile != null && !selectedProfile.trim().isEmpty()) ? selectedProfile : "default";
    }

    public float getBlur() {
        return blur;
    }

    public void setBlur(float blur) {
        this.blur = Math.max(0.0f, Math.min(5.0f, blur));
    }

    public boolean isBackgroundBlur() {
        return blur > 0.01f;
    }

    public void setBackgroundBlur(boolean backgroundBlur) {
        this.blur = backgroundBlur ? 0.50f : 0.0f;
    }

    // returns background color in argb format for drawcontext.fill
    public int getArgbColor() {
        int alpha = (int) (this.transparency * 255.0f) & 0xFF;
        return (alpha << 24) | (this.backgroundColor & 0x00FFFFFF);
    }
}
