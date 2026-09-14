package net.phoenix.core.integration.conflux.dimension.sky;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import net.phoenix.core.client.worldfx.ShaderProfiler;
import net.phoenix.core.client.worldfx.WorldFXManager;
import net.phoenix.core.client.worldfx.WorldFXShaders;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class DisciplineSkyEffects extends DimensionSpecialEffects {

    public DisciplineSkyEffects() {
        super(192.0f, false, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return fogColor.multiply(brightness, brightness, brightness);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, Camera camera,
                             Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        if (isFoggy) return true;

        SkyManager manager = SkyManager.getInstance();
        if (!manager.isSkyRenderingEnabled()) return true;

        try {
            float skyBrightness = level.getSkyDarken() / 11.0f;
            Minecraft mc = Minecraft.getInstance();

            String currentDiscipline = SkyManager.getInstance().getCurrentDimension();
            if ("void".equals(currentDiscipline)) {
                ShaderProfiler.time("void_galaxy", () -> renderVoidGalaxy(poseStack, projectionMatrix));
                ShaderProfiler.time("void_black_hole",
                        () -> renderVoidBlackHole(poseStack, projectionMatrix, camera.getPosition()));
            } else if ("phoenix".equals(currentDiscipline)) {
                ShaderProfiler.time("phoenix_sunflare", () -> renderPhoenixSunflare(poseStack, projectionMatrix));
            } else if ("sculk".equals(currentDiscipline)) {
                ShaderProfiler.time("sculk_abyss", () -> renderSculkAbyss(poseStack, projectionMatrix));
            } else if ("sealed_a".equals(currentDiscipline)) {
                ShaderProfiler.time("sealed_a_industrial", () -> renderSealedAIndustrial(poseStack, projectionMatrix));
            } else if ("sealed_b".equals(currentDiscipline)) {
                ShaderProfiler.time("sealed_b_chaos", () -> renderSealedBChaos(poseStack, projectionMatrix));
            }

            RenderSystem.getModelViewStack().pushPose();
            RenderSystem.getModelViewStack().setIdentity();
            RenderSystem.applyModelViewMatrix();

            manager.render(poseStack, mc.renderBuffers().bufferSource(), partialTick, skyBrightness);
            mc.renderBuffers().bufferSource().endBatch();

            RenderSystem.getModelViewStack().popPose();
            RenderSystem.applyModelViewMatrix();
        } catch (Exception e) {
            System.err.println("[PhoenixCore Sky] Error rendering sky: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    private static void renderVoidGalaxy(PoseStack poseStack, Matrix4f projectionMatrix) {
        ShaderInstance shader = WorldFXShaders.VOID_GALAXY;
        if (shader == null) return;

        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();

        Matrix4f invView = new Matrix4f(poseStack.last().pose()).invert();
        Matrix4f invProj = new Matrix4f(projectionMatrix).invert();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(() -> shader);

        shader.safeGetUniform("OutSize").set((float) w, (float) h);
        shader.safeGetUniform("InvViewMat").set(invView);
        shader.safeGetUniform("InvProjMat").set(invProj);
        shader.safeGetUniform("PrimaryColor").set(0.7f, 0.15f, 0.9f);
        shader.safeGetUniform("SecondaryColor").set(0.15f, 0.35f, 1.0f);
        shader.safeGetUniform("GalaxyDir").set(0.3f, 0.5f, 0.8f);
        shader.safeGetUniform("Density").set(0.55f);
        shader.safeGetUniform("Scale").set(1.1f);
        shader.safeGetUniform("Seed").set(0.0f);
        shader.safeGetUniform("Time").set((float) (System.currentTimeMillis() % 10000000L) / 10000.0f);

        shader.apply();
        drawNdcQuad();
        shader.clear();

        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    private static void renderVoidBlackHole(PoseStack poseStack, Matrix4f projectionMatrix, Vec3 cameraPos) {
        ShaderInstance shader = WorldFXShaders.VOID_BLACK_HOLE;
        if (shader == null) return;

        Vec3 dir = new Vec3(0.8, 0.1, -0.4).normalize();
        Vec3 world = cameraPos.add(dir.scale(300.0));

        Matrix4f view = new Matrix4f(poseStack.last().pose());
        Vector4f clip = new Vector4f(
                (float) (world.x - cameraPos.x),
                (float) (world.y - cameraPos.y),
                (float) (world.z - cameraPos.z),
                1.0f).mul(view).mul(projectionMatrix);

        if (clip.w <= 0.01f) return;

        float screenX = (clip.x / clip.w) * 0.5f + 0.5f;
        float screenY = (clip.y / clip.w) * 0.5f + 0.5f;

        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();

        int skyTexId = WorldFXManager.captureSkyToTexture();

        mc.getMainRenderTarget().bindWrite(false);

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(() -> shader);

        shader.setSampler("InSampler", skyTexId);
        shader.safeGetUniform("OutSize").set((float) w, (float) h);
        shader.safeGetUniform("BlackHoleScreenPos").set(screenX, screenY);

        shader.safeGetUniform("EventHorizonRadius").set(0.045f);
        shader.safeGetUniform("LensingStrength").set(0.015f);
        shader.safeGetUniform("AccretionDiskBrightness").set(1.3f);
        shader.safeGetUniform("AspectRatio").set((float) w / h);
        shader.safeGetUniform("Time").set((float) (System.currentTimeMillis() % 10000000L) / 1000.0f);

        shader.apply();
        drawNdcQuad();
        shader.clear();

        RenderSystem.enableDepthTest();
    }

    private static void renderPhoenixSunflare(PoseStack poseStack, Matrix4f projectionMatrix) {
        ShaderInstance shader = WorldFXShaders.PHOENIX_SUNFLARE;
        if (shader == null) return;

        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();

        Matrix4f invView = new Matrix4f(poseStack.last().pose()).invert();
        Matrix4f invProj = new Matrix4f(projectionMatrix).invert();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(() -> shader);

        shader.safeGetUniform("OutSize").set((float) w, (float) h);
        shader.safeGetUniform("InvViewMat").set(invView);
        shader.safeGetUniform("InvProjMat").set(invProj);

        shader.safeGetUniform("SunDir").set(0.55f, 0.28f, 0.65f);
        shader.safeGetUniform("FlareColor").set(4.0f, 1.0f, 0.3f);
        shader.safeGetUniform("CoreRadius").set(24.0f);
        shader.safeGetUniform("Time").set((float) (System.currentTimeMillis() % 10000000L) / 10000.0f);

        shader.apply();
        drawNdcQuad();
        shader.clear();

        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    private static void renderSculkAbyss(PoseStack poseStack, Matrix4f projectionMatrix) {
        ShaderInstance shader = WorldFXShaders.SCULK_ABYSS;
        if (shader == null) return;

        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();

        Matrix4f invView = new Matrix4f(poseStack.last().pose()).invert();
        Matrix4f invProj = new Matrix4f(projectionMatrix).invert();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(() -> shader);

        shader.safeGetUniform("OutSize").set((float) w, (float) h);
        shader.safeGetUniform("InvViewMat").set(invView);
        shader.safeGetUniform("InvProjMat").set(invProj);
        shader.safeGetUniform("VeinColor").set(0.1f, 1.0f, 0.75f);
        shader.safeGetUniform("GlowColor").set(0.0f, 0.6f, 0.55f);
        shader.safeGetUniform("Time").set((float) (System.currentTimeMillis() % 10000000L) / 1000.0f);

        shader.apply();
        drawNdcQuad();
        shader.clear();

        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    private static void renderSealedAIndustrial(PoseStack poseStack, Matrix4f projectionMatrix) {
        ShaderInstance shader = WorldFXShaders.SEALED_A_INDUSTRIAL;
        if (shader == null) return;

        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();

        Matrix4f invView = new Matrix4f(poseStack.last().pose()).invert();
        Matrix4f invProj = new Matrix4f(projectionMatrix).invert();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(() -> shader);

        shader.safeGetUniform("OutSize").set((float) w, (float) h);
        shader.safeGetUniform("InvViewMat").set(invView);
        shader.safeGetUniform("InvProjMat").set(invProj);
        shader.safeGetUniform("GridColor").set(1.0f, 0.2f, 0.9f);
        shader.safeGetUniform("HazeColor").set(0.15f, 0.5f, 0.55f);
        shader.safeGetUniform("Time").set((float) (System.currentTimeMillis() % 10000000L) / 1000.0f);

        shader.apply();
        drawNdcQuad();
        shader.clear();

        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    private static void renderSealedBChaos(PoseStack poseStack, Matrix4f projectionMatrix) {
        ShaderInstance shader = WorldFXShaders.SEALED_B_CHAOS;
        if (shader == null) return;

        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();

        Matrix4f invView = new Matrix4f(poseStack.last().pose()).invert();
        Matrix4f invProj = new Matrix4f(projectionMatrix).invert();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(() -> shader);

        shader.safeGetUniform("OutSize").set((float) w, (float) h);
        shader.safeGetUniform("InvViewMat").set(invView);
        shader.safeGetUniform("InvProjMat").set(invProj);
        shader.safeGetUniform("PrimaryColor").set(1.0f, 0.15f, 0.85f);
        shader.safeGetUniform("SecondaryColor").set(0.85f, 0.85f, 0.1f);
        shader.safeGetUniform("Time").set((float) (System.currentTimeMillis() % 10000000L) / 1000.0f);

        shader.apply();
        drawNdcQuad();
        shader.clear();

        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    private static void drawNdcQuad() {
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        bb.vertex(-1, -1, 0).endVertex();
        bb.vertex(1, -1, 0).endVertex();
        bb.vertex(1, 1, 0).endVertex();
        bb.vertex(-1, 1, 0).endVertex();
        Tesselator.getInstance().end();
    }

    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack,
                                double camX, double camY, double camZ, Matrix4f projectionMatrix) {
        return true;
    }
}
