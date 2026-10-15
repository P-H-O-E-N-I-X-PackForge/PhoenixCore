package net.phoenix.core.integration.conflux.dimension.physics;

public class DimensionPhysicsPresets {

    public static void initializeDimensionPhysics(String dimensionId) {}

    public static String getPhysicsDescription(String dimensionId) {
        return switch (dimensionId) {
            case "phoenix" -> "Thermal updrafts over active volcanoes, and lava falls that run down the plateaus";
            case "sculk" -> "Slow descent into cenotes, bouncy fungus caps, thermals over the canyon ribs";
            case "void" -> "Low gravity on floating islands, weightless geodes, a shallow well in impact craters";
            case "sealed_a" -> "A haul road that carries you down the pit, steam lifts up the cooling towers, walkways on overpasses";
            case "sealed_b" -> "Geysers that launch you, slick basalt columns, rising heat over the canyons";
            default -> "No special physics";
        };
    }
}
