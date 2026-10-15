package net.phoenix.core.client.worldfx;

import java.util.LinkedHashMap;
import java.util.Map;

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

    public static Map<String, Double> snapshot() {
        return new LinkedHashMap<>(AVERAGE_MS);
    }

    public static void clear() {
        AVERAGE_MS.clear();
    }
}
