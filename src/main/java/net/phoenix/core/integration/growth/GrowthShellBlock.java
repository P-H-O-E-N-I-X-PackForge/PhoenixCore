package net.phoenix.core.integration.growth;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** The living shell a growing machine builds around itself. It lets off the odd spore. */
public class GrowthShellBlock extends Block {

    public GrowthShellBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(24) != 0) return;
        level.addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, pos.getX() + random.nextDouble(),
                pos.getY() + 1.05, pos.getZ() + random.nextDouble(), 0.0, -0.02, 0.0);
    }
}
