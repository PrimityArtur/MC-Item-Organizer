package com.itemorganizer.gui.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

// centralized sound playback helper for ui interactions
public class SoundHelper {
    private static boolean isAvailable() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client != null && client.getSoundManager() != null;
    }

    public static void playClick() {
        if (!isAvailable()) return;
        play(SoundEvents.UI_BUTTON_CLICK, 1.0F);
    }

    public static void playLock() {
        if (!isAvailable()) return;
        play(SoundEvents.BLOCK_CHEST_LOCKED, 1.0F);
    }

    public static void playChime() {
        if (!isAvailable()) return;
        play(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.0F);
    }

    public static void playPickup() {
        if (!isAvailable()) return;
        play(SoundEvents.ENTITY_ITEM_PICKUP, 1.0F);
    }

    public static void playTrapdoorOpen() {
        if (!isAvailable()) return;
        play(SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN, 1.2F);
    }

    public static void playTrapdoorClose() {
        if (!isAvailable()) return;
        play(SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, 1.2F);
    }

    public static void playBreak() {
        if (!isAvailable()) return;
        play(SoundEvents.ENTITY_ITEM_BREAK, 1.0F);
    }

    public static void play(RegistryEntry<SoundEvent> sound, float pitch) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getSoundManager() != null && sound != null) {
            client.getSoundManager().play(PositionedSoundInstance.ui(sound, pitch));
        }
    }

    public static void play(SoundEvent sound, float pitch) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getSoundManager() != null && sound != null) {
            client.getSoundManager().play(PositionedSoundInstance.ui(sound, pitch));
        }
    }
}
