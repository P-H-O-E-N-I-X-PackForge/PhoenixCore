package net.phoenix.core.integration.conflux.dimension;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "phoenixcore")
public class EtherealSpawnManager {

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        initializeEtherealSpawnDimension(server);
    }

    public static void initializeEtherealSpawnDimension(MinecraftServer server) {
        ServerLevel etherealDim = server.getLevel(EtherealSpawnDimension.DIMENSION_KEY);

        if (etherealDim == null) {

            System.out.println("[PhoenixCore] Ethereal spawn dimension not found. " +
                    "Ensure datapacks/dimension.json defines: " + EtherealSpawnDimension.DIMENSION_KEY);
            return;
        }

        etherealDim.setSpawnSettings(true, true);
        System.out.println("[PhoenixCore] Ethereal spawn dimension initialized at " +
                EtherealSpawnDimension.DIMENSION_KEY);
    }

    public static ServerLevel getOrCreateEtherealDimension(MinecraftServer server) {
        ServerLevel etherealDim = server.getLevel(EtherealSpawnDimension.DIMENSION_KEY);
        if (etherealDim != null) {
            return etherealDim;
        }

        return server.overworld();
    }

    public static void teleportToEtherealSpawn(net.minecraft.server.level.ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        ServerLevel etherealDim = getOrCreateEtherealDimension(server);
        player.teleportTo(etherealDim, 0.5, 65, 0.5, 0, 0);
    }
}
