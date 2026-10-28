package net.phoenix.core.common.machine.multiblock.stellar;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.phoenix.core.api.gui.widget.ActionButtons;

import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.api.drawable.Text;
import brachy.modularui.screen.RichTooltip;
import brachy.modularui.screen.viewport.GuiContext;
import brachy.modularui.theme.WidgetTheme;
import brachy.modularui.value.sync.DoubleSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widget.Widget;
import brachy.modularui.widgets.SliderWidget;
import brachy.modularui.widgets.TextWidget;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * The Stellar Forge's screen. It builds up as the forge does: the ladder of stages lights one rung at a time, the gauges
 * show where mass, heat and stability stand, and each of the three settings has its band drawn on its slider, so the
 * puzzle is keeping the markers inside the green while the bands move with the mass.
 */
final class StellarForgeUi {

    private static final int BG_TOP = 0xFF0a0716;
    private static final int BG_BOTTOM = 0xFF1b1030;
    private static final int TITLE = 0xFFFFE0A8;
    private static final int LABEL = 0xFFA88FD9;
    private static final int GOOD = 0xFF7affb0;
    private static final int WARN = 0xFFffd27a;
    private static final int BAD = 0xFFff6b5c;
    private static final int DIM = 0xFF6a6488;

    private StellarForgeUi() {}

    private record Background() implements IDrawable {

