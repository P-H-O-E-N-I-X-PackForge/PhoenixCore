package net.phoenix.core.common.data.recipe.pft;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.common.data.PftRecipe;
import net.phoenix.core.common.data.materials.PhoenixMaterialFlags;

import org.jetbrains.annotations.Nullable;

public final class PftLapidaryRecipes {

    private PftLapidaryRecipes() {}

    public static final String[] CUTS = { "pear", "round", "cushion", "princess", "emerald", "heart", "marquise",
            "oval" };

    public static Material[] gems() {
        return new Material[] { GTMaterials.Diamond.get(), GTMaterials.Emerald.get(), GTMaterials.Ruby.get(),
                GTMaterials.Sapphire.get(), GTMaterials.GreenSapphire.get(), GTMaterials.Amethyst.get(),
                GTMaterials.Topaz.get(), GTMaterials.BlueTopaz.get(), GTMaterials.Opal.get() };
    }

    public static TagPrefix[] cutPrefixes() {
        return new TagPrefix[] { PhoenixMaterialFlags.pear_cut.get(), PhoenixMaterialFlags.round_cut.get(),
                PhoenixMaterialFlags.cushion_cut.get(), PhoenixMaterialFlags.princess_cut.get(),
                PhoenixMaterialFlags.emerald_cut.get(), PhoenixMaterialFlags.heart_cut.get(),
                PhoenixMaterialFlags.marquise_cut.get(), PhoenixMaterialFlags.oval_cut.get() };
    }

    public static @Nullable String itemId(TagPrefix prefix, Material material) {
        ItemStack stack = ChemicalHelper.get(prefix, material, 1);
        return stack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public static void init(RecipeOutput provider) {
        TagPrefix[] cuts = cutPrefixes();
        for (Material gem : gems()) {
            String input = itemId(TagPrefix.gem.get(), gem);
            if (input == null) continue;
            for (int i = 0; i < CUTS.length; i++) {
                String output = itemId(cuts[i], gem);
                if (output == null) continue;
                PftRecipe.gt(provider, "lapidary", gem.getName() + "_" + CUTS[i] + "_cut")
                        .itemInputs(input)
                        .circuit(i + 1)
                        .inputFluids("minecraft:water 100")
                        .itemOutputs(output)
                        .duration(160)
                        .EUt(GTValues.VA[GTValues.LV])
                        .save();
            }
        }
    }
}
