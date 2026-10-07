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

/** A window-sized offscreen target for a screen's 3D pass, plus the blit that puts it behind the GUI. */
public final class SceneTarget implements AutoCloseable {

    private @Nullable RenderTarget target;
    private int width = -1;
    private int height = -1;

    /** (Re)creates the target to match the window. Returns null if it could not be made. */
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

    /** Draws the target over the whole GUI area of {@code guiWidth} x {@code guiHeight}. */
    public void blit(GuiGraphics graphics, int guiWidth, int guiHeight) {
        if (target == null) return;

        RenderSystem.disableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, target.getColorTextureId());

        Matrix4f pose = graphics.pose().last().pose();
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        // a framebuffer texture is upside down relative to the GUI
        bb.vertex(pose, 0, guiHeight, 0).uv(0, 0).endVertex();
        bb.vertex(pose, guiWidth, guiHeight, 0).uv(1, 0).endVertex();
        bb.vertex(pose, guiWidth, 0, 0).uv(1, 1).endVertex();
        bb.vertex(pose, 0, 0, 0).uv(0, 1).endVertex();
        BufferUploader.drawWithShader(bb.end());
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
