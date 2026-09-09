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
        // verify slabs and stairs from mods match expected categories
        assertEquals(com.itemorganizer.core.model.ItemCategory.SLABS, com.itemorganizer.core.model.ItemCategory.getCategory("create:cut_granite_slab"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.SLABS, com.itemorganizer.core.model.ItemCategory.getCategory("botania:shimmerrock_slab"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.SLABS, com.itemorganizer.core.model.ItemCategory.getCategory("quark:duskbound_slab"));

        assertEquals(com.itemorganizer.core.model.ItemCategory.STAIRS, com.itemorganizer.core.model.ItemCategory.getCategory("create:cut_granite_stairs"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.STAIRS, com.itemorganizer.core.model.ItemCategory.getCategory("botania:shimmerrock_stairs"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.STAIRS, com.itemorganizer.core.model.ItemCategory.getCategory("quark:duskbound_stairs"));

        assertEquals(com.itemorganizer.core.model.ItemCategory.GLASS, com.itemorganizer.core.model.ItemCategory.getCategory("botania:mana_glass"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.GLASS_PANES, com.itemorganizer.core.model.ItemCategory.getCategory("botania:mana_glass_pane"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.BARS, com.itemorganizer.core.model.ItemCategory.getCategory("create:brass_bars"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.WALLS, com.itemorganizer.core.model.ItemCategory.getCategory("create:industrial_iron_wall"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.FENCES, com.itemorganizer.core.model.ItemCategory.getCategory("botania:dreamwood_fence"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.FENCE_GATES, com.itemorganizer.core.model.ItemCategory.getCategory("botania:dreamwood_fence_gate"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.DOORS, com.itemorganizer.core.model.ItemCategory.getCategory("create:andesite_door"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.TRAPDOORS, com.itemorganizer.core.model.ItemCategory.getCategory("create:andesite_trapdoor"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.TOOLS, com.itemorganizer.core.model.ItemCategory.getCategory("create:wrench"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.FULL_BLOCKS, com.itemorganizer.core.model.ItemCategory.getCategory("create:gearbox"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.FULL_BLOCKS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:brain_coral_block"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.CORALS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:brain_coral"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.SHERDS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:angler_pottery_sherd"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.CHESTS, com.itemorganizer.core.model.ItemCategory.getCategory("ironchest:copper_chest"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.MINECARTS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:chest_minecart"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.SPAWN_EGGS, com.itemorganizer.core.model.ItemCategory.getCategory("minecraft:iron_golem_spawn_egg"));
        assertEquals(com.itemorganizer.core.model.ItemCategory.MISC, com.itemorganizer.core.model.ItemCategory.getCategory("create:brass_ingot"));
    }
}
