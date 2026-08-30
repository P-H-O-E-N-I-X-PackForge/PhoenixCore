package net.phoenix.core.integration.conflux.dimension.particles;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class ParticleEffectSystem {

    public static void spawnAshRain(Vec3 position, int count) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level == null) return;

            for (int i = 0; i < count; i++) {
                double x = position.x + (Math.random() - 0.5) * 20;
                double y = position.y + Math.random() * 10;
                double z = position.z + (Math.random() - 0.5) * 20;

                double vx = (Math.random() - 0.5) * 0.5;
                double vy = -Math.random() * 0.3; 
                double vz = (Math.random() - 0.5) * 0.5;

                SimpleParticleType particle = DimensionParticleTypes.VOLCANIC_ASH.get();
                mc.level.addParticle(particle, x, y, z, vx, vy, vz);
            }
        });
    }

    public static void spawnHeatShimmer(Vec3 position, int count) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level == null) return;

            for (int i = 0; i < count; i++) {
                double x = position.x + (Math.random() - 0.5) * 5;
                double y = position.y + Math.random() * 3;
                double z = position.z + (Math.random() - 0.5) * 5;

                double vx = (Math.random() - 0.5) * 0.2;
                double vy = Math.random() * 0.5; 
                double vz = (Math.random() - 0.5) * 0.2;

                SimpleParticleType particle = DimensionParticleTypes.HEAT_SHIMMER.get();
                mc.level.addParticle(particle, x, y, z, vx, vy, vz);
            }
        });
    }

    public static void spawnLavaSparks(Vec3 position, int count) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level == null) return;

            for (int i = 0; i < count; i++) {
                double x = position.x + (Math.random() - 0.5) * 10;
                double y = position.y + Math.random() * 20;
                double z = position.z + (Math.random() - 0.5) * 10;

                double vx = (Math.random() - 0.5) * 2;
                double vy = Math.random() * 2; 
                double vz = (Math.random() - 0.5) * 2;

                SimpleParticleType particle = DimensionParticleTypes.LAVA_SPARK.get();
                mc.level.addParticle(particle, x, y, z, vx, vy, vz);
            }
        });
    }

    public static void spawnSculkSpores(Vec3 position, int count) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level == null) return;

            for (int i = 0; i < count; i++) {
                double x = position.x + (Math.random() - 0.5) * 15;
                double y = position.y + (Math.random() - 0.5) * 10;
                double z = position.z + (Math.random() - 0.5) * 15;

                double vx = (Math.random() - 0.5) * 0.3;
                double vy = (Math.random() - 0.5) * 0.3;
                double vz = (Math.random() - 0.5) * 0.3;

                SimpleParticleType particle = DimensionParticleTypes.SCULK_SPORE.get();
                mc.level.addParticle(particle, x, y, z, vx, vy, vz);
            }
        });
    }

    public static void spawnBiolumGlow(Vec3 position, int count) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level == null) return;

            for (int i = 0; i < count; i++) {
                double x = position.x + (Math.random() - 0.5) * 8;
                double y = position.y + Math.random() * 5;
                double z = position.z + (Math.random() - 0.5) * 8;

                double vx = (Math.random() - 0.5) * 0.1;
                double vy = Math.random() * 0.2;
                double vz = (Math.random() - 0.5) * 0.1;

                SimpleParticleType particle = DimensionParticleTypes.BIOLUM_GLOW.get();
                mc.level.addParticle(particle, x, y, z, vx, vy, vz);
            }
        });
    }

    public static void spawnCosmicDust(Vec3 position, int count) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level == null) return;

            for (int i = 0; i < count; i++) {
                double x = position.x + (Math.random() - 0.5) * 30;
                double y = position.y + (Math.random() - 0.5) * 30;
                double z = position.z + (Math.random() - 0.5) * 30;

                double vx = (Math.random() - 0.5) * 0.2;
                double vy = (Math.random() - 0.5) * 0.2;
                double vz = (Math.random() - 0.5) * 0.2;

                SimpleParticleType particle = DimensionParticleTypes.COSMIC_DUST.get();
                mc.level.addParticle(particle, x, y, z, vx, vy, vz);
            }
        });
    }

    public static void spawnVoidShimmer(Vec3 position, int count) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level == null) return;

            for (int i = 0; i < count; i++) {
                double x = position.x + (Math.random() - 0.5) * 20;
                double y = position.y + (Math.random() - 0.5) * 20;
                double z = position.z + (Math.random() - 0.5) * 20;

                double vx = (Math.random() - 0.5) * 0.3;
                double vy = (Math.random() - 0.5) * 0.3;
                double vz = (Math.random() - 0.5) * 0.3;

                SimpleParticleType particle = DimensionParticleTypes.VOID_SHIMMER.get();
                mc.level.addParticle(particle, x, y, z, vx, vy, vz);
            }
        });
    }

    public static void spawnNeonSparks(Vec3 position, int count) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level == null) return;

            for (int i = 0; i < count; i++) {
                double x = position.x + (Math.random() - 0.5) * 12;
                double y = position.y + Math.random() * 8;
                double z = position.z + (Math.random() - 0.5) * 12;

                double vx = (Math.random() - 0.5) * 1.0;
                double vy = Math.random() * 1.0;
                double vz = (Math.random() - 0.5) * 1.0;

                SimpleParticleType particle = DimensionParticleTypes.NEON_SPARK.get();
                mc.level.addParticle(particle, x, y, z, vx, vy, vz);
            }
        });
    }

    public static void spawnIndustrialSmoke(Vec3 position, int count) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level == null) return;

            for (int i = 0; i < count; i++) {
                double x = position.x + (Math.random() - 0.5) * 15;
                double y = position.y + Math.random() * 5;
                double z = position.z + (Math.random() - 0.5) * 15;

                double vx = (Math.random() - 0.5) * 0.5;
                double vy = Math.random() * 0.4; 
                double vz = (Math.random() - 0.5) * 0.5;

                SimpleParticleType particle = DimensionParticleTypes.INDUSTRIAL_SMOKE.get();
                mc.level.addParticle(particle, x, y, z, vx, vy, vz);
            }
        });
    }

    public static void spawnGlitchEffect(Vec3 position, int count) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level == null) return;

            for (int i = 0; i < count; i++) {
                double x = position.x + (Math.random() - 0.5) * 25;
                double y = position.y + (Math.random() - 0.5) * 25;
                double z = position.z + (Math.random() - 0.5) * 25;

                double vx = (Math.random() - 0.5) * 1.5;
                double vy = (Math.random() - 0.5) * 1.5;
                double vz = (Math.random() - 0.5) * 1.5;

                SimpleParticleType particle = DimensionParticleTypes.GLITCH_EFFECT.get();
                mc.level.addParticle(particle, x, y, z, vx, vy, vz);
            }
        });
    }

    public static void spawnRealityTears(Vec3 position, int count) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level == null) return;

            for (int i = 0; i < count; i++) {
                double x = position.x + (Math.random() - 0.5) * 15;
                double y = position.y + (Math.random() - 0.5) * 15;
                double z = position.z + (Math.random() - 0.5) * 15;

                double vx = (Math.random() - 0.5) * 1.2;
                double vy = (Math.random() - 0.5) * 1.2;
                double vz = (Math.random() - 0.5) * 1.2;

                SimpleParticleType particle = DimensionParticleTypes.REALITY_TEAR.get();
                mc.level.addParticle(particle, x, y, z, vx, vy, vz);
            }
        });
    }

    public static void spawnStarSparkles(Vec3 position, int count) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level == null) return;

            for (int i = 0; i < count; i++) {
                double x = position.x + (Math.random() - 0.5) * 40;
                double y = position.y + (Math.random() - 0.5) * 40;
                double z = position.z + (Math.random() - 0.5) * 40;

                double vx = (Math.random() - 0.5) * 0.1;
                double vy = (Math.random() - 0.5) * 0.1;
                double vz = (Math.random() - 0.5) * 0.1;

                SimpleParticleType particle = DimensionParticleTypes.STAR_SPARKLE.get();
                mc.level.addParticle(particle, x, y, z, vx, vy, vz);
            }
        });
    }
}
