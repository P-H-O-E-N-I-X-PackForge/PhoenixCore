package net.phoenix.core.integration.continuum.common;

import net.minecraft.util.RandomSource;

import org.jetbrains.annotations.Nullable;

/**
 * Something that happens to a mission on the way. At most one is rolled per mission, at launch, and it stays hidden
 * until the mission lands (it is shown on the landing card and in the chat message). Bad events cost the player,
 * good ones pay out; on a failed mission only a solar flare still matters.
 */
public enum MissionEvent {

    SOLAR_FLARE("solar_flare", true, 3, "A solar flare scoured the hull: extra rocket wear."),
    MICROMETEOROIDS("micrometeoroids", true, 3, "Micrometeoroids holed the payload bay: one probe was lost."),
    TAILWIND("tailwind", false, 3, "A favourable tailwind cut the trip short."),
    DERELICT("derelict", false, 2, "The probes found a derelict cache: extra resources."),
    SIGNAL_ECHO("signal_echo", false, 2, "An echo off the target revealed another signal nearby."),

    // survey anomalies: found while surveying, never rolled with the ordinary events
    SURFACE_CACHE("surface_cache", false, 5, "Survey anomaly: a surface cache. The rocket brought some of it home.",
            true),
    VEIN_SIGNATURE("vein_signature", false, 4, "Survey anomaly: a vein signature mapped another deposit.", true),
    ANCIENT_RUINS("ancient_ruins", false, 1, "Survey anomaly: ancient ruins. The probes salvaged a large cache.", true);

    /** Extra wear from a solar flare. */
    public static final float SOLAR_FLARE_WEAR = 0.12f;
    /** Trip time multiplier with a tailwind. */
    public static final float TAILWIND_FACTOR = 0.85f;

    public final String id;
    public final boolean bad;
    public final int weight;
    public final String text;

    /** Anomalies are found by surveys, not rolled at launch. */
    public final boolean anomaly;

    MissionEvent(String id, boolean bad, int weight, String text) {
        this(id, bad, weight, text, false);
    }

    MissionEvent(String id, boolean bad, int weight, String text, boolean anomaly) {
        this.id = id;
        this.bad = bad;
        this.weight = weight;
        this.text = text;
        this.anomaly = anomaly;
    }

    public boolean appliesTo(Mission.Type type, int payload) {
        if (anomaly) return false;
        return switch (this) {
            case SOLAR_FLARE -> true;
            case MICROMETEOROIDS -> (type == Mission.Type.EXTRACT || type == Mission.Type.DEPLOY) && payload >= 2;
            case TAILWIND -> true;
            case DERELICT -> type == Mission.Type.EXTRACT || type == Mission.Type.HAUL;
            case SIGNAL_ECHO -> type == Mission.Type.SURVEY;
            default -> false;
        };
    }

    public static @Nullable MissionEvent byId(String id) {
        for (MissionEvent event : values()) {
            if (event.id.equals(id)) return event;
        }
        return null;
    }

    /** Picks which anomaly a survey turned up. */
    public static MissionEvent rollAnomaly(RandomSource random) {
        int total = 0;
        for (MissionEvent event : values()) {
            if (event.anomaly) total += event.weight;
        }
        int pick = random.nextInt(total);
        for (MissionEvent event : values()) {
            if (!event.anomaly) continue;
            pick -= event.weight;
            if (pick < 0) return event;
        }
        return SURFACE_CACHE;
    }

    /** Rolls whether this mission has an event and, if so, which. */
    public static @Nullable MissionEvent roll(Mission.Type type, int payload, RandomSource random, double chance) {
        if (chance <= 0.0 || random.nextDouble() >= chance) return null;

        int total = 0;
        for (MissionEvent event : values()) {
            if (event.appliesTo(type, payload)) total += event.weight;
        }
        if (total <= 0) return null;

        int pick = random.nextInt(total);
        for (MissionEvent event : values()) {
            if (!event.appliesTo(type, payload)) continue;
            pick -= event.weight;
            if (pick < 0) return event;
        }
        return null;
    }
}
