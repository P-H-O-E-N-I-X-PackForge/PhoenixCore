package net.phoenix.core.client.nullwell;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.phoenixvine.wiki.ui.hud.GuiDraw;

public final class NullWellArt {

    private NullWellArt() {}

    public static final int INK = 0xFF_07030F;
    public static final int GRID = 0xFF_3B2A5C;
    public static final int LILAC = 0xFF_C98CFF;
    public static final int AMBER = 0xFF_FFC44D;
    public static final int RED = 0xFF_FF4A5A;
    public static final int RADIANCE = 0xFF_E8F6FF;

    public static float time() {
        return Util.getMillis() / 1000.0f;
    }

    public static int ringColor(float stability) {
        if (stability >= 66.0f) return LILAC;
        if (stability >= 33.0f) return GuiDraw.mix(AMBER, LILAC, (stability - 33.0f) / 33.0f);
        return GuiDraw.mix(RED, AMBER, stability / 33.0f);
    }

    public static void draw(GuiGraphics g, int x, int y, int size, float massFrac, float comfortFrac, float stability,
                            float feed, float draw, boolean live) {
        draw(g, x, y, size, massFrac, comfortFrac, stability, feed, draw, live, true);
    }

    public static void draw(GuiGraphics g, int x, int y, int size, float massFrac, float comfortFrac, float stability,
                            float feed, float draw, boolean live, boolean framed) {
        float t = time();
        int cx = x + size / 2;
        int cy = y + size / 2;
        float R = size / 2.0f - 3.0f;

        if (framed) {
            g.fill(x, y, x + size, y + size, INK);
            GuiDraw.drawBorder(g, x, y, size, size, GuiDraw.withAlpha(GRID, 0xFF), 1);
        }

        for (int i = 0; i < 12; i++) {
            float a = i * Mth.PI / 6.0f;
            for (float d = R * 0.12f; d < R; d += 4.0f) {
                dot(g, cx + Mth.cos(a) * d, cy + Mth.sin(a) * d, 1, GuiDraw.withAlpha(GRID, 0x70));
            }
        }
        dottedRing(g, cx, cy, R * 0.34f, 28, GuiDraw.withAlpha(GRID, 0xA0));
        dottedRing(g, cx, cy, R * 0.67f, 44, GuiDraw.withAlpha(GRID, 0xA0));

        float limitR = R * 0.90f;
        float comfortR = horizon(R, comfortFrac);
        dottedRing(g, cx, cy, comfortR * 1.6f, 36, GuiDraw.withAlpha(AMBER, 0x60));
        dashedRing(g, cx, cy, limitR, 60, 0.0f, GuiDraw.withAlpha(RED, 0xB0));

        if (!live) {
            g.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, "no field", cx, cy - 4,
                    GuiDraw.withAlpha(GRID, 0xFF));
            return;
        }

        float r = horizon(R, massFrac);
        boolean shaky = stability < 40.0f;
        float jitter = shaky ? (40.0f - stability) / 40.0f * 1.6f : 0.0f;
        int ring = ringColor(stability);

        int falling = 4 + Math.round(feed * 22.0f);
        for (int i = 0; i < falling; i++) {
            float p = frac(t * (0.12f + 0.2f * feed) + i * 0.6180339f);
            float a = i * 2.399f + p * 4.2f;
            float d = Mth.lerp(p * p, R * 0.97f, r * 1.05f);
            int alpha = (int) (0xFF * (1.0f - p * 0.7f));
            dot(g, cx + Mth.cos(a) * d, cy + Mth.sin(a) * d * 0.96f, p > 0.75f ? 1 : 2,
                    GuiDraw.withAlpha(GuiDraw.mix(RADIANCE, ring, p), alpha));
        }

