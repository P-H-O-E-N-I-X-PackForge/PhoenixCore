package net.phoenix.core.client.fx;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class FxRender {

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

        public FxRender build() {
            FxRender render = new FxRender(id, title, Map.copyOf(params));
            FxRegistry.register(render);
            return render;
        }
    }

    public String id() {
        return id;
    }

    public String machineId() {
        return machineId;
    }

    public FxRender machine(String blockId) {
        this.machineId = blockId;
        return this;
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

    public float get(String name, float fallback) {
        Float v = overrides.get(name);
        return v == null ? fallback : v;
    }

    public Float override(String name) {
        return overrides.get(name);
    }

    public void set(String name, Float value) {
        Param p = params.get(name);
        if (p == null) throw new IllegalArgumentException("no setting " + name + " on " + id);
        if (value == null) overrides.remove(name);
        else overrides.put(name, Math.max(p.min(), Math.min(p.max(), value)));
    }

    public FxTheme theme() {
        return theme;
    }

    public void setTheme(FxTheme theme) {
        this.theme = theme;
    }

    public float[] colour() {
        return colour;
    }

    public void setColour(float[] colour) {
        this.colour = colour;
    }

    public FxClock clock() {
        clock.speed(get("speed", 1f));
        return clock;
    }

    public void reset() {
        overrides.clear();
        theme = null;
        colour = null;
        clock.speed(1f);
    }

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
