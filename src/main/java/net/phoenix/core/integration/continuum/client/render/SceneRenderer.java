package net.phoenix.core.integration.continuum.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.phoenix.core.client.worldfx.WorldFXShaders;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The 3D half of every Continuum view: bind an offscreen {@link RenderTarget}, lay the nebula down as a backdrop,
 * draw any number of planets and stars into it with explicit view/projection matrices, then hand the target back to
 * the caller to blit. Nothing here touches the world or the main projection.
 */
public final class SceneRenderer {

    private SceneRenderer() {}

    private static final float ATMOSPHERE_SCALE = 1.06f;

    private static @Nullable RenderTarget previous;

    /** Clears and binds {@code target}, then draws the backdrop. Pair with {@link #end()}. */
    public static void begin(RenderTarget target, float backdropYawDeg) {
        previous = Minecraft.getInstance().getMainRenderTarget();

        target.setClearColor(0.004f, 0.005f, 0.014f, 1.0f);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);

        drawBackdrop(target.width, target.height, backdropYawDeg);

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static void end() {
        if (previous != null) previous.bindWrite(true);
        previous = null;
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
    }

    public static Matrix4f projection(float width, float height, float fovDeg) {
        return new Matrix4f().perspective((float) Math.toRadians(fovDeg), width / height, 0.1f, 400.0f);
    }

    private static float time() {
        return (float) (System.currentTimeMillis() % 10000000L) / 1000.0f;
    }

    /**
     * Draws a planet or moon: the surface pass, then (optionally) the atmosphere shell.
     *
     * @param sunWorld where the light comes from, in the same space as {@code worldPos}
     */
    public static void drawPlanet(PlanetParams params, Matrix4f view, Matrix4f projection, Vector3f worldPos,
                                  float radius, Vector3f sunWorld, float spinDeg, float cloudSpinDeg,
                                  PlanetRenderer.Quality quality, boolean atmosphere) {
        if (!ContinuumShaders.ready()) return;

        Matrix4f modelView = new Matrix4f(view)
                .translate(worldPos)
                .rotateZ((float) Math.toRadians(params.axialTiltDeg()))
                .rotateY((float) Math.toRadians(spinDeg))
                .scale(radius);

        // the sun direction shaders want is in view space, from this body toward the light
        Vector3f bodyView = view.transformPosition(new Vector3f(worldPos));
        Vector3f sunView = view.transformPosition(new Vector3f(sunWorld)).sub(bodyView).normalize();

        IcosphereMesh mesh = PlanetRenderer.mesh(quality);

        ShaderInstance planet = ContinuumShaders.PLANET;
        planet.safeGetUniform("SunDir").set(sunView.x, sunView.y, sunView.z);
        planet.safeGetUniform("Time").set(time());
        planet.safeGetUniform("Seed").set(params.seed());
        planet.safeGetUniform("Scale").set(params.scale());
        planet.safeGetUniform("OceanLevel").set(params.oceanLevel());
        planet.safeGetUniform("PolarIce").set(params.polarIce());
        planet.safeGetUniform("CloudCover").set(params.cloudCover());
        planet.safeGetUniform("CloudOffset").set((float) Math.toRadians(cloudSpinDeg));
        planet.safeGetUniform("Bump").set(params.bump());
        planet.safeGetUniform("Kind").set(params.gasGiant() ? 1.0f : 0.0f);
        planet.safeGetUniform("Octaves").set((float) quality.octaves);
        setColor(planet, "PalLow", params.low());
        setColor(planet, "PalMid", params.mid());
        setColor(planet, "PalHigh", params.high());
        setColor(planet, "OceanDeep", params.oceanDeep());
        setColor(planet, "OceanShallow", params.oceanShallow());
        setColor(planet, "EmissiveColor", params.emissive());
        planet.safeGetUniform("EmissiveAmount").set(params.emissiveAmount());
        mesh.draw(planet, modelView, projection);

        if (atmosphere && params.atmoDensity() > 0.0f) {
            ShaderInstance shell = ContinuumShaders.ATMOSPHERE;
            shell.safeGetUniform("SunDir").set(sunView.x, sunView.y, sunView.z);
            setColor(shell, "AtmoColor", params.atmoColor());
            shell.safeGetUniform("AtmoDensity").set(params.atmoDensity());

            RenderSystem.enableBlend();
            RenderSystem.depthMask(false);
            mesh.draw(shell, new Matrix4f(modelView).scale(ATMOSPHERE_SCALE), projection);
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
        }
    }

