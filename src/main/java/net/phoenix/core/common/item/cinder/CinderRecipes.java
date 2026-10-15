package net.phoenix.core.common.item.cinder;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.phoenix.core.PhoenixCore;

public class CinderRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister
            .create(ForgeRegistries.RECIPE_SERIALIZERS, PhoenixCore.MOD_ID);

    public static final RegistryObject<RecipeSerializer<CinderCoreForgeRecipe>> CINDER_FORGE_CRAFTING = SERIALIZERS
            .register("cinder_core_forge", () -> new SimpleCraftingRecipeSerializer<>(CinderCoreForgeRecipe::new));

    public static void init() {}
}
