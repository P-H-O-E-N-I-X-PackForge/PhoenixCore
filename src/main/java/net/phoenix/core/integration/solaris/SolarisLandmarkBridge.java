package net.phoenix.core.integration.solaris;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.conflux.dimension.DisciplineChunkGenerator;
import net.phoenix.core.integration.conflux.dimension.worldgen.Landforms;
import net.phoenix.core.integration.conflux.research.ResearchTeamHelper;
import net.phoenixvine.solaris.api.SolarisMarkerSyncEvent;
import net.phoenixvine.solaris.integration.MarkerSync;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Pins the Conflux landforms a research team has found onto the Solaris map. Landforms are generated
 * from the world seed, so being near one is enough to find it; once found it stays on the map for the
 * whole team. Only reached when Solaris is loaded.
 */
final class SolarisLandmarkBridge {

    private static final MarkerSync MARKERS = new MarkerSync("FFFFFF", "DOT", "Landforms");
    private static boolean listening = false;

    private SolarisLandmarkBridge() {}

    static void init() {
        if (listening) return;
        listening = true;
        NeoForge.EVENT_BUS.addListener(SolarisLandmarkBridge::onMarkerSync);
    }

    private static void onMarkerSync(SolarisMarkerSyncEvent event) {
        if (!SolarisSupport.isActive() || !PhoenixConfigs.INSTANCE.confluxSolaris.landformMarkers) return;
        MARKERS.reset(event.getPlayer().getUUID());
        sync(event.getPlayer());
    }

    static void forget(UUID player) {
        MARKERS.reset(player);
    }

    static void discover(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (!(level.getChunkSource().getGenerator() instanceof DisciplineChunkGenerator generator)) return;

        int margin = PhoenixConfigs.INSTANCE.confluxSolaris.discoverMargin;
        var sightings = generator.landformSightingsNear(level, player.getBlockX(), player.getBlockZ(), margin);
        if (sightings.isEmpty()) return;

        LandformDiscoveryData data = LandformDiscoveryData.get(level);
        UUID team = ResearchTeamHelper.getTeamId(player);
        String dimension = level.dimension().location().toString();
        for (Landforms.Sighting sighting : sightings) {
            var entry = new LandformDiscoveryData.Entry(dimension, sighting.label(), sighting.color(),
                    sighting.icon(), sighting.x(), sighting.z(), sighting.baseY());
            if (data.discover(team, sighting.key(), entry)) {
                player.displayClientMessage(Component.literal("Discovered: " + sighting.label()), true);
            }
        }
    }

    static void sync(ServerPlayer player) {
        UUID team = ResearchTeamHelper.getTeamId(player);
        Map<String, MarkerSync.Marker> desired = new HashMap<>();
        for (Map.Entry<String, LandformDiscoveryData.Entry> known : LandformDiscoveryData.get(player.serverLevel())
                .forTeam(team).entrySet()) {
            LandformDiscoveryData.Entry e = known.getValue();
            String key = "phoenixcore:lf/" + known.getKey().replace(':', '/');
            desired.put(key, new MarkerSync.Marker(e.label(), ResourceLocation.parse(e.dimension()),
                    new BlockPos(e.x(), e.y(), e.z()), e.color(), e.icon(), "Landforms"));
        }
        MARKERS.apply(player, desired);
    }
}
