package net.phoenix.core.integration.emi;

import dev.emi.emi.runtime.EmiReloadManager;

public final class ContinuumEmiBridge {

    private ContinuumEmiBridge() {}

    public static void reload() {
        EmiReloadManager.reloadRecipes();
    }
}
