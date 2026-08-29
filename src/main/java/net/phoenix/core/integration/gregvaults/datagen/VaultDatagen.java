package net.phoenix.core.integration.gregvaults.datagen;

import com.gregtechceu.gtceu.api.registry.registrate.provider.GTBlockstateProvider;

import net.phoenix.core.integration.gregvaults.common.recipe.GTVaultsRecipes;
import net.phoenix.core.integration.gregvaults.datagen.lang.VaultLangHandler;
import net.phoenix.core.integration.gregvaults.datagen.model.VaultModelProvider;

import com.tterrag.registrate.providers.ProviderType;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public class VaultDatagen {

    public static void init() {
        REGISTRATE.addDataGenerator(ProviderType.LANG, VaultLangHandler::init);
        REGISTRATE.addDataGenerator(ProviderType.RECIPE, GTVaultsRecipes::init);
        REGISTRATE.addDataGenerator(ProviderType.BLOCKSTATE,
                provider -> VaultModelProvider.init((GTBlockstateProvider) provider));
    }
}
