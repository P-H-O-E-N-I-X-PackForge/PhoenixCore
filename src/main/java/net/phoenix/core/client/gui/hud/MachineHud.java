package net.phoenix.core.client.gui.hud;

import com.gregtechceu.gtceu.api.machine.mui.MachineUIPanelBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.PanelSyncManager;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Turns a {@link HudModel} into a machine screen in the HUD style: a tab column with a progress page (readouts, bars,
 * controls), a math page with the formulas and numbers, and a graph page of everything the model asked to be graphed.
 */
public final class MachineHud {

    private MachineHud() {}

    public static final int WIDTH = 264;
    public static final int HEIGHT = 230;

    /** What a control on the screen does on the server. */
    @FunctionalInterface
    public interface Actions {

        void run(int id, int value);
    }

    private static final Map<Object, ArrayDeque<float[]>> HISTORY = new WeakHashMap<>();
    private static final int HISTORY_SECONDS = 300;

    /**
     * Build the panel. Call it from {@code buildUI}; the actions are registered on both sides, the pages only on the
     * client. {@code model} is asked for only on the client.
     */
    public static ModularPanel<?> build(com.gregtechceu.gtceu.api.machine.MetaMachine machine, PosGuiData data,
                                        PanelSyncManager sync, UISettings settings, Actions actions,
                                        java.util.function.Supplier<HudModel> model) {
        sync.registerServerSyncedAction("hud_control", buf -> {
            int id = buf.readVarInt();
            int value = buf.readVarInt();
            actions.run(id, value);
        });
        TabStrip screen = null;
        if (machine.getLevel() != null && machine.getLevel().isClientSide) {
            HudModel m = model.get();
            Actions sender = (id, value) -> sync.callSyncedAction("hud_control", buf -> {
                buf.writeVarInt(id);
                buf.writeVarInt(value);
            });
            ArrayDeque<float[]> history = HISTORY.computeIfAbsent(machine, k -> new ArrayDeque<>());
            Sampler sampler = new Sampler(m, history);
            var tabs = new java.util.ArrayList<TabStrip.Tab>();
            tabs.add(new TabStrip.Tab("H", new Sampled(sampler, new ProgressPage(m, sender))));
            if (m.hasMath) tabs.add(new TabStrip.Tab("M", new Sampled(sampler, new MathPage(m))));
            if (!m.series.isEmpty()) tabs.add(new TabStrip.Tab("~", new Sampled(sampler, new GraphPage(m, history))));
            screen = new TabStrip(tabs);
        }
        final TabStrip tabs = screen;
        var panel = MachineUIPanelBuilder.panelBuilder(machine).attachInventory(false).addTitleBar(false)
                .drawGTLogo(false)
                .mainContents(main -> {
                    if (tabs != null) main.child(new CanvasWidget(tabs).size(WIDTH, HEIGHT));
                    else main.size(WIDTH, HEIGHT);
                }).build(sync, settings);
        if (tabs != null) HudPanel.apply(panel, tabs);
        return panel;
    }

    // ------------------------------------------------------------------------------------------------ sampling

    private static final class Sampler {

        private final HudModel model;
        private final ArrayDeque<float[]> history;
        private long last;

        Sampler(HudModel model, ArrayDeque<float[]> history) {
            this.model = model;
            this.history = history;
        }

        void tick() {
            long now = System.currentTimeMillis();
            if (now - last < 1000L || model.series.isEmpty()) return;
            last = now;
            float[] sample = new float[model.series.size()];
            for (int i = 0; i < sample.length; i++) {
                Float v = model.series.get(i).value().get();
                sample[i] = v == null ? 0.0f : v;
            }
            history.addLast(sample);
            while (history.size() > HISTORY_SECONDS) history.removeFirst();
        }
    }

    private record Sampled(Sampler sampler, CanvasPainter page) implements CanvasPainter {

