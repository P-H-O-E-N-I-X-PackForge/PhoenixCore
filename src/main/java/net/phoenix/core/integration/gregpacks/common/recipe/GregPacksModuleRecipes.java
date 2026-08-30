package net.phoenix.core.integration.gregpacks.common.recipe;

import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialEntry;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
import com.gregtechceu.gtceu.data.recipe.VanillaRecipeHelper;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.gregpacks.common.item.GregPacksItems;
import net.phoenix.core.integration.gregpacks.common.registry.GregPacksUpgrades;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.*;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.ASSEMBLER_RECIPES;

@SuppressWarnings("all")
public class GregPacksModuleRecipes {

    public static void init(Consumer<FinishedRecipe> provider) {
        
        VanillaRecipeHelper.addShapedRecipe(provider, true,
                PhoenixCore.id("item_module_1"), new ItemStack(GregPacksUpgrades.ITEM_CAPACITY_I),
                "ABA", "CDC", "ABA",
                'A', new MaterialEntry(cableGtSingle, Tin),
                'B', GTMachines.WOODEN_CRATE.asStack(),
                'C', GTItems.CONVEYOR_MODULE_LV,
                'D', GregPacksItems.LV_MODULE_BASE);

        ASSEMBLER_RECIPES.recipeBuilder(PhoenixCore.id("item_module_2"))
                .inputItems(GregPacksItems.HV_MODULE_BASE)
                .inputItems(CustomTags.HV_CIRCUITS)
                .inputItems(GTMachines.STAINLESS_STEEL_CRATE)
                .inputItems(GTItems.CONVEYOR_MODULE_HV)
                .inputItems(cableGtSingle, Gold, 4)
                .outputItems(GregPacksUpgrades.ITEM_CAPACITY_II)
                .circuitMeta(32)
                .duration(100)
                .EUt(480)
                .addMaterialInfo(true)
                .save(provider);

        ASSEMBLER_RECIPES.recipeBuilder(PhoenixCore.id("item_module_3"))
                .inputItems(GregPacksItems.IV_MODULE_BASE)
                .inputItems(CustomTags.IV_CIRCUITS)
                .inputItems(GTMachines.TUNGSTENSTEEL_CRATE)
                .inputItems(GTItems.CONVEYOR_MODULE_IV)
                .inputItems(cableGtSingle, Platinum, 4)
                .outputItems(GregPacksUpgrades.ITEM_CAPACITY_III)
                .circuitMeta(32)
                .duration(100)
                .EUt(7680)
                .addMaterialInfo(true)
                .save(provider);

        VanillaRecipeHelper.addShapedRecipe(provider, true,
                PhoenixCore.id("fluid_module_1"), new ItemStack(GregPacksUpgrades.FLUID_CAPACITY_I),
                "ABA", "CDC", "ABA",
                'A', new MaterialEntry(cableGtSingle, Tin),
                'B', GTMachines.WOODEN_DRUM.asStack(),
                'C', GTItems.ELECTRIC_PUMP_LV,
                'D', GregPacksItems.LV_MODULE_BASE);

        ASSEMBLER_RECIPES.recipeBuilder(PhoenixCore.id("fluid_module_2"))
                .inputItems(GregPacksItems.EV_MODULE_BASE)
                .inputItems(CustomTags.EV_CIRCUITS)
                .inputItems(GTMachines.TITANIUM_DRUM)
                .inputItems(GTItems.ELECTRIC_PUMP_EV)
                .inputItems(cableGtSingle, Aluminium, 4)
                .outputItems(GregPacksUpgrades.FLUID_CAPACITY_II)
                .circuitMeta(32)
                .duration(100)
                .EUt(1920)
                .addMaterialInfo(true)
                .save(provider);

        ASSEMBLER_RECIPES.recipeBuilder(PhoenixCore.id("fluid_module_3"))
                .inputItems(GregPacksItems.LUV_MODULE_BASE)
                .inputItems(CustomTags.LuV_CIRCUITS)
                .inputItems(GTMachines.TUNGSTENSTEEL_DRUM)
                .inputItems(GTItems.ELECTRIC_PUMP_LuV)
                .inputItems(cableGtSingle, NiobiumTitanium, 4)
                .outputItems(GregPacksUpgrades.FLUID_CAPACITY_III)
                .circuitMeta(32)
                .duration(100)
                .EUt(30720)
                .addMaterialInfo(true)
                .save(provider);

        VanillaRecipeHelper.addShapedRecipe(provider, true,
                PhoenixCore.id("energy_module_1"), new ItemStack(GregPacksUpgrades.ENERGY_CAPACITY_I),
                "ABA", "CDC", "ABA",
                'A', new MaterialEntry(cableGtSingle, Tin),
                'B', new MaterialEntry(plate, BatteryAlloy),
                'C', CustomTags.LV_BATTERIES,
                'D', GregPacksItems.LV_MODULE_BASE);

        ASSEMBLER_RECIPES.recipeBuilder(PhoenixCore.id("energy_module_2"))
                .inputItems(GregPacksItems.EV_MODULE_BASE)
                .inputItems(CustomTags.EV_CIRCUITS)
                .inputItems(CustomTags.EV_BATTERIES)
                .inputItems(plate, BatteryAlloy)
                .inputItems(cableGtSingle, Aluminium, 4)
                .outputItems(GregPacksUpgrades.ENERGY_CAPACITY_II)
                .circuitMeta(32)
                .duration(100)
                .EUt(1920)
                .addMaterialInfo(true)
                .save(provider);

        ASSEMBLER_RECIPES.recipeBuilder(PhoenixCore.id("energy_module_3"))
                .inputItems(GregPacksItems.LUV_MODULE_BASE)
                .inputItems(CustomTags.LuV_CIRCUITS)
                .inputItems(CustomTags.LuV_BATTERIES)
                .inputItems(plate, BatteryAlloy)
                .inputItems(cableGtSingle, NiobiumTitanium, 4)
                .outputItems(GregPacksUpgrades.ENERGY_CAPACITY_III)
                .circuitMeta(32)
                .duration(100)
                .EUt(30720)
                .addMaterialInfo(true)
                .save(provider);

        VanillaRecipeHelper.addShapedRecipe(provider, true,
                PhoenixCore.id("magnet_module_1"), new ItemStack(GregPacksUpgrades.MAGNET_MODULE_I),
                "ABA", "CDC", "ABA",
                'A', new MaterialEntry(rod, SteelMagnetic),
                'B', new MaterialEntry(plate, Gold),
                'C', new MaterialEntry(cableGtSingle, Tin),
                'D', GregPacksItems.LV_MODULE_BASE);

        ASSEMBLER_RECIPES.recipeBuilder(PhoenixCore.id("magnet_module_2"))
                .inputItems(GregPacksUpgrades.MAGNET_MODULE_I)
                .inputItems(GregPacksItems.MV_MODULE_BASE)
                .inputItems(GTItems.FIELD_GENERATOR_LV)
                .inputItems(cableGtSingle, Copper, 2)
                .outputItems(GregPacksUpgrades.MAGNET_MODULE_II)
                .circuitMeta(32)
                .duration(200)
                .EUt(30)
                .addMaterialInfo(true)
                .save(provider);

        VanillaRecipeHelper.addShapedRecipe(provider, true,
                PhoenixCore.id("feeding_module"), new ItemStack(GregPacksUpgrades.FEEDING_MODULE),
                "ABA", "CDC", "ABA",
                'A', new MaterialEntry(cableGtSingle, Tin),
                'B', new MaterialEntry(rod, Iron),
                'C', GTItems.ROBOT_ARM_LV,
                'D', GregPacksItems.LV_MODULE_BASE);

        VanillaRecipeHelper.addShapedRecipe(provider, true,
                PhoenixCore.id("maintenance_module"), new ItemStack(GregPacksUpgrades.MAINTENANCE_MODULE),
                "ABA", "CDC", "ABA",
                'A', CustomTags.LV_CIRCUITS,
                'B', new MaterialEntry(cableGtSingle, Tin),
                'C', GTItems.ROBOT_ARM_LV,
                'D', GregPacksItems.LV_MODULE_BASE);

        ASSEMBLER_RECIPES.recipeBuilder(PhoenixCore.id("jetpack_module_1"))
                .inputItems(GregPacksItems.HV_MODULE_BASE)
                .inputItems(CustomTags.HV_CIRCUITS, 2)
                .inputItems(GTItems.POWER_THRUSTER, 2)
                .inputItems(cableGtSingle, Gold, 2)
                .outputItems(GregPacksUpgrades.JETPACK_MODULE_I)
                .circuitMeta(32)
                .duration(200)
                .EUt(120)
                .addMaterialInfo(true)
                .save(provider);

        ASSEMBLER_RECIPES.recipeBuilder(PhoenixCore.id("jetpack_module_2"))
                .inputItems(GregPacksUpgrades.JETPACK_MODULE_I)
                .inputItems(GregPacksItems.IV_MODULE_BASE)
                .inputItems(CustomTags.LuV_CIRCUITS, 2)
                .inputItems(GTItems.POWER_THRUSTER_ADVANCED, 2)
                .inputItems(cableGtSingle, Platinum, 2)
                .outputItems(GregPacksUpgrades.JETPACK_MODULE_II)
                .circuitMeta(32)
                .duration(200)
                .EUt(7680)
                .addMaterialInfo(true)
                .save(provider);

        VanillaRecipeHelper.addShapelessRecipe(provider, PhoenixCore.id("crafting_module"),
                new ItemStack(GregPacksUpgrades.CRAFTING_MODULE), GregPacksItems.LV_MODULE_BASE, Blocks.CRAFTING_TABLE);

        ASSEMBLER_RECIPES.recipeBuilder(PhoenixCore.id("processing_module"))
                .inputItems(GregPacksItems.HV_MODULE_BASE)
                .inputItems(CustomTags.EV_CIRCUITS, 2)
                .inputItems(GTItems.ROBOT_ARM_LV, 2)
                .inputItems(cableGtSingle, Gold, 4)
                .outputItems(GregPacksUpgrades.PROCESSING_MODULE)
                .circuitMeta(32)
                .duration(200)
                .EUt(480)
                .addMaterialInfo(true)
                .save(provider);
    }
}
