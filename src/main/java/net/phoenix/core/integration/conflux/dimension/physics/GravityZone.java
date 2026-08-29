package net.phoenix.core.integration.conflux.dimension.physics;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class GravityZone {

    private final Vec3 center;
    private final double radius;
    private final float gravityMultiplier;
    private final String dimensionId;
    private final AABB bounds;
    private final boolean isActive;

    public GravityZone(Vec3 center, double radius, float gravityMultiplier, String dimensionId) {
        this.center = center;
        this.radius = radius;
        this.gravityMultiplier = gravityMultiplier;
        this.dimensionId = dimensionId;
        this.bounds = new AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);
        this.isActive = true;
    }

    public boolean containsEntity(Entity entity) {
        if (!isActive) return false;
        return entity.getBoundingBox().intersects(bounds);
    }

    public float getGravityMultiplierForEntity(Entity entity) {
        if (!containsEntity(entity)) {
            return 1.0f;
        }

        double distFromCenter = entity.position().distanceTo(center);
        if (distFromCenter > radius) {
            return 1.0f;
        }

        double normalized = 1.0 - (distFromCenter / radius);
        float lerped = (float) (1.0 + (gravityMultiplier - 1.0) * normalized);

        return Math.max(0.0f, Math.min(1.0f, lerped));
    }

    public void applyGravity(Entity entity) {
        if (!containsEntity(entity)) {
            return;
        }

        float multiplier = getGravityMultiplierForEntity(entity);
        Vec3 velocity = entity.getDeltaMovement();

        double newY = velocity.y * multiplier;
        entity.setDeltaMovement(velocity.x, newY, velocity.z);
    }

    public Vec3 getCenter() {
        return center;
    }

    public double getRadius() {
        return radius;
    }

    public float getGravityMultiplier() {
        return gravityMultiplier;
    }

    public String getDimensionId() {
        return dimensionId;
    }

    public boolean isActive() {
        return isActive;
    }

    @Override
    public String toString() {
        return String.format("GravityZone[center=%.1f,%.1f,%.1f radius=%.1f gravity=%.2f]",
                center.x, center.y, center.z, radius, gravityMultiplier);
    }
}
