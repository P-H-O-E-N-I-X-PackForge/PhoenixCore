package net.phoenix.core.common.data.recipe.generated;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;

import net.phoenix.core.common.data.materials.PhoenixMaterialFlags;

public class BeePrefixHelper {

    public static void addTierOneBeeFlag(Material material) {
        if (material == null) {
            return;
        }

        material.addFlags(PhoenixMaterialFlags.GENERATE_TIER_ONE_BEE);
    }

    public static void addTierOneBeeFlag(Material... materials) {
        if (materials == null) return;
        for (Material m : materials) {
            addTierOneBeeFlag(m);
        }
    }

    public static void addTierTwoBeeFlag(Material... materials) {
        if (materials == null) return;
        for (Material m : materials) {
            addTierTwoBeeFlag(m);
        }
    }

    public static void addTierTwoBeeFlag(Material material) {
        if (material == null) {
            return;
        }

        material.addFlags(PhoenixMaterialFlags.GENERATE_TIER_TWO_BEE);
    }

    public static void addBeeCombFlag(Material material) {
        if (material == null) {
            return;
        }

        material.addFlags(PhoenixMaterialFlags.GENERATE_BEE_COMB);
    }

    public static void addBeeCombFlag(Material... materials) {
        if (materials == null) return;
        for (Material m : materials) {
            addBeeCombFlag(m);
        }
    }
}
