package net.phoenix.core.integration.continuum.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
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

        boolean cube = net.phoenix.core.integration.continuum.client.ContinuumVisuals.cube();
        Matrix4f modelView = new Matrix4f(view)
                .translate(worldPos)
                .rotateZ((float) Math.toRadians(params.axialTiltDeg()))
                .rotateY((float) Math.toRadians(spinDeg))
                // a cube of half-extent 0.82 holds about the volume of the unit sphere
                .scale(cube ? radius * 0.82f : radius);

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
        planet.safeGetUniform("Voxel").set(cube ?
                net.phoenix.core.integration.continuum.client.ContinuumVisuals.cubeCells(quality) : 0.0f);
        if (cube) CubeMesh.draw(planet, modelView, projection);
        else mesh.draw(planet, modelView, projection);

        // the smooth atmosphere shell would wrap a cube badly, so cube worlds go without
        if (!cube && atmosphere && params.atmoDensity() > 0.0f) {
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

        // stars follow the planet style (a blocky sun in cube mode); a black hole's horizon stays round
        boolean cube = !blackHole && net.phoenix.core.integration.continuum.client.ContinuumVisuals.cube();
        Matrix4f modelView = new Matrix4f(view).translate(worldPos).scale(cube ? radius * 0.82f : radius);

        ShaderInstance star = ContinuumShaders.PLANET;
        star.safeGetUniform("Voxel").set(cube ?
                net.phoenix.core.integration.continuum.client.ContinuumVisuals.cubeCells(quality) : 0.0f);
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
        if (cube) CubeMesh.draw(star, modelView, projection);
        else PlanetRenderer.mesh(quality).draw(star, modelView, projection);
    }

    private static void setColor(ShaderInstance shader, String uniform, int rgb) {
        shader.safeGetUniform(uniform).set(((rgb >> 16) & 0xFF) / 255.0f, ((rgb >> 8) & 0xFF) / 255.0f,
                (rgb & 0xFF) / 255.0f);
    }

    private static @Nullable VertexBuffer holeQuad;

    private static VertexBuffer holeQuad() {
        if (holeQuad == null) {
            BufferBuilder builder = new BufferBuilder(128);
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            builder.vertex(-1, -1, 0).endVertex();
            builder.vertex(1, -1, 0).endVertex();
            builder.vertex(1, 1, 0).endVertex();
            builder.vertex(-1, 1, 0).endVertex();
            holeQuad = new VertexBuffer(VertexBuffer.Usage.STATIC);
            holeQuad.bind();
            holeQuad.upload(builder.end());
            VertexBuffer.unbind();
        }
        return holeQuad;
    }

    /**
     * The accretion disk, photon ring, lensed far side of the disk and (for a quasar) jets around a black hole whose
     * horizon sphere of {@code radius} was just drawn with {@link #drawStar}. A camera-facing billboard; the shader
     * does the rest.
     */
    public static void drawBlackHole(Matrix4f view, Matrix4f projection, Vector3f worldPos, float radius,
                                     boolean quasar) {
        ShaderInstance shader = ContinuumShaders.BLACKHOLE;
        if (shader == null) return;

        float extent = quasar ? 18.0f : 6.0f;
        // the disk lies near the world's horizontal plane, tipped slightly so it never reads as flat
        Vector3f normal = view.transformDirection(new Vector3f(0.10f, 1.0f, 0.06f).normalize());

        shader.safeGetUniform("Time").set(time());
        shader.safeGetUniform("Extent").set(extent);
        shader.safeGetUniform("DiskNormal").set(normal.x, normal.y, normal.z);
        shader.safeGetUniform("Jets").set(quasar ? 1.0f : 0.0f);
        shader.safeGetUniform("DiskOuter").set(quasar ? 7.5f : 5.0f);
        if (quasar) {
            shader.safeGetUniform("HotColor").set(0.85f, 0.95f, 1.0f);
            shader.safeGetUniform("CoolColor").set(0.25f, 0.5f, 1.0f);
        } else {
            shader.safeGetUniform("HotColor").set(1.0f, 0.86f, 0.6f);
            shader.safeGetUniform("CoolColor").set(0.95f, 0.32f, 0.08f);
        }

        Vector3f centre = view.transformPosition(new Vector3f(worldPos));
        Matrix4f modelView = new Matrix4f().translate(centre).scale(radius * extent);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        VertexBuffer quad = holeQuad();
        quad.bind();
        quad.drawWithShader(modelView, projection, shader);
        VertexBuffer.unbind();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    /**
     * Continuum's own backdrop shader: opaque and fully covering (the shared nebula shader fades to nothing between
     * its filaments, which left most of the screen as bare clear colour). It drifts a little with the camera yaw.
     */
    private static void drawBackdrop(float width, float height, float yawDeg) {
        ShaderInstance backdrop = ContinuumShaders.BACKDROP;
        if (backdrop == null) return;

        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShader(() -> backdrop);

        backdrop.safeGetUniform("Aspect").set(width / height);
        backdrop.safeGetUniform("Yaw").set((float) Math.toRadians(yawDeg * 0.15f));
        backdrop.safeGetUniform("Time").set(time());
        backdrop.safeGetUniform("Octaves").set((float) net.phoenix.core.integration.continuum.client.ContinuumVisuals
                .backdropOctaves());
        backdrop.safeGetUniform("PrimaryColor").set(0.42f, 0.2f, 0.85f);
        backdrop.safeGetUniform("SecondaryColor").set(0.1f, 0.35f, 0.9f);

        backdrop.apply();
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        bb.vertex(-1, -1, 0).endVertex();
        bb.vertex(1, -1, 0).endVertex();
        bb.vertex(1, 1, 0).endVertex();
        bb.vertex(-1, 1, 0).endVertex();
        Tesselator.getInstance().end();
        backdrop.clear();
    }
}