        @Override
        public void render(GuiGraphics g, int w, int h, int mouseX, int mouseY) {
            sampler.tick();
            page.render(g, w, h, mouseX, mouseY);
        }

        @Override
        public boolean click(double mx, double my, int button) {
            return page.click(mx, my, button);
        }

        @Override
        public void drag(double mx, double my, int button) {
            page.drag(mx, my, button);
        }

        @Override
        public void release(int button) {
            page.release(button);
        }

        @Override
        public boolean scroll(double mx, double my, double amount) {
            return page.scroll(mx, my, amount);
        }
    }

    // ------------------------------------------------------------------------------------------------ progress

    private static final class ProgressPage implements CanvasPainter {

        private final HudModel m;
        private final Actions send;
        private int dragging = -1;
        private int lastSent = Integer.MIN_VALUE;
        private int[][] sliderRects = new int[0][];
        private int[][] buttonRects = new int[0][];

        ProgressPage(HudModel m, Actions send) {
            this.m = m;
            this.send = send;
        }

        @Override
        public void render(GuiGraphics g, int w, int h, int mouseX, int mouseY) {
            Font font = Minecraft.getInstance().font;
            g.fill(0, 0, w, h, 0xEE_010810);
            GuiDraw.drawBorder(g, 0, 0, w, h, 0x33_00E5CC, 1);
            g.drawString(font, m.title.toUpperCase(Locale.ROOT), 8, 7, HudModel.CYAN, false);
            if (!m.formed.get()) {
                g.drawString(font, "Structure not formed.", 8, 24, HudModel.DIM, false);
                return;
            }
            var badge = m.badge.get();
            if (!badge.text().isEmpty()) {
                int bw = font.width(badge.text()) + 10;
                g.fill(w - 8 - bw, 4, w - 8, 16, GuiDraw.withAlpha(badge.color(), 0x30));
                GuiDraw.drawBorder(g, w - 8 - bw, 4, bw, 12, badge.color(), 1);
                g.drawString(font, badge.text(), w - 8 - bw + 5, 6, badge.color(), false);
            }
            g.fill(8, 19, w - 8, 20, 0x33_00E5CC);
            int y = 24;
            String sub = m.subtitle.get();
            if (!sub.isEmpty()) {
                g.drawString(font, sub, 8, y, HudModel.MID, false);
                y += 12;
            }

            List<HudModel.Cell> cells = m.cells.get();
            int cw = (w - 16 - 8) / 3;
            for (int i = 0; i < cells.size(); i++) {
                int cx = 8 + (i % 3) * (cw + 4);
                int cy = y + (i / 3) * 32;
                var c = cells.get(i);
                g.fill(cx, cy, cx + cw, cy + 30, 0xAA_010A12);
                GuiDraw.drawBorder(g, cx, cy, cw, 30, 0x55_00E5CC, 1);
                g.fill(cx, cy, cx + 2, cy + 30, c.color());
                g.drawString(font, font.plainSubstrByWidth(c.label(), cw - 8), cx + 6, cy + 3, HudModel.DIM, false);
                g.drawString(font, font.plainSubstrByWidth(c.value(), cw - 8), cx + 6, cy + 13, c.color(), false);
                g.drawString(font, font.plainSubstrByWidth(c.sub(), cw - 8), cx + 6, cy + 22, HudModel.MID, false);
            }
            y += ((cells.size() + 2) / 3) * 32 + 2;

            for (var bar : m.bars.get()) {
                g.drawString(font, font.plainSubstrByWidth(bar.label(), w - 100), 8, y, HudModel.MID, false);
                g.drawString(font, bar.right(), w - 8 - font.width(bar.right()), y, HudModel.WHITE, false);
                int bw = w - 16;
                g.fill(8, y + 10, 8 + bw, y + 17, 0xFF_0A2430);
                g.fill(8, y + 10, 8 + (int) (bw * Mth.clamp(bar.fraction(), 0.0f, 1.0f)), y + 17, bar.color());
                GuiDraw.drawBorder(g, 8, y + 10, bw, 7, 0x66_00E5CC, 1);
                y += 21;
            }

            // controls: sliders, then buttons side by side
            int sliderCount = 0;
            int buttonCount = 0;
            for (var c : m.controls) {
                if (c instanceof HudModel.Slider) sliderCount++;
                else buttonCount++;
            }
            sliderRects = new int[sliderCount][];
            buttonRects = new int[buttonCount][];
            int si = 0;
            int bi = 0;
            int bx = 8;
            for (var c : m.controls) {
                if (c instanceof HudModel.Slider s) {
                    sliderRects[si] = slider(g, font, s, y, mouseX, mouseY);
                    si++;
                    y += 14;
                }
            }
            for (var c : m.controls) {
                if (c instanceof HudModel.Button b) {
                    String text = b.text().get();
                    int bw = font.width(text) + 16;
                    if (bx + bw > w - 8) {
                        bx = 8;
                        y += 17;
                    }
                    boolean enabled = b.enabled().getAsBoolean();
                    boolean hover = enabled && mouseX >= bx && mouseX < bx + bw && mouseY >= y && mouseY < y + 14;
                    g.fill(bx, y, bx + bw, y + 14, !enabled ? 0xFF_010810 : hover ? 0xFF_061825 : 0xFF_010A12);
                    GuiDraw.drawBorder(g, bx, y, bw, 14, enabled ? (hover ? b.color() : GuiDraw.withAlpha(b.color(),
                            0xAA)) : 0x44_3A5E6A, 1);
                    g.drawString(font, text, bx + 8, y + 3, enabled ? b.color() : HudModel.DIM, false);
                    buttonRects[bi] = new int[] { bx, y, enabled ? bw : 0, 14, b.id() };
                    bi++;
                    bx += bw + 6;
                }
            }
            if (buttonCount > 0) y += 17;

            var notes = m.notes.get();
            int ny = h - 6 - notes.size() * 10;
            for (var note : notes) {
                g.drawString(font, font.plainSubstrByWidth(note.text(), w - 16), 8, ny, note.color(), false);
                ny += 10;
            }
        }

