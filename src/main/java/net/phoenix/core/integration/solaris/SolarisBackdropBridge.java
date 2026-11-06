package net.phoenix.core.integration.solaris;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.client.worldfx.WorldFXShaders;
import net.phoenixvine.solaris.api.SolarisAPI;
import net.phoenixvine.solaris.client.render.UnexploredBackdrops;
import net.phoenixvine.solaris.client.render.UnexploredStyle;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
final class SolarisBackdropBridge {

    private record Look(Supplier<ShaderInstance> shader, float timeDivisor, Consumer<ShaderInstance> uniforms) {}

    private static final Map<String, Look> LOOKS = Map.of(
            "phoenix", new Look(() -> WorldFXShaders.NEBULA, 10000f, shader -> {
                shader.safeGetUniform("PrimaryColor").set(1.0f, 0.35f, 0.05f);
                shader.safeGetUniform("SecondaryColor").set(0.9f, 0.1f, 0.0f);
                shader.safeGetUniform("Density").set(0.5f);
                shader.safeGetUniform("Scale").set(1.2f);
                shader.safeGetUniform("Seed").set(0.0f);
            }),
            "void", new Look(() -> WorldFXShaders.VOID_GALAXY, 10000f, shader -> {
                shader.safeGetUniform("PrimaryColor").set(0.7f, 0.15f, 0.9f);
                shader.safeGetUniform("SecondaryColor").set(0.15f, 0.35f, 1.0f);
                shader.safeGetUniform("GalaxyDir").set(0.3f, 0.5f, 0.8f);
                shader.safeGetUniform("Density").set(0.55f);
                shader.safeGetUniform("Scale").set(1.1f);
                shader.safeGetUniform("Seed").set(0.0f);
            }),
            "sculk", new Look(() -> WorldFXShaders.SCULK_ABYSS, 1000f, shader -> {
                shader.safeGetUniform("VeinColor").set(0.1f, 1.0f, 0.75f);
                shader.safeGetUniform("GlowColor").set(0.0f, 0.6f, 0.55f);
            }),
            "sealed_a", new Look(() -> WorldFXShaders.SEALED_A_INDUSTRIAL, 1000f, shader -> {
                shader.safeGetUniform("GridColor").set(1.0f, 0.2f, 0.9f);
                shader.safeGetUniform("HazeColor").set(0.15f, 0.5f, 0.55f);
            }),
            "sealed_b", new Look(() -> WorldFXShaders.SEALED_B_CHAOS, 1000f, shader -> {
                shader.safeGetUniform("PrimaryColor").set(1.0f, 0.15f, 0.85f);
                shader.safeGetUniform("SecondaryColor").set(0.85f, 0.85f, 0.1f);
            }));

    private static boolean registered = false;

    private SolarisBackdropBridge() {}

    static void registerAll() {
        if (registered) return;
        registered = true;

        for (Map.Entry<String, Look> entry : LOOKS.entrySet()) {
            ResourceLocation dimension = ResourceLocation.fromNamespaceAndPath("phoenixcore",
                    "conflux/" + entry.getKey());
            Look look = entry.getValue();
            UnexploredBackdrops.set(dimension,
                    (g, left, top, right, bottom, centerX, centerZ, partialTick) -> draw(g, look, centerX, centerZ));
            SolarisAPI.setUnexploredStyle(dimension, UnexploredStyle.BACKDROP);
        }
    }

    private static void draw(GuiGraphics g, Look look, double centerX, double centerZ) {
        ShaderInstance shader = look.shader().get();
        if (shader == null) return;

        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();

        float yaw = (float) (centerX * 0.0015);
        float pitch = (float) Mth.clamp(centerZ * 0.001, -1.1, 1.1);
        Matrix4f invView = new Matrix4f().rotateX(pitch).rotateY(yaw).invert();
        Matrix4f invProj = new Matrix4f().perspective((float) Math.toRadians(70.0), (float) w / h, 0.05f, 1000f)
                .invert();

        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(() -> shader);

        shader.safeGetUniform("OutSize").set((float) w, (float) h);
        shader.safeGetUniform("InvViewMat").set(invView);
        shader.safeGetUniform("InvProjMat").set(invProj);
        look.uniforms().accept(shader);
        shader.safeGetUniform("Time").set((float) (System.currentTimeMillis() % 10000000L) / look.timeDivisor());

        shader.apply();
        BufferBuilder quad = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        quad.addVertex(-1, -1, 0);
        quad.addVertex(1, -1, 0);
        quad.addVertex(1, 1, 0);
        quad.addVertex(-1, 1, 0);
        BufferUploader.drawWithShader(quad.buildOrThrow());
        shader.clear();

        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
    }
}
