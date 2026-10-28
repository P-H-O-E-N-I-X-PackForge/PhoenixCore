package net.phoenix.core.client.gui.hud;

import net.minecraft.client.gui.GuiGraphics;

public interface CanvasPainter {

    void render(GuiGraphics g, int w, int h, int mouseX, int mouseY);

    default boolean click(double mouseX, double mouseY, int button) {
        return false;
    }

    default void drag(double mouseX, double mouseY, int button) {}

    default void release(int button) {}

    default boolean scroll(double mouseX, double mouseY, double amount) {
        return false;
    }
}
