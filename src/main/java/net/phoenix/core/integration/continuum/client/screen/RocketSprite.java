package net.phoenix.core.integration.continuum.client.screen;

import net.minecraft.client.gui.GuiGraphics;

final class RocketSprite {

    private RocketSprite() {}

    static void draw(GuiGraphics graphics, float cx, float tailY, float rocketHeight, float alpha) {
        if (alpha <= 0.01f || rocketHeight < 2.0f) return;
        int top = Math.round(tailY - rocketHeight);
        int rows = Math.max(2, Math.round(rocketHeight));
        float bodyHalf = rocketHeight * 0.085f;
        int a = (int) (alpha * 255.0f) << 24;

        for (int i = 0; i < rows; i++) {
            float u = i / (float) rows;
            
            float half = u < 0.26f ? bodyHalf * (float) Math.sin(Math.PI / 2.0 * (u / 0.26f)) : bodyHalf;
            boolean fin = u > 0.80f;
            float outer = fin ? half + bodyHalf * 0.95f * ((u - 0.80f) / 0.20f) : half;

            int y = top + i;
            int body = Math.max(1, Math.round(half));
            int finW = Math.max(body, Math.round(outer));

            int hull = u < 0.20f ? 0xd8362f : (u > 0.58f && u < 0.64f ? 0x3a3f55 : 0xe8ecf4);
            int shade = scale(hull, 0.62f);
            int light = scale(hull, 1.18f);

            if (fin && finW > body) {
                graphics.fill((int) cx - finW, y, (int) cx - body, y + 1, a | 0xb02a2a);
                graphics.fill((int) cx + body, y, (int) cx + finW, y + 1, a | 0x7a1c1c);
            }
            graphics.fill((int) cx - body, y, (int) cx, y + 1, a | hull);
            graphics.fill((int) cx, y, (int) cx + body, y + 1, a | shade);
            graphics.fill((int) cx - body + Math.max(1, body / 4), y, (int) cx - body + Math.max(2, body / 2), y + 1,
                    a | light);
        }

        int wy = top + Math.round(rocketHeight * 0.38f);
        int wr = Math.max(2, Math.round(bodyHalf * 0.42f));
        graphics.fill((int) cx - wr - 1, wy - wr - 1, (int) cx + wr + 1, wy + wr + 1, a | 0x2a3150);
        graphics.fill((int) cx - wr, wy - wr, (int) cx + wr, wy + wr, a | 0x6cc4ff);

        int nozzle = Math.max(2, Math.round(bodyHalf * 0.55f));
        graphics.fill((int) cx - nozzle, top + rows, (int) cx + nozzle, top + rows + Math.max(2, nozzle / 2),
                a | 0x2a2d3a);
    }

    private static int scale(int rgb, float f) {
        int r = Math.min(255, Math.round(((rgb >> 16) & 0xFF) * f));
        int g = Math.min(255, Math.round(((rgb >> 8) & 0xFF) * f));
        int b = Math.min(255, Math.round((rgb & 0xFF) * f));
        return (r << 16) | (g << 8) | b;
    }
}
