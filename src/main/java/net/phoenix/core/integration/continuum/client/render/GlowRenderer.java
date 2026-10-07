package net.phoenix.core.integration.continuum.client.render;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;

/**
 * GUI-space drawing for the map overlays: additive glows (stars, unknown signals, coronas) and thin lines (orbit
 * rings, selection brackets). All coordinates are GUI pixels; colours are 0..1 floats.
 */
public final class GlowRenderer {

    private GlowRenderer() {}

    /** Batches glows into one draw call. */
    public static final class Batch {

        private final Matrix4f pose;
        private final BufferBuilder builder = Tesselator.getInstance().getBuilder();
        private boolean empty = true;

        private Batch(Matrix4f pose) {
            this.pose = pose;
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        }

        public Batch glow(float x, float y, float radius, float r, float g, float b, float a) {
            builder.vertex(pose, x - radius, y + radius, 0).uv(0, 1).color(r, g, b, a).endVertex();
            builder.vertex(pose, x + radius, y + radius, 0).uv(1, 1).color(r, g, b, a).endVertex();
            builder.vertex(pose, x + radius, y - radius, 0).uv(1, 0).color(r, g, b, a).endVertex();
            builder.vertex(pose, x - radius, y - radius, 0).uv(0, 0).color(r, g, b, a).endVertex();
            empty = false;
            return this;
        }

        public Batch glow(float x, float y, float radius, int rgb, float a) {
            return glow(x, y, radius, ((rgb >> 16) & 0xFF) / 255.0f, ((rgb >> 8) & 0xFF) / 255.0f,
                    (rgb & 0xFF) / 255.0f, a);
        }

        public void draw() {
            if (empty || ContinuumShaders.GLOW == null) {
                builder.end();
                return;
            }
            RenderSystem.enableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.setShader(() -> ContinuumShaders.GLOW);
            BufferUploader.drawWithShader(builder.end());
        }
    }

    public static Batch batch(GuiGraphics graphics) {
        return new Batch(graphics.pose().last().pose());
    }

    /** Batches line segments into one draw call. */
    public static final class Lines {

        private final Matrix4f pose;
        private final BufferBuilder builder = Tesselator.getInstance().getBuilder();
        private boolean empty = true;

        private Lines(Matrix4f pose) {
            this.pose = pose;
            builder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        }

        public Lines line(float x1, float y1, float x2, float y2, float r, float g, float b, float a) {
            builder.vertex(pose, x1, y1, 0).color(r, g, b, a).endVertex();
            builder.vertex(pose, x2, y2, 0).color(r, g, b, a).endVertex();
            empty = false;
            return this;
        }

        public Lines line(float x1, float y1, float x2, float y2, int rgb, float a) {
            return line(x1, y1, x2, y2, ((rgb >> 16) & 0xFF) / 255.0f, ((rgb >> 8) & 0xFF) / 255.0f,
                    (rgb & 0xFF) / 255.0f, a);
        }

        /** Four corner ticks around a point, for hover and selection. */
        public Lines brackets(float cx, float cy, float half, float tick, int rgb, float a) {
            float l = cx - half, r = cx + half, t = cy - half, b = cy + half;
            line(l, t, l + tick, t, rgb, a).line(l, t, l, t + tick, rgb, a);
            line(r, t, r - tick, t, rgb, a).line(r, t, r, t + tick, rgb, a);
            line(l, b, l + tick, b, rgb, a).line(l, b, l, b - tick, rgb, a);
            line(r, b, r - tick, b, rgb, a).line(r, b, r, b - tick, rgb, a);
            return this;
        }

        public void draw(float width) {
            if (empty) {
                builder.end();
                return;
            }
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.lineWidth(width);
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            BufferUploader.drawWithShader(builder.end());
            RenderSystem.lineWidth(1.0f);
        }
    }

    public static Lines lines(GuiGraphics graphics) {
        return new Lines(graphics.pose().last().pose());
    }
}
