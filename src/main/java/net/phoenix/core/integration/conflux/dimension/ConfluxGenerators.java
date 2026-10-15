package net.phoenix.core.integration.conflux.dimension;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.phoenix.core.PhoenixCore;

import com.mojang.serialization.MapCodec;

public final class ConfluxGenerators {

    private static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS = DeferredRegister
            .create(Registries.CHUNK_GENERATOR, PhoenixCore.MOD_ID);

    static {
        CHUNK_GENERATORS.register("discipline", () -> DisciplineChunkGenerator.CODEC);
        CHUNK_GENERATORS.register("ethereal_spawn", () -> EtherealSpawnGenerator.CODEC);
    }

    private ConfluxGenerators() {}

    public static void register(IEventBus bus) {
        CHUNK_GENERATORS.register(bus);
    }
}
