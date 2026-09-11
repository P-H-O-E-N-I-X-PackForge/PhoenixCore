package net.phoenix.core.common.block.cinder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * The two beats of a Rebirth Cinder Core's build sequence - shatter on commit, then a single big
 * impact when the whole structure lands at once after its fall (see
 * {@code CinderConstructionBlockEntity}). Used to be three beats with a per-Y-layer "clang" in
 * between, back when the structure was staged in layer by layer instead of dropped as one batch - see
 * the class doc there for why that changed. Server-triggered {@code sendParticles}/{@code playSound}
 * calls only, deliberately not synced client-state fields - matches this codebase's existing
 * convention for this kind of discrete, one-shot effect (see the wing-suit charging sparks).
 */
public final class CinderVisualEffects {

    private CinderVisualEffects() {}

    public static void playCommitShatter(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        level.sendParticles(ParticleTypes.FLAME, x, y, z, 40, 0.4, 0.4, 0.4, 0.05);
        level.sendParticles(ParticleTypes.ASH, x, y + 0.2, z, 30, 0.5, 0.3, 0.5, 0.03);
        level.sendParticles(ParticleTypes.LAVA, x, y - 0.2, z, 6, 0.3, 0.1, 0.3, 0.0);
        level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0f, 0.7f);
        level.playSound(null, pos, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 0.8f, 0.6f);
    }

    /** The whole structure slamming down at once, scaled to its actual footprint (so a huge Fusion
     *  Reactor-sized drop reads as a much bigger event than a small one) - combines the old landing
     *  "thud" with the old materialize flash into one beat, since there's only one landing now. */
    public static void playImpact(ServerLevel level, BlockPos pos, int spanX, int spanZ) {
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        double spreadX = Math.max(1.0, spanX / 2.0);
        double spreadZ = Math.max(1.0, spanZ / 2.0);
        int footprint = Math.max(1, spanX * spanZ);

        level.sendParticles(ParticleTypes.FLAME, x, y + 0.5, z, Math.min(150, 40 + footprint), spreadX, 0.6, spreadZ,
                0.06);
        level.sendParticles(ParticleTypes.ASH, x, y + 0.8, z, Math.min(80, 20 + footprint / 2), spreadX, 0.6, spreadZ,
                0.04);
        level.sendParticles(ParticleTypes.END_ROD, x, y + 1.0, z, 40, spreadX * 0.6, 1.2, spreadZ * 0.6, 0.05);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y + 0.5, z, 25, spreadX * 0.5, 0.8, spreadZ * 0.5,
                0.1);
        level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.2f, 0.7f);
        level.playSound(null, pos, SoundEvents.TOTEM_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
        level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 0.6f, 1.4f);
    }
}
