package net.phoenix.core.integration.conflux.dimension.sky;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import net.phoenix.core.client.worldfx.ShaderProfiler;
import net.phoenix.core.client.worldfx.WorldFXManager;
import net.phoenix.core.client.worldfx.WorldFXShaders;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Discipline dimensions use vanilla's "minecraft:overworld" dimension_type (for its skylight/
 * height/ambient settings), which comes bundled with the full vanilla sun/moon/star/cloud sky -
 * that vanilla sky rendered on top of (or, depending on draw order, behind) the custom
 * PlanetOrbit/etc content is why the discipline dimensions still just showed the
 * ordinary sun and clouds no matter what SkyManager drew. Routing through a dedicated
 * DimensionSpecialEffects (registered under a custom "effects" id used by our own
 * dimension_type) fully replaces vanilla's sky/cloud rendering with SkyManager's output instead
 * of merely drawing over it.
 */
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

            // poseStack here is the same matrix vanilla's own sky dome uses as its modelview
            // (camera rotation baked in), and PlanetOrbit bakes it straight into each vertex
            // position. Left alone, RenderSystem's *global* modelview matrix gets applied a
            // second time at actual draw time - that double rotation is exactly what made the
            // sky content show up but not track the camera correctly. Zeroing the global
            // modelview for the span of the draw avoids applying the camera transform twice.
            // Shader backdrops render first, so the planets/moons SkyManager draws afterward sit
            // in front of them rather than potentially being drawn over.
            // Each call wrapped in ShaderProfiler.time() so the shader-profiler HUD (Numpad-4)
            // can show a real per-shader ms cost instead of a guess - zero overhead when the HUD
            // is off, since time() just runs the call directly in that case.
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

    /**
     * Fullscreen domain-warped-fBm nebula + galaxy-core glow + star field, reconstructing a
     * camera ray per pixel from the inverse view/projection matrices (same technique already
     * proven in NebulaSkyLayer/phoenix_nebula.fsh) - deliberately not a solid mesh, since a
     * cloud/galaxy effect painted per-pixel reads far richer than any vertex-colored geometry
     * could at reasonable polycount.
     */
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

    /**
     * A smaller black hole off to one side of the sky, separate from the galaxy's glowing core
     * (void_galaxy's own GalaxyDir point light, the "purple sun" - see that shader's galDot term)
     * so the two read as two distinct features instead of the hole sitting on top of and hiding
     * the core glow. Replaces VoidSkyRenderer's orbiting planet meshes (see void_black_hole.fsh
     * for the lensing/accretion-disk math and why this one works in screen-space UV instead of
     * camera-ray reconstruction like the other sky shaders).
     *
     * Unlike the fixed-cone sunflare, this needs an actual screen position - lensing bends
     * whatever's already on screen around a point, so "which pixel is the hole's center" has to
     * be a concrete UV. That position is computed the same way the dormant
     * BlackHoleSkyLayer/WorldFXManager block-effect system already does it: project a point far
     * away in the target world direction through the view/projection matrices, same as vanilla
     * projects the sun/moon.
     */
    private static void renderVoidBlackHole(PoseStack poseStack, Matrix4f projectionMatrix, Vec3 cameraPos) {
        ShaderInstance shader = WorldFXShaders.VOID_BLACK_HOLE;
        if (shader == null) return;

        // Deliberately NOT void_galaxy's GalaxyDir (0.3, 0.5, 0.8) - that's the galaxy core glow
        // ("the purple sun"), and this needs to sit well clear of it instead of overlapping.
        Vec3 dir = new Vec3(0.8, 0.1, -0.4).normalize();
        Vec3 world = cameraPos.add(dir.scale(300.0));

        Matrix4f view = new Matrix4f(poseStack.last().pose());
        Vector4f clip = new Vector4f(
                (float) (world.x - cameraPos.x),
                (float) (world.y - cameraPos.y),
                (float) (world.z - cameraPos.z),
                1.0f).mul(view).mul(projectionMatrix);

        // Behind the camera this frame - the projected point is meaningless (or would flip to
        // the wrong side of the screen), so skip drawing entirely rather than show it somewhere
        // nonsensical. The nebula backdrop from renderVoidGalaxy is still visible underneath.
        if (clip.w <= 0.01f) return;

        float screenX = (clip.x / clip.w) * 0.5f + 0.5f;
        float screenY = (clip.y / clip.w) * 0.5f + 0.5f;

        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();

        // Captures whatever renderVoidGalaxy just drew into the frame buffer, so the lensing
        // below bends the actual nebula/stars around the hole instead of a flat sample.
        int skyTexId = WorldFXManager.captureSkyToTexture();
        // captureSkyToTexture's blit leaves framebuffer 0 (the raw default) bound rather than
        // restoring Minecraft's actual main render target - without this rebind, the draw below
        // silently lands in the wrong framebuffer and never reaches the screen.
        mc.getMainRenderTarget().bindWrite(false);

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(() -> shader);

        shader.setSampler("InSampler", skyTexId);
        shader.safeGetUniform("OutSize").set((float) w, (float) h);
        shader.safeGetUniform("BlackHoleScreenPos").set(screenX, screenY);
        // Sized down from the original 0.07/1.6 - a smaller, secondary feature off to the side
        // rather than something competing with the galaxy core glow for focal point.
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

    /**
     * Nimitz's flare/sun-ray shader (see phoenix_sunflare.fsh for the full attribution and the
     * exact macro-resolved formula being ported), anchored to a fixed sky direction instead of
     * the screen center so it reads as an actual sun in the sky.
     */
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
        // Was (0.3, 0.6, 0.4) - normalizes to ~51 degrees above the horizon, well above where a
        // normal, roughly-level camera pitch ever looks without deliberately tilting up. Lowered
        // toward eye level so it's actually visible during ordinary play instead of needing to
        // look almost straight up.
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

    /**
     * Cellular vein network over near-black void - see sculk_abyss.fsh for the Worley-noise
     * technique that gives the cracked/vein look instead of void_galaxy's soft nebula clouds.
     */
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

    /**
     * Glowing angular scaffold over a drifting industrial haze - see sealed_a_industrial.fsh.
     */
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

    /**
     * Chromatic-aberration nebula with glitch-band tearing - see sealed_b_chaos.fsh.
     */
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
        // Discipline dimensions get their atmosphere from SkyManager, not vanilla clouds.
        return true;
    }
}
