package net.phoenix.core.datagen.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;

public class PhoenixMaterialLangHandler {

    public static void init(RegistrateLangProvider provider) {
        provider.add("tagprefix.nanites", "%s Nanites");
        provider.add("tagprefix.crystal_rose", "%s Crystal Rose");
        provider.add("tagprefix.tier_one_bee", "%s Lively Bee");
        provider.add("tagprefix.tier_two_bee", "%s Energetic Bee");
        provider.add("tagprefix.tier_three_bee", "%s Stronk Bee");
        provider.add("tagprefix.honeycomb_block", "%s Rich Honey Comb (Block)");
        provider.add("tagprefix.honeycomb", "%s Rich Honey Comb");

        provider.add("tagprefix.pear_cut", "%s Pear Cut");
        provider.add("tagprefix.round_cut", "%s Round Cut");
        provider.add("tagprefix.cushion_cut", "%s Cushion Cut");
        provider.add("tagprefix.princess_cut", "%s Princess Cut");
        provider.add("tagprefix.emerald_cut", "%s Emerald Cut");
        provider.add("tagprefix.heart_cut", "%s Heart Cut");
        provider.add("tagprefix.marquise_cut", "%s Marquise Cut");
        provider.add("tagprefix.oval_cut", "%s Oval Cut");
    }

    private static String formatName(String name) {
        String[] parts = name.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    private static void addMaterialLang(RegistrateLangProvider provider, String id, String name) {
        provider.add("material.phoenixcore." + id, name);
    }
}
