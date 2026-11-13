package net.phoenix.core.client.fx;

import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class FxHook {

    private FxHook() {}

    public static boolean preview(com.gregtechceu.gtceu.api.machine.MetaMachine machine) {
        return FxPreview.isPreview(machine.getBlockPos());
    }

    public static FxRender standard(String id, String title) {
        return FxRender.builder(id, title)
                .param("speed", 0f, 10f, "how fast it animates, 1 being as it is")
                .param("size", 0.1f, 12f, "how big it is, 1 being as it is")
                .param("power", 0f, 4f, "how bright it is, 1 being as it is")
                .build();
    }

    public static FxRender sizePower(String id, String title) {
        return FxRender.builder(id, title)
                .param("size", 0.1f, 12f, "how big it is, 1 being as it is")
                .param("power", 0f, 4f, "how bright it is, 1 being as it is")
                .build();
    }

    public static FxRender tintOnly(String id, String title) {
        return FxRender.builder(id, title)
                .param("power", 0f, 4f, "how bright it is, 1 being as it is")
                .build();
    }

    public static float[] tint(FxRender fx, float r, float g, float b, float a, float s, float t) {
        float[] rgb = fx.colour();
        if (rgb == null && fx.theme() != null) rgb = fx.theme().colourAt(s, t);
        float alpha = Math.min(1f, a * fx.get("power", 1f));
        if (rgb == null) return new float[] { r, g, b, alpha };
        float lum = Math.max(r, Math.max(g, b));
        return new float[] { rgb[0] * lum, rgb[1] * lum, rgb[2] * lum, alpha };
    }

    public static float time(FxRender fx, float natural) {
        return fx.override("speed") != null ? fx.clock().now() : natural;
    }

    public static void scale(FxRender fx, PoseStack poseStack, Vec3 centre) {
        float k = fx.get("size", 1f);
        if (k == 1f) return;
        poseStack.translate(centre.x, centre.y, centre.z);
        poseStack.scale(k, k, k);
        poseStack.translate(-centre.x, -centre.y, -centre.z);
    }

    public static VertexConsumer glow(FxRender fx, Matrix4f pose, VertexConsumer vc, Vec3 centre, float t) {
        float power = fx.get("power", 1f);
        FxTheme theme = fx.theme();
        float[] colour = fx.colour();
        if (power == 1f && theme == null && colour == null) return vc;
        Vector3f c = pose.transformPosition((float) centre.x, (float) centre.y, (float) centre.z, new Vector3f());
        return new Tinted(vc, power, theme, colour, c.x, c.z, t);
    }

    private static final class Tinted implements VertexConsumer {

        private final VertexConsumer out;
        private final float power;
        private final FxTheme theme;
        private final float[] colour;
        private final float cx;
        private final float cz;
        private final float t;
        private float angle;

        Tinted(VertexConsumer out, float power, FxTheme theme, float[] colour, float cx, float cz, float t) {
            this.out = out;
            this.power = power;
            this.theme = theme;
            this.colour = colour;
            this.cx = cx;
            this.cz = cz;
            this.t = t;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            angle = (float) (Math.atan2(z - cz, x - cx) / (2.0 * Math.PI)) + 0.5f;
            out.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            float[] rgb = colour;
            if (rgb == null && theme != null) rgb = theme.colourAt(angle, t);
            int alpha = Math.min(255, Math.max(0, Math.round(a * power)));
            if (rgb == null) {
                out.setColor(r, g, b, alpha);
            } else {

                float lum = Math.max(r, Math.max(g, b)) / 255f;
                float white = Math.max(0f, lum - 0.85f) / 0.15f * 0.5f;
                out.setColor(Math.round(255 * Math.min(1f, rgb[0] * lum + white)),
                        Math.round(255 * Math.min(1f, rgb[1] * lum + white)),
                        Math.round(255 * Math.min(1f, rgb[2] * lum + white)), alpha);
            }
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            out.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            out.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            out.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            out.setNormal(x, y, z);
            return this;
        }
    }
}
