package net.phoenix.core.common.data.recipe.generated;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;

import net.phoenix.core.common.data.materials.PhoenixMaterialFlags;

public class CrystalRoseHelper {

    public static void addCrystalRoseFlag(Material material) {
        if (material == null || material.isNull()) {
            return;
        }

        material.addFlags(PhoenixMaterialFlags.GENERATE_CRYSTAL_ROSE);
    }

    public static void addCrystalRoseFlags(Material... materials) {
        if (materials == null) return;
        for (Material m : materials) {
            addCrystalRoseFlag(m);
        }
    }
}
