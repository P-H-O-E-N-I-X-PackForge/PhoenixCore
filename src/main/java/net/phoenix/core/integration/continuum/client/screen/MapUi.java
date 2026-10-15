package net.phoenix.core.integration.continuum.client.screen;

import net.minecraft.client.gui.GuiGraphics;

import java.util.Locale;

final class MapUi {

    private MapUi() {}

    static int FRAME = 0xFF7a5cff;
    static int PANEL_BG = 0xD00b0a18;
    static int PANEL_LINE = 0xFF3a2f7a;
    static int TITLE = 0xFFE8D8FF;
    static int TEXT = 0xFFB8B0D8;
    static int DIM = 0xFF7a7498;
    static int GOOD = 0xFF7affb0;
    static int WARN = 0xFFffd27a;
    static final int BAD = 0xFFff6b6b;

    static int BUTTON = 0xFF1d1a40;
    static int BUTTON_HOVER = 0xFF2c2760;
    static int BUTTON_ON = 0xFF3a3380;
    static int BUTTON_OFF = 0xFF14122a;
    static int BUTTON_LINE = 0xFF2a2548;
    static int PANEL_SOLID = 0xFF0b0a18;
    static int SCREEN_BG = 0xFF07061a;
    static int CHROME = 0xAA1a1830;

    static void refreshTheme() {
        try {
            net.phoenixvine.wiki.theme.PhoenixTheme theme = net.phoenixvine.wiki.theme.PhoenixTheme.current();

            int bg = darkened(opaque(theme.bg.getColor()), 0.10f);
            int panel = darkened(opaque(theme.panel.getColor()), 0.16f);
            int accent = lightened(opaque(theme.accent.getColor()), 0.35f);
            FRAME = accent;
            PANEL_BG = (panel & 0x00FFFFFF) | 0xD0000000;
            PANEL_LINE = lightened(opaque(theme.border.getColor()), 0.25f);
            TITLE = lightened(opaque(theme.header.getColor()), 0.85f);
            TEXT = lightened(opaque(theme.text.getColor()), 0.72f);
            DIM = lightened(opaque(theme.textDim.getColor()), 0.55f);
            GOOD = lightened(opaque(theme.done.getColor()), 0.65f);
            WARN = lightened(opaque(theme.activeColor.getColor()), 0.65f);

            PANEL_SOLID = panel;
            SCREEN_BG = bg;
            BUTTON = mix(panel, accent, 0.18f);
            BUTTON_HOVER = mix(panel, accent, 0.38f);
            BUTTON_ON = mix(panel, accent, 0.55f);
            BUTTON_OFF = mix(bg, panel, 0.5f);
            BUTTON_LINE = mix(PANEL_LINE, bg, 0.45f);
            CHROME = (BUTTON_OFF & 0x00FFFFFF) | 0xAA000000;
        } catch (RuntimeException | LinkageError ignored) {

        }
    }

    private static float luminance(int rgb) {
        return (0.2126f * ((rgb >> 16) & 0xFF) + 0.7152f * ((rgb >> 8) & 0xFF) + 0.0722f * (rgb & 0xFF)) / 255.0f;
    }

    private static int lightened(int rgb, float minLuminance) {
        float lum = luminance(rgb);
        if (lum >= minLuminance) return rgb;

        float t = Math.min(1.0f, (minLuminance - lum) / Math.max(0.001f, 1.0f - lum));
        return mix(rgb, 0xFFFFFF, t);
    }

    private static int darkened(int rgb, float maxLuminance) {
        float lum = luminance(rgb);
        if (lum <= maxLuminance) return rgb;
        return mix(rgb, 0x000000, 1.0f - maxLuminance / lum);
    }

    private static int opaque(int argb) {
        return argb | 0xFF000000;
    }

    static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    static void panel(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, PANEL_BG);
        graphics.renderOutline(x, y, w, h, PANEL_LINE);
    }

    static void bar(GuiGraphics graphics, int x, int y, int w, int h, float fraction, int color) {
        graphics.fill(x, y, x + w, y + h, BUTTON_OFF);
        int filled = Math.round(w * Math.max(0.0f, Math.min(1.0f, fraction)));
        if (filled > 0) graphics.fill(x, y, x + filled, y + h, color);
        graphics.renderOutline(x, y, w, h, BUTTON_LINE);
    }

    static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    static String duration(long millis) {
        long total = Math.max(0L, millis) / 1000L;
        long hours = total / 3600L;
        long minutes = (total / 60L) % 60L;
        long seconds = total % 60L;
        return hours > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds) :
                String.format(Locale.ROOT, "%d:%02d", minutes, seconds);
    }

    static int wearColor(float wear) {
        return wear < 0.35f ? GOOD : wear < 0.7f ? WARN : BAD;
    }
}
