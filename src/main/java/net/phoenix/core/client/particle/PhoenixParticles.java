package net.phoenix.core.client.particle;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.ForgeRegistries;
import net.neoforged.neoforge.registries.RegistryObject;
import net.phoenix.core.PhoenixCore;

public class PhoenixParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister
            .create(ForgeRegistries.PARTICLE_TYPES, PhoenixCore.MOD_ID);

    public static final RegistryObject<SimpleParticleType> TESLA_SPARK = PARTICLES.register("tesla_spark",
            () -> new SimpleParticleType(false));

    public static void init(IEventBus modBus) {
        PARTICLES.register(modBus);
    }
}
