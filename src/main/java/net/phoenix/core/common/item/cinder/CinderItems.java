package net.phoenix.core.common.item.cinder;

import com.tterrag.registrate.util.entry.ItemEntry;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public class CinderItems {

    public static final ItemEntry<CinderCoreItem> CINDER_CORE = REGISTRATE
            .item("cinder_core", CinderCoreItem::new)
            .lang("Rebirth Cinder Core")
            .properties(p -> p.stacksTo(1))
            .register();

    public static final ItemEntry<CinderAtlasItem> CINDER_ATLAS = REGISTRATE
            .item("cinder_atlas", CinderAtlasItem::new)
            .lang("Cinder Atlas")
            .properties(p -> p.stacksTo(1))
            .register();

    public static final ItemEntry<CinderAtlasUpgradeItem> CINDER_LOADOUT_EXPANSION = REGISTRATE
            .item("cinder_loadout_expansion", p -> new CinderAtlasUpgradeItem(p, CinderAtlasUpgradeType.LOADOUT_EXPANSION))
            .lang("Cinder Atlas Loadout Expansion")
            .register();

    public static final ItemEntry<CinderAtlasUpgradeItem> CINDER_RANGE_EXTENDER_I = REGISTRATE
            .item("cinder_range_extender_1", p -> new CinderAtlasUpgradeItem(p, CinderAtlasUpgradeType.RANGE_EXTENDER_I))
            .lang("Cinder Atlas Range Extender I")
            .register();

    public static final ItemEntry<CinderAtlasUpgradeItem> CINDER_RANGE_EXTENDER_II = REGISTRATE
            .item("cinder_range_extender_2", p -> new CinderAtlasUpgradeItem(p, CinderAtlasUpgradeType.RANGE_EXTENDER_II))
            .lang("Cinder Atlas Range Extender II")
            .register();

    public static final ItemEntry<CinderAtlasUpgradeItem> CINDER_RANGE_EXTENDER_III = REGISTRATE
            .item("cinder_range_extender_3", p -> new CinderAtlasUpgradeItem(p, CinderAtlasUpgradeType.RANGE_EXTENDER_III))
            .lang("Cinder Atlas Range Extender III")
            .register();

    public static void init() {}
}
