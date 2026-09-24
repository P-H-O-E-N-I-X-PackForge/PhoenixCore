package net.phoenix.core.client.renderer.cinema.cutscene.background;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Something drawn full-screen behind cutscene text. Build one in code with {@link CutsceneBackgroundBuilder}
 * or the factories in {@link CutsceneBackgrounds}, or load one from JSON via {@link CutsceneBackgrounds#CODEC}.
 */
@FunctionalInterface
public interface CutsceneBackground {

    void render(GuiGraphics graphics, BackgroundContext context);

    /** A background that can be read from JSON. {@link #type()} is its key in {@link CutsceneBackgrounds}. */
    interface Data extends CutsceneBackground {

        String type();
    }

    /**
     * @param time  seconds since the cutscene started
     * @param alpha overall opacity (used for fading the cutscene out)
     */
    record BackgroundContext(int width, int height, float time, float alpha, int mouseX, int mouseY) {}
}
