package net.phoenix.core.integration.conflux.dimension.physics;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public record PhysicsImpulse(double dx, double dy, double dz, double capXZ, double capUp, double setUp, double scaleXZ,
                             boolean resetFall) {

    public static final double GRAVITY = 0.08;
    private static final double MOVING = 0.03;

    public static PhysicsImpulse lift(double accel, double cap) {
        return new PhysicsImpulse(0, GRAVITY + accel, 0, 0, cap, 0, 1, true);
    }

    public static PhysicsImpulse push(double dx, double dz, double capXZ) {
        return new PhysicsImpulse(dx, 0, dz, capXZ, 0, 0, 1, false);
    }

    public static PhysicsImpulse bounce(double speed) {
        return new PhysicsImpulse(0, 0, 0, 0, 0, speed, 1, true);
    }

    public static PhysicsImpulse slide(double scale) {
        return new PhysicsImpulse(0, 0, 0, 0, 0, 0, scale, false);
    }

    public void apply(Entity entity) {
        Vec3 m = entity.getDeltaMovement();
        double x = m.x;
        double y = m.y;
        double z = m.z;

        if (scaleXZ != 1.0 && Math.hypot(x, z) > MOVING) {
            x *= scaleXZ;
            z *= scaleXZ;
        }
        if (dx != 0.0 || dz != 0.0) {
            double nx = x + dx;
            double nz = z + dz;
            if (capXZ > 0.0) {
                double speed = Math.hypot(nx, nz);
                double now = Math.hypot(x, z);
                if (speed > capXZ && speed > 0.0) {
                    double f = Math.max(capXZ, now) / speed;
                    nx *= f;
                    nz *= f;
                }
            }
            x = nx;
            z = nz;
        }
        if (dy != 0.0) {
            double ny = y + dy;
            if (dy > 0.0 && ny > capUp) ny = Math.max(y, capUp);
            y = ny;
        }
        if (setUp > 0.0 && y < setUp) y = setUp;

        entity.setDeltaMovement(x, y, z);
        if (resetFall) entity.fallDistance = 0.0f;
    }
}
