package net.phoenix.core.client.worldfx;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight per-shader draw timing for the discipline sky shaders (see DisciplineSkyEffects),
 * toggled by PhoenixKeybinds.SHOW_SHADER_PROFILER. Wraps each shader's draw call with
 * System.nanoTime() and keeps a smoothed (exponential moving average) cost per name, so "which
 * shader is actually expensive" has a real number instead of a guess - see
 * net.phoenix.core.client.renderer.ShaderProfilerOverlay for the HUD that displays this.
 *
 * When disabled, time() just runs the call directly with zero timing overhead - this is meant to
 * be left in the render path permanently rather than compiled out, so it has to cost nothing when
 * off.
 */
public final class ShaderProfiler {

    private ShaderProfiler() {}

    public static volatile boolean enabled = false;

    private static final Map<String, Double> AVERAGE_MS = new LinkedHashMap<>();
    private static final double SMOOTHING = 0.15;

    public static void time(String name, Runnable renderCall) {
        if (!enabled) {
            renderCall.run();
            return;
        }

        long start = System.nanoTime();
        renderCall.run();
        double ms = (System.nanoTime() - start) / 1_000_000.0;

        AVERAGE_MS.merge(name, ms, (oldMs, newMs) -> oldMs + (newMs - oldMs) * SMOOTHING);
    }

    /** A defensive copy - the overlay reads this every frame while this map keeps mutating. */
    public static Map<String, Double> snapshot() {
        return new LinkedHashMap<>(AVERAGE_MS);
    }

    public static void clear() {
        AVERAGE_MS.clear();
    }
}
