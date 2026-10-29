package net.phoenix.core.integration.conflux.dimension.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.phoenix.core.integration.conflux.dimension.EtherealSpawnDimension;

@EventBusSubscriber(modid = "phoenixcore", value = Dist.CLIENT)
public class EtherealSpawnClientEvents {

    private static boolean pickerShown = false;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !isInEtherealSpawn(mc)) {
            pickerShown = false;
            return;
        }
        // the picker used to open here; it now waits for a click on the choosing screen in the spawn (CinemaClientActions)
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        pickerShown = false;
    }

    private static boolean isInEtherealSpawn(Minecraft mc) {
        return mc.level != null && mc.level.dimension().equals(EtherealSpawnDimension.DIMENSION_KEY);
    }

    public static void resetPickerShown() {
        pickerShown = false;
    }
}
