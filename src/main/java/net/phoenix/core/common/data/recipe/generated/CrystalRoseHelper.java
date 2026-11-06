package net.phoenix.core.common.data.recipe.generated;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.registry.registrate.entry.MaterialRegistryEntry;

import net.phoenix.core.common.data.materials.PhoenixMaterialFlags;

public class CrystalRoseHelper {

    public static void addCrystalRoseFlag(Material material) {
        if (material == null) {
            return;
        }

        material.addFlags(PhoenixMaterialFlags.GENERATE_CRYSTAL_ROSE);
    }

    public static void addCrystalRoseFlag(MaterialRegistryEntry entry) {
        if (entry == null) {
            return;
        }
        addCrystalRoseFlag(entry.get());
    }

    public static void addCrystalRoseFlags(Object... materials) {
        if (materials == null) return;
        for (Object obj : materials) {
            if (obj instanceof Material material) {
                addCrystalRoseFlag(material);
            } else if (obj instanceof MaterialRegistryEntry entry) {
                addCrystalRoseFlag(entry);
            }
        }
    }
}
