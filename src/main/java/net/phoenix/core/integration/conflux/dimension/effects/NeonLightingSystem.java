package net.phoenix.core.integration.conflux.dimension.effects;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import org.jetbrains.annotations.Nullable;

public class NeonLightingSystem {

    private static final int NEON_PINK = 0xFF1493;
    private static final int NEON_CYAN = 0x00FFFF;
    private static final int NEON_YELLOW = 0xFFFF00;
    private static final float GLOW_INTENSITY = 1.0f;

    private long lastUpdateTime = 0;

    public void update(Level level, @Nullable Player player) {
        if (player == null) return;

        long gameTime = level.getGameTime();

        float pulse = (float) Math.sin(gameTime * 0.05f) * 0.3f + 0.7f;

        applyNeonGlow(level, player.blockPosition(), pulse);

        spawnNeonParticles(level, player.blockPosition());
    }

    private void applyNeonGlow(Level level, BlockPos center, float intensity) {
        int range = 32;

        for (int x = center.getX() - range; x <= center.getX() + range; x++) {
            for (int y = center.getY() - range; y <= center.getY() + range; y++) {
                for (int z = center.getZ() - range; z <= center.getZ() + range; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    double distance = center.distSqr(pos);

                    if (distance <= range * range) {
                        var blockState = level.getBlockState(pos);
                        var block = blockState.getBlock();

                        if (isNeonBlock(block)) {

                            applyNeonEffectToBlock(level, pos, getNeonColor(block), intensity);
                        }
                    }
                }
            }
        }
    }

    private boolean isNeonBlock(net.minecraft.world.level.block.Block block) {
        String blockName = block.getName().getString().toLowerCase();

        return blockName.contains("light") ||
                blockName.contains("lamp") ||
                blockName.contains("glow") ||
                blockName.contains("glowstone") ||
                blockName.contains("amethyst") ||
                blockName.contains("neon") ||
                block == Blocks.AMETHYST_CLUSTER ||
                block == Blocks.GLOWSTONE ||
                block == Blocks.SHROOMLIGHT;
    }

    private int getNeonColor(net.minecraft.world.level.block.Block block) {
        String name = block.getName().getString().toLowerCase();

        if (name.contains("pink") || name.contains("magenta")) {
            return NEON_PINK;
        } else if (name.contains("cyan") || name.contains("blue")) {
            return NEON_CYAN;
        } else if (name.contains("yellow") || name.contains("gold")) {
            return NEON_YELLOW;
        }

        int hashCode = block.hashCode();
        return switch (hashCode % 3) {
            case 0 -> NEON_PINK;
            case 1 -> NEON_CYAN;
            default -> NEON_YELLOW;
        };
    }

    private void applyNeonEffectToBlock(Level level, BlockPos pos, int color, float intensity) {}

    private void spawnNeonParticles(Level level, BlockPos center) {}

    public void createNeonSign(Level level, BlockPos pos, String text, int color) {}

    public void pulsateNeon(BlockPos center, float frequency) {}

    public void createLightBeam(Level level, BlockPos start, BlockPos end, int color) {}
}
