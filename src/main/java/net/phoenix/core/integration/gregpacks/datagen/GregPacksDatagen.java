package net.phoenix.core.integration.gregpacks.datagen;

import net.phoenix.core.integration.gregpacks.datagen.lang.GregPacksLangHandler;

import com.tterrag.registrate.providers.ProviderType;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public class GregPacksDatagen {

    public static void init() {
        REGISTRATE.addDataGenerator(ProviderType.LANG, GregPacksLangHandler::init);
    }
}
