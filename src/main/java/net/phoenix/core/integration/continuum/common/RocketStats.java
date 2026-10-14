package net.phoenix.core.integration.continuum.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.continuum.data.ContinuumBody;
import net.phoenix.core.integration.continuum.data.DiscoveryStage;

public final class RocketStats {

    private RocketStats() {}

    public static final String HULL_INTERLOCK = "hull_interlock";
    public static final String OVERDRIVE = "overdrive";
    public static final String REINFORCED_HULL = "reinforced_hull";

    public static final String STELLAR_SHIELD = "stellar_shield";

    public static final String SINGULARITY_SHIELD = "singularity_shield";

    private static final String TAG_WEAR = "Wear";
    private static final String TAG_UPGRADES = "Upgrades";

    private static final float SAFE_WEAR = 0.35f;

    private static final float UNSURVEYED_RISK = 0.12f;

    public static final float FAILURE_WEAR = 0.30f;

    private static ContinuumConfig config() {
        var cfg = PhoenixConfigs.INSTANCE.continuum;
        return new ContinuumConfig(cfg.tripTimeMultiplier, cfg.maxTripReduction, cfg.interlockWearThreshold,
                cfg.wearMultiplier);
    }

    private record ContinuumConfig(double tripTimeMultiplier, double maxTripReduction, double interlockThreshold,
                                   double wearMultiplier) {}

    public static float wear(ItemStack rocket) {
        CompoundTag tag = rocket.getTag();
        return tag == null ? 0.0f : Math.max(0.0f, Math.min(1.0f, tag.getFloat(TAG_WEAR)));
    }

    public static void setWear(ItemStack rocket, float wear) {
        rocket.getOrCreateTag().putFloat(TAG_WEAR, Math.max(0.0f, Math.min(1.0f, wear)));
    }

    public static int level(ItemStack rocket, String upgrade) {
        CompoundTag tag = rocket.getTag();
        if (tag == null || !tag.contains(TAG_UPGRADES)) return 0;
        return tag.getCompound(TAG_UPGRADES).getInt(upgrade);
    }

    public static void setLevel(ItemStack rocket, String upgrade, int level) {
        CompoundTag upgrades = rocket.getOrCreateTag().getCompound(TAG_UPGRADES);
        if (level <= 0) upgrades.remove(upgrade);
        else upgrades.putInt(upgrade, level);
        rocket.getOrCreateTag().put(TAG_UPGRADES, upgrades);
    }

    public static long outpostCycleMillis() {
        return Math.max(1000L, Math.round(PhoenixConfigs.INSTANCE.continuum.outpostCycleMinutes * 60_000.0));
    }

    public static int outpostMaxReady() {
        return Math.max(1, PhoenixConfigs.INSTANCE.continuum.outpostMaxStoredCycles);
    }

    /** The best hull a rocket can be refitted to. */
    public static final int MAX_TIER = 4;

    private static final String TAG_TIER = "Tier";
    private static final String[] TIER_NAMES = { "Mk I", "Mk II", "Mk III", "Mk IV" };

    /** A rocket's hull tier, 1 to {@link #MAX_TIER}; every rocket starts at Mk I. */
    public static int tier(ItemStack rocket) {
        CompoundTag tag = rocket.getTag();
        int tier = tag == null ? 0 : tag.getInt(TAG_TIER);
        return Math.max(1, Math.min(MAX_TIER, tier == 0 ? 1 : tier));
    }

    public static void setTier(ItemStack rocket, int tier) {
        rocket.getOrCreateTag().putInt(TAG_TIER, Math.max(1, Math.min(MAX_TIER, tier)));
    }

    public static String tierName(int tier) {
        return TIER_NAMES[Math.max(1, Math.min(MAX_TIER, tier)) - 1];
    }

    /**
     * The highest level of an upgrade a hull of this tier can carry. A better hull has room for more of each, and the
     * shielding modules need a hull strong enough to carry them.
     */
    public static int upgradeCap(int tier, String upgrade) {
        return switch (upgrade) {
            case OVERDRIVE -> Math.min(5, tier + 1);
            case REINFORCED_HULL -> Math.min(3, tier);
            case HULL_INTERLOCK -> 1;
            case STELLAR_SHIELD -> tier >= 2 ? 1 : 0;
            case SINGULARITY_SHIELD -> tier >= 3 ? 1 : 0;
            default -> 0;
        };
    }

    /** The hull a rocket needs to work at a body: set by the body's tier. */
    public static int requiredTier(ContinuumBody body) {
        return switch (body.minTier().toLowerCase(java.util.Locale.ROOT)) {
            case "mid" -> 2;
            case "late" -> 3;
            case "endgame" -> 4;
            default -> 1;
        };
    }

    /** Share of the usual wear a trip costs with this hull (reinforced plating and the tier itself). */
    public static float wearFactor(ItemStack rocket) {
        double hull = 1.0 - Math.min(0.75, 0.25 * level(rocket, REINFORCED_HULL));
        return (float) Math.max(0.0, hull * (1.0 - 0.10 * (tier(rocket) - 1)));
    }

    /** Share of the usual failure risk this hull leaves. */
    public static float failureFactor(ItemStack rocket) {
        return 1.0f - 0.12f * (tier(rocket) - 1);
    }

    public static boolean isKnownUpgrade(String id) {
        return HULL_INTERLOCK.equals(id) || OVERDRIVE.equals(id) || REINFORCED_HULL.equals(id) ||
                STELLAR_SHIELD.equals(id) || SINGULARITY_SHIELD.equals(id);
    }

    public static @org.jetbrains.annotations.Nullable String requiredShield(ContinuumBody body) {
        return switch (body.type()) {
            case STAR -> STELLAR_SHIELD;
            case BLACK_HOLE -> SINGULARITY_SHIELD;
            default -> null;
        };
    }

    public static String shieldName(String upgrade) {
        return SINGULARITY_SHIELD.equals(upgrade) ? "Singularity Shielding Module" : "Stellar Shielding Module";
    }

    public static boolean shieldedFor(ItemStack rocket, ContinuumBody body) {
        String shield = requiredShield(body);
        return shield == null || level(rocket, shield) > 0;
    }

    public static boolean interlockRefuses(ItemStack rocket) {
        return level(rocket, HULL_INTERLOCK) > 0 && wear(rocket) >= config().interlockThreshold();
    }

    public static float tripReduction(ItemStack rocket) {
        return (float) Math.min(config().maxTripReduction(),
                0.10 * level(rocket, OVERDRIVE) + 0.05 * (tier(rocket) - 1));
    }

    public static long tripMillis(ContinuumBody destination, ItemStack rocket) {
        double minutes = destination.tripMinutes() * (1.0 - tripReduction(rocket)) * config().tripTimeMultiplier();
        return Math.max(1000L, Math.round(minutes * 60_000.0));
    }

    public static float wearCost(ContinuumBody destination, ItemStack rocket) {
        double base = 0.05 + 0.04 * (destination.tripMinutes() / 60.0);
        return (float) Math.max(0.0, base * wearFactor(rocket) * config().wearMultiplier());
    }

    public static float failureChance(ItemStack rocket, DiscoveryStage destinationStage) {
        float chance = Math.max(0.0f, wear(rocket) - SAFE_WEAR) * 1.1f;
        if (destinationStage != DiscoveryStage.SURVEYED) chance += UNSURVEYED_RISK;
        return Math.max(0.0f, Math.min(0.9f, chance * failureFactor(rocket)));
    }
}
