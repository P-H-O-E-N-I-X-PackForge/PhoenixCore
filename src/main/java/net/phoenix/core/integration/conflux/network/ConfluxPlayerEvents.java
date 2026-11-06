package net.phoenix.core.integration.conflux.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.phoenix.core.PhoenixCore;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID)
public final class ConfluxPlayerEvents {

    @SubscribeEvent
    public static void onDatapackSync(net.neoforged.neoforge.event.OnDatapackSyncEvent event) {
        event.getRelevantPlayers().forEach(ConfluxNetwork::sendDefinitions);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ConfluxNetwork.syncResearchToPlayer(player);
        }
    }
}
