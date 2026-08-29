package net.phoenix.core.integration.conflux.dimension.worldgen;

import net.minecraftforge.fml.ModList;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.*;

public class GTVeinAPI {

    private static final boolean GT_LOADED = ModList.get().isLoaded("gtceu");
    private static GTVeinManager veinManager;

    static {
        if (GT_LOADED) {
            try {
                veinManager = new GTVeinManager();
            } catch (Exception e) {
                System.err.println("[PhoenixCore] Failed to initialize GT Vein API: " + e.getMessage());
            }
        }
    }

    public static boolean isAvailable() {
        return GT_LOADED && veinManager != null;
    }

    @Nullable
    public static GTVeinManager getManager() {
        return veinManager;
    }

    public static class VeinBuilder {

        private String veinName;
        private String primaryMaterial;
        private String secondaryMaterial;
        private int veinsPerChunk = 1;
        private int minY = 0;
        private int maxY = 256;
        private float density = 1.0f;
        private boolean enabled = true;
        private final Map<String, String> properties = new HashMap<>();

        public VeinBuilder(String veinName) {
            this.veinName = veinName;
        }

        public VeinBuilder primaryMaterial(String material) {
            this.primaryMaterial = material;
            return this;
        }

        public VeinBuilder secondaryMaterial(String material) {
            this.secondaryMaterial = material;
            return this;
        }

        public VeinBuilder veinsPerChunk(int count) {
            this.veinsPerChunk = count;
            return this;
        }

        public VeinBuilder yRange(int min, int max) {
            this.minY = min;
            this.maxY = max;
            return this;
        }

        public VeinBuilder density(float density) {
            this.density = density;
            return this;
        }

        public VeinBuilder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public VeinBuilder withProperty(String key, String value) {
            this.properties.put(key, value);
            return this;
        }

        public VeinConfiguration build() {
            return new VeinConfiguration(
                    veinName, primaryMaterial, secondaryMaterial,
                    veinsPerChunk, minY, maxY, density, enabled, properties);
        }
    }

    public static class VeinConfiguration {

        public final String veinName;
        public final String primaryMaterial;
        public final String secondaryMaterial;
        public final int veinsPerChunk;
        public final int minY;
        public final int maxY;
        public final float density;
        public final boolean enabled;
        public final Map<String, String> properties;

        public VeinConfiguration(
                                 String veinName,
                                 String primaryMaterial,
                                 String secondaryMaterial,
                                 int veinsPerChunk,
                                 int minY,
                                 int maxY,
                                 float density,
                                 boolean enabled,
                                 Map<String, String> properties) {
            this.veinName = veinName;
            this.primaryMaterial = primaryMaterial;
            this.secondaryMaterial = secondaryMaterial;
            this.veinsPerChunk = veinsPerChunk;
            this.minY = minY;
            this.maxY = maxY;
            this.density = density;
            this.enabled = enabled;
            this.properties = new HashMap<>(properties);
        }

        public static VeinConfiguration fromGTVein(String veinName) {
            if (veinManager != null) {
                return veinManager.queryVeinConfiguration(veinName);
            }
            return null;
        }
    }

    public static class VeinFilter {

        private String namePattern;
        private Integer minYFilter;
        private Integer maxYFilter;
        private String materialFilter;
        private Boolean enabledFilter;

        public VeinFilter withNamePattern(String pattern) {
            this.namePattern = pattern;
            return this;
        }

        public VeinFilter inYRange(int min, int max) {
            this.minYFilter = min;
            this.maxYFilter = max;
            return this;
        }

        public VeinFilter withMaterial(String material) {
            this.materialFilter = material;
            return this;
        }

        public VeinFilter enabled(boolean enabled) {
            this.enabledFilter = enabled;
            return this;
        }

        public List<VeinConfiguration> query() {
            if (veinManager == null) return new ArrayList<>();
            return veinManager.queryVeins(this);
        }
    }

    public static class GTVeinManager {

        private final Class<?> veinWorldHandlerClass;
        private final Method getVeinWorldMethod;
        private final Method placeVeinsMethod;
        private final Method getMaterialMethod;
        private final Method registerVeinMethod;

        GTVeinManager() throws Exception {
            this.veinWorldHandlerClass = Class.forName(
                    "com.gregtechceu.gtceu.api.data.chemical.material.properties.VeinWorldHandler");

            this.getVeinWorldMethod = veinWorldHandlerClass.getMethod("getVeinWorld");
            this.placeVeinsMethod = veinWorldHandlerClass.getMethod(
                    "placeVeins",
                    net.minecraft.world.level.WorldGenLevel.class,
                    net.minecraft.util.RandomSource.class,
                    int.class,
                    int.class,
                    String[].class);

            try {
                this.getMaterialMethod = veinWorldHandlerClass.getMethod(
                        "getMaterial", String.class);
            } catch (NoSuchMethodException e) {
                throw new Exception("GT version incompatible: getMaterial method not found", e);
            }

            Method registerMethod = null;
            try {
                registerMethod = veinWorldHandlerClass.getMethod(
                        "registerVein", String.class, VeinConfiguration.class);
            } catch (NoSuchMethodException e) {

            }
            this.registerVeinMethod = registerMethod;
        }

