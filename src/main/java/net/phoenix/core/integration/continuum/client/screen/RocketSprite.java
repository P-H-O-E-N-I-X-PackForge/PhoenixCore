package net.phoenix.core.integration.continuum.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * The rocket on the ascent, transit and arrival screens. It is the rocket item's own texture, so the screens and the
 * item in the player's hand are always the same rocket. It is scaled smoothly, because the rocket shrinks and grows as
 * it flies; the texture filter is nearest, so the pixels stay hard-edged at any size.
 */
final class RocketSprite {

    private static final ResourceLocation TEXTURE = new ResourceLocation("phoenixcore",
            "textures/item/continuum_rocket.png");
    private static final int TEXEL = 16;

    private RocketSprite() {}

    /**
     * @param cx           the middle of the rocket
     * @param tailY        the bottom of the rocket
     * @param rocketHeight roughly how tall to draw it
     */
    static void draw(GuiGraphics graphics, float cx, float tailY, float rocketHeight, float alpha) {
        if (alpha <= 0.01f || rocketHeight < 2.0f) return;
        int size = Math.max(TEXEL, Math.round(rocketHeight * 1.1f));
        int x = Math.round(cx - size / 2.0f);
        int y = Math.round(tailY - size);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor(1.0f, 1.0f, 1.0f, Math.min(1.0f, alpha));
        graphics.blit(TEXTURE, x, y, size, size, 0.0f, 0.0f, TEXEL, TEXEL, TEXEL, TEXEL);
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
}
