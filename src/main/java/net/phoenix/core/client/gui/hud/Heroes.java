package net.phoenix.core.client.gui.hud;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

import java.util.function.Supplier;

/**
 * Animated pictures for the top of a machine screen, each a different idea: a shield ring, turning runes, packets
 * flowing down lanes, an equaliser, a growing vine, a constellation, a set of lock tumblers, a gauge, a pulse line.
 * They move on their own and are driven by the machine's numbers, so a screen has something to look at.
 */
public final class Heroes {

    private Heroes() {}

    @FunctionalInterface
    public interface Hero {

        /** Draw into the box at (x, y) of w by h, {@code t} seconds into the animation. */
        void draw(GuiGraphics g, int x, int y, int w, int h, float t);
    }

    private static float time() {
        return (Util.getMillis() % 1_000_000L) / 1000.0f;
    }

    public static float now() {
        return time();
    }

    private static void arc(GuiGraphics g, float cx, float cy, float rx, float ry, double from, double to, int color) {
        int steps = Math.max(8, (int) (Math.abs(to - from) * Math.max(rx, ry) / 1.6));
        int px = Math.round(cx + (float) Math.cos(from) * rx);
        int py = Math.round(cy + (float) Math.sin(from) * ry);
        for (int i = 1; i <= steps; i++) {
            double a = from + (to - from) * i / steps;
            int x = Math.round(cx + (float) Math.cos(a) * rx);
            int y = Math.round(cy + (float) Math.sin(a) * ry);
            GuiDraw.drawLine(g, px, py, x, y, color);
            px = x;
            py = y;
        }
    }

    private static void dot(GuiGraphics g, float x, float y, int r, int color) {
        g.fill(Math.round(x) - r, Math.round(y) - r, Math.round(x) + r + 1, Math.round(y) + r + 1, color);
    }

    // ------------------------------------------------------------------------------------------------ shield ring

    /** Concentric rings, the outer one filled by how full the shield is, ticks turning round it. */
    public static Hero shieldRing(Supplier<Float> fill, int color) {
        return (g, x, y, w, h, t) -> {
            float cx = x + w / 2.0f;
            float cy = y + h / 2.0f;
            float r = Math.min(w, h) / 2.0f - 3;
            float f = Mth.clamp(fill.get(), 0.0f, 1.0f);
            arc(g, cx, cy, r, r, 0, Math.PI * 2, GuiDraw.withAlpha(color, 0x30));
            arc(g, cx, cy, r, r, -Math.PI / 2, -Math.PI / 2 + Math.PI * 2 * f, color);
            arc(g, cx, cy, r - 5, r - 5, 0, Math.PI * 2, GuiDraw.withAlpha(color, 0x20));
            for (int i = 0; i < 24; i++) {
                double a = Math.PI * 2 * i / 24 + t * 0.35;
                float inner = r - 9;
                float outer = r - (i % 6 == 0 ? 6 : 8);
                GuiDraw.drawLine(g, Math.round(cx + (float) Math.cos(a) * inner), Math.round(cy + (float) Math.sin(a) * inner),
                        Math.round(cx + (float) Math.cos(a) * outer), Math.round(cy + (float) Math.sin(a) * outer),
                        GuiDraw.withAlpha(color, i % 6 == 0 ? 0xAA : 0x55));
            }
            float breath = 0.55f + 0.45f * (float) Math.sin(t * 2.0);
            dot(g, cx, cy, 2, GuiDraw.withAlpha(color, (int) (0xFF * breath * f)));
            arc(g, cx, cy, 6 + 3 * breath, 6 + 3 * breath, 0, Math.PI * 2, GuiDraw.withAlpha(color, (int) (0x80 * f)));
        };
    }

    // ------------------------------------------------------------------------------------------------ runes

