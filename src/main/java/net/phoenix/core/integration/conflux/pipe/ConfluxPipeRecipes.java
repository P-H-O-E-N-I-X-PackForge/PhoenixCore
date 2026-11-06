package net.phoenix.core.integration.conflux.pipe;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.integration.conflux.ConfluxDataType;
import net.phoenix.core.integration.conflux.ConfluxRegistry;

import static com.gregtechceu.gtceu.api.GTValues.LV;
import static com.gregtechceu.gtceu.api.GTValues.VA;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.plate;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.wireFine;
import static com.gregtechceu.gtceu.common.data.GTMaterials.Copper;
import static com.gregtechceu.gtceu.common.data.GTMaterials.Steel;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.ASSEMBLER_RECIPES;

/**
 * Data pipes: an assembler recipe for each type. The programmed circuit picks the type (1 to 5, in the order of the
 * types), the same way GregTech tells apart recipes that share their ingredients.
 */
public final class ConfluxPipeRecipes {

    private ConfluxPipeRecipes() {}

    public static void init(RecipeOutput provider) {
        for (ConfluxDataType type : ConfluxDataType.values()) {
            ASSEMBLER_RECIPES.recipeBuilder("conflux_" + type.id() + "_data_pipe")
                    .inputItems(plate, Steel, 2)
                    .inputItems(wireFine, Copper, 4)
                    .circuitMeta(type.ordinal() + 1)
                    .outputItems(new ItemStack(ConfluxRegistry.PIPES.get(type).get(), 4))
                    .duration(100)
                    .EUt(VA[LV])
                    .save(provider);
        }
    }
}
