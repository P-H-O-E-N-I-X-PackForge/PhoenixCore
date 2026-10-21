package net.phoenix.core.integration.solaris;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.phoenixvine.solaris.integration.MarkerSync;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Shows what {@link OreSense} finds as Solaris markers, with detail that grows with research:
 * tier 1 says there is ore, tier 2 says which, tier 3 gives the exact spot, amount and depth.
 * Only reached when Solaris is loaded.
 */
final class SolarisOreBridge {

    private static final String CATEGORY = "Ore Sense";
    private static final MarkerSync MARKERS = new MarkerSync("9AA0A8", "RESOURCE", CATEGORY);

    private SolarisOreBridge() {}

    static void forget(UUID player) {
        MARKERS.reset(player);
    }

    static void sync(ServerPlayer player) {
        Map<String, MarkerSync.Marker> desired = new HashMap<>();
        if (OreSense.disciplineOf(player.serverLevel()) != null) {
            int tier = OreSense.tier(player);
            if (tier > 0) {
                var dimension = player.serverLevel().dimension().location();
                for (OreSense.Reading reading : OreSense.readings(player)) {
                    String key = "phoenixcore:ore/" + reading.chunkX() + "/" + reading.chunkZ() + "/" +
                            Integer.toHexString(BuiltInRegistries.BLOCK.getKey(reading.block()).hashCode());
                    desired.put(key, marker(tier, reading, dimension));
                }
            }
        }
        MARKERS.apply(player, desired);
    }

    private static MarkerSync.Marker marker(int tier, OreSense.Reading reading,
                                            net.minecraft.resources.ResourceLocation dimension) {
        if (tier == 1) {
            return new MarkerSync.Marker("Ore deposit", dimension, OreSense.chunkCenter(reading), "9AA0A8", null,
                    null);
        }

        String color = colorOf(reading.block());
        String ore = reading.block().getName().getString();
        if (tier == 2) {
            return new MarkerSync.Marker(ore + " deposit", dimension, OreSense.chunkCenter(reading), color, null,
                    null);
        }

        String label = ore + " x" + reading.count() + " (y" + reading.minY() + " to " + reading.maxY() + ")";
        return new MarkerSync.Marker(label, dimension, new BlockPos(reading.x(), reading.y(), reading.z()), color,
                null, null);
    }

    private static String colorOf(Block block) {
        return String.format("%06X", block.defaultMapColor().col & 0xFFFFFF);
    }
}
