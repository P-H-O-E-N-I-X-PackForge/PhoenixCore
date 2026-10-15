package net.phoenix.core.integration.emi;

import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.phoenix.core.PhoenixCore;

import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.screen.EmiScreenBase;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID, value = Dist.CLIENT)
public class EmiScreenBoundsDebugListener {

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!ModList.get().isLoaded("emi")) return;

        Screen screen = event.getScreen();
        EmiScreenBase base = EmiScreenBase.of(screen);
        if (base.isEmpty()) {
            PhoenixCore.LOGGER.info("[EmiBoundsDebug] {} window={}x{} -> EmiScreenBase.of() is EMPTY " +
                    "(no bounds provider/generic fallback matched)", screen.getClass().getName(), screen.width,
                    screen.height);
            return;
        }

        Bounds b = base.bounds();
        PhoenixCore.LOGGER.info(
                "[EmiBoundsDebug] {} window={}x{} -> bounds=[x={} y={} w={} h={}] left={} right={} " +
                        "top={} bottom={}",
                screen.getClass().getName(), screen.width, screen.height, b.x(), b.y(), b.width(), b.height(),
                b.left(), b.right(), b.top(), b.bottom());
    }
}