    /** A ring of rune ticks turning with orbiting sparks; more power spins it faster and brighter. */
    public static Hero runes(Supplier<Float> power, int color) {
        return (g, x, y, w, h, t) -> {
            float cx = x + w / 2.0f;
            float cy = y + h / 2.0f;
            float r = Math.min(w, h) / 2.0f - 3;
            float p = Mth.clamp(power.get(), 0.0f, 1.0f);
            float spin = t * (0.2f + 1.3f * p);
            arc(g, cx, cy, r, r, 0, Math.PI * 2, GuiDraw.withAlpha(color, 0x40));
            arc(g, cx, cy, r - 7, r - 7, 0, Math.PI * 2, GuiDraw.withAlpha(color, 0x30));
            for (int i = 0; i < 18; i++) {
                double a = Math.PI * 2 * i / 18 + spin;
                float rr = r - 3.5f;
                float cxr = cx + (float) Math.cos(a) * rr;
                float cyr = cy + (float) Math.sin(a) * rr;
                int variant = i % 4;
                int c = GuiDraw.withAlpha(color, 0x60 + (int) (0x80 * p));
                // each tick a small glyph of strokes
                g.fill(Math.round(cxr), Math.round(cyr) - 2, Math.round(cxr) + 1, Math.round(cyr) + 3, c);
                if (variant == 1) g.fill(Math.round(cxr) - 1, Math.round(cyr), Math.round(cxr) + 3, Math.round(cyr) + 1, c);
                if (variant == 2) g.fill(Math.round(cxr) - 1, Math.round(cyr) - 2, Math.round(cxr) + 2, Math.round(cyr) - 1, c);
                if (variant == 3) g.fill(Math.round(cxr), Math.round(cyr) + 2, Math.round(cxr) + 3, Math.round(cyr) + 3, c);
            }
            // a triangle and a counter-turning one, a seal
            for (int tri = 0; tri < 2; tri++) {
                double base = (tri == 0 ? 1 : -1) * t * 0.5 * (0.3 + p) + tri * Math.PI / 3;
                float rr = r - 9;
                int[] px = new int[3];
                int[] py = new int[3];
                for (int i = 0; i < 3; i++) {
                    double a = base + Math.PI * 2 * i / 3;
                    px[i] = Math.round(cx + (float) Math.cos(a) * rr);
                    py[i] = Math.round(cy + (float) Math.sin(a) * rr);
                }
                for (int i = 0; i < 3; i++) {
                    GuiDraw.drawLine(g, px[i], py[i], px[(i + 1) % 3], py[(i + 1) % 3],
                            GuiDraw.withAlpha(color, 0x50 + (int) (0x70 * p)));
                }
            }
            for (int i = 0; i < 3; i++) {
                double a = -spin * 1.7 + i * Math.PI * 2 / 3;
                dot(g, cx + (float) Math.cos(a) * (r + 1), cy + (float) Math.sin(a) * (r + 1), 1,
                        GuiDraw.withAlpha(0xFF_FFFFFF, 0xC0));
            }
        };
    }

    // ------------------------------------------------------------------------------------------------ data flow

    /** Lanes of packets running left to right; faster and denser with the rate; the colour shifts down the lane. */
    public static Hero dataFlow(Supplier<Float> rate, int lanes, int from, int to) {
        return (g, x, y, w, h, t) -> {
            float r = Mth.clamp(rate.get(), 0.0f, 1.0f);
            int laneH = Math.max(4, h / Math.max(1, lanes));
            for (int lane = 0; lane < lanes; lane++) {
                int ly = y + lane * laneH + laneH / 2;
                g.fill(x, ly, x + w, ly + 1, GuiDraw.withAlpha(from, 0x28));
                int packets = 2 + (int) (r * 5);
                for (int i = 0; i < packets; i++) {
                    float speed = 18 + 70 * r + lane * 5;
                    float pos = ((t * speed + i * (w / (float) packets) + lane * 13) % (w + 20)) - 10;
                    float f = Mth.clamp(pos / w, 0, 1);
                    int col = GuiDraw.mix(from, to, f);
                    int a = (int) (0x40 + 0xA0 * r);
                    int len = 4 + (int) (6 * r);
                    g.fill(Math.round(x + pos), ly - 1, Math.round(x + pos) + len, ly + 2, GuiDraw.withAlpha(col, a));
                }
            }
            // gates at both ends
            g.fill(x, y, x + 2, y + h, GuiDraw.withAlpha(from, 0xB0));
            g.fill(x + w - 2, y, x + w, y + h, GuiDraw.withAlpha(to, 0xB0));
        };
    }

    // ------------------------------------------------------------------------------------------------ equaliser