        @Override
        public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme theme) {
            GuiGraphics g = context.getGraphics();
            g.fillGradient(x, y, x + width, y + height, BG_TOP, BG_BOTTOM);
            g.renderOutline(x, y, width, height, 0xFF3a2f7a);
        }
    }

    /** A bar filled to a fraction, with an optional band marked on it and a tick for a threshold. */
    private record Bar(Supplier<Float> fraction, int color, Supplier<StellarForgeState.Band> band,
                       float threshold) implements IDrawable {

        @Override
        public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme theme) {
            GuiGraphics g = context.getGraphics();
            g.fill(x, y, x + width, y + height, 0xFF14122a);
            var b = band == null ? null : band.get();
            if (b != null) {
                g.fill(x + Math.round(width * b.low() / 100.0f), y, x + Math.round(width * b.high() / 100.0f),
                        y + height, 0x5538ff9a);
            }
            int filled = Math.round(width * Math.max(0.0f, Math.min(1.0f, fraction.get())));
            if (filled > 0) g.fill(x, y + 2, x + filled, y + height - 2, color | 0xFF000000);
            if (threshold > 0.0f) {
                int tx = x + Math.round(width * threshold);
                g.fill(tx, y - 1, tx + 1, y + height + 1, 0xFFffffff);
            }
            g.renderOutline(x, y, width, height, 0xFF2a2548);
        }
    }

    /** The track of a slider, with the band the setting should sit in marked green. */
    private record Track(Supplier<StellarForgeState.Band> band) implements IDrawable {

        @Override
        public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme theme) {
            GuiGraphics g = context.getGraphics();
            int mid = y + height / 2;
            g.fill(x, mid - 3, x + width, mid + 3, 0xFF14122a);
            var b = band.get();
            g.fill(x + Math.round(width * b.low() / 100.0f), mid - 3, x + Math.round(width * b.high() / 100.0f),
                    mid + 3, 0xAA38ff9a);
            g.renderOutline(x, mid - 3, width, 6, 0xFF2a2548);
        }
    }

    private static TextWidget<?> text(Supplier<Component> text, int x, int y) {
        TextWidget<?> widget = new TextWidget<>(Text.dynamic(text));
        widget.pos(x, y);
        return widget;
    }

    private static Component line(String label, String value, int color) {
        return Component.literal(label + " ").withStyle(s -> s.withColor(LABEL & 0xFFFFFF))
                .append(Component.literal(value).withStyle(s -> s.withColor(color & 0xFFFFFF)));
    }

    static void build(StellarForgeMachine m, ParentWidget<?> main, PanelSyncManager sync) {
        main.size(340, 236);
        main.background(new Background());

        main.child(Text.str("STELLAR FORGE").color(TITLE).asWidget().pos(8, 6).size(160, 10));
        main.child(text(() -> m.isFormed() ? Component.literal(m.stage().label.toUpperCase(Locale.ROOT)).withStyle(
                s -> s.withColor(stageColor(m.stage()) & 0xFFFFFF)) : Component.literal("NOT FORMED").withStyle(
                        s -> s.withColor(BAD & 0xFFFFFF)), 250, 6));

        // the ladder of stages
        String[] rungs = { "GAS CLOUD", "PROTOSTAR", "STAR", "BLACK HOLE" };
        for (int i = 0; i < rungs.length; i++) {
            final int rung = i + 1;
            final String label = rungs[i];
            main.child(text(() -> {
                int now = m.stage().ordinal();
                int color = now == rung ? GOOD : now > rung ? WARN : DIM;
                String mark = now == rung ? "> " : now > rung ? "+ " : "  ";
                return Component.literal(mark + label).withStyle(s -> s.withColor(color & 0xFFFFFF));
            }, 8 + i * 82, 22));
        }

        // gauges
        gauge(main, "MASS", 40, () -> (float) m.massFraction(), 0x6ab0ff, null, 0.0f,
                () -> String.format(Locale.ROOT, "%,.1f M  (next: %s)", m.mass(), nextText(m)));
        gauge(main, "HEAT", 62, () -> m.heat() / 100.0f, 0xff9a3a, () -> m.view().heatBand(), 0.0f,
                () -> String.format(Locale.ROOT, "%.0f%%", m.heat()));
        gauge(main, "STABILITY", 84, () -> m.stability() / 100.0f, 0x7affb0, null, 0.7f,
                () -> String.format(Locale.ROOT, "%.0f%%  (hold above 70)", m.stability()));

        // the three settings
        control(main, sync, "feed", "FEED", 112, m::feedSetting, m::setFeed, null);
        control(main, sync, "field", "CONTAINMENT FIELD", 142, m::fieldSetting, m::setField,
                () -> m.view().fieldBand());
        control(main, sync, "spin", "SPIN", 172, m::spinSetting, m::setSpin, () -> m.view().spinBand());

        // actions
        main.child(ActionButtons.simple(sync, "stellarArm", m::armCollapse,
                Text.str("ARM COLLAPSE").color(WARN), new RichTooltip().addLine(Text.of(Component.literal(
                        "Once a star has enough mass, the field is high and it burns hot, it falls in as a " +
                                "black hole")))).pos(8, 208).size(100, 16));
        main.child(ActionButtons.simple(sync, "stellarDisperse", m::disperse,
                Text.str("DISPERSE").color(BAD), new RichTooltip().addLine(Text.of(Component.literal(
                        "Let it all scatter and start again")))).pos(116, 208).size(80, 16));
        main.child(text(() -> {
            if (!m.isFormed()) return Component.empty();
            String power = m.powerOk() ? "ok" : "short!";
            return line("Power", String.format(Locale.ROOT, "%,d EU/t (%s)   Plasma %.1f mB/t", m.energyUse(), power,
                    m.plasmaPerTick()), m.powerOk() ? GOOD : BAD);
        }, 8, 192));
        main.child(text(() -> hint(m), 204, 212));
    }

    private static int stageColor(StellarForgeState.Stage stage) {
        return switch (stage) {
            case IDLE -> DIM;
            case GAS_CLOUD -> 0xFF6ab0ff;
            case PROTOSTAR -> 0xFFff9a3a;
            case STAR -> 0xFFffe0a8;
            case BLACK_HOLE -> 0xFFb08cff;
        };
    }

    private static String nextText(StellarForgeMachine m) {
        var s = m.view();
        return switch (s.stage) {
            case IDLE -> "feed it matter";
            case GAS_CLOUD -> String.format(Locale.ROOT, "%.0f M protostar", StellarForgeState.CLOUD_TO_PROTOSTAR);
            case PROTOSTAR -> String.format(Locale.ROOT, "%.0f M star", StellarForgeState.PROTOSTAR_TO_STAR);
            case STAR -> String.format(Locale.ROOT, "%.0f M collapse", StellarForgeState.STAR_TO_HOLE);
            case BLACK_HOLE -> "complete";
        };
    }

    private static Component hint(StellarForgeMachine m) {
        var s = m.view();
        String text;
        int color = LABEL;
        if (!m.isFormed()) {
            text = "";
        } else if (s.stage == StellarForgeState.Stage.IDLE) {
            text = "Hydrogen, helium or neutronium in, feed up.";
        } else if (!m.powerOk()) {
            text = "Out of power: the field slips.";
            color = BAD;
        } else if (s.stability < 40.0f) {
            text = "Unstable: bring the markers into the green.";
            color = BAD;
        } else if (m.armed()) {
            text = "Collapse armed.";
            color = WARN;
        } else {
            text = "Hold the markers in the green.";
        }
        final int c = color;
        return Component.literal(text).withStyle(st -> st.withColor(c & 0xFFFFFF));
    }

    private static void gauge(ParentWidget<?> main, String label, int y, Supplier<Float> fraction, int color,
                              Supplier<StellarForgeState.Band> band, float threshold, Supplier<String> value) {
        main.child(Text.str(label).color(LABEL).asWidget().pos(8, y).size(70, 10));
        Widget<?> bar = new Widget<>().pos(80, y - 1).size(130, 10).background(new Bar(fraction, color, band,
                threshold));
        main.child(bar);
        main.child(text(() -> Component.literal(value.get()).withStyle(s -> s.withColor(0xB8B0D8)), 216, y));
    }

    private static void control(ParentWidget<?> main, PanelSyncManager sync, String key, String label, int y,
                                java.util.function.IntSupplier get, java.util.function.IntConsumer set,
                                Supplier<StellarForgeState.Band> band) {
        DoubleSyncValue value = new DoubleSyncValue(() -> get.getAsInt(), v -> set.accept((int) Math.round(v)));
        sync.syncValue(key, 0, value);
        main.child(Text.str(label).color(LABEL).asWidget().pos(8, y).size(120, 10));
        SliderWidget slider = new SliderWidget().bounds(0, 100).value(value).pos(8, y + 10).size(240, 12);
        if (band != null) slider.background(new Track(band));
        main.child(slider);
        main.child(text(() -> Component.literal(get.getAsInt() + "%").withStyle(s -> s.withColor(0xFFFFFF)), 256,
                y + 12));
        if (band != null) {
            main.child(text(() -> {
                var b = band.get();
                boolean in = b.contains(get.getAsInt());
                return Component.literal(String.format(Locale.ROOT, "%.0f-%.0f", b.low(), b.high()))
                        .withStyle(s -> s.withColor((in ? GOOD : WARN) & 0xFFFFFF));
            }, 284, y + 12));
        }
    }
}
