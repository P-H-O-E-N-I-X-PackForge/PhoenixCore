package net.phoenix.core.client.render.structure.camera;

import net.minecraft.util.Mth;

import org.joml.Vector3f;

import javax.annotation.Nullable;

public class StructureCamera {

    public record LerpJob(
                          float fromYaw, float fromPitch, float fromZoom,
                          float fromTX, float fromTY, float fromTZ,
                          float toYaw, float toPitch, float toZoom,
                          float toTX, float toTY, float toTZ,
                          int totalTicks,
                          int elapsed,
                          LerpType type) {

        LerpJob advance() {
            return new LerpJob(fromYaw, fromPitch, fromZoom,
                    fromTX, fromTY, fromTZ,
                    toYaw, toPitch, toZoom,
                    toTX, toTY, toTZ,
                    totalTicks, elapsed + 1, type);
        }

        boolean finished() {
            return type == LerpType.SNAP || elapsed >= totalTicks;
        }
    }

    public static final float FOV = 60f;

    private float yaw;
    private float pitch;
    private float zoom;
    private float targetX;
    private float targetY;
    private float targetZ;

    private float floorY = Float.NEGATIVE_INFINITY;

    @Nullable
    private LerpJob activeLerp;

    private boolean playerOwned = false;

    private boolean locked = false;

    @Nullable
    private CameraSnapshot savedSnapshot;

