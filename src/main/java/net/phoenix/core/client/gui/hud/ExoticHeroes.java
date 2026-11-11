package net.phoenix.core.client.gui.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

import java.util.function.Supplier;

public final class ExoticHeroes {

    private ExoticHeroes() {}

    private static float frac(float v) {
        return v - (float) Math.floor(v);
    }

    private static void dot(GuiGraphics g, float x, float y, int r, int color) {
        g.fill(Math.round(x) - r, Math.round(y) - r, Math.round(x) + r + 1, Math.round(y) + r + 1, color);
    }

    public static Heroes.Hero sourceOrb(Supplier<Float> fill, boolean inward, int color) {
        return (g, x, y, w, h, t) -> {
            float level = Mth.clamp(fill.get(), 0.0f, 1.0f);
            float cx = x + w / 2.0f;
            float cy = y + h / 2.0f;
            float r = Math.min(w / 2.0f - 6, h / 2.0f - 2);
            if (r < 6) return;

            for (int i = 0; i < 3; i++) {
                float hr = r + 2 + i * 2;
                float sweep = t * (0.5f + i * 0.25f) * (inward ? -1 : 1);
                for (int k = 0; k < 12; k++) {
                    double a = sweep + k * Math.PI * 2 / 12 + i;
                    dot(g, cx + (float) Math.cos(a) * hr * 1.5f, cy + (float) Math.sin(a) * hr * 0.6f, 0,
                            GuiDraw.withAlpha(color, 0x70 - i * 0x20));
                }
            }

            float top = cy + r - level * 2 * r;
            for (int row = (int) -r; row <= r; row++) {
                float py = cy + row;
                float half = (float) Math.sqrt(Math.max(0.0f, r * r - row * row));
                if (half <= 0) continue;

                g.fill(Math.round(cx - half), Math.round(py), Math.round(cx + half), Math.round(py) + 1,
                        GuiDraw.withAlpha(color, 0x12));
                if (level <= 0) continue;
                float ripple = (float) Math.sin(t * 2.2f + row * 0.7f) * 1.2f * Math.min(1f, level * 4f);
                if (py + ripple < top) continue;
                float depth = Mth.clamp((py - top) / (2 * r), 0f, 1f);
                int c = GuiDraw.mix(color, 0xFF_FFFFFF, 0.25f * (1f - depth));
                g.fill(Math.round(cx - half), Math.round(py), Math.round(cx + half), Math.round(py) + 1,
                        GuiDraw.withAlpha(c, 0x90));
            }

            for (int k = 0; k < 40; k++) {
                double a = k * Math.PI * 2 / 40;
                dot(g, cx + (float) Math.cos(a) * r, cy + (float) Math.sin(a) * r, 0, GuiDraw.withAlpha(color, 0xB0));
            }

            int motes = 9;
            for (int i = 0; i < motes; i++) {
                float phase = frac(t * 0.22f + i / (float) motes);
                float radius = (inward ? 1.0f - phase : phase) * (r + 14);
                double a = t * 1.1f + i * 2.4f + phase * 4.0f;
                float alpha = (float) Math.sin(phase * Math.PI);
                dot(g, cx + (float) Math.cos(a) * radius * 1.4f, cy + (float) Math.sin(a) * radius * 0.7f, 1,
                        GuiDraw.withAlpha(0xFF_FFFFFF, (int) (0xC0 * alpha)));
            }
        };
    }

    public static Heroes.Hero threadLoom(Supplier<Float> fill, boolean inward, int color, int star) {
        return (g, x, y, w, h, t) -> {
            float level = Mth.clamp(fill.get(), 0.0f, 1.0f);

            for (int i = 0; i < 14; i++) {
                float sx = x + ((i * 53) % 97) / 97.0f * w;
                float sy = y + ((i * 31) % 89) / 89.0f * h;
                float twinkle = 0.35f + 0.65f * (0.5f + 0.5f * (float) Math.sin(t * 1.7f + i * 1.9f));
                dot(g, sx, sy, 0, GuiDraw.withAlpha(star, (int) (0x90 * twinkle)));
            }

            int strands = 6;
            int lit = Math.max(1, Math.round(level * strands));
            float dir = inward ? -1.0f : 1.0f;
            for (int s = 0; s < strands; s++) {
                boolean on = s < lit && level > 0.0f;
                float baseY = y + h * (s + 0.5f) / strands;
                float amp = (on ? 2.0f + 4.0f * level : 1.0f) * (0.7f + 0.3f * (s % 3));
                int c = on ? GuiDraw.withAlpha(color, 0xE0) : GuiDraw.withAlpha(color, 0x35);
                int px = x;
                int py = Math.round(baseY);
                for (int i = 1; i <= w; i += 2) {
                    float wave = (float) Math.sin(i * 0.11f + s * 1.3f + dir * t * (on ? 1.8f : 0.5f)) * amp;
                    int cxp = x + i;
                    int cyp = Math.round(baseY + wave);
                    GuiDraw.drawLine(g, px, py, cxp, cyp, c);
                    px = cxp;
                    py = cyp;
                }

                if (on) {
                    float bead = frac(t * (0.25f + s * 0.04f) * dir + s * 0.17f);
                    float bx = x + bead * w;
                    float by = baseY + (float) Math.sin((bx - x) * 0.11f + s * 1.3f + dir * t * 1.8f) * amp;
                    dot(g, bx, by, 1, GuiDraw.withAlpha(0xFF_FFFFFF, 0xE0));
                }
            }
        };
    }
}
