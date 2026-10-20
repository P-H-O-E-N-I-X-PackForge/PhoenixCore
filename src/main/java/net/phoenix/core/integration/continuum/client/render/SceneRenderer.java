package net.phoenix.core.integration.continuum.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class SceneRenderer {

    private SceneRenderer() {}

    private static final float ATMOSPHERE_SCALE = 1.06f;

    private static @Nullable RenderTarget previous;

    private static @Nullable RenderTarget current;
    private static @Nullable com.mojang.blaze3d.pipeline.TextureTarget holeTarget;

    public static void begin(RenderTarget target, float backdropYawDeg) {
        previous = Minecraft.getInstance().getMainRenderTarget();
        current = target;

        target.setClearColor(0.004f, 0.005f, 0.014f, 1.0f);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);

        drawBackdrop(target.width, target.height, backdropYawDeg);
        drawBackdropGalaxies(target.width, target.height, backdropYawDeg);

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void blendPremultiplied() {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }

    private static void blendAtmosphere() {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE,
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ZERO,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
    }

    public static void end() {
        if (current != null) {
            RenderSystem.colorMask(false, false, false, true);
            RenderSystem.clearColor(0.0f, 0.0f, 0.0f, 1.0f);
            RenderSystem.clear(16384, Minecraft.ON_OSX);
            RenderSystem.colorMask(true, true, true, true);
        }
        if (previous != null) previous.bindWrite(true);
        previous = null;
        current = null;
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
    }

    public static Matrix4f projection(float width, float height, float fovDeg) {
        return new Matrix4f().perspective((float) Math.toRadians(fovDeg), width / height, 0.1f, 400.0f);
    }

    private static float time() {
        return (float) (System.currentTimeMillis() % 10000000L) / 1000.0f;
    }

    public static void drawPlanet(PlanetParams params, Matrix4f view, Matrix4f projection, Vector3f worldPos,
                                  float radius, Vector3f sunWorld, float spinDeg, float cloudSpinDeg,
                                  PlanetRenderer.Quality quality, boolean atmosphere) {
        if (!ContinuumShaders.ready()) return;

        boolean cube = net.phoenix.core.integration.continuum.client.ContinuumVisuals.cube();
        Matrix4f modelView = new Matrix4f(view)
                .translate(worldPos)
                .rotateZ((float) Math.toRadians(params.axialTiltDeg()))
                .rotateY((float) Math.toRadians(spinDeg))

                .scale(cube ? radius * 0.82f : radius);

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
        planet.safeGetUniform("Style").set((float) params.style());
        planet.safeGetUniform("Voxel").set(cube ?
                net.phoenix.core.integration.continuum.client.ContinuumVisuals.cubeCells(quality) : 0.0f);
        if (cube) CubeMesh.draw(planet, modelView, projection);
        else mesh.draw(planet, modelView, projection);

        if (!cube && atmosphere && params.atmoDensity() > 0.0f) {
            ShaderInstance shell = ContinuumShaders.ATMOSPHERE;
            shell.safeGetUniform("SunDir").set(sunView.x, sunView.y, sunView.z);
            setColor(shell, "AtmoColor", params.atmoColor());
            shell.safeGetUniform("AtmoDensity").set(params.atmoDensity());

            blendAtmosphere();
            RenderSystem.depthMask(false);
            mesh.draw(shell, new Matrix4f(modelView).scale(ATMOSPHERE_SCALE), projection);
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        }

        if (!cube && params.ring() != null && ContinuumShaders.RING != null) {
            PlanetParams.Ring ring = params.ring();
            ShaderInstance rs = ContinuumShaders.RING;
            rs.safeGetUniform("Time").set(time());
            rs.safeGetUniform("SunDir").set(sunView.x, sunView.y, sunView.z);
            rs.safeGetUniform("PlanetCenter").set(bodyView.x, bodyView.y, bodyView.z);
            rs.safeGetUniform("PlanetRadius").set(radius);
            rs.safeGetUniform("Inner").set(ring.inner() / ring.outer());
            rs.safeGetUniform("Opacity").set(ring.opacity());
            rs.safeGetUniform("Seed").set(params.seed());
            setColor(rs, "Color1", ring.color());
            setColor(rs, "Color2", ring.color2());

            Matrix4f ringModel = new Matrix4f(view)
                    .translate(worldPos)
                    .rotateZ((float) Math.toRadians(params.axialTiltDeg()))
                    .rotateX((float) Math.toRadians(ring.tiltDeg()))
                    .scale(radius * ring.outer());
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            VertexBuffer quad = ringQuad();
            assert quad != null;
            quad.bind();
            quad.drawWithShader(ringModel, projection, rs);
            unbindBuffers();
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
        }
    }

    public static final int STYLE_RED_GIANT = 1;
    public static final int STYLE_BROWN_DWARF = 2;
    public static final int STYLE_RAPID_ROTATOR = 3;

    public static void drawStar(int rgb, boolean blackHole, Matrix4f view, Matrix4f projection, Vector3f worldPos,
                                float radius, PlanetRenderer.Quality quality) {
        drawStar(rgb, blackHole, view, projection, worldPos, radius, quality, 0);
    }

    public static void drawStar(int rgb, boolean blackHole, Matrix4f view, Matrix4f projection, Vector3f worldPos,
                                float radius, PlanetRenderer.Quality quality, int style) {
        if (!ContinuumShaders.ready()) return;

        boolean cube = net.phoenix.core.integration.continuum.client.ContinuumVisuals.cube();

        float bodyScale = !cube ? radius : blackHole ? radius * 0.5f : radius * 0.82f;

        float flatten = (!cube && !blackHole && style == STYLE_RAPID_ROTATOR) ? 0.84f : 1.0f;
        Matrix4f modelView = new Matrix4f(view).translate(worldPos).scale(bodyScale, bodyScale * flatten, bodyScale);

        ShaderInstance star = ContinuumShaders.PLANET;
        star.safeGetUniform("Voxel").set(cube ?
                net.phoenix.core.integration.continuum.client.ContinuumVisuals.cubeCells(quality) / 1.64f : 0.0f);
        star.safeGetUniform("Time").set(time());
        star.safeGetUniform("Seed").set(1.0f);
        star.safeGetUniform("Kind").set(blackHole ? 3.0f : 2.0f);
        star.safeGetUniform("Style").set(blackHole ? 0.0f : (float) style);
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

    private static float[] rgbFloats(int rgb) {
        return new float[] { ((rgb >> 16) & 0xFF) / 255.0f, ((rgb >> 8) & 0xFF) / 255.0f, (rgb & 0xFF) / 255.0f };
    }

    public static void drawHole(net.phoenix.core.integration.continuum.data.ContinuumSystem system, Matrix4f view,
                                Matrix4f projection, Vector3f worldPos, float radius, PlanetRenderer.Quality quality,
                                boolean sky) {
        var style = system != null ? system.hole() :
                net.phoenix.core.integration.continuum.data.ContinuumSystem.HoleStyle.DEFAULT;
        if (system != null && system.isBinaryHole()) {
            drawBinaryHole(style, view, projection, worldPos, radius, quality, sky);
            return;
        }
        drawStar(0, true, view, projection, worldPos, radius, quality);
        drawBlackHole(view, projection, worldPos, radius, system != null && system.isQuasar(), sky, style);
    }

    private static void drawBinaryHole(net.phoenix.core.integration.continuum.data.ContinuumSystem.HoleStyle style,
                                       Matrix4f view, Matrix4f projection, Vector3f worldPos, float radius,
                                       PlanetRenderer.Quality quality, boolean sky) {
        float q = style.massRatio();
        float sep = style.separation() * radius;
        double phase = 2.0 * Math.PI * (time() / Math.max(style.periodSeconds(), 1.0f));
        Vector3f u = new Vector3f((float) Math.cos(phase), 0.0f, (float) Math.sin(phase));
        Vector3f p1 = new Vector3f(worldPos).fma(-q / (1.0f + q) * sep, u);
        Vector3f p2 = new Vector3f(worldPos).fma(1.0f / (1.0f + q) * sep, u);
        float r1 = radius;
        float r2 = radius * (float) Math.sqrt(q);

        int tint = style.hotColor() >= 0 ? style.hotColor() : 0xb45cff;
        drawDiskFx(tint, 6.0f, 1.45f, 3.4f, new float[] { 2.1f, 2.7f, 0.0f, 0.0f }, view, projection, worldPos, sep,
                quality, sky);

        float ownSize1 = Math.max(0.25f, Math.min(1.0f, 0.42f * style.separation() / 5.6f));
        float ownSize2 = Math.max(0.25f, Math.min(1.0f, 0.42f * style.separation() / (float) Math.sqrt(q) / 5.6f));
        var s1 = style.withDisk(ownSize1, Math.max(1.0f, style.diskIntensity()));
        var s2 = style.withDisk(ownSize2, Math.max(1.0f, style.diskIntensity()));

        boolean firstIsFarther = view.transformPosition(new Vector3f(p1)).z <
                view.transformPosition(new Vector3f(p2)).z;
        for (int pass = 0; pass < 2; pass++) {
            boolean first = (pass == 0) == firstIsFarther;
            Vector3f p = first ? p1 : p2;
            float r = first ? r1 : r2;
            drawStar(0, true, view, projection, p, r, quality);
            drawBlackHole(view, projection, p, r, false, sky, first ? s1 : s2);
        }
    }

    private static void drawDiskFx(int rgb, float extent, float inner, float outer, float[] gaps, Matrix4f view,
                                   Matrix4f projection, Vector3f worldPos, float unit, PlanetRenderer.Quality quality,
                                   boolean sky) {
        Vector3f normal = view.transformDirection(new Vector3f(0.0f, 1.0f, 0.0f));
        if (sky) {
            Vector3f nW = skyDiskWorldNormal(worldPos);
            if (nW != null) {
                org.joml.Matrix3f axes = skyAxes(worldPos);
                normal = new Vector3f(nW.dot(axes.getColumn(0, new Vector3f())),
                        nW.dot(axes.getColumn(1, new Vector3f())),
                        nW.dot(axes.getColumn(2, new Vector3f())));
            }
        }
        final Vector3f diskNormal = normal;
        final float[] g = gaps != null && gaps.length >= 4 ? gaps : new float[] { 3.4f, 5.1f, 7.3f, 0.0f };
        drawStarFx(ContinuumShaders.PROTOSTAR, rgb, extent, view, projection, worldPos, unit, quality, sky, s -> {
            s.safeGetUniform("DiskNormal").set(diskNormal.x, diskNormal.y, diskNormal.z);
            s.safeGetUniform("Gaps").set(g[0], g[1], g[2], g[3]);
            s.safeGetUniform("DiskInner").set(inner);
            s.safeGetUniform("DiskOuter").set(outer);
        });
    }

    public static void drawSystemStar(net.phoenix.core.integration.continuum.data.ContinuumSystem system, int color,
                                      Matrix4f view, Matrix4f projection, Vector3f worldPos, float radius,
                                      PlanetRenderer.Quality quality, boolean sky) {
        if (system != null && system.isWolfRayet()) {
            drawWolfRayet(color, view, projection, worldPos, radius, quality, sky);
        } else if (system != null && system.isPulsar()) {
            drawPulsar(color, view, projection, worldPos, radius, quality, sky);
        } else if (system != null && system.isPlanetaryNebula()) {
            drawPlanetaryNebula(color, view, projection, worldPos, radius, quality, sky);
        } else if (system != null && system.isProtostar()) {
            drawProtostar(color, view, projection, worldPos, radius, quality, sky,
                    sky ? null : orbitGaps(system, radius));
        } else {
            drawStar(color, false, view, projection, worldPos, radius, quality,
                    system != null ? system.starStyle() : 0);
        }
    }

    private static void drawHotBody(int rgb, float whiten, Matrix4f view, Matrix4f projection, Vector3f worldPos,
                                    float radius, PlanetRenderer.Quality quality) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int hot = ((r + (int) ((255 - r) * whiten)) << 16) | ((g + (int) ((255 - g) * whiten)) << 8) |
                (b + (int) ((255 - b) * whiten));
        drawStar(hot, false, view, projection, worldPos, radius, quality);
    }

    private static void drawStarFx(ShaderInstance shader, int rgb, float extent, Matrix4f view, Matrix4f projection,
                                   Vector3f worldPos, float radius, PlanetRenderer.Quality quality, boolean sky) {
        drawStarFx(shader, rgb, extent, view, projection, worldPos, radius, quality, sky, null);
    }

    private static void drawStarFx(ShaderInstance shader, int rgb, float extent, Matrix4f view, Matrix4f projection,
                                   Vector3f worldPos, float radius, PlanetRenderer.Quality quality, boolean sky,
                                   java.util.function.Consumer<ShaderInstance> extra) {
        if (shader == null) return;

        boolean cube = net.phoenix.core.integration.continuum.client.ContinuumVisuals.cube();
        shader.safeGetUniform("Time").set(time());
        shader.safeGetUniform("Extent").set(extent);
        shader.safeGetUniform("Voxel").set(cube ?
                net.phoenix.core.integration.continuum.client.ContinuumVisuals.cubeCells(quality) / 1.64f : 0.0f);
        shader.safeGetUniform("Core").set(cube ? 0.82f : 1.0f);
        shader.safeGetUniform("Detail").set(switch (quality) {
            case LOW -> 2.0f;
            case MEDIUM -> 3.0f;
            case HIGH -> 4.0f;
        });
        shader.safeGetUniform("Tint").set(((rgb >> 16) & 0xFF) / 255.0f, ((rgb >> 8) & 0xFF) / 255.0f,
                (rgb & 0xFF) / 255.0f);

        if (extra != null) extra.accept(shader);

        Matrix4f modelView = sky ? skyBillboard(view, worldPos, radius * extent) :
                new Matrix4f().translate(view.transformPosition(new Vector3f(worldPos))).scale(radius * extent);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        if (sky) RenderSystem.disableDepthTest();
        else RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        VertexBuffer quad = holeQuad();
        assert quad != null;
        quad.bind();
        quad.drawWithShader(modelView, projection, shader);
        unbindBuffers();

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    public static void drawWolfRayet(int rgb, Matrix4f view, Matrix4f projection, Vector3f worldPos, float radius,
                                     PlanetRenderer.Quality quality, boolean sky) {
        drawHotBody(rgb, 0.4f, view, projection, worldPos, radius, quality);
        drawStarFx(ContinuumShaders.WOLFRAYET, rgb, 14.0f, view, projection, worldPos, radius, quality, sky);
    }

    public static void drawPulsar(int rgb, Matrix4f view, Matrix4f projection, Vector3f worldPos, float radius,
                                  PlanetRenderer.Quality quality, boolean sky) {
        drawHotBody(rgb, 0.55f, view, projection, worldPos, radius, quality);
        drawStarFx(ContinuumShaders.PULSAR, rgb, 12.0f, view, projection, worldPos, radius, quality, sky);
    }

    private static float orbitWorldRadius(float au) {
        return 2.6f + 3.0f * (float) Math.sqrt(au);
    }

    private static float[] orbitGaps(net.phoenix.core.integration.continuum.data.ContinuumSystem system,
                                     float starRadius) {
        java.util.List<Float> radii = new java.util.ArrayList<>();
        for (var b : net.phoenix.core.integration.continuum.data.ContinuumData.bodiesOf(system.id())) {
            if (b.isMoon() || b.isCentral() || b.transientInfo() != null) continue;
            radii.add(orbitWorldRadius(b.orbitAu()) / Math.max(starRadius, 1.0e-3f));
        }
        java.util.Collections.sort(radii);
        float[] out = new float[4];
        int n = 0;
        for (float r : radii) {
            if (n < 4) out[n++] = r;
        }
        return out;
    }

    public static void drawProtostar(int rgb, Matrix4f view, Matrix4f projection, Vector3f worldPos, float radius,
                                     PlanetRenderer.Quality quality, boolean sky, float[] gaps) {
        drawHotBody(rgb, 0.25f, view, projection, worldPos, radius, quality);
        drawDiskFx(rgb, 18.0f, 0.0f, 0.0f, gaps, view, projection, worldPos, radius, quality, sky);
    }

    public static void drawPlanetaryNebula(int rgb, Matrix4f view, Matrix4f projection, Vector3f worldPos,
                                           float radius, PlanetRenderer.Quality quality, boolean sky) {
        drawHotBody(rgb, 0.3f, view, projection, worldPos, radius, quality);
        drawStarFx(ContinuumShaders.PLANETARY, rgb, 16.0f, view, projection, worldPos, radius, quality, sky);
    }

    public static void drawGalaxies(Matrix4f view, Matrix4f projection, long seed, PlanetRenderer.Quality quality) {
        ShaderInstance shader = ContinuumShaders.GALAXY;
        if (shader == null) return;

        int count = switch (quality) {
            case LOW -> 2;
            case MEDIUM -> 3;
            case HIGH -> 4;
        };
        boolean cube = net.phoenix.core.integration.continuum.client.ContinuumVisuals.cube();
        float cells = cube ? net.phoenix.core.integration.continuum.client.ContinuumVisuals.cubeCells(quality) * 1.5f :
                0.0f;
        float detail = switch (quality) {
            case LOW -> 2.0f;
            case MEDIUM -> 3.0f;
            case HIGH -> 4.0f;
        };
        Vector3f centralDir = new Vector3f(52.0f, 30.0f, -76.0f).normalize();
        float[][] armPalette = { { 0.60f, 0.78f, 1.00f }, { 1.00f, 0.72f, 0.52f }, { 0.78f, 0.66f, 1.00f } };

        java.util.Random rng = new java.util.Random(seed * 0x9E3779B97F4A7C15L + 0x2545F4914F6CDD1DL);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        VertexBuffer quad = holeQuad();
        quad.bind();
        for (int i = 0; i < count; i++) {
            Vector3f dir = new Vector3f();
            for (int attempt = 0; attempt < 8; attempt++) {
                double az = rng.nextDouble() * Math.PI * 2.0;
                double s = -0.7 + rng.nextDouble() * 1.65;
                s = Math.max(-0.9, Math.min(0.97, s));
                double c = Math.sqrt(1.0 - s * s);
                dir.set((float) (c * Math.cos(az)), (float) s, (float) (c * Math.sin(az)));
                if (dir.dot(centralDir) < 0.93f) break;
            }
            float size = 6.0f + rng.nextFloat() * 4.0f;
            float[] arm = armPalette[rng.nextInt(armPalette.length)];

            shader.safeGetUniform("Seed").set(rng.nextFloat());
            shader.safeGetUniform("Cells").set(cells);
            shader.safeGetUniform("Detail").set(detail);
            shader.safeGetUniform("Tilt").set(0.22f + rng.nextFloat() * 0.73f);
            shader.safeGetUniform("Rot").set(rng.nextFloat() * 3.14159f);
            shader.safeGetUniform("Intensity").set(0.65f + rng.nextFloat() * 0.35f);
            shader.safeGetUniform("CoreColor").set(1.0f, 0.84f, 0.62f);
            shader.safeGetUniform("ArmColor").set(arm[0], arm[1], arm[2]);

            Matrix4f modelView = skyBillboard(view, new Vector3f(dir).mul(100.0f), size);
            quad.drawWithShader(modelView, projection, shader);
        }
        unbindBuffers();

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private static void setColor(ShaderInstance shader, String uniform, int rgb) {
        shader.safeGetUniform(uniform).set(((rgb >> 16) & 0xFF) / 255.0f, ((rgb >> 8) & 0xFF) / 255.0f,
                (rgb & 0xFF) / 255.0f);
    }

    public static boolean drawCentralBody(net.phoenix.core.integration.continuum.data.ContinuumBody body, Matrix4f view,
                                          Matrix4f projection, Vector3f worldPos, float radius,
                                          PlanetRenderer.Quality quality) {
        return drawCentralBody(body, view, projection, worldPos, radius, quality, false);
    }

    public static boolean drawCentralBody(net.phoenix.core.integration.continuum.data.ContinuumBody body, Matrix4f view,
                                          Matrix4f projection, Vector3f worldPos, float radius,
                                          PlanetRenderer.Quality quality, boolean sky) {
        var owner = net.phoenix.core.integration.continuum.data.ContinuumData.system(body.system());
        switch (body.type()) {
            case BLACK_HOLE -> {
                drawHole(owner, view, projection, worldPos, radius, quality, sky);
                return true;
            }
            case STAR -> {
                drawSystemStar(owner, owner != null ? owner.starColor() : 0xffd9a0, view, projection, worldPos, radius,
                        quality, sky);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private static @Nullable com.mojang.blaze3d.pipeline.TextureTarget nebulaTarget;

    public static void drawNebulas(java.util.List<Nebula> nebulas, Matrix4f view, Matrix4f projection, float scale) {
        ShaderInstance shader = ContinuumShaders.NEBULA;
        if (shader == null || nebulas.isEmpty()) return;

        RenderTarget destination = current != null ? current : Minecraft.getInstance().getMainRenderTarget();
        ensureNebulaTarget(destination);

        assert nebulaTarget != null;
        nebulaTarget.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        nebulaTarget.clear(Minecraft.ON_OSX);
        nebulaTarget.bindWrite(true);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        shader.safeGetUniform("Time").set(time());
        shader.safeGetUniform("Octaves").set((float) Math.min(5,
                net.phoenix.core.integration.continuum.client.ContinuumVisuals.quality().octaves));
        blendPremultiplied();
        VertexBuffer quad = holeQuad();
        quad.bind();
        for (Nebula nebula : nebulas) {
            setColor(shader, "Color1", nebula.color1());
            setColor(shader, "Color2", nebula.color2());
            shader.safeGetUniform("Density").set(nebula.density());

            java.util.Random random = new java.util.Random((long) (nebula.seed() * 7919.0f));
            for (int i = 0; i < nebula.puffs(); i++) {

                Vector3f offset = new Vector3f(random.nextFloat() - 0.5f, (random.nextFloat() - 0.5f) * 0.5f,
                        random.nextFloat() - 0.5f).mul(nebula.radius() * 1.3f);
                Vector3f centre = view.transformPosition(new Vector3f(nebula.center()).add(offset).mul(scale));
                if (centre.z > -0.5f) continue;

                float size = nebula.radius() * scale * (0.65f + random.nextFloat() * 0.6f);
                shader.safeGetUniform("Seed").set(nebula.seed() + i * 3.7f);
                quad.drawWithShader(new Matrix4f().translate(centre).rotateZ(random.nextFloat() * 6.2831853f)
                        .scale(size), projection, shader);
            }
        }
        unbindBuffers();
        destination.bindWrite(true);

        compositeHole(nebulaTarget);

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private static void ensureNebulaTarget(RenderTarget destination) {
        int w = Math.max(32, destination.width / 2);
        int h = Math.max(32, destination.height / 2);
        if (nebulaTarget == null || nebulaTarget.width != w || nebulaTarget.height != h) {
            if (nebulaTarget != null) nebulaTarget.destroyBuffers();
            nebulaTarget = new com.mojang.blaze3d.pipeline.TextureTarget(w, h, false, Minecraft.ON_OSX);
            nebulaTarget.setFilterMode(9729);
        }
    }

    public static void drawNebulaWash(Nebula nebula, float strength, Matrix4f projection, float yawDeg,
                                      float pitchDeg) {
        ShaderInstance shader = ContinuumShaders.NEBULA;
        if (shader == null) return;

        RenderTarget destination = current != null ? current : Minecraft.getInstance().getMainRenderTarget();
        ensureNebulaTarget(destination);
        nebulaTarget.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        nebulaTarget.clear(Minecraft.ON_OSX);
        nebulaTarget.bindWrite(true);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        float depth = 80.0f;
        float aspect = destination.width / (float) Math.max(1, destination.height);
        float cover = depth * 0.364f * aspect * 1.2f;

        shader.safeGetUniform("Time").set(time());
        shader.safeGetUniform("Octaves").set((float) Math.min(4,
                net.phoenix.core.integration.continuum.client.ContinuumVisuals.quality().octaves));
        shader.safeGetUniform("Density").set(nebula.density() * (0.35f + 0.45f * strength));
        setColor(shader, "Color1", nebula.color1());
        setColor(shader, "Color2", nebula.color2());
        blendPremultiplied();
        VertexBuffer quad = holeQuad();
        quad.bind();
        for (int i = 0; i < 3; i++) {
            float parallax = 0.35f + 0.2f * i;
            Vector3f centre = new Vector3f((i - 1) * cover * 0.55f - yawDeg * parallax,
                    (1 - i) * cover * 0.18f + pitchDeg * parallax * 0.6f, -depth - i * 6.0f);
            shader.safeGetUniform("Seed").set(nebula.seed() + i * 5.3f);
            quad.drawWithShader(new Matrix4f().translate(centre).rotateZ(i * 1.7f).scale(cover * (1.0f - 0.12f * i)),
                    projection, shader);
        }
        unbindBuffers();
        destination.bindWrite(true);

        compositeHole(nebulaTarget);

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private static @Nullable VertexBuffer ringQuad;

    private static @Nullable VertexBuffer holeQuad;

    private static @Nullable VertexBuffer ringQuad() {
        if (ringQuad == null) {
            BufferBuilder builder = new BufferBuilder(new com.mojang.blaze3d.vertex.ByteBufferBuilder(128),
                    VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            builder.addVertex(-1, 0, -1);
            builder.addVertex(-1, 0, 1);
            builder.addVertex(1, 0, 1);
            builder.addVertex(1, 0, -1);
            ringQuad = new VertexBuffer(VertexBuffer.Usage.STATIC);
            ringQuad.bind();
            ringQuad.upload(builder.buildOrThrow());
            unbindBuffers();
        }
        return ringQuad;
    }

    private static @Nullable VertexBuffer holeQuad() {
        if (holeQuad == null) {
            BufferBuilder builder = new BufferBuilder(new com.mojang.blaze3d.vertex.ByteBufferBuilder(128),
                    VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            builder.addVertex(-1, -1, 0);
            builder.addVertex(1, -1, 0);
            builder.addVertex(1, 1, 0);
            builder.addVertex(-1, 1, 0);
            holeQuad = new VertexBuffer(VertexBuffer.Usage.STATIC);
            holeQuad.bind();
            holeQuad.upload(builder.buildOrThrow());
            unbindBuffers();
        }
        return holeQuad;
    }

    private static org.joml.Matrix3f skyAxes(Vector3f worldPos) {
        Vector3f z = new Vector3f(worldPos).negate();
        if (z.lengthSquared() < 1.0e-6f) z.set(0.0f, 0.0f, 1.0f);
        z.normalize();
        Vector3f x = new Vector3f(0.0f, 1.0f, 0.0f).cross(z);
        if (x.lengthSquared() < 1.0e-6f) x.set(1.0f, 0.0f, 0.0f);
        x.normalize();
        Vector3f y = new Vector3f(z).cross(x);
        return new org.joml.Matrix3f(x, y, z);
    }

    private static Matrix4f skyBillboard(Matrix4f view, Vector3f worldPos, float scale) {
        return new Matrix4f(view).translate(worldPos).mul(new Matrix4f(skyAxes(worldPos))).scale(scale);
    }

    private static Vector3f skyDiskWorldNormal(Vector3f worldPos) {
        Vector3f losW = new Vector3f(worldPos).negate();
        if (losW.lengthSquared() <= 1.0e-6f) return null;
        losW.normalize();
        Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f).sub(new Vector3f(losW).mul(losW.y));
        if (up.lengthSquared() <= 1.0e-6f) return null;
        up.normalize();
        Vector3f right = new Vector3f(up).cross(losW).normalize();
        float lean = (float) Math.toRadians(SKY_LEAN_DEG);
        Vector3f tilt = up.mul((float) Math.cos(lean)).add(right.mul((float) Math.sin(lean)));
        float inc = (float) Math.toRadians(SKY_INCLINATION_DEG);
        return losW.mul((float) Math.cos(inc)).add(tilt.mul((float) Math.sin(inc))).normalize();
    }

    private static Vector3f skyDiskNormal(Matrix4f view, Vector3f worldPos, Vector3f fallback) {
        Vector3f normal = new Vector3f(fallback);
        Vector3f nW = skyDiskWorldNormal(worldPos);
        if (nW != null) normal = view.transformDirection(nW);
        Vector3f los = view.transformPosition(new Vector3f(worldPos)).negate();
        if (los.lengthSquared() > 1.0e-6f) {
            los.normalize();
            float nz = normal.dot(los);
            float sx = normal.x - los.x * nz;
            float sy = normal.y - los.y * nz;
            float planar = (float) Math.sqrt(sx * sx + sy * sy);
            float sin = (float) Math.sqrt(Math.max(0.0f, 1.0f - nz * nz));
            if (planar > 1.0e-5f) normal.set(sx / planar * sin, sy / planar * sin, nz);
        }
        return normal;
    }

    public static float SKY_INCLINATION_DEG = 45.0f;

    public static float SKY_LEAN_DEG = 15.0f;

    public static void drawBlackHole(Matrix4f view, Matrix4f projection, Vector3f worldPos, float radius,
                                     boolean quasar) {
        drawBlackHole(view, projection, worldPos, radius, quasar, false);
    }

    public static void drawBlackHole(Matrix4f view, Matrix4f projection, Vector3f worldPos, float radius,
                                     boolean quasar, boolean sky) {
        drawBlackHole(view, projection, worldPos, radius, quasar, sky,
                net.phoenix.core.integration.continuum.data.ContinuumSystem.HoleStyle.DEFAULT);
    }

    public static void drawBlackHole(Matrix4f view, Matrix4f projection, Vector3f worldPos, float radius,
                                     boolean quasar, boolean sky,
                                     net.phoenix.core.integration.continuum.data.ContinuumSystem.HoleStyle style) {
        ShaderInstance shader = ContinuumShaders.BLACKHOLE;
        if (shader == null) return;

        float diskOuter = (quasar ? 9.0f : 5.6f) * style.diskSize();
        float reach = diskOuter * (1.0f + 0.35f * style.feeding());
        float extent = Math.max(quasar ? 18.0f : 7.0f, reach / 0.78f);

        double tiltRad = Math.toRadians(90.0 * Math.max(0.0f, Math.min(1.0f, style.tilt())));
        Vector3f normal = view.transformDirection(
                new Vector3f(0.10f, (float) Math.cos(tiltRad), (float) Math.sin(tiltRad)).normalize());
        if (sky) normal = skyDiskNormal(view, worldPos, normal);

        shader.safeGetUniform("Time").set(time());
        shader.safeGetUniform("Extent").set(extent);
        shader.safeGetUniform("DiskNormal").set(normal.x, normal.y, normal.z);
        shader.safeGetUniform("Jets").set(quasar ? 1.0f : 0.0f);
        shader.safeGetUniform("DiskOuter").set(diskOuter);
        shader.safeGetUniform("DiskIntensity").set(style.diskIntensity());
        shader.safeGetUniform("Feeding").set(style.feeding());
        shader.safeGetUniform("Beaming").set(0.75f);
        float[] hot = quasar ? new float[] { 0.85f, 0.95f, 1.0f } : new float[] { 1.0f, 0.86f, 0.6f };
        float[] cool = quasar ? new float[] { 0.25f, 0.5f, 1.0f } : new float[] { 0.95f, 0.32f, 0.08f };
        if (style.hotColor() >= 0) hot = rgbFloats(style.hotColor());
        if (style.coolColor() >= 0) cool = rgbFloats(style.coolColor());
        shader.safeGetUniform("HotColor").set(hot[0], hot[1], hot[2]);
        shader.safeGetUniform("CoolColor").set(cool[0], cool[1], cool[2]);

        Vector3f centre = view.transformPosition(new Vector3f(worldPos));
        Matrix4f modelView = new Matrix4f().translate(centre).scale(radius * extent);

        var quality = net.phoenix.core.integration.continuum.client.ContinuumVisuals.quality();
        shader.safeGetUniform("Detail").set(switch (quality) {
            case LOW -> 2.0f;
            case MEDIUM -> 3.0f;
            case HIGH -> 4.0f;
        });
        shader.safeGetUniform("Voxel").set(net.phoenix.core.integration.continuum.client.ContinuumVisuals.cube() ?
                net.phoenix.core.integration.continuum.client.ContinuumVisuals.cubeCells(quality) / 1.64f : 0.0f);
        shader.safeGetUniform("Lensing")
                .set(quality == net.phoenix.core.integration.continuum.client.render.PlanetRenderer.Quality.LOW ? 0.0f :
                        1.0f);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        float scale = switch (quality) {
            case LOW -> 0.6f;
            case MEDIUM, HIGH -> 1.0f;
        };
        RenderTarget destination = current != null ? current : Minecraft.getInstance().getMainRenderTarget();
        com.mojang.blaze3d.pipeline.TextureTarget low = holeTarget(destination.width, destination.height, scale);

        low.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        low.clear(Minecraft.ON_OSX);
        low.bindWrite(true);
        blendPremultiplied();
        VertexBuffer quad = holeQuad();
        quad.bind();
        quad.drawWithShader(modelView, projection, shader);
        unbindBuffers();
        destination.bindWrite(true);

        compositeHole(low);

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private static TextureTarget holeTarget(int destWidth, int destHeight, float scale) {
        int w = Math.max(32, Math.round(destWidth * scale));
        int h = Math.max(32, Math.round(destHeight * scale));
        if (holeTarget == null || holeTarget.width != w || holeTarget.height != h) {
            if (holeTarget != null) holeTarget.destroyBuffers();
            holeTarget = new TextureTarget(w, h, false, Minecraft.ON_OSX);
            holeTarget.setFilterMode(9729);
        }
        return holeTarget;
    }

    private static void compositeHole(TextureTarget low) {
        Matrix4f savedProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var savedSorting = RenderSystem.getVertexSorting();
        Matrix4f ortho = new Matrix4f().setOrtho(0.0f, 1.0f, 1.0f, 0.0f, 1000.0f, 3000.0f);
        RenderSystem.setProjectionMatrix(ortho, com.mojang.blaze3d.vertex.VertexSorting.ORTHOGRAPHIC_Z);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        ShaderInstance blit = Minecraft.getInstance().gameRenderer.blitShader;
        blit.setSampler("DiffuseSampler", low.getColorTextureId());
        if (blit.MODEL_VIEW_MATRIX != null)
            blit.MODEL_VIEW_MATRIX.set(new Matrix4f().translation(0.0f, 0.0f, -2000.0f));
        if (blit.PROJECTION_MATRIX != null) blit.PROJECTION_MATRIX.set(ortho);
        blit.apply();

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX_COLOR);
        bb.addVertex(0, 1, 0).setUv(0, 0).setColor(255, 255, 255, 255);
        bb.addVertex(1, 1, 0).setUv(1, 0).setColor(255, 255, 255, 255);
        bb.addVertex(1, 0, 0).setUv(1, 1).setColor(255, 255, 255, 255);
        bb.addVertex(0, 0, 0).setUv(0, 1).setColor(255, 255, 255, 255);
        com.mojang.blaze3d.vertex.BufferUploader.draw(bb.buildOrThrow());
        blit.clear();

        RenderSystem.setProjectionMatrix(savedProjection, savedSorting);
    }

    private static void drawBackdropGalaxies(float width, float height, float yawDeg) {
        ShaderInstance g = ContinuumShaders.GALAXY;
        if (g == null) return;
        var quality = net.phoenix.core.integration.continuum.client.ContinuumVisuals.quality();
        int count = switch (quality) {
            case LOW -> 2;
            case MEDIUM -> 3;
            case HIGH -> 4;
        };
        boolean cube = net.phoenix.core.integration.continuum.client.ContinuumVisuals.cube();
        float cells = cube ? net.phoenix.core.integration.continuum.client.ContinuumVisuals.cubeCells(quality) * 1.5f :
                0.0f;
        float detail = switch (quality) {
            case LOW -> 2.0f;
            case MEDIUM -> 3.0f;
            case HIGH -> 4.0f;
        };
        float[][] spec = {
                { -0.62f, 0.38f, 0.17f, 0.45f, 0.5f, 0.31f, 1.0f, 0.60f, 0.78f, 1.00f },
                { 0.58f, -0.30f, 0.22f, 0.80f, -0.9f, 0.64f, 0.7f, 1.00f, 0.72f, 0.52f },
                { 0.18f, 0.55f, 0.12f, 0.28f, 2.2f, 0.17f, 1.3f, 0.78f, 0.66f, 1.00f },
                { -0.35f, -0.52f, 0.14f, 0.62f, 1.4f, 0.88f, 0.9f, 0.60f, 0.78f, 1.00f },
        };
        float aspect = width / height;
        double yawShift = Math.toRadians(yawDeg * 0.15f);
        Matrix4f identity = new Matrix4f();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);

        VertexBuffer quad = holeQuad();
        quad.bind();
        for (int i = 0; i < count; i++) {
            float[] s = spec[i];
            float x = (float) (s[0] - yawShift * s[6] * 1.15 / aspect);
            x = ((x + 1.4f) % 2.8f + 2.8f) % 2.8f - 1.4f;
            g.safeGetUniform("Seed").set(s[5]);
            g.safeGetUniform("Cells").set(cells);
            g.safeGetUniform("Detail").set(detail);
            g.safeGetUniform("Tilt").set(s[3]);
            g.safeGetUniform("Rot").set(s[4]);
            g.safeGetUniform("Intensity").set(0.85f);
            g.safeGetUniform("CoreColor").set(1.0f, 0.84f, 0.62f);
            g.safeGetUniform("ArmColor").set(s[7], s[8], s[9]);
            Matrix4f modelView = new Matrix4f().translate(x, s[1], 0.0f).scale(s[2] / aspect, s[2], 1.0f);
            quad.drawWithShader(modelView, identity, g);
        }
        unbindBuffers();

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private static boolean warnedBackdrop = false;

    private static void drawBackdrop(float width, float height, float yawDeg) {
        ShaderInstance backdrop = ContinuumShaders.BACKDROP;
        if (backdrop == null) {
            if (!warnedBackdrop) {
                warnedBackdrop = true;
                org.apache.logging.log4j.LogManager.getLogger().error(
                        "[PhoenixCore/Continuum] continuum_backdrop did not load; using the built-in star field. " +
                                "Look earlier in the log for 'Failed to register shader'.");
            }
            backdrop = ContinuumShaders.STARFIELD;
            if (backdrop == null) return;
        }

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();
        RenderSystem.disableCull();

        backdrop.safeGetUniform("Aspect").set(width / height);
        backdrop.safeGetUniform("Yaw").set((float) Math.toRadians(yawDeg * 0.15f));
        backdrop.safeGetUniform("Time").set(time());
        backdrop.safeGetUniform("Octaves").set((float) net.phoenix.core.integration.continuum.client.ContinuumVisuals
                .backdropOctaves());
        backdrop.safeGetUniform("PrimaryColor").set(0.42f, 0.2f, 0.85f);
        backdrop.safeGetUniform("SecondaryColor").set(0.1f, 0.35f, 0.9f);

        VertexBuffer quad = holeQuad();
        quad.bind();
        quad.drawWithShader(new Matrix4f(), new Matrix4f(), backdrop);
        unbindBuffers();

        RenderSystem.enableCull();
    }

    private static void unbindBuffers() {
        VertexBuffer.unbind();
        BufferUploader.invalidate();
    }
}
