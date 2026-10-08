package net.phoenix.core.integration.continuum.client.render;

import net.minecraft.client.renderer.ShaderInstance;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * A unit cube (corners at +-1) for the blocky planet style. Only six flat faces: the planet shader turns the
 * interpolated position into square cells itself, so the mesh needs no subdivision.
 */
public final class CubeMesh {

    private CubeMesh() {}

    private static @Nullable VertexBuffer buffer;

    private static final float[][] FACES = {
            // each face: four corners counter-clockwise seen from outside
            { 1, -1, -1, 1, 1, -1, 1, 1, 1, 1, -1, 1 }, // +x
            { -1, -1, 1, -1, 1, 1, -1, 1, -1, -1, -1, -1 }, // -x
            { -1, 1, -1, -1, 1, 1, 1, 1, 1, 1, 1, -1 }, // +y
            { -1, -1, 1, -1, -1, -1, 1, -1, -1, 1, -1, 1 }, // -y
            { -1, -1, 1, 1, -1, 1, 1, 1, 1, -1, 1, 1 }, // +z
            { 1, -1, -1, -1, -1, -1, -1, 1, -1, 1, 1, -1 } // -z
    };

    private static VertexBuffer buffer() {
        if (buffer == null) {
            BufferBuilder builder = new BufferBuilder(512);
            builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION);
            for (float[] f : FACES) {
                int[][] order = { { 0, 1, 2 }, { 0, 2, 3 } };
                for (int[] tri : order) {
                    for (int corner : tri) {
                        builder.vertex(f[corner * 3], f[corner * 3 + 1], f[corner * 3 + 2]).endVertex();
                    }
                }
            }
            buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            buffer.bind();
            buffer.upload(builder.end());
            VertexBuffer.unbind();
        }
        return buffer;
    }

    public static void draw(ShaderInstance shader, Matrix4f modelView, Matrix4f projection) {
        VertexBuffer b = buffer();
        b.bind();
        b.drawWithShader(modelView, projection, shader);
        VertexBuffer.unbind();
    }
}
