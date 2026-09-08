package com.itemorganizer.gui.util;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class TextureColorAnalyzerTest {

    @Test
    public void testExtractProminentColor_FlowerPetals() throws IOException {
        // 16x16 sprite: red petals on top, green stem on bottom
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                img.setRGB(x, y, 0x00000000); // transparent
            }
        }
        // red petals on top
        for (int y = 2; y <= 7; y++) {
            for (int x = 4; x <= 11; x++) {
                img.setRGB(x, y, 0xFFFF1E1E);
            }
        }
        // green stem on bottom
        for (int y = 8; y <= 15; y++) {
            for (int x = 7; x <= 8; x++) {
                img.setRGB(x, y, 0xFF1E8A1E);
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        int color = TextureColorAnalyzer.extractProminentColor(bais, "poppy");
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        assertTrue(r > 180, "red channel should dominate flower petals: r=" + r);
        assertTrue(g < 80, "green stem channel should be minimized: g=" + g);
    }

    @Test
    public void testExtractProminentColor_CakeFrosting() throws IOException {
        // 16x16 sprite: white frosting on top, brown cake base on bottom
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                img.setRGB(x, y, 0x00000000);
            }
        }
        // white frosting on top
        for (int y = 2; y <= 6; y++) {
            for (int x = 2; x <= 13; x++) {
                img.setRGB(x, y, 0xFFFFFFFF);
            }
        }
        // brown cake base on bottom
        for (int y = 7; y <= 13; y++) {
            for (int x = 2; x <= 13; x++) {
                img.setRGB(x, y, 0xFF7A4B26);
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        int color = TextureColorAnalyzer.extractProminentColor(bais, "cake");
        float[] hsv = ItemColorHelper.rgbToHsv(color);

        assertTrue(hsv[2] > 0.70f, "brightness should be high for white cake frosting: v=" + hsv[2]);
        assertTrue(hsv[1] < 0.35f, "saturation should be low for cake white frosting: s=" + hsv[1]);
    }

    @Test
    public void testExtractProminentColor_TorchFlame() throws IOException {
        // 16x16 sprite: orange flame on top, wood stick on bottom
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                img.setRGB(x, y, 0x00000000);
            }
        }
        // orange flame on top
        for (int y = 2; y <= 5; y++) {
            for (int x = 6; x <= 9; x++) {
                img.setRGB(x, y, 0xFFFFA010);
            }
        }
        // brown wood stick on bottom
        for (int y = 6; y <= 14; y++) {
            for (int x = 7; x <= 8; x++) {
                img.setRGB(x, y, 0xFF4A321A);
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        int color = TextureColorAnalyzer.extractProminentColor(bais, "torch");
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        assertTrue(r > 160, "red channel should be high for torch flame: r=" + r);
        assertTrue(g > 80, "green channel should be present for orange/yellow flame: g=" + g);
        assertTrue(b < 50, "blue channel should be low for torch flame: b=" + b);
    }

    @Test
    public void testModularExtractProminentColor_WithoutName() throws IOException {
        // generic extraction test without item name
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                img.setRGB(x, y, 0x00000000);
            }
        }
        // vibrant blue subject
        for (int y = 4; y <= 11; y++) {
            for (int x = 4; x <= 11; x++) {
                img.setRGB(x, y, 0xFF2A80E0);
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        int color = TextureColorAnalyzer.extractProminentColor(bais);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        assertTrue(b > 180, "blue channel should dominate: b=" + b);
        assertTrue(r < 60, "red channel should be low: r=" + r);
    }

    @Test
    public void testModdedCategoryOrder_SlabsAndStairs() {
        // verify slabs and stairs from mods match expected category indices
        assertEquals(8, ItemColorHelper.getItemCategoryOrder("create:cut_granite_slab"));
        assertEquals(8, ItemColorHelper.getItemCategoryOrder("botania:shimmerrock_slab"));
        assertEquals(8, ItemColorHelper.getItemCategoryOrder("quark:duskbound_slab"));

        assertEquals(10, ItemColorHelper.getItemCategoryOrder("create:cut_granite_stairs"));
        assertEquals(10, ItemColorHelper.getItemCategoryOrder("botania:shimmerrock_stairs"));
        assertEquals(10, ItemColorHelper.getItemCategoryOrder("quark:duskbound_stairs"));

        assertEquals(2, ItemColorHelper.getItemCategoryOrder("botania:mana_glass"));
        assertEquals(3, ItemColorHelper.getItemCategoryOrder("botania:mana_glass_pane"));
        assertEquals(4, ItemColorHelper.getItemCategoryOrder("create:brass_bars"));
        assertEquals(7, ItemColorHelper.getItemCategoryOrder("create:industrial_iron_wall"));
        assertEquals(6, ItemColorHelper.getItemCategoryOrder("botania:dreamwood_fence"));
        assertEquals(9, ItemColorHelper.getItemCategoryOrder("botania:dreamwood_fence_gate"));
        assertEquals(17, ItemColorHelper.getItemCategoryOrder("create:andesite_door"));
        assertEquals(18, ItemColorHelper.getItemCategoryOrder("create:andesite_trapdoor"));
        assertEquals(29, ItemColorHelper.getItemCategoryOrder("create:wrench"));
        assertEquals(1, ItemColorHelper.getItemCategoryOrder("create:gearbox"));
        assertEquals(1, ItemColorHelper.getItemCategoryOrder("minecraft:brain_coral_block"));
        assertEquals(27, ItemColorHelper.getItemCategoryOrder("minecraft:brain_coral"));
        assertEquals(44, ItemColorHelper.getItemCategoryOrder("minecraft:angler_pottery_sherd"));
        assertEquals(45, ItemColorHelper.getItemCategoryOrder("ironchest:copper_chest"));
        assertEquals(46, ItemColorHelper.getItemCategoryOrder("minecraft:chest_minecart"));
        assertEquals(47, ItemColorHelper.getItemCategoryOrder("minecraft:iron_golem_spawn_egg"));
        assertEquals(48, ItemColorHelper.getItemCategoryOrder("create:brass_ingot"));
    }
}
