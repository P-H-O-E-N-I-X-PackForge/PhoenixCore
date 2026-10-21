package net.phoenix.core;

import com.gregtechceu.gtceu.api.addon.GTAddon;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PhoenixFissionMachineRecipes;
import net.phoenix.core.common.data.PhoenixMachineRecipes;
import net.phoenix.core.common.data.PhoenixToolRecipes;
import net.phoenix.core.common.data.recipe.generated.*;
import net.phoenix.core.integration.ars_nouveau.common.data.recipe.SourceHatchRecipes;
import net.phoenix.core.integration.gregpacks.common.recipe.GregPacksModuleBaseRecipes;
import net.phoenix.core.integration.gregpacks.common.recipe.GregPacksModuleRecipes;
import net.phoenix.core.integration.gregpacks.common.recipe.GregPacksOmniPacksRecipes;
import net.phoenix.core.integration.gregvaults.common.recipe.GTVaultsRecipes;

@SuppressWarnings("unused")
@GTAddon(PhoenixCore.MOD_ID)
public class PhoenixGTAddon implements IGTAddon {

    @Override
    public GTRegistrate getRegistrate() {
        return PhoenixCore.PHOENIX_REGISTRATE;
    }

    @Override
    public void addRecipes(RecipeOutput provider) {
        PhoenixMachineRecipes.init(provider);
        PhoenixFissionMachineRecipes.init(provider);
        PhoenixToolRecipes.init(provider);
        PhoenixBeeRecipeGenerator.loadBeeRecipes(provider);
        net.phoenix.core.integration.tesla.TeslaRecipes.init(provider);
        CrystalRoseAssemblerGenerator.generateCrystalRoseRecipes(provider);
        net.phoenix.core.common.data.recipe.generated.UlvCoverRecipes.init(provider);
        SourceHatchRecipes.init(provider);
        GTVaultsRecipes.init(provider);
        GregPacksModuleBaseRecipes.init(provider);
        GregPacksModuleRecipes.init(provider);
        GregPacksOmniPacksRecipes.init(provider);
    }
}
