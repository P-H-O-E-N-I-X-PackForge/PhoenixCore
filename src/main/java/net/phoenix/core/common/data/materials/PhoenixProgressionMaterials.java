package net.phoenix.core.common.data.materials;

import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.BlastProperty;
import com.gregtechceu.gtceu.api.registry.registrate.entry.MaterialRegistryEntry;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.*;
import static com.gregtechceu.gtceu.api.data.chemical.material.properties.BlastProperty.GasTier.*;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.*;
import static com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry.SOURCE_GEM;
import static net.phoenix.core.PhoenixCore.PHOENIX_REGISTRATE;

public class PhoenixProgressionMaterials {

    public static MaterialRegistryEntry ALUMINFROST, SOURCE_IMBUED_TITANIUM, RESONANT_RHODIUM_ALLOY;
    public static MaterialRegistryEntry EightyFivePercentPureNevonianSteel, AURUM_STEEL,
            FIERY_BRONZE, VOID_TOUCHED_TUNGSTEN_STEEL, FROST_REINFORCED_STAINED_STEEL, FROST,
            ADVANCED_QUIN_NAQUADIAN_ALLOY;

    public static MaterialRegistryEntry MAGMATIC_MANGANESE_LEAD, CRYOGENIC_ALUMINUM_STRAND, ICY_STEEL_MATRIX,
            SOURCE_TITANIUM_FILAMENT, MAGMA, SOURCE_GEM, SPONGE, SLIME, ZOMBIE, WITHERED, GHOSTLY, FLUIX, PRISMARINE,
            SILKY;

    public static MaterialRegistryEntry PHOENIX_ENRICHED_NAQUADAH, PHOENIX_ENRICHED_TRITANIUM, AETHERIUM_STEEL,
            UNREFINED_ALUMINFROST;
    public static MaterialRegistryEntry BLAZING_DRILLING_FLUID, VOIDIC_DRILLING_FLUID, CRYO_EMBER_FLUID;
    public static MaterialRegistryEntry RESONANT_ENDER, PRISMATIC_PAINT;
    public static MaterialRegistryEntry NAQUADATITE_ACIDIC, HEAVILY_FLUORINE_CONTAMINATED_NAQUADATITE,
            RADIOACTIVE_AND_HEAVY_METAL_CONTAMINATED_NAQUADATITE, RADIOACTIVE_CONTAMINATED_NAQUADATITE,
            RADIOACTIVE_NAQUADACID, NAQUADAH_DIOXIDE, LIGHTLY_CHLORINE_CONTAMINATED_WASTE,
            CALCIUM_FLUORINE, ALUMINIUM_CAESIUM_CHLORIDE, ASTATINE, NAQUADATIA_ASTATINE,
            SODIUM_ASTATINE_BISULFATE, HEAVY_METAL_SLUDGE, ACIDIC_WASTE_SLUDGE,
            HEAVILY_ACIDIC_NAQUADATITE, CHLORINE_WASTE_NAQUADATIA_ASTATINE, NAQUADATITE, GRAPHENE_SLUDGE;

