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

    private static Screen layoutLogged;
    private static int layoutFrames;

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!ModList.get().isLoaded("emi")) return;
        Screen screen = event.getScreen();
        if (screen != layoutLogged) {
            layoutLogged = screen;
            layoutFrames = 0;
        }
        if (++layoutFrames != 60) return;
        String name = screen.getClass().getSimpleName();
        if (!name.contains("Vault") && !name.contains("OmniPack")) return;

        try {
            StringBuilder sb = new StringBuilder("[EmiLayoutDebug] ").append(name).append(" window=")
                    .append(screen.width).append('x').append(screen.height);

            var panelsField = dev.emi.emi.screen.EmiScreenManager.class.getDeclaredField("panels");
            panelsField.setAccessible(true);
            for (Object panel : (java.util.List<?>) panelsField.get(null)) {
                var p = (dev.emi.emi.screen.EmiScreenManager.SidebarPanel) panel;
                sb.append("\n  panel ").append(p.side).append(" visible=").append(p.isVisible());
                if (p.space != null) {
                    sb.append(" space=[tx=").append(p.space.tx).append(" ty=").append(p.space.ty).append(" tw=")
                            .append(p.space.tw).append(" th=").append(p.space.th).append(']');
                }
            }
            var search = dev.emi.emi.screen.EmiScreenManager.search;
            if (search != null) {
                sb.append("\n  search x=").append(search.getX()).append(" y=").append(search.getY()).append(" w=")
                        .append(search.getWidth());
            }
            var exclusionField = dev.emi.emi.screen.EmiScreenManager.class.getDeclaredField("lastExclusion");
            exclusionField.setAccessible(true);
            for (Object ex : (java.util.List<?>) exclusionField.get(null)) {
                var b = (dev.emi.emi.api.widget.Bounds) ex;
                sb.append("\n  exclusion [x=").append(b.x()).append(" y=").append(b.y()).append(" w=")
                        .append(b.width()).append(" h=").append(b.height()).append(']');
            }
            Bounds bounds = EmiScreenBase.of(screen).bounds();
            sb.append("\n  screen bounds [x=").append(bounds.x()).append(" y=").append(bounds.y()).append(" w=")
                    .append(bounds.width()).append(" h=").append(bounds.height()).append(']');
            PhoenixCore.LOGGER.info(sb.toString());
        } catch (Throwable e) {
            PhoenixCore.LOGGER.info("[EmiLayoutDebug] could not read EMI's layout: {}", e.toString());
        }
    }
}
