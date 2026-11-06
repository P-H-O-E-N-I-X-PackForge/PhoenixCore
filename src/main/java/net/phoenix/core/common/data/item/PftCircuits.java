package net.phoenix.core.common.data.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import static net.phoenix.core.PhoenixCore.PHOENIX_CREATIVE_TAB;
import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public final class PftCircuits {

    static {
        REGISTRATE.creativeModeTab(() -> PHOENIX_CREATIVE_TAB);
    }

    private PftCircuits() {}

    public static void init() {}

    private static class GlowingItem extends Item {

        GlowingItem(Properties properties) {
            super(properties);
        }

        @Override
        public boolean isFoil(ItemStack stack) {
            return true;
        }
    }

    private static void circuit(String id, String name, String texture, String tier, boolean glow) {
        var builder = glow ? REGISTRATE.item(id, GlowingItem::new) : REGISTRATE.item(id, Item::new);
        builder.lang(name)
                .model((ctx, prov) -> prov.generated(ctx, prov.modLoc("item/" + texture)));
        if (tier != null) {
            builder.tag(TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath("gtceu", "circuits/" + tier)));
        }
        builder.register();
    }

    static {
        circuit("unstable_processing_unit", "§6Unstable Processing Unit - “Chaotic Computational Core ”",
                "phoenixware/circuits/unstable_processing_unit", "zpm", false);

        String[][] universal = {
                { "zpm", "zpm" }, { "uv", "uv" }, { "uhv", "uhv" }, { "uev", "uev" }, { "uiv", "uiv" }, { "iv", "iv" },
                { "uxv", "uev" }, { "max", "uev" }, { "luv", "luv" }, { "ev", "ev" }, { "hv", "hv" }, { "mv", "mv" },
                { "lv", "lv" },
        };
        for (String[] t : universal) {
            circuit(t[0] + "_universal_circuit", "§dUniversal " + t[0].toUpperCase() + " Circuit",
                    "universal_circuits/" + t[1] + "_universal_circuit", t[0], true);
        }
    }
}
