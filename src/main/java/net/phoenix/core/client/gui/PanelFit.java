package net.phoenix.core.client.gui;

import net.minecraft.client.Minecraft;

import brachy.modularui.api.layout.IViewportStack;
import brachy.modularui.screen.ModularPanel;

public final class PanelFit {

    private PanelFit() {}

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void apply(ModularPanel<?> panel, int width, int height) {
        ((ModularPanel) panel).transform((widget, stack) -> {
            float scale = fitScale(width, height);
            if (scale >= 0.999f) return;
            IViewportStack viewport = (IViewportStack) stack;
            viewport.translate(width / 2f, height / 2f);
            viewport.scale(scale, scale);
            viewport.translate(-width / 2f, -height / 2f);
        });
    }

    public static int[] localMouse(brachy.modularui.api.widget.IWidget widget, int absX, int absY, int panelWidth,
                                   int panelHeight) {
        float scale = fitScale(panelWidth, panelHeight);
        float x = absX;
        float y = absY;
        if (scale < 0.999f) {
            var panel = widget.getPanel().getArea();
            float cx = panel.x + panel.width / 2f;
            float cy = panel.y + panel.height / 2f;
            x = cx + (absX - cx) / scale;
            y = cy + (absY - cy) / scale;
        }
        var area = widget.getArea();
        return new int[] { Math.round(x) - area.x, Math.round(y) - area.y };
    }

    public static float fitScale(int width, int height) {
        var window = Minecraft.getInstance().getWindow();
        float margin = Math.max(PixelScale.bottomReserve(), 8f);
        float fit = Math.min(1f, Math.min((window.getGuiScaledHeight() - 2 * margin) / height,
                (window.getGuiScaledWidth() - 16f) / width));
        return PixelScale.snap(Math.max(0.3f, fit));
    }
}
