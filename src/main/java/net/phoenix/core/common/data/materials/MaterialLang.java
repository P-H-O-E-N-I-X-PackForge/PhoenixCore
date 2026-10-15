package net.phoenix.core.common.data.materials;

import net.phoenix.core.PhoenixCore;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Material display names. GTCEu 1.21 has no name setter on the material builder, so they ride on lang entries.
 */
public final class MaterialLang {

    private static final Map<String, String> NAMES = new LinkedHashMap<>();

    private MaterialLang() {}

    public static void put(String id, String name) {
        NAMES.put(id, name);
    }

    public static void register() {
        put("astral_filament", "§dAstral Filament");
        put("skein", "§5Skein");
        put("ensorcelled_weave", "§dEnsorcelled Weave");
        put("honey", "§6Honey");
        put("superconductive_honey", "§6Superconductive Honey");
        put("honey_catalyst", "§6Honey Catalyst");
        put("honey_comb_base_mixture", "Honeycomb Base Mixture");
        put("pollen_concentrate_fluid", "§6Pollen Concentrate Fluid");
        put("aurum_wood", "§6Aurum Wood");
        put("boron_carbide", "§5Boron Carbide");
        put("niobium_modified_silicon_carbide", "§5Niobium Modified Silicon Carbide");
        put("cryo_graphite_binding_solution", "Cryo-Graphite Binding Solution");
        put("cryo_zirconium_binding_solution", "Cryo-Zirconium Binding Solution");
        put("hot_sodium_potassium", "Hot Sodium-Potassium");
        put("rhodium_palladium_solution", "Rhodium-Palladium Solution");
        put("technetium_strontium_solution", "Technetium-Strontium Solution");
        put("uranium_233", "Uranium-233");
        put("uranium_236", "Uranium-236");
        put("americium_241", "Americium-241");
        put("spent_uranium_233", "Spent Uranium-233");
        put("spent_uranium_235", "Spent Uranium-235");
        put("spent_uranium_236", "Spent Uranium-236");
        put("extremely_modified_space_grade_steel", "§cExtremely Modified Space Grade Steel");
        put("nevvonian_iron", "§7Nevvonian Iron");
        put("voidglass_shard", "§dVoidglass Shard");
        put("polarity_flipped_bismuthite", "§bPolarity Flipped Bismuthite");
        put("permafrost", "§bPermafrost");
        put("fluorite", "§aFluorite");
        put("ignisium", "§cIgnisium");
        put("crystallized_fluxstone", "§dCrystallized Fluxstone");
        put("polymethyl_methacrylate", "Polymethyl Methacrylate (PMMA)");
        put("resonant_ender", "§3Resonant Ender");
        put("fluix", "§dFluix");
        put("slime", "§aSlime");
        put("magma", "§cMagma");
        put("source_gem", "§zSource Gem");
        put("frost", "§bFrost");
        put("aluminfrost", "§bAluminfrost");
        put("source_imbued_titanium", "§5Source Imbued Titanium");
        put("resonant_rhodium_alloy", "§dResonant Rhodium Alloy");
        put("advanced_quin_naquadian_alloy", "Advanced Quin-Naquadian Alloy");
        put("frost_reinforced_stained_steel", "§bFrost-Reinforced Stained Steel");
        put("eighty_five_percent_pure_nevonian_steel", "§6Eighty Five Percent Pure Nevonian Steel");
        put("void_touched_tungsten_steel", "§5Void Touched Tungsten Steel");
        put("blazing_drilling_fluid", "§cBlazing Drilling Fluid");
        put("voidic_drilling_fluid", "§dVoidic Drilling Fluid");
        put("cryo_ember_fluid", "§bCryo§f-§cEmber§f Fluid");
        put("phoenix_enriched_naquadah", "§6Phoenix Enriched Naquadah");
        put("phoenix_enriched_tritanium", "§cPhoenix Enriched Tritanium");
        put("radioactive_and_heavy_metal_contaminated_naquadatite", "Radioactive and Heavy Metal Contaminated Naquadatite");
        NAMES.forEach((id, name) -> PhoenixCore.PHOENIX_REGISTRATE.addRawLang("material.phoenixcore." + id, name));
    }
}
