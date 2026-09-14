package net.phoenix.core.common.item.cinder;

import com.tterrag.registrate.util.entry.ItemEntry;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public class CinderItems {

    public static final ItemEntry<CinderCoreItem> CINDER_CORE = REGISTRATE
            .item("cinder_core", CinderCoreItem::new)
            .lang("Rebirth Cinder Core")
            .properties(p -> p.stacksTo(1))
            .register();

    public static void init() {}
}
