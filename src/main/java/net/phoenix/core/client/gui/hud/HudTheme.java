package net.phoenix.core.client.gui.hud;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;

import brachy.modularui.api.drawable.IDrawable;

/**
 * How a machine screen looks: its colours, what is drawn behind everything, and how its tabs are laid out. Every machine
 * picks its own, so they do not all read as the same screen with different numbers.
 */
public record HudTheme(int bgTop, int bgBottom, int accent, int accent2, Pattern pattern, TabStyle tabs) {

    public enum Pattern {
        GRID,
        HEX,
        DOTS,
        SCAN,
        CIRCUIT,
        STARS,
        NONE
    }

    public enum TabStyle {
        /** A column of squares down the left edge. */
        SIDE,
        /** A column of diamonds down the left edge. */
        DIAMOND,
        /** A bar of tabs along the top edge. */
        TOP
    }

    public static final HudTheme TEAL = new HudTheme(0xFF_02101A, 0xFF_010508, 0xFF_00E5CC, 0xFF_00AAA0, Pattern.GRID,
            TabStyle.SIDE);
    public static final HudTheme ORGANIC = new HudTheme(0xFF_04120A, 0xFF_010603, 0xFF_6BFF8A, 0xFF_2FA84F,
            Pattern.DOTS, TabStyle.DIAMOND);
    public static final HudTheme FURNACE = new HudTheme(0xFF_160905, 0xFF_070302, 0xFF_FF8A3A, 0xFF_B8481A,
            Pattern.CIRCUIT, TabStyle.SIDE);
    public static final HudTheme ARCANE = new HudTheme(0xFF_12071E, 0xFF_05020A, 0xFF_C98CFF, 0xFF_7A3FD1, Pattern.HEX,
            TabStyle.DIAMOND);
    public static final HudTheme REACTOR = new HudTheme(0xFF_1A0714, 0xFF_070208, 0xFF_FF5CB8, 0xFF_B0287A,
            Pattern.SCAN, TabStyle.SIDE);
    public static final HudTheme DATA = new HudTheme(0xFF_03101E, 0xFF_010409, 0xFF_52B8FF, 0xFF_2A74C4, Pattern.SCAN,
            TabStyle.TOP);
    public static final HudTheme LOCK = new HudTheme(0xFF_171003, 0xFF_070401, 0xFF_FFC44D, 0xFF_B07A18,
            Pattern.CIRCUIT, TabStyle.TOP);
    public static final HudTheme NEON = new HudTheme(0xFF_0E0618, 0xFF_030108, 0xFF_FF4DE1, 0xFF_00E5FF, Pattern.SCAN,
            TabStyle.TOP);
    public static final HudTheme ASTRAL = new HudTheme(0xFF_050818, 0xFF_010208, 0xFF_E8D27A, 0xFF_6A78FF,
            Pattern.STARS, TabStyle.DIAMOND);
    public static final HudTheme DEEP_SPACE = new HudTheme(0xFF_02030A, 0xFF_000002, 0xFF_FFD27A, 0xFF_8CA8FF,
            Pattern.STARS, TabStyle.SIDE);

    public static final HudTheme DRONE = new HudTheme(0xFF_04141A, 0xFF_010708, 0xFF_5CF0D0, 0xFF_2A8F8A, Pattern.HEX,
            TabStyle.SIDE);
    public static final HudTheme EMBER = new HudTheme(0xFF_1C0A04, 0xFF_080201, 0xFF_FFB347, 0xFF_C4501A,
            Pattern.DOTS, TabStyle.DIAMOND);
    public static final HudTheme VAULT = new HudTheme(0xFF_0B1014, 0xFF_030506, 0xFF_9FB8C8, 0xFF_566B79,
            Pattern.GRID, TabStyle.TOP);

    public int line() {
        return GuiDraw.withAlpha(accent, 0x33);
    }

    public int dim() {
        return GuiDraw.mix(accent2, 0xFF_000000, 0.55f);
    }

