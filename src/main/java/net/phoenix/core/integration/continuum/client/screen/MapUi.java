package net.phoenix.core.integration.continuum.client.screen;

import net.minecraft.client.gui.GuiGraphics;

import java.util.Locale;

final class MapUi {

    private MapUi() {}

    static final int FRAME = 0xFF7a5cff;
    static final int PANEL_BG = 0xD00b0a18;
    static final int PANEL_LINE = 0xFF3a2f7a;
    static final int TITLE = 0xFFE8D8FF;
    static final int TEXT = 0xFFB8B0D8;
    static final int DIM = 0xFF7a7498;
    static final int GOOD = 0xFF7affb0;
    static final int WARN = 0xFFffd27a;
    static final int BAD = 0xFFff6b6b;

    static void panel(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, PANEL_BG);
        graphics.renderOutline(x, y, w, h, PANEL_LINE);
    }

    static void bar(GuiGraphics graphics, int x, int y, int w, int h, float fraction, int color) {
        graphics.fill(x, y, x + w, y + h, 0xFF14122a);
        int filled = Math.round(w * Math.max(0.0f, Math.min(1.0f, fraction)));
        if (filled > 0) graphics.fill(x, y, x + filled, y + h, color);
        graphics.renderOutline(x, y, w, h, 0xFF2a2548);
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
