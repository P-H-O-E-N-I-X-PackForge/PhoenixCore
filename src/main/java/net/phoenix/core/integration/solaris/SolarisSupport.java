package net.phoenix.core.integration.solaris;

import net.neoforged.fml.ModList;
import net.phoenix.core.configs.PhoenixConfigs;

/**
 * Soft link to Solaris (the map). Solaris is a compile-only dependency, so nothing outside the
 * {@code *Bridge} classes in this package may name a Solaris type: those are only reached after
 * {@link #isActive()} says the mod is loaded and the integration is switched on.
 */
public final class SolarisSupport {

    public static final String MOD_ID = "solaris";

    private SolarisSupport() {}

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static boolean isActive() {
        return isLoaded() && PhoenixConfigs.INSTANCE != null && PhoenixConfigs.INSTANCE.confluxSolaris.enabled;
    }
}
