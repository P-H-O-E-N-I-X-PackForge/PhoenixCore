package net.phoenix.core.integration.continuum.client.pdim;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.phoenix.core.integration.continuum.client.ContinuumVisuals;
import net.phoenix.core.integration.continuum.client.render.ContinuumShaders;
import net.phoenix.core.integration.continuum.client.render.SceneRenderer;
import net.phoenix.core.integration.continuum.data.ContinuumBody;
import net.phoenix.core.integration.continuum.data.ContinuumData;
import net.phoenix.core.integration.continuum.data.ContinuumSystem;
import net.phoenix.core.integration.continuum.pdim.PdimDimensions;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class PdimSkyEffects extends DimensionSpecialEffects {

    public PdimSkyEffects() {
        super(Float.NaN, false, SkyType.NONE, false, false);
    }

    /** The horizon colour of the sky as last drawn, so distant fog fades into it; null until the first frame. */
    private static volatile Vec3 horizonFog = null;

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        Vec3 fog = horizonFog;
        return fog != null ? fog : fogColor;
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix, Camera camera,
                             Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(modelViewMatrix);
        ResourceLocation id = PdimDimensions.bodyOf(level.dimension());
        ContinuumBody body = id == null ? null : ContinuumData.body(id);
        ContinuumSystem system = body == null ? null : ContinuumData.system(body.system());
        Matrix4f view = new Matrix4f(poseStack.last().pose());
        float time = (float) (System.currentTimeMillis() % 10000000L) / 1000.0f;

        PdimSkyModel.Sample sample = PdimSkyModel.sample(level, partialTick, body, system);
        float[] fog = sample.horizonNow();
        horizonFog = new Vec3(fog[0], fog[1], fog[2]);

        drawNebula(view, projectionMatrix, time, body, system, sample);

        if (body != null && ContinuumShaders.ready()) {
            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);

            // the star crossing the sky: it rises, climbs, sets, and is not there at night
            if (sample.hasSun() && system != null && sample.sunDir().y > -0.12f) {
                Vector3f sunPos = new Vector3f(sample.sunDir()).mul(98.0f);
                SceneRenderer.drawSystemStar(system, system.starColor(), view, projectionMatrix, sunPos, 6.0f,
                        ContinuumVisuals.quality(), true);
                RenderSystem.enableCull();
                RenderSystem.enableDepthTest();
                RenderSystem.depthMask(true);
            }

            Vector3f position = new Vector3f(52.0f, 30.0f, -76.0f);
            boolean centralBody = body.type() == ContinuumBody.Type.STAR ||
                    body.type() == ContinuumBody.Type.BLACK_HOLE;
            boolean quasar = system != null && system.isQuasar();
            // skybox holes are drawn larger than on the map screen; a quasar's jets already span the sky, so it grows less
            float radius = body.type() == ContinuumBody.Type.BLACK_HOLE ? (quasar ? 8.5f : 9.0f) :
                    centralBody ? 16.0f : 22.0f;

            if (!SceneRenderer.drawCentralBody(body, view, projectionMatrix, position, radius,
                    ContinuumVisuals.quality(), true)) {
                var params = body.params();
                SceneRenderer.drawPlanet(params, view, projectionMatrix, position, radius,
                        new Vector3f(-300.0f, 220.0f, 260.0f), time * params.spinDegPerSec() * 0.5f,
                        time * params.cloudSpinDegPerSec() * 0.5f, ContinuumVisuals.quality(), true);
            }
        }

        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        return true;
    }

    private static void drawNebula(Matrix4f view, Matrix4f projection, float time, ContinuumBody body,
                                   ContinuumSystem system, PdimSkyModel.Sample sample) {
        ShaderInstance sky = ContinuumShaders.SKY;
        if (sky == null) return;

        int primary = body != null ? body.params().atmoDensity() > 0.05f ? body.params().atmoColor() :
                body.params().high() : 0x5a3aa8;
        int secondary = system != null ? system.starColor() : 0x2a5ad0;

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();
        RenderSystem.setShader(() -> sky);

        sky.safeGetUniform("InvViewMat").set(new Matrix4f(view).invert());
        sky.safeGetUniform("InvProjMat").set(new Matrix4f(projection).invert());
        sky.safeGetUniform("Time").set(time);
        sky.safeGetUniform("Octaves").set((float) ContinuumVisuals.backdropOctaves());
        sky.safeGetUniform("PrimaryColor").set(((primary >> 16) & 0xFF) / 255.0f * 0.9f,
                ((primary >> 8) & 0xFF) / 255.0f * 0.9f, (primary & 0xFF) / 255.0f * 0.9f);
        sky.safeGetUniform("SecondaryColor").set(((secondary >> 16) & 0xFF) / 255.0f * 0.7f,
                ((secondary >> 8) & 0xFF) / 255.0f * 0.7f, (secondary & 0xFF) / 255.0f * 0.7f);

        sky.safeGetUniform("SunDir").set(sample.sunDir().x, sample.sunDir().y, sample.sunDir().z);
        sky.safeGetUniform("Atmo").set(sample.atmo());
        sky.safeGetUniform("ZenithColor").set(sample.zenith()[0], sample.zenith()[1], sample.zenith()[2]);
        sky.safeGetUniform("HorizonColor").set(sample.horizon()[0], sample.horizon()[1], sample.horizon()[2]);
        sky.safeGetUniform("DuskColor").set(sample.dusk()[0], sample.dusk()[1], sample.dusk()[2]);
        sky.safeGetUniform("SunColor").set(sample.sunColor()[0], sample.sunColor()[1], sample.sunColor()[2]);

        sky.apply();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        bb.addVertex(-1, -1, 0);
        bb.addVertex(1, -1, 0);
        bb.addVertex(1, 1, 0);
        bb.addVertex(-1, 1, 0);
        BufferUploader.drawWithShader(bb.buildOrThrow());
        sky.clear();
    }
}
