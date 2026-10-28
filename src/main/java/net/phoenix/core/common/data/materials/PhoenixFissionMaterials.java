package net.phoenix.core.common.data.materials;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.BlastProperty;
import com.gregtechceu.gtceu.api.registry.registrate.entry.MaterialRegistryEntry;
import com.gregtechceu.gtceu.common.data.GTElements;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import static com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.*;
import static com.gregtechceu.gtceu.api.data.chemical.material.properties.BlastProperty.GasTier.HIGH;
import static com.gregtechceu.gtceu.api.data.chemical.material.properties.BlastProperty.GasTier.MID;
import static net.phoenix.core.PhoenixCore.PHOENIX_REGISTRATE;
import static net.phoenix.core.common.data.materials.PhoenixProgressionMaterials.ICY_STEEL_MATRIX;
import static net.phoenix.core.common.data.materials.PhoenixProgressionMaterials.SOURCE_IMBUED_TITANIUM;

public class PhoenixFissionMaterials {

    public static MaterialRegistryEntry URANIUM_233, URANIUM_236, AMERICIUM_241, AMERICIUM_HEXAFLUORIDE, URANIUM_OXIDE;
    public static MaterialRegistryEntry INERT_GAS_WASTE, IRRADIATED_THORIUM, IRRADIATED_URANIUM_236, SPENT_URANIUM_233,
            SPENT_URANIUM_235, DEPLETED_URANIUM, DEPLETED_THORIUM, PLUTONIUM_FISSION_ASH, DEPLETED_PLUTONIUM_241,
            LOW_LEVEL_RADIOACTIVE_WASTE, EXTREMELY_MODIFIED_SPACE_GRADE_STEEL;

    public static MaterialRegistryEntry BORON_CARBIDE;
    public static MaterialRegistryEntry NIOBIUM_MODIFIED_SILICON_CARBIDE;
    public static MaterialRegistryEntry CRYO_GRAPHITE_BINDING_SOLUTION, CRYO_ZIRCONIUM_BINDING_SOLUTION;
    public static MaterialRegistryEntry HAFNIUM_CHLORIDE;
    public static MaterialRegistryEntry ZIRCALLOY;
    public static MaterialRegistryEntry ZIRCON, SPACE_GRADE_STEEL;

    public static MaterialRegistryEntry MEDIUM_LEVEL_RADIOACTIVE_WASTE;
    public static MaterialRegistryEntry HOT_SODIUM_POTASSIUM;
    public static MaterialRegistryEntry MEDIUM_PRESSURE_FISSILE_STEAM;
    public static MaterialRegistryEntry CRITICAL_STEAM;

    public static MaterialRegistryEntry FISSION_PRODUCTS_FLUID;
    public static MaterialRegistryEntry RADIOACTIVE_SLUDGE;
    public static MaterialRegistryEntry RADIOACTIVE_GAS_MIXTURE;
    public static MaterialRegistryEntry IMPURE_ZIRCONIUM;
    public static MaterialRegistryEntry IMPURE_HAFNIUM;

    public static MaterialRegistryEntry RHODIUM_PALLADIUM_SOLUTION;
    public static MaterialRegistryEntry TECHNETIUM_STRONTIUM_SOLUTION;
    public static MaterialRegistryEntry GASEOUS_FISSION_BYPRODUCTS;
    public static MaterialRegistryEntry ACIDIC_WASTE;
    public static MaterialRegistryEntry TRACE_FISSION_GASES;
    public static MaterialRegistryEntry PURIFIED_RADIOACTIVE_WASTE_FLUID;

    public static MaterialRegistryEntry EXOTIC_FISSION_CONCENTRATE;
    public static MaterialRegistryEntry EXOTIC_FISSILE_MATERIALS_CLUMP;
    public static MaterialRegistryEntry FISSILE_ASH;
    public static MaterialRegistryEntry TRACE_ACTINIDES;