    /** An equaliser: bars that dance harder with the level, kicking at the beat. */
    public static Hero equaliser(Supplier<Float> level, Supplier<Float> bpm, int color, int peak) {
        return (g, x, y, w, h, t) -> {
            float l = Mth.clamp(level.get(), 0.0f, 1.0f);
            float b = Math.max(60.0f, bpm.get());
            float beat = (float) Math.pow(0.5 + 0.5 * Math.cos(t * b / 60.0 * Math.PI * 2), 3.0);
            int bars = Math.max(8, w / 7);
            for (int i = 0; i < bars; i++) {
                float n1 = (float) (0.5 + 0.5 * Math.sin(t * (2.1 + (i % 5) * 0.7) + i * 1.9));
                float n2 = (float) (0.5 + 0.5 * Math.sin(t * (4.3 + (i % 3)) + i * 0.7));
                float v = Mth.clamp((0.12f + 0.88f * l) * (0.25f + 0.5f * n1 + 0.25f * n2) * (0.75f + 0.5f * beat), 0.03f, 1.0f);
                int bh = Math.max(1, Math.round(v * h));
                int bx = x + i * (w / bars);
                int col = GuiDraw.mix(color, peak, v);
                g.fill(bx, y + h - bh, bx + Math.max(2, w / bars - 2), y + h, GuiDraw.withAlpha(col, 0xB0));
                g.fill(bx, y + h - bh, bx + Math.max(2, w / bars - 2), y + h - bh + 1, GuiDraw.withAlpha(0xFF_FFFFFF, 0xC0));
            }
        };
    }

    // ------------------------------------------------------------------------------------------------ vines

    /** A vine that grows branch by branch as the progress rises, swaying a little. */
    public static Hero vines(Supplier<Float> progress, Supplier<Integer> stage, int color, int leaf) {
        return (g, x, y, w, h, t) -> {
            float p = Mth.clamp(progress.get(), 0.0f, 1.0f);
            int s = Math.max(1, stage.get());
            int stems = Math.min(9, 2 + s);
            for (int i = 0; i < stems; i++) {
                float baseX = x + (i + 0.5f) * w / stems;
                float grow = Mth.clamp((p * (stems + 1) - i * 0.6f) / 1.5f + 0.15f * Math.min(1, s - 1), 0.0f, 1.0f);
                float height = h * (0.45f + 0.5f * ((i * 37) % 10) / 10.0f) * grow;
                float px = baseX;
                float py = y + h - 1;
                int segs = Math.max(2, (int) (height / 4));
                for (int k = 1; k <= segs; k++) {
                    float f = k / (float) segs;
                    float sway = (float) Math.sin(t * 0.9 + i * 1.7 + f * 3.0) * 3.0f * f + (float) Math.sin(f * 5 + i) * 4 * f;
                    float nx = baseX + sway;
                    float ny = y + h - 1 - height * f;
                    GuiDraw.drawLine(g, Math.round(px), Math.round(py), Math.round(nx), Math.round(ny),
                            GuiDraw.withAlpha(color, 0xC0));
                    if (k % 2 == 0 && f > 0.25f) {
                        int side = (k / 2) % 2 == 0 ? 1 : -1;
                        float lx = nx + side * 4;
                        g.fill(Math.round(lx), Math.round(ny) - 1, Math.round(lx) + 3, Math.round(ny) + 1, GuiDraw.withAlpha(leaf, 0xD0));
                    }
                    px = nx;
                    py = ny;
                }
                if (grow > 0.8f) dot(g, px, py, 1, GuiDraw.withAlpha(0xFF_FFF4A0, 0xE0));
            }
            g.fill(x, y + h - 1, x + w, y + h, GuiDraw.withAlpha(color, 0x60));
        };
    }

    // ------------------------------------------------------------------------------------------------ constellation

    /** A scatter of stars that link up into a constellation as the fraction rises. */
    public static Hero constellation(long seed, Supplier<Float> fraction, int color) {
        return (g, x, y, w, h, t) -> {
            float f = Mth.clamp(fraction.get(), 0.0f, 1.0f);
            int n = 11;
            float[] sx = new float[n];
            float[] sy = new float[n];
            long s = seed * 31 + 7;
            for (int i = 0; i < n; i++) {
                s = s * 6364136223846793005L + 1442695040888963407L;
                sx[i] = x + 6 + (int) ((s >>> 33) % Math.max(1, w - 12));
                s = s * 6364136223846793005L + 1442695040888963407L;
                sy[i] = y + 4 + (int) ((s >>> 33) % Math.max(1, h - 8));
            }
            int linked = (int) (f * (n - 1));
            for (int i = 0; i < linked; i++) {
                GuiDraw.drawLine(g, Math.round(sx[i]), Math.round(sy[i]), Math.round(sx[i + 1]), Math.round(sy[i + 1]),
                        GuiDraw.withAlpha(color, 0x90));
            }
            for (int i = 0; i < n; i++) {
                boolean lit = i <= linked;
                float tw = 0.6f + 0.4f * (float) Math.sin(t * 1.5 + i * 2.0);
                dot(g, sx[i], sy[i], lit ? 1 : 0, GuiDraw.withAlpha(lit ? 0xFF_FFFFFF : color, (int) ((lit ? 0xFF : 0x60) * tw)));
                if (lit) {
                    g.fill(Math.round(sx[i]) - 3, Math.round(sy[i]), Math.round(sx[i]) + 4, Math.round(sy[i]) + 1,
                            GuiDraw.withAlpha(color, 0x40));
                    g.fill(Math.round(sx[i]), Math.round(sy[i]) - 3, Math.round(sx[i]) + 1, Math.round(sy[i]) + 4,
                            GuiDraw.withAlpha(color, 0x40));
                }
            }
        };
    }

