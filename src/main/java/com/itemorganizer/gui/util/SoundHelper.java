package com.itemorganizer.gui.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

// centralized sound playback helper for ui interactions
public class SoundHelper {
    public static void playClick() {
        play(SoundEvents.UI_BUTTON_CLICK, 1.0F);
    }

    public static void playLock() {
        play(SoundEvents.BLOCK_CHEST_LOCKED, 1.0F);
    }

    public static void playChime() {
        play(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.0F);
    }

    public static void playPickup() {
        play(SoundEvents.ENTITY_ITEM_PICKUP, 1.0F);
    }

    public static void playTrapdoorOpen() {
        play(SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN, 1.2F);
    }

    public static void playTrapdoorClose() {
        play(SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, 1.2F);
    }

    public static void playBreak() {
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
