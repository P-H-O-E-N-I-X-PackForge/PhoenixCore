package net.phoenix.core.common.data.materials;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet;
import com.gregtechceu.gtceu.api.registry.registrate.entry.MaterialRegistryEntry;

import java.util.HashMap;
import java.util.Map;

import static com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.*;
import static net.phoenix.core.PhoenixCore.PHOENIX_REGISTRATE;

public class PhoenixBeeMaterials {

    public static MaterialRegistryEntry FRUCTOSE, GLUCOSE, SUCROSE, SUGAR_WATER;
    public static MaterialRegistryEntry PEANUT_BUTTER, PEANUT, CREAM, SKIM_MILK, MOLASSES;
    public static MaterialRegistryEntry AMMONIUM_BISULFATE, AMMONIUM_BISULFATE_SOLUTION, AMMONIUM_PERSULFATE,
            PROTEIN_SOLUTION,
            AMINO_ACIDS;
    public static MaterialRegistryEntry HONEY_CATALYST, HONEY_COMB_BASE_MIXTURE, POLLEN_CONCENTRATE_FLUID,
            WAX_MELTING_CATALYST;
    public static MaterialRegistryEntry AURUM_WOOD, INVERT_SUGAR_SOLUTION, HONEY, SUPERCONDUCTIVE_HONEY;

    public static MaterialRegistryEntry IMPURE_WAX, IMPURE_HONEY;

    public static final Map<String, MaterialRegistryEntry> HONEYED_MATERIALS = new HashMap<>();
    public static final Map<String, MaterialRegistryEntry> RAW_WAX_MATERIALS = new HashMap<>();

    public static void register() {
        HONEY = PHOENIX_REGISTRATE.material("honey", builder -> builder.fluid().ingot().color(0xf9c901)
                .iconSet(MaterialIconSet.BRIGHT));
        SUPERCONDUCTIVE_HONEY = PHOENIX_REGISTRATE.material("superconductive_honey", builder -> builder

                .ingot().fluid()
                .color(0xFFBF00)
                .secondaryColor(0x87CEFA)
                .iconSet(MaterialIconSet.DULL)
                .flags(MaterialFlags.NO_SMELTING)
                .cableProperties(GTValues.V[GTValues.IV], 32, 0, true));
        FRUCTOSE = PHOENIX_REGISTRATE.material("fructose", builder -> builder.fluid().color(0xF0F0F0)
                .iconSet(MaterialIconSet.DULL));
        GLUCOSE = PHOENIX_REGISTRATE.material("glucose",
                builder -> builder.fluid().color(0xFFFAF0).iconSet(MaterialIconSet.DULL));
        SUCROSE = PHOENIX_REGISTRATE.material("sucrose",
                builder -> builder.fluid().color(0xF8F8F8).iconSet(MaterialIconSet.DULL));
        SUGAR_WATER = PHOENIX_REGISTRATE.material("sugar_water", builder -> builder.fluid().color(0xFFFFF0)
                .flags(DISABLE_DECOMPOSITION).iconSet(MaterialIconSet.DULL));
        INVERT_SUGAR_SOLUTION = PHOENIX_REGISTRATE.material("invert_sugar_solution",
                builder -> builder.fluid().color(0xFFFCDE)
                        .iconSet(MaterialIconSet.DULL));
        MOLASSES = PHOENIX_REGISTRATE.material("molasses", builder -> builder.fluid().color(0x82574C)
                .iconSet(MaterialIconSet.DULL));

        PEANUT_BUTTER = PHOENIX_REGISTRATE.material("peanut_butter", builder -> builder.fluid().color(0xD8BC9D)
                .iconSet(MaterialIconSet.DULL));
        PEANUT = PHOENIX_REGISTRATE.material("peanut",
                builder -> builder.dust().color(0xE0C8A0).iconSet(MaterialIconSet.DULL));
        CREAM = PHOENIX_REGISTRATE.material("cream",
                builder -> builder.fluid().color(0xFFFBE6).iconSet(MaterialIconSet.DULL));
        SKIM_MILK = PHOENIX_REGISTRATE.material("skim_milk", builder -> builder.fluid().color(0xF8F8FF)
                .iconSet(MaterialIconSet.DULL));

        AMMONIUM_BISULFATE = PHOENIX_REGISTRATE.material("ammonium_bisulfate", builder -> builder.dust().color(0xF0F0F0)
                .iconSet(MaterialIconSet.DULL));
        AMMONIUM_BISULFATE_SOLUTION = PHOENIX_REGISTRATE.material("ammonium_bisulfate_solution",
                builder -> builder.fluid()
                        .color(0xF0F0F0).iconSet(MaterialIconSet.DULL));
        AMMONIUM_PERSULFATE = PHOENIX_REGISTRATE.material("ammonium_persulfate",
                builder -> builder.fluid().color(0xF0FFFF)
                        .iconSet(MaterialIconSet.DULL));
        PROTEIN_SOLUTION = PHOENIX_REGISTRATE.material("protein_solution", builder -> builder.fluid().color(0xFFE0C0)
                .iconSet(MaterialIconSet.DULL));
        AMINO_ACIDS = PHOENIX_REGISTRATE.material("amino_acids", builder -> builder.fluid().color(0xFFFFFF)
                .iconSet(MaterialIconSet.DULL));

        HONEY_CATALYST = PHOENIX_REGISTRATE.material("honey_catalyst", builder -> builder.fluid()
                .color(0xFFF9E3)
                .iconSet(MaterialIconSet.DULL));
        HONEY_COMB_BASE_MIXTURE = PHOENIX_REGISTRATE.material("honey_comb_base_mixture", builder -> builder
                .fluid()
                .color(0xFFF0F5).iconSet(MaterialIconSet.DULL));
        POLLEN_CONCENTRATE_FLUID = PHOENIX_REGISTRATE.material("pollen_concentrate_fluid", builder -> builder
                .fluid()
                .color(0xFFC200).iconSet(MaterialIconSet.DULL));
        WAX_MELTING_CATALYST = PHOENIX_REGISTRATE.material("wax_melting_catalyst",
                builder -> builder.fluid().color(0xADD8E6)
                        .secondaryColor(0x6A5ACD).iconSet(MaterialIconSet.DULL));

        AURUM_WOOD = PHOENIX_REGISTRATE.material("aurum_wood", builder -> builder.dust()
                .color(0x291306).secondaryColor(0xfccd31)
                .iconSet(MaterialIconSet.DULL));

        IMPURE_WAX = PHOENIX_REGISTRATE.material("impure_wax",
                builder -> builder.fluid().color(0xFFA500).secondaryColor(0xD2691E)
                        .iconSet(MaterialIconSet.DULL));
        IMPURE_HONEY = PHOENIX_REGISTRATE.material("impure_honey", builder -> builder.fluid().color(0xD2B48C)
                .secondaryColor(0x8B4513).iconSet(MaterialIconSet.DULL));

        registerAllBeeMineralMaterials();
    }

