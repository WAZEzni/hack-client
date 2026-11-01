package dev.hack.client.gui;

import dev.hack.client.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class ClickGuiScreen extends Screen {
    // Palette (RGB)
    private static final int PURPLE = 0xA855F7, PURPLE_DK = 0x7C3AED;
    private static final int BG = 0x0C0C10, SIDEBAR = 0x121217, TITLE_BG = 0x15151B;
    private static final int ROW = 0x1B1B23, ROW_HOV = 0x24242E, OFF = 0x2E2E3A;
    private static final int TEXT = 0xF2F2F7, MUTED = 0x8A8A9A;

    private static final int W = 520, H = 320, TITLE = 38, SIDE = 124, ROW_H = 38, GAP = 8;
    private static final String[] TABS = {"Combat", "Movement", "Player", "Render", "World", "Settings"};

    private int x0, y0;
    private int tab = Math.max(0, Math.min(TABS.length - 1, Config.lastTab));
    private final float[] tabHover = new float[TABS.length];
    private float indicatorY = -1, guiChipHover, open, guiHover;
    private long last = System.nanoTime();
    private float alpha = 1f, dt;

    private Module listening;      // module whose key is being rebound
    private boolean listeningGui;  // ClickGUI key being rebound

    public ClickGuiScreen() { super(Component.literal("HACK CLIENT")); }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        Config.lastTab = tab;
        Config.save();
        super.onClose();
    }

    // ---------- helpers ----------
    private static float approach(float cur, float target, float dt, float speed) {
        return cur + (target - cur) * Math.min(1f, dt * speed);
    }
    private static int mix(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }
    private int col(int rgb, float a) {
        int al = Math.max(0, Math.min(255, Math.round(a * alpha * 255)));
        return (al << 24) | (rgb & 0xFFFFFF);
    }
    /** Rounded-ish rectangle (corner notch). */
    private void rect(GuiGraphicsExtractor g, int x, int y, int w, int h, int rgb, float a) {
        int c = col(rgb, a);
        if (w < 4 || h < 4) { g.fill(x, y, x + w, y + h, c); return; }
        g.fill(x + 1, y, x + w - 1, y + h, c);
        g.fill(x, y + 1, x + w, y + h - 1, c);
    }
    private void text(GuiGraphicsExtractor g, String s, int x, int y, int rgb, float a) {
        int c = col(rgb, a);
        if ((c >>> 24) < 6) return;
        g.text(this.font, s, x, y, c, false);
    }
    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ---------- layout ----------
    private int rowX() { return x0 + SIDE + 16; }
    private int rowW() { return W - SIDE - 32; }
    private int rowY(int i) { return y0 + TITLE + 16 + i * (ROW_H + GAP); }
    private int chipW(String label) { return this.font.width(label) + 14; }
    private int switchX() { return rowX() + rowW() - 14 - 34; }

    // ---------- rendering ----------
    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
        long now = System.nanoTime();
        dt = Math.min(0.1f, (now - last) / 1_000_000_000f);
        last = now;
        open = approach(open, 1f, dt, 9f);
        float e = 1f - (float) Math.pow(1f - Math.min(1f, open), 3);
        alpha = e;

        x0 = (this.width - W) / 2;
        y0 = (this.height - H) / 2;

        // dim backdrop
        g.fill(0, 0, this.width, this.height, col(0x000000, 0.55f));

        float scale = 0.94f + 0.06f * e;
        g.pose().pushMatrix();
        g.pose().translate(this.width / 2f, this.height / 2f + (1f - e) * 10f);
        g.pose().scale(scale, scale);
        g.pose().translate(-this.width / 2f, -this.height / 2f);

        // soft glow + window
        rect(g, x0 - 2, y0 - 2, W + 4, H + 4, PURPLE_DK, 0.35f);
        rect(g, x0, y0, W, H, BG, 1f);
        rect(g, x0, y0, W, TITLE, TITLE_BG, 1f);
        g.fill(x0, y0 + TITLE, x0 + W, y0 + TITLE + 1, col(PURPLE, 0.55f));
        rect(g, x0, y0 + TITLE + 1, SIDE, H - TITLE - 1, SIDEBAR, 1f);

        drawTitle(g);
        drawTabs(g, mx, my);
        if (tab < Category.values().length) drawModules(g, mx, my);
        else drawSettings(g, mx, my);

        g.pose().popMatrix();
    }

    private void drawTitle(GuiGraphicsExtractor g) {
        g.pose().pushMatrix();
        g.pose().translate(x0 + 16, y0 + 11);
        g.pose().scale(1.5f, 1.5f);
        text(g, "HACK", 0, 0, PURPLE, 1f);
        g.pose().popMatrix();
        text(g, "CLIENT", x0 + 16 + (int) (this.font.width("HACK") * 1.5f) + 5, y0 + 15, TEXT, 0.9f);
        String note = "Visual only  -  no gameplay effects";
        text(g, note, x0 + W - this.font.width(note) - 14, y0 + 15, MUTED, 1f);
    }

    private void drawTabs(GuiGraphicsExtractor g, int mx, int my) {
        int ty = y0 + TITLE + 14;
        float targetY = ty + tab * 30;
        if (indicatorY < 0) indicatorY = targetY;
        indicatorY = approach(indicatorY, targetY, dt, 16f);

        // moving highlight
        rect(g, x0 + 8, Math.round(indicatorY), SIDE - 16, 26, PURPLE, 0.16f);
        rect(g, x0 + 8, Math.round(indicatorY) + 5, 3, 16, PURPLE, 1f);

        for (int i = 0; i < TABS.length; i++) {
            int y = ty + i * 30;
            boolean hov = in(mx, my, x0 + 8, y, SIDE - 16, 26);
            tabHover[i] = approach(tabHover[i], hov ? 1f : 0f, dt, 14f);
            if (i != tab) rect(g, x0 + 8, y, SIDE - 16, 26, ROW_HOV, tabHover[i] * 0.6f);
            int c = mix(MUTED, TEXT, i == tab ? 1f : tabHover[i]);
            text(g, TABS[i], x0 + 20 + (int) (tabHover[i] * 2), y + 9, c, 1f);
        }
    }

    private void drawChip(GuiGraphicsExtractor g, int x, int y, String label, boolean active, float hov) {
        int w = chipW(label);
        rect(g, x, y, w, 16, mix(OFF, PURPLE_DK, active ? 1f : hov * 0.5f), 1f);
        text(g, label, x + 7, y + 4, active ? TEXT : mix(MUTED, TEXT, hov), 1f);
    }

    private void drawModules(GuiGraphicsExtractor g, int mx, int my) {
        List<Module> mods = ModuleManager.in(Category.values()[tab]);
        text(g, TABS[tab].toUpperCase(), rowX(), y0 + TITLE + 5, MUTED, 0.9f);
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            int rx = rowX(), ry = rowY(i) + 6, rw = rowW();
            boolean hov = in(mx, my, rx, ry, rw, ROW_H);
            m.hoverAnim = approach(m.hoverAnim, hov ? 1f : 0f, dt, 14f);
            m.toggleAnim = approach(m.toggleAnim, m.isEnabled() ? 1f : 0f, dt, 12f);

            rect(g, rx, ry, rw, ROW_H, mix(ROW, ROW_HOV, m.hoverAnim), 1f);
            int bar = Math.round(ROW_H * m.toggleAnim);
            if (bar > 0) rect(g, rx, ry + (ROW_H - bar) / 2, 3, bar, PURPLE, 1f);

            text(g, m.name, rx + 14, ry + 8, mix(0xC8C8D2, TEXT, m.toggleAnim), 1f);
            text(g, m.description, rx + 14, ry + 21, MUTED, 0.8f);

            // toggle switch
            int sx = switchX(), sy = ry + (ROW_H - 16) / 2;
            rect(g, sx, sy, 34, 16, mix(OFF, PURPLE, m.toggleAnim), 1f);
            int kx = sx + 2 + Math.round(m.toggleAnim * 18);
            rect(g, kx, sy + 2, 12, 12, mix(0xB4B4C0, 0xFFFFFF, m.toggleAnim), 1f);

            // keybind chip
            boolean lis = listening == m;
            String label = lis ? "..." : ModuleManager.keyLabel(m.key);
            int cw = chipW(label), cx = sx - 12 - cw, cy = ry + (ROW_H - 16) / 2;
            drawChip(g, cx, cy, label, lis, in(mx, my, cx, cy, cw, 16) ? 1f : 0f);
        }
        if (mods.isEmpty()) text(g, "No modules", rowX(), rowY(0), MUTED, 1f);

        text(g, "Click chip to bind  -  Backspace unbinds  -  Esc cancels",
                rowX(), y0 + H - 16, MUTED, 0.7f);
    }

    private void drawSettings(GuiGraphicsExtractor g, int mx, int my) {
        text(g, "SETTINGS", rowX(), y0 + TITLE + 5, MUTED, 0.9f);
        int rx = rowX(), ry = rowY(0) + 6, rw = rowW();
        boolean hov = in(mx, my, rx, ry, rw, ROW_H);
        guiHover = approach(guiHover, hov ? 1f : 0f, dt, 14f);
        rect(g, rx, ry, rw, ROW_H, mix(ROW, ROW_HOV, guiHover), 1f);
        text(g, "ClickGUI Key", rx + 14, ry + 8, TEXT, 1f);
        text(g, "Key that opens and closes this menu.", rx + 14, ry + 21, MUTED, 0.8f);

        String label = listeningGui ? "..." : ModuleManager.keyLabel(ModuleManager.guiKey);
        int cw = chipW(label), cx = rx + rw - 14 - cw, cy = ry + (ROW_H - 16) / 2;
        guiChipHover = approach(guiChipHover, in(mx, my, cx, cy, cw, 16) ? 1f : 0f, dt, 14f);
        drawChip(g, cx, cy, label, listeningGui, guiChipHover);
    }

    // ---------- input ----------
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int btn = event.button();
        if (btn != 0) return super.mouseClicked(event, doubleClick);

        // tabs
        int ty = y0 + TITLE + 14;
        for (int i = 0; i < TABS.length; i++) {
            if (in(mx, my, x0 + 8, ty + i * 30, SIDE - 16, 26)) {
                tab = i; listening = null; listeningGui = false;
                return true;
            }
        }

        if (tab < Category.values().length) {
            List<Module> mods = ModuleManager.in(Category.values()[tab]);
            for (int i = 0; i < mods.size(); i++) {
                Module m = mods.get(i);
                int rx = rowX(), ry = rowY(i) + 6, rw = rowW();
                if (!in(mx, my, rx, ry, rw, ROW_H)) continue;
                int sx = switchX();
                String label = listening == m ? "..." : ModuleManager.keyLabel(m.key);
                int cw = chipW(label), cx = sx - 12 - cw, cy = ry + (ROW_H - 16) / 2;
                if (in(mx, my, cx, cy, cw, 16)) {
                    listening = (listening == m) ? null : m; listeningGui = false;
                } else {
                    listening = null; listeningGui = false;
                    m.toggle();
                }
                return true;
            }
        } else {
            int rx = rowX(), ry = rowY(0) + 6, rw = rowW();
            String label = listeningGui ? "..." : ModuleManager.keyLabel(ModuleManager.guiKey);
            int cw = chipW(label), cx = rx + rw - 14 - cw, cy = ry + (ROW_H - 16) / 2;
            if (in(mx, my, cx, cy, cw, 16)) {
                listeningGui = !listeningGui; listening = null;
                return true;
            }
        }
        listening = null; listeningGui = false;
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (listening != null || listeningGui) {
            KeyMapping target = listeningGui ? ModuleManager.guiKey : listening.key;
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                // cancel
            } else if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) {
                if (!listeningGui) ModuleManager.bind(target, -1); // GUI key must stay bound
            } else {
                ModuleManager.bind(target, key);
            }
            listening = null; listeningGui = false;
            return true;
        }
        if (ModuleManager.guiKey.matches(event)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }
}
