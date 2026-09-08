package com.itemorganizer.client;

import com.itemorganizer.ItemOrganizer;
import com.itemorganizer.gui.OrganizerScreen;
import com.itemorganizer.gui.viewmodel.OrganizerViewModel;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

// client entrypoint registering keybindings and screen triggers
public class ItemOrganizerClient implements ClientModInitializer {
    public static final String KEY_CATEGORY = "category.itemorganizer";
    private static KeyBinding openKeyBinding;
    private static OrganizerViewModel sharedViewModel;

    @Override
    public void onInitializeClient() {
        ItemOrganizer.LOGGER.info("Initializing ItemOrganizer Client...");

        // register open keybinding ('O' by default)
        KeyBinding.Category category = KeyBinding.Category.create(net.minecraft.util.Identifier.of(ItemOrganizer.MOD_ID, "keys"));
        openKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.itemorganizer.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                category
        ));

        // key press listener to open screen
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKeyBinding.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    if (sharedViewModel == null) {
                        sharedViewModel = new OrganizerViewModel();
                    } else {
                        // reload profile and settings data
                        sharedViewModel.loadInitialData();
                    }
                    client.setScreen(new OrganizerScreen(sharedViewModel));
                }
            }
        });
    }

    public static KeyBinding getOpenKeyBinding() {
        return openKeyBinding;
    }

    public static OrganizerViewModel getSharedViewModel() {
        return sharedViewModel;
    }
}
