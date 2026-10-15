package net.phoenix.core.integration.conflux.dimension.effects;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

public class BioluminescentLighting {

    private static final float GLOW_RANGE = 24.0f;
    private static final int GLOW_COLOR = 0x00FF88;
    private static final float GLOW_INTENSITY = 0.9f;

    public void update(Level level, @Nullable Player player) {
        if (player == null || level.isClientSide) return;

        BlockPos playerPos = player.blockPosition();

        applyBiolumGlow(level, playerPos);

        spawnSculkEyes(level, playerPos, player);
    }

    private void applyBiolumGlow(Level level, BlockPos center) {
        int range = (int) GLOW_RANGE;

        for (int x = center.getX() - range; x <= center.getX() + range; x++) {
            for (int y = center.getY() - range; y <= center.getY() + range; y++) {
                for (int z = center.getZ() - range; z <= center.getZ() + range; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    double distance = center.distSqr(pos);

                    if (distance <= GLOW_RANGE * GLOW_RANGE) {

                        if (shouldGlow(level, pos)) {

                            applyGlowToBlock(level, pos);
                        }
                    }
                }
            }
        }
    }

    private boolean shouldGlow(Level level, BlockPos pos) {
        var block = level.getBlockState(pos).getBlock();

        if (block.toString().contains("sculk")) {
            return true;
        }

        if (block.toString().contains("flower") ||
                block.toString().contains("plant") ||
                block.toString().contains("vine")) {
            return true;
        }

        return false;
    }

    private void applyGlowToBlock(Level level, BlockPos pos) {}

    private void spawnSculkEyes(Level level, BlockPos playerPos, Player player) {
        if (level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, playerPos) > 8) {
            return;
        }

        int range = 30;
        int attempts = 5;

        for (int i = 0; i < attempts; i++) {
            int x = playerPos.getX() + (level.random.nextInt(range * 2) - range);
            int y = playerPos.getY() + (level.random.nextInt(20) - 10);
            int z = playerPos.getZ() + (level.random.nextInt(range * 2) - range);

            BlockPos eyePos = new BlockPos(x, y, z);

            if (isValidEyePosition(level, eyePos)) {

            }
        }
    }

    private boolean isValidEyePosition(Level level, BlockPos pos) {
        int lightLevel = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos);
        return lightLevel < 7;
    }

    public void playAmbientWhispers(Level level, Player player) {}

    public void createSoundRipple(Level level, BlockPos source, float volume) {}
}