    private static void registerAllBeeMineralMaterials() {
        Map<String, int[]> colors = new HashMap<>();
        colors.put("pitchblende", new int[] { 0xffd647, 0x0d1e2f });
        colors.put("steel", new int[] { 0x3b3b3b, 0x727272 });
        colors.put("apatite", new int[] { 0x06cdf1, 0x701c07 });
        colors.put("cobalt", new int[] { 0x0f3d79, 0x0f3d79 });
        colors.put("salt", new int[] { 0x945e5a, 0x68fcfc });
        colors.put("sponge", new int[] { 0xbcbc96, 0xe81123 });
        colors.put("ghostly", new int[] { 0xb5c3c8, 0xbdc8cd });
        colors.put("copper", new int[] { 0xB87333, 0xA0522D });
        colors.put("magma", new int[] { 0xcd691b, 0x7b3f10 });
        colors.put("rock_salt", new int[] { 0x646060, 0xc1b9b8 });
        colors.put("slime", new int[] { 0x2ce551, 0x1c9234 });
        colors.put("brown_shroom", new int[] { 0x71492e, 0x2ce551 });
        colors.put("sculk", new int[] { 0x131313, 0x0c0c0c });
        colors.put("nether_quartz", new int[] { 0x7a7672, 0x8c8883 });
        colors.put("scheelite", new int[] { 0xd7e8b3, 0x143cae });
        colors.put("certus_quartz", new int[] { 0xdce2f3, 0x6e95bf });
        colors.put("silky", new int[] { 0xdce2f3, 0x6e95bf });
        colors.put("ice", new int[] { 0x979797, 0x8e908b });
        colors.put("withered", new int[] { 0x131313, 0x141414 });
        colors.put("zombie", new int[] { 0x3e3434, 0x786464 });
        colors.put("blaze", new int[] { 0xf9d678, 0x3e3434 });
        colors.put("bone", new int[] { 0x636363, 0x727272 });
        colors.put("lepidolite", new int[] { 0xffdae4, 0x75376f });
        colors.put("source_gem", new int[] { 0xb403e9, 0x8e02b8 });
        colors.put("cinnabar", new int[] { 0xE34234, 0xB22222 });
        colors.put("topaz", new int[] { 0xFFC87C, 0xFFB347 });
        colors.put("amethyst", new int[] { 0x9966CC, 0x6C3483 });
        colors.put("prismarine", new int[] { 0x7FFFD4, 0x40E0D0 });
        colors.put("realgar", new int[] { 0xff3d33, 0x3f0110 });
        colors.put("pyrope", new int[] { 0x8B0000, 0xB22222 });
        colors.put("zinc", new int[] { 0x7D7F7D, 0xA9A9A9 });
        colors.put("tin", new int[] { 0xD9D6CF, 0xB0B0B0 });
        colors.put("diamond", new int[] { 0xB9F2FF, 0xE0FFFF });
        colors.put("iron", new int[] { 0xD8D8D8, 0xA9A9A9 });
        colors.put("fluorite", new int[] { 0x0C9949, 0x0C9949 });
        colors.put("ruby", new int[] { 0xE0115F, 0xA52A2A });
        colors.put("sapphire", new int[] { 0x0F52BA, 0x4682B4 });
        colors.put("stibnite", new int[] { 0x484848, 0xA9A9A9 });
        colors.put("opal", new int[] { 0xA8C3BC, 0xE0FFFF });
        colors.put("lapis", new int[] { 0x26619C, 0x1C39BB });
        colors.put("electrotine", new int[] { 0x83cbf5, 0x004585 });
        colors.put("redstone", new int[] { 0xFF0000, 0x8B0000 });
        colors.put("saltpeter", new int[] { 0xEDEDED, 0xB0B0B0 });
        colors.put("coal", new int[] { 0x222222, 0x444444 });
        colors.put("ilmenite", new int[] { 0x4B3A2F, 0x8B5F2F });
        colors.put("silicon", new int[] { 0xC0C0C0, 0x808080 });
        colors.put("galena", new int[] { 0x8B8B8B, 0x5A5A5A });
        colors.put("sodalite", new int[] { 0x284387, 0x1C1C7C });
        colors.put("gold", new int[] { 0xFFD700, 0xB8860B });
        colors.put("obsidian", new int[] { 0x2D2D2D, 0x4B0082 });
        colors.put("cobaltite", new int[] { 0x3D59AB, 0x1E3A5C });
        colors.put("bauxite", new int[] { 0xA0522D, 0x8B4513 });
        colors.put("silver", new int[] { 0xC0C0C0, 0xB0B0B0 });
        colors.put("tungstate", new int[] { 0xe0ffc4, 0xab4400 });
        colors.put("emerald", new int[] { 0x50C878, 0x228B22 });
        colors.put("tricalcium_phosphate", new int[] { 0xF5F5DC, 0xE0E0E0 });
        colors.put("nickel", new int[] { 0xAFAFAF, 0x7F7F7F });
        colors.put("fluix", new int[] { 0x6E00FF, 0xB266FF });
        colors.put("malachite", new int[] { 0x43B48C, 0x228B22 });
        colors.put("lead", new int[] { 0x575757, 0x363636 });
        colors.put("invar", new int[] { 0xB2B2B2, 0x8B8B8B });
        colors.put("thorium", new int[] { 0x14230e, 0x111e0c });
        colors.put("graphite", new int[] { 0x10130b, 0x262821 });
        colors.put("sphalerite", new int[] { 0xffdc88, 0x0f1605 });
        colors.put("netherite", new int[] { 0x4c484c, 0x474447 });
        colors.put("resonant_ender", new int[] { 0x0b0b0b, 0x0b0b0b });
        colors.put("chromite", new int[] { 0x544155, 0x2c222c });
        colors.put("pyrolusite", new int[] { 0x1f1e1d, 0x363230 });
        colors.put("platinum", new int[] { 0xfff4ba, 0x8d8d71 });
        colors.put("bismuth", new int[] { 0x5fdddd, 0x517385 });
        colors.put("glowstone", new int[] { 0x816f40, 0xe7c874 });
        colors.put("bastnasite", new int[] { 0x733a21, 0x452314 });
        colors.put("tetrahedrite", new int[] { 0x171d16, 0x242d22 });
        colors.put("sulfur", new int[] { 0xd3ec8a, 0xe1fc93 });
        colors.put("oilsands", new int[] { 0x181814, 0x3c3729 });
        colors.put("tantalite", new int[] { 0x180b04, 0x302828 });
        colors.put("barite", new int[] { 0xe8e2d1, 0xf4b74b });
        colors.put("vanadium_magnetite", new int[] { 0x373741, 0x130f17 });
        colors.put("pyrochlore", new int[] { 0x160d07, 0x2a1a0d });
        colors.put("voidglass_shard", new int[] { 0x6900a8, 0x62009d });
        colors.put("ignisium", new int[] { 0x972900, 0x6c2207 });

        for (Map.Entry<String, int[]> entry : colors.entrySet()) {
            String name = entry.getKey();
            int baseColor = entry.getValue()[0];
            int secColor = entry.getValue()[1];

            MaterialRegistryEntry honeyed = PHOENIX_REGISTRATE.material("honeyed_" + name, builder -> builder
                    .fluid()
                    .color(baseColor)
                    .secondaryColor(secColor)
                    .iconSet(MaterialIconSet.DULL));
            HONEYED_MATERIALS.put(name, honeyed);

            MaterialRegistryEntry rawWax = PHOENIX_REGISTRATE.material("raw_" + name + "_wax", builder -> builder
                    .dust()
                    .color(baseColor)
                    .secondaryColor(secColor)
                    .iconSet(MaterialIconSet.DULL));
            RAW_WAX_MATERIALS.put(name, rawWax);
        }
    }
}
