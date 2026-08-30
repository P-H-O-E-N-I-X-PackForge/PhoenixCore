package net.phoenix.core.integration.conflux.dimension.effects;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

public abstract class ParticleEffectSystem {

    public abstract void update(Level level, @Nullable Player player);

    public static class AshRainParticles extends ParticleEffectSystem {
        private static final float ASH_DENSITY = 0.8f;

        @Override
        public void update(Level level, @Nullable Player player) {
            if (player == null) return;

            int particleCount = (int)(16 * 16 * ASH_DENSITY);
            BlockPos playerPos = player.blockPosition();

            for (int i = 0; i < particleCount; i++) {
                int x = playerPos.getX() + (level.random.nextInt(32) - 16);
                int y = playerPos.getY() + 64 + level.random.nextInt(32);
                int z = playerPos.getZ() + (level.random.nextInt(32) - 16);

                spawnAshParticle(level, x, y, z);
            }
        }

        private void spawnAshParticle(Level level, int x, int y, int z) {

        }
    }

    public static class SoundRipples extends ParticleEffectSystem {
        @Override
        public void update(Level level, @Nullable Player player) {

        }

        public void createRipple(Level level, BlockPos source, float volume) {

        }
    }

    public static class CosmicDust extends ParticleEffectSystem {
        private static final float DUST_DENSITY = 0.6f;

        @Override
        public void update(Level level, @Nullable Player player) {
            if (player == null) return;

            BlockPos playerPos = player.blockPosition();

            int dustCount = (int)(8 * 8 * DUST_DENSITY);

            for (int i = 0; i < dustCount; i++) {
                int x = playerPos.getX() + (level.random.nextInt(40) - 20);
                int y = playerPos.getY() + (level.random.nextInt(40) - 20);
                int z = playerPos.getZ() + (level.random.nextInt(40) - 20);

                spawnCosmicDustParticle(level, x, y, z);
            }
        }

        private void spawnCosmicDustParticle(Level level, int x, int y, int z) {

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

        }
    }
}
