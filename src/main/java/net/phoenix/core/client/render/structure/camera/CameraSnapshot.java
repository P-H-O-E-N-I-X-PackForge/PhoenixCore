package net.phoenix.core.client.render.structure.camera;

public record CameraSnapshot(
                             float yaw,
                             float pitch,
                             float zoom,
                             float targetX,
                             float targetY,
                             float targetZ,
                             boolean playerOwned) {}