    /** A self-lit star (or, with {@code blackHole}, an almost black disc for the glow to sit around). */
    public static void drawStar(int rgb, boolean blackHole, Matrix4f view, Matrix4f projection, Vector3f worldPos,
                                float radius, PlanetRenderer.Quality quality) {
        if (!ContinuumShaders.ready()) return;

        Matrix4f modelView = new Matrix4f(view).translate(worldPos).scale(radius);

        ShaderInstance star = ContinuumShaders.PLANET;
        star.safeGetUniform("Time").set(time());
        star.safeGetUniform("Seed").set(1.0f);
        star.safeGetUniform("Kind").set(2.0f);
        star.safeGetUniform("Octaves").set((float) quality.octaves);
        star.safeGetUniform("SunDir").set(0.0f, 0.0f, 1.0f);
        if (blackHole) {
            star.safeGetUniform("PalMid").set(0.012f, 0.012f, 0.016f);
            star.safeGetUniform("PalLow").set(0.0f, 0.0f, 0.0f);
        } else {
            float r = ((rgb >> 16) & 0xFF) / 255.0f;
            float g = ((rgb >> 8) & 0xFF) / 255.0f;
            float b = (rgb & 0xFF) / 255.0f;
            star.safeGetUniform("PalMid").set(r * 1.25f, g * 1.25f, b * 1.25f);
            star.safeGetUniform("PalLow").set(r * 0.35f, g * 0.3f, b * 0.3f);
        }
        PlanetRenderer.mesh(quality).draw(star, modelView, projection);
    }

    private static void setColor(ShaderInstance shader, String uniform, int rgb) {
        shader.safeGetUniform(uniform).set(((rgb >> 16) & 0xFF) / 255.0f, ((rgb >> 8) & 0xFF) / 255.0f,
                (rgb & 0xFF) / 255.0f);
    }

    /** The existing nebula shader as a backdrop; it drifts a little with the camera yaw for parallax. */
    private static void drawBackdrop(float width, float height, float yawDeg) {
        ShaderInstance nebula = WorldFXShaders.NEBULA;
        if (nebula == null) return;

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.setShader(() -> nebula);

        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(90.0), width / height, 0.05f, 10.0f);
        Matrix4f invProjection = new Matrix4f(projection).invert();
        Matrix4f invView = new Matrix4f().rotateY((float) Math.toRadians(-yawDeg * 0.15f));
        float t = (float) (System.currentTimeMillis() % 10000000L) / 10000.0f;

        nebula.safeGetUniform("OutSize").set(width, height);
        nebula.safeGetUniform("InvViewMat").set(invView);
        nebula.safeGetUniform("InvProjMat").set(invProjection);
        nebula.safeGetUniform("Time").set(t);
        nebula.safeGetUniform("PrimaryColor").set(0.35f, 0.18f, 0.7f);
        nebula.safeGetUniform("SecondaryColor").set(0.1f, 0.3f, 0.8f);
        nebula.safeGetUniform("Density").set(0.4f);
        nebula.safeGetUniform("Scale").set(1.1f);
        nebula.safeGetUniform("Seed").set(2.0f);

        nebula.apply();
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        bb.vertex(-1, -1, 0).endVertex();
        bb.vertex(1, -1, 0).endVertex();
        bb.vertex(1, 1, 0).endVertex();
        bb.vertex(-1, 1, 0).endVertex();
        Tesselator.getInstance().end();
        nebula.clear();
    }
}
