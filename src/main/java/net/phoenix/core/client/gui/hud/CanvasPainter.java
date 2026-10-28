package net.phoenix.core.client.gui.hud;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Immediate-mode drawing surface for HUD style machine screens. Everything is drawn in local coordinates with the
 * origin at the widget's top-left corner (the pose stack is already translated).
 */
public interface CanvasPainter {

    void render(GuiGraphics g, int w, int h, int mouseX, int mouseY);

    default boolean click(double mouseX, double mouseY, int button) {
        return false;
    }

    /** The mouse moved with a button held, after a click that this painter took. */
    default void drag(double mouseX, double mouseY, int button) {}

    default void release(int button) {}

    default boolean scroll(double mouseX, double mouseY, double amount) {
        return false;
    }
}
