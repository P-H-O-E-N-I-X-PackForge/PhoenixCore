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

        hatch(provider, "conflux_data_input_hatch",
                net.phoenix.core.integration.conflux.hatch.ConfluxHatches.DATA_INPUT);
        hatch(provider, "conflux_data_output_hatch",
                net.phoenix.core.integration.conflux.hatch.ConfluxHatches.DATA_OUTPUT);
    }

    private static void hatch(RecipeOutput provider, String id,
                              com.gregtechceu.gtceu.api.registry.registrate.entry.MachineEntry<com.gregtechceu.gtceu.api.machine.MachineDefinition>[] entries) {
        var machine = entries == null ? null : entries[com.gregtechceu.gtceu.api.GTValues.IV];
        if (machine == null) return;
        var builder = ASSEMBLER_RECIPES.recipeBuilder(id)
                .inputItems(com.gregtechceu.gtceu.common.data.GTMachines.HULL[com.gregtechceu.gtceu.api.GTValues.IV]
                        .asStack())
                .inputItems(com.gregtechceu.gtceu.data.recipe.CustomTags.IV_CIRCUITS, 2);
        for (ConfluxDataType type : ConfluxDataType.values()) {
            builder = builder.inputItems(ConfluxRegistry.PIPES.get(type).get(), 1);
        }
        builder.outputItems(machine.asStack())
                .duration(300)
                .EUt(VA[com.gregtechceu.gtceu.api.GTValues.IV])
                .save(provider);
    }
}
