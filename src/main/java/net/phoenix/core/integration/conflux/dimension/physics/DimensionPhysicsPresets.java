package net.phoenix.core.integration.conflux.dimension.physics;

import net.minecraft.world.phys.Vec3;

public class DimensionPhysicsPresets {

    public static void initializeDimensionPhysics(String dimensionId) {
        PhysicsRegistry registry = PhysicsRegistry.getInstance();

        switch (dimensionId) {
            case "phoenix" -> initializePhoenixPhysics(registry);
            case "sculk" -> initializeSculkPhysics(registry);
            case "void" -> initializeVoidPhysics(registry);
            case "sealed_a" -> initializeSealedAPhysics(registry);
            case "sealed_b" -> initializeSealedBPhysics(registry);
        }
    }

    private static void initializePhoenixPhysics(PhysicsRegistry registry) {
        String dimensionId = "phoenix";

        registry.registerGravityZone(dimensionId,
                new GravityZone(new Vec3(0, 64, 0), 20.0, 0.4f, dimensionId));

        registry.registerGravityZone(dimensionId,
                new GravityZone(new Vec3(100, 80, 100), 25.0, 0.5f, dimensionId));

        registry.registerGravityZone(dimensionId,
                new GravityZone(new Vec3(-100, 70, -100), 22.0, 0.45f, dimensionId));

        System.out.println("[PhoenixCore] Initialized Phoenix physics (3 thermal updrafts)");
    }

    private static void initializeSculkPhysics(PhysicsRegistry registry) {
        String dimensionId = "sculk";

        registry.registerPlatform(dimensionId,
                new MovingPlatform(
                        new Vec3(0, 32, 0),
                        new Vec3(20, 2, 20),
                        new Vec3(1, 0, 0),
                        MovingPlatform.PlatformType.CONVEYOR_BELT,
                        2.0f));

        registry.registerPlatform(dimensionId,
                new MovingPlatform(
                        new Vec3(100, 40, 100),
                        new Vec3(20, 2, 20),
                        new Vec3(0, 0, 1),
                        MovingPlatform.PlatformType.CONVEYOR_BELT,
                        1.5f));

        registry.registerPlatform(dimensionId,
                new MovingPlatform(
                        new Vec3(-100, 50, -100),
                        new Vec3(8, 15, 8),
                        new Vec3(0, 1, 0),
                        MovingPlatform.PlatformType.ELEVATOR_UP,
                        1.0f));

        System.out.println("[PhoenixCore] Initialized Sculk physics (2 conveyors, 1 elevator)");
    }

    private static void initializeVoidPhysics(PhysicsRegistry registry) {
        String dimensionId = "void";

        registry.registerGravityZone(dimensionId,
                new GravityZone(new Vec3(0, 100, 0), 50.0, 0.0f, dimensionId));

        registry.registerGravityZone(dimensionId,
                new GravityZone(new Vec3(150, 80, 150), 45.0, 0.1f, dimensionId));

        registry.registerGravityZone(dimensionId,
                new GravityZone(new Vec3(-150, 120, -150), 40.0, 0.2f, dimensionId));

        registry.registerPlatform(dimensionId,
                new MovingPlatform(
                        new Vec3(0, 50, 0),
                        new Vec3(5, 30, 5),
                        new Vec3(0, 1, 0),
                        MovingPlatform.PlatformType.SPIRAL,
                        1.2f));

        System.out.println("[PhoenixCore] Initialized Void physics (3 zero-G zones, 1 spiral)");
    }

    private static void initializeSealedAPhysics(PhysicsRegistry registry) {
        String dimensionId = "sealed_a";

        registry.registerPlatform(dimensionId,
                new MovingPlatform(
                        new Vec3(0, 64, 0),
                        new Vec3(30, 1, 8),
                        new Vec3(1, 0, 0),
                        MovingPlatform.PlatformType.CONVEYOR_BELT,
                        3.0f));

        registry.registerPlatform(dimensionId,
                new MovingPlatform(
                        new Vec3(100, 64, 0),
                        new Vec3(30, 1, 8),
                        new Vec3(-1, 0, 0),
                        MovingPlatform.PlatformType.CONVEYOR_BELT,
                        3.0f));

        registry.registerPlatform(dimensionId,
                new MovingPlatform(
                        new Vec3(0, 50, 50),
                        new Vec3(10, 20, 10),
                        new Vec3(0, 1, 0),
                        MovingPlatform.PlatformType.ELEVATOR_UP,
                        2.0f));

        registry.registerPlatform(dimensionId,
                new MovingPlatform(
                        new Vec3(0, 30, 50),
                        new Vec3(10, 20, 10),
                        new Vec3(0, -1, 0),
                        MovingPlatform.PlatformType.ELEVATOR_DOWN,
                        2.0f));

        System.out.println("[PhoenixCore] Initialized Sealed-A physics (2 conveyors, 2 elevators)");
    }

    private static void initializeSealedBPhysics(PhysicsRegistry registry) {
        String dimensionId = "sealed_b";

        registry.registerGravityZone(dimensionId,
                new GravityZone(new Vec3(0, 80, 0), 35.0, -1.0f, dimensionId));

        registry.registerGravityZone(dimensionId,
                new GravityZone(new Vec3(100, 100, 100), 30.0, 0.0f, dimensionId));

        registry.registerGravityZone(dimensionId,
                new GravityZone(new Vec3(-100, 50, -100), 25.0, 2.0f, dimensionId));

        registry.registerPlatform(dimensionId,
                new MovingPlatform(
                        new Vec3(50, 60, 50),
                        new Vec3(10, 25, 10),
                        new Vec3(0, 1, 0),
                        MovingPlatform.PlatformType.SPIRAL,
                        1.5f));

        System.out.println("[PhoenixCore] Initialized Sealed-B physics (3 gravity zones, 1 spiral)");
    }

    public static String getPhysicsDescription(String dimensionId) {
        return switch (dimensionId) {
            case "phoenix" -> "Volcanic thermal updrafts (reduced gravity zones)";
            case "sculk" -> "Sculk network conveyors and elevators";
            case "void" -> "Zero-gravity floating islands with spiral transit";
            case "sealed_a" -> "Industrial cargo systems (fast conveyors and elevators)";
            case "sealed_b" -> "Reality-breaking chaos (inverted, zero, and high-G zones)";
            default -> "No special physics";
        };
    }
}