        public void placeVeins(
                               net.minecraft.world.level.WorldGenLevel level,
                               net.minecraft.util.RandomSource random,
                               int chunkX,
                               int chunkZ,
                               String[] veinNames) {
            try {
                placeVeinsMethod.invoke(null, level, random, chunkX, chunkZ, veinNames);
            } catch (Exception e) {
                System.err.println("[PhoenixCore] Error placing GT veins: " + e.getMessage());
            }
        }

        @Nullable
        public VeinConfiguration queryVeinConfiguration(String veinName) {
            try {
                Object gtVein = getMaterialMethod.invoke(null, veinName);
                if (gtVein != null) {

                    return extractVeinConfig(gtVein, veinName);
                }
            } catch (Exception e) {

            }
            return null;
        }

        public List<VeinConfiguration> queryVeins(VeinFilter filter) {
            List<VeinConfiguration> results = new ArrayList<>();

            return results;
        }

        public boolean registerCustomVein(VeinConfiguration config) {
            if (registerVeinMethod == null) {
                System.err.println("[PhoenixCore] Custom vein registration not supported in this GT version");
                return false;
            }

            try {
                registerVeinMethod.invoke(null, config.veinName, config);
                return true;
            } catch (Exception e) {
                System.err.println("[PhoenixCore] Error registering custom vein: " + e.getMessage());
                return false;
            }
        }

        private VeinConfiguration extractVeinConfig(Object gtVein, String veinName) {
            try {

                Method getPrimaryMethod = gtVein.getClass().getMethod("getPrimaryMaterial");
                Object primaryMaterial = getPrimaryMethod.invoke(gtVein);
                String primaryName = primaryMaterial != null ? primaryMaterial.toString() : "unknown";

                Method getSecondaryMethod = gtVein.getClass().getMethod("getSecondaryMaterial");
                Object secondaryMaterial = getSecondaryMethod.invoke(gtVein);
                String secondaryName = secondaryMaterial != null ? secondaryMaterial.toString() : "unknown";

                Method getMinYMethod = gtVein.getClass().getMethod("getMinY");
                Method getMaxYMethod = gtVein.getClass().getMethod("getMaxY");
                int minY = (int) getMinYMethod.invoke(gtVein);
                int maxY = (int) getMaxYMethod.invoke(gtVein);

                return new VeinConfiguration(
                        veinName, primaryName, secondaryName,
                        1, minY, maxY, 1.0f, true, new HashMap<>());
            } catch (Exception e) {
                System.err.println("[PhoenixCore] Error extracting vein config: " + e.getMessage());
                return null;
            }
        }
    }

    public static VeinBuilder createVein(String veinName) {
        return new VeinBuilder(veinName);
    }

    public static VeinFilter filterVeins() {
        return new VeinFilter();
    }

    public static void placeVeinsInChunk(
                                         net.minecraft.world.level.WorldGenLevel level,
                                         net.minecraft.util.RandomSource random,
                                         int chunkX,
                                         int chunkZ,
                                         String... veinNames) {
        if (veinManager != null) {
            veinManager.placeVeins(level, random, chunkX, chunkZ, veinNames);
        }
    }

    public static VeinConfiguration queryVein(String veinName) {
        if (veinManager == null) return null;
        return veinManager.queryVeinConfiguration(veinName);
    }

    public static List<String> getAvailableVeins() {
        return new ArrayList<>();
    }

    public static boolean registerVein(VeinConfiguration config) {
        if (veinManager == null) return false;
        return veinManager.registerCustomVein(config);
    }

    public static VeinConfiguration fromJson(com.google.gson.JsonObject json) {
        String veinName = json.get("name").getAsString();
        String primary = json.has("primary") ? json.get("primary").getAsString() : null;
        String secondary = json.has("secondary") ? json.get("secondary").getAsString() : null;
        int veinsPerChunk = json.has("veins_per_chunk") ? json.get("veins_per_chunk").getAsInt() : 1;
        int minY = json.has("min_y") ? json.get("min_y").getAsInt() : 0;
        int maxY = json.has("max_y") ? json.get("max_y").getAsInt() : 256;
        float density = json.has("density") ? json.get("density").getAsFloat() : 1.0f;
        boolean enabled = !json.has("enabled") || json.get("enabled").getAsBoolean();

        Map<String, String> properties = new HashMap<>();
        if (json.has("properties")) {
            com.google.gson.JsonObject propsObj = json.getAsJsonObject("properties");
            for (String key : propsObj.keySet()) {
                properties.put(key, propsObj.get(key).getAsString());
            }
        }

        return new VeinConfiguration(veinName, primary, secondary, veinsPerChunk, minY, maxY, density, enabled,
                properties);
    }
}
