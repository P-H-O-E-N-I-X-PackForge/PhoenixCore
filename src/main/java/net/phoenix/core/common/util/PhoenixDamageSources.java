package net.phoenix.core.common.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;
import net.phoenix.core.PhoenixCore;

@SuppressWarnings("removal")
public class PhoenixDamageSources {

    public static final ResourceKey<DamageType> STERILIZED = ResourceKey.create(Registries.DAMAGE_TYPE,
            new ResourceLocation(PhoenixCore.MOD_ID, "sterilized"));

    public static void bootstrap(BootstapContext<DamageType> context) {
        context.register(STERILIZED, new DamageType("sterilized", 0.1F));
    }

    public static DamageSource sterilized(Level level) {
        return new DamageSource(
                level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(STERILIZED));
    }
}
