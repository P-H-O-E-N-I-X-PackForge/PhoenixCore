package net.phoenix.core.integration.conflux.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.phoenix.core.PhoenixCore;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class EtherealSpawnGenerator extends ChunkGenerator {

    private static final int PLATFORM_Y = 64;
    private static final int SHRINE_RADIUS = 10;
    private static final int[][] PILLAR_OFFSETS = { { -8, -8 }, { 8, -8 }, { -8, 8 }, { 8, 8 } };

    public static final Codec<EtherealSpawnGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
    ).apply(instance, instance.stable(EtherealSpawnGenerator::new)));

    public static void register() {
        Registry.register(BuiltInRegistries.CHUNK_GENERATOR, PhoenixCore.id("ethereal_spawn"), CODEC);
    }

    public EtherealSpawnGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState randomState, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving carving) {
        
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState randomState, ChunkAccess chunk) {
        
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion worldGenRegion) {
        
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        return CompletableFuture.supplyAsync(() -> {
            ChunkPos chunkPos = chunk.getPos();
            int minX = chunkPos.getMinBlockX();
            int minZ = chunkPos.getMinBlockZ();

            for (int x = minX; x < minX + 16; x++) {
                for (int z = minZ; z < minZ + 16; z++) {
                    if (!isInSpawnArea(x, z)) continue;

                    chunk.setBlockState(new BlockPos(x, PLATFORM_Y, z), platformTopBlock(x, z), false);
                    for (int depth = 1; depth <= 3; depth++) {
                        chunk.setBlockState(new BlockPos(x, PLATFORM_Y - depth, z),
                                Blocks.CALCITE.defaultBlockState(), false);
                    }

                    if (!isInShrine(x, z) && Math.floorMod(x * 928371 + z * 12289, 97) == 0) {
                        chunk.setBlockState(new BlockPos(x, PLATFORM_Y + 1, z),
                                Blocks.AMETHYST_CLUSTER.defaultBlockState(), false);
                    }
                }
            }

            placeShrine(chunk, minX, minZ);
            return chunk;
        }, executor);
    }

    private BlockState platformTopBlock(int x, int z) {
        return isInShrine(x, z) ? Blocks.SMOOTH_QUARTZ.defaultBlockState() : Blocks.CALCITE.defaultBlockState();
    }

    private boolean isInShrine(int x, int z) {
        return x * x + z * z <= SHRINE_RADIUS * SHRINE_RADIUS;
    }

    private void placeShrine(ChunkAccess chunk, int minX, int minZ) {
        for (int[] offset : PILLAR_OFFSETS) {
            placePillarIfInChunk(chunk, minX, minZ, offset[0], offset[1]);
        }
        placeChestIfInChunk(chunk, minX, minZ);
    }

    private void placePillarIfInChunk(ChunkAccess chunk, int minX, int minZ, int x, int z) {
        if (x < minX || x >= minX + 16 || z < minZ || z >= minZ + 16) return;

        for (int y = PLATFORM_Y + 1; y <= PLATFORM_Y + 3; y++) {
            chunk.setBlockState(new BlockPos(x, y, z), Blocks.PURPUR_PILLAR.defaultBlockState(), false);
        }
        chunk.setBlockState(new BlockPos(x, PLATFORM_Y + 4, z), Blocks.GLOWSTONE.defaultBlockState(), false);
    }

    private void placeChestIfInChunk(ChunkAccess chunk, int minX, int minZ) {
        int x = 0;
        int z = 0;
        if (x < minX || x >= minX + 16 || z < minZ || z >= minZ + 16) return;

        BlockPos chestPos = new BlockPos(x, PLATFORM_Y + 1, z);
        chunk.setBlockState(chestPos, Blocks.CHEST.defaultBlockState(), false);

        if (chunk.getBlockEntity(chestPos) instanceof RandomizableContainerBlockEntity chestEntity) {
            chestEntity.setLootTable(PhoenixCore.id("chests/ethereal_spawn"), chestPos.asLong());
        }
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types heightmapTypes, LevelHeightAccessor levelHeightAccessor, RandomState randomState) {
        if (isInSpawnArea(x, z)) {
            return PLATFORM_Y;
        }
        return levelHeightAccessor.getMinBuildHeight();
    }

    @Override
    public NoiseColumn getBaseColumn(int i, int i1, LevelHeightAccessor levelHeightAccessor, RandomState randomState) {
        return new NoiseColumn(levelHeightAccessor.getMinBuildHeight(), new net.minecraft.world.level.block.state.BlockState[0]);
    }

    @Override
    public int getMinY() {
        return -64;
    }

    @Override
    public int getGenDepth() {
        return 384;
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public void addDebugScreenInfo(java.util.List<String> list, RandomState randomState, BlockPos pos) {
        
    }

    private boolean isInSpawnArea(int x, int z) {
        return Math.abs(x) < 64 && Math.abs(z) < 64;
    }
}