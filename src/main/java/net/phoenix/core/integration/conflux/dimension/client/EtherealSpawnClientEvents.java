package net.phoenix.core.integration.conflux.dimension.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.phoenix.core.integration.conflux.client.DisciplinePickerScreen;
import net.phoenix.core.integration.conflux.dimension.EtherealSpawnDimension;

@EventBusSubscriber(modid = "phoenixcore", value = Dist.CLIENT)
public class EtherealSpawnClientEvents {

    private static boolean pickerShown = false;

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Init.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        if (!isInEtherealSpawn(mc)) {
            return;
        }

        if (event.getScreen() instanceof PauseScreen && !(event.getScreen() instanceof DisciplinePickerScreen)) {
            event.setCanceled(true);
        }

        if (!pickerShown) {
            pickerShown = true;
            mc.setScreen(new DisciplinePickerScreen());
        }
    }

    private static boolean isInEtherealSpawn(Minecraft mc) {
        if (mc.level == null) return false;

        return mc.level.dimension().equals(EtherealSpawnDimension.DIMENSION_KEY);
    }

    public static void resetPickerShown() {
        pickerShown = false;
    }
}
