package net.phoenix.core.integration.continuum.common;

import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.Random;
import java.util.UUID;

public final class Deposits {

    private Deposits() {}

    public static final float POOR = 0.6f;
    public static final float NORMAL = 1.0f;
    public static final float RICH = 1.45f;
    public static final float JACKPOT = 2.1f;

    public static float richness(UUID team, ResourceLocation body, ResourceLocation item) {
        long seed = team.getMostSignificantBits() * 31L + team.getLeastSignificantBits();
        seed = seed * 31L + body.toString().hashCode();
        seed = seed * 31L + item.toString().hashCode();
        double roll = new Random(seed ^ 0x5DEECE66DL).nextDouble();
        if (roll < 0.20) return POOR;
        if (roll < 0.65) return NORMAL;
        if (roll < 0.90) return RICH;
        return JACKPOT;
    }

    public static String label(float richness) {
        if (richness >= JACKPOT - 0.01f) return "jackpot";
        if (richness >= RICH - 0.01f) return "rich";
        if (richness >= NORMAL - 0.01f) return "average";
        return "poor";
    }

    public static String percent(float richness) {
        return String.format(Locale.ROOT, "x%.2f", richness);
    }
}
