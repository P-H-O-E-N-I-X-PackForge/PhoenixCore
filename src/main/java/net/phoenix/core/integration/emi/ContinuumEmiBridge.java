package net.phoenix.core.integration.emi;

import dev.emi.emi.runtime.EmiReloadManager;

/**
 * Asks EMI to rebuild its recipes. Continuum's bodies reach the client after EMI has already built its pages (they are
 * synced when the player joins), so the Continuum pages are rebuilt once that data has arrived. Only ever loaded when EMI
 * is installed.
 */
public final class ContinuumEmiBridge {

    private ContinuumEmiBridge() {}

    public static void reload() {
        EmiReloadManager.reloadRecipes();
    }
}
