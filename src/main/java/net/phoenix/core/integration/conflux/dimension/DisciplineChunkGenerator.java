package net.phoenix.core.integration.conflux.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.worldgen.TerrainProfile;
import net.phoenix.core.integration.conflux.dimension.worldgen.*;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class DisciplineChunkGenerator extends ChunkGenerator {
    public static final Codec<DisciplineChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
            Codec.STRING.fieldOf("discipline_id").forGetter(gen -> gen.disciplineId),
            Codec.LONG.fieldOf("seed").forGetter(gen -> gen.seed)
    ).apply(instance, instance.stable(DisciplineChunkGenerator::new)));

    private final TerrainProfile terrainProfile;
    private final WorldgenProfile worldgenProfile;
    private final String disciplineId;
    private final long seed;

    private volatile TerrainProfile worldSeededProfile;

    private final com.gregtechceu.gtceu.api.data.worldgen.ores.OrePlacer orePlacer =
            new com.gregtechceu.gtceu.api.data.worldgen.ores.OrePlacer();

    private static final int PROVINCE_CELL_SIZE = 160;
    private static final float PROVINCE_VEIN_CHANCE = 0.4f;
    private final Map<Long, ProvinceVeinInstance> provinceCellCache = new ConcurrentHashMap<>();

    private record ProvinceVeinInstance(BlockPos origin, int radius, long seed, ProvinceVeinTemplates.Template template) {}

    private static final java.util.concurrent.atomic.AtomicLong terrainNanos = new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong decorationNanos = new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong oreNanos = new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong provinceOreNanos = new java.util.concurrent.atomic.AtomicLong();

    public static void resetProfiling() {
        terrainNanos.set(0);
        decorationNanos.set(0);
        oreNanos.set(0);
        provinceOreNanos.set(0);
    }

    public static String getProfilingSummary() {
        return String.format("terrain=%.1fms decoration=%.1fms gtOres=%.1fms provinceOres=%.1fms",
                terrainNanos.get() / 1_000_000.0, decorationNanos.get() / 1_000_000.0,
                oreNanos.get() / 1_000_000.0, provinceOreNanos.get() / 1_000_000.0);
    }

    private static final double BIOME_REGION_FREQUENCY = 0.0035;
    private volatile SimplexNoise biomeRegionNoise;

    public static void register() {
        Registry.register(BuiltInRegistries.CHUNK_GENERATOR, PhoenixCore.id("discipline"), CODEC);
    }

    public DisciplineChunkGenerator(BiomeSource biomeSource, String disciplineId, long seed) {
        super(biomeSource);
        this.disciplineId = disciplineId;
        this.seed = seed;
        this.terrainProfile = createTerrainForDiscipline(disciplineId, seed);
        this.worldgenProfile = DisciplineWorldgenPresets.getPreset(disciplineId);
    }

    private TerrainProfile resolveTerrainProfile(RandomState randomState) {
        TerrainProfile cached = worldSeededProfile;
        if (cached != null) return cached;

        long derivedSeed = randomState
                .getOrCreateRandomFactory(PhoenixCore.id("discipline_terrain_" + disciplineId))
                .at(0, 0, 0)
                .nextLong();
        TerrainProfile built = createTerrainForDiscipline(disciplineId, derivedSeed);
        worldSeededProfile = built;
        return built;
    }

    private SimplexNoise resolveBiomeRegionNoise(RandomState randomState) {
        SimplexNoise cached = biomeRegionNoise;
        if (cached != null) return cached;

        long derivedSeed = randomState
                .getOrCreateRandomFactory(PhoenixCore.id("discipline_biome_" + disciplineId))
                .at(0, 0, 0)
                .nextLong();
        SimplexNoise built = new SimplexNoise(new WorldgenRandom(new LegacyRandomSource(derivedSeed)));
        biomeRegionNoise = built;
        return built;
    }

    private static final double PRIMARY_BIOME_BIAS = 0.65;

    private static WorldgenProfile.BiomeDefinition selectBiome(List<WorldgenProfile.BiomeDefinition> biomes,
                                                                 String primaryBiomeId,
                                                                 SimplexNoise regionNoise, int x, int z) {
        if (biomes.isEmpty()) return null;
        if (biomes.size() == 1) return biomes.get(0);

        WorldgenProfile.BiomeDefinition best = null;
        double bestScore = -Double.MAX_VALUE;
        for (int i = 0; i < biomes.size(); i++) {
            WorldgenProfile.BiomeDefinition def = biomes.get(i);
            double offsetX = i * 10037.29;
            double offsetZ = i * 7349.13;
            double score = regionNoise.getValue(
                    (x + offsetX) * BIOME_REGION_FREQUENCY, 0, (z + offsetZ) * BIOME_REGION_FREQUENCY);
            if (def.biomeId.equals(primaryBiomeId)) {
                score += PRIMARY_BIOME_BIAS;
            }
            if (score > bestScore) {
                bestScore = score;
                best = def;
            }
        }
        return best;
    }

    private static TerrainProfile createTerrainForDiscipline(String disciplineId, long seed) {

        return switch (disciplineId) {
            case "phoenix" -> TerrainProfile.builder("phoenix")
                    .seed(seed)
                    .baseY(72).amplitude(100).frequency(0.004).octaves(6)
                    .style(TerrainProfile.Style.RIDGED) 
                    .ocean(0.0007, 0.18, 25).river(0.0025, 0.07, 5)
                    .caves(true).build();

            case "sculk" -> TerrainProfile.builder("sculk")
                    .seed(seed).baseY(66).amplitude(45)
                    .frequency(0.0035).octaves(5)
                    .style(TerrainProfile.Style.HEIGHTMAP)
                    .ocean(0.0007, 0.16, 20).river(0.0022, 0.07, 4)
                    .caves(true).build();

            case "void" -> TerrainProfile.builder("void")
                    .seed(seed).baseY(68).amplitude(55)
                    .frequency(0.003).octaves(5)
                    .style(TerrainProfile.Style.HEIGHTMAP)
                    .ocean(0.0006, 0.06, 15).river(0.002, 0.045, 3)
                    .caves(true).build();

            case "sealed_a" -> TerrainProfile.builder("sealed_a")
                    .seed(seed).baseY(68).amplitude(40)
                    .frequency(0.003).octaves(4)
                    .style(TerrainProfile.Style.TERRACED).terraceStep(6.0) 
                    .ocean(0.0007, 0.17, 22).river(0.0024, 0.07, 4)
                    .caves(true).build();

            case "sealed_b" -> TerrainProfile.builder("sealed_b")
                    .seed(seed).baseY(65).amplitude(75)
                    .frequency(0.0032).octaves(6)
                    .style(TerrainProfile.Style.WARPED).warpStrength(20.0) 
                    .ocean(0.0006, 0.07, 15).river(0.002, 0.045, 3)
                    .caves(true).build();

            default -> TerrainProfile.builder("default").seed(seed).caves(true).build();
        };
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types heightmapTypes, LevelHeightAccessor levelHeightAccessor, net.minecraft.world.level.levelgen.RandomState randomState) {
        TerrainProfile profile = resolveTerrainProfile(randomState);
        for (int y = levelHeightAccessor.getMaxBuildHeight() - 1; y >= levelHeightAccessor.getMinBuildHeight(); y--) {
            if (profile.sampler().sample(x, y, z) > 0) {
                return y + 1;
            }
        }
        return levelHeightAccessor.getMinBuildHeight();
    }

    @Override
    public int getMinY() {
        return terrainProfile.minY();
    }

    @Override
    public int getGenDepth() {
        return terrainProfile.maxY() - terrainProfile.minY();
    }

    @Override
    public int getSeaLevel() {
        return terrainProfile.seaLevel();
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState randomState, BiomeManager biomeManager,
                              StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step) {
        
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState randomState, ChunkAccess chunk) {
        long profileStart = System.nanoTime();
        try {
            buildSurface0(level, randomState, chunk);
        } finally {
            terrainNanos.addAndGet(System.nanoTime() - profileStart);
        }
    }

    private void buildSurface0(WorldGenRegion level, RandomState randomState, ChunkAccess chunk) {
        List<WorldgenProfile.BiomeDefinition> biomes = worldgenProfile.biomes.biomes;
        if (biomes.isEmpty()) return;

        SimplexNoise regionNoise = resolveBiomeRegionNoise(randomState);

        net.phoenix.core.common.worldgen.PhoenixTerrainNoise.WaterMask waterMask =
                resolveTerrainProfile(randomState).waterMask();

        ChunkPos chunkPos = chunk.getPos();
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        int minY = getMinY();
        int maxY = minY + getGenDepth();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {

                int topY = minY - 1;
                for (int y = maxY - 1; y >= minY; y--) {
                    if (chunk.getBlockState(pos.set(x, y, z)).is(Blocks.STONE)) {
                        topY = y;
                        break;
                    }
                }
                if (topY < minY) continue;

                WorldgenProfile.BiomeDefinition biome = selectBiome(biomes, worldgenProfile.biomes.primaryBiome, regionNoise, x, z);

                BlockState surface;
                BlockState subSurface;
                if (waterMask != null && waterMask.isWaterColumn(x, z)) {

                    surface = Blocks.SAND.defaultBlockState();
                    subSurface = Blocks.GRAVEL.defaultBlockState();
                } else {
                    surface = resolveBlockState(biome.surfaceBlock, Blocks.STONE);
                    subSurface = resolveBlockState(biome.subSurfaceBlock, Blocks.STONE);
                }

                chunk.setBlockState(pos.set(x, topY, z), surface, false);
                for (int depth = 1; depth <= 3 && topY - depth >= minY; depth++) {
                    chunk.setBlockState(pos.set(x, topY - depth, z), subSurface, false);
                }
            }
        }
    }

    private static BlockState resolveBlockState(@Nullable String id, net.minecraft.world.level.block.Block fallback) {
        if (id == null) return fallback.defaultBlockState();
        net.minecraft.world.level.block.Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .get(net.minecraft.resources.ResourceLocation.parse(id));
        return block != Blocks.AIR ? block.defaultBlockState() : fallback.defaultBlockState();
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
        
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {

        ChunkPos chunkPos = chunk.getPos();

        long t0 = System.nanoTime();
        applyWorldgenFeatures(level, chunkPos.x, chunkPos.z);
        long t1 = System.nanoTime();
        decorationNanos.addAndGet(t1 - t0);

        orePlacer.placeOres(level, this, chunk);
        long t2 = System.nanoTime();
        oreNanos.addAndGet(t2 - t1);

        if ("phoenix".equals(disciplineId) || "void".equals(disciplineId) || "sculk".equals(disciplineId)) {
            placeProvinceOres(level, chunk);
            provinceOreNanos.addAndGet(System.nanoTime() - t2);
        }
    }

    private void placeProvinceOres(WorldGenLevel level, ChunkAccess chunk) {
        List<WorldgenProfile.BiomeDefinition> biomes = worldgenProfile.biomes.biomes;
        SimplexNoise regionNoise = biomeRegionNoise;
        if (biomes.isEmpty() || regionNoise == null) return;

        ChunkPos chunkPos = chunk.getPos();
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        int maxX = minX + 15;
        int maxZ = minZ + 15;

        int minCellX = Math.floorDiv(minX, PROVINCE_CELL_SIZE) - 1;
        int maxCellX = Math.floorDiv(maxX, PROVINCE_CELL_SIZE) + 1;
        int minCellZ = Math.floorDiv(minZ, PROVINCE_CELL_SIZE) - 1;
        int maxCellZ = Math.floorDiv(maxZ, PROVINCE_CELL_SIZE) + 1;

        int worldMinY = getMinY();
        int worldMaxY = worldMinY + getGenDepth();

        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                int finalCellX = cellX;
                int finalCellZ = cellZ;
                ProvinceVeinInstance vein = provinceCellCache.computeIfAbsent(cellKey(cellX, cellZ),
                        k -> generateCellVein(biomes, regionNoise, finalCellX, finalCellZ));
                if (vein == null) continue;

                BlockPos origin = vein.origin();
                int radius = vein.radius();
                if (origin.getX() + radius < minX || origin.getX() - radius > maxX
                        || origin.getZ() + radius < minZ || origin.getZ() - radius > maxZ) {
                    continue;
                }

                placeVeinInChunk(chunk, vein, minX, minZ, worldMinY, worldMaxY);
            }
        }
    }

    private static final float PROVINCE_BASE_DENSITY = 0.85f;

    private static void placeVeinInChunk(ChunkAccess chunk, ProvinceVeinInstance vein,
                                          int minX, int minZ, int worldMinY, int worldMaxY) {
        ProvinceVeinTemplates.Template template = vein.template();
        BlockPos origin = vein.origin();
        int radius = vein.radius();
        double radiusSq = (double) radius * radius;

        int loY = Math.max(worldMinY, Math.max(template.minY(), origin.getY() - radius));
        int hiY = Math.min(worldMaxY - 1, Math.min(template.maxY(), origin.getY() + radius));
        if (loY > hiY) return;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x < minX + 16; x++) {
            double dx = x - origin.getX();
            double dxSq = dx * dx;
            if (dxSq > radiusSq) continue;

            for (int z = minZ; z < minZ + 16; z++) {
                double dz = z - origin.getZ();
                double dxzSq = dxSq + dz * dz;
                if (dxzSq > radiusSq) continue;

                for (int y = loY; y <= hiY; y++) {
                    double dy = y - origin.getY();
                    double distSq = dxzSq + dy * dy;
                    if (distSq > radiusSq) continue;

                    pos.set(x, y, z);
                    BlockState current = chunk.getBlockState(pos);
                    if (!current.is(ProvinceVeinTemplates.STONE_TAG)) continue;

                    long hash = positionHash(vein.seed(), x, y, z);

                    double falloff = 1.0 - Math.sqrt(distSq) / radius;
                    double densityRoll = ((hash >>> 40) & 0xFFFFFF) / (double) 0x1000000;
                    if (densityRoll >= PROVINCE_BASE_DENSITY * falloff) continue;

                    int materialRoll = (int) (((hash & 0xFFFFFF) / (double) 0x1000000) * template.totalWeight());
                    Material material = pickMaterial(template, materialRoll);

                    Optional<TagPrefix> prefix = ChemicalHelper.getOrePrefix(current);
                    if (prefix.isEmpty()) continue;
                    net.minecraft.world.level.block.Block toPlace = ChemicalHelper.getBlock(prefix.get(), material);
                    if (toPlace == null || toPlace.defaultBlockState().isAir()) continue;

                    chunk.setBlockState(pos, toPlace.defaultBlockState(), false);
                }
            }
        }
    }

    private static Material pickMaterial(ProvinceVeinTemplates.Template template, int roll) {
        int cumulative = 0;
        for (ProvinceVeinTemplates.OreLayer layer : template.layers()) {
            cumulative += layer.weight();
            if (roll < cumulative) return layer.material();
        }
        return template.layers().get(template.layers().size() - 1).material();
    }

    private static long positionHash(long seed, int x, int y, int z) {
        long h = seed;
        h = h * 6364136223846793005L + x;
        h = h * 6364136223846793005L + y;
        h = h * 6364136223846793005L + z;
        h ^= (h >>> 33);
        h *= 0xff51afd7ed558ccdL;
        h ^= (h >>> 33);
        return h;
    }

    private static long cellKey(int cellX, int cellZ) {
        return ((long) cellX << 32) ^ (cellZ & 0xFFFFFFFFL);
    }

    @Nullable
    private ProvinceVeinInstance generateCellVein(List<WorldgenProfile.BiomeDefinition> biomes,
                                                    SimplexNoise regionNoise, int cellX, int cellZ) {
        RandomSource cellRandom = new XoroshiroRandomSource(
                seed ^ ((long) cellX * 341873128712L) ^ ((long) cellZ * 132897987541L) ^ 0x50484F454E4958L);

        if (cellRandom.nextFloat() >= PROVINCE_VEIN_CHANCE) return null;

        int originX = cellX * PROVINCE_CELL_SIZE + cellRandom.nextInt(PROVINCE_CELL_SIZE);
        int originZ = cellZ * PROVINCE_CELL_SIZE + cellRandom.nextInt(PROVINCE_CELL_SIZE);

        WorldgenProfile.BiomeDefinition region = selectBiome(
                biomes, worldgenProfile.biomes.primaryBiome, regionNoise, originX, originZ);
        if (region == null || region.oreVeins.isEmpty()) return null;

        String oreId = region.oreVeins.get(cellRandom.nextInt(region.oreVeins.size()));
        ProvinceVeinTemplates.Template template = ProvinceVeinTemplates.get(oreId);
        if (template == null) return null;

        int radius = template.minRadius() + cellRandom.nextInt(template.maxRadius() - template.minRadius() + 1);
        int originY = template.minY() + cellRandom.nextInt(template.maxY() - template.minY() + 1);
        long veinSeed = cellRandom.nextLong();

        return new ProvinceVeinInstance(new BlockPos(originX, originY, originZ), radius, veinSeed, template);
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor levelHeightAccessor, RandomState randomState) {
        TerrainProfile profile = resolveTerrainProfile(randomState);
        int minY = levelHeightAccessor.getMinBuildHeight();
        int height = levelHeightAccessor.getHeight();
        BlockState[] column = new BlockState[height];
        for (int i = 0; i < height; i++) {
            int y = minY + i;
            column[i] = profile.sampler().sample(x, y, z) > 0
                ? Blocks.STONE.defaultBlockState()
                : Blocks.AIR.defaultBlockState();
        }
        return new NoiseColumn(minY, column);
    }

    @Override
    public void addDebugScreenInfo(List<String> list, RandomState randomState, BlockPos pos) {
        list.add("Discipline: " + disciplineId);
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState randomState,
                                                          StructureManager structureManager, ChunkAccess chunk) {
        return CompletableFuture.supplyAsync(() -> {
            long profileStart = System.nanoTime();
            try {
                return fillFromNoise0(randomState, chunk);
            } finally {
                terrainNanos.addAndGet(System.nanoTime() - profileStart);
            }
        }, executor);
    }

    private ChunkAccess fillFromNoise0(RandomState randomState, ChunkAccess chunk) {
        TerrainProfile profile = resolveTerrainProfile(randomState);
        ChunkPos chunkPos = chunk.getPos();
        int minX = chunkPos.getMinBlockX();
            int minZ = chunkPos.getMinBlockZ();
            int minY = getMinY();
            int maxY = minY + getGenDepth();

            net.phoenix.core.common.worldgen.PhoenixTerrainNoise.WaterMask waterMask = profile.waterMask();

            for (int x = minX; x < minX + 16; x++) {
                for (int z = minZ; z < minZ + 16; z++) {
                    for (int y = minY; y < maxY; y++) {
                        if (profile.sampler().sample(x, y, z) > 0) {
                            chunk.setBlockState(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState(), false);
                        }
                    }

                    if (waterMask != null && waterMask.isWaterColumn(x, z)) {

                        int surfaceY = (int) Math.floor(waterMask.waterSurfaceY(x, z));
                        for (int y = surfaceY; y >= minY; y--) {
                            BlockPos pos = new BlockPos(x, y, z);
                            if (!chunk.getBlockState(pos).isAir()) break;
                            chunk.setBlockState(pos, Blocks.WATER.defaultBlockState(), false);
                        }
                    }
                }
            }
        return chunk;
    }

    public void applyWorldgenFeatures(WorldGenLevel level, int chunkX, int chunkZ) {

        WorldgenApplier.applyWorldgenProfile(level, worldgenProfile, null, chunkX, chunkZ);

        applySignatureFeatures(level, chunkX, chunkZ);
    }

    private void applySignatureFeatures(WorldGenLevel level, int chunkX, int chunkZ) {
        switch (disciplineId) {
            case "phoenix" -> applyPhoenixSignature(level, chunkX, chunkZ);
            case "sculk" -> applySculkSignature(level, chunkX, chunkZ);
            case "void" -> applyVoidSignature(level, chunkX, chunkZ);
            case "sealed_a" -> applySealedASignature(level, chunkX, chunkZ);
            case "sealed_b" -> applySealedBSignature(level, chunkX, chunkZ);
        }
    }

    private void applyPhoenixSignature(WorldGenLevel level, int chunkX, int chunkZ) {

    }

    private void applySculkSignature(WorldGenLevel level, int chunkX, int chunkZ) {

    }

    private void applyVoidSignature(WorldGenLevel level, int chunkX, int chunkZ) {

    }

    private void applySealedASignature(WorldGenLevel level, int chunkX, int chunkZ) {

    }

    private void applySealedBSignature(WorldGenLevel level, int chunkX, int chunkZ) {

    }

    public WorldgenProfile getWorldgenProfile() {
        return worldgenProfile;
    }

    public String getDisciplineId() {
        return disciplineId;
    }
}
