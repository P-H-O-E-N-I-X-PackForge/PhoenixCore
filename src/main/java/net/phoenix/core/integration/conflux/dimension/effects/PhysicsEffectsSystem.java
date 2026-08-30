package net.phoenix.core.integration.conflux.dimension.effects;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

public abstract class PhysicsEffectsSystem {

    public abstract void update(Level level, @Nullable Player player);

    public static class GravityAnomalies extends PhysicsEffectsSystem {
        private static final float NORMAL_GRAVITY = 0.08f;
        private static final float LOW_GRAVITY = 0.04f;
        private static final float REVERSE_GRAVITY = -0.08f;

        @Override
        public void update(Level level, @Nullable Player player) {
            if (player == null) return;

            BlockPos playerPos = player.blockPosition();

            modifyGravityForPlayer(player, playerPos);
        }

        private void modifyGravityForPlayer(Player player, BlockPos pos) {

        }
    }

    public static class SculkTendrilGrowth extends PhysicsEffectsSystem {
        @Override
        public void update(Level level, @Nullable Player player) {
            if (player == null || level.getGameTime() % 10 != 0) return;

            BlockPos playerPos = player.blockPosition();

            growSculkTendrils(level, playerPos);
        }

        private void growSculkTendrils(Level level, BlockPos playerPos) {

        }
    }

    public static class GravityBridges extends PhysicsEffectsSystem {
        @Override
        public void update(Level level, @Nullable Player player) {
            if (player == null) return;

            BlockPos playerPos = player.blockPosition();

            applyBridgeGravity(level, player, playerPos);
        }

        private void applyBridgeGravity(Level level, Player player, BlockPos pos) {

        }
    }

    public static class MovingPlatforms extends PhysicsEffectsSystem {
        @Override
        public void update(Level level, @Nullable Player player) {
            if (player == null) return;

            BlockPos playerPos = player.blockPosition();

            handleMovingPlatformMotion(level, player, playerPos);
        }

        private void handleMovingPlatformMotion(Level level, Player player, BlockPos pos) {

        }

        public void createElevator(Level level, BlockPos basePos, int height) {

        }

        public void createConveyorBelt(Level level, BlockPos start, BlockPos end, int direction) {

        }
    }

    public static class RealityGlitches extends PhysicsEffectsSystem {
        @Override
        public void update(Level level, @Nullable Player player) {
            if (level.getGameTime() % 15 != 0) return;

            if (level.random.nextFloat() < 0.2f) {
                createRandomGlitch(level, player);
            }
        }

        private void createRandomGlitch(Level level, @Nullable Player player) {
            if (player == null) return;

            BlockPos playerPos = player.blockPosition();

            int x = playerPos.getX() + (level.random.nextInt(40) - 20);
            int y = playerPos.getY() + (level.random.nextInt(20) - 10);
            int z = playerPos.getZ() + (level.random.nextInt(40) - 20);

            BlockPos randomPos = new BlockPos(x, y, z);

            glitchBlock(level, randomPos);
        }

        private void glitchBlock(Level level, BlockPos pos) {

        }
    }
}
