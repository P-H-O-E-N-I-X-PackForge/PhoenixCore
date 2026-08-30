package net.phoenix.core.integration.conflux.dimension.worldgen;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class CustomVeinRegistry {

    private static final Map<String, CustomVeinType> CUSTOM_VEINS = new HashMap<>();

    public static void register(CustomVeinType veinType) {
        CUSTOM_VEINS.put(veinType.getName(), veinType);
        System.out.println("[PhoenixCore] Registered custom vein type: " + veinType.getName());
    }

    @Nullable
    public static CustomVeinType get(String name) {
        return CUSTOM_VEINS.get(name);
    }

    public static boolean exists(String name) {
        return CUSTOM_VEINS.containsKey(name);
    }

    public static Map<String, CustomVeinType> getAll() {
        return new HashMap<>(CUSTOM_VEINS);
    }

    public static void initializeDefaults() {
        
        register(new ClusterVeinType());
        register(new PillarVeinType());
        register(new ScatteredVeinType());
        register(new NetworkVeinType());
        register(new BlobVeinType());
    }
}
