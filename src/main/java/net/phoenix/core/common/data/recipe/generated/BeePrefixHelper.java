package net.phoenix.core.common.data.recipe.generated;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.registry.registrate.entry.MaterialRegistryEntry;

import net.phoenix.core.common.data.materials.PhoenixMaterialFlags;

public class BeePrefixHelper {

    public static void addTierOneBeeFlag(Material material) {
        if (material == null) {
            return;
        }
        material.addFlags(PhoenixMaterialFlags.GENERATE_TIER_ONE_BEE);
    }

    public static void addTierOneBeeFlag(MaterialRegistryEntry entry) {
        if (entry == null) {
            return;
        }
        addTierOneBeeFlag(entry.get());
    }

    public static void addTierOneBeeFlags(Object... materials) {
        if (materials == null) return;
        for (Object obj : materials) {
            if (obj instanceof Material material) {
                addTierOneBeeFlag(material);
            } else if (obj instanceof MaterialRegistryEntry entry) {
                addTierOneBeeFlag(entry);
            }
        }
    }

    public static void addTierTwoBeeFlag(Material material) {
        if (material == null) {
            return;
        }
        material.addFlags(PhoenixMaterialFlags.GENERATE_TIER_TWO_BEE);
    }

    public static void addTierTwoBeeFlag(MaterialRegistryEntry entry) {
        if (entry == null) {
            return;
        }
        addTierTwoBeeFlag(entry.get());
    }

    public static void addTierTwoBeeFlags(Object... materials) {
        if (materials == null) return;
        for (Object obj : materials) {
            if (obj instanceof Material material) {
                addTierTwoBeeFlag(material);
            } else if (obj instanceof MaterialRegistryEntry entry) {
                addTierTwoBeeFlag(entry);
            }
        }
    }

    public static void addBeeCombFlag(Material material) {
        if (material == null) {
            return;
        }
        material.addFlags(PhoenixMaterialFlags.GENERATE_BEE_COMB);
    }

    public static void addBeeCombFlag(MaterialRegistryEntry entry) {
        if (entry == null) {
            return;
        }
        addBeeCombFlag(entry.get());
    }

    public static void addBeeCombFlags(Object... materials) {
        if (materials == null) return;
        for (Object obj : materials) {
            if (obj instanceof Material material) {
                addBeeCombFlag(material);
            } else if (obj instanceof MaterialRegistryEntry entry) {
                addBeeCombFlag(entry);
            }
        }
    }
}
