package net.phoenix.core.common.item.cinder;


import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.phoenix.core.PhoenixCore;

public class CinderRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister
            .create(BuiltInRegistries.RECIPE_SERIALIZER, PhoenixCore.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CinderCoreForgeRecipe>> CINDER_FORGE_CRAFTING = SERIALIZERS
            .register("cinder_core_forge", () -> new SimpleCraftingRecipeSerializer<>(CinderCoreForgeRecipe::new));

    public static void init() {}
}
