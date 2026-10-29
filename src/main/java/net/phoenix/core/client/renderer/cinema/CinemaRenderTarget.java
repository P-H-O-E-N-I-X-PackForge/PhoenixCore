package net.phoenix.core.client.renderer.cinema;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.phoenix.core.client.worldfx.WorldFXShaders;
import net.phoenix.core.common.block.cinema.CinemaScreenBlockEntity.Background;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

public final class CinemaRenderTarget {

    private CinemaRenderTarget() {}

    /** Pixels along a block at most, and along either side of a whole screen: a big screen is not four times as sharp. */
    private static final int SIZE = 256;
    private static final int MAX_SIDE = 640;
    /** A shader picture is redrawn this often, nearer or farther than 16 blocks, and not at all beyond the far distance. */
    private static final long NEAR_INTERVAL_MS = 66;
    private static final long FAR_INTERVAL_MS = 200;
    private static final double FAR_RANGE_SQ = 64.0 * 64.0;
    /** Only this many pictures are redrawn in one frame: with a dozen screens in view they take turns. */
    private static final int PER_FRAME = 1;
    private static long frameStamp;
    private static int drawnThisFrame;

    private record GroupKey(BlockPos anchor, int width, int height, Background background) {}

    private record CacheEntry(RenderTarget target, long[] lastRenderTime, int px) {}

    private static final Map<GroupKey, CacheEntry> cache = new HashMap<>();

    public static int getOrRenderTexture(CinemaGroupUtil.GroupLayout layout, Background background) {
        int width = layout.width();
        int height = layout.height();
        GroupKey key = new GroupKey(layout.anchor(), width, height, background);
        int px = Math.max(64, Math.min(SIZE, MAX_SIDE / Math.max(width, height)));
        CacheEntry entry = cache.computeIfAbsent(key,
                k -> new CacheEntry(new MainTarget(px * width, px * height), new long[] { -1 }, px));

        long now = System.currentTimeMillis();
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double dx = camera.x - (layout.anchor() != null ? layout.anchor().getX() : camera.x);
        double dy = camera.y - (layout.anchor() != null ? layout.anchor().getY() : camera.y);
        double dz = camera.z - (layout.anchor() != null ? layout.anchor().getZ() : camera.z);
        double distSq = dx * dx + dy * dy + dz * dz;
        long interval = distSq < 16.0 * 16.0 ? NEAR_INTERVAL_MS : FAR_INTERVAL_MS;
        boolean first = entry.lastRenderTime()[0] < 0;
        if (first || (distSq < FAR_RANGE_SQ && now - entry.lastRenderTime()[0] >= interval)) {
            long stamp = System.nanoTime() / 4_000_000L;
            if (stamp != frameStamp) {
                frameStamp = stamp;
                drawnThisFrame = 0;
            }
            if (drawnThisFrame < PER_FRAME || first) {
                drawnThisFrame++;
                entry.lastRenderTime()[0] = now;
                renderShader(entry.target(), width, height, entry.px(), background);
            }
        }

        return entry.target().getColorTextureId();
    }

    private static void renderShader(RenderTarget target, int width, int height, int px, Background background) {
        ShaderInstance shader = switch (background) {

            case VOID_GALAXY -> WorldFXShaders.CINEMA_VOID_GALAXY;
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

        RenderSystem.clearColor(0.01f, 0.01f, 0.03f, 1.0f);
        RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT, Minecraft.ON_OSX);

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.setShader(() -> shader);

        float outW = px * width;
        float outH = px * height;
        float time = (float) (System.currentTimeMillis() % 10000000L) / 10000.0f;

        Matrix4f projection = new Matrix4f().perspective(
                (float) Math.toRadians(90.0), outW / outH, 0.05f, 10.0f);
        Matrix4f invProj = new Matrix4f(projection).invert();
        Matrix4f invView = new Matrix4f();

        shader.safeGetUniform("OutSize").set(outW, outH);
        shader.safeGetUniform("InvViewMat").set(invView);
        shader.safeGetUniform("InvProjMat").set(invProj);
        shader.safeGetUniform("Time").set(time);

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
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        bb.addVertex(-1, -1, 0);
        bb.addVertex(1, -1, 0);
        bb.addVertex(1, 1, 0);
        bb.addVertex(-1, 1, 0);
        BufferUploader.drawWithShader(bb.buildOrThrow());
    }
}
