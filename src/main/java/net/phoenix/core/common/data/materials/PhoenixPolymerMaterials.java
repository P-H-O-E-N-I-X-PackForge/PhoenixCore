package net.phoenix.core.common.data.materials;

import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet;
import com.gregtechceu.gtceu.api.registry.registrate.entry.MaterialRegistryEntry;

import static com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.*;
import static net.phoenix.core.PhoenixCore.PHOENIX_REGISTRATE;

public class PhoenixPolymerMaterials {

    public static MaterialRegistryEntry POLYMETHYL_METHACRYLATE, METHYL_METHACRYLATE;
    public static MaterialRegistryEntry ACETONE_CYANOHYDRIN, CONCENTRATED_SULFURIC_ACID, OLEUM;
    public static MaterialRegistryEntry SUPERCRITICAL_CARBON_DIOXIDE;

    public static void register() {
        POLYMETHYL_METHACRYLATE = PHOENIX_REGISTRATE.material("polymethyl_methacrylate", builder -> builder
                .ingot().polymer()
                .fluid().color(0xF0F8FF).secondaryColor(0xF5FFFF).flags(GENERATE_PLATE).iconSet(MaterialIconSet.DULL));
        METHYL_METHACRYLATE = PHOENIX_REGISTRATE.material("methyl_methacrylate",
                builder -> builder.fluid().color(0xE8F8F8)
                        .secondaryColor(0xF5FFFF).iconSet(MaterialIconSet.DULL));
        ACETONE_CYANOHYDRIN = PHOENIX_REGISTRATE.material("acetone_cyanohydrin",
                builder -> builder.fluid().color(0xFFF8DC)
                        .iconSet(MaterialIconSet.DULL));
        CONCENTRATED_SULFURIC_ACID = PHOENIX_REGISTRATE.material("concentrated_sulfuric_acid",
                builder -> builder.fluid()
                        .color(0xFF8C00).iconSet(MaterialIconSet.DULL));
        OLEUM = PHOENIX_REGISTRATE.material("oleum",
                builder -> builder.fluid().color(0xDA6600).iconSet(MaterialIconSet.DULL));
        SUPERCRITICAL_CARBON_DIOXIDE = PHOENIX_REGISTRATE.material("supercritical_carbon_dioxide",
                builder -> builder.fluid()
                        .color(0x70A070).secondaryColor(0x90C090).iconSet(MaterialIconSet.DULL));
    }
}
