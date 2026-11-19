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

/**
 * Draws one of the machine shaders (fx_flame, fx_cortex, fx_collider, fx_crucible) as a volume: a box round the machine
 * that the shader marches through from the camera, so it is lit and seen in three dimensions, not painted on a card.
 * They all take the same inputs, so the renders only differ in what they put in the two four-number slots.
 */
public final class FxVolume {

    private FxVolume() {}

    public record Look(float t, float power, float seed, float[] a, float[] b) {}

    /**
     * How much of the full look the machines are drawn at, 0.3 to 1, moved by {@link #adapt} to keep the frame rate up
     * on slow graphics cards.
     */
    private static float adaptive = 1f;
    private static int lastFps = -1;

    /**
     * Called once a frame. When a new frame rate reading comes in, drops the quality if it is low and lets it back up
     * if not.
     */
    public static void adapt(int fps) {
        if (fps == lastFps || fps <= 0) return;
        lastFps = fps;
        if (fps < 40) adaptive = Math.max(0.3f, adaptive * 0.85f);
        else if (fps > 58) adaptive = Math.min(1f, adaptive * 1.08f);
    }

    /**
     * Sets the matrices a draw from the level stage needs: the view as the model-view, and the projection of the frame.
     * Returns what undoes it.
     */
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

    /**
     * @param m     the pose of the machine, which must put block-local coordinates relative to the camera
     * @param half  half the size of the box. It must match the shader's own box
     * @param scale the size setting, which the pose has already applied to the box
     */
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
        // the six faces. The shader only keeps the far side of the box, so the winding does not matter
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

    /** Adds the light of the shader to what is already on the screen, behind blocks but not hiding anything. */
    public static void drawAdditive(ShaderInstance shader, MeshData mesh) {
        volumeBlended(shader, mesh, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
    }

    /**
     * Draws a volume. The shader reads the depth of everything already drawn and stops its rays at the first thing in
     * the way, so the depth test is off: testing the box's own face would hide the whole volume behind any block inside
     * the box, and show it through any block that is not.
     */
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

    /**
     * Draws with a blend set here rather than in the shader's json. The json blend is cached by vanilla and skipped if
     * unchanged, so a draw that turns blending off afterwards leaves the next frame drawing opaque, as a black square.
     */
    public static void blended(ShaderInstance shader, MeshData mesh, GlStateManager.SourceFactor src,
                               GlStateManager.DestFactor dst) {
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        // apply() alone leaves the matrices at what the json says; drawWithShader is what fills them in
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
