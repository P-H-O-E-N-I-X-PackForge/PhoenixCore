package net.phoenix.core.integration.emi;

import brachy.modularui.ModularUIMenuTypes;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;

@EmiEntrypoint
public class VaultEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.addRecipeHandler(ModularUIMenuTypes.MODULAR_CONTAINER.get(), new VaultRecipeHandler<>());
    }
}
