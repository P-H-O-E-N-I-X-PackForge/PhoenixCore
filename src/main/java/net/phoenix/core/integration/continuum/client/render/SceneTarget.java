package net.phoenix.core.integration.continuum.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public final class SceneTarget implements AutoCloseable {

    private @Nullable RenderTarget target;
    private int width = -1;
    private int height = -1;

    public @Nullable RenderTarget ensure() {
        Minecraft mc = Minecraft.getInstance();
        int w = Math.max(64, mc.getWindow().getWidth());
        int h = Math.max(64, mc.getWindow().getHeight());
        if (target != null && width == w && height == h) return target;

        close();
        target = new TextureTarget(w, h, true, Minecraft.ON_OSX);
        width = w;
        height = h;
        return target;
    }

    public int pixelWidth() {
        return width;
    }

    public int pixelHeight() {
        return height;
    }

    public void blit(GuiGraphics graphics, int guiWidth, int guiHeight) {
        if (target == null) return;

        graphics.fill(0, 0, guiWidth, guiHeight, 0xFF05060f);

        RenderSystem.disableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, target.getColorTextureId());

        Matrix4f pose = graphics.pose().last().pose();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        bb.addVertex(pose, -1, guiHeight + 1, 0).setUv(0, 0);
        bb.addVertex(pose, guiWidth + 1, guiHeight + 1, 0).setUv(1, 0);
        bb.addVertex(pose, guiWidth + 1, -1, 0).setUv(1, 1);
        bb.addVertex(pose, -1, -1, 0).setUv(0, 1);
        BufferUploader.drawWithShader(bb.buildOrThrow());
        RenderSystem.enableBlend();
    }

    @Override
    public void close() {
        if (target != null) {
            target.destroyBuffers();
            target = null;
        }
        width = -1;
        height = -1;
    }
}