        float spin = t * (0.25f + 0.9f * feed);
        int dashes = 30;
        for (int layer = 0; layer < 2; layer++) {
            float rr = r * (1.28f + 0.22f * layer) + 2.0f;
            for (int i = 0; i < dashes; i++) {
                float a = spin * (layer == 0 ? 1.0f : -0.7f) + i * Mth.TWO_PI / dashes;
                float bright = 0.35f + 0.65f * (0.5f + 0.5f * Mth.sin(a * 2.0f - t * 2.0f));
                if (shaky && ((i + (int) (t * 12.0f)) % 5 == 0)) continue;
                int c = GuiDraw.withAlpha(ring, (int) (0xFF * bright * (layer == 0 ? 1.0f : 0.6f)));
                arcDash(g, cx, cy, rr, a, 0.07f, c);
            }
        }

        int leaving = Math.round(draw * 20.0f);
        for (int i = 0; i < leaving; i++) {
            float p = frac(t * (0.35f + 0.5f * draw) + i * 0.3819660f);
            float a = i * 1.7f + (float) Math.floor(t * 0.5f + i) * 0.9f;
            float d = r + 3.0f + p * (R * 0.95f - r);
            dot(g, cx + Mth.cos(a) * d, cy + Mth.sin(a) * d, 2,
                    GuiDraw.withAlpha(RADIANCE, (int) (0xFF * (1.0f - p))));
        }

        float jx = shaky ? (Mth.sin(t * 41.0f) * jitter) : 0.0f;
        float jy = shaky ? (Mth.cos(t * 37.0f) * jitter) : 0.0f;
        disc(g, cx + jx, cy + jy, r + 1.5f, ring);
        disc(g, cx + jx, cy + jy, r, 0xFF_000000);

        float glint = r * 0.85f;
        for (int i = -6; i <= 6; i++) {
            float a = -2.4f + i * 0.04f;
            dot(g, cx + jx + Mth.cos(a) * glint, cy + jy + Mth.sin(a) * glint, 1,
                    GuiDraw.withAlpha(RADIANCE, 0x50 - Math.abs(i) * 6));
        }
    }

    public static void markRing(GuiGraphics g, int cx, int cy, float R, float massFrac, int argb) {
        dashedRing(g, cx, cy, horizon(R, massFrac), 48, 0.0f, argb);
    }

    public static float horizon(float R, float massFrac) {
        float m = Math.max(0.0f, massFrac);
        return R * (0.07f + 0.30f * (float) Math.cbrt(m));
    }

    private static void disc(GuiGraphics g, float cx, float cy, float r, int argb) {
        int ri = (int) Math.ceil(r);
        for (int dy = -ri; dy <= ri; dy++) {
            float h = r * r - dy * dy;
            if (h < 0) continue;
            int half = (int) Math.sqrt(h);
            g.fill(Math.round(cx) - half, Math.round(cy) + dy, Math.round(cx) + half + 1, Math.round(cy) + dy + 1,
                    argb);
        }
    }

    private static void dot(GuiGraphics g, float x, float y, int s, int argb) {
        int px = Math.round(x);
        int py = Math.round(y);
        g.fill(px, py, px + s, py + s, argb);
    }

    private static void dottedRing(GuiGraphics g, int cx, int cy, float r, int n, int argb) {
        for (int i = 0; i < n; i++) {
            float a = i * Mth.TWO_PI / n;
            dot(g, cx + Mth.cos(a) * r, cy + Mth.sin(a) * r, 1, argb);
        }
    }

    private static void dashedRing(GuiGraphics g, int cx, int cy, float r, int n, float phase, int argb) {
        for (int i = 0; i < n; i += 2) {
            arcDash(g, cx, cy, r, phase + i * Mth.TWO_PI / n, Mth.TWO_PI / n * 0.8f, argb);
        }
    }

    private static void arcDash(GuiGraphics g, int cx, int cy, float r, float a, float span, int argb) {
        int steps = Math.max(2, (int) (r * span));
        for (int i = 0; i <= steps; i++) {
            float aa = a + span * (i / (float) steps - 0.5f);
            dot(g, cx + Mth.cos(aa) * r, cy + Mth.sin(aa) * r, 1, argb);
        }
    }

    private static float frac(float x) {
        return x - (float) Math.floor(x);
    }
}
