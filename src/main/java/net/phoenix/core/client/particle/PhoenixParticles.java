package net.phoenix.core.client.particle;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.phoenix.core.PhoenixCore;

public class PhoenixParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister
            .create(BuiltInRegistries.PARTICLE_TYPE, PhoenixCore.MOD_ID);

    public static void init(IEventBus modBus) {
        PARTICLES.register(modBus);
    }
}
