package net.phoenix.core.api.capability;

import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.phoenix.core.integration.ars_nouveau.api.capability.SourceRecipeCapability;
import net.phoenix.core.integration.astral.api.capability.AstralThreadRecipeCapability;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

@SuppressWarnings("all")
public class PhoenixRecipeCapabilities {

    public static final ShieldRecipeCapability SHIELDTYPES = ShieldRecipeCapability.CAP;
    public static final AstralThreadRecipeCapability ASTRAL_THREAD = AstralThreadRecipeCapability.CAP;

    public static void init() {
        register(SHIELDTYPES);
        register(SourceRecipeCapability.CAP);
        register(ASTRAL_THREAD);
    }

    private static void register(RecipeCapability<?> cap) {
        REGISTRATE.generic(cap.id.getPath(), GTRegistries.Keys.RECIPE_CAPABILITY, () -> cap).register();
    }
}