        private int[] slider(GuiGraphics g, Font font, HudModel.Slider s, int y, int mouseX, int mouseY) {
            int x0 = 64;
            int sw = 130;
            int value = s.value().getAsInt();
            float frac = (value - s.min()) / (float) Math.max(1, s.max() - s.min());
            g.drawString(font, s.label(), 8, y + 1, HudModel.DIM, false);
            g.fill(x0, y + 3, x0 + sw, y + 8, 0xFF_0A2430);
            HudModel.Band band = s.band() == null ? null : s.band().get();
            if (band != null) {
                float lo = (band.low() - s.min()) / (float) Math.max(1, s.max() - s.min());
                float hi = (band.high() - s.min()) / (float) Math.max(1, s.max() - s.min());
                g.fill(x0 + Math.round(sw * Mth.clamp(lo, 0, 1)), y + 3, x0 + Math.round(sw * Mth.clamp(hi, 0, 1)),
                        y + 8, 0x88_33FF88);
            }
            GuiDraw.drawBorder(g, x0, y + 3, sw, 5, 0x66_00E5CC, 1);
            int hx = x0 + Math.round(sw * Mth.clamp(frac, 0.0f, 1.0f));
            boolean inside = band == null || band.contains(value);
            g.fill(hx - 2, y, hx + 2, y + 11, inside ? HudModel.GREEN : HudModel.GOLD);
            g.fill(hx - 1, y + 1, hx + 1, y + 10, 0xFF_DCF0F4);
            g.drawString(font, value + s.unit(), x0 + sw + 8, y + 1, inside ? HudModel.WHITE : HudModel.GOLD, false);
            if (band != null) {
                g.drawString(font, String.format(Locale.ROOT, "%.0f-%.0f", band.low(), band.high()), x0 + sw + 40, y + 1,
                        inside ? HudModel.GREEN : HudModel.DIM, false);
            }
            return new int[] { x0 - 4, y - 1, sw + 8, 13, s.id(), s.min(), s.max() };
        }

