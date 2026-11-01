package dev.hack.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hack.client.gui.ClickGuiScreen;

import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();
    private static KeyMapping.Category keyCategory;
    public static KeyMapping guiKey;

    public static List<Module> all() { return MODULES; }

    public static List<Module> in(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : MODULES) if (m.category == c) out.add(m);
        return out;
    }

    public static void init() {
        keyCategory = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("hack_client", "main"));

        guiKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.hack_client.clickgui", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, keyCategory));

        add("KillAura",   "Visual toggle only. Does nothing.", Category.COMBAT);
        add("Fly",        "Visual toggle only. Does nothing.", Category.MOVEMENT);
        add("Speed",      "Visual toggle only. Does nothing.", Category.MOVEMENT);
        add("AutoSprint", "Visual toggle only. Does nothing.", Category.MOVEMENT);
        add("NoFall",     "Visual toggle only. Does nothing.", Category.PLAYER);
        add("ESP",        "Visual toggle only. Does nothing.", Category.RENDER);
        add("Scaffold",   "Visual toggle only. Does nothing.", Category.WORLD);

        Config.load();

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (guiKey.consumeClick()) {
                if (mc.screen == null) mc.setScreenAndShow(new ClickGuiScreen());
            }
            for (Module m : MODULES) {
                while (m.key.consumeClick()) {
                    if (mc.screen == null) m.toggle();
                }
            }
        });
    }

    private static void add(String name, String desc, Category cat) {
        KeyMapping km = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.hack_client." + name.toLowerCase(), InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN, keyCategory));
        MODULES.add(new Module(name, desc, cat, km));
    }

    /** Rebinds a key (-1 = unbind) and persists it via the vanilla options file. */
    public static void bind(KeyMapping mapping, int glfwKey) {
        mapping.setKey(Module.keyOf(glfwKey));
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
    }

    public static String keyLabel(KeyMapping m) {
        return m.isUnbound() ? "NONE" : m.getTranslatedKeyMessage().getString().toUpperCase();
    }
}