    // ------------------------------------------------------------------------------------------------ tumblers

    /** Rows of lock tumblers; solved ones click to a stop and glow, the rest keep turning. */
    public static Hero tumblers(Supplier<Integer> solved, Supplier<Integer> total, int color) {
        return (g, x, y, w, h, t) -> {
            int n = Math.max(1, Math.min(14, total.get()));
            int done = Mth.clamp(solved.get(), 0, n);
            int dw = Math.min(h - 2, w / n - 3);
            float r = dw / 2.0f - 1;
            for (int i = 0; i < n; i++) {
                float cx = x + (w - n * (dw + 3)) / 2.0f + i * (dw + 3) + dw / 2.0f;
                float cy = y + h / 2.0f;
                boolean set = i < done;
                arc(g, cx, cy, r, r, 0, Math.PI * 2, GuiDraw.withAlpha(color, set ? 0xC0 : 0x50));
                double a = set ? -Math.PI / 2 : t * (1.2 + i * 0.3) + i;
                GuiDraw.drawLine(g, Math.round(cx), Math.round(cy), Math.round(cx + (float) Math.cos(a) * r),
                        Math.round(cy + (float) Math.sin(a) * r), set ? 0xFF_FFFFFF : GuiDraw.withAlpha(color, 0xA0));
                if (set) dot(g, cx, cy - r - 2, 0, GuiDraw.withAlpha(color, 0xFF));
            }
        };
    }

    // ------------------------------------------------------------------------------------------------ arc gauge

    /** A half-moon gauge with a needle that settles and shivers a little. */
    public static Hero arcGauge(Supplier<Float> value, int color) {
        return (g, x, y, w, h, t) -> {
            float v = Mth.clamp(value.get(), 0.0f, 1.0f);
            float cx = x + w / 2.0f;
            float cy = y + h - 4;
            float r = Math.min(w / 2.0f - 4, h - 8);
            arc(g, cx, cy, r, r, Math.PI, Math.PI * 2, GuiDraw.withAlpha(color, 0x40));
            arc(g, cx, cy, r, r, Math.PI, Math.PI + Math.PI * v, color);
            arc(g, cx, cy, r - 4, r - 4, Math.PI, Math.PI * 2, GuiDraw.withAlpha(color, 0x20));
            for (int i = 0; i <= 10; i++) {
                double a = Math.PI + Math.PI * i / 10;
                GuiDraw.drawLine(g, Math.round(cx + (float) Math.cos(a) * (r - 7)), Math.round(cy + (float) Math.sin(a) * (r - 7)),
                        Math.round(cx + (float) Math.cos(a) * (r - (i % 5 == 0 ? 3 : 5))),
                        Math.round(cy + (float) Math.sin(a) * (r - (i % 5 == 0 ? 3 : 5))), GuiDraw.withAlpha(color, 0x80));
            }
            double needle = Math.PI + Math.PI * v + Math.sin(t * 13) * 0.012;
            GuiDraw.drawLine(g, Math.round(cx), Math.round(cy), Math.round(cx + (float) Math.cos(needle) * (r - 5)),
                    Math.round(cy + (float) Math.sin(needle) * (r - 5)), 0xFF_FFFFFF);
            dot(g, cx, cy, 2, color);
        };
    }

    // ------------------------------------------------------------------------------------------------ pulse line