    public static void register() {
        PRISMATIC_PAINT = PHOENIX_REGISTRATE.material("prismatic_paint", builder -> builder
                .fluid()
                .color(0x00FFFF)
                .iconSet(MaterialIconSet.DULL));

        RESONANT_ENDER = PHOENIX_REGISTRATE.material("resonant_ender", builder -> builder

                .ingot()
                .fluid()
                .color(0x032620)
                .iconSet(MaterialIconSet.SHINY));

        FLUIX = PHOENIX_REGISTRATE.material("fluix", builder -> builder

                .ingot()
                .color(0x6E00FF)
                .secondaryColor(0xB266FF)
                .iconSet(MaterialIconSet.SHINY));

        SLIME = PHOENIX_REGISTRATE.material("slime", builder -> builder
                .ingot()
                .color(0x2ce551)
                .secondaryColor(0x1c9234)
                .iconSet(MaterialIconSet.SHINY));

        ZOMBIE = PHOENIX_REGISTRATE.material("zombie", builder -> builder
                .ingot()
                .color(0x3e3434)
                .secondaryColor(0x786464)
                .iconSet(MaterialIconSet.SHINY));

        PRISMARINE = PHOENIX_REGISTRATE.material("prismarine", builder -> builder
                .dust()
                .color(0x7FFFD4)
                .secondaryColor(0x40E0D0)
                .iconSet(MaterialIconSet.SHINY));

        SILKY = PHOENIX_REGISTRATE.material("silky", builder -> builder
                .dust()
                .color(0xdce2f3)
                .secondaryColor(0x6e95bf)
                .iconSet(MaterialIconSet.SHINY));

        MAGMA = PHOENIX_REGISTRATE.material("magma", builder -> builder

                .dust()
                .ingot()
                .fluid()
                .color(0xff6d00)
                .secondaryColor(0xeaa92e)
                .iconSet(MaterialIconSet.DULL));

        SPONGE = PHOENIX_REGISTRATE.material("sponge", builder -> builder
                .dust()
                .ingot()
                .color(0xbcbc96)
                .secondaryColor(0xe81123)
                .iconSet(MaterialIconSet.DULL));

        WITHERED = PHOENIX_REGISTRATE.material("withered", builder -> builder
                .dust()
                .ingot()
                .color(0x131313)
                .secondaryColor(0x141414)
                .iconSet(MaterialIconSet.DULL));

        GHOSTLY = PHOENIX_REGISTRATE.material("ghostly", builder -> builder
                .dust()
                .ingot()
                .color(0xb5c3c8)
                .secondaryColor(0xbdc8cd)
                .iconSet(MaterialIconSet.DULL));

        SOURCE_GEM = PHOENIX_REGISTRATE.material("source_gem", builder -> builder

                .dust()
                .color(0xBF00FF)
                .iconSet(MaterialIconSet.DULL));

        FROST = PHOENIX_REGISTRATE.material("frost", builder -> builder

                .fluid()
                .color(0xA7D1EB)
                .secondaryColor(0x778899)
                .iconSet(MaterialIconSet.SHINY));

        UNREFINED_ALUMINFROST = PHOENIX_REGISTRATE.material("unrefined_aluminfrost", builder -> builder
                .dust()
                .color(0xA7D1EB)
                .cableProperties(GTValues.MV, 1, 2, false)
                .secondaryColor(0x778899)
                .iconSet(MaterialIconSet.DULL));

        ALUMINFROST = PHOENIX_REGISTRATE.material("aluminfrost", builder -> builder.ingot().fluid()
                .color(0xadd8e6)
                .secondaryColor(0xc0c0c0).iconSet(MaterialIconSet.BRIGHT)
                .fluidPipeProperties(1200, 110, true, true, false, false)
                .flags(GENERATE_PLATE, GENERATE_RING, GENERATE_ROUND, GENERATE_GEAR, GENERATE_SMALL_GEAR,
                        PHOSPHORESCENT, GENERATE_LONG_ROD,
                        GENERATE_ROD, GENERATE_BOLT_SCREW, GENERATE_FRAME, GENERATE_DENSE, GENERATE_ROTOR,
                        GENERATE_FOIL));
        SOURCE_IMBUED_TITANIUM = PHOENIX_REGISTRATE.material("source_imbued_titanium", builder -> builder
                .ingot().fluid()
                .color(0xc600ff).formula("✨C✨Ti").iconSet(MaterialIconSet.METALLIC)
                .cableProperties(GTValues.EV, 1, 2, false)
                .fluidPipeProperties(2800, 200, true, true, false, false)
                .flags(GENERATE_PLATE, GENERATE_RING, GENERATE_SMALL_GEAR, GENERATE_GEAR, PHOSPHORESCENT, GENERATE_ROD,
                        GENERATE_LONG_ROD,
                        GENERATE_BOLT_SCREW, GENERATE_FRAME, GENERATE_DENSE, GENERATE_ROTOR, GENERATE_FOIL,
                        GENERATE_ROTOR));
        RESONANT_RHODIUM_ALLOY = PHOENIX_REGISTRATE.material("resonant_rhodium_alloy", builder -> builder

                .ingot().fluid()
                .color(0xE245F8).secondaryColor(0xA345B0).iconSet(MaterialIconSet.METALLIC)
                .components(GTMaterials.Rhodium, 3, GTMaterials.Palladium, 4, PhoenixOres.POLARITY_FLIPPED_BISMUTHITE,
                        1, GTMaterials.Cerium, 4)
                .cableProperties(GTValues.LuV, 1, 2, false)
                .blastTemp(3600, HIGH, 480, 400).fluidPipeProperties(2800, 200, true, true, false, false)
                .flags(GENERATE_PLATE, GENERATE_RING, PHOSPHORESCENT, GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_GEAR,
                        GENERATE_SMALL_GEAR, GENERATE_BOLT_SCREW, GENERATE_FRAME, GENERATE_DENSE, GENERATE_ROTOR,
                        GENERATE_FOIL));

        ADVANCED_QUIN_NAQUADIAN_ALLOY = PHOENIX_REGISTRATE.material("advanced_quin_naquadian_alloy", builder -> builder

                .ingot()
                .liquid(7400)
                .color(0x000000)
                .secondaryColor(0x8B0000)
                .cableProperties(GTValues.ZPM, 1, 2, false)
                .iconSet(MaterialIconSet.RADIOACTIVE)
                .fluidPipeProperties(8000, 800, true, true, true, true)
                .components(GTMaterials.Naquadah, 5, GTMaterials.Trinium, 1, GTMaterials.Technetium, 3,
                        GTMaterials.Strontium, 4, GTMaterials.Iodine, 1)
                .blastTemp(7200, HIGH, GTValues.VA[GTValues.ZPM], 1950)
                .flags(GENERATE_PLATE,
                        GENERATE_RING,
                        PHOSPHORESCENT,
                        GENERATE_ROD,
                        GENERATE_BOLT_SCREW,
                        GENERATE_FRAME,
                        GENERATE_DENSE,
                        GENERATE_SMALL_GEAR,
                        GENERATE_ROTOR,
                        GENERATE_FOIL,
                        GENERATE_LONG_ROD));

        FROST_REINFORCED_STAINED_STEEL = PHOENIX_REGISTRATE.material("frost_reinforced_stained_steel",
                builder -> builder

                        .ingot()
                        .fluid()
                        .color(0xB0E0E6).secondaryColor(0x708090)
                        .iconSet(MaterialIconSet.SHINY)
                        .cableProperties(GTValues.HV, 1, 2, false)
                        .components(GTMaterials.StainlessSteel, 4, FROST, 2, GTMaterials.Aluminium, 2)
                        .blastTemp(4500, BlastProperty.GasTier.HIGH, GTValues.VA[GTValues.EV], 1200)
                        .flags(
                                GENERATE_PLATE,
                                GENERATE_ROD,
                                GENERATE_LONG_ROD,
                                GENERATE_GEAR,
                                GENERATE_SMALL_GEAR,
                                GENERATE_FRAME,
                                GENERATE_BOLT_SCREW,
                                GENERATE_ROTOR,
                                GENERATE_RING,
                                GENERATE_DENSE,
                                GENERATE_FOIL));
        EightyFivePercentPureNevonianSteel = PHOENIX_REGISTRATE.material("eighty_five_percent_pure_nevonian_steel",
                builder -> builder
                        .ingot().color(0xFFFFE0)
                        .secondaryColor(0xFFD700).iconSet(PhoenixMaterialSet.ALMOST_PURE_NEVONIAN_STEEL_HOLDER)
                        .blastTemp(3800, MID, GTValues.VA[GTValues.EV], 1200)
                        .flags(GENERATE_PLATE, GENERATE_GEAR, GENERATE_SMALL_GEAR, GENERATE_SPRING, PHOSPHORESCENT,
                                GENERATE_ROD, GENERATE_DENSE, GENERATE_BOLT_SCREW, GENERATE_FRAME));
        VOID_TOUCHED_TUNGSTEN_STEEL = PHOENIX_REGISTRATE.material("void_touched_tungsten_steel", builder -> builder

                .ingot()
                .liquid(3100)
                .color(0x4B0082).secondaryColor(0x000000)
                .iconSet(MaterialIconSet.METALLIC)
                .cableProperties(GTValues.IV, 1, 2, false)
                .components(GTMaterials.Tungsten, 4, PhoenixOres.VOIDGLASS_SHARD, 4, GTMaterials.Molybdenum, 2)
                .blastTemp(4200, BlastProperty.GasTier.MID, GTValues.VA[GTValues.EV], 1000)
                .fluidPipeProperties(3800, 250, true, true, true, true)
                .flags(
                        GENERATE_PLATE,
                        GENERATE_RING,
                        PHOSPHORESCENT,
                        GENERATE_ROD,
                        GENERATE_LONG_ROD,
                        GENERATE_BOLT_SCREW,
                        GENERATE_FRAME,
                        GENERATE_GEAR,
                        GENERATE_SMALL_GEAR,
                        GENERATE_DENSE,
                        GENERATE_ROTOR, GENERATE_FOIL));
        AURUM_STEEL = PHOENIX_REGISTRATE.material("aurum_steel", builder -> builder.ingot().fluid().color(0xd0a860)
                .secondaryColor(0xc0c0c0).iconSet(MaterialIconSet.METALLIC)
                .cableProperties(LV, 1, 2, true)
                .fluidPipeProperties(5000, 50, true, true, false, false)
                .flags(GENERATE_PLATE, GENERATE_RING, GENERATE_ROUND, GENERATE_GEAR, PHOSPHORESCENT, GENERATE_ROD,
                        GENERATE_BOLT_SCREW, GENERATE_FRAME, GENERATE_DENSE, GENERATE_ROTOR, GENERATE_FOIL,
                        GENERATE_SMALL_GEAR, GENERATE_LONG_ROD));
        FIERY_BRONZE = PHOENIX_REGISTRATE.material("fiery_bronze", builder -> builder.ingot().fluid().color(0xff6d00)
                .secondaryColor(0xa0522d).iconSet(MaterialIconSet.DULL)
                .fluidPipeProperties(2000, 40, true, false, false, false)
                .flags(GENERATE_PLATE, GENERATE_RING, GENERATE_ROUND, GENERATE_GEAR, PHOSPHORESCENT, GENERATE_ROD,
                        GENERATE_BOLT_SCREW, GENERATE_FRAME, GENERATE_DENSE, GENERATE_ROTOR,
                        PhoenixMaterialFlags.GENERATE_BEE_HONEYCOMB_BLOCK, PhoenixMaterialFlags.GENERATE_BEE_COMB));

        MAGMATIC_MANGANESE_LEAD = PHOENIX_REGISTRATE.material("magmatic_manganese_lead",
                builder -> builder.ingot().fluid()
                        .color(0x8B4513).cableProperties(GTValues.V[LV], 2, 0, true));
        CRYOGENIC_ALUMINUM_STRAND = PHOENIX_REGISTRATE.material("cryogenic_aluminum_strand",
                builder -> builder.ingot().fluid()
                        .color(0xADD8E6).cableProperties(GTValues.V[GTValues.MV], 4, 0, true));
        ICY_STEEL_MATRIX = PHOENIX_REGISTRATE.material("icy_steel_matrix",
                builder -> builder.ingot().fluid().color(0xE0FFFF)
                        .secondaryColor(0x696969).element(PhoenixElements.ICY_STEEL_MATRIX)
                        .cableProperties(GTValues.V[GTValues.HV], 8, 0, true));
        SOURCE_TITANIUM_FILAMENT = PHOENIX_REGISTRATE.material("source_titanium_filament",
                builder -> builder.ingot().fluid()
                        .color(0x8B008B).secondaryColor(0x454545)
                        .cableProperties(GTValues.V[GTValues.EV], 16, 0, true));
        BLAZING_DRILLING_FLUID = PHOENIX_REGISTRATE.material("blazing_drilling_fluid", builder -> builder

                .fluid()
                .color(0xFF6600)
                .secondaryColor(0xEE4400)
                .iconSet(MaterialIconSet.DULL));
        VOIDIC_DRILLING_FLUID = PHOENIX_REGISTRATE.material("voidic_drilling_fluid", builder -> builder

                .fluid()
                .color(0x400080)
                .secondaryColor(0x7B68EE)
                .iconSet(MaterialIconSet.DULL));
        CRYO_EMBER_FLUID = PHOENIX_REGISTRATE.material("cryo_ember_fluid", builder -> builder

                .liquid(6000)
                .color(0xFF4500)
                .secondaryColor(0xA020F0)
                .iconSet(MaterialIconSet.DULL));

        PHOENIX_ENRICHED_NAQUADAH = PHOENIX_REGISTRATE.material("phoenix_enriched_naquadah", builder -> builder
                .ingot()
                .color(0xFFA500).iconSet(MaterialIconSet.SHINY).cableProperties(GTValues.V[GTValues.ZPM], 64, 8, true)
                .blastTemp(1500, MID, GTValues.VA[GTValues.ZPM], 750000)
                .flags(GENERATE_FRAME, PhoenixMaterialFlags.GENERATE_CRYSTAL_ROSE));
        PHOENIX_ENRICHED_TRITANIUM = PHOENIX_REGISTRATE.material("phoenix_enriched_tritanium", builder -> builder
                .ingot()
                .color(0xFF0000)
                .iconSet(PhoenixMaterialSet.ALMOST_PURE_NEVONIAN_STEEL_HOLDER)
                .cableProperties(GTValues.V[GTValues.ZPM], 64, 8, true)
                .blastTemp(1800, MID, GTValues.VA[GTValues.ZPM], 900000)
                .flags(GENERATE_FRAME, PhoenixMaterialFlags.GENERATE_CRYSTAL_ROSE));

        AETHERIUM_STEEL = PHOENIX_REGISTRATE.material("aetherium_steel",
                builder -> builder.ingot().fluid().color(0xADD8E6)
                        .cableProperties(GTValues.V[GTValues.ZPM], 32, 4, true)
                        .blastTemp(1300, MID, GTValues.VA[GTValues.ZPM], 600000).flags(GENERATE_PLATE));

        NAQUADATITE_ACIDIC = PHOENIX_REGISTRATE.material("naquadatite_acidic", builder -> builder
                .dust()
                .color(0x3B5A40)
                .iconSet(MaterialIconSet.DULL));

        HEAVILY_FLUORINE_CONTAMINATED_NAQUADATITE = PHOENIX_REGISTRATE
                .material("heavily_fluorine_contaminated_naquadatite", builder -> builder
                        .dust()
                        .color(0x4E7C59)
                        .iconSet(MaterialIconSet.DULL));

        RADIOACTIVE_AND_HEAVY_METAL_CONTAMINATED_NAQUADATITE = PHOENIX_REGISTRATE
                .material("radioactive_and_heavy_metal_contaminated_naquadatite", builder -> builder

                        .dust()
                        .color(0x2B3A2C)
                        .iconSet(MaterialIconSet.RADIOACTIVE));

        RADIOACTIVE_CONTAMINATED_NAQUADATITE = PHOENIX_REGISTRATE.material("radioactive_contaminated_naquadatite",
                builder -> builder
                        .dust()
                        .color(0x334D37)
                        .iconSet(MaterialIconSet.RADIOACTIVE));

        RADIOACTIVE_NAQUADACID = PHOENIX_REGISTRATE.material("radioactive_naquadacid", builder -> builder
                .dust()
                .color(0x1E4620)
                .iconSet(MaterialIconSet.RADIOACTIVE));

        NAQUADAH_DIOXIDE = PHOENIX_REGISTRATE.material("naquadah_dioxide", builder -> builder
                .dust()
                .color(0x222222)
                .iconSet(MaterialIconSet.DULL));

        LIGHTLY_CHLORINE_CONTAMINATED_WASTE = PHOENIX_REGISTRATE.material("lightly_chlorine_contaminated_waste",
                builder -> builder
                        .dust()
                        .color(0x8FBC8F)
                        .iconSet(MaterialIconSet.DULL));

        CALCIUM_FLUORINE = PHOENIX_REGISTRATE.material("calcium_fluorine", builder -> builder
                .dust()
                .color(0xE0EEEE)
                .iconSet(MaterialIconSet.DULL));

        ALUMINIUM_CAESIUM_CHLORIDE = PHOENIX_REGISTRATE.material("aluminium_caesium_chloride", builder -> builder
                .dust()
                .color(0x778899)
                .iconSet(MaterialIconSet.DULL));

        ASTATINE = PHOENIX_REGISTRATE.material("astatine", builder -> builder
                .dust()
                .color(0xBF00FF)
                .secondaryColor(0x41006F)
                .iconSet(MaterialIconSet.DULL));

        NAQUADATIA_ASTATINE = PHOENIX_REGISTRATE.material("naquadatia_astatine", builder -> builder
                .dust()
                .color(0x5B1382)
                .iconSet(MaterialIconSet.DULL));

        SODIUM_ASTATINE_BISULFATE = PHOENIX_REGISTRATE.material("sodium_astatine_bisulfate", builder -> builder
                .dust()
                .color(0x8A2BE2)
                .iconSet(MaterialIconSet.DULL));

        HEAVY_METAL_SLUDGE = PHOENIX_REGISTRATE.material("heavy_metal_sludge", builder -> builder
                .dust()
                .liquid()
                .color(0x2F4F4F)
                .iconSet(MaterialIconSet.DULL));

        ACIDIC_WASTE_SLUDGE = PHOENIX_REGISTRATE.material("acidic_waste_sludge", builder -> builder
                .dust()
                .liquid()
                .color(0x4B5320)
                .iconSet(MaterialIconSet.DULL));

        HEAVILY_ACIDIC_NAQUADATITE = PHOENIX_REGISTRATE.material("heavily_acidic_naquadatite", builder -> builder
                .dust()
                .color(0x1B4D3E)
                .iconSet(MaterialIconSet.DULL));

        CHLORINE_WASTE_NAQUADATIA_ASTATINE = PHOENIX_REGISTRATE.material("chlorine_waste_naquadatia_astatine",
                builder -> builder
                        .dust()
                        .color(0x3D6346)
                        .iconSet(MaterialIconSet.DULL));

        GRAPHENE_SLUDGE = PHOENIX_REGISTRATE.material("graphene_sludge", builder -> builder
                .dust()
                .liquid()
                .color(0x1C1C1C)
                .iconSet(MaterialIconSet.DULL));
    }
}
