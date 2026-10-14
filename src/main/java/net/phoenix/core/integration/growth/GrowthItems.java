package net.phoenix.core.integration.growth;

import net.minecraft.world.item.Item;

import com.tterrag.registrate.util.entry.ItemEntry;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

/** The two seeds growing machines are fed: they pay for stages, and every harvest recipe uses one. */
public final class GrowthItems {

    private GrowthItems() {}

    /** Feeds the botanical machines (Verdant Bloom, Bloomvine Core). */
    public static final ItemEntry<Item> GROWTH_SEED = REGISTRATE
            .item("growth_seed", Item::new)
            .lang("Growth Seed")
            .register();

    /** Feeds the crystal machines (Crystal Garden). */
    public static final ItemEntry<Item> CRYSTAL_SEED = REGISTRATE
            .item("crystal_seed", Item::new)
            .lang("Crystal Seed")
            .register();

    public static void init() {}
}
