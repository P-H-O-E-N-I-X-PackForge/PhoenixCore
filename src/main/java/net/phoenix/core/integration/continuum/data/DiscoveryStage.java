package net.phoenix.core.integration.continuum.data;

import java.util.Locale;

public enum DiscoveryStage {

    UNKNOWN,

    DETECTED,

    SURVEYED;

    public String label() {
        String name = name();
        return name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
    }

    public DiscoveryStage next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public boolean atLeast(DiscoveryStage other) {
        return ordinal() >= other.ordinal();
    }

    public static DiscoveryStage parse(String value, DiscoveryStage fallback) {
        if (value == null) return fallback;
        try {
            return valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
