package com.itemorganizer;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemOrganizer implements ModInitializer {
	public static final String MOD_ID = "item-organizer";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// initialize storage on mod startup
		LOGGER.info("Initializing ItemOrganizer...");
		com.itemorganizer.storage.StorageManager.getInstance().init();
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
