package net.phoenix.core.common.data.item;

import net.minecraft.world.item.Item;

import static net.phoenix.core.PhoenixCore.PHOENIX_CREATIVE_TAB;
import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public final class PftItems {

    static {
        REGISTRATE.creativeModeTab(() -> PHOENIX_CREATIVE_TAB);
    }

    private PftItems() {}

    private static void simple(String id, String name, String texture) {
        REGISTRATE.item(id, Item::new)
                .lang(name)
                .model((ctx, prov) -> prov.generated(ctx, prov.modLoc("item/" + texture)))
                .register();
    }

    public static void init() {}

    static {

        for (String kind : new String[] { "hostile", "passive", "neutral" }) {
            String colour = kind.equals("hostile") ? "\u00a7c" : kind.equals("passive") ? "\u00a7a" : "\u00a79";
            String title = Character.toUpperCase(kind.charAt(0)) + kind.substring(1);
            for (String stage : new String[] { "empty", "midway", "full" }) {
                String id = stage + "_" + kind + "_mob_essence_canister";
                simple(id, colour + Character.toUpperCase(stage.charAt(0)) + stage.substring(1) + " " + title +
                        " Mob Essence Canister", "componets/" + id);
            }
        }
        simple("fully_charged_mob_essence_capsule", "\u00a7cFully Charged Mob Essence Capsule",
                "componets/fully_charged_mob_essence_capsule");

        simple("peanut", "Peanut", "peanut");
        simple("roasted_peanut", "Roasted Peanut", "roasted_peanut");
        simple("water_chestnut", "Water Chestnut", "water_chestnut");

        simple("copper_vein_traces", "Copper Vein Traces", "veins/copper_traces");
        simple("sapphire_vein_traces", "Sapphire Vein Traces", "veins/sapphire_traces");
        simple("redstone_vein_traces", "Redstone Vein Traces", "veins/redstone_traces");
        simple("olivine_vein_traces", "Olivine Vein Traces", "veins/olivine_traces");
        simple("mica_vein_traces", "Mica Vein Traces", "veins/mica_traces");
        simple("manganese_vein_traces", "Manganese Vein Traces", "veins/manganese_traces");
        simple("lapis_vein_traces", "Lapis Vein Traces", "veins/lapis_traces");
        simple("diamond_vein_traces", "Diamond Vein Traces", "veins/diamond_traces");
        simple("oilsands_vein_traces", "Oilsands Traces", "veins/oilsands_traces");
        simple("salts_vein_traces", "Salt Traces", "veins/salts_traces");
        simple("nickel_vein_traces", "Nickel Vein Traces", "veins/nickel_traces");
        simple("mineral_sand_vein_traces", "Mineral Sands Vein Traces", "veins/mineral_sand_traces");
        simple("iron_vein_traces", "Iron Vein Traces", "veins/iron_traces");
        simple("magnetite_vein_traces", "Magnetite Vein Traces", "veins/magnetite_traces");
        simple("garnet_vein_traces", "Garnet Vein Traces", "veins/garnet_traces");
        simple("cassiterite_vein_traces", "Cassiterite Vein Traces", "veins/cassiterite_traces");
        simple("garnet_tin_vein_traces", "Garnet Tin Vein Traces", "veins/garnet_tin_traces");
        simple("galena_vein_traces", "Galena Vein Traces", "veins/galena_traces");
        simple("copper_tin_vein_traces", "Copper Tin Vein Traces", "veins/copper_tin_traces");
        simple("coal_vein_traces", "Coal Vein Traces", "veins/coal_traces");
        simple("apatite_vein_traces", "Apatite Vein Traces", "veins/apatite_traces");
        simple("topaz_vein_traces", "Topaz Vein Traces", "veins/topaz_traces");
        simple("tetrahedrite_vein_traces", "Tetrahedrite Vein Traces", "veins/tetrahedrite_traces");

        simple("pft_icon", "PFT Icon", "pft_icon");
        simple("phoenix_enriched_naquad_boule", "\u00a76Brilliant Boule",
                "phoenixware/mats/phoenix_enriched_naquad_boule");
    }
}
