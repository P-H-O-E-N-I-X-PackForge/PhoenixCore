package net.phoenix.core.client.gui.hud;

import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.screen.ModularPanel;

/** The shared chrome of HUD style machine screens: the theme's background and the tab strip outside the panel edge. */
public final class HudPanel {

    private HudPanel() {}

    public static final IDrawable BACKGROUND = HudTheme.TEAL.background();

    public static void apply(ModularPanel<?> panel, TabStrip screen) {
        HudTheme theme = screen.theme();
        panel.background(theme.background());
        var strip = new CanvasWidget(screen.stripPainter());
        strip.size(screen.stripWidth(), screen.stripHeight());
        if (screen.horizontal()) strip.leftRel(0f, 8, 0f).topRel(0f, 0, 1f);
        else strip.leftRel(1f).top(8);
        strip.excludeAreaInRecipeViewer();
        panel.child(strip);
    }
}
