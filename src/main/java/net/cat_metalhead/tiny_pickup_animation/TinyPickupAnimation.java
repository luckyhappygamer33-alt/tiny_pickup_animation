package net.cat_metalhead.tiny_pickup_animation;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TinyPickupAnimation implements ModInitializer {
	public static final String MOD_ID = "tiny_pickup_animation";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Mod initialized!");
	}
}