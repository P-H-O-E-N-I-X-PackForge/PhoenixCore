package net.phoenix.core.client.gui.hud;

import net.minecraft.client.gui.GuiGraphics;

import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.screen.ModularPanel;

/** The shared chrome of HUD style machine screens: a dark gridded background and the tab column outside the left edge. */
public final class HudPanel {

    private HudPanel() {}

    private static final int BG_TOP = 0xFF_02101A;
    private static final int BG_BOT = 0xFF_010508;
    private static final int C_TEAL = 0xFF_00B8A5;

    public static final IDrawable BACKGROUND = (ctx, x, y, w, h, theme) -> {
        GuiGraphics g = ctx.getGraphics();
        GuiDraw.drawGradientRect(g, x, y, w, h, BG_TOP, BG_BOT);
        for (int gx = x + 4; gx < x + w; gx += 8) g.fill(gx, y, gx + 1, y + h, 0x0C_00E5CC);
        for (int gy = y + 4; gy < y + h; gy += 8) g.fill(x, gy, x + w, gy + 1, 0x0C_00E5CC);
        GuiDraw.drawBorder(g, x, y, w, h, C_TEAL, 1);
        GuiDraw.drawBorder(g, x + 2, y + 2, w - 4, h - 4, 0x22_00E5CC, 1);
    };

    public static void apply(ModularPanel<?> panel, TabStrip screen) {
        panel.background(BACKGROUND);
        var strip = new CanvasWidget(screen.stripPainter());
        strip.size(TabStrip.TAB_W, screen.stripHeight()).leftRel(1f).top(8);
        strip.excludeAreaInRecipeViewer();
        panel.child(strip);
    }
}
