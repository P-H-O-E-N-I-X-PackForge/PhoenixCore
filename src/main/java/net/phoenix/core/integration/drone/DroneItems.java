package net.phoenix.core.integration.drone;

import net.minecraft.resources.ResourceLocation;

import com.tterrag.registrate.util.entry.ItemEntry;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public class DroneItems {

    public static final ItemEntry<DroneWandItem> DRONE_WAND = REGISTRATE
            .item("drone_wand", DroneWandItem::new)
            .properties(p -> p.stacksTo(1))
            .model((ctx, prov) -> prov.handheld(ctx, ResourceLocation.fromNamespaceAndPath("minecraft", "item/blaze_rod")))
            .lang("Drone Wand")
            .register();

    public static void init() {}
}
