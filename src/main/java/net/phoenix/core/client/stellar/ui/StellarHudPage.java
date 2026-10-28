package net.phoenix.core.client.stellar.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.phoenix.core.client.gui.hud.CanvasPainter;
import net.phoenix.core.client.gui.hud.GuiDraw;
import net.phoenix.core.client.stellar.StellarForgeClient;
import net.phoenix.core.common.machine.multiblock.stellar.StellarControls;
import net.phoenix.core.common.machine.multiblock.stellar.StellarForgeMachine;
import net.phoenix.core.common.machine.multiblock.stellar.StellarForgeState;

import java.util.Locale;

/**
 * The first page: where the forge is on its way from gas cloud to black hole, in numbers, with the three settings to
 * keep inside their bands (click or drag a slider, or scroll over it).
 */
public final class StellarHudPage implements CanvasPainter {

    static final int C_CYAN = 0xFF_00E5CC;
    static final int C_TEAL = 0xFF_00AAA0;
    static final int C_DIM = 0xFF_3A5E6A;
    static final int C_MID = 0xFF_6A9BAA;
    static final int C_WHITE = 0xFF_DCF0F4;
    static final int C_GOLD = 0xFF_FFC44D;
    static final int C_ORANGE = 0xFF_FF8833;
    static final int C_RED = 0xFF_FF3333;
    static final int C_GREEN = 0xFF_33FF88;
    static final int C_BLUE = 0xFF_44AAFF;
    static final int C_PURPLE = 0xFF_B08CFF;

    private static final String[] RUNGS = { "GAS CLOUD", "PROTOSTAR", "STAR", "BLACK HOLE" };

    private static final int SLIDER_X = 64;
    private static final int SLIDER_W = 120;
    private static final int[] SLIDER_Y = { 150, 164, 178 };

    private final StellarForgeMachine machine;
    private final StellarControls controls;
    private int dragging = -1;
    private int lastSent = -1;

    private final int[] armRect = new int[4];
    private final int[] disperseRect = new int[4];

    public StellarHudPage(StellarForgeMachine machine, StellarControls controls) {
        this.machine = machine;
        this.controls = controls;
    }

    static int stageColor(StellarForgeState.Stage stage) {
        return switch (stage) {
            case IDLE -> C_DIM;
            case GAS_CLOUD -> C_BLUE;
            case PROTOSTAR -> C_ORANGE;
            case STAR -> C_GOLD;
            case BLACK_HOLE -> C_PURPLE;
        };
    }