        private int sliderAt(double mx, double my) {
            for (int i = 0; i < sliderRects.length; i++) {
                int[] r = sliderRects[i];
                if (r != null && mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) return i;
            }
            return -1;
        }

        private void setFromMouse(int index, double mx) {
            int[] r = sliderRects[index];
            double t = Mth.clamp((mx - (r[0] + 4)) / (r[2] - 8), 0.0, 1.0);
            int value = r[5] + (int) Math.round(t * (r[6] - r[5]));
            if (value == lastSent) return;
            lastSent = value;
            send.run(r[4], value);
        }

        @Override
        public boolean click(double mx, double my, int button) {
            if (button != 0) return false;
            for (int[] r : buttonRects) {
                if (r != null && r[2] > 0 && mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                    send.run(r[4], 0);
                    return true;
                }
            }
            int s = sliderAt(mx, my);
            if (s >= 0) {
                dragging = s;
                lastSent = Integer.MIN_VALUE;
                setFromMouse(s, mx);
                return true;
            }
            return false;
        }

        @Override
        public void drag(double mx, double my, int button) {
            if (dragging >= 0 && dragging < sliderRects.length) setFromMouse(dragging, mx);
        }

        @Override
        public void release(int button) {
            dragging = -1;
        }

        @Override
        public boolean scroll(double mx, double my, double amount) {
            int s = sliderAt(mx, my);
            if (s < 0) return false;
            int[] r = sliderRects[s];
            for (var c : m.controls) {
                if (c instanceof HudModel.Slider slider && slider.id() == r[4]) {
                    int step = Math.max(1, (slider.max() - slider.min()) / 20);
                    int next = Mth.clamp(slider.value().getAsInt() + (int) Math.signum(amount) * step, slider.min(),
                            slider.max());
                    send.run(slider.id(), next);
                    return true;
                }
            }
            return false;
        }
    }

    // ------------------------------------------------------------------------------------------------ math

    private static final class MathPage implements CanvasPainter {

        private final HudModel m;
        private int scroll;
        private int contentHeight = 300;

        MathPage(HudModel m) {
            this.m = m;
        }

        @Override
        public void render(GuiGraphics g, int w, int h, int mouseX, int mouseY) {
            Font font = Minecraft.getInstance().font;
            g.fill(0, 0, w, h, 0xEE_010810);
            GuiDraw.drawBorder(g, 0, 0, w, h, 0x33_00E5CC, 1);
            int maxScroll = Math.max(0, contentHeight - h);
            scroll = Mth.clamp(scroll, 0, maxScroll);
            GuiDraw.enableScissor(g, 0, 0, w, h);
            g.pose().pushPose();
            g.pose().translate(0, -scroll, 0);
            int y = 6;
            if (!m.formed.get()) {
                g.drawString(font, "Structure not formed.", 8, y, HudModel.DIM, false);
            } else {
                for (var line : m.math.get()) {
                    if (line.text().startsWith("# ")) {
                        y += 3;
                        g.fill(8, y, w - 18, y + 1, 0x33_00E5CC);
                        g.fill(8, y + 3, 11, y + 11, 0xFF_00AAA0);
                        g.drawString(font, line.text().substring(2), 15, y + 3, HudModel.CYAN, false);
                        y += 15;
                    } else {
                        g.drawString(font, font.plainSubstrByWidth(line.text(), w - 24), 8, y, line.color(), false);
                        y += 10;
                    }
                }
            }
            contentHeight = y + 8;
            g.pose().popPose();
            if (maxScroll > 0) {
                int thumbH = Math.max(12, (int) (h * (h / (double) contentHeight)));
                int thumbY = (int) ((h - thumbH) * (scroll / (double) maxScroll));
                g.fill(w - 4, 0, w, h, 0x33_00E5CC);
                g.fill(w - 4, thumbY, w, thumbY + thumbH, 0xAA_00E5CC);
            }
            g.disableScissor();
        }

