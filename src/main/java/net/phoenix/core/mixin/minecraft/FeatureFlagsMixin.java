package net.phoenix.core.mixin.minecraft;

import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(FeatureFlags.class)
public class FeatureFlagsMixin {

    @Overwrite
    public static boolean isExperimental(FeatureFlagSet flagSet) {
        return false;
    }
}
