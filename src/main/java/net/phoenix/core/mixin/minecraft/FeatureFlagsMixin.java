package net.phoenix.core.mixin.minecraft;

import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Any custom datapack dimension (like Conflux's 5 discipline dimensions) makes vanilla treat
 * the whole world as "using experimental features", forcing the confirmation screen on every
 * new world. There's nothing actually experimental about it - it's just how vanilla flags any
 * non-default LevelStem set - so this always reports the world as non-experimental instead.
 */
@Mixin(FeatureFlags.class)
public class FeatureFlagsMixin {

    @Overwrite
    public static boolean isExperimental(FeatureFlagSet flagSet) {
        return false;
    }
}
