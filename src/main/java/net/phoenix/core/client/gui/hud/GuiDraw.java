package net.phoenix.core.client.gui.hud;

import net.minecraft.client.gui.GuiGraphics;

/** Small drawing helpers for HUD style screens: borders, gradients, lines, scissoring in local coordinates. */
public final class GuiDraw {

    private GuiDraw() {}

    public static void drawGradientRect(GuiGraphics g, int x, int y, int w, int h, int topColor, int bottomColor) {
        g.fillGradient(x, y, x + w, y + h, topColor, bottomColor);
    }

    public static void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color, int thickness) {
        g.fill(x, y, x + w, y + thickness, color);
        g.fill(x, y + h - thickness, x + w, y + h, color);
        g.fill(x, y + thickness, x + thickness, y + h - thickness, color);
        g.fill(x + w - thickness, y + thickness, x + w, y + h - thickness, color);
    }

    /** A line of one pixel dots, which is all a canvas can do. */
    public static void drawLine(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) {
            g.fill(x1, y1, x1 + 1, y1 + 1, color);
            return;
        }
        for (int s = 0; s <= steps; s++) {
            int px = x1 + Math.round(dx * (s / (float) steps));
            int py = y1 + Math.round(dy * (s / (float) steps));
            g.fill(px, py, px + 1, py + 1, color);
        }
    }

    /**
     * Vanilla 1.21.1 scissors in absolute GUI coordinates and ignores the pose stack, so local drawing code has to
     * push the current translation and scale into the rectangle itself.
     */
    public static void enableScissor(GuiGraphics g, int x1, int y1, int x2, int y2) {
        var m = g.pose().last().pose();
        int ax = Math.round(m.m00() * x1 + m.m30());
        int ay = Math.round(m.m11() * y1 + m.m31());
        int bx = Math.round(m.m00() * x2 + m.m30());
        int by = Math.round(m.m11() * y2 + m.m31());
        g.enableScissor(ax, ay, bx, by);
    }

    public static int withAlpha(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    public static int mix(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return 0xFF000000 | ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) |
                (int) (ab + (bb - ab) * t);
    }
}
