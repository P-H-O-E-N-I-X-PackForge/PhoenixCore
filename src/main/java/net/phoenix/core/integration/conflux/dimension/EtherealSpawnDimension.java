package net.phoenix.core.integration.conflux.dimension;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "phoenixcore")
public class EtherealSpawnDimension {

    public static final ResourceKey<Level> DIMENSION_KEY =
        ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "ethereal_spawn"));

    public static final String DIMENSION_ID = "ethereal_spawn";

    public static ChunkGenerator createChunkGenerator(Holder<Biome> voidBiome) {
        BiomeSource biomeSource = new FixedBiomeSource(voidBiome);
        return new EtherealSpawnGenerator(biomeSource);
    }
}