    public StructureCamera(float yaw, float pitch, float zoom,
                           float targetX, float targetY, float targetZ) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.zoom = zoom;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
    }

    public void tick() {
        if (activeLerp == null) return;

        if (activeLerp.finished()) {
            commitLerpEnd();
            activeLerp = null;
            return;
        }

        activeLerp = activeLerp.advance();

        if (activeLerp.finished()) {
            commitLerpEnd();
            activeLerp = null;
        }
    }

    private void commitLerpEnd() {
        yaw = normYaw(activeLerp.toYaw());
        pitch = activeLerp.toPitch();
        zoom = activeLerp.toZoom();
        targetX = activeLerp.toTX();
        targetY = activeLerp.toTY();
        targetZ = activeLerp.toTZ();
    }

    public CameraView getView(float partialTicks) {
        float ry, rp, rz, tx, ty, tz;

        if (activeLerp != null && !activeLerp.finished()) {
            float t = (activeLerp.elapsed() + partialTicks) / (float) activeLerp.totalTicks();
            t = Mth.clamp(t, 0f, 1f);
            t = applyEasing(t, activeLerp.type());

            ry = lerpAngle(activeLerp.fromYaw(), activeLerp.toYaw(), t);
            rp = Mth.lerp(t, activeLerp.fromPitch(), activeLerp.toPitch());
            rz = Mth.lerp(t, activeLerp.fromZoom(), activeLerp.toZoom());
            tx = Mth.lerp(t, activeLerp.fromTX(), activeLerp.toTX());
            ty = Mth.lerp(t, activeLerp.fromTY(), activeLerp.toTY());
            tz = Mth.lerp(t, activeLerp.fromTZ(), activeLerp.toTZ());
        } else {
            ry = yaw;
            rp = pitch;
            rz = zoom;
            tx = targetX;
            ty = targetY;
            tz = targetZ;
        }

        return buildView(ry, rp, rz, tx, ty, tz);
    }

    private CameraView buildView(float yawDeg, float pitchDeg, float dist, float tx, float ty, float tz) {
        double yr = Math.toRadians(yawDeg);
        double pr = Math.toRadians(pitchDeg);

        float nx = (float) (Math.cos(pr) * Math.sin(yr));
        float ny = (float) Math.sin(pr);
        float nz = (float) (Math.cos(pr) * Math.cos(yr));

        float eyeX = tx + nx * dist;
        float eyeY = ty + ny * dist;
        float eyeZ = tz + nz * dist;

        eyeY = Math.max(eyeY, floorY);

        return new CameraView(new Vector3f(eyeX, eyeY, eyeZ), new Vector3f(tx, ty, tz));
    }

    public void orbit(float dx, float dy) {
        cancelLerp();
        this.yaw -= dx;
        this.pitch = Mth.clamp(this.pitch + dy, -89.99f, 89.99f);
        this.playerOwned = true;
    }

    public void zoom(float factor, float minZoom, float maxZoom) {
        cancelLerp();
        zoom = Mth.clamp(zoom * factor, minZoom, maxZoom);
        playerOwned = true;
    }

    public void pan(float worldDX, float worldDY, float worldDZ) {
        cancelLerp();
        targetX += worldDX;
        targetY += worldDY;
        targetZ += worldDZ;
        playerOwned = true;
    }

    public void hardReset(float toYaw, float toPitch, float toZoom, float toTX, float toTY, float toTZ,
                          LerpType lerpType, int lerpTicks) {
        playerOwned = false;
        startLerp(toYaw, toPitch, toZoom, toTX, toTY, toTZ, lerpType, lerpTicks);
    }

    public void hardReset(float toYaw, float toPitch, float toZoom, float toTX, float toTY, float toTZ) {
        hardReset(toYaw, toPitch, toZoom, toTX, toTY, toTZ, LerpType.SNAP, 0);
    }

    public void save() {
        savedSnapshot = new CameraSnapshot(yaw, pitch, zoom, targetX, targetY, targetZ, playerOwned);
    }

    public boolean restore() {
        if (savedSnapshot == null) return false;
        cancelLerp();
        yaw = savedSnapshot.yaw();
        pitch = savedSnapshot.pitch();
        zoom = savedSnapshot.zoom();
        targetX = savedSnapshot.targetX();
        targetY = savedSnapshot.targetY();
        targetZ = savedSnapshot.targetZ();
        playerOwned = savedSnapshot.playerOwned();
        savedSnapshot = null;
        return true;
    }

    public void setFloorY(float y) {
        this.floorY = y;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean l) {
        this.locked = l;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        cancelLerp();
        this.yaw = normYaw(yaw);
    }

    public float getPitch() {
        return pitch;
    }

    public float getZoom() {
        return zoom;
    }

    public boolean isPlayerOwned() {
        return playerOwned;
    }

    public void setPosition(float yaw, float pitch, float zoom) {
        this.yaw = normYaw(yaw);
        this.pitch = pitch;
        this.zoom = zoom;
    }

    public void setTarget(float tx, float ty, float tz) {
        this.targetX = tx;
        this.targetY = ty;
        this.targetZ = tz;
    }

    public void getRightAndUp(Vector3f outRight, Vector3f outUp) {
        double yr = Math.toRadians(yaw);
        double pr = Math.toRadians(pitch);
        float fx = -(float) (Math.cos(pr) * Math.sin(yr));
        float fy = -(float) Math.sin(pr);
        float fz = -(float) (Math.cos(pr) * Math.cos(yr));
        Vector3f fwd = new Vector3f(fx, fy, fz).normalize();
        Vector3f worldUp = new Vector3f(0, 1, 0);
        fwd.cross(worldUp, outRight);
        outRight.normalize();
        outRight.cross(fwd, outUp);
        outUp.normalize();
    }

    private void startLerp(float toYaw, float toPitch, float toZoom, float toTX, float toTY, float toTZ,
                           LerpType type, int durationTicks) {
        if (type == LerpType.SNAP || durationTicks <= 0) {
            activeLerp = null;
            yaw = normYaw(toYaw);
            pitch = toPitch;
            zoom = toZoom;
            targetX = toTX;
            targetY = toTY;
            targetZ = toTZ;
            return;
        }

        float curYaw = yaw, curPitch = pitch, curZoom = zoom, curTX = targetX, curTY = targetY, curTZ = targetZ;
        if (activeLerp != null) {
            float t = Mth.clamp((float) activeLerp.elapsed() / activeLerp.totalTicks(), 0f, 1f);
            t = applyEasing(t, activeLerp.type());
            curYaw = lerpAngle(activeLerp.fromYaw(), activeLerp.toYaw(), t);
            curPitch = Mth.lerp(t, activeLerp.fromPitch(), activeLerp.toPitch());
            curZoom = Mth.lerp(t, activeLerp.fromZoom(), activeLerp.toZoom());
            curTX = Mth.lerp(t, activeLerp.fromTX(), activeLerp.toTX());
            curTY = Mth.lerp(t, activeLerp.fromTY(), activeLerp.toTY());
            curTZ = Mth.lerp(t, activeLerp.fromTZ(), activeLerp.toTZ());
        }

        float normTo = shortArcYaw(curYaw, toYaw);
        activeLerp = new LerpJob(curYaw, curPitch, curZoom, curTX, curTY, curTZ,
                normTo, toPitch, toZoom, toTX, toTY, toTZ, durationTicks, 0, type);
    }

    private void cancelLerp() {
        if (activeLerp == null) return;
        float t = Mth.clamp((float) activeLerp.elapsed() / activeLerp.totalTicks(), 0f, 1f);
        t = applyEasing(t, activeLerp.type());
        yaw = lerpAngle(activeLerp.fromYaw(), activeLerp.toYaw(), t);
        pitch = Mth.lerp(t, activeLerp.fromPitch(), activeLerp.toPitch());
        zoom = Mth.lerp(t, activeLerp.fromZoom(), activeLerp.toZoom());
        targetX = Mth.lerp(t, activeLerp.fromTX(), activeLerp.toTX());
        targetY = Mth.lerp(t, activeLerp.fromTY(), activeLerp.toTY());
        targetZ = Mth.lerp(t, activeLerp.fromTZ(), activeLerp.toTZ());
        activeLerp = null;
    }

    private static float applyEasing(float t, LerpType type) {
        return switch (type) {
            case SNAP, LINEAR -> t;
            case EASE_IN -> t * t;
            case EASE_OUT -> 1f - (1f - t) * (1f - t);
            case EASE_IN_OUT -> t < 0.5f ? 2f * t * t : 1f - (-2f * t + 2f) * (-2f * t + 2f) / 2f;
            case SMOOTHSTEP -> t * t * (3.0f - 2.0f * t);
            case SINE_IN -> 1.0f - (float) Math.cos(t * Math.PI / 2.0);
            case SINE_OUT -> (float) Math.sin(t * Math.PI / 2.0);
            case SINE_IN_OUT -> -((float) Math.cos(Math.PI * t) - 1.0f) / 2.0f;
        };
    }

    private static float lerpAngle(float from, float to, float t) {
        float delta = ((to - from + 540f) % 360f) - 180f;
        return from + delta * t;
    }

    private static float shortArcYaw(float from, float to) {
        float delta = ((to - from + 540f) % 360f) - 180f;
        return from + delta;
    }

    private static float normYaw(float yaw) {
        yaw = yaw % 360f;
        if (yaw > 180f) yaw -= 360f;
        if (yaw < -180f) yaw += 360f;
        return yaw;
    }
}
