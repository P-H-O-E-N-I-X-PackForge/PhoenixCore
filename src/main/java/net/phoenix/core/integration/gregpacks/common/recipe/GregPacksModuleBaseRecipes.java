package net.phoenix.core.integration.gregpacks.common.recipe;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialEntry;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.data.recipe.VanillaRecipeHelper;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.gregpacks.common.item.GregPacksItems;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.L;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.*;

@SuppressWarnings("all")
public class GregPacksModuleBaseRecipes {

    public static void init(Consumer<FinishedRecipe> provider) {

        registerModuleBase(provider, "lv", GregPacksItems.LV_MODULE_BASE, GTMaterials.Steel);
        registerModuleBase(provider, "mv", GregPacksItems.MV_MODULE_BASE, GTMaterials.Aluminium);
        registerModuleBase(provider, "hv", GregPacksItems.HV_MODULE_BASE, GTMaterials.StainlessSteel);
        registerModuleBase(provider, "ev", GregPacksItems.EV_MODULE_BASE, GTMaterials.Titanium);
        registerModuleBase(provider, "iv", GregPacksItems.IV_MODULE_BASE, GTMaterials.TungstenSteel);
        registerModuleBase(provider, "luv", GregPacksItems.LUV_MODULE_BASE, GTMaterials.HSSE);

        VanillaRecipeHelper.addShapedRecipe(provider, true, PhoenixCore.id("module_casting_mold"),
                new ItemStack(GregPacksItems.MODULE_CASTING_MOLD),
                "P  ", "   ", "h  ", 'P', GTItems.SHAPE_EMPTY);
        FORMING_PRESS_RECIPES.recipeBuilder(PhoenixCore.id("copy_module_casting_mold"))
                .inputItems(GTItems.SHAPE_EMPTY)
                .notConsumable(GregPacksItems.MODULE_CASTING_MOLD)
                .outputItems(GregPacksItems.MODULE_CASTING_MOLD)
                .duration(120)
                .EUt(22)
                .save(provider);

        VanillaRecipeHelper.addShapedRecipe(provider, true, PhoenixCore.id("module_extruder_mold"),
                new ItemStack(GregPacksItems.MODULE_EXTRUDER_MOLD),
                "P  ", "   ", " h ", 'P', GTItems.SHAPE_EMPTY);
        FORMING_PRESS_RECIPES.recipeBuilder(PhoenixCore.id("copy_module_extruder_mold"))
                .inputItems(GTItems.SHAPE_EMPTY)
                .notConsumable(GregPacksItems.MODULE_EXTRUDER_MOLD)
                .outputItems(GregPacksItems.MODULE_EXTRUDER_MOLD)
                .duration(120)
                .EUt(22)
                .save(provider);
    }

    private static void registerModuleBase(Consumer<FinishedRecipe> provider, String tier,
                                           ItemLike output, Material material) {
        VanillaRecipeHelper.addShapedRecipe(provider, false, PhoenixCore.id(tier + "_module_base"),
                new ItemStack(output), "   ", "PPf", "PPh",
                'P', new MaterialEntry(TagPrefix.plate, material));
        EXTRUDER_RECIPES.recipeBuilder(PhoenixCore.id("module_base_" + tier))
                .inputItems(TagPrefix.ingot, material, 4)
                .notConsumable(GregPacksItems.MODULE_EXTRUDER_MOLD)
                .outputItems(output)
                .duration((int) material.getMass() * 2)
                .EUt(24)
                .addMaterialInfo(true)
                .save(provider);
        FLUID_SOLIDFICATION_RECIPES.recipeBuilder(PhoenixCore.id("module_base_" + tier))
                .inputFluids(material.getFluid(L * 4))
                .notConsumable(GregPacksItems.MODULE_CASTING_MOLD)
                .outputItems(output)
                .duration(200)
                .EUt(24)
                .addMaterialInfo(true)
                .save(provider);
    }
}
