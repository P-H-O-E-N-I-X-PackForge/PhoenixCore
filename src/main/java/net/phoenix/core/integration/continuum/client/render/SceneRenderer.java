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

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static void end() {
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

            RenderSystem.enableBlend();
            RenderSystem.depthMask(false);
            mesh.draw(shell, new Matrix4f(modelView).scale(ATMOSPHERE_SCALE), projection);
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
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
            quad.bind();
            quad.drawWithShader(ringModel, projection, rs);
            VertexBuffer.unbind();
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
        }
    }

    public static void drawStar(int rgb, boolean blackHole, Matrix4f view, Matrix4f projection, Vector3f worldPos,
                                float radius, PlanetRenderer.Quality quality) {
        if (!ContinuumShaders.ready()) return;

        boolean cube = !blackHole && net.phoenix.core.integration.continuum.client.ContinuumVisuals.cube();
        Matrix4f modelView = new Matrix4f(view).translate(worldPos).scale(cube ? radius * 0.82f : radius);

        ShaderInstance star = ContinuumShaders.PLANET;
        star.safeGetUniform("Voxel").set(cube ?
                net.phoenix.core.integration.continuum.client.ContinuumVisuals.cubeCells(quality) : 0.0f);
        star.safeGetUniform("Time").set(time());
        star.safeGetUniform("Seed").set(1.0f);
        star.safeGetUniform("Kind").set(blackHole ? 3.0f : 2.0f);
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

    public static boolean drawCentralBody(net.phoenix.core.integration.continuum.data.ContinuumBody body, Matrix4f view,
                                          Matrix4f projection, Vector3f worldPos, float radius,
                                          PlanetRenderer.Quality quality) {
        var owner = net.phoenix.core.integration.continuum.data.ContinuumData.system(body.system());
        switch (body.type()) {
            case BLACK_HOLE -> {
                drawStar(0, true, view, projection, worldPos, radius, quality);
                drawBlackHole(view, projection, worldPos, radius, owner != null && owner.isQuasar());
                return true;
            }
            case STAR -> {
                drawStar(owner != null ? owner.starColor() : 0xffd9a0, false, view, projection, worldPos, radius,
                        quality);
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

        nebulaTarget.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        nebulaTarget.clear(Minecraft.ON_OSX);
        nebulaTarget.bindWrite(true);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        shader.safeGetUniform("Time").set(time());
        shader.safeGetUniform("Octaves").set((float) Math.min(5,
                net.phoenix.core.integration.continuum.client.ContinuumVisuals.quality().octaves));
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
        VertexBuffer.unbind();
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
        VertexBuffer.unbind();
        destination.bindWrite(true);

        compositeHole(nebulaTarget);

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private static @Nullable VertexBuffer ringQuad;

    private static VertexBuffer ringQuad() {
        if (ringQuad == null) {
            BufferBuilder builder = new BufferBuilder(128);
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            builder.vertex(-1, 0, -1).endVertex();
            builder.vertex(-1, 0, 1).endVertex();
            builder.vertex(1, 0, 1).endVertex();
            builder.vertex(1, 0, -1).endVertex();
            ringQuad = new VertexBuffer(VertexBuffer.Usage.STATIC);
            ringQuad.bind();
            ringQuad.upload(builder.end());
            VertexBuffer.unbind();
        }
        return ringQuad;
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

    public static void drawBlackHole(Matrix4f view, Matrix4f projection, Vector3f worldPos, float radius,
                                     boolean quasar) {
        ShaderInstance shader = ContinuumShaders.BLACKHOLE;
        if (shader == null) return;

        float extent = quasar ? 18.0f : 6.0f;

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

        var quality = net.phoenix.core.integration.continuum.client.ContinuumVisuals.quality();
        shader.safeGetUniform("Detail").set(switch (quality) {
            case LOW -> 2.0f;
            case MEDIUM -> 4.0f;
            case HIGH -> 4.0f;
        });

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        float scale = switch (quality) {
            case LOW -> 0.6f;
            case MEDIUM -> 1.0f;
            case HIGH -> 1.0f;
        };
        RenderTarget destination = current != null ? current : Minecraft.getInstance().getMainRenderTarget();
        com.mojang.blaze3d.pipeline.TextureTarget low = holeTarget(destination.width, destination.height, scale);

        low.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        low.clear(Minecraft.ON_OSX);
        low.bindWrite(true);
        VertexBuffer quad = holeQuad();
        quad.bind();
        quad.drawWithShader(modelView, projection, shader);
        VertexBuffer.unbind();
        destination.bindWrite(true);

        compositeHole(low);

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private static com.mojang.blaze3d.pipeline.TextureTarget holeTarget(int destWidth, int destHeight, float scale) {
        int w = Math.max(32, Math.round(destWidth * scale));
        int h = Math.max(32, Math.round(destHeight * scale));
        if (holeTarget == null || holeTarget.width != w || holeTarget.height != h) {
            if (holeTarget != null) holeTarget.destroyBuffers();
            holeTarget = new com.mojang.blaze3d.pipeline.TextureTarget(w, h, false, Minecraft.ON_OSX);
            holeTarget.setFilterMode(9729);
        }
        return holeTarget;
    }

    private static void compositeHole(com.mojang.blaze3d.pipeline.TextureTarget low) {
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

        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bb.vertex(0, 1, 0).uv(0, 0).color(255, 255, 255, 255).endVertex();
        bb.vertex(1, 1, 0).uv(1, 0).color(255, 255, 255, 255).endVertex();
        bb.vertex(1, 0, 0).uv(1, 1).color(255, 255, 255, 255).endVertex();
        bb.vertex(0, 0, 0).uv(0, 1).color(255, 255, 255, 255).endVertex();
        com.mojang.blaze3d.vertex.BufferUploader.draw(bb.end());
        blit.clear();

        RenderSystem.setProjectionMatrix(savedProjection, savedSorting);
    }

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
