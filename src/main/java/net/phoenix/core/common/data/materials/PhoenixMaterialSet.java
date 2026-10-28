package net.phoenix.core.common.data.materials;

import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconType;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.phoenix.core.PhoenixCore;

import static com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet.*;

public class PhoenixMaterialSet {

    public static final MaterialIconType NANITES = new MaterialIconType("nanites");
    public static final MaterialIconType CRYSTAL_ROSE = new MaterialIconType("crystal_rose");
    public static final MaterialIconType TIER_ONE_BEE = new MaterialIconType("tier_one_bee");
    public static final MaterialIconType TIER_TWO_BEE = new MaterialIconType("tier_two_bee");
    public static final MaterialIconType TIER_THREE_BEE = new MaterialIconType("tier_three_bee");
    public static final MaterialIconType BEE_COMB = new MaterialIconType("honeycomb");
    public static final MaterialIconType HONEYCOMB_BLOCK = new MaterialIconType("honeycomb_block");

    public static final MaterialIconSet ALMOST_PURE_NEVONIAN_STEEL = Registry.register(
            GTRegistries.MATERIAL_ICON_SETS,
            PhoenixCore.id("almost_pure_nevonian_steel"),
            new MaterialIconSet(PhoenixCore.id("almost_pure_nevonian_steel"), SHINY));

    public static final Holder<MaterialIconSet> ALMOST_PURE_NEVONIAN_STEEL_HOLDER = GTRegistries.MATERIAL_ICON_SETS
            .getHolderOrThrow(
                    ResourceKey.create(GTRegistries.Keys.MATERIAL_ICON_SET,
                            PhoenixCore.id("almost_pure_nevonian_steel")));

    public static void init() {}
}
