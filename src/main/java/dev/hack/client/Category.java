package dev.hack.client;

public enum Category {
    COMBAT("Combat"), MOVEMENT("Movement"), PLAYER("Player"), RENDER("Render"), WORLD("World");

    public final String label;
    Category(String label) { this.label = label; }
}
