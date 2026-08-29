package net.phoenix.core.integration.drone.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.screen.viewport.GuiContext;
import brachy.modularui.theme.WidgetTheme;

@OnlyIn(Dist.CLIENT)
public class DroneUIBackground implements IDrawable {

    private static final int BG_COLOR_A = 0xFF071018;
    private static final int BG_COLOR_B = 0xFF03080C;
    private static final int GRID_COLOR = 0x0F44E0FF;
    private static final int BORDER_COLOR = 0xFF2FD9FF;

    @Override
    public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme widgetTheme) {
        var graphics = context.getGraphics();

        graphics.fillGradient(x, y, x + width, y + height, BG_COLOR_A, BG_COLOR_B);

        int spacing = 16;
        for (int gx = x + spacing; gx < x + width; gx += spacing) {
            graphics.fill(gx, y, gx + 1, y + height, GRID_COLOR);
        }
        for (int gy = y + spacing; gy < y + height; gy += spacing) {
            graphics.fill(x, gy, x + width, gy + 1, GRID_COLOR);
        }

        graphics.fill(x, y, x + width, y + 1, BORDER_COLOR);
        graphics.fill(x, y + height - 1, x + width, y + height, BORDER_COLOR);
        graphics.fill(x, y, x + 1, y + height, BORDER_COLOR);
        graphics.fill(x + width - 1, y, x + width, y + height, BORDER_COLOR);
    }
}
