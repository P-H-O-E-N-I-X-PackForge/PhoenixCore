package net.phoenix.core.client.render.structure.camera;

import org.joml.Vector3f;

/** Ported from Phantasia's {@code net.phoenixvine.phantasia.client.camera.CameraView} (verbatim). */
public record CameraView(Vector3f eyePos, Vector3f lookAt) {

    public float eyeX() {
        return eyePos.x();
    }

    public float eyeY() {
        return eyePos.y();
    }

    public float eyeZ() {
        return eyePos.z();
    }

    public float lookAtX() {
        return lookAt.x();
    }

    public float lookAtY() {
        return lookAt.y();
    }

    public float lookAtZ() {
        return lookAt.z();
    }

    public Vector3f direction() {
        return new Vector3f(lookAt).sub(eyePos);
    }
}
