package net.phoenix.core.integration.continuum.client.render;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * An orbit camera around a focus point, with every parameter easing toward its target so zooms and focus changes
 * glide. {@link #project} maps a world point to GUI pixels, which the map screens use for labels, glows and picking.
 */
public final class MapCamera {

    public float yaw;
    public float pitch;
    public float distance;
    public final Vector3f focus = new Vector3f();

    public float targetYaw;
    public float targetPitch;
    public float targetDistance;
    public final Vector3f targetFocus = new Vector3f();

    public final float minDistance;
    public final float maxDistance;

    public MapCamera(float yaw, float pitch, float distance, float minDistance, float maxDistance) {
        this.yaw = this.targetYaw = yaw;
        this.pitch = this.targetPitch = pitch;
        this.distance = this.targetDistance = distance;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
    }

    public void update(float dt) {
        float k = 1.0f - (float) Math.exp(-dt * 9.0f);
        yaw += (targetYaw - yaw) * k;
        pitch += (targetPitch - pitch) * k;
        distance += (targetDistance - distance) * k;
        focus.add(new Vector3f(targetFocus).sub(focus).mul(k));
    }

    public void rotate(float dYaw, float dPitch) {
        targetYaw += dYaw;
        targetPitch = Math.max(-85.0f, Math.min(85.0f, targetPitch + dPitch));
    }

    public void zoom(float amount) {
        targetDistance = Math.max(minDistance, Math.min(maxDistance, targetDistance * (float) Math.exp(-amount * 0.12)));
    }

    /** Jumps both the current and target distance, for transitions that start a level mid-zoom. */
    public void snapDistance(float value) {
        distance = value;
        targetDistance = Math.max(minDistance, Math.min(maxDistance, value));
    }

    public Matrix4f view() {
        return new Matrix4f()
                .translate(0.0f, 0.0f, -distance)
                .rotateX((float) Math.toRadians(pitch))
                .rotateY((float) Math.toRadians(yaw))
                .translate(-focus.x, -focus.y, -focus.z);
    }

    /** Where a world point lands on a view of {@code width} x {@code height} GUI pixels, or null if behind the camera. */
    public static @Nullable float[] project(Matrix4f viewProjection, Vector3f world, float width, float height) {
        Vector4f clip = viewProjection.transform(new Vector4f(world.x, world.y, world.z, 1.0f));
        if (clip.w <= 0.05f) return null;
        float ndcX = clip.x / clip.w;
        float ndcY = clip.y / clip.w;
        return new float[] { (ndcX * 0.5f + 0.5f) * width, (1.0f - (ndcY * 0.5f + 0.5f)) * height, clip.w };
    }
}
