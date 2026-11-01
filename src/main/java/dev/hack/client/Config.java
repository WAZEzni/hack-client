package dev.hack.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Saves which modules are on and the last GUI tab to config/hack_client.json. Keybinds live in options.txt. */
public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("hack_client.json"); }

    public static int lastTab = 0;

    public static void load() {
        Path p = path();
        if (!Files.exists(p)) return;
        try {
            JsonObject root = GSON.fromJson(Files.readString(p), JsonObject.class);
            if (root == null) return;
            if (root.has("lastTab")) lastTab = root.get("lastTab").getAsInt();
            JsonObject mods = root.has("modules") ? root.getAsJsonObject("modules") : new JsonObject();
            for (Module m : ModuleManager.all()) {
                if (mods.has(m.name)) m.setEnabled(mods.get(m.name).getAsBoolean(), true);
            }
        } catch (Exception e) {
            HackClient.LOGGER.warn("Could not read config, using defaults", e);
        }
    }

    public static void save() {
        JsonObject root = new JsonObject();
        root.addProperty("lastTab", lastTab);
        JsonObject mods = new JsonObject();
        for (Module m : ModuleManager.all()) mods.addProperty(m.name, m.isEnabled());
        root.add("modules", mods);
        try {
            Files.writeString(path(), GSON.toJson(root));
        } catch (IOException e) {
            HackClient.LOGGER.warn("Could not save config", e);
        }
    }
}
