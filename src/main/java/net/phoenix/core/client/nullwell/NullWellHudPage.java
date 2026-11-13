package net.phoenix.core.client.nullwell;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.phoenix.core.common.machine.multiblock.nullwell.NullWellControls;
import net.phoenix.core.common.machine.multiblock.nullwell.NullWellMachine;
import net.phoenixvine.wiki.ui.hud.CanvasPainter;
import net.phoenixvine.wiki.ui.hud.GuiDraw;
import net.phoenixvine.wiki.ui.hud.HudTheme;

import java.util.Locale;

public final class NullWellHudPage implements CanvasPainter {

    private static final HudTheme T = HudTheme.ARCANE;
    private static final int C_DIM = 0xFF_6E5A93;
    private static final int C_MID = 0xFF_A992CF;
    private static final int C_WHITE = 0xFF_F0E8FF;
    private static final int C_GREEN = 0xFF_7CF0B0;
    private static final int C_GOLD = NullWellArt.AMBER;
    private static final int C_RED = NullWellArt.RED;
    private static final int C_CYAN = 0xFF_7FE6FF;

    private static final int CHART_X = 8;
    private static final int CHART_Y = 24;
    private static final int CHART = 148;

    private static final int SLIDER_X = 52;
    private static final int SLIDER_W = 140;
    private static final int[] SLIDER_Y = { 184, 200 };

    private final NullWellMachine machine;
    private final NullWellControls controls;
    private int dragging = -1;
    private int lastSent = -1;
    private final int[] ventRect = new int[4];

    public NullWellHudPage(NullWellMachine machine, NullWellControls controls) {
        this.machine = machine;
        this.controls = controls;
    }

    private static String power(long eu) {
        if (eu >= 1_000_000L) return String.format(Locale.ROOT, "%.2f MEU/t", eu / 1.0e6);
        if (eu >= 10_000L) return String.format(Locale.ROOT, "%.1f kEU/t", eu / 1.0e3);
        return String.format(Locale.ROOT, "%,d EU/t", eu);
    }

    @Override
    public void render(GuiGraphics g, int w, int h, int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;
        g.fill(0, 0, w, h, GuiDraw.withAlpha(T.bgBottom(), 0xF2));
        T.pattern(g, 0, 0, w, h);
        GuiDraw.drawBorder(g, 0, 0, w, h, T.line(), 1);

        g.drawString(font, "NULL WELL", 8, 7, T.accent(), false);
        if (!machine.isFormed()) {
            g.drawString(font, "Structure not formed.", 8, 24, C_DIM, false);
            return;
        }
        float stability = machine.stability();
        double mass = machine.mass();
        String status;
        int statusColor;
        if (!machine.lit()) {
            status = "DORMANT";
            statusColor = C_GOLD;
        } else if (!machine.powerOk()) {
            status = "NO POWER";
            statusColor = C_RED;
        } else if (mass > NullWellMachine.LIMIT) {
            status = "OVER LIMIT";
            statusColor = C_RED;
        } else if (stability < 40.0f) {
            status = "FAILING";
            statusColor = C_RED;
        } else if (mass > NullWellMachine.COMFORT) {
            status = "STRAINED";
            statusColor = C_GOLD;
        } else {
            status = "HELD";
            statusColor = C_GREEN;
        }
        int bw = font.width(status) + 10;
        g.fill(w - 8 - bw, 4, w - 8, 16, GuiDraw.withAlpha(statusColor, 0x30));
        GuiDraw.drawBorder(g, w - 8 - bw, 4, bw, 12, statusColor, 1);
        g.drawString(font, status, w - 8 - bw + 5, 6, statusColor, false);
        g.fill(8, 19, w - 8, 20, T.line());

        NullWellArt.draw(g, CHART_X, CHART_Y, CHART, (float) (mass / NullWellMachine.LIMIT),
                (float) (NullWellMachine.COMFORT / NullWellMachine.LIMIT), stability,
                machine.feedSetting() / 100.0f, machine.drawSetting() / 100.0f, true);
        g.drawString(font, "LIMIT", CHART_X + CHART - 4 - font.width("LIMIT"), CHART_Y + 4,
                GuiDraw.withAlpha(NullWellArt.RED, 0xC0), false);

        int cx = CHART_X + CHART + 8;
        int cw = w - 8 - cx;
        int y = CHART_Y;
        y = vital(g, font, cx, y, cw, "MASS", String.format(Locale.ROOT, "%,.1f M", mass),
                (float) (mass / NullWellMachine.LIMIT), C_MID, (float) (NullWellMachine.COMFORT /
                        NullWellMachine.LIMIT));
        y = vital(g, font, cx, y, cw, "FIELD", String.format(Locale.ROOT, "%.0f%%", stability), stability / 100.0f,
                NullWellArt.ringColor(stability), -1.0f);
        g.drawString(font, "CONTAIN", cx, y, C_DIM, false);
        g.drawString(font, font.plainSubstrByWidth(power(machine.energyUse()), cw), cx, y + 9,
                machine.powerOk() ? C_WHITE : C_RED, false);
        y += 22;
        g.drawString(font, "RADIATION", cx, y, C_DIM, false);
        g.drawString(font, font.plainSubstrByWidth(power(machine.emitting()), cw), cx, y + 9,
                machine.emitting() > 0 ? C_CYAN : C_DIM, false);

        sliders(g, font);
        boolean can = mass > 1.0;
        boolean hover = can && mouseX >= 200 && mouseX < 256 && mouseY >= 182 && mouseY < 214;
        g.fill(200, 182, 256, 214, hover ? GuiDraw.mix(T.bgTop(), C_RED, 0.25f) : T.bgTop());
        GuiDraw.drawBorder(g, 200, 182, 56, 32, can ? (hover ? C_RED : GuiDraw.withAlpha(C_RED, 0xAA)) :
                GuiDraw.withAlpha(C_DIM, 0x66), 1);
        g.drawCenteredString(font, "VENT", 228, 194, can ? C_RED : C_DIM);
        ventRect[0] = 200;
        ventRect[1] = 182;
        ventRect[2] = can ? 56 : 0;
        ventRect[3] = 32;

        String tip;
        if (machine.feeding() > 0) tip = String.format(Locale.ROOT, "taking %,d mB/t", machine.feeding());
        else if (machine.feedSetting() > 0) tip = "feed is on, but no fluid is reaching it";
        else tip = "Fluid in raises mass. Draw turns it into energy.";
        g.drawString(font, font.plainSubstrByWidth(tip, w - 16), 8, 222, C_MID, false);
        if (machine.breaches() > 0) {
            String b = "breaches: " + machine.breaches();
            g.drawString(font, b, w - 8 - font.width(b), 232, GuiDraw.withAlpha(C_RED, 0xA0), false);
        }
    }

