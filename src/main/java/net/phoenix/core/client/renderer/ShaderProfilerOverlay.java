package net.phoenix.core.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.phoenix.core.client.worldfx.ShaderProfiler;

import java.util.Map;

/**
 * Displays ShaderProfiler's per-shader timing in the top-left corner, only while
 * ShaderProfiler.enabled is on (toggled via PhoenixKeybinds.SHOW_SHADER_PROFILER) - a direct
 * answer to "which discipline sky shader is actually costing FPS" instead of guessing.
 */
public class ShaderProfilerOverlay {

    public static final IGuiOverlay HUD_SHADER_PROFILER = (gui, guiGraphics, partialTick, width, height) -> {
        if (!ShaderProfiler.enabled) return;

        Map<String, Double> averages = ShaderProfiler.snapshot();
        if (averages.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        int x = 6;
        int y = 6;

        guiGraphics.drawString(mc.font, "§6[PhoenixCore] Sky shader cost (ms, smoothed):", x, y, 0xFFFFFF);
        y += 10;

        double total = 0;
        for (Map.Entry<String, Double> entry : averages.entrySet()) {
            guiGraphics.drawString(mc.font, String.format("  %s: %.2fms", entry.getKey(), entry.getValue()),
                    x, y, 0xFFFFAA);
            total += entry.getValue();
            y += 10;
        }
        guiGraphics.drawString(mc.font, String.format("  total: %.2fms", total), x, y, 0xFFFFFF);
    };
}
