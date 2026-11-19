package net.phoenix.core.client.fx;

import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * What a glow render needs to join {@code /pftfx} with three small changes, the same in each:
 *
 * <pre>{@code
 * private static final FxRender FX = FxHook.standard("null_well", "Null Well");
 *
 * float t = FxHook.time(FX, naturalSeconds);            // the speed setting
 * ...                                                    // the centre of the render is c
 * poseStack.pushPose();
 * FxHook.scale(FX, poseStack, c);                        // the size setting, before the pose is read
 * Matrix4f m = poseStack.last().pose();
 * VertexConsumer vc = FxHook.glow(FX, m, buffer.getBuffer(GlowDraw.GLOW), c, t);   // brightness, colour and theme
 * }</pre>
 *
 * The settings are {@code speed} (1 as it runs), {@code size} (a multiplier, 1 as it is) and {@code power} (a
 * brightness multiplier, 1 as it is), plus a colour or a theme. A render that wants more settings, or to read the theme
 * itself, can build its own {@link FxRender} and use as much of this as it likes.
 */
public final class FxHook {

    private FxHook() {}

    /**
     * True if this machine is one that {@code /pftfx <render> spawn} put down, so the render should draw as if it
     * were formed and running, with nothing built around it.
     */
    public static boolean preview(com.gregtechceu.gtceu.api.machine.MetaMachine machine) {
        return FxPreview.isPreview(machine.getBlockPos());
    }

    /** A render with the usual three settings: speed, size and power. */
    public static FxRender standard(String id, String title) {
        return FxRender.builder(id, title)
                .param("speed", 0f, 10f, "how fast it animates, 1 being as it is")
                .param("size", 0.1f, 12f, "how big it is, 1 being as it is")
                .param("power", 0f, 4f, "how bright it is, 1 being as it is")
                .param("detail", 0.3f, 1f, "how many steps the 3D shader takes: lower is faster, 1 is the full look")
                .build();
    }

    /** A render with a size and a brightness, for one that has nothing to speed up. */
    public static FxRender sizePower(String id, String title) {
        return FxRender.builder(id, title)
                .param("size", 0.1f, 12f, "how big it is, 1 being as it is")
                .param("power", 0f, 4f, "how bright it is, 1 being as it is")
                .build();
    }

    /** A render that can only be made brighter, dimmer or another colour, such as one that fills a space. */
    public static FxRender tintOnly(String id, String title) {
        return FxRender.builder(id, title)
                .param("power", 0f, 4f, "how bright it is, 1 being as it is")
                .build();
    }

    /**
     * The colour to give a model that is drawn with a red, green, blue and alpha of its own: the same shape, brighter
     * or
     * dimmer by the power setting, or in the colour or theme that was set. {@code s} is how far along a theme to look,
     * from 0 to 1, and {@code t} is seconds.
     */
    public static float[] tint(FxRender fx, float r, float g, float b, float a, float s, float t) {
        float[] rgb = fx.colour();
        if (rgb == null && fx.theme() != null) rgb = fx.theme().colourAt(s, t);
        float alpha = Math.min(1f, a * fx.get("power", 1f));
        if (rgb == null) return new float[] { r, g, b, alpha };
        float lum = Math.max(r, Math.max(g, b));
        return new float[] { rgb[0] * lum, rgb[1] * lum, rgb[2] * lum, alpha };
    }

    /** Seconds for the animation: the render's own time, or one that runs at the speed that was set. */
    public static float time(FxRender fx, float natural) {
        return fx.override("speed") != null ? fx.clock().now() : natural;
    }

    /**
     * Scales what is drawn about the middle of the render. Call it after {@code pushPose} and before reading the pose.
     */
    public static void scale(FxRender fx, PoseStack poseStack, Vec3 centre) {
        float k = fx.get("size", 1f);
        if (k == 1f) return;
        poseStack.translate(centre.x, centre.y, centre.z);
        poseStack.scale(k, k, k);
        poseStack.translate(-centre.x, -centre.y, -centre.z);
    }

    /**
     * The consumer to draw glow with: the render's own if nothing is set, or one that scales the brightness and swaps
     * the colours for the colour that was set, or for a theme that runs round the middle of the render. The shape of
     * the colours stays: what was bright is still bright, and what was dark is still dark.
     */
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
                // keep how bright each part was, in the new colour
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
