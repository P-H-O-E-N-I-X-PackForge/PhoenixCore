package net.phoenix.core.integration.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import net.phoenix.core.integration.gregvaults.common.registry.VaultRegistry;

@EmiEntrypoint
public class VaultEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.addRecipeHandler(VaultRegistry.VAULT_MENU.get(), new VaultRecipeHandler<>());
        registry.addRecipeHandler(VaultRegistry.VAULT_TERMINAL_MENU.get(), new VaultRecipeHandler<>());
    }
}
