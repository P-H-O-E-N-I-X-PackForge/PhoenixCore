package net.phoenix.core.integration.growth;

import net.minecraft.world.item.Item;

import com.tterrag.registrate.util.entry.ItemEntry;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public final class GrowthItems {

    private GrowthItems() {}

    public static final ItemEntry<Item> GROWTH_SEED = REGISTRATE
            .item("growth_seed", Item::new)
            .lang("Growth Seed")
            .register();

    public static final ItemEntry<Item> CRYSTAL_SEED = REGISTRATE
            .item("crystal_seed", Item::new)
            .lang("Crystal Seed")
            .register();

    public static void init() {}
}
