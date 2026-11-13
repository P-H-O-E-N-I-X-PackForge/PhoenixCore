package net.phoenix.core.client.nullwell;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.phoenix.core.common.machine.multiblock.nullwell.NullWellMachine;
import net.phoenixvine.wiki.ui.hud.CanvasPainter;
import net.phoenixvine.wiki.ui.hud.GuiDraw;
import net.phoenixvine.wiki.ui.hud.HudTheme;

import java.util.ArrayDeque;
import java.util.Locale;

public final class NullWellHistoryPage implements CanvasPainter {

    private static final HudTheme T = HudTheme.ARCANE;
    private static final int SAMPLES = 120;
    private static final long EVERY_MS = 1000L;

    private final NullWellMachine machine;
    private final ArrayDeque<float[]> history = new ArrayDeque<>();
    private long lastSample;

    public NullWellHistoryPage(NullWellMachine machine) {
        this.machine = machine;
    }

    private void sample() {
        long now = System.currentTimeMillis();
        if (now - lastSample < EVERY_MS) return;
        lastSample = now;
        history.addLast(new float[] { (float) machine.mass(), machine.stability(), machine.emitting() });
        while (history.size() > SAMPLES) history.removeFirst();
    }

    @Override
    public void render(GuiGraphics g, int w, int h, int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;
        g.fill(0, 0, w, h, GuiDraw.withAlpha(T.bgBottom(), 0xF2));
        T.pattern(g, 0, 0, w, h);
        GuiDraw.drawBorder(g, 0, 0, w, h, T.line(), 1);
        g.drawString(font, "HISTORY", 8, 7, T.accent(), false);
        g.fill(8, 19, w - 8, 20, T.line());
        if (!machine.isFormed()) {
            g.drawString(font, "Structure not formed.", 8, 24, T.dim(), false);
            return;
        }
        sample();

        int x = 12;
        int y = 36;
        int cw = w - 24;
        int ch = 80;
        chart(g, font, "MASS", x, y, cw, ch, 0, (float) NullWellMachine.LIMIT * 1.2f, NullWellArt.LILAC,
                (float) NullWellMachine.LIMIT, (float) NullWellMachine.COMFORT);
        chart(g, font, "FIELD", x, y + ch + 22, cw, 50, 1, 100.0f, NullWellArt.AMBER, -1.0f, -1.0f);
        String note = history.isEmpty() ? "collecting..." : String.format(Locale.ROOT, "%d s", history.size());
        g.drawString(font, note, w - 8 - font.width(note), 7, GuiDraw.withAlpha(T.accent2(), 0xB0), false);
    }

    private void chart(GuiGraphics g, Font font, String title, int x, int y, int w, int h, int channel, float max,
                       int color, float limit, float comfort) {
        g.drawString(font, title, x, y - 11, T.dim(), false);
        g.fill(x, y, x + w, y + h, NullWellArt.INK);
        GuiDraw.drawBorder(g, x, y, w, h, GuiDraw.withAlpha(NullWellArt.GRID, 0xFF), 1);
        for (int i = 1; i < 4; i++) g.fill(x + 1, y + h * i / 4, x + w - 1, y + h * i / 4 + 1,
                GuiDraw.withAlpha(NullWellArt.GRID, 0x60));
        if (limit > 0) line(g, x, y, w, h, limit / max, GuiDraw.withAlpha(NullWellArt.RED, 0xB0));
        if (comfort > 0) line(g, x, y, w, h, comfort / max, GuiDraw.withAlpha(NullWellArt.AMBER, 0x70));
        int n = history.size();
        if (n < 2) return;
        int i = 0;
        float[] prev = null;
        for (float[] s : history) {
            if (prev != null) {
                int x0 = x + 1 + (int) ((w - 2) * (i - 1) / (float) (SAMPLES - 1));
                int x1 = x + 1 + (int) ((w - 2) * i / (float) (SAMPLES - 1));
                int y0 = y + h - 1 - (int) ((h - 2) * Mth.clamp(prev[channel] / max, 0.0f, 1.0f));
                int y1 = y + h - 1 - (int) ((h - 2) * Mth.clamp(s[channel] / max, 0.0f, 1.0f));
                segment(g, x0, y0, x1, y1, color);
            }
            prev = s;
            i++;
        }
        float[] last = history.peekLast();
        String value = channel == 0 ? String.format(Locale.ROOT, "%,.1f M", last[0]) :
                String.format(Locale.ROOT, "%.0f%%", last[1]);
        g.drawString(font, value, x + w - 4 - font.width(value), y + 3, color, false);
    }

    private static void line(GuiGraphics g, int x, int y, int w, int h, float frac, int argb) {
        int ly = y + h - 1 - (int) ((h - 2) * Mth.clamp(frac, 0.0f, 1.0f));
        for (int px = x + 1; px < x + w - 1; px += 4) g.fill(px, ly, px + 2, ly + 1, argb);
    }

    private static void segment(GuiGraphics g, int x0, int y0, int x1, int y1, int argb) {
        int steps = Math.max(1, Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)));
        for (int i = 0; i <= steps; i++) {
            int px = x0 + (x1 - x0) * i / steps;
            int py = y0 + (y1 - y0) * i / steps;
            g.fill(px, py, px + 1, py + 1, argb);
        }
    }
}
