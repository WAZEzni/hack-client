package dev.hack.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HackClient implements ClientModInitializer {
    public static final String ID = "hack_client";
    public static final Logger LOGGER = LoggerFactory.getLogger("HACK CLIENT");

    @Override
    public void onInitializeClient() {
        ModuleManager.init();
        LOGGER.info("HACK CLIENT loaded (visual-only modules, no gameplay effects)");
    }
}
