package net.phoenix.core.integration.conflux.dimension.effects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.phoenix.core.integration.conflux.dimension.particles.DimensionParticleTypes;

import org.jetbrains.annotations.Nullable;

public abstract class ParticleEffectSystem {

    public abstract void update(Level level, @Nullable Player player);

    /**
     * Dispatches a single particle add to the client thread - every spawn* method below used to be an
     * empty body, so none of these ambient effects ever actually rendered anything.
     */
    private static void spawnClientParticle(Level level, SimpleParticleType type, double x, double y, double z,
                                            double vx, double vy, double vz) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level == null) return;
            mc.level.addParticle(type, x, y, z, vx, vy, vz);
        });
    }

    public static class AshRainParticles extends ParticleEffectSystem {

        // The original 16*16*0.8 = ~204 particles/tick (4000+/sec) was never actually run, since
        // spawnAshParticle was an empty stub - that count would have been a serious frame-time hit.
        // Scaled down to a sane ambient sprinkle; DimensionEffectsManager also only ticks this every
        // few client ticks, not every tick.
        private static final int PARTICLES_PER_UPDATE = 6;

        @Override
        public void update(Level level, @Nullable Player player) {
            if (player == null) return;

            BlockPos playerPos = player.blockPosition();

            for (int i = 0; i < PARTICLES_PER_UPDATE; i++) {
                int x = playerPos.getX() + (level.random.nextInt(32) - 16);
                int y = playerPos.getY() + 12 + level.random.nextInt(16);
                int z = playerPos.getZ() + (level.random.nextInt(32) - 16);

                spawnAshParticle(level, x, y, z);
            }
        }

        private void spawnAshParticle(Level level, int x, int y, int z) {
            spawnClientParticle(level, DimensionParticleTypes.VOLCANIC_ASH.get(), x + 0.5, y, z + 0.5,
                    (level.random.nextDouble() - 0.5) * 0.05, -0.05, (level.random.nextDouble() - 0.5) * 0.05);
        }
    }

    public static class SoundRipples extends ParticleEffectSystem {

        // Sculk's ambient loop is driven by explicit createRipple() calls (e.g. from a nearby sculk
        // sensor/shrieker triggering), not a per-tick spawn - nothing periodic belongs here.
        @Override
        public void update(Level level, @Nullable Player player) {}

        public void createRipple(Level level, BlockPos source, float volume) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.level == null) return;

                for (int i = 0; i < 6; i++) {
                    double angle = (Math.PI * 2 / 6) * i;
                    double x = source.getX() + 0.5 + Math.cos(angle) * 0.6;
                    double y = source.getY() + 0.3;
                    double z = source.getZ() + 0.5 + Math.sin(angle) * 0.6;
                    mc.level.addParticle(ParticleTypes.SCULK_CHARGE_POP, x, y, z, 0.0, 0.02, 0.0);
                }
            });

            level.playSound(null, source, SoundEvents.SCULK_CLICKING_STOP, SoundSource.AMBIENT,
                    Math.max(0.1f, volume), 1.0f + (level.random.nextFloat() * 0.2f));
        }
    }

    public static class CosmicDust extends ParticleEffectSystem {

        // Same rescale reasoning as AshRainParticles - 8*8*0.6 = ~30/tick was never tuned against an
        // actually-running spawn call.
        private static final int PARTICLES_PER_UPDATE = 3;

        @Override
        public void update(Level level, @Nullable Player player) {
            if (player == null) return;

            BlockPos playerPos = player.blockPosition();

            for (int i = 0; i < PARTICLES_PER_UPDATE; i++) {
                int x = playerPos.getX() + (level.random.nextInt(40) - 20);
                int y = playerPos.getY() + (level.random.nextInt(40) - 20);
                int z = playerPos.getZ() + (level.random.nextInt(40) - 20);

                spawnCosmicDustParticle(level, x, y, z);
            }
        }

        private void spawnCosmicDustParticle(Level level, int x, int y, int z) {
            spawnClientParticle(level, DimensionParticleTypes.COSMIC_DUST.get(), x + 0.5, y + 0.5, z + 0.5,
                    (level.random.nextDouble() - 0.5) * 0.02, (level.random.nextDouble() - 0.5) * 0.02,
                    (level.random.nextDouble() - 0.5) * 0.02);
        }
    }

    public static class GlitchEffects extends ParticleEffectSystem {

        @Override
        public void update(Level level, @Nullable Player player) {
            if (level.getGameTime() % 20 != 0) return;

            if (level.random.nextFloat() < 0.1f) {
                createRandomGlitch(level, player);
            }
        }

        private void createRandomGlitch(Level level, @Nullable Player player) {
            if (player == null) return;

            BlockPos playerPos = player.blockPosition();
            int x = playerPos.getX() + (level.random.nextInt(40) - 20);
            int y = playerPos.getY() + (level.random.nextInt(20) - 10);
            int z = playerPos.getZ() + (level.random.nextInt(40) - 20);

            for (int i = 0; i < 10; i++) {
                spawnClientParticle(level, DimensionParticleTypes.GLITCH_EFFECT.get(),
                        x + level.random.nextDouble(), y + level.random.nextDouble(), z + level.random.nextDouble(),
                        (level.random.nextDouble() - 0.5) * 0.3, (level.random.nextDouble() - 0.5) * 0.3,
                        (level.random.nextDouble() - 0.5) * 0.3);
            }
        }
    }
}
