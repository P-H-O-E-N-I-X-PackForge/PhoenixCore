package net.phoenix.core.client.fx;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One render's part in the system: what it is called, which numbers can be tuned on it, and the settings a player has
 * made with {@code /pftfx}. A render makes one of these once with {@link #builder}, and reads it as it draws:
 *
 * <pre>{@code
 * 
 * public static final FxRender FX = FxRender.builder("null_well", "Null Well")
 *         .param("speed", 0f, 10f, "how fast it animates")
 *         .param("size", 0.2f, 12f, "how big it is")
 *         .param("power", 0f, 1f, "how bright")
 *         .build();
 *
 * // while drawing: the setting if there is one, the render's own value if not
 * float size = FX.get("size", ownSize);
 * float t = FX.clock().now();            // seconds, running at the speed that was set
 * FxTheme theme = FX.theme();            // null unless one was chosen
 * float[] rgb = theme != null ? theme.colourAt(s, t) : normalColour;
 * }</pre>
 *
 * Settings are only on this client, and only for tuning: nothing here reaches the server.
 */
public final class FxRender {

    /** A number that can be set, between a smallest and a largest. */
    public record Param(String name, float min, float max, String description) {}

    private final String id;
    private final String title;
    private final Map<String, Param> params;
    private final Map<String, Float> overrides = new ConcurrentHashMap<>();
    private volatile FxTheme theme;
    private volatile float[] colour;
    private final FxClock clock = new FxClock();
    private volatile String machineId;

    private FxRender(String id, String title, Map<String, Param> params) {
        this.id = id;
        this.title = title;
        this.params = params;
    }

    public static Builder builder(String id, String title) {
        return new Builder(id, title);
    }

    public static final class Builder {

        private final String id;
        private final String title;
        private final Map<String, Param> params = new LinkedHashMap<>();

        private Builder(String id, String title) {
            this.id = id;
            this.title = title;
        }

        public Builder param(String name, float min, float max, String description) {
            params.put(name, new Param(name, min, max, description));
            return this;
        }

        /** Makes it and puts it in the system, so {@code /pftfx} can reach it. */
        public FxRender build() {
            FxRender render = new FxRender(id, title, Map.copyOf(params));
            FxRegistry.register(render);
            return render;
        }
    }

    public String id() {
        return id;
    }

    /**
     * The block that hosts this render, such as {@code phoenixcore:null_well}, or null if there is no single one.
     * {@code /pftfx <render> spawn} puts this block down.
     */
    public String machineId() {
        return machineId;
    }

    /** Says which block hosts this render, so {@code /pftfx <render> spawn} can put one down. Returns this. */
    public FxRender machine(String blockId) {
        this.machineId = blockId;
        return this;
    }

    private java.util.function.IntFunction<String> spawner;

    /**
     * Gives a render that has no machine of its own a way to put itself down for {@code /pftfx <render> spawn}: it is
     * handed the seconds and returns the message to show. Returns this.
     */
    public FxRender spawner(java.util.function.IntFunction<String> spawner) {
        this.spawner = spawner;
        return this;
    }

    public java.util.function.IntFunction<String> spawner() {
        return spawner;
    }

    public String title() {
        return title;
    }

    public List<Param> params() {
        return new ArrayList<>(params.values());
    }

    public Param param(String name) {
        return params.get(name);
    }

    /** The value that was set for a number, or {@code fallback} if none was. */
    public float get(String name, float fallback) {
        Float v = overrides.get(name);
        return v == null ? fallback : v;
    }

    /** The value that was set for a number, or null. */
    public Float override(String name) {
        return overrides.get(name);
    }

    /** Sets a number, kept between its smallest and largest; null puts it back to the render's own. */
    public void set(String name, Float value) {
        Param p = params.get(name);
        if (p == null) throw new IllegalArgumentException("no setting " + name + " on " + id);
        if (value == null) overrides.remove(name);
        else overrides.put(name, Math.max(p.min(), Math.min(p.max(), value)));
    }

    /** The theme that was chosen, or null. */
    public FxTheme theme() {
        return theme;
    }

    public void setTheme(FxTheme theme) {
        this.theme = theme;
    }

    /** The colour that was set, red, green and blue from 0 to 1, or null. */
    public float[] colour() {
        return colour;
    }

    public void setColour(float[] colour) {
        this.colour = colour;
    }

    /** Time at the speed that was set. */
    public FxClock clock() {
        clock.speed(get("speed", 1f));
        return clock;
    }

    /** Everything back to the render's own. */
    public void reset() {
        overrides.clear();
        theme = null;
        colour = null;
        clock.speed(1f);
    }

    /** What is set, for showing. */
    public String describe() {
        StringBuilder out = new StringBuilder();
        for (Param p : params.values()) {
            Float v = overrides.get(p.name());
            out.append(p.name()).append(' ').append(v == null ? "auto" : String.format("%.2f", v)).append(", ");
        }
        out.append("theme ").append(theme == null ? "auto" : theme.id());
        out.append(", colour ").append(colour == null ? "auto" : "set");
        return out.toString();
    }
}
