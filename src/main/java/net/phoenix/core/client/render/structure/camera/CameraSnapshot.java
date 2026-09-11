package net.phoenix.core.client.render.structure.camera;

/** Ported from Phantasia's {@code net.phoenixvine.phantasia.client.camera.CameraSnapshot} (verbatim). */
public record CameraSnapshot(
                             float yaw,
                             float pitch,
                             float zoom,
                             float targetX,
                             float targetY,
                             float targetZ,
                             boolean playerOwned) {}
