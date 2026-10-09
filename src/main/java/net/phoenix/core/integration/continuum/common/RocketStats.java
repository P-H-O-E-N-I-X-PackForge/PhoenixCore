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
        return (float) Math.min(config().maxTripReduction(), 0.10 * level(rocket, OVERDRIVE));
    }

    public static long tripMillis(ContinuumBody destination, ItemStack rocket) {
        double minutes = destination.tripMinutes() * (1.0 - tripReduction(rocket)) * config().tripTimeMultiplier();
        return Math.max(1000L, Math.round(minutes * 60_000.0));
    }

    public static float wearCost(ContinuumBody destination, ItemStack rocket) {
        double base = 0.05 + 0.04 * (destination.tripMinutes() / 60.0);
        double hull = 1.0 - Math.min(0.75, 0.25 * level(rocket, REINFORCED_HULL));
        return (float) Math.max(0.0, base * hull * config().wearMultiplier());
    }

    public static float failureChance(ItemStack rocket, DiscoveryStage destinationStage) {
        float chance = Math.max(0.0f, wear(rocket) - SAFE_WEAR) * 1.1f;
        if (destinationStage != DiscoveryStage.SURVEYED) chance += UNSURVEYED_RISK;
        return Math.max(0.0f, Math.min(0.9f, chance));
    }
}
