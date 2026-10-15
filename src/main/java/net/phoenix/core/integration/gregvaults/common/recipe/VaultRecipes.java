package net.phoenix.core.integration.gregvaults.common.recipe;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.ForgeRegistries;
import net.neoforged.neoforge.registries.RegistryObject;
import net.phoenix.core.PhoenixCore;

public class VaultRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister
            .create(ForgeRegistries.RECIPE_SERIALIZERS, PhoenixCore.MOD_ID);

    public static final RegistryObject<RecipeSerializer<EmitterUpgradeRecipe>> EMITTER_UPGRADE_SERIALIZER = SERIALIZERS
            .register("emitter_upgrade", EmitterUpgradeRecipe.Serializer::new);

    public static void init() {}
}
