package net.phoenix.core.integration.gregpacks.datagen;

import com.tterrag.registrate.providers.ProviderType;
import net.phoenix.core.integration.gregpacks.datagen.lang.GregPacksLangHandler;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public class GregPacksDatagen {

    public static void init() {
        REGISTRATE.addDataGenerator(ProviderType.LANG, GregPacksLangHandler::init);
    }
}