    /** A heart monitor line: a steady trace with a spike per beat, sharper and quicker with the level. */
    public static Hero pulseLine(Supplier<Float> level, int color) {
        return (g, x, y, w, h, t) -> {
            float l = Mth.clamp(level.get(), 0.0f, 1.0f);
            float rate = 0.6f + 1.8f * l;
            int mid = y + h / 2;
            int px = x;
            int py = mid;
            for (int i = 0; i <= w; i++) {
                float tt = t * rate - i / (float) w * 2.2f;
                float phase = tt - (float) Math.floor(tt);
                float v = 0;
                if (phase < 0.08f) v = (float) Math.sin(phase / 0.08f * Math.PI) * (0.3f + 0.7f * l);
                else if (phase < 0.14f) v = -0.35f * (float) Math.sin((phase - 0.08f) / 0.06f * Math.PI) * l;
                int cy = mid - Math.round(v * (h / 2 - 2));
                if (i > 0) GuiDraw.drawLine(g, px, py, x + i, cy, GuiDraw.withAlpha(color, 0x40 + (int) (0xA0 * (i / (float) w))));
                px = x + i;
                py = cy;
            }
            dot(g, px, py, 1, 0xFF_FFFFFF);
            g.fill(x, mid, x + w, mid + 1, GuiDraw.withAlpha(color, 0x20));
        };
    }

    // ------------------------------------------------------------------------------------------------ radar

    /** A radar sweep over rings; each contact shows as a blip that flares when the beam passes it. */
    public static Hero radar(Supplier<Integer> contacts, int color) {
        return (g, x, y, w, h, t) -> {
            float cx = x + w / 2.0f;
            float cy = y + h / 2.0f;
            float rx = w / 2.0f - 3;
            float ry = h / 2.0f - 2;
            for (int i = 1; i <= 3; i++) arc(g, cx, cy, rx * i / 3, ry * i / 3, 0, Math.PI * 2, GuiDraw.withAlpha(color, 0x30));
            g.fill(Math.round(cx - rx), Math.round(cy), Math.round(cx + rx), Math.round(cy) + 1, GuiDraw.withAlpha(color, 0x20));
            double sweep = (t * 1.1) % (Math.PI * 2);
            for (int i = 0; i < 14; i++) {
                double a = sweep - i * 0.045;
                GuiDraw.drawLine(g, Math.round(cx), Math.round(cy), Math.round(cx + (float) Math.cos(a) * rx),
                        Math.round(cy + (float) Math.sin(a) * ry), GuiDraw.withAlpha(color, Math.max(0, 0x90 - i * 10)));
            }
            int n = Math.min(24, contacts.get());
            long s = 99;
            for (int i = 0; i < n; i++) {
                s = s * 6364136223846793005L + 1442695040888963407L;
                double a = ((s >>> 33) % 6283) / 1000.0;
                s = s * 6364136223846793005L + 1442695040888963407L;
                float d = 0.2f + 0.75f * ((s >>> 33) % 1000) / 1000.0f;
                double since = ((sweep - a) % (Math.PI * 2) + Math.PI * 2) % (Math.PI * 2);
                int alpha = (int) (0x40 + 0xBF * Math.max(0.0, 1.0 - since / 3.0));
                dot(g, cx + (float) Math.cos(a) * rx * d, cy + (float) Math.sin(a) * ry * d, 1, GuiDraw.withAlpha(0xFF_FFFFFF, alpha));
            }
        };
    }

    // ------------------------------------------------------------------------------------------------ embers

    /** Sparks rising off a glowing bed; a hotter level throws more of them, higher. */
    public static Hero embers(Supplier<Float> heat, int color, int hot) {
        return (g, x, y, w, h, t) -> {
            float l = Mth.clamp(heat.get(), 0.0f, 1.0f);
            for (int i = 0; i < 4; i++) {
                int a = (int) ((0x20 + 0x50 * l) * (1 - i / 4.0f));
                g.fill(x, y + h - 2 - i * 2, x + w, y + h - i * 2, GuiDraw.withAlpha(GuiDraw.mix(hot, color, i / 4.0f), a));
            }
            int n = 10 + (int) (28 * l);
            for (int i = 0; i < n; i++) {
                float seed = (i * 0.6180339f) % 1.0f;
                float life = (t * (0.18f + 0.35f * seed) * (0.6f + l) + seed * 7) % 1.0f;
                float px = x + seed * w + (float) Math.sin(t * 1.3 + i) * 5 * life;
                float py = y + h - 3 - life * (h - 4) * (0.5f + 0.5f * l);
                int a = (int) (0xE0 * (1 - life) * (0.4f + 0.6f * l));
                g.fill(Math.round(px), Math.round(py), Math.round(px) + 1, Math.round(py) + (life < 0.3f ? 2 : 1),
                        GuiDraw.withAlpha(GuiDraw.mix(0xFF_FFF0B0, color, life), a));
            }
        };
    }
}
