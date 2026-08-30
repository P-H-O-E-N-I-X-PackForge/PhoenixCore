package net.phoenix.core.integration.conflux.dimension.sky;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

@Mod.EventBusSubscriber(modid = "phoenixcore", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class SkyRenderingHook {

    private static String lastDimension = "";
    private static boolean skyManagerInitialized = false;
    private static Level lastLevel = null;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;

        if (level == null) {
            return;
        }

        // Each dimension switch hands the client a brand new Level instance - re-init so the
        // sky renderers aren't left holding a stale reference to whichever level was loaded
        // first (previously they only ever got built once, on the very first tick).
        if (level != lastLevel) {
            SkyManager.getInstance().init(level);
            skyManagerInitialized = true;
            lastLevel = level;
        }

        String currentDimension = getDimensionId(level);
        if (!currentDimension.equals(lastDimension)) {
            lastDimension = currentDimension;
            SkyManager.getInstance().onDimensionChange(currentDimension);
        }

        SkyManager.getInstance().update();
    }

    // Actual sky rendering happens in DisciplineSkyEffects.renderSky() - registered via
    // RegisterDimensionSpecialEffectsEvent against the "phoenixcore:discipline" dimension_type,
    // which fully replaces vanilla's own sun/moon/star/cloud rendering instead of drawing
    // alongside it. This class only tracks which discipline is active and keeps SkyManager's
    // per-frame state (time, brightness) up to date.

    private static String getDimensionId(Level level) {

        String path = level.dimension().location().getPath();
        String discipline = path.startsWith("conflux/") ? path.substring("conflux/".length()) : path;

        if (discipline.startsWith("phoenix")) return "phoenix";
        if (discipline.startsWith("sculk")) return "sculk";
        if (discipline.startsWith("void")) return "void";
        if (discipline.startsWith("sealed_a")) return "sealed_a";
        if (discipline.startsWith("sealed_b")) return "sealed_b";

        return "";
    }

    public static boolean isSkyManagerInitialized() {
        return skyManagerInitialized;
    }

    public static void resetSkyManager() {
        SkyManager.getInstance().cleanup();
        skyManagerInitialized = false;
        lastLevel = null;
        lastDimension = "";
    }
}
