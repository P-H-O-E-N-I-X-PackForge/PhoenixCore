package net.phoenix.core.common.item.cinder;

import net.minecraft.world.item.ItemStack;

/**
 * Design doc feature #7 - resolves an Atlas's installed {@link CinderAtlasUpgradeItem}s (stored via
 * {@link CinderAtlasData#getUpgradeSlot}) into the derived stats the rest of the system actually reads,
 * the same role {@code UpgradeEffects} plays for OmniPack elsewhere in this codebase: nothing else needs
 * to know upgrades exist as items at all, just call these.
 */
public final class CinderAtlasUpgrades {

    private CinderAtlasUpgrades() {}

    /** Un-upgraded wireless range, in blocks - deliberately generous rather than punitive, since this
     *  closes a previously wide-open "unlimited range" gap rather than clawing back something players
     *  were promised. */
    public static final double BASE_RANGE = 64.0;
    public static final double RANGE_TIER_1 = 128.0;
    public static final double RANGE_TIER_2 = 256.0;

    public static int getLoadoutCapacity(ItemStack atlas) {
        int capacity = CinderAtlasData.BASE_LOADOUT_COUNT;
        for (int i = 0; i < CinderAtlasData.UPGRADE_SLOT_COUNT; i++) {
            if (typeOf(CinderAtlasData.getUpgradeSlot(atlas, i)) == CinderAtlasUpgradeType.LOADOUT_EXPANSION) {
                capacity++;
            }
        }
        return Math.min(capacity, CinderAtlasData.MAX_LOADOUT_COUNT);
    }

    /** {@link Double#POSITIVE_INFINITY} for the top range tier - deliberately not special-cased by
     *  callers, since any finite distance compares {@code true} against it naturally. */
    public static double getWirelessRange(ItemStack atlas) {
        double range = BASE_RANGE;
        for (int i = 0; i < CinderAtlasData.UPGRADE_SLOT_COUNT; i++) {
            CinderAtlasUpgradeType type = typeOf(CinderAtlasData.getUpgradeSlot(atlas, i));
            if (type == null) continue;
            range = Math.max(range, switch (type) {
                case RANGE_EXTENDER_I -> RANGE_TIER_1;
                case RANGE_EXTENDER_II -> RANGE_TIER_2;
                case RANGE_EXTENDER_III -> Double.POSITIVE_INFINITY;
                case LOADOUT_EXPANSION -> BASE_RANGE;
            });
        }
        return range;
    }

    private static CinderAtlasUpgradeType typeOf(ItemStack upgrade) {
        return upgrade.getItem() instanceof CinderAtlasUpgradeItem item ? item.getUpgradeType() : null;
    }
}
