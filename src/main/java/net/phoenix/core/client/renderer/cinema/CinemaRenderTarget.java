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
import net.phoenix.core.common.block.cinema.CinemaScreenBlockEntity.Background;

import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

public final class CinemaRenderTarget {

    private CinemaRenderTarget() {}

    private static final int SIZE = 256;
    private static final long REFRESH_INTERVAL_MS = 50;

    private record GroupKey(BlockPos anchor, int width, int height, Background background) {}
    private record CacheEntry(RenderTarget target, long[] lastRenderTime) {}

    private static final Map<GroupKey, CacheEntry> cache = new HashMap<>();

    public static int getOrRenderTexture(CinemaGroupUtil.GroupLayout layout, Background background) {
        int width = layout.width();
        int height = layout.height();
        GroupKey key = new GroupKey(layout.anchor(), width, height, background);
        CacheEntry entry = cache.computeIfAbsent(key,
                k -> new CacheEntry(new MainTarget(SIZE * width, SIZE * height), new long[] { -1 }));

        long now = System.currentTimeMillis();
        if (now - entry.lastRenderTime()[0] >= REFRESH_INTERVAL_MS) {
            entry.lastRenderTime()[0] = now;
            renderShader(entry.target(), width, height, background);
        }

        return entry.target().getColorTextureId();
    }

    private static void renderShader(RenderTarget target, int width, int height, Background background) {
        ShaderInstance shader = switch (background) {
            case VOID_GALAXY -> WorldFXShaders.VOID_GALAXY;
            case NEBULA -> WorldFXShaders.NEBULA;
            case SCULK_ABYSS -> WorldFXShaders.SCULK_ABYSS;
            case SEALED_INDUSTRIAL -> WorldFXShaders.SEALED_A_INDUSTRIAL;
            case SEALED_CHAOS -> WorldFXShaders.SEALED_B_CHAOS;
            case SUNFLARE -> WorldFXShaders.PHOENIX_SUNFLARE;
        };
        if (shader == null) return;

        Minecraft mc = Minecraft.getInstance();
        RenderTarget previous = mc.getMainRenderTarget();

        target.bindWrite(true);

        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShader(() -> shader);

        Matrix4f identity = new Matrix4f();
        float outW = SIZE * width;
        float outH = SIZE * height;
        float time = (float) (System.currentTimeMillis() % 10000000L) / 10000.0f;

        shader.safeGetUniform("OutSize").set(outW, outH);
        shader.safeGetUniform("InvViewMat").set(identity);
        shader.safeGetUniform("InvProjMat").set(identity);
        shader.safeGetUniform("Time").set(time);

        shader.safeGetUniform("ForceOpaque").set(1.0f);

        switch (background) {
            case VOID_GALAXY -> {
                shader.safeGetUniform("PrimaryColor").set(0.7f, 0.15f, 0.9f);
                shader.safeGetUniform("SecondaryColor").set(0.15f, 0.35f, 1.0f);
                shader.safeGetUniform("GalaxyDir").set(0.3f, 0.5f, 0.8f);
                shader.safeGetUniform("Density").set(0.55f);
                shader.safeGetUniform("Scale").set(1.1f);
                shader.safeGetUniform("Seed").set(0.0f);
            }
            case NEBULA -> {
                shader.safeGetUniform("PrimaryColor").set(0.7f, 0.15f, 0.9f);
                shader.safeGetUniform("SecondaryColor").set(0.15f, 0.35f, 1.0f);
                shader.safeGetUniform("Density").set(0.55f);
                shader.safeGetUniform("Scale").set(1.1f);
                shader.safeGetUniform("Seed").set(0.0f);
            }
            case SCULK_ABYSS -> {
                shader.safeGetUniform("VeinColor").set(0.1f, 1.0f, 0.75f);
                shader.safeGetUniform("GlowColor").set(0.0f, 0.6f, 0.55f);
            }
            case SEALED_INDUSTRIAL -> {
                shader.safeGetUniform("GridColor").set(1.0f, 0.2f, 0.9f);
                shader.safeGetUniform("HazeColor").set(0.15f, 0.5f, 0.55f);
            }
            case SEALED_CHAOS -> {
                shader.safeGetUniform("PrimaryColor").set(1.0f, 0.15f, 0.85f);
                shader.safeGetUniform("SecondaryColor").set(0.85f, 0.85f, 0.1f);
            }
            case SUNFLARE -> {
                shader.safeGetUniform("SunDir").set(0.55f, 0.28f, 0.65f);
                shader.safeGetUniform("FlareColor").set(4.0f, 1.0f, 0.3f);
                shader.safeGetUniform("CoreRadius").set(24.0f);
            }
        }

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
