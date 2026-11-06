package net.phoenix.core.integration.drone;

import net.minecraft.resources.ResourceLocation;

import com.tterrag.registrate.util.entry.ItemEntry;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public class DroneItems {

    public static final ItemEntry<DroneWandItem> DRONE_WAND = REGISTRATE
            .item("drone_wand", DroneWandItem::new)
            .properties(p -> p.stacksTo(1))
            .model((ctx, prov) -> prov.handheld(ctx,
                    ResourceLocation.fromNamespaceAndPath("minecraft", "item/blaze_rod")))
            .lang("Drone Wand")
            .register();

    private static ItemEntry<DroneRangeModuleItem> module(int tier, int bonus, String texture) {
        return REGISTRATE.item("drone_range_module_" + tier, props -> new DroneRangeModuleItem(props, bonus))
                .properties(p -> p.stacksTo(16))
                .model((ctx, prov) -> prov.generated(ctx,
                        ResourceLocation.fromNamespaceAndPath("minecraft", "item/" + texture)))
                .lang("Drone Range Module Mk" + tier)
                .register();
    }

    public static final ItemEntry<DroneRangeModuleItem> RANGE_MODULE_1 = module(1, 16, "ender_eye");
    public static final ItemEntry<DroneRangeModuleItem> RANGE_MODULE_2 = module(2, 32, "ender_pearl");
    public static final ItemEntry<DroneRangeModuleItem> RANGE_MODULE_3 = module(3, 64, "echo_shard");
    public static final ItemEntry<DroneRangeModuleItem> RANGE_MODULE_4 = module(4, 128, "nether_star");

    public static void init() {}
}