    static String clock(double seconds) {
        if (!(seconds >= 0.0) || seconds > 99 * 3600) return "--:--";
        long s = (long) seconds;
        return s >= 3600 ? String.format(Locale.ROOT, "%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60) :
                String.format(Locale.ROOT, "%02d:%02d", s / 60, s % 60);
    }

    /** Seconds until the next stage at the mass gained over the last ten readings, or -1. */
    static double eta(StellarForgeMachine machine, StellarForgeState view) {
        var history = StellarForgeClient.animOf(machine).history;
        if (history.size() < 4) return -1.0;
        float[][] all = history.toArray(new float[0][]);
        int from = Math.max(0, all.length - 10);
        double gained = all[all.length - 1][0] - all[from][0];
        double seconds = all.length - 1 - from;
        if (gained <= 0.0 || seconds <= 0.0) return -1.0;
        double left = view.nextMass() - view.mass;
        return left <= 0.0 ? 0.0 : left / (gained / seconds);
    }

    @Override
    public void render(GuiGraphics g, int w, int h, int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;
        g.fill(0, 0, w, h, 0xEE_010810);
        GuiDraw.drawBorder(g, 0, 0, w, h, 0x33_00E5CC, 1);

        if (!machine.isFormed()) {
            g.drawString(font, "STELLAR FORGE", 8, 8, C_CYAN, false);
            g.drawString(font, "Structure not formed.", 8, 24, C_DIM, false);
            return;
        }
        StellarForgeState view = machine.view();
        var stage = view.stage;
        int accent = stageColor(stage);

        // header
        g.drawString(font, "STELLAR FORGE", 8, 7, C_CYAN, false);
        String label = stage.label.toUpperCase(Locale.ROOT);
        int bw = font.width(label) + 10;
        g.fill(w - 8 - bw, 4, w - 8, 16, GuiDraw.withAlpha(accent, 0x30));
        GuiDraw.drawBorder(g, w - 8 - bw, 4, bw, 12, accent, 1);
        g.drawString(font, label, w - 8 - bw + 5, 6, accent, false);
        g.fill(8, 19, w - 8, 20, 0x33_00E5CC);

        ladder(g, font, w, view, accent);
        readouts(g, font, view, accent);
        progress(g, font, w, view, accent);
        sliders(g, font, view, mouseX, mouseY);
        buttons(g, font, view, mouseX, mouseY);

        checklist(g, font, w, view, 214);
    }

    private void ladder(GuiGraphics g, Font font, int w, StellarForgeState view, int accent) {
        int now = view.stage.ordinal();
        for (int i = 0; i < 4; i++) {
            int cx = 34 + i * 66;
            int rung = i + 1;
            if (i < 3) {
                int x0 = cx + 8;
                int x1 = 34 + (i + 1) * 66 - 8;
                g.fill(x0, 30, x1, 32, 0xFF_0A2430);
                double fill = now > rung ? 1.0 : now == rung ? view.massFraction() : 0.0;
                if (fill > 0.0) g.fill(x0, 30, x0 + (int) ((x1 - x0) * fill), 32, accent);
            }
            boolean lit = now >= rung;
            boolean current = now == rung;
            int col = lit ? stageColor(StellarForgeState.Stage.of(rung)) : C_DIM;
            g.fill(cx - 6, 25, cx + 6, 37, current ? GuiDraw.withAlpha(col, 0x55) : 0xFF_010A12);
            GuiDraw.drawBorder(g, cx - 6, 25, 12, 12, col, 1);
            if (lit) g.fill(cx - 3, 28, cx + 3, 34, col);
            String text = RUNGS[i];
            g.drawString(font, text, cx - font.width(text) / 2, 41, current ? C_WHITE : lit ? C_MID : C_DIM, false);
        }
    }

    private void cell(GuiGraphics g, Font font, int x, int y, String label, String value, String sub, int valueColor) {
        g.fill(x, y, x + 80, y + 30, 0xAA_010A12);
        GuiDraw.drawBorder(g, x, y, 80, 30, 0x55_00E5CC, 1);
        g.fill(x, y, x + 2, y + 30, valueColor);
        g.drawString(font, label, x + 6, y + 3, C_DIM, false);
        g.drawString(font, value, x + 6, y + 13, valueColor, false);
        g.drawString(font, sub, x + 6, y + 22, C_MID, false);
    }

    private void readouts(GuiGraphics g, Font font, StellarForgeState view, int accent) {
        float stab = view.stability;
        int stabColor = stab >= StellarForgeState.HOLD_STABILITY ? C_GREEN : stab >= 40 ? C_GOLD : C_RED;
        double k = view.kelvin();
        String temp = k >= 1.0e6 ? String.format(Locale.ROOT, "%.2f MK", k / 1.0e6) :
                String.format(Locale.ROOT, "%,.0f K", k);
        cell(g, font, 8, 52, "MASS", String.format(Locale.ROOT, "%,.1f M", view.mass),
                view.stage == StellarForgeState.Stage.BLACK_HOLE ? "collapsed" :
                        String.format(Locale.ROOT, "%.0f%% of next", view.massFraction() * 100.0), accent);
        cell(g, font, 92, 52, "TEMPERATURE", temp, String.format(Locale.ROOT, "heat %.0f%%", view.heat), C_ORANGE);
        cell(g, font, 176, 52, "STABILITY", String.format(Locale.ROOT, "%.0f%%", stab),
                stab >= StellarForgeState.HOLD_STABILITY ? "holding" : stab >= 40 ? "drifting" : "failing", stabColor);
        cell(g, font, 8, 84, "SIZE", String.format(Locale.ROOT, "%.2f blk", view.radius()),
                String.format(Locale.ROOT, "fits %.1f / 1.2", view.radius()), C_BLUE);
        cell(g, font, 92, 84, "POWER", String.format(Locale.ROOT, "%,d EU/t", machine.energyUse()),
                machine.powerOk() ? "supplied" : "SHORT", machine.powerOk() ? C_CYAN : C_RED);
        cell(g, font, 176, 84, "PLASMA", String.format(Locale.ROOT, "%.1f mB/t", machine.plasmaPerTick()),
                machine.plasmaPerTick() > 0 ? "oxygen plasma" : "nothing yet", C_GREEN);
    }

    private void progress(GuiGraphics g, Font font, int w, StellarForgeState view, int accent) {
        int y = 122;
        String next = switch (view.stage) {
            case IDLE -> "feed it matter to begin";
            case GAS_CLOUD -> "NEXT: PROTOSTAR  at " + (int) StellarForgeState.CLOUD_TO_PROTOSTAR + " M";
            case PROTOSTAR -> "NEXT: STAR  at " + (int) StellarForgeState.PROTOSTAR_TO_STAR + " M";
            case STAR -> "NEXT: BLACK HOLE  at " + (int) StellarForgeState.STAR_TO_HOLE + " M";
            case BLACK_HOLE -> "COMPLETE: the star has fallen in";
        };
        g.drawString(font, next, 8, y, C_MID, false);
        double eta = eta(machine, view);
        String right = view.stage == StellarForgeState.Stage.BLACK_HOLE || view.stage == StellarForgeState.Stage.IDLE ?
                "" : "ETA " + clock(eta);
        g.drawString(font, right, w - 8 - font.width(right), y, C_WHITE, false);
        int x = 8;
        int bw = w - 16;
        g.fill(x, y + 11, x + bw, y + 19, 0xFF_0A2430);
        double frac = view.stage == StellarForgeState.Stage.BLACK_HOLE ? 1.0 : view.massFraction();
        g.fill(x, y + 11, x + (int) (bw * frac), y + 19, accent);
        // how long it has held steady, as a thin line under the bar
        double hold = Math.min(1.0, view.holdTicks / (double) StellarForgeState.HOLD_TICKS_TO_ADVANCE);
        g.fill(x, y + 20, x + (int) (bw * hold), y + 21, C_GREEN);
        GuiDraw.drawBorder(g, x, y + 11, bw, 8, 0x66_00E5CC, 1);
        String pct = String.format(Locale.ROOT, "%.0f%%", frac * 100.0);
        g.drawString(font, pct, x + bw / 2 - font.width(pct) / 2, y + 11, C_WHITE, false);
    }

    private void sliders(GuiGraphics g, Font font, StellarForgeState view, int mouseX, int mouseY) {
        String[] names = { "FEED", "FIELD", "SPIN" };
        int[] values = { view.feed, view.field, view.spin };
        StellarForgeState.Band[] bands = { null, view.fieldBand(), view.spinBand() };
        for (int i = 0; i < 3; i++) {
            int y = SLIDER_Y[i];
            g.drawString(font, names[i], 8, y + 1, C_DIM, false);
            g.fill(SLIDER_X, y + 3, SLIDER_X + SLIDER_W, y + 8, 0xFF_0A2430);
            if (bands[i] != null) {
                g.fill(SLIDER_X + Math.round(SLIDER_W * bands[i].low() / 100.0f), y + 3,
                        SLIDER_X + Math.round(SLIDER_W * bands[i].high() / 100.0f), y + 8, 0x88_33FF88);
            }
            GuiDraw.drawBorder(g, SLIDER_X, y + 3, SLIDER_W, 5, 0x66_00E5CC, 1);
            int hx = SLIDER_X + Math.round(SLIDER_W * values[i] / 100.0f);
            boolean inside = bands[i] == null || bands[i].contains(values[i]);
            g.fill(hx - 2, y, hx + 2, y + 11, inside ? C_GREEN : C_GOLD);
            g.fill(hx - 1, y + 1, hx + 1, y + 10, 0xFF_DCF0F4);
            g.drawString(font, values[i] + "%", SLIDER_X + SLIDER_W + 8, y + 1, inside ? C_WHITE : C_GOLD, false);
            if (bands[i] != null) {
                String range = String.format(Locale.ROOT, "%.0f-%.0f", bands[i].low(), bands[i].high());
                g.drawString(font, range, SLIDER_X + SLIDER_W + 34, y + 1, inside ? C_GREEN : C_DIM, false);
            }
        }
    }

    private void button(GuiGraphics g, Font font, int[] rect, int x, int y, int w, int h, String text, int color,
                        boolean enabled, int mouseX, int mouseY) {
        boolean hover = enabled && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        g.fill(x, y, x + w, y + h, !enabled ? 0xFF_010810 : hover ? 0xFF_061825 : 0xFF_010A12);
        GuiDraw.drawBorder(g, x, y, w, h, enabled ? (hover ? color : GuiDraw.withAlpha(color, 0xAA)) : 0x44_3A5E6A, 1);
        g.drawString(font, text, x + (w - font.width(text)) / 2, y + (h - 8) / 2, enabled ? color : C_DIM, false);
        rect[0] = x;
        rect[1] = y;
        rect[2] = enabled ? w : 0;
        rect[3] = h;
    }

    private void buttons(GuiGraphics g, Font font, StellarForgeState view, int mouseX, int mouseY) {
        boolean star = view.stage == StellarForgeState.Stage.STAR;
        button(g, font, armRect, 8, 195, 110, 14, view.collapseArmed ? "COLLAPSE ARMED" : "ARM COLLAPSE",
                view.collapseArmed ? C_GOLD : C_PURPLE, star, mouseX, mouseY);
        button(g, font, disperseRect, 124, 195, 70, 14, "DISPERSE", C_RED, view.stage != StellarForgeState.Stage.IDLE,
                mouseX, mouseY);
    }

    /** What the forge is waiting for, as ticks and crosses, wrapping onto more lines as needed. */
    private void checklist(GuiGraphics g, Font font, int w, StellarForgeState view, int top) {
        var stage = view.stage;
        if (!machine.powerOk() && stage != StellarForgeState.Stage.IDLE) {
            g.drawString(font, "Out of power: the field slips.", 8, top, C_RED, false);
            return;
        }
        if (stage == StellarForgeState.Stage.IDLE) {
            g.drawString(font, "Hydrogen, helium or neutronium in, feed up.", 8, top, C_MID, false);
            return;
        }
        if (stage == StellarForgeState.Stage.BLACK_HOLE) {
            g.drawString(font, "A black hole. It gives plasma while it is steady.", 8, top, C_MID, false);
            return;
        }
        var fb = view.fieldBand();
        var sb = view.spinBand();
        boolean star = stage == StellarForgeState.Stage.STAR;
        java.util.List<Object[]> checks = new java.util.ArrayList<>();
        checks.add(new Object[] { String.format(Locale.ROOT, "mass %,.0f/%,.0f", view.mass, view.nextMass()),
                view.mass >= view.nextMass() });
        checks.add(new Object[] { String.format(Locale.ROOT, "stability %.0f/%.0f", view.stability,
                StellarForgeState.HOLD_STABILITY), view.stability >= StellarForgeState.HOLD_STABILITY });
        checks.add(new Object[] { "heat in band", view.heatBand().miss(view.heat, 25.0f) < 0.2f });
        checks.add(new Object[] { "field in band", view.field >= fb.low() && view.field <= fb.high() });
        checks.add(new Object[] { "spin in band", view.spin >= sb.low() && view.spin <= sb.high() });
        if (star) {
            checks.add(new Object[] { "collapse armed", view.collapseArmed });
            checks.add(new Object[] { String.format(Locale.ROOT, "field %d/70 to fall in", view.field),
                    view.field >= 70 });
        }
        int x = 8;
        int y = top;
        for (Object[] check : checks) {
            String text = ((boolean) check[1] ? "[x] " : "[ ] ") + check[0];
            int width = font.width(text) + 8;
            if (x + width > w - 8 && x > 8) {
                x = 8;
                y += 10;
            }
            g.drawString(font, text, x, y, (boolean) check[1] ? C_GREEN : C_DIM, false);
            x += width;
        }
        if (view.holdTicks > 0) {
            String hold = String.format(Locale.ROOT, "holding %d/%d", view.holdTicks,
                    StellarForgeState.HOLD_TICKS_TO_ADVANCE);
            g.drawString(font, hold, w - 8 - font.width(hold), top - 12, C_GREEN, false);
        }
    }

    private static boolean hit(int[] r, double mx, double my) {
        return r[2] > 0 && mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
    }

    private int sliderAt(double mx, double my) {
        for (int i = 0; i < 3; i++) {
            if (my >= SLIDER_Y[i] - 1 && my < SLIDER_Y[i] + 12 && mx >= SLIDER_X - 4 && mx <= SLIDER_X + SLIDER_W + 4) {
                return i;
            }
        }
        return -1;
    }

    private void setFromMouse(int which, double mx) {
        int value = (int) Math.round(Math.max(0.0, Math.min(1.0, (mx - SLIDER_X) / SLIDER_W)) * 100.0);
        int key = which * 1000 + value;
        if (key == lastSent) return;
        lastSent = key;
        controls.set(which, value);
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (button != 0) return false;
        if (hit(armRect, mx, my)) {
            controls.press(StellarControls.ARM);
            return true;
        }
        if (hit(disperseRect, mx, my)) {
            controls.press(StellarControls.DISPERSE);
            return true;
        }
        int which = sliderAt(mx, my);
        if (which >= 0) {
            dragging = which;
            lastSent = -1;
            setFromMouse(which, mx);
            return true;
        }
        return false;
    }

    @Override
    public void drag(double mx, double my, int button) {
        if (dragging >= 0) setFromMouse(dragging, mx);
    }

    @Override
    public void release(int button) {
        dragging = -1;
    }

    @Override
    public boolean scroll(double mx, double my, double amount) {
        int which = sliderAt(mx, my);
        if (which < 0) return false;
        int now = which == 0 ? machine.feedSetting() : which == 1 ? machine.fieldSetting() : machine.spinSetting();
        controls.set(which, (int) Math.max(0, Math.min(100, now + Math.signum(amount) * 5)));
        return true;
    }
}
