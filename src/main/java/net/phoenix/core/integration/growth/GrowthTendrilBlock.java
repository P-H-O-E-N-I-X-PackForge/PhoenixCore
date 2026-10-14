package net.phoenix.core.integration.growth;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** A block of tendril: more spores than the shell, and the odd glowing mote. */
public class GrowthTendrilBlock extends Block {

    public GrowthTendrilBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(10) == 0) {
            level.addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, pos.getX() + random.nextDouble(),
                    pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(), 0.0, -0.01, 0.0);
        }
        if (random.nextInt(60) == 0) {
            level.addParticle(ParticleTypes.HAPPY_VILLAGER, pos.getX() + random.nextDouble(),
                    pos.getY() + 1.0, pos.getZ() + random.nextDouble(), 0.0, 0.02, 0.0);
        }
    }
}
