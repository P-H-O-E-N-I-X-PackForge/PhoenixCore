package net.phoenix.core.common.item.cinder;

import com.tterrag.registrate.util.entry.ItemEntry;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

/**
 * V1: a single Cinder Core item to prove out the configure -> validate -> place -> build pipeline.
 * Tiered variants (Ash-Bound Crucible / Pyretic Core / Omega Phoenix Matrix) are a content/balancing
 * concern layered on top once the mechanism itself works - see the Rebirth Cinder Core design notes.
 */
public class CinderItems {

    public static final ItemEntry<CinderCoreItem> CINDER_CORE = REGISTRATE
            .item("cinder_core", CinderCoreItem::new)
            .lang("Rebirth Cinder Core")
            .properties(p -> p.stacksTo(1))
            .register();

    public static void init() {}
}
