package net.phoenix.core.client.renderer.cinema.cutscene.background;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.phoenix.core.client.renderer.cinema.cutscene.background.BackgroundEffect.Point;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public final class CutsceneBackgroundBuilder {

    private final List<Layer> layers = new ArrayList<>();
    private int resolution = Layered.DEFAULT_RESOLUTION;
    private float speed = 1f;

    private CutsceneBackgroundBuilder() {}

    public static CutsceneBackgroundBuilder create() {
        return new CutsceneBackgroundBuilder();
    }

    public CutsceneBackgroundBuilder layer(BackgroundEffect effect) {
        return layer(effect, 1f, BlendMode.NORMAL);
    }

    public CutsceneBackgroundBuilder layer(BackgroundEffect effect, float opacity) {
        return layer(effect, opacity, BlendMode.NORMAL);
    }

    public CutsceneBackgroundBuilder layer(BackgroundEffect effect, float opacity, BlendMode blend) {
        layers.add(new Layer(effect, opacity, blend));
        return this;
    }

    public CutsceneBackgroundBuilder resolution(int samplesAcross) {
        this.resolution = samplesAcross;
        return this;
    }

    public CutsceneBackgroundBuilder speed(float multiplier) {
        this.speed = multiplier;
        return this;
    }

    public Layered build() {
        return new Layered(List.copyOf(layers), resolution, speed);
    }

    public record Layer(BackgroundEffect effect, float opacity, BlendMode blend) {

        public static final Codec<Layer> CODEC = RecordCodecBuilder.create(i -> i.group(
                BackgroundEffects.MAP_CODEC.forGetter(layer -> (BackgroundEffect.Data) layer.effect()),
                Codec.FLOAT.optionalFieldOf("opacity", 1f).forGetter(Layer::opacity),
                BlendMode.CODEC.optionalFieldOf("blend", BlendMode.NORMAL).forGetter(Layer::blend))
                .apply(i, Layer::new));
    }

    public record Layered(List<Layer> layers, int resolution, float speed) implements CutsceneBackground.Data {

        public static final int DEFAULT_RESOLUTION = 64;

        public static final MapCodec<Layered> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Layer.CODEC.listOf().fieldOf("layers").forGetter(Layered::layers),
                Codec.intRange(2, 256).optionalFieldOf("resolution", DEFAULT_RESOLUTION).forGetter(Layered::resolution),
                Codec.FLOAT.optionalFieldOf("speed", 1f).forGetter(Layered::speed))
                .apply(i, Layered::new));

        @Override
        public String type() {
            return "layered";
        }

        @Override
        public void render(GuiGraphics graphics, BackgroundContext context) {
            int width = context.width(), height = context.height();
            if (layers.isEmpty() || width <= 0 || height <= 0 || context.alpha() <= 0) return;

            int cols = Mth.clamp(resolution, 2, 256);
            int rows = Math.max(2, Math.round(cols * height / (float) width));
            float aspect = width / (float) height;
            float time = context.time() * speed;

            int[] colors = new int[(cols + 1) * (rows + 1)];
            Point point = new Point();
            for (int gy = 0; gy <= rows; gy++) {
                for (int gx = 0; gx <= cols; gx++) {
                    point.u = gx / (float) cols;
                    point.v = gy / (float) rows;
                    point.nx = (point.u * 2 - 1) * aspect;
                    point.ny = point.v * 2 - 1;
                    point.dist = Mth.sqrt(point.nx * point.nx + point.ny * point.ny);
                    point.angle = (float) Mth.atan2(point.ny, point.nx);

                    int color = 0;
                    for (Layer layer : layers) {
                        int sample = layer.effect().colorAt(point, time);
                        int alpha = Math.round(((sample >>> 24) & 0xFF) * layer.opacity());
                        if (alpha <= 0) continue;
                        color = layer.blend().composite(color, (Math.min(255, alpha) << 24) | (sample & 0xFFFFFF));
                    }
                    colors[gy * (cols + 1) + gx] = BackgroundEffects.withAlphaScale(color, context.alpha());
                }
            }

            Matrix4f matrix = graphics.pose().last().pose();
            VertexConsumer consumer = graphics.bufferSource().getBuffer(RenderType.gui());
            for (int gy = 0; gy < rows; gy++) {
                float y0 = gy * height / (float) rows, y1 = (gy + 1) * height / (float) rows;
                for (int gx = 0; gx < cols; gx++) {
                    float x0 = gx * width / (float) cols, x1 = (gx + 1) * width / (float) cols;
                    int i = gy * (cols + 1) + gx;
                    consumer.vertex(matrix, x0, y0, 0).color(colors[i]).endVertex();
                    consumer.vertex(matrix, x0, y1, 0).color(colors[i + cols + 1]).endVertex();
                    consumer.vertex(matrix, x1, y1, 0).color(colors[i + cols + 2]).endVertex();
                    consumer.vertex(matrix, x1, y0, 0).color(colors[i + 1]).endVertex();
                }
            }
            graphics.flush();
        }
    }
}