        @Override
        public boolean scroll(double mx, double my, double amount) {
            scroll -= (int) (amount * 12);
            return true;
        }
    }

    // ------------------------------------------------------------------------------------------------ graphs

    private static final class GraphPage implements CanvasPainter {

        private final HudModel m;
        private final ArrayDeque<float[]> history;

        GraphPage(HudModel m, ArrayDeque<float[]> history) {
            this.m = m;
            this.history = history;
        }

        @Override
        public void render(GuiGraphics g, int w, int h, int mouseX, int mouseY) {
            Font font = Minecraft.getInstance().font;
            g.fill(0, 0, w, h, 0xEE_010810);
            GuiDraw.drawBorder(g, 0, 0, w, h, 0x33_00E5CC, 1);
            int x = 8;
            int y = 8;
            int bw = w - 16;
            if (!m.formed.get()) {
                g.drawString(font, "Structure not formed.", x, y, HudModel.DIM, false);
                return;
            }
            int lx = x;
            for (var s : m.series) {
                g.fill(lx, y + 3, lx + 8, y + 5, s.color());
                g.drawString(font, s.name(), lx + 10, y, s.color(), false);
                lx += 10 + font.width(s.name()) + 8;
                if (lx > w - 40) {
                    lx = x;
                    y += 10;
                }
            }
            y += 12;
            int plotH = Math.min(110, h - y - 14 - m.series.size() * 10 - 12);
            g.fill(x, y, x + bw, y + plotH, 0x18_FFFFFF);
            GuiDraw.drawBorder(g, x, y, bw, plotH, 0x44_00E5CC, 1);
            for (int i = 1; i <= 3; i++) {
                int gy = y + plotH - (int) (i * 0.25 * plotH);
                g.fill(x, gy, x + bw, gy + 1, 0x18_FFFFFF);
            }
            if (history.size() < 2) {
                g.drawCenteredString(font, "Gathering readings...", x + bw / 2, y + plotH / 2 - 4, HudModel.DIM);
                return;
            }
            float[][] all = history.toArray(new float[0][]);
            for (int s = 0; s < m.series.size(); s++) {
                var series = m.series.get(s);
                float max = series.max();
                if (max <= 0.0f) {
                    max = 1.0f;
                    for (float[] row : all) max = Math.max(max, row[s]);
                    max *= 1.1f;
                }
                int px = -1;
                int py = -1;
                for (int i = 0; i < all.length; i++) {
                    int cx = x + 1 + Math.round(i / (float) (all.length - 1) * (bw - 3));
                    float n = Mth.clamp(all[i][s] / max, 0.0f, 1.0f);
                    int cy = y + plotH - 2 - Math.round(n * (plotH - 4));
                    if (px >= 0) GuiDraw.drawLine(g, px, py, cx, cy, series.color());
                    px = cx;
                    py = cy;
                }
            }
            y += plotH + 4;
            g.drawString(font, "-" + all.length + "s", x, y, HudModel.DIM, false);
            g.drawString(font, "now", x + bw - font.width("now"), y, HudModel.DIM, false);
            y += 14;
            g.fill(x, y - 3, x + bw, y - 2, 0x33_00E5CC);
            for (int s = 0; s < m.series.size(); s++) {
                var series = m.series.get(s);
                float latest = all[all.length - 1][s];
                float peak = 0.0f;
                for (float[] row : all) peak = Math.max(peak, row[s]);
                g.drawString(font, String.format(Locale.ROOT, "%s  %,.1f   (peak %,.1f)", series.name(), latest, peak), x, y,
                        series.color(), false);
                y += 10;
            }
        }
    }
}
