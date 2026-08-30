package net.phoenix.core.integration.conflux.dimension.particles;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class DimensionParticleTypes {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
        DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, "phoenixcore");

    public static final RegistryObject<SimpleParticleType> VOLCANIC_ASH =
        PARTICLE_TYPES.register("volcanic_ash", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> HEAT_SHIMMER =
        PARTICLE_TYPES.register("heat_shimmer", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> LAVA_SPARK =
        PARTICLE_TYPES.register("lava_spark", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> SCULK_SPORE =
        PARTICLE_TYPES.register("sculk_spore", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> BIOLUM_GLOW =
        PARTICLE_TYPES.register("biolum_glow", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> SCULK_TENDRIL =
        PARTICLE_TYPES.register("sculk_tendril", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> COSMIC_DUST =
        PARTICLE_TYPES.register("cosmic_dust", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> VOID_SHIMMER =
        PARTICLE_TYPES.register("void_shimmer", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> STAR_SPARKLE =
        PARTICLE_TYPES.register("star_sparkle", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> NEON_SPARK =
        PARTICLE_TYPES.register("neon_spark", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> INDUSTRIAL_SMOKE =
        PARTICLE_TYPES.register("industrial_smoke", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> ENERGY_BOLT =
        PARTICLE_TYPES.register("energy_bolt", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> GLITCH_EFFECT =
        PARTICLE_TYPES.register("glitch_effect", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> REALITY_TEAR =
        PARTICLE_TYPES.register("reality_tear", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> INVERTED_GLOW =
        PARTICLE_TYPES.register("inverted_glow", () -> new SimpleParticleType(false));

    public static void register(IEventBus modEventBus) {
        PARTICLE_TYPES.register(modEventBus);
        System.out.println("[PhoenixCore Particles] Registered " + 15 + " custom particle types");
    }

    public static SimpleParticleType getParticleType(String dimension, String particleType) {
        return switch (dimension) {
            case "phoenix" -> switch (particleType) {
                case "ash" -> VOLCANIC_ASH.get();
                case "shimmer" -> HEAT_SHIMMER.get();
                case "spark" -> LAVA_SPARK.get();
                default -> VOLCANIC_ASH.get();
            };

            case "sculk" -> switch (particleType) {
                case "spore" -> SCULK_SPORE.get();
                case "glow" -> BIOLUM_GLOW.get();
                case "tendril" -> SCULK_TENDRIL.get();
                default -> SCULK_SPORE.get();
            };

            case "void" -> switch (particleType) {
                case "dust" -> COSMIC_DUST.get();
                case "shimmer" -> VOID_SHIMMER.get();
                case "sparkle" -> STAR_SPARKLE.get();
                default -> COSMIC_DUST.get();
            };

            case "sealed_a" -> switch (particleType) {
                case "spark" -> NEON_SPARK.get();
                case "smoke" -> INDUSTRIAL_SMOKE.get();
                case "bolt" -> ENERGY_BOLT.get();
                default -> NEON_SPARK.get();
            };

            case "sealed_b" -> switch (particleType) {
                case "glitch" -> GLITCH_EFFECT.get();
                case "tear" -> REALITY_TEAR.get();
                case "glow" -> INVERTED_GLOW.get();
                default -> GLITCH_EFFECT.get();
            };

            default -> VOLCANIC_ASH.get();
        };
    }
}
