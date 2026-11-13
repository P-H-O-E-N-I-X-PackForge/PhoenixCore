package net.phoenix.core.client.renderer.machine;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class GlowDraw extends RenderType {

    private GlowDraw(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                     boolean affectsCrumbling, boolean sortOnUpload, Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
    }

    public static final RenderType GLOW = RenderType.create("phoenix_capstone_glow", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 4096, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
                    .setLayeringState(RenderStateShard.NO_LAYERING)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false));

    public static final RenderType DARK = RenderType.create("phoenix_capstone_dark", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 1024, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .createCompositeState(false));

    public record Cam(float rx, float ry, float rz, float ux, float uy, float uz, float px, float py, float pz) {

        public static Cam now(BlockPos machine) {
            var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
            Vector3f left = camera.getLeftVector();
            Vector3f up = camera.getUpVector();
            Vec3 pos = camera.getPosition();
            return new Cam(-left.x, -left.y, -left.z, up.x, up.y, up.z, (float) (pos.x - machine.getX()),
                    (float) (pos.y - machine.getY()), (float) (pos.z - machine.getZ()));
        }
    }

    public static Vec3 chamber(Direction front, BlockPos pos, double back, double up) {
        Direction behind = front.getOpposite();
        return new Vec3(0.5 + behind.getStepX() * back, up, 0.5 + behind.getStepZ() * back);
    }

    public static void vtx(VertexConsumer vc, Matrix4f m, float x, float y, float z, float r, float g, float b,
                           float a) {
        vc.addVertex(m, x, y, z).setColor(r, g, b, Mth.clamp(a, 0f, 1f));
    }

    public static void disc(VertexConsumer vc, Matrix4f m, Cam cam, float cx, float cy, float cz, float radius, float r,
                            float g, float b, float aCentre, float aEdge) {
        int segs = 14;
        for (int i = 0; i < segs; i++) {
            float a0 = Mth.TWO_PI * i / segs;
            float a1 = Mth.TWO_PI * (i + 1) / segs;
            float c0 = Mth.cos(a0) * radius;
            float s0 = Mth.sin(a0) * radius;
            float c1 = Mth.cos(a1) * radius;
            float s1 = Mth.sin(a1) * radius;
            vtx(vc, m, cx, cy, cz, r, g, b, aCentre);
            vtx(vc, m, cx + cam.rx() * c0 + cam.ux() * s0, cy + cam.ry() * c0 + cam.uy() * s0,
                    cz + cam.rz() * c0 + cam.uz() * s0, r, g, b, aEdge);
            vtx(vc, m, cx + cam.rx() * c1 + cam.ux() * s1, cy + cam.ry() * c1 + cam.uy() * s1,
                    cz + cam.rz() * c1 + cam.uz() * s1, r, g, b, aEdge);
            vtx(vc, m, cx, cy, cz, r, g, b, aCentre);
        }
    }

    public interface Shade {

        float[] at(float angle, float out);
    }

    public static void annulus(VertexConsumer vc, Matrix4f m, float cx, float cy, float cz, float[] u, float[] v,
                               float r0, float r1, float from, float sweep, int rings, int segs, Shade shade) {
        for (int ring = 0; ring < rings; ring++) {
            float f0 = ring / (float) rings;
            float f1 = (ring + 1) / (float) rings;
            float ra = Mth.lerp(f0, r0, r1);
            float rb = Mth.lerp(f1, r0, r1);
            for (int i = 0; i < segs; i++) {
                float a0 = from + sweep * i / segs;
                float a1 = from + sweep * (i + 1) / segs;
                float[] ca = shade.at(a0, f0);
                float[] cb = shade.at(a1, f0);
                float[] cc = shade.at(a1, f1);
                float[] cd = shade.at(a0, f1);
                vtx3(vc, m, cx, cy, cz, u, v, ra, a0, ca);
                vtx3(vc, m, cx, cy, cz, u, v, ra, a1, cb);
                vtx3(vc, m, cx, cy, cz, u, v, rb, a1, cc);
                vtx3(vc, m, cx, cy, cz, u, v, rb, a0, cd);
            }
        }
    }

    private static void vtx3(VertexConsumer vc, Matrix4f m, float cx, float cy, float cz, float[] u, float[] v,
                             float rad, float angle, float[] c) {
        float ca = Mth.cos(angle) * rad;
        float sa = Mth.sin(angle) * rad;
        vtx(vc, m, cx + u[0] * ca + v[0] * sa, cy + u[1] * ca + v[1] * sa, cz + u[2] * ca + v[2] * sa, c[0], c[1],
                c[2], c[3]);
    }

    public static void ring(VertexConsumer vc, Matrix4f m, float cx, float cy, float cz, float[] u, float[] v,
                            float radius, float width, float from, float sweep, int segs, float r, float g, float b,
                            float a) {
        float[] colour = { r, g, b, a };
        annulus(vc, m, cx, cy, cz, u, v, radius - width / 2f, radius + width / 2f, from, sweep, 1, segs,
                (angle, out) -> colour);
    }

    public static void ribbon(VertexConsumer vc, Matrix4f m, Cam cam, float x0, float y0, float z0, float x1, float y1,
                              float z1, float w0, float w1, float r, float g, float b, float a0, float a1) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float dz = z1 - z0;
        float mx = (x0 + x1) / 2f - cam.px();
        float my = (y0 + y1) / 2f - cam.py();
        float mz = (z0 + z1) / 2f - cam.pz();

        float sx = dy * mz - dz * my;
        float sy = dz * mx - dx * mz;
        float sz = dx * my - dy * mx;
        float len = Mth.sqrt(sx * sx + sy * sy + sz * sz);
        if (len < 1.0e-5f) return;
        sx /= len;
        sy /= len;
        sz /= len;
        vtx(vc, m, x0 - sx * w0 / 2f, y0 - sy * w0 / 2f, z0 - sz * w0 / 2f, r, g, b, a0);
        vtx(vc, m, x0 + sx * w0 / 2f, y0 + sy * w0 / 2f, z0 + sz * w0 / 2f, r, g, b, a0);
        vtx(vc, m, x1 + sx * w1 / 2f, y1 + sy * w1 / 2f, z1 + sz * w1 / 2f, r, g, b, a1);
        vtx(vc, m, x1 - sx * w1 / 2f, y1 - sy * w1 / 2f, z1 - sz * w1 / 2f, r, g, b, a1);
    }

    public static void sphere(VertexConsumer vc, Matrix4f m, float cx, float cy, float cz, float radius, int bands,
                              int segs, float r, float g, float b, float a) {
        for (int i = 0; i < bands; i++) {
            float p0 = Mth.PI * i / bands - Mth.HALF_PI;
            float p1 = Mth.PI * (i + 1) / bands - Mth.HALF_PI;
            for (int j = 0; j < segs; j++) {
                float t0 = Mth.TWO_PI * j / segs;
                float t1 = Mth.TWO_PI * (j + 1) / segs;
                pt(vc, m, cx, cy, cz, radius, p0, t0, r, g, b, a);
                pt(vc, m, cx, cy, cz, radius, p0, t1, r, g, b, a);
                pt(vc, m, cx, cy, cz, radius, p1, t1, r, g, b, a);
                pt(vc, m, cx, cy, cz, radius, p1, t0, r, g, b, a);
            }
        }
    }

    private static void pt(VertexConsumer vc, Matrix4f m, float cx, float cy, float cz, float radius, float lat,
                           float lon, float r, float g, float b, float a) {
        float cl = Mth.cos(lat);
        vtx(vc, m, cx + radius * cl * Mth.cos(lon), cy + radius * Mth.sin(lat), cz + radius * cl * Mth.sin(lon), r, g,
                b, a);
    }

    public static void flatDisc(VertexConsumer vc, Matrix4f m, float cx, float cy, float cz, float radius, float r,
                                float g, float b, float aCentre, float aEdge) {
        int segs = 20;
        for (int i = 0; i < segs; i++) {
            float a0 = Mth.TWO_PI * i / segs;
            float a1 = Mth.TWO_PI * (i + 1) / segs;
            vtx(vc, m, cx, cy, cz, r, g, b, aCentre);
            vtx(vc, m, cx + Mth.cos(a0) * radius, cy, cz + Mth.sin(a0) * radius, r, g, b, aEdge);
            vtx(vc, m, cx + Mth.cos(a1) * radius, cy, cz + Mth.sin(a1) * radius, r, g, b, aEdge);
            vtx(vc, m, cx, cy, cz, r, g, b, aCentre);
        }
    }

    public static float[] lerp3(float[] a, float[] b, float f) {
        return new float[] { Mth.lerp(f, a[0], b[0]), Mth.lerp(f, a[1], b[1]), Mth.lerp(f, a[2], b[2]) };
    }

    public static float noise(int i, int seed) {
        int h = i * 374761393 + seed * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFF) / 65535f;
    }

    public static final float[] X = { 1f, 0f, 0f };
    public static final float[] Y = { 0f, 1f, 0f };
    public static final float[] Z = { 0f, 0f, 1f };
}