    private int vital(GuiGraphics g, Font font, int x, int y, int w, String label, String value, float frac, int color,
                      float mark) {
        g.drawString(font, label, x, y, C_DIM, false);
        g.drawString(font, font.plainSubstrByWidth(value, w), x, y + 9, color, false);
        g.fill(x, y + 20, x + w, y + 25, GuiDraw.withAlpha(C_DIM, 0x80));
        g.fill(x, y + 20, x + (int) (w * Mth.clamp(frac, 0.0f, 1.0f)), y + 25, color);
        if (mark >= 0.0f) g.fill(x + (int) (w * mark), y + 18, x + (int) (w * mark) + 1, y + 27, C_GOLD);
        GuiDraw.drawBorder(g, x, y + 20, w, 5, GuiDraw.withAlpha(T.accent(), 0x55), 1);
        return y + 32;
    }

    private void sliders(GuiGraphics g, Font font) {
        String[] names = { "FEED", "DRAW" };
        int[] values = { machine.feedSetting(), machine.drawSetting() };
        for (int i = 0; i < 2; i++) {
            int y = SLIDER_Y[i];
            g.drawString(font, names[i], 8, y + 1, C_DIM, false);
            g.fill(SLIDER_X, y + 3, SLIDER_X + SLIDER_W, y + 8, 0xFF_160B26);
            g.fill(SLIDER_X, y + 3, SLIDER_X + Math.round(SLIDER_W * values[i] / 100.0f), y + 8,
                    GuiDraw.withAlpha(i == 0 ? NullWellArt.LILAC : C_CYAN, 0xAA));
            GuiDraw.drawBorder(g, SLIDER_X, y + 3, SLIDER_W, 5, GuiDraw.withAlpha(T.accent(), 0x66), 1);
            int hx = SLIDER_X + Math.round(SLIDER_W * values[i] / 100.0f);
            g.fill(hx - 2, y, hx + 2, y + 11, i == 0 ? NullWellArt.LILAC : C_CYAN);
            g.fill(hx - 1, y + 1, hx + 1, y + 10, 0xFF_F0E8FF);
            g.drawString(font, values[i] + "%", SLIDER_X + SLIDER_W + 4, y + 1, C_WHITE, false);
        }
    }

    private static boolean hit(int[] r, double mx, double my) {
        return r[2] > 0 && mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
    }

    private int sliderAt(double mx, double my) {
        for (int i = 0; i < 2; i++) {
            if (my >= SLIDER_Y[i] - 1 && my < SLIDER_Y[i] + 12 && mx >= SLIDER_X - 4 && mx <= SLIDER_X + SLIDER_W + 4) {
                return i;
            }
        }
        return -1;
    }

    private void setFromMouse(int which, double mx) {
        int value = (int) Math.round(Mth.clamp((mx - SLIDER_X) / SLIDER_W, 0.0, 1.0) * 100.0);
        int key = which * 1000 + value;
        if (key == lastSent) return;
        lastSent = key;
        controls.set(which == 0 ? NullWellControls.FEED : NullWellControls.DRAW, value);
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (button != 0) return false;
        if (hit(ventRect, mx, my)) {
            controls.press(NullWellControls.VENT);
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
        int now = which == 0 ? machine.feedSetting() : machine.drawSetting();
        controls.set(which == 0 ? NullWellControls.FEED : NullWellControls.DRAW,
                (int) Mth.clamp(now + Math.signum(amount) * 5, 0, 100));
        return true;
    }
}
