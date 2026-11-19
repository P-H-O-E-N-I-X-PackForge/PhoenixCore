package net.phoenix.core.client.renderer.machine;

import net.minecraft.client.renderer.ShaderInstance;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

public final class FxVolume {

    private FxVolume() {}

    public record Look(float t, float power, float seed, float[] a, float[] b) {}

    private static float adaptive = 1f;
    private static int lastFps = -1;

    public static void adapt(int fps) {
        if (fps == lastFps || fps <= 0) return;
        lastFps = fps;
        if (fps < 40) adaptive = Math.max(0.3f, adaptive * 0.85f);
        else if (fps > 58) adaptive = Math.min(1f, adaptive * 1.08f);
    }

    public static Runnable stageMatrices(Matrix4f view, Matrix4f projection) {
        org.joml.Matrix4fStack stack = RenderSystem.getModelViewStack();
        stack.pushMatrix();
        stack.set(new Matrix4f(view));
        RenderSystem.applyModelViewMatrix();
        Matrix4f oldProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        com.mojang.blaze3d.vertex.VertexSorting oldSorting = RenderSystem.getVertexSorting();
        RenderSystem.setProjectionMatrix(new Matrix4f(projection),
                com.mojang.blaze3d.vertex.VertexSorting.DISTANCE_TO_ORIGIN);
        return () -> {
            stack.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(oldProjection, oldSorting);
        };
    }

    public static float quality() {
        return adaptive;
    }

    public static void draw(ShaderInstance shader, Matrix4f m, float detail, float cx, float cy, float cz, float hx,
                            float hy, float hz, float scale, Look look, float[] p0, float[] p1) {
        Vector3f centre = m.transformPosition(cx, cy, cz, new Vector3f());
        RenderSystem.setShader(() -> shader);
        shader.safeGetUniform("Time").set(look.t());
        shader.safeGetUniform("Power").set(look.power());
        shader.safeGetUniform("Seed").set(look.seed());
        shader.safeGetUniform("Scale").set(scale);
        shader.safeGetUniform("Detail").set(Math.max(0.3f, Math.min(1f, detail * adaptive)));
        shader.safeGetUniform("Centre").set(centre.x, centre.y, centre.z);
        shader.safeGetUniform("ColorA").set(look.a()[0], look.a()[1], look.a()[2]);
        shader.safeGetUniform("ColorB").set(look.b()[0], look.b()[1], look.b()[2]);
        shader.safeGetUniform("P0").set(p0[0], p0[1], p0[2], p0[3]);
        shader.safeGetUniform("P1").set(p1[0], p1[1], p1[2], p1[3]);

        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

        float x0 = cx - hx, x1 = cx + hx, y0 = cy - hy, y1 = cy + hy, z0 = cz - hz, z1 = cz + hz;
        face(bb, m, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0);
        face(bb, m, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        face(bb, m, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        face(bb, m, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0);
        face(bb, m, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        face(bb, m, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
        drawAdditive(shader, bb.buildOrThrow());
    }

    private static void face(BufferBuilder bb, Matrix4f m, float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        bb.addVertex(m, ax, ay, az);
        bb.addVertex(m, bx, by, bz);
        bb.addVertex(m, cx, cy, cz);
        bb.addVertex(m, dx, dy, dz);
    }

    public static void drawAdditive(ShaderInstance shader, MeshData mesh) {
        volumeBlended(shader, mesh, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
    }

    public static void volumeBlended(ShaderInstance shader, MeshData mesh, GlStateManager.SourceFactor src,
                                     GlStateManager.DestFactor dst) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        shader.setSampler("DepthSampler", net.phoenix.core.client.fx.SceneDepth.textureId());
        shader.setDefaultUniforms(mesh.drawState().mode(), RenderSystem.getModelViewMatrix(),
                RenderSystem.getProjectionMatrix(), net.minecraft.client.Minecraft.getInstance().getWindow());
        shader.apply();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(src, dst);
        BufferUploader.draw(mesh);
        shader.clear();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
    }

    public static void blended(ShaderInstance shader, MeshData mesh, GlStateManager.SourceFactor src,
                               GlStateManager.DestFactor dst) {
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        shader.setDefaultUniforms(mesh.drawState().mode(), RenderSystem.getModelViewMatrix(),
                RenderSystem.getProjectionMatrix(), net.minecraft.client.Minecraft.getInstance().getWindow());
        shader.apply();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(src, dst);
        BufferUploader.draw(mesh);
        shader.clear();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
    }
}