    /** The panel background: a gradient with this theme's pattern over it. */
    public IDrawable background() {
        return (ctx, x, y, w, h, theme) -> {
            GuiGraphics g = ctx.getGraphics();
            GuiDraw.drawGradientRect(g, x, y, w, h, bgTop, bgBottom);
            pattern(g, x, y, w, h);
            GuiDraw.drawBorder(g, x, y, w, h, accent2, 1);
            GuiDraw.drawBorder(g, x + 2, y + 2, w - 4, h - 4, GuiDraw.withAlpha(accent, 0x22), 1);
        };
    }

    /** The pattern behind a page, drawn in local coordinates. */
    public void pattern(GuiGraphics g, int x, int y, int w, int h) {
        float t = (Util.getMillis() % 100000L) / 1000.0f;
        int faint = GuiDraw.withAlpha(accent, 0x0C);
        switch (pattern) {
            case GRID -> {
                for (int gx = x + 4; gx < x + w; gx += 8) g.fill(gx, y, gx + 1, y + h, faint);
                for (int gy = y + 4; gy < y + h; gy += 8) g.fill(x, gy, x + w, gy + 1, faint);
            }
            case DOTS -> {
                for (int gx = x + 5; gx < x + w; gx += 7) {
                    for (int gy = y + 5; gy < y + h; gy += 7) g.fill(gx, gy, gx + 1, gy + 1, GuiDraw.withAlpha(accent, 0x22));
                }
            }
            case HEX -> {
                // a lattice of small diamonds, every other row shifted
                for (int row = 0, gy = y + 6; gy < y + h; gy += 6, row++) {
                    for (int gx = x + 6 + (row % 2) * 6; gx < x + w; gx += 12) {
                        g.fill(gx, gy - 1, gx + 1, gy, faint);
                        g.fill(gx - 1, gy, gx + 2, gy + 1, faint);
                        g.fill(gx, gy + 1, gx + 1, gy + 2, faint);
                    }
                }
            }
            case SCAN -> {
                for (int gy = y + 2; gy < y + h; gy += 3) g.fill(x, gy, x + w, gy + 1, GuiDraw.withAlpha(0xFF_000000, 0x30));
                int sweep = y + (int) ((t * 40.0f) % (h + 30)) - 15;
                for (int i = 0; i < 12; i++) {
                    int gy = sweep + i;
                    if (gy >= y && gy < y + h) g.fill(x, gy, x + w, gy + 1, GuiDraw.withAlpha(accent, 0x0A + i));
                }
            }
            case CIRCUIT -> {
                // fixed traces: right angles and pads
                long seed = 0x9E3779B97F4A7C15L;
                for (int i = 0; i < 26; i++) {
                    seed = seed * 6364136223846793005L + 1442695040888963407L;
                    int sx = x + 6 + (int) ((seed >>> 33) % Math.max(1, w - 12));
                    int sy = y + 6 + (int) ((seed >>> 13) % Math.max(1, h - 12));
                    int len = 10 + (int) ((seed >>> 50) % 26);
                    boolean horizontal = ((seed >>> 7) & 1) == 0;
                    if (horizontal) {
                        g.fill(sx, sy, Math.min(x + w - 4, sx + len), sy + 1, faint);
                        g.fill(sx + len, sy - 2, sx + len + 1, sy + 3, faint);
                    } else {
                        g.fill(sx, sy, sx + 1, Math.min(y + h - 4, sy + len), faint);
                        g.fill(sx - 2, sy + len, sx + 3, sy + len + 1, faint);
                    }
                    g.fill(sx - 1, sy - 1, sx + 2, sy + 2, GuiDraw.withAlpha(accent, 0x18));
                }
            }
            case STARS -> {
                long seed = 12345L;
                for (int i = 0; i < 60; i++) {
                    seed = seed * 6364136223846793005L + 1442695040888963407L;
                    int sx = x + 3 + (int) ((seed >>> 33) % Math.max(1, w - 6));
                    int sy = y + 3 + (int) ((seed >>> 13) % Math.max(1, h - 6));
                    float twinkle = 0.5f + 0.5f * (float) Math.sin(t * (0.8f + (i % 5) * 0.4f) + i);
                    int a = 0x14 + (int) (0x30 * twinkle);
                    g.fill(sx, sy, sx + 1, sy + 1, GuiDraw.withAlpha(i % 7 == 0 ? accent : 0xFF_FFFFFF, a));
                }
            }
            case NONE -> {}
        }
    }
}