    public static void register() {
        ZIRCON = PHOENIX_REGISTRATE.material("zircon", builder -> builder
                .dust()
                .color(0xC2B280)
                .secondaryColor(0x8B7D6B)
                .iconSet(MaterialIconSet.DULL)
                .formula("ZrSiO4"));

        BORON_CARBIDE = PHOENIX_REGISTRATE.material("boron_carbide", builder -> builder

                .ingot()
                .color(0x353630)
                .iconSet(MaterialIconSet.DULL)
                .blastTemp(3600, com.gregtechceu.gtceu.api.data.chemical.material.properties.BlastProperty.GasTier.LOW,
                        500, 1500)
                .flags(MaterialFlags.GENERATE_PLATE, MaterialFlags.GENERATE_ROD, MaterialFlags.GENERATE_DENSE)
                .formula("B4C"));

        LOW_LEVEL_RADIOACTIVE_WASTE = PHOENIX_REGISTRATE.material("low_level_radioactive_waste", builder -> builder
                .ingot()
                .color(0x262a23)
                .secondaryColor(0x365320)
                .iconSet(MaterialIconSet.RADIOACTIVE));

        MEDIUM_LEVEL_RADIOACTIVE_WASTE = PHOENIX_REGISTRATE.material("medium_level_radioactive_waste",
                builder -> builder
                        .ingot()
                        .color(0x4d5447).secondaryColor(0x1a2e0a)
                        .iconSet(MaterialIconSet.RADIOACTIVE));

        NIOBIUM_MODIFIED_SILICON_CARBIDE = PHOENIX_REGISTRATE.material("niobium_modified_silicon_carbide",
                builder -> builder

                        .ingot()
                        .color(0x4A4B6B)
                        .secondaryColor(0x101021)
                        .iconSet(MaterialIconSet.METALLIC)
                        .flags(MaterialFlags.GENERATE_PLATE, MaterialFlags.GENERATE_FRAME, MaterialFlags.GENERATE_FOIL)
                        .blastTemp(4500, BlastProperty.GasTier.MID,
                                2000, 1800)
                        .formula("Nb(SiC)x"));

        CRYO_GRAPHITE_BINDING_SOLUTION = PHOENIX_REGISTRATE.material("cryo_graphite_binding_solution",
                builder -> builder

                        .fluid()
                        .color(0x507080)
                        .secondaryColor(0x7090A0)
                        .iconSet(MaterialIconSet.DULL));

        CRYO_ZIRCONIUM_BINDING_SOLUTION = PHOENIX_REGISTRATE.material("cryo_zirconium_binding_solution",
                builder -> builder

                        .fluid()
                        .color(0x80B0CC)
                        .secondaryColor(0xA0D0E0)
                        .iconSet(MaterialIconSet.DULL));

        HAFNIUM_CHLORIDE = PHOENIX_REGISTRATE.material("hafnium_chloride", builder -> builder
                .fluid()
                .color(0xC0C0C0)
                .iconSet(MaterialIconSet.BRIGHT));

        ZIRCALLOY = PHOENIX_REGISTRATE.material("zircalloy", builder -> builder
                .ingot().dust()
                .color(0x002327)
                .secondaryColor(0x000000)
                .iconSet(MaterialIconSet.DULL)
                .flags(
                        MaterialFlags.GENERATE_PLATE,
                        MaterialFlags.GENERATE_GEAR,
                        MaterialFlags.GENERATE_SMALL_GEAR,
                        MaterialFlags.GENERATE_ROD,
                        MaterialFlags.GENERATE_DENSE,
                        MaterialFlags.GENERATE_FOIL,
                        MaterialFlags.GENERATE_SPRING,
                        MaterialFlags.GENERATE_FRAME));

        SPACE_GRADE_STEEL = PHOENIX_REGISTRATE.material("space_grade_steel", builder -> builder
                .ingot()
                .color(0xA9A9A9)
                .secondaryColor(0xB0B0B0)
                .components(PhoenixFissionMaterials.ZIRCALLOY, 1, GTMaterials.Tungsten, 2, ICY_STEEL_MATRIX, 4,
                        GTMaterials.Molybdenum, 1, GTMaterials.Steel, 1)
                .blastTemp(3700, MID, GTValues.VA[GTValues.EV], 1600)
                .iconSet(MaterialIconSet.METALLIC)
                .flags(GENERATE_PLATE,
                        GENERATE_LONG_ROD,
                        PHOSPHORESCENT,
                        GENERATE_ROD,
                        GENERATE_BOLT_SCREW,
                        GENERATE_FRAME,
                        GENERATE_DENSE,
                        GENERATE_ROTOR));

        HOT_SODIUM_POTASSIUM = PHOENIX_REGISTRATE.material("hot_sodium_potassium", builder -> builder

                .fluid()
                .color(0xFF4500)
                .secondaryColor(0xFFD700)
                .iconSet(MaterialIconSet.DULL));

        MEDIUM_PRESSURE_FISSILE_STEAM = PHOENIX_REGISTRATE.material("medium_pressure_fissile_steam", builder -> builder
                .fluid()
                .color(0x7DA10E)
                .iconSet(MaterialIconSet.BRIGHT));

        CRITICAL_STEAM = PHOENIX_REGISTRATE.material("critical_steam", builder -> builder
                .gas()
                .color(0xF0F8FF)
                .secondaryColor(0xADD8E6)
                .iconSet(MaterialIconSet.DULL));

        FISSION_PRODUCTS_FLUID = PHOENIX_REGISTRATE.material("fission_products_fluid", builder -> builder
                .fluid()
                .color(0x556B2F)
                .iconSet(MaterialIconSet.DULL));

        RADIOACTIVE_SLUDGE = PHOENIX_REGISTRATE.material("radioactive_sludge", builder -> builder
                .fluid()
                .color(0x2F4F4F)
                .iconSet(MaterialIconSet.DULL));

        RADIOACTIVE_GAS_MIXTURE = PHOENIX_REGISTRATE.material("radioactive_gas_mixture", builder -> builder
                .gas()
                .color(0x6B8E23)
                .iconSet(MaterialIconSet.DULL));

        RHODIUM_PALLADIUM_SOLUTION = PHOENIX_REGISTRATE.material("rhodium_palladium_solution", builder -> builder

                .fluid()
                .color(0x9E9E9E)
                .iconSet(MaterialIconSet.SHINY));

        TECHNETIUM_STRONTIUM_SOLUTION = PHOENIX_REGISTRATE.material("technetium_strontium_solution", builder -> builder

                .fluid()
                .color(0x7CFC00)
                .iconSet(MaterialIconSet.BRIGHT));

        GASEOUS_FISSION_BYPRODUCTS = PHOENIX_REGISTRATE.material("gaseous_fission_byproducts", builder -> builder
                .gas()
                .color(0xB0C4DE)
                .iconSet(MaterialIconSet.DULL));

        ACIDIC_WASTE = PHOENIX_REGISTRATE.material("acidic_waste", builder -> builder
                .fluid()
                .color(0x9ACD32)
                .iconSet(MaterialIconSet.DULL));

        TRACE_FISSION_GASES = PHOENIX_REGISTRATE.material("trace_fission_gases", builder -> builder
                .gas()
                .color(0xC0FFEE)
                .iconSet(MaterialIconSet.DULL));

        PURIFIED_RADIOACTIVE_WASTE_FLUID = PHOENIX_REGISTRATE.material("purified_radioactive_waste_fluid",
                builder -> builder
                        .fluid()
                        .color(0x3B5323)
                        .iconSet(MaterialIconSet.DULL));

        EXOTIC_FISSION_CONCENTRATE = PHOENIX_REGISTRATE.material("exotic_fission_concentrate", builder -> builder
                .dust()
                .color(0x800080)
                .secondaryColor(0x00FF00)
                .iconSet(MaterialIconSet.RADIOACTIVE));

        EXOTIC_FISSILE_MATERIALS_CLUMP = PHOENIX_REGISTRATE.material("exotic_fissile_materials_clump",
                builder -> builder
                        .dust()
                        .color(0x556B2F)
                        .secondaryColor(0x00FF00)
                        .iconSet(MaterialIconSet.RADIOACTIVE));

        FISSILE_ASH = PHOENIX_REGISTRATE.material("fissile_ash", builder -> builder
                .dust()
                .color(0x444444)
                .iconSet(MaterialIconSet.RADIOACTIVE));

        PLUTONIUM_FISSION_ASH = PHOENIX_REGISTRATE.material("plutonium_fission_ash", builder -> builder
                .dust()
                .color(0x4B0082)
                .secondaryColor(0x8A2BE2)
                .iconSet(MaterialIconSet.RADIOACTIVE));

        TRACE_ACTINIDES = PHOENIX_REGISTRATE.material("trace_actinides", builder -> builder
                .dust()
                .color(0x2E8B57)
                .iconSet(MaterialIconSet.RADIOACTIVE));
        URANIUM_233 = PHOENIX_REGISTRATE.material("uranium_233", builder -> builder

                .ingot().fluid().color(0x33FF33).secondaryColor(0x00CC00).element(PhoenixElements.URANIUM_233)
                .iconSet(MaterialIconSet.RADIOACTIVE));

        URANIUM_236 = PHOENIX_REGISTRATE.material("uranium_236", builder -> builder

                .ingot().fluid().color(0x33CCFF).secondaryColor(0x0099EE).element(PhoenixElements.URANIUM_236)
                .iconSet(MaterialIconSet.RADIOACTIVE));

        AMERICIUM_241 = PHOENIX_REGISTRATE.material("americium_241", builder -> builder

                .ingot().color(0xCDC9C9).secondaryColor(0x8B8B7A).element(PhoenixElements.AMERICIUM_241)
                .iconSet(MaterialIconSet.RADIOACTIVE));

        AMERICIUM_HEXAFLUORIDE = PHOENIX_REGISTRATE.material("americium_hexafluoride", builder -> builder
                .gas().color(0xFFFFFF).secondaryColor(0xADD8E6).iconSet(MaterialIconSet.DULL));

        URANIUM_OXIDE = PHOENIX_REGISTRATE.material("uranium_oxide", builder -> builder
                .fluid().color(0x00FF00).secondaryColor(0x000000).iconSet(MaterialIconSet.DULL));

        INERT_GAS_WASTE = PHOENIX_REGISTRATE.material("inert_gas_waste", builder -> builder
                .gas().color(0xC0C0C0).secondaryColor(0xD0D0D0).iconSet(MaterialIconSet.DULL));

        IRRADIATED_THORIUM = PHOENIX_REGISTRATE.material("irradiated_thorium", builder -> builder
                .ingot().fluid().color(0x90A090).secondaryColor(0x708070).element(GTElements.Th)
                .iconSet(MaterialIconSet.RADIOACTIVE));

        IRRADIATED_URANIUM_236 = PHOENIX_REGISTRATE.material("irradiated_uranium_236", builder -> builder
                .ingot().fluid().color(0x90A090).secondaryColor(0x708070).element(PhoenixElements.URANIUM_236)
                .iconSet(MaterialIconSet.RADIOACTIVE));

        SPENT_URANIUM_233 = PHOENIX_REGISTRATE.material("spent_uranium_233", builder -> builder

                .dust().ingot().color(0x503040).secondaryColor(0x705060).element(PhoenixElements.URANIUM_233)
                .iconSet(MaterialIconSet.RADIOACTIVE));

        SPENT_URANIUM_235 = PHOENIX_REGISTRATE.material("spent_uranium_235", builder -> builder

                .ingot().color(0x603030).secondaryColor(0x402020).element(GTElements.U235)
                .iconSet(MaterialIconSet.RADIOACTIVE));

        SPENT_URANIUM_235 = PHOENIX_REGISTRATE.material("spent_uranium_236", builder -> builder

                .ingot()
                .color(0x8B8878)
                .secondaryColor(0x556B2F)
                .element(PhoenixElements.URANIUM_236)
                .iconSet(MaterialIconSet.RADIOACTIVE));

        DEPLETED_URANIUM = PHOENIX_REGISTRATE.material("depleted_uranium", builder -> builder
                .ingot().color(0x555030).secondaryColor(0x333010).element(GTElements.U238)
                .iconSet(MaterialIconSet.RADIOACTIVE));

        DEPLETED_THORIUM = PHOENIX_REGISTRATE.material("depleted_thorium", builder -> builder
                .ingot()
                .color(0x505050)
                .secondaryColor(0x707070)
                .element(GTElements.Th)
                .iconSet(MaterialIconSet.RADIOACTIVE));

        DEPLETED_PLUTONIUM_241 = PHOENIX_REGISTRATE.material("depleted_plutonium_241", builder -> builder
                .ingot()
                .color(0x2F4F4F)
                .secondaryColor(0x696969)
                .element(GTElements.Pu241)
                .iconSet(MaterialIconSet.RADIOACTIVE));

        EXTREMELY_MODIFIED_SPACE_GRADE_STEEL = PHOENIX_REGISTRATE.material("extremely_modified_space_grade_steel",
                builder -> builder

                        .ingot()
                        .liquid(6500)
                        .color(0xad6161)
                        .secondaryColor(0x593856)
                        .iconSet(MaterialIconSet.BRIGHT)
                        .cableProperties(GTValues.V[GTValues.LuV], 64, 0, true)
                        .components(SOURCE_IMBUED_TITANIUM, 4, GTMaterials.Ruthenium, 2,
                                PhoenixFissionMaterials.SPACE_GRADE_STEEL, 4, GTMaterials.Radon, 1)
                        .blastTemp(7200, HIGH, GTValues.VA[GTValues.LuV], 600));
    }
}
