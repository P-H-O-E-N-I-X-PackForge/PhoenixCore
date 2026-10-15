package net.phoenix.core.integration.continuum.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.systems.RenderSystem;

final class RocketSprite {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("phoenixcore",
            "textures/item/continuum_rocket.png");
    private static final int TEXEL = 16;

    private RocketSprite() {}

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
