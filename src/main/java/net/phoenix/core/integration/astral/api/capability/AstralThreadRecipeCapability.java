package net.phoenix.core.integration.astral.api.capability;

import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;

import net.minecraft.resources.ResourceLocation;

public class AstralThreadRecipeCapability extends RecipeCapability<AstralThreadIngredient> {

    public static final AstralThreadRecipeCapability CAP = new AstralThreadRecipeCapability();

    protected AstralThreadRecipeCapability() {
        super(ResourceLocation.fromNamespaceAndPath("phoenixcore", "astral_thread"), 0xFF9C5CFF, false, 13,
                AstralThreadIngredient.Serializer.INSTANCE);
    }

    @Override
    public AstralThreadIngredient copyWithModifier(AstralThreadIngredient content, ContentModifier modifier) {
        return new AstralThreadIngredient((int) modifier.apply(content.getThread()));
    }

    @Override
    public AstralThreadIngredient copyInner(AstralThreadIngredient content) {
        return content.copy();
    }
}
