package net.phoenix.core.integration.gregvaults.datagen;

import com.gregtechceu.gtceu.api.registry.registrate.provider.GTBlockstateProvider;
import com.tterrag.registrate.providers.ProviderType;
import net.phoenix.core.integration.gregvaults.common.recipe.GTVaultsRecipes;
import net.phoenix.core.integration.gregvaults.datagen.lang.VaultLangHandler;
import net.phoenix.core.integration.gregvaults.datagen.model.VaultModelProvider;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public class VaultDatagen {

    public static void init() {
        REGISTRATE.addDataGenerator(ProviderType.LANG, VaultLangHandler::init);
        REGISTRATE.addDataGenerator(ProviderType.RECIPE, GTVaultsRecipes::init);
        REGISTRATE.addDataGenerator(ProviderType.BLOCKSTATE,
                provider -> VaultModelProvider.init((GTBlockstateProvider) provider));
    }
}
