package net.phoenix.core.client.stellar.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.phoenix.core.client.gui.hud.CanvasPainter;
import net.phoenix.core.client.gui.hud.GuiDraw;
import net.phoenix.core.client.stellar.StellarForgeClient;
import net.phoenix.core.common.machine.multiblock.stellar.StellarForgeMachine;
import net.phoenix.core.common.machine.multiblock.stellar.StellarForgeState;

import java.util.Locale;

/**
 * Two graphs: how mass, heat and stability have gone over the last minutes, and the response curve: where stability
 * would settle at every value of the field (and of the spin) with the other settings held where they are, which is
 * the puzzle seen from above.
 */
public final class StellarGraphsPage implements CanvasPainter {

    private static final int C_MASS = 0xFF_44AAFF;
    private static final int C_HEAT = 0xFF_FF8833;
    private static final int C_STAB = 0xFF_33FF88;
    private static final int C_POWER = 0xFF_00E5CC;

    private final StellarForgeMachine machine;
    private int active = 0;
    private final int[][] tabRects = new int[2][4];

    public StellarGraphsPage(StellarForgeMachine machine) {
        this.machine = machine;
    }

    @Override
    public void render(GuiGraphics g, int w, int h, int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;
        g.fill(0, 0, w, h, 0xEE_010810);
        GuiDraw.drawBorder(g, 0, 0, w, h, 0x33_00E5CC, 1);
        int x = 8;
        int y = 8;
        int bw = w - 16;
        if (!machine.isFormed()) {
            g.drawString(font, "Structure not formed.", x, y, StellarHudPage.C_DIM, false);
            return;
        }

        String[] labels = { "HISTORY", "RESPONSE" };
        int gap = 3;
        int btnW = (bw - gap) / 2;
        for (int i = 0; i < 2; i++) {
            int bx = x + i * (btnW + gap);
            boolean on = active == i;
            boolean hover = mouseX >= bx && mouseX < bx + btnW && mouseY >= y && mouseY < y + 13;
            g.fill(bx, y, bx + btnW, y + 13, on ? 0xFF_002830 : hover ? 0xFF_061825 : 0xFF_010810);
            GuiDraw.drawBorder(g, bx, y, btnW, 13, on ? 0xFF_00AAA0 : hover ? 0xCC_00E5CC : 0x66_00E5CC, 1);
            g.drawString(font, labels[i], bx + (btnW - font.width(labels[i])) / 2, y + 3,
                    on ? StellarHudPage.C_CYAN : hover ? StellarHudPage.C_WHITE : StellarHudPage.C_DIM, false);
            tabRects[i][0] = bx;
            tabRects[i][1] = y;
            tabRects[i][2] = btnW;
            tabRects[i][3] = 13;
        }
        y += 19;
        if (active == 0) history(g, font, x, y, bw, h - y - 8);
        else response(g, font, x, y, bw, h - y - 8);
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (button != 0) return false;
        for (int i = 0; i < 2; i++) {
            int[] r = tabRects[i];
            if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                active = i;
                return true;
            }
        }
        return false;
    }

    private static void plot(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0x18_FFFFFF);
        GuiDraw.drawBorder(g, x, y, w, h, 0x44_00E5CC, 1);
        for (int i = 1; i <= 3; i++) {
            int gy = y + h - (int) (i * 0.25 * h);
            g.fill(x, gy, x + w, gy + 1, 0x18_FFFFFF);
        }
    }

    private static void curve(GuiGraphics g, int x, int y, int w, int h, float[] values, float max, int color) {
        if (values.length < 2) return;
        int px = -1;
        int py = -1;
        for (int i = 0; i < values.length; i++) {
            int cx = x + 1 + Math.round(i / (float) (values.length - 1) * (w - 3));
            float n = Math.max(0.0f, Math.min(1.0f, values[i] / max));
            int cy = y + h - 2 - Math.round(n * (h - 4));
            if (px >= 0) GuiDraw.drawLine(g, px, py, cx, cy, color);
            px = cx;
            py = cy;
        }
    }

    private void history(GuiGraphics g, Font font, int x, int y, int w, int h) {
        var hist = StellarForgeClient.animOf(machine).history;
        int plotH = Math.min(96, h - 52);
        int lx = x;
        String[] names = { "mass", "heat", "stability", "EU/t" };
        int[] colors = { C_MASS, C_HEAT, C_STAB, C_POWER };
        for (int i = 0; i < 4; i++) {
            g.fill(lx, y + 3, lx + 8, y + 5, colors[i]);
            g.drawString(font, names[i], lx + 10, y, colors[i], false);
            lx += 10 + font.width(names[i]) + 8;
        }
        y += 12;
        plot(g, x, y, w, plotH);
        if (hist.size() < 2) {
            g.drawCenteredString(font, "Gathering readings...", x + w / 2, y + plotH / 2 - 4, StellarHudPage.C_DIM);
            return;
        }
        float[][] all = hist.toArray(new float[0][]);
        float[] mass = new float[all.length];
        float[] heat = new float[all.length];
        float[] stab = new float[all.length];
        float[] power = new float[all.length];
        float maxMass = 1.0f;
        float maxPower = 1.0f;
        for (int i = 0; i < all.length; i++) {
            mass[i] = all[i][0];
            heat[i] = all[i][1];
            stab[i] = all[i][2];
            power[i] = all[i][3];
            maxMass = Math.max(maxMass, mass[i]);
            maxPower = Math.max(maxPower, power[i]);
        }
        curve(g, x, y, w, plotH, mass, maxMass * 1.1f, C_MASS);
        curve(g, x, y, w, plotH, power, maxPower * 1.1f, C_POWER);
        curve(g, x, y, w, plotH, heat, 100.0f, C_HEAT);
        curve(g, x, y, w, plotH, stab, 100.0f, C_STAB);
        // the 70% line stability has to stay above
        int hold = y + plotH - 2 - Math.round(0.7f * (plotH - 4));
        g.fill(x + 1, hold, x + w - 1, hold + 1, 0x55_33FF88);
        y += plotH + 4;
        int seconds = all.length;
        g.drawString(font, "-" + seconds + "s", x, y, StellarHudPage.C_DIM, false);
        g.drawString(font, "now", x + w - font.width("now"), y, StellarHudPage.C_DIM, false);
        y += 14;
        float last = all.length - 1;
        g.fill(x, y - 3, x + w, y - 2, 0x33_00E5CC);
        g.drawString(font, String.format(Locale.ROOT, "mass %,.1f M (peak %,.1f)", mass[all.length - 1], maxMass), x, y,
                C_MASS, false);
        g.drawString(font, String.format(Locale.ROOT, "heat %.0f%%   stability %.0f%%", heat[all.length - 1],
                stab[all.length - 1]), x, y + 10, C_STAB, false);
        g.drawString(font, String.format(Locale.ROOT, "power %,d EU/t (peak %,.0f)", (int) power[all.length - 1],
                maxPower), x, y + 20, C_POWER, false);
    }

    private void response(GuiGraphics g, Font font, int x, int y, int w, int h) {
        StellarForgeState s = machine.view();
        g.drawString(font, "where stability would settle", x, y, StellarHudPage.C_MID, false);
        y += 11;
        int plotH = Math.min(96, h - 70);
        int samples = Math.max(2, w - 2);
        plot(g, x, y, w, plotH);

        // the field's band, shaded
        var fb = s.fieldBand();
        var sb = s.spinBand();
        g.fill(x + 1 + Math.round((w - 2) * fb.low() / 100.0f), y + 1, x + 1 + Math.round((w - 2) * fb.high() / 100.0f),
                y + plotH - 1, 0x22_33FF88);

        float[] vsField = new float[samples];
        float[] vsSpin = new float[samples];
        for (int i = 0; i < samples; i++) {
            int v = Math.round(i / (float) (samples - 1) * 100.0f);
            vsField[i] = s.equilibriumStability(s.feed, v, s.spin);
            vsSpin[i] = s.equilibriumStability(s.feed, s.field, v);
        }
        curve(g, x, y, w, plotH, vsField, 100.0f, StellarHudPage.C_CYAN);
        curve(g, x, y, w, plotH, vsSpin, 100.0f, StellarHudPage.C_PURPLE);

        // where the settings are now
        int fx = x + 1 + Math.round((w - 3) * s.field / 100.0f);
        int sx = x + 1 + Math.round((w - 3) * s.spin / 100.0f);
        g.fill(fx, y + 1, fx + 1, y + plotH - 1, StellarHudPage.C_CYAN);
        g.fill(sx, y + 1, sx + 1, y + plotH - 1, StellarHudPage.C_PURPLE);
        int hold = y + plotH - 2 - Math.round(0.7f * (plotH - 4));
        g.fill(x + 1, hold, x + w - 1, hold + 1, 0x55_33FF88);

        y += plotH + 4;
        g.drawString(font, "0%", x, y, StellarHudPage.C_DIM, false);
        String mid = "setting";
        g.drawString(font, mid, x + w / 2 - font.width(mid) / 2, y, StellarHudPage.C_DIM, false);
        g.drawString(font, "100%", x + w - font.width("100%"), y, StellarHudPage.C_DIM, false);
        y += 14;

        g.fill(x, y - 3, x + w, y - 2, 0x33_00E5CC);
        g.fill(x, y + 3, x + 8, y + 5, StellarHudPage.C_CYAN);
        g.drawString(font, String.format(Locale.ROOT, "field: now %d%%, would settle at %.0f%% (band %.0f-%.0f)", s.field,
                s.equilibriumStability(s.feed, s.field, s.spin), fb.low(), fb.high()), x + 12, y, StellarHudPage.C_CYAN,
                false);
        g.fill(x, y + 13, x + 8, y + 15, StellarHudPage.C_PURPLE);
        g.drawString(font, String.format(Locale.ROOT, "spin:  now %d%%, band %.0f-%.0f", s.spin, sb.low(), sb.high()),
                x + 12, y + 10, StellarHudPage.C_PURPLE, false);
        g.drawString(font, "the peak of a curve is where that setting wants to be", x, y + 22,
                StellarHudPage.C_DIM, false);
    }
}
