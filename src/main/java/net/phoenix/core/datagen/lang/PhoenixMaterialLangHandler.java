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
