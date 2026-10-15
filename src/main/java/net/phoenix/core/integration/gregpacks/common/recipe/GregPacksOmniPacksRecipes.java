package net.phoenix.core.integration.gregpacks.common.recipe;

import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialEntry;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
import com.gregtechceu.gtceu.data.recipe.VanillaRecipeHelper;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.gregpacks.common.registry.GregPacksBlocks;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.L;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.*;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.ASSEMBLER_RECIPES;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.ASSEMBLY_LINE_RECIPES;

public class GregPacksOmniPacksRecipes {

    public static void init(Consumer<FinishedRecipe> provider) {
        VanillaRecipeHelper.addShapedRecipe(provider, false, PhoenixCore.id("basic_omnipack"),
                new ItemStack(GregPacksBlocks.BASIC_OMNIPACK_BLOCK.asItem()), "ABA", "CED", "ABA",
                'A', new MaterialEntry(plate, Rubber), 'B', new MaterialEntry(cableGtSingle, Tin),
                'C', GTMachines.BRONZE_CRATE.asStack(), 'D', GTMachines.BRONZE_DRUM.asStack(),
                'E', CustomTags.ULV_CIRCUITS);

        ASSEMBLER_RECIPES.recipeBuilder(PhoenixCore.id("advanced_omnipack"))
                .inputItems(CustomTags.MV_CIRCUITS)
                .inputItems(GTMachines.ALUMINIUM_CRATE)
                .inputItems(GTMachines.ALUMINIUM_DRUM)
                .inputItems(GTItems.ELECTRIC_PUMP_MV)
                .inputItems(GTItems.CONVEYOR_MODULE_MV)
                .inputItems(CustomTags.MV_BATTERIES, 2)
                .inputItems(plate, Polyethylene, 8)
                .inputItems(cableGtSingle, Copper, 4)
                .outputItems(GregPacksBlocks.ADVANCED_OMNIPACK_BLOCK)
                .circuitMeta(32)
                .duration(400)
                .EUt(120)
                .save(provider);

        ASSEMBLY_LINE_RECIPES.recipeBuilder(PhoenixCore.id("elite_omnipack"))
                .inputItems(CustomTags.IV_CIRCUITS, 2)
                .inputItems(GTMachines.TUNGSTENSTEEL_CRATE)
                .inputItems(GTMachines.TUNGSTENSTEEL_DRUM)
                .inputItems(CustomTags.IV_BATTERIES, 2)
                .inputItems(GTItems.CONVEYOR_MODULE_IV)
                .inputItems(GTItems.ELECTRIC_PUMP_IV)
                .inputItems(GTItems.CARBON_MESH, 16)
                .inputItems(cableGtSingle, Platinum, 8)
                .inputFluids(SolderingAlloy.getFluid(L * 4))
                .scannerResearch(GregPacksBlocks.ADVANCED_OMNIPACK_BLOCK.asStack())
                .outputItems(GregPacksBlocks.ELITE_OMNIPACK_BLOCK)
                .duration(400)
                .EUt(7680)
                .save(provider);
    }
}
