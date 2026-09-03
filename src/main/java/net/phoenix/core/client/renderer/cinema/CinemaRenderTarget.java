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
import net.minecraft.core.BlockPos;
import net.phoenix.core.client.worldfx.WorldFXShaders;

import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

public final class CinemaRenderTarget {

    private CinemaRenderTarget() {}

    private static final int SIZE = 256;
    private static final long REFRESH_INTERVAL_MS = 50;

    private record GroupKey(BlockPos anchor, int width, int height) {}
    private record CacheEntry(RenderTarget target, long[] lastRenderTime) {}

    private static final Map<GroupKey, CacheEntry> cache = new HashMap<>();

    public static int getOrRenderTexture(CinemaGroupUtil.GroupLayout layout) {
        int width = layout.width();
        int height = layout.height();
        GroupKey key = new GroupKey(layout.anchor(), width, height);
        CacheEntry entry = cache.computeIfAbsent(key,
                k -> new CacheEntry(new MainTarget(SIZE * width, SIZE * height), new long[] { -1 }));

        long now = System.currentTimeMillis();
        if (now - entry.lastRenderTime()[0] >= REFRESH_INTERVAL_MS) {
            entry.lastRenderTime()[0] = now;
            renderShader(entry.target(), width, height);
        }

        return entry.target().getColorTextureId();
    }

    private static void renderShader(RenderTarget target, int width, int height) {
        ShaderInstance shader = WorldFXShaders.VOID_GALAXY;
        if (shader == null) return;

        Minecraft mc = Minecraft.getInstance();
        RenderTarget previous = mc.getMainRenderTarget();

        target.bindWrite(true);

        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShader(() -> shader);

        Matrix4f identity = new Matrix4f();

        shader.safeGetUniform("OutSize").set((float) (SIZE * width), (float) (SIZE * height));
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
        drawFullNdcQuad();
        shader.clear();

        previous.bindWrite(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
    }

    private static void drawFullNdcQuad() {
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        bb.vertex(-1, -1, 0).endVertex();
        bb.vertex(1, -1, 0).endVertex();
        bb.vertex(1, 1, 0).endVertex();
        bb.vertex(-1, 1, 0).endVertex();
        Tesselator.getInstance().end();
    }
}
