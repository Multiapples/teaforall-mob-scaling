package io.github.multiapples;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;

public class TeaForAllMobScaling implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger logger = LoggerFactory.getLogger("teaforallmobscaling");
	public static Config config = null;

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		logger.info("Initializing TeaForAll Mob Scaling");

		URI configFilePath = FabricLoader.getInstance().getConfigDir().resolve("teaforall-mob-scaling.json").toUri();
		try {
			config = Config.load(new File(configFilePath));
		} catch (FileNotFoundException e) {
			logger.info("Config file missing. Creating default config file at " + configFilePath.toASCIIString() + ".");
			config = new Config();
			try {
				Config.save(new File(configFilePath), config);
			} catch (IOException e2) {
				logger.warn("Could not write file to " + configFilePath.toASCIIString() + ".");
			}
		}
		MobScaling.initialize(config);
	}
}