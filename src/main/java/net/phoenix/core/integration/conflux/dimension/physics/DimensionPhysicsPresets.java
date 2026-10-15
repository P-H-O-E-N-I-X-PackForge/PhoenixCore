package net.phoenix.core.integration.conflux.dimension.physics;

/**
 * Conflux physics now comes from the landforms themselves (see {@code LandformPhysics}): a volcano's updraft, a floating
 * island's low gravity, a geyser's eruption. There are no longer zones and platforms at fixed coordinates, which had
 * nothing to do with the terrain. The registry stays for anything that wants to register a zone by hand.
 */
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
