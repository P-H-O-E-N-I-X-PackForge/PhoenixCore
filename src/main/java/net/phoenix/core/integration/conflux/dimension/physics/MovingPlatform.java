package net.phoenix.core.integration.conflux.dimension.physics;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class MovingPlatform {

    public enum PlatformType {
        CONVEYOR_BELT,
        ELEVATOR_UP,
        ELEVATOR_DOWN,
        SPIRAL
    }

    private final Vec3 position;
    private final Vec3 size;
    private final Vec3 velocity;
    private final PlatformType type;
    private final AABB bounds;
    private final float speed;
    private final boolean isActive;

    public MovingPlatform(Vec3 position, Vec3 size, Vec3 velocity, PlatformType type, float speed) {
        this.position = position;
        this.size = size;
        this.velocity = velocity;
        this.type = type;
        this.speed = speed;
        this.bounds = new AABB(
                position.x - size.x / 2, position.y - size.y / 2, position.z - size.z / 2,
                position.x + size.x / 2, position.y + size.y / 2, position.z + size.z / 2);
        this.isActive = true;
    }

    public boolean isEntityOnPlatform(Entity entity) {
        if (!isActive) return false;

        AABB entityBounds = entity.getBoundingBox();
        AABB platformTop = new AABB(
                bounds.minX, bounds.maxY - 0.1, bounds.minZ,
                bounds.maxX, bounds.maxY + 0.1, bounds.maxZ);

        return entityBounds.intersects(platformTop);
    }

    public void applyVelocity(Entity entity) {
        if (!isEntityOnPlatform(entity)) {
            return;
        }

        Vec3 scaledVelocity = velocity.scale(speed);

        switch (type) {
            case CONVEYOR_BELT -> {

                entity.setDeltaMovement(
                        scaledVelocity.x,
                        entity.getDeltaMovement().y,
                        scaledVelocity.z);
            }

            case ELEVATOR_UP -> {

                Vec3 current = entity.getDeltaMovement();
                entity.setDeltaMovement(
                        current.x,
                        scaledVelocity.y,
                        current.z);
            }

            case ELEVATOR_DOWN -> {

                Vec3 current = entity.getDeltaMovement();
                entity.setDeltaMovement(
                        current.x,
                        scaledVelocity.y,
                        current.z);
            }

            case SPIRAL -> {

                double angle = System.currentTimeMillis() * 0.001;
                double radius = 5.0;
                double rotationX = Math.cos(angle) * radius * speed;
                double rotationZ = Math.sin(angle) * radius * speed;

                Vec3 current = entity.getDeltaMovement();
                entity.setDeltaMovement(
                        rotationX,
                        scaledVelocity.y,
                        rotationZ);
            }
        }
    }

    public AABB getPlatformBounds() {
        return bounds;
    }

    public Vec3 getMotionDirection() {
        return velocity.normalize();
    }

    public Vec3 getPosition() {
        return position;
    }

    public Vec3 getSize() {
        return size;
    }

    public Vec3 getVelocity() {
        return velocity;
    }

    public PlatformType getType() {
        return type;
    }

    public float getSpeed() {
        return speed;
    }

    public boolean isActive() {
        return isActive;
    }

    @Override
    public String toString() {
        return String.format("MovingPlatform[type=%s pos=%.1f,%.1f,%.1f size=%.1f,%.1f,%.1f speed=%.1f]",
                type, position.x, position.y, position.z, size.x, size.y, size.z, speed);
    }
}
