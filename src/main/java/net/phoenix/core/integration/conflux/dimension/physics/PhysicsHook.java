package net.phoenix.core.integration.conflux.dimension.physics;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "phoenixcore", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PhysicsHook {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        event.getServer().getAllLevels().forEach(level -> {
            applyPhysicsToLevel(level);
        });
    }

    private static void applyPhysicsToLevel(Level level) {
        String dimensionId = getDimensionId(level);

        PhysicsRegistry registry = PhysicsRegistry.getInstance();
        var gravityZones = registry.getGravityZones(dimensionId);
        var platforms = registry.getPlatforms(dimensionId);

        if (gravityZones.isEmpty() && platforms.isEmpty()) {
            return;
        }

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.getAllEntities().forEach(entity -> {
                applyGravityZones(entity, gravityZones);
                applyMovingPlatforms(entity, platforms);
            });
        }
    }

    static void applyPhysicsToEntity(Entity entity, Level level) {
        String dimensionId = getDimensionId(level);

        PhysicsRegistry registry = PhysicsRegistry.getInstance();
        var gravityZones = registry.getGravityZones(dimensionId);
        var platforms = registry.getPlatforms(dimensionId);

        applyGravityZones(entity, gravityZones);
        applyMovingPlatforms(entity, platforms);
    }

    private static void applyGravityZones(Entity entity, java.util.List<GravityZone> zones) {
        float maxMultiplier = 1.0f;

        for (GravityZone zone : zones) {
            float multiplier = zone.getGravityMultiplierForEntity(entity);
            if (multiplier < maxMultiplier) {
                maxMultiplier = multiplier;
            }
        }

        if (maxMultiplier < 1.0f) {
            applyGravityModifier(entity, maxMultiplier);
        }
    }

    private static void applyGravityModifier(Entity entity, float gravityMultiplier) {
        if (entity.onGround() && gravityMultiplier > 0.8f) {
            return;
        }

        Vec3 velocity = entity.getDeltaMovement();
        double newY = velocity.y * gravityMultiplier;

        newY = Math.max(newY, -0.5);

        entity.setDeltaMovement(velocity.x, newY, velocity.z);
    }

    private static void applyMovingPlatforms(Entity entity, java.util.List<MovingPlatform> platforms) {
        for (MovingPlatform platform : platforms) {
            if (platform.isEntityOnPlatform(entity)) {
                platform.applyVelocity(entity);
            }
        }
    }

    private static String getDimensionId(Level level) {
        String path = level.dimension().location().getPath();
        String discipline = path.startsWith("conflux/") ? path.substring("conflux/".length()) : path;

        if (discipline.startsWith("phoenix")) return "phoenix";
        if (discipline.startsWith("sculk")) return "sculk";
        if (discipline.startsWith("void")) return "void";
        if (discipline.startsWith("sealed_a")) return "sealed_a";
        if (discipline.startsWith("sealed_b")) return "sealed_b";

        return "";
    }
}
