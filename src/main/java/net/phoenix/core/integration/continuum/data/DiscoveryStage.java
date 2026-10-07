package net.phoenix.core.integration.continuum.data;

import java.util.Locale;

/**
 * How much of a system or body a team has uncovered. Milestone 2 keeps this on the client as demo state; from
 * milestone 3 it is per-team server state driven by Conflux research.
 */
public enum DiscoveryStage {

    /** A faint unlabelled point: something is there. */
    UNKNOWN,
    /** Named and typed, can be planned to, but only resolved as a ghost. */
    DETECTED,
    /** Fully revealed. */
    SURVEYED;

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
