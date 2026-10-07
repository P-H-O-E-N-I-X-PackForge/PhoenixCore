package net.phoenix.core.integration.continuum.client.render;

import net.minecraft.client.renderer.ShaderInstance;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * A unit icosphere uploaded once to a {@link VertexBuffer}. Every planet, moon and atmosphere shell is this same
 * mesh, scaled and parameterised by shader uniforms. Vertices carry only a position, which doubles as the surface
 * direction the planet shader samples its procedural terrain with.
 */
public final class IcosphereMesh implements AutoCloseable {

    private static final float T = (1.0f + (float) Math.sqrt(5.0)) / 2.0f;

    private static final float[][] BASE_VERTICES = {
            { -1, T, 0 }, { 1, T, 0 }, { -1, -T, 0 }, { 1, -T, 0 },
            { 0, -1, T }, { 0, 1, T }, { 0, -1, -T }, { 0, 1, -T },
            { T, 0, -1 }, { T, 0, 1 }, { -T, 0, -1 }, { -T, 0, 1 } };

    private static final int[][] BASE_FACES = {
            { 0, 11, 5 }, { 0, 5, 1 }, { 0, 1, 7 }, { 0, 7, 10 }, { 0, 10, 11 },
            { 1, 5, 9 }, { 5, 11, 4 }, { 11, 10, 2 }, { 10, 7, 6 }, { 7, 1, 8 },
            { 3, 9, 4 }, { 3, 4, 2 }, { 3, 2, 6 }, { 3, 6, 8 }, { 3, 8, 9 },
            { 4, 9, 5 }, { 2, 4, 11 }, { 6, 2, 10 }, { 8, 6, 7 }, { 9, 8, 1 } };

    private final VertexBuffer buffer;
    private final int triangleCount;

    private IcosphereMesh(VertexBuffer buffer, int triangleCount) {
        this.buffer = buffer;
        this.triangleCount = triangleCount;
    }

    public int triangleCount() {
        return triangleCount;
    }

    /** Must be called on the render thread. {@code subdivisions} 0 is the 20-face icosahedron; each step x4. */
    public static IcosphereMesh create(int subdivisions) {
        List<Vector3f[]> triangles = new ArrayList<>();
        for (int[] face : BASE_FACES) {
            Vector3f a = unit(BASE_VERTICES[face[0]]);
            Vector3f b = unit(BASE_VERTICES[face[1]]);
            Vector3f c = unit(BASE_VERTICES[face[2]]);

            // make sure every face winds counter-clockwise seen from outside, whatever the table says
            Vector3f normal = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a));
            Vector3f centroid = new Vector3f(a).add(b).add(c);
            if (normal.dot(centroid) < 0) {
                Vector3f swap = b;
                b = c;
                c = swap;
            }
            triangles.add(new Vector3f[] { a, b, c });
        }

        for (int level = 0; level < subdivisions; level++) {
            List<Vector3f[]> next = new ArrayList<>(triangles.size() * 4);
            for (Vector3f[] tri : triangles) {
                Vector3f ab = midpoint(tri[0], tri[1]);
                Vector3f bc = midpoint(tri[1], tri[2]);
                Vector3f ca = midpoint(tri[2], tri[0]);
                next.add(new Vector3f[] { tri[0], ab, ca });
                next.add(new Vector3f[] { tri[1], bc, ab });
                next.add(new Vector3f[] { tri[2], ca, bc });
                next.add(new Vector3f[] { ab, bc, ca });
            }
            triangles = next;
        }

        BufferBuilder builder = new BufferBuilder(triangles.size() * 3 * 12 + 64);
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION);
        for (Vector3f[] tri : triangles) {
            for (Vector3f v : tri) {
                builder.vertex(v.x, v.y, v.z).endVertex();
            }
        }

        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        buffer.bind();
        buffer.upload(builder.end());
        VertexBuffer.unbind();
        return new IcosphereMesh(buffer, triangles.size());
    }

    public void draw(ShaderInstance shader, Matrix4f modelView, Matrix4f projection) {
        buffer.bind();
        buffer.drawWithShader(modelView, projection, shader);
        VertexBuffer.unbind();
    }

    @Override
    public void close() {
        buffer.close();
    }

    private static Vector3f unit(float[] v) {
        return new Vector3f(v[0], v[1], v[2]).normalize();
    }

    private static Vector3f midpoint(Vector3f a, Vector3f b) {
        return new Vector3f(a).add(b).normalize();
    }
}
