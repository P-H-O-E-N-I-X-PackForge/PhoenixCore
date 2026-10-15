package net.phoenix.core.client.event;


import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.client.emi.PhoenixFavoriteSets;
import net.phoenix.core.mixin.accessor.MinecraftServerStorageAccessor;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID, value = Dist.CLIENT)
public class EmiFavoritesWorldHandler {

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        PhoenixFavoriteSets.loadForWorld(computeWorldId());
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        PhoenixFavoriteSets.unload();
    }

    private static String computeWorldId() {
        Minecraft mc = Minecraft.getInstance();

        IntegratedServer integratedServer = mc.getSingleplayerServer();
        if (integratedServer != null) {
            String levelId = ((MinecraftServerStorageAccessor) integratedServer)
                    .phoenixcore$getStorageSource().getLevelId();
            return "sp_" + sanitize(levelId);
        }

        ServerData server = mc.getCurrentServer();
        if (server != null && server.ip != null) {
            return "mp_" + sanitize(server.ip);
        }

        return "global";
    }

    private static String sanitize(String s) {
        return s.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
