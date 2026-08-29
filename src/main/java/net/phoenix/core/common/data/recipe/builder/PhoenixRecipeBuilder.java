package net.phoenix.core.common.data.recipe.builder;

import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.common.data.recipeConditions.FluidInHatchCondition;

import lombok.experimental.Accessors;

import javax.annotation.ParametersAreNonnullByDefault;

@SuppressWarnings("unused")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true, fluent = true)
public class PhoenixRecipeBuilder extends GTRecipeBuilder {

    public PhoenixRecipeBuilder(ResourceLocation id, GTRecipeType recipeType) {
        super(id, recipeType);
    }

    public PhoenixRecipeBuilder plasmaTemp(ResourceLocation plasmaFluidId) {
        return (PhoenixRecipeBuilder) this.addCondition(
                FluidInHatchCondition.of(plasmaFluidId.toString()));
    }
}
