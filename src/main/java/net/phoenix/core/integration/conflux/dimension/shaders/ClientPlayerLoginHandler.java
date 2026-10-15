package net.phoenix.core.integration.conflux.dimension.shaders;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = "phoenixcore", value = Dist.CLIENT)
public class ClientPlayerLoginHandler {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!event.getEntity().level().isClientSide) return;

        ClientSetupHandler.tryInitializeShaders("PhoenixCore shaders initialized on login");
    }
}
