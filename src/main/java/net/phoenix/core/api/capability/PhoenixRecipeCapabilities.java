package net.phoenix.core.api.capability;

import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.phoenix.core.integration.ars_nouveau.api.capability.SourceRecipeCapability;
import net.phoenix.core.integration.astral.api.capability.AstralThreadRecipeCapability;

@SuppressWarnings("all")
public class PhoenixRecipeCapabilities {

    public static final ShieldRecipeCapability SHIELDTYPES = ShieldRecipeCapability.CAP;
    public static final AstralThreadRecipeCapability ASTRAL_THREAD = AstralThreadRecipeCapability.CAP;

    public static void init() {
        GTRegistries.RECIPE_CAPABILITIES.register(SHIELDTYPES.id, SHIELDTYPES);
        GTRegistries.RECIPE_CAPABILITIES.register(SourceRecipeCapability.CAP.id, SourceRecipeCapability.CAP);
        GTRegistries.RECIPE_CAPABILITIES.register(ASTRAL_THREAD.id, ASTRAL_THREAD);
    }
}
