package dev.hack.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

/**
 * A purely cosmetic module. It only stores an on/off flag for the GUI.
 * It never reads or modifies the player, world or any entity.
 */
public final class Module {
    public final String name;
    public final String description;
    public final Category category;
    public final KeyMapping key;
    private boolean enabled;

    // GUI animation state (0..1)
    public float toggleAnim, hoverAnim;

    public Module(String name, String description, Category category, KeyMapping key) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.key = key;
    }

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean value, boolean snapAnimation) {
        this.enabled = value;
        if (snapAnimation) toggleAnim = value ? 1f : 0f;
    }

    public void toggle() {
        enabled = !enabled;
        Config.save();
    }

    public static InputConstants.Key keyOf(int glfwKey) {
        return glfwKey < 0 ? InputConstants.UNKNOWN : InputConstants.Type.KEYSYM.getOrCreate(glfwKey);
    }
}
