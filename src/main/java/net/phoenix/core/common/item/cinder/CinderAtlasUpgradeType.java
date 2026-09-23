package net.phoenix.core.common.item.cinder;

/**
 * Design doc feature #7 - the axes an installed {@link CinderAtlasUpgradeItem} can affect, resolved by
 * {@link CinderAtlasUpgrades}. Mirrors the shape of the existing OmniPack upgrade system
 * ({@code net.phoenix.core.integration.gregpacks.common.item.UpgradeType}) - a flat enum of concrete
 * upgrade kinds, one real item per kind, rather than a single parameterized "upgrade" item.
 * <p>
 * {@code LOADOUT_EXPANSION} stacks (installing several adds capacity cumulatively, capped at
 * {@link CinderAtlasData#MAX_LOADOUT_COUNT}); the {@code RANGE_EXTENDER} tiers don't (installing more
 * than one just uses whichever tier is highest - see {@link CinderAtlasUpgrades#getWirelessRange}).
 */
public enum CinderAtlasUpgradeType {

    LOADOUT_EXPANSION,
    RANGE_EXTENDER_I,
    RANGE_EXTENDER_II,
    RANGE_EXTENDER_III
}
