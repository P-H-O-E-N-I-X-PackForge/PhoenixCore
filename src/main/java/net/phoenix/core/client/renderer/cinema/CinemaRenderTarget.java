package net.phoenix.core.client.renderer.cinema;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.phoenix.core.client.worldfx.WorldFXShaders;

import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

public final class CinemaRenderTarget {

    private CinemaRenderTarget() {}

    private static final int SIZE = 256;
    private static final long REFRESH_INTERVAL_MS = 50; 

    private record CellKey(int col, int row, int width, int height) {}
    private record CacheEntry(RenderTarget target, long[] lastRenderTime) {}

    private static final Map<CellKey, CacheEntry> cache = new HashMap<>();

    public static int getOrRenderTexture(CinemaGroupUtil.GroupLayout layout) {
        CellKey key = new CellKey(layout.col(), layout.row(), layout.width(), layout.height());
        CacheEntry entry = cache.computeIfAbsent(key,
                k -> new CacheEntry(new MainTarget(SIZE, SIZE), new long[] { -1 }));

        long now = System.currentTimeMillis();
        if (now - entry.lastRenderTime()[0] >= REFRESH_INTERVAL_MS) {
            entry.lastRenderTime()[0] = now;
            renderShader(entry.target(), layout);
        }

        return entry.target().getColorTextureId();
    }

    private static void renderShader(RenderTarget target, CinemaGroupUtil.GroupLayout layout) {
        ShaderInstance shader = WorldFXShaders.VOID_GALAXY;
        if (shader == null) return;

        Minecraft mc = Minecraft.getInstance();
        RenderTarget previous = mc.getMainRenderTarget();

        target.bindWrite(true);

        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShader(() -> shader);

        Matrix4f identity = new Matrix4f();

        shader.safeGetUniform("OutSize").set((float) SIZE, (float) SIZE);
        shader.safeGetUniform("InvViewMat").set(identity);
        shader.safeGetUniform("InvProjMat").set(identity);
        shader.safeGetUniform("PrimaryColor").set(0.7f, 0.15f, 0.9f);
        shader.safeGetUniform("SecondaryColor").set(0.15f, 0.35f, 1.0f);
        shader.safeGetUniform("GalaxyDir").set(0.3f, 0.5f, 0.8f);
        shader.safeGetUniform("Density").set(0.55f);
        shader.safeGetUniform("Scale").set(1.1f);
        shader.safeGetUniform("Seed").set(0.0f);
        shader.safeGetUniform("Time").set((float) (System.currentTimeMillis() % 10000000L) / 10000.0f);

        shader.apply();
        drawCroppedNdcQuad(layout);
        shader.clear();

        previous.bindWrite(true);
        RenderSystem.enableDepthTest();
    }

    private static void drawCroppedNdcQuad(CinemaGroupUtil.GroupLayout layout) {
        float cellW = 2.0f / layout.width();
        float cellH = 2.0f / layout.height();
        float x0 = -1.0f + layout.col() * cellW;
        float x1 = x0 + cellW;

        float y1 = 1.0f - layout.row() * cellH;
        float y0 = y1 - cellH;

        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        bb.vertex(x0, y0, 0).endVertex();
        bb.vertex(x1, y0, 0).endVertex();
        bb.vertex(x1, y1, 0).endVertex();
        bb.vertex(x0, y1, 0).endVertex();
        Tesselator.getInstance().end();
    }
}
