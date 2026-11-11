package net.phoenix.core.client.renderer.machine;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.conflux.panoptic.PanopticEngineMachine;
import net.phoenix.core.integration.conflux.panoptic.PanopticTreeModel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PanopticTreeRender {

    private static final ResourceLocation WHITE = PhoenixCore.id("textures/misc/panoptic_white.png");

    private static final int WELL_DEPTH = 5;
    private static final int GROWTH_STEPS = 240;

    private static final float[][] COS = new float[9][];
    private static final float[][] SIN = new float[9][];

    static {
        for (int sides : new int[] { 4, 6, 8 }) {
            COS[sides] = new float[sides + 1];
            SIN[sides] = new float[sides + 1];
            for (int i = 0; i <= sides; i++) {
                double a = Math.PI * 2.0 * i / sides + (sides == 4 ? Math.PI / 4.0 : 0.0);
                COS[sides][i] = (float) Math.cos(a);
                SIN[sides][i] = (float) Math.sin(a);
            }
        }
    }

    private record Key(long seed, String discipline, int step) {}

    private record Built(PanopticTreeModel.Params params, List<PanopticTreeModel.Segment> branches,
                         List<PanopticTreeModel.Leaf> leaves, float[] axes, int[] startColor, int[] endColor,
                         int[] leafColor) {}

    private static final Map<Key, Built> CACHE = new LinkedHashMap<>(16, 0.75f, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<Key, Built> eldest) {
            return size() > 12;
        }
    };

    public static ResourceLocation texture() {
        return WHITE;
    }

    private PanopticTreeRender() {}

    private static Built build(long seed, String discipline, int step) {
        PanopticTreeModel.Params params = PanopticTreeModel.params(discipline, step / (float) GROWTH_STEPS);
        List<PanopticTreeModel.Segment> branches = PanopticTreeModel.branches(seed, params);
        List<PanopticTreeModel.Leaf> leaves = PanopticTreeModel.leaves(seed, branches, params);

        int n = branches.size();
        float[] axes = new float[n * 6];
        int[] startColor = new int[n];
        int[] endColor = new int[n];
        for (int i = 0; i < n; i++) {
            PanopticTreeModel.Segment s = branches.get(i);
            float dx = s.x1() - s.x0();
            float dy = s.y1() - s.y0();
            float dz = s.z1() - s.z0();
            float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1e-4f) len = 1f;
            dx /= len;
            dy /= len;
            dz /= len;
            float ux;
            float uy;
            float uz;
            if (Math.abs(dy) > 0.95f) {
                ux = 1f;
                uy = 0f;
                uz = 0f;
            } else {
                ux = -dz;
                uy = 0f;
                uz = dx;
                float ul = (float) Math.sqrt(ux * ux + uz * uz);
                ux /= ul;
                uz /= ul;
            }
            axes[i * 6] = ux;
            axes[i * 6 + 1] = uy;
            axes[i * 6 + 2] = uz;
            axes[i * 6 + 3] = dy * uz - dz * uy;
            axes[i * 6 + 4] = dz * ux - dx * uz;
            axes[i * 6 + 5] = dx * uy - dy * ux;

            float warm = Math.min(0.55f, s.depth() * 0.08f);
            float variation = 0.9f + 0.2f * (((i * 2654435761L) >>> 8 & 0xFF) / 255f);
            int base = scale(PanopticTreeModel.lerpColor(params.trunkColor(), params.leafColorA(), warm), variation);
            float groundShade = s.y0() < 1.5f && s.depth() == 0 ? 0.62f + 0.38f * Math.max(0f, s.y0() / 1.5f) : 1f;
            startColor[i] = scale(base, 0.82f * groundShade);
            endColor[i] = scale(base, 1.05f);
        }

        int[] leafColor = new int[leaves.size() * 4];
        for (int i = 0; i < leaves.size(); i++) {
            PanopticTreeModel.Leaf leaf = leaves.get(i);
            float top = Math.max(0f, Math.min(1f, leaf.y() / Math.max(1f, params.height())));
            for (int k = 0; k < 4; k++) {
                float phase = (leaf.phase() + k * 0.29f) % 1f;
                int color = PanopticTreeModel.lerpColor(params.leafColorA(), params.leafColorB(), phase);
                leafColor[i * 4 + k] = scale(color, 0.7f + 0.3f * top + (phase - 0.5f) * 0.14f);
            }
        }
        return new Built(params, branches, leaves, axes, startColor, endColor, leafColor);
    }

    public static void draw(PanopticEngineMachine engine, float partialTick, PoseStack poseStack,
                            MultiBufferSource buffer, int packedLight, double distance) {
        float growth = engine.shownGrowth();
        String discipline = engine.getDiscipline();
        int step = Math.max(1, Math.round(growth * GROWTH_STEPS));
        Built built = CACHE.computeIfAbsent(new Key(engine.getTreeSeed(), String.valueOf(discipline), step),
                k -> build(k.seed(), discipline, k.step()));

        Direction back = engine.getFrontFacing().getOpposite();
        float seconds = (engine.getOffsetTimer() + partialTick) / 20f;
        int lod = distance < 36 ? 0 : distance < 100 ? 1 : 2;

        poseStack.pushPose();
        poseStack.translate(0.5 + back.getStepX() * WELL_DEPTH, 0.0, 0.5 + back.getStepZ() * WELL_DEPTH);
        VertexConsumer out = buffer.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        PoseStack.Pose pose = poseStack.last();

        float height = Math.max(1f, built.params().height());
        for (int i = 0; i < built.branches().size(); i++) {
            drawBranch(out, pose, built, i, seconds, height, packedLight, lod);
        }
        drawLeaves(out, pose, built, seconds, height, lod);
        poseStack.popPose();
    }

    private static float swayX(float x, float y, float z, float t, float height) {
        return (float) Math.sin(t * 0.9f + y * 0.33f + z * 0.21f) * 0.0045f * y * (0.5f + y / height);
    }

    private static float swayZ(float x, float y, float z, float t, float height) {
        return (float) Math.cos(t * 0.7f + y * 0.29f + x * 0.23f) * 0.0045f * y * (0.5f + y / height);
    }

    private static void drawBranch(VertexConsumer out, PoseStack.Pose pose, Built built, int index, float t,
                                   float height, int light, int lod) {
        PanopticTreeModel.Segment s = built.branches().get(index);
        int sides = s.radius0() > 0.25f ? 8 : s.radius0() > 0.09f ? 6 : 4;
        if (lod == 1) sides = Math.min(sides, 6);
        if (lod == 2) sides = Math.min(sides, 4);

        if (lod == 2 && s.radius0() < 0.07f && s.depth() > 3) return;

        float x0 = s.x0() + swayX(s.x0(), s.y0(), s.z0(), t, height);
        float z0 = s.z0() + swayZ(s.x0(), s.y0(), s.z0(), t, height);
        float x1 = s.x1() + swayX(s.x1(), s.y1(), s.z1(), t, height);
        float z1 = s.z1() + swayZ(s.x1(), s.y1(), s.z1(), t, height);
        float[] axes = built.axes();
        int a = index * 6;
        float ux = axes[a];
        float uy = axes[a + 1];
        float uz = axes[a + 2];
        float vx = axes[a + 3];
        float vy = axes[a + 4];
        float vz = axes[a + 5];
        int c0 = built.startColor()[index];
        int c1 = built.endColor()[index];
        float[] cos = COS[sides];
        float[] sin = SIN[sides];

        for (int i = 0; i < sides; i++) {
            float nx0 = ux * cos[i] + vx * sin[i];
            float ny0 = uy * cos[i] + vy * sin[i];
            float nz0 = uz * cos[i] + vz * sin[i];
            float nx1 = ux * cos[i + 1] + vx * sin[i + 1];
            float ny1 = uy * cos[i + 1] + vy * sin[i + 1];
            float nz1 = uz * cos[i + 1] + vz * sin[i + 1];
            vertex(out, pose, x0 + nx0 * s.radius0(), s.y0() + ny0 * s.radius0(), z0 + nz0 * s.radius0(), c0, light,
                    nx0, ny0, nz0);
            vertex(out, pose, x0 + nx1 * s.radius0(), s.y0() + ny1 * s.radius0(), z0 + nz1 * s.radius0(), c0, light,
                    nx1, ny1, nz1);
            vertex(out, pose, x1 + nx1 * s.radius1(), s.y1() + ny1 * s.radius1(), z1 + nz1 * s.radius1(), c1, light,
                    nx1, ny1, nz1);
            vertex(out, pose, x1 + nx0 * s.radius1(), s.y1() + ny0 * s.radius1(), z1 + nz0 * s.radius1(), c1, light,
                    nx0, ny0, nz0);
        }
    }

    private static void drawLeaves(VertexConsumer out, PoseStack.Pose pose, Built built, float t, float height,
                                   int lod) {
        PanopticTreeModel.Params p = built.params();
        int bright = LightTexture.FULL_BRIGHT;
        float breathe = 0.93f + 0.07f * (float) Math.sin(t * 1.6f);
        int clusters = lod == 0 ? 4 : lod == 1 ? 3 : 2;
        List<PanopticTreeModel.Leaf> leaves = built.leaves();
        for (int i = 0; i < leaves.size(); i++) {
            PanopticTreeModel.Leaf leaf = leaves.get(i);
            float ox = 0;
            float oy = 0;
            float oz = 0;
            float size = leaf.size() * breathe;
            float[] fall = PanopticTreeModel.leafFall(leaf, p, t);
            if (fall != null) {
                ox = fall[0];
                oy = fall[1];
                oz = fall[2];

                float drop = Math.min(1f, -oy / Math.max(1f, p.height() * 0.5f));
                size *= 1f - drop * 0.85f;
                if (size < 0.05f) continue;
            }

            float flutter = (float) Math.sin(t * 3.1f + leaf.phase() * 17f) * 0.07f;
            float bx = leaf.x() + swayX(leaf.x(), leaf.y(), leaf.z(), t, height) + ox + flutter;
            float by = leaf.y() + oy;
            float bz = leaf.z() + swayZ(leaf.x(), leaf.y(), leaf.z(), t, height) + oz - flutter;

            for (int k = 0; k < clusters; k++) {
                float phase = (leaf.phase() + k * 0.29f) % 1f;
                int color = built.leafColor()[i * 4 + k];
                float r = size * (0.52f - k * 0.07f);
                float theta = (float) (phase * Math.PI * 2.0);
                float ax = (float) Math.cos(theta);
                float az = (float) Math.sin(theta);
                float cx = bx + (phase - 0.5f) * size * 0.9f;
                float cy = by + (k - 1) * size * 0.22f;
                float cz = bz + (0.5f - phase) * size * 0.9f;
                float wx = ax * r * 0.62f;
                float wz = az * r * 0.62f;

                float lean = (phase - 0.5f) * r * 0.8f;
                quad(out, pose, color, bright, cx + lean * ax, cy - r, cz + lean * az, cx + wx, cy, cz + wz,
                        cx - lean * ax, cy + r, cz - lean * az, cx - wx, cy, cz - wz, -az, 0, ax);
            }
        }
    }

    private static void quad(VertexConsumer out, PoseStack.Pose pose, int color, int light, float x0, float y0,
                             float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3,
                             float z3, float nx, float ny, float nz) {
        vertex(out, pose, x0, y0, z0, color, light, nx, ny, nz);
        vertex(out, pose, x1, y1, z1, color, light, nx, ny, nz);
        vertex(out, pose, x2, y2, z2, color, light, nx, ny, nz);
        vertex(out, pose, x3, y3, z3, color, light, nx, ny, nz);
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, float z, int argb, int light,
                               float nx, float ny, float nz) {
        out.addVertex(pose, x, y, z).setColor(argb | 0xFF000000).setUv(0.5f, 0.5f)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
    }

    private static int scale(int argb, float k) {
        int r = Math.min(255, (int) (((argb >> 16) & 0xFF) * k));
        int g = Math.min(255, (int) (((argb >> 8) & 0xFF) * k));
        int b = Math.min(255, (int) ((argb & 0xFF) * k));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
