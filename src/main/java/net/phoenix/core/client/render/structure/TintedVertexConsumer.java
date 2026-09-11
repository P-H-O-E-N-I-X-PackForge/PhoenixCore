package net.phoenix.core.client.render.structure;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Ported from Phantasia's {@code TintedVertexConsumer} (verbatim) - a {@link VertexConsumer}
 *  decorator that multiplies emitted vertex color by a settable tint and can offset positions,
 *  used when baking blocks into the structure's VBOs. */
public final class TintedVertexConsumer implements VertexConsumer {

    private final VertexConsumer delegate;

    private double offsetX, offsetY, offsetZ;

    private float r = 1f, g = 1f, b = 1f, a = 1f;

    public TintedVertexConsumer(VertexConsumer delegate) {
        this.delegate = delegate;
    }

    public void setAlpha(float alpha) {
        this.a = alpha;
    }

    /** Used by {@code CinderStructureGhostRenderer} to tint an otherwise normally-textured ghost
     *  preview green/red for build-site validity, without touching alpha. */
    public void setTint(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public void resetTint() {
        r = 1f;
        g = 1f;
        b = 1f;
        a = 1f;
    }

    public void addOffset(double ox, double oy, double oz) {
        offsetX += ox;
        offsetY += oy;
        offsetZ += oz;
    }

    public void clearOffset() {
        offsetX = 0;
        offsetY = 0;
        offsetZ = 0;
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        return delegate.vertex(x + offsetX, y + offsetY, z + offsetZ);
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        return delegate.color((int) (red * r), (int) (green * g), (int) (blue * b), (int) (alpha * a));
    }

    @Override
    public VertexConsumer uv(float u, float v) {
        return delegate.uv(u, v);
    }

    @Override
    public VertexConsumer overlayCoords(int u, int v) {
        return delegate.overlayCoords(u, v);
    }

    @Override
    public VertexConsumer uv2(int u, int v) {
        return delegate.uv2(u, v);
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        return delegate.normal(x, y, z);
    }

    @Override
    public void endVertex() {
        delegate.endVertex();
    }

    @Override
    public void defaultColor(int r, int g, int b, int a) {
        delegate.defaultColor(r, g, b, a);
    }

    @Override
    public void unsetDefaultColor() {
        delegate.unsetDefaultColor();
    }
}
