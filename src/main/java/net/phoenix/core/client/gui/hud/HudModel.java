package net.phoenix.core.client.gui.hud;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * What a machine shows on its HUD screen, described once with suppliers that are read every frame: a header and badge,
 * readouts, progress bars, controls, notes, the formulas it uses and the values to graph. {@link MachineHud} turns it
 * into the tabbed pages (progress, math, graphs) so each machine only says what its numbers are.
 */
public final class HudModel {

    public static final int CYAN = 0xFF_00E5CC;
    public static final int TEAL = 0xFF_00AAA0;
    public static final int DIM = 0xFF_3A5E6A;
    public static final int MID = 0xFF_6A9BAA;
    public static final int WHITE = 0xFF_DCF0F4;
    public static final int GOLD = 0xFF_FFC44D;
    public static final int ORANGE = 0xFF_FF8833;
    public static final int RED = 0xFF_FF3333;
    public static final int GREEN = 0xFF_33FF88;
    public static final int BLUE = 0xFF_44AAFF;
    public static final int PURPLE = 0xFF_B08CFF;

    public record Badge(String text, int color) {}

    public record Cell(String label, String value, String sub, int color) {}

    public record Bar(String label, float fraction, String right, int color) {}

    public record Line(String text, int color) {}

    /** A range a setting should sit in, in the slider's own units. */
    public record Band(float low, float high) {

        public boolean contains(float v) {
            return v >= low && v <= high;
        }
    }

    public sealed interface Control permits Slider, Button {}

    /** A slider: click, drag or scroll. {@code id} is what the server is told. */
    public record Slider(String label, int id, IntSupplier value, int min, int max, Supplier<Band> band,
                         String unit) implements Control {}

    /** A button: it tells the server {@code id}, or when {@code local} is set runs that on this client instead. */
    public record Button(String label, int id, int color, BooleanSupplier enabled, Supplier<String> text,
                         Runnable local) implements Control {}

    /** A value to graph: sampled once a second while the screen is open. */
    public record Series(String name, int color, Supplier<Float> value, float max) {}

    String title = "";
    Supplier<Badge> badge = () -> new Badge("", DIM);
    Supplier<String> subtitle = () -> "";
    Supplier<List<Cell>> cells = List::of;
    Supplier<List<Bar>> bars = List::of;
    final List<Control> controls = new ArrayList<>();
    Supplier<List<Line>> notes = List::of;
    Supplier<List<Line>> math = List::of;
    final List<Series> series = new ArrayList<>();
    Supplier<Boolean> formed = () -> true;
    boolean hasMath;
    HudTheme theme = HudTheme.TEAL;
    Heroes.Hero hero;
    int heroHeight = 36;

    public static HudModel of(String title) {
        HudModel m = new HudModel();
        m.title = title;
        return m;
    }

    /** How this machine's screen looks: colours, background pattern and tab layout. */
    public HudModel theme(HudTheme theme) {
        this.theme = theme;
        return this;
    }

    /** A moving picture across the top of the progress page, drawn when there is room for it. */
    public HudModel hero(Heroes.Hero hero, int height) {
        this.hero = hero;
        this.heroHeight = height;
        return this;
    }

    public HudModel badge(Supplier<Badge> badge) {
        this.badge = badge;
        return this;
    }

    public HudModel subtitle(Supplier<String> subtitle) {
        this.subtitle = subtitle;
        return this;
    }

    public HudModel formed(Supplier<Boolean> formed) {
        this.formed = formed;
        return this;
    }

    public HudModel cells(Supplier<List<Cell>> cells) {
        this.cells = cells;
        return this;
    }

    public HudModel bars(Supplier<List<Bar>> bars) {
        this.bars = bars;
        return this;
    }

    public HudModel slider(String label, int id, IntSupplier value, int min, int max, Supplier<Band> band,
                           String unit) {
        controls.add(new Slider(label, id, value, min, max, band, unit));
        return this;
    }

    public HudModel button(String label, int id, int color, BooleanSupplier enabled) {
        controls.add(new Button(label, id, color, enabled, () -> label, null));
        return this;
    }

    public HudModel button(Supplier<String> text, int id, int color, BooleanSupplier enabled) {
        controls.add(new Button(text.get(), id, color, enabled, text, null));
        return this;
    }

    /** A button that does something on this client only, such as opening a screen. */
    public HudModel localButton(String label, int color, BooleanSupplier enabled, Runnable action) {
        controls.add(new Button(label, -1, color, enabled, () -> label, action));
        return this;
    }

    public HudModel notes(Supplier<List<Line>> notes) {
        this.notes = notes;
        return this;
    }

    public HudModel math(Supplier<List<Line>> math) {
        this.math = math;
        this.hasMath = true;
        return this;
    }

    public HudModel graph(String name, int color, Supplier<Float> value, float max) {
        series.add(new Series(name, color, value, max));
        return this;
    }

    public static Cell cell(String label, String value, String sub, int color) {
        return new Cell(label, value, sub, color);
    }

    public static Line line(String text, int color) {
        return new Line(text, color);
    }

    /** A heading line in the math page. */
    public static Line heading(String text) {
        return new Line("# " + text, CYAN);
    }
}
