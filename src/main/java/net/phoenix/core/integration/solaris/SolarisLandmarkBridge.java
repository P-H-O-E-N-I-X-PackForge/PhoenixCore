package net.phoenix.core.integration.solaris;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.conflux.dimension.DisciplineChunkGenerator;
import net.phoenix.core.integration.conflux.dimension.EtherealSpawnDimension;
import net.phoenix.core.integration.conflux.dimension.worldgen.Landforms;
import net.phoenix.core.integration.conflux.research.ResearchTeamHelper;
import net.phoenix.core.integration.conflux.terminal.ResearchTerminalBlockEntity;
import net.phoenix.core.network.packet.S2CLandformZonesPacket;
import net.phoenixvine.solaris.api.SolarisMarkerSyncEvent;
import net.phoenixvine.solaris.integration.MarkerSync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private static final Map<UUID, Integer> ZONE_HASH = new HashMap<>();

    private SolarisLandmarkBridge() {}

    static void init() {
        if (listening) return;
        listening = true;
        NeoForge.EVENT_BUS.addListener(SolarisLandmarkBridge::onMarkerSync);
    }

    private static void onMarkerSync(SolarisMarkerSyncEvent event) {
        if (!SolarisSupport.isActive()) return;
        forget(event.getPlayer().getUUID());
        sync(event.getPlayer());
        if (PhoenixConfigs.INSTANCE.confluxSolaris.oreSense) SolarisOreBridge.sync(event.getPlayer());
    }

    static void forget(UUID player) {
        ZONE_HASH.remove(player);
        MARKERS.reset(player);
        SolarisOreBridge.forget(player);
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
                    sighting.icon(), sighting.x(), sighting.z(), sighting.baseY(), sighting.zones());
            if (data.discover(team, sighting.key(), entry)) {
                player.displayClientMessage(Component.literal("Discovered: " + sighting.label()), true);
            }
        }
    }

    static void sync(ServerPlayer player) {
        UUID team = ResearchTeamHelper.getTeamId(player);
        Map<String, MarkerSync.Marker> desired = new HashMap<>();
        Map<String, LandformDiscoveryData.Entry> landforms = PhoenixConfigs.INSTANCE.confluxSolaris.landformMarkers ?
                LandformDiscoveryData.get(player.serverLevel()).forTeam(team) : Map.of();
        for (Map.Entry<String, LandformDiscoveryData.Entry> known : landforms.entrySet()) {
            LandformDiscoveryData.Entry e = known.getValue();
            String key = "phoenixcore:lf/" + known.getKey().replace(':', '/');
            desired.put(key, new MarkerSync.Marker(e.label(), ResourceLocation.parse(e.dimension()),
                    new BlockPos(e.x(), e.y(), e.z()), e.color(), e.icon(), "Landforms"));
        }
        if (PhoenixConfigs.INSTANCE.confluxSolaris.researchSiteMarkers) addSites(player, team, desired);
        if (PhoenixConfigs.INSTANCE.confluxSolaris.networkSiteMarkers) addNetworkSites(player, desired);
        MARKERS.apply(player, desired);
        if (PhoenixConfigs.INSTANCE.confluxSolaris.landformZones) sendZones(player, team);
    }

    private static void sendZones(ServerPlayer player, UUID team) {
        List<S2CLandformZonesPacket.ZoneDef> defs = new ArrayList<>();
        for (LandformDiscoveryData.Entry entry : LandformDiscoveryData.get(player.serverLevel()).forTeam(team)
                .values()) {
            for (Landforms.Zone zone : entry.zones()) defs.add(new S2CLandformZonesPacket.ZoneDef(entry.dimension(),
                    zone));
        }
        int hash = defs.hashCode();
        Integer sent = ZONE_HASH.get(player.getUUID());
        if (sent != null && sent == hash) return;
        ZONE_HASH.put(player.getUUID(), hash);
        PacketDistributor.sendToPlayer(player, new S2CLandformZonesPacket(defs));
    }

    /** Tesla towers, range extenders and Continuum launch complexes. These use the guild/player team, not research. */
    private static void addNetworkSites(ServerPlayer player, Map<String, MarkerSync.Marker> desired) {
        for (NetworkSites.Site site : NetworkSites.of(player)) {
            String key = "phoenixcore:net/" + site.kind() + "/" + Long.toHexString(site.pos().asLong()) + "/" +
                    Integer.toHexString(site.dimension().hashCode());
            desired.put(key, new MarkerSync.Marker(site.name(), site.dimension(), site.pos(), site.color(),
                    site.icon(), site.category()));
        }
    }

    private static void addSites(ServerPlayer player, UUID team, Map<String, MarkerSync.Marker> desired) {
        MinecraftServer server = player.server;
        TeamSiteData sites = TeamSiteData.get(player.serverLevel());
        for (Map.Entry<String, TeamSiteData.Site> entry : new ArrayList<>(sites.forTeam(team).entrySet())) {
            if (!entry.getKey().startsWith("terminal/")) continue;
            TeamSiteData.Site site = entry.getValue();
            ResourceLocation dimension = ResourceLocation.parse(site.dimension());
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
            if (level != null && level.isLoaded(site.pos()) &&
                    !(level.getBlockEntity(site.pos()) instanceof ResearchTerminalBlockEntity)) {
                sites.remove(team, entry.getKey());
                continue;
            }
            String key = "phoenixcore:terminal/" + Long.toHexString(site.pos().asLong()) + "/" +
                    Integer.toHexString(dimension.hashCode());
            desired.put(key, new MarkerSync.Marker("Research Terminal", dimension, site.pos(), "5577FF", "RESOURCE",
                    "Research"));
        }

        if (server.getLevel(EtherealSpawnDimension.DIMENSION_KEY) != null) {
            desired.put("phoenixcore:ethereal_spawn", new MarkerSync.Marker("Ethereal Spawn",
                    EtherealSpawnDimension.DIMENSION_KEY.location(), new BlockPos(0, 65, 0), "E8E0FF", "HOME",
                    "Research"));
        }
    }
}
