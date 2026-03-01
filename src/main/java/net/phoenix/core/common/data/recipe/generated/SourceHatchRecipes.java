 package net.phoenix.core.common.data.recipe.generated;

 import com.gregtechceu.gtceu.api.GTValues;
 import com.gregtechceu.gtceu.common.data.GTMachines;
 import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
 import net.minecraft.data.recipes.FinishedRecipe;
 import net.minecraft.resources.ResourceLocation;
 import net.minecraftforge.registries.ForgeRegistries;
 import net.phoenix.core.common.machine.PhoenixMachines;
 import net.phoenix.core.common.machine.PhoenixTeslaMachines;
 import org.jetbrains.annotations.NotNull;

 import java.util.function.Consumer;

 import static com.gregtechceu.gtceu.api.GTValues.VN;
 import static com.hollingsworth.arsnouveau.setup.registry.BlockRegistry.SOURCE_JAR;
 import static net.phoenix.core.common.data.recipe.generated.CustomComponetRecipes.*;
 import static net.phoenix.core.common.data.recipe.generated.TeslaHatchRecipes.*;

 public class SourceHatchRecipes {
    private static final int[] VA = GTValues.VA;
     public static void init(@NotNull Consumer<FinishedRecipe> provider) {
run(provider);
     }

     public static void run(@NotNull Consumer<FinishedRecipe> provider) {
         for (int tier = GTValues.LV; tier <= GTValues.OpV; tier++) {
             processSourceHatches(provider, tier);
         }
     }

     private static void processSourceHatches(@NotNull Consumer<FinishedRecipe> provider, int tier) {
         var sourceStone = ForgeRegistries.ITEMS.getValue(new ResourceLocation("ars_nouveau", "sourcestone"));
         GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("source_hatch_output" + VN[tier].toLowerCase())
                 .inputItems(getSmallGearForTier(tier), 4)
                 .inputItems(GTMachines.HULL[tier])
                 .inputItems(sourceStone, 8)
                 .inputItems(SOURCE_JAR)
                 .inputItems(getDrumForTier(tier), 2)
                 .inputItems(getCircuitForTier(tier), 2)
                 .outputItems(PhoenixMachines.SOURCE_EXPORT_HATCH[tier])
                 .duration(100)
                 .EUt(VA[tier])
                 .save(provider);
         GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("source_hatch_input" + VN[tier].toLowerCase())
                 .inputItems(getGearForTier(tier), 2)
                 .inputItems(GTMachines.HULL[tier])
                 .inputItems(sourceStone, 4)
                 .inputItems(SOURCE_JAR)
                 .inputItems(getDrumForTier(tier))
                 .inputItems(getCircuitForTier(tier), 2)
                 .outputItems(PhoenixMachines.SOURCE_IMPORT_HATCH[tier])
                 .duration(100)
                 .EUt(VA[tier])
                 .save(provider);
     }

}