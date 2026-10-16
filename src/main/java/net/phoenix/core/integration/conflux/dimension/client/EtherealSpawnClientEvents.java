package net.phoenix.core.integration.conflux.dimension.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.phoenix.core.integration.conflux.client.DisciplinePickerScreen;
import net.phoenix.core.integration.conflux.dimension.EtherealSpawnDimension;

@EventBusSubscriber(modid = "phoenixcore", value = Dist.CLIENT)
public class EtherealSpawnClientEvents {

    private static boolean pickerShown = false;

    /**
     * Polled from the client tick: a world join opens no screen, so waiting on a screen-init event never fires
     * and the picker never appeared.
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !isInEtherealSpawn(mc)) {
            pickerShown = false;
            return;
        }
        if (!pickerShown && mc.screen == null) {
            pickerShown = true;
            mc.setScreen(new DisciplinePickerScreen());
        }
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
