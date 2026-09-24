package net.phoenix.core.integration.emi;

import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.PhoenixCore;

import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.screen.EmiScreenBase;

/**
 * Temporary diagnostic (2026-09-24) - user reports EMI's sidebar shifts to the left specifically when
 * a gregpacks/gregvaults screen is open, but not with other screens, despite the exclusion-area and
 * {@code EmiScreenBase} bounds-provider fixes already verified correct for those screens in isolation
 * (logged and checked against real numbers at GUI Scale 3). Logs, for every screen that opens,
 * exactly what {@link EmiScreenBase#of} resolves as that screen's bounds - the same value {@code
 * EmiScreenManager#recalculate} uses to split left/right sidebar space - so a repro comparing "another
 * UI" against "OmniPack/Vault" shows the actual numbers EMI sees for each, instead of guessing. Remove
 * once this is root-caused.
 */
@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
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
