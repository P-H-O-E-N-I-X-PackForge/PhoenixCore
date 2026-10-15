package net.phoenix.core.integration.continuum.common;

public final class Stations {

    private Stations() {}

    public static final int MAX_LEVEL = 3;

    public static float tripFactor(int level) {
        return 1.0f - 0.10f * clamp(level);
    }

    public static float wearFactor(int level) {
        return 1.0f - 0.15f * clamp(level);
    }

    public static float yieldFactor(int level) {
        return 1.0f + 0.15f * clamp(level);
    }

    public static boolean preventsStranding(int level) {
        return level >= MAX_LEVEL;
    }

    public static String describe(int level) {
        String text = "Trips there 10% faster, rocket wear 15% lower, hauls 15% richer";
        if (level >= MAX_LEVEL) text += ", and a failed run can no longer strand the rocket";
        return text + ". Also adds a mission slot.";
    }

    private static int clamp(int level) {
        return Math.max(0, Math.min(MAX_LEVEL, level));
    }
}
