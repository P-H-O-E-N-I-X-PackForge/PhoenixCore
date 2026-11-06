package net.phoenix.core.integration.solaris;

import net.neoforged.fml.ModList;
import net.phoenix.core.configs.PhoenixConfigs;

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
