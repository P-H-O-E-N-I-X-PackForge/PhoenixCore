package net.phoenix.core.integration.solaris;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.api.event.ConfluxResearchEvent;
import net.phoenix.core.api.event.ConfluxTeamChangedEvent;
import net.phoenix.core.configs.PhoenixConfigs;

/**
 * Entry points for the Solaris integration. This class never names a Solaris type; every call into
 * Solaris goes through a bridge class and only after {@link SolarisSupport#isActive()}.
 */
public final class SolarisEvents {

    private static final int DISCOVER_INTERVAL_TICKS = 40;
    private static final int SYNC_INTERVAL_TICKS = 100;
    private static final int ORE_SCAN_INTERVAL_TICKS = 10;

    private SolarisEvents() {}

    @EventBusSubscriber(modid = PhoenixCore.MOD_ID)
    public static final class Server {

        @SubscribeEvent
        public static void onServerStarted(ServerStartedEvent event) {
            if (!SolarisSupport.isActive()) return;
            SolarisGatesBridge.registerGates();
            if (PhoenixConfigs.INSTANCE.confluxSolaris.gateMapFeatures) SolarisGatesBridge.bindDefaults();
            SolarisLandmarkBridge.init();
        }

        @SubscribeEvent
        public static void onDatapackSync(OnDatapackSyncEvent event) {
            if (!SolarisSupport.isActive() || event.getPlayer() != null) return;
            SolarisGatesBridge.registerGates();
        }

        @SubscribeEvent
        public static void onResearch(ConfluxResearchEvent.Post event) {
            if (SolarisSupport.isActive()) SolarisGatesBridge.refreshTeam(event.getServer(), event.getTeam());
        }

        @SubscribeEvent
        public static void onTeamChanged(ConfluxTeamChangedEvent event) {
            if (SolarisSupport.isActive()) SolarisGatesBridge.refreshAll(event.getServer());
        }

        @SubscribeEvent
        public static void onPlayerTick(PlayerTickEvent.Post event) {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            if (!SolarisSupport.isActive()) return;

            var config = PhoenixConfigs.INSTANCE.confluxSolaris;
            if (config.landformMarkers && player.tickCount % DISCOVER_INTERVAL_TICKS == 0) {
                SolarisLandmarkBridge.discover(player);
            }
            if (config.oreSense && player.tickCount % ORE_SCAN_INTERVAL_TICKS == 0) OreSense.scanStep(player);

            if (player.tickCount % SYNC_INTERVAL_TICKS == 0) {
                if (config.landformMarkers || config.researchSiteMarkers) SolarisLandmarkBridge.sync(player);
                if (config.oreSense) SolarisOreBridge.sync(player);
            }
        }

        @SubscribeEvent
        public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
            if (SolarisSupport.isLoaded()) SolarisLandmarkBridge.forget(event.getEntity().getUUID());
        }
    }

    @EventBusSubscriber(modid = PhoenixCore.MOD_ID, value = Dist.CLIENT)
    public static final class Client {

        private static final int SKIN_INTERVAL_TICKS = 20;
        private static int ticks = 0;

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            if (++ticks < SKIN_INTERVAL_TICKS) return;
            ticks = 0;
            if (!SolarisSupport.isActive() || !PhoenixConfigs.INSTANCE.confluxSolaris.disciplineSkin) return;
            SolarisSkinBridge.update();
        }

        @SubscribeEvent
        public static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
            if (SolarisSupport.isLoaded()) SolarisSkinBridge.clear();
        }
    }
}
