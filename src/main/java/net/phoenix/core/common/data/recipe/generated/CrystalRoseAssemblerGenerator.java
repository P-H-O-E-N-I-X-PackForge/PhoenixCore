package net.phoenix.core.common.data.recipe.generated;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialEntry;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.phoenix.core.common.data.bees.BeeRecipeData;
import net.phoenix.core.common.data.materials.PhoenixMaterialFlags;

import java.util.function.Consumer;

import static net.phoenix.core.common.data.materials.PhoenixFissionMaterials.CRYO_GRAPHITE_BINDING_SOLUTION;

public class CrystalRoseAssemblerGenerator {

    public static void generateCrystalRoseRecipes(Consumer<FinishedRecipe> provider) {
        BeeRecipeData.ALL_BEE_CONFIGS.forEach((id, config) -> {

            ItemStack output = config.finalOutputItem();
            if (output == null || output.isEmpty()) return;

            Material material = tryGetMaterialFromOutput(output);
            if (material == null || material.isNull()) return;

            ItemStack inputStack = getPreferredInputForMaterial(material);
            if (inputStack.isEmpty()) return;
            inputStack.setCount(4);

            ItemStack roseStack = ChemicalHelper.get(PhoenixMaterialFlags.crystal_rose, material, 1);
            if (roseStack.isEmpty()) return;

            FluidStack crystalRoseFluid = CRYO_GRAPHITE_BINDING_SOLUTION.getFluid(144);

            GTRecipeBuilder builder = GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder(
                    "phoenixcore:crystal_rose_" + material.getName())
                    .EUt(GTValues.V[GTValues.IV])
                    .duration(200)
                    .inputItems(inputStack)
                    .inputFluids(crystalRoseFluid)
                    .outputItems(roseStack);

            builder.save(provider);
        });
    }

    public static void linkCrystalRoseFlags() {
        BeeRecipeData.ALL_BEE_CONFIGS.forEach((id, config) -> {

            ItemStack output = config.finalOutputItem();
            if (output == null || output.isEmpty()) return;

            Material material = tryGetMaterialFromOutput(output);
            if (material == null || material.isNull()) return;

            material.addFlags(PhoenixMaterialFlags.GENERATE_CRYSTAL_ROSE);
        });
    }

    private static Material tryGetMaterialFromOutput(ItemStack stack) {
        MaterialEntry entry = ChemicalHelper.getMaterialEntry(stack.getItem());
        if (entry == null || entry.isEmpty()) return null;

        Material mat = entry.material();
        if (mat == null || mat.isNull()) return null;

        return mat;
    }

    private static ItemStack getPreferredInputForMaterial(Material material) {
        if (!TagPrefix.rawOre.isIgnored(material)) {
            ItemStack raw = ChemicalHelper.get(TagPrefix.rawOre, material, 1);
            if (!raw.isEmpty()) return raw;
        }

        if (!TagPrefix.dust.isIgnored(material)) {
            ItemStack dust = ChemicalHelper.get(TagPrefix.dust, material, 1);
            if (!dust.isEmpty()) return dust;
        }

        if (!TagPrefix.gem.isIgnored(material)) {
            ItemStack gem = ChemicalHelper.get(TagPrefix.gem, material, 1);
            if (!gem.isEmpty()) return gem;
        }

        return ItemStack.EMPTY;
    }
}
