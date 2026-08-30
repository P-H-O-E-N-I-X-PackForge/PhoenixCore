package net.phoenix.core.integration.conflux.dimension.physics;

import java.util.*;

public class PhysicsRegistry {

    private static final PhysicsRegistry INSTANCE = new PhysicsRegistry();

    private final Map<String, List<GravityZone>> gravityZonesByDimension = new HashMap<>();
    private final Map<String, List<MovingPlatform>> platformsByDimension = new HashMap<>();

    public static PhysicsRegistry getInstance() {
        return INSTANCE;
    }

    public void registerGravityZone(String dimensionId, GravityZone zone) {
        gravityZonesByDimension
            .computeIfAbsent(dimensionId, k -> new ArrayList<>())
            .add(zone);

        System.out.println("[PhoenixCore] Registered gravity zone: " + zone);
    }

    public void unregisterGravityZone(String dimensionId, GravityZone zone) {
        List<GravityZone> zones = gravityZonesByDimension.get(dimensionId);
        if (zones != null) {
            zones.remove(zone);
        }
    }

    public List<GravityZone> getGravityZones(String dimensionId) {
        return gravityZonesByDimension.getOrDefault(dimensionId, new ArrayList<>());
    }

    public void clearGravityZones(String dimensionId) {
        gravityZonesByDimension.remove(dimensionId);
    }

    public void registerPlatform(String dimensionId, MovingPlatform platform) {
        platformsByDimension
            .computeIfAbsent(dimensionId, k -> new ArrayList<>())
            .add(platform);

        System.out.println("[PhoenixCore] Registered platform: " + platform);
    }

    public void unregisterPlatform(String dimensionId, MovingPlatform platform) {
        List<MovingPlatform> platforms = platformsByDimension.get(dimensionId);
        if (platforms != null) {
            platforms.remove(platform);
        }
    }

    public List<MovingPlatform> getPlatforms(String dimensionId) {
        return platformsByDimension.getOrDefault(dimensionId, new ArrayList<>());
    }

    public void clearPlatforms(String dimensionId) {
        platformsByDimension.remove(dimensionId);
    }

    public void clearDimension(String dimensionId) {
        clearGravityZones(dimensionId);
        clearPlatforms(dimensionId);
    }

    public void clearAll() {
        gravityZonesByDimension.clear();
        platformsByDimension.clear();
    }

    public int getTotalGravityZones() {
        return gravityZonesByDimension.values().stream()
            .mapToInt(List::size)
            .sum();
    }

    public int getTotalPlatforms() {
        return platformsByDimension.values().stream()
            .mapToInt(List::size)
            .sum();
    }

    public void logStatistics() {
        System.out.println("[PhoenixCore] Physics Registry Statistics:");
        System.out.println("  Total Gravity Zones: " + getTotalGravityZones());
        System.out.println("  Total Platforms: " + getTotalPlatforms());

        for (Map.Entry<String, List<GravityZone>> entry : gravityZonesByDimension.entrySet()) {
            System.out.println("  Dimension " + entry.getKey() + ": " + entry.getValue().size() + " zones");
        }
    }
}
