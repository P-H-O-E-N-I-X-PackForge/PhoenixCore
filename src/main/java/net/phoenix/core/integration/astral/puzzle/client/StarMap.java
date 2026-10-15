package net.phoenix.core.integration.astral.puzzle.client;

import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.integration.astral.puzzle.StarChart;

@OnlyIn(Dist.CLIENT)
final class StarMap {

    static final int STAR_COLOR = 0xFFE8E0FF;
    static final int LINE_COLOR = 0xFF9A86FF;
    static final int KNOWN_COLOR = 0xFFFFD27A;
    static final int PICK_COLOR = 0xFF7AFFB0;

    private final int left;
    private final int top;
    private final int size;

    StarMap(int left, int top, int size) {
        this.left = left;
        this.top = top;
        this.size = size;
    }

    int left() {
        return left;
    }

    int top() {
        return top;
    }

    int size() {
        return size;
    }

    int px(int gridX) {
        return left + 12 + Math.round(gridX * (size - 24) / (float) (StarChart.GRID - 1));
    }

    int py(int gridY) {
        return top + 12 + Math.round(gridY * (size - 24) / (float) (StarChart.GRID - 1));
    }

    void background(GuiGraphics g) {
        g.fill(left, top, left + size, top + size, 0xFF07061A);
        g.renderOutline(left, top, size, size, 0xFF3A2F7A);

        for (int i = 0; i < StarChart.GRID; i++) {
            g.fill(px(i), top + 4, px(i) + 1, top + size - 4, 0x14FFFFFF);
            g.fill(left + 4, py(i), left + size - 4, py(i) + 1, 0x14FFFFFF);
        }
    }

    void star(GuiGraphics g, int x, int y, int color, boolean big) {
        int r = big ? 4 : 3;
        g.fill(x - r - 2, y - r - 2, x + r + 3, y + r + 3, (color & 0xFFFFFF) | 0x22000000);
        g.fill(x - r, y - r, x + r + 1, y + r + 1, color);
    }

    static void line(GuiGraphics g, int x1, int y1, int x2, int y2, int color, boolean arrow) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) return;
        for (int i = 0; i <= steps; i++) {
            int x = x1 + dx * i / steps;
            int y = y1 + dy * i / steps;
            g.fill(x, y, x + 2, y + 2, color);
        }
        if (arrow) {

            for (int k = 0; k < 3; k++) {
                float t = 0.80f - k * 0.07f;
                int x = x1 + Math.round(dx * t);
                int y = y1 + Math.round(dy * t);
                g.fill(x - 1, y - 1, x + 3, y + 3, 0xFFFFFFFF);
            }
        }
    }
}
