package net.phoenix.core.common.data.materials;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet;

import net.phoenix.core.PhoenixCore;

import static com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.*;

public class PhoenixPolymerMaterials {

    public static Material POLYMETHYL_METHACRYLATE, METHYL_METHACRYLATE;
    public static Material ACETONE_CYANOHYDRIN, CONCENTRATED_SULFURIC_ACID, OLEUM;
    public static Material SUPERCRITICAL_CARBON_DIOXIDE;

    public static void register() {
        POLYMETHYL_METHACRYLATE = new Material.Builder(PhoenixCore.id("polymethyl_methacrylate")).ingot().polymer()
                .fluid().color(0xF0F8FF).secondaryColor(0xF5FFFF).flags(GENERATE_PLATE).iconSet(MaterialIconSet.DULL)
                .buildAndRegister();
        METHYL_METHACRYLATE = new Material.Builder(PhoenixCore.id("methyl_methacrylate")).fluid().color(0xE8F8F8)
                .secondaryColor(0xF5FFFF).iconSet(MaterialIconSet.DULL).buildAndRegister();
        ACETONE_CYANOHYDRIN = new Material.Builder(PhoenixCore.id("acetone_cyanohydrin")).fluid().color(0xFFF8DC)
                .iconSet(MaterialIconSet.DULL).buildAndRegister();
        CONCENTRATED_SULFURIC_ACID = new Material.Builder(PhoenixCore.id("concentrated_sulfuric_acid")).fluid()
                .color(0xFF8C00).iconSet(MaterialIconSet.DULL).buildAndRegister();
        OLEUM = new Material.Builder(PhoenixCore.id("oleum")).fluid().color(0xDA6600).iconSet(MaterialIconSet.DULL)
                .buildAndRegister();
        SUPERCRITICAL_CARBON_DIOXIDE = new Material.Builder(PhoenixCore.id("supercritical_carbon_dioxide")).fluid()
                .color(0x70A070).secondaryColor(0x90C090).iconSet(MaterialIconSet.DULL).buildAndRegister();
    }
}
