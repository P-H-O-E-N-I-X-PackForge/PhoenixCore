package net.phoenix.core.integration.drone.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.screen.viewport.GuiContext;
import brachy.modularui.theme.WidgetTheme;

@OnlyIn(Dist.CLIENT)
public class DroneUIBackground implements IDrawable {

    private static final int BG_COLOR_A = 0xFF1A1428;
    private static final int BG_COLOR_B = 0xFF120E1E;
    private static final int BORDER_COLOR = 0xFF4A3F7A;

    @Override
    public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme widgetTheme) {
        var graphics = context.getGraphics();

        graphics.fillGradient(x, y, x + width, y + height, BG_COLOR_A, BG_COLOR_B);

        graphics.fill(x, y, x + width, y + 1, BORDER_COLOR);
        graphics.fill(x, y + height - 1, x + width, y + height, BORDER_COLOR);
        graphics.fill(x, y, x + 1, y + height, BORDER_COLOR);
        graphics.fill(x + width - 1, y, x + width, y + height, BORDER_COLOR);
    }
}
