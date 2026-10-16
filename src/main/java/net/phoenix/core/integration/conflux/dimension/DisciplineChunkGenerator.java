package net.phoenix.core.integration.conflux.dimension;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.worldgen.TerrainProfile;
import net.phoenix.core.integration.conflux.dimension.worldgen.*;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class DisciplineChunkGenerator extends ChunkGenerator {

    public static final MapCodec<DisciplineChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
                    Codec.STRING.fieldOf("discipline_id").forGetter(gen -> gen.disciplineId),
                    Codec.LONG.fieldOf("seed").forGetter(gen -> gen.seed))
            .apply(instance, instance.stable(DisciplineChunkGenerator::new)));

    private final TerrainProfile terrainProfile;
    private final WorldgenProfile worldgenProfile;
    private final String disciplineId;
    private final long seed;

    private volatile TerrainProfile worldSeededProfile;
    private final net.phoenix.core.integration.conflux.dimension.worldgen.Landforms landforms;

    private final com.gregtechceu.gtceu.api.data.worldgen.ores.OrePlacer orePlacer = new com.gregtechceu.gtceu.api.data.worldgen.ores.OrePlacer();

    private static final int BEDROCK_LAYERS = 5;

    private static final int DEEP_LAVA_TOP = 9;

    private static final int DEEPSLATE_FULL = 0;
    private static final int DEEPSLATE_TOP = 8;

    private static final int PROVINCE_CELL_SIZE = 128;
    private static final float PROVINCE_VEIN_CHANCE = 0.65f;
    private final Map<Long, ProvinceVeinInstance> provinceCellCache = new ConcurrentHashMap<>();

    private record ProvinceVeinInstance(BlockPos origin, int radius, long seed,
                                        ProvinceVeinTemplates.Template template) {}

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

    private static final double BIOME_REGION_FREQUENCY = 0.0016;
    private static final double BIOME_WARP_FREQUENCY = 0.004;
    private static final double BIOME_WARP_STRENGTH = 70.0;
    private static final double BIOME_BLEND_BAND = 0.14;

    private static final double ACCENT_FREQUENCY = 0.03;
    private static final double ACCENT_THRESHOLD = 0.55;
    private volatile SimplexNoise biomeRegionNoise;

    public DisciplineChunkGenerator(BiomeSource biomeSource, String disciplineId, long seed) {
        super(biomeSource);
        this.disciplineId = disciplineId;
        this.seed = seed;
        this.terrainProfile = createTerrainForDiscipline(disciplineId, seed);
        this.worldgenProfile = DisciplineWorldgenPresets.getPreset(disciplineId);
        this.landforms = new net.phoenix.core.integration.conflux.dimension.worldgen.Landforms(disciplineId, seed);
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

    private record BiomePick(WorldgenProfile.BiomeDefinition best, WorldgenProfile.BiomeDefinition second,
                             int bestIndex,
                             double margin) {}

    private static BiomePick pickBiome(List<WorldgenProfile.BiomeDefinition> biomes, String primaryBiomeId,
                                       SimplexNoise regionNoise, int x, int z) {
        if (biomes.isEmpty()) return null;
        if (biomes.size() == 1) return new BiomePick(biomes.get(0), null, 0, 1.0);

        double wx = x + regionNoise.getValue(x * BIOME_WARP_FREQUENCY + 5000.0, 0, z * BIOME_WARP_FREQUENCY) *
                BIOME_WARP_STRENGTH;
        double wz = z + regionNoise.getValue(x * BIOME_WARP_FREQUENCY, 0, z * BIOME_WARP_FREQUENCY + 9000.0) *
                BIOME_WARP_STRENGTH;

        int bestIndex = -1;
        double bestScore = -Double.MAX_VALUE;
        int secondIndex = -1;
        double secondScore = -Double.MAX_VALUE;
        for (int i = 0; i < biomes.size(); i++) {
            WorldgenProfile.BiomeDefinition def = biomes.get(i);
            double offsetX = i * 10037.29;
            double offsetZ = i * 7349.13;
            double score = regionNoise.getValue(
                    (wx + offsetX) * BIOME_REGION_FREQUENCY, 0, (wz + offsetZ) * BIOME_REGION_FREQUENCY);
            if (def.biomeId.equals(primaryBiomeId)) {
                score += PRIMARY_BIOME_BIAS;
            }
            if (score > bestScore) {
                secondScore = bestScore;
                secondIndex = bestIndex;
                bestScore = score;
                bestIndex = i;
            } else if (score > secondScore) {
                secondScore = score;
                secondIndex = i;
            }
        }
        return new BiomePick(biomes.get(bestIndex), secondIndex < 0 ? null : biomes.get(secondIndex), bestIndex,
                bestScore - secondScore);
    }

    private static WorldgenProfile.BiomeDefinition selectBiome(List<WorldgenProfile.BiomeDefinition> biomes,
                                                               String primaryBiomeId,
                                                               SimplexNoise regionNoise, int x, int z) {
        BiomePick pick = pickBiome(biomes, primaryBiomeId, regionNoise, x, z);
        return pick == null ? null : pick.best();
    }

    private WorldgenProfile.BiomeDefinition groundBiome(List<WorldgenProfile.BiomeDefinition> biomes,
                                                        SimplexNoise regionNoise, int x, int z) {
        BiomePick pick = pickBiome(biomes, worldgenProfile.biomes.primaryBiome, regionNoise, x, z);
        if (pick == null) return null;
        WorldgenProfile.BiomeDefinition chosen = pick.best();

        if (pick.second() != null && pick.margin() < BIOME_BLEND_BAND) {
            int dither = (int) ((seed * 31 + x * 73428767L + z * 912931L) >>> 7) & 1023;
            if (dither / 1023.0 < 0.5 * (1.0 - pick.margin() / BIOME_BLEND_BAND)) chosen = pick.second();
        } else if (biomes.size() > 2) {
            double patch = regionNoise.getValue(x * ACCENT_FREQUENCY + 3000.0, 0, z * ACCENT_FREQUENCY + 7000.0);
            if (patch > ACCENT_THRESHOLD) {
                int step = 1 + (int) ((patch - ACCENT_THRESHOLD) * 40.0) % (biomes.size() - 1);
                chosen = biomes.get((pick.bestIndex() + step) % biomes.size());
            }
        }
        return chosen;
    }

    private static TerrainProfile createTerrainForDiscipline(String disciplineId, long seed) {
        return switch (disciplineId) {
            case "phoenix" -> TerrainProfile.builder("phoenix")
                    .seed(seed)
                    .baseY(72).amplitude(100).frequency(0.004).octaves(6)
                    .style(TerrainProfile.Style.RIDGED)
                    .seaLevel(28)
                    .caves(true).build();

            case "sculk" -> TerrainProfile.builder("sculk")
                    .seed(seed).baseY(66).amplitude(45)
                    .frequency(0.0035).octaves(5)
                    .style(TerrainProfile.Style.HEIGHTMAP)
                    .seaLevel(57)
                    .caves(true).build();

            case "void" -> TerrainProfile.builder("void")
                    .seed(seed).baseY(68).amplitude(55)
                    .frequency(0.003).octaves(5)
                    .style(TerrainProfile.Style.HEIGHTMAP)
                    .seaLevel(55)
                    .caves(true).build();

            case "sealed_a" -> TerrainProfile.builder("sealed_a")
                    .seed(seed).baseY(68).amplitude(40)
                    .frequency(0.003).octaves(4)
                    .style(TerrainProfile.Style.TERRACED).terraceStep(6.0)
                    .seaLevel(52)
                    .caves(true).build();

            case "sealed_b" -> TerrainProfile.builder("sealed_b")
                    .seed(seed).baseY(65).amplitude(75)
                    .frequency(0.0032).octaves(6)
                    .style(TerrainProfile.Style.WARPED).warpStrength(20.0)
                    .seaLevel(47)
                    .caves(true).build();

            default -> TerrainProfile.builder("default").seed(seed).caves(true).build();
        };
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types heightmapTypes, LevelHeightAccessor levelHeightAccessor,
                             net.minecraft.world.level.levelgen.RandomState randomState) {
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
                             StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step) {}

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState randomState,
                             ChunkAccess chunk) {
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

                WorldgenProfile.BiomeDefinition biome = groundBiome(biomes, regionNoise, x, z);

                BlockState surface;
                BlockState subSurface;
                if (topY < getSeaLevel()) {

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
    public void spawnOriginalMobs(WorldGenRegion level) {}

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        ChunkPos chunkPos = chunk.getPos();

        long t0 = System.nanoTime();
        shapeLandforms(chunk);
        UndergroundBiomes.apply(chunk, disciplineId, biomeRegionNoise, seed, getMinY(), getMinY() + getGenDepth());
        UnderwaterDecor.apply(chunk, disciplineId, biomeRegionNoise, seed, getMinY(), getMinY() + getGenDepth(),
                getSeaLevel());
        placeStructures(level, chunk, structureManager);
        applyWorldgenFeatures(level, chunkPos.x, chunkPos.z);
        long t1 = System.nanoTime();
        decorationNanos.addAndGet(t1 - t0);

        orePlacer.placeOres(level, this, chunk);
        long t2 = System.nanoTime();
        oreNanos.addAndGet(t2 - t1);

        if (!"default".equals(disciplineId)) {
            placeProvinceOres(level, chunk);
            provinceOreNanos.addAndGet(System.nanoTime() - t2);
        }
    }

    private void placeStructures(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        ChunkPos chunkPos = chunk.getPos();
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        net.minecraft.core.SectionPos section = net.minecraft.core.SectionPos.of(chunkPos, level.getMinSection());
        net.minecraft.world.level.levelgen.structure.BoundingBox writable = new net.minecraft.world.level.levelgen.structure.BoundingBox(
                minX, level.getMinBuildHeight() + 1, minZ, minX + 15, level.getMaxBuildHeight() - 1, minZ + 15);

        WorldgenRandom random = new WorldgenRandom(
                new XoroshiroRandomSource(net.minecraft.world.level.levelgen.RandomSupport.generateUniqueSeed()));
        long decorationSeed = random.setDecorationSeed(level.getSeed(), minX, minZ);

        int index = 0;
        Registry<net.minecraft.world.level.levelgen.structure.Structure> registry = level.registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        for (net.minecraft.world.level.levelgen.structure.Structure structure : registry) {
            if (!(structure instanceof net.phoenix.core.integration.conflux.structure.ConfluxStructure)) continue;
            random.setFeatureSeed(decorationSeed, index++, 0);
            for (net.minecraft.world.level.levelgen.structure.StructureStart start : structureManager
                    .startsForStructure(section, structure)) {
                start.placeInChunk(level, structureManager, this, random, writable, chunkPos);
            }
        }
    }

    public void tickLandformPhysics(net.minecraft.server.level.ServerLevel level) {
        if (level.players().isEmpty() || !landforms.enabled()) return;
        TerrainProfile profile = resolveTerrainProfile(level.getChunkSource().randomState());
        int minY = getMinY();
        int maxY = minY + getGenDepth();
        LandformPhysics.tick(level, landforms, getSeaLevel(), (gx, gz) -> {
            for (int y = maxY - 1; y >= minY; y--) {
                if (profile.sampler().sample(gx, y, gz) > 0) return y + 1;
            }
            return minY;
        });
    }

    public boolean landformNear(int x, int z, int margin, RandomState randomState) {
        TerrainProfile profile = resolveTerrainProfile(randomState);
        int minY = getMinY();
        int maxY = minY + getGenDepth();
        return landforms.occupies(x, z, margin, getSeaLevel(), (gx, gz) -> {
            for (int y = maxY - 1; y >= minY; y--) {
                if (profile.sampler().sample(gx, y, gz) > 0) return y + 1;
            }
            return minY;
        });
    }

    private void shapeLandforms(ChunkAccess chunk) {
        if (!landforms.enabled()) return;
        TerrainProfile profile = worldSeededProfile != null ? worldSeededProfile : terrainProfile;
        int minY = getMinY();
        int maxY = minY + getGenDepth();
        landforms.apply(chunk, minY, maxY, getSeaLevel(), (x, z) -> {
            for (int y = maxY - 1; y >= minY; y--) {
                if (profile.sampler().sample(x, y, z) > 0) return y + 1;
            }
            return minY;
        });
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
                if (origin.getX() + radius < minX || origin.getX() - radius > maxX || origin.getZ() + radius < minZ ||
                        origin.getZ() - radius > maxZ) {
                    continue;
                }

                placeVeinInChunk(chunk, vein, minX, minZ, worldMinY, worldMaxY);
                placeVeinIndicators(chunk, vein, minX, minZ, worldMinY, worldMaxY);
            }
        }
    }

    private static final float PROVINCE_CLUMP_CHANCE = 0.85f;
    private static final int CLUMP_CELL = 5;
    private static final double CLUMP_RADIUS = 2.6;
    private static final double CLUMP_FILL = 0.85;
    private static final double VEIN_SQUASH = 0.6;

    private static void placeVeinInChunk(ChunkAccess chunk, ProvinceVeinInstance vein,
                                         int minX, int minZ, int worldMinY, int worldMaxY) {
        ProvinceVeinTemplates.Template template = vein.template();
        BlockPos origin = vein.origin();
        int radius = vein.radius();
        double radiusSq = (double) radius * radius;

        int vertical = (int) Math.ceil(radius * VEIN_SQUASH);
        int loY = Math.max(worldMinY, Math.max(template.minY(), origin.getY() - vertical));
        int hiY = Math.min(worldMaxY - 1, Math.min(template.maxY(), origin.getY() + vertical));
        if (loY > hiY) return;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        double clumpRadiusSq = CLUMP_RADIUS * CLUMP_RADIUS;
        for (int x = minX; x < minX + 16; x++) {
            double dx = x - origin.getX();
            double dxSq = dx * dx;
            if (dxSq > radiusSq) continue;

            for (int z = minZ; z < minZ + 16; z++) {
                double dz = z - origin.getZ();
                double dxzSq = dxSq + dz * dz;
                if (dxzSq > radiusSq) continue;

                for (int y = loY; y <= hiY; y++) {
                    double dy = (y - origin.getY()) / VEIN_SQUASH;
                    double distSq = dxzSq + dy * dy;
                    if (distSq > radiusSq) continue;

                    pos.set(x, y, z);
                    BlockState current = chunk.getBlockState(pos);
                    if (!current.is(ProvinceVeinTemplates.STONE_TAG) &&
                            !current.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES))
                        continue;

                    int cellX = Math.floorDiv(x, CLUMP_CELL);
                    int cellY = Math.floorDiv(y, CLUMP_CELL);
                    int cellZ = Math.floorDiv(z, CLUMP_CELL);
                    long cellHash = positionHash(vein.seed() ^ 0x5DEECE66DL, cellX, cellY, cellZ);

                    double falloff = 1.0 - Math.sqrt(distSq) / radius;
                    double clumpRoll = ((cellHash >>> 40) & 0xFFFFFF) / (double) 0x1000000;
                    if (clumpRoll >= PROVINCE_CLUMP_CHANCE * falloff) continue;

                    double cx = cellX * CLUMP_CELL + ((cellHash & 0xFF) / 255.0) * CLUMP_CELL;
                    double cy = cellY * CLUMP_CELL + (((cellHash >>> 8) & 0xFF) / 255.0) * CLUMP_CELL;
                    double cz = cellZ * CLUMP_CELL + (((cellHash >>> 16) & 0xFF) / 255.0) * CLUMP_CELL;
                    double bx = x + 0.5 - cx;
                    double by = y + 0.5 - cy;
                    double bz = z + 0.5 - cz;
                    if (bx * bx + by * by + bz * bz > clumpRadiusSq) continue;

                    long hash = positionHash(vein.seed(), x, y, z);
                    if (((hash >>> 40) & 0xFFFFFF) / (double) 0x1000000 >= CLUMP_FILL) continue;

                    int materialRoll = (int) (((cellHash >>> 24) & 0xFFFFFF) / (double) 0x1000000 *
                            template.totalWeight());
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

    private static final int SHAPE_ROUND = 0;
    private static final int SHAPE_RING = 1;
    private static final int SHAPE_STREAK = 2;
    private static final int SHAPE_CLUMPS = 3;
    private static final int SHAPE_ROWS = 4;

    private record GardenTheme(BlockState bed, BlockState bedAlt, BlockState filler, BlockState filler2,
                               BlockState accent, int accentOneIn, int shape, double sizeMul, int extraGardens) {}

    private GardenTheme gardenTheme() {
        return switch (disciplineId) {

            case "phoenix" -> new GardenTheme(Blocks.BASALT.defaultBlockState(), Blocks.COARSE_DIRT.defaultBlockState(),
                    Blocks.DEAD_BUSH.defaultBlockState(), Blocks.CRIMSON_ROOTS.defaultBlockState(),
                    Blocks.MAGMA_BLOCK.defaultBlockState(), 40, SHAPE_STREAK, 1.1, 0);

            case "sculk" -> new GardenTheme(Blocks.SCULK.defaultBlockState(), Blocks.MOSS_BLOCK.defaultBlockState(),
                    Blocks.MOSS_CARPET.defaultBlockState(), Blocks.FLOWERING_AZALEA.defaultBlockState(),
                    Blocks.VERDANT_FROGLIGHT.defaultBlockState(), 60, SHAPE_RING, 1.0, 0);

            case "void" -> new GardenTheme(Blocks.END_STONE.defaultBlockState(),
                    Blocks.PURPUR_BLOCK.defaultBlockState(),
                    Blocks.CHORUS_FLOWER.defaultBlockState(), Blocks.CHORUS_PLANT.defaultBlockState(),
                    Blocks.AMETHYST_BLOCK.defaultBlockState(), 25, SHAPE_CLUMPS, 0.8, 3);

            case "sealed_a" -> new GardenTheme(Blocks.PODZOL.defaultBlockState(),
                    Blocks.ROOTED_DIRT.defaultBlockState(),
                    Blocks.CORNFLOWER.defaultBlockState(), Blocks.ALLIUM.defaultBlockState(),
                    Blocks.LANTERN.defaultBlockState(), 70, SHAPE_ROWS, 0.9, 1);

            case "sealed_b" -> new GardenTheme(Blocks.WARPED_NYLIUM.defaultBlockState(),
                    Blocks.NETHERRACK.defaultBlockState(), Blocks.WARPED_ROOTS.defaultBlockState(),
                    Blocks.NETHER_SPROUTS.defaultBlockState(), Blocks.SHROOMLIGHT.defaultBlockState(), 45,
                    SHAPE_ROUND, 1.0, 0);
            default -> new GardenTheme(Blocks.MOSS_BLOCK.defaultBlockState(), Blocks.MOSS_BLOCK.defaultBlockState(),
                    Blocks.SHORT_GRASS.defaultBlockState(), Blocks.SHORT_GRASS.defaultBlockState(),
                    Blocks.MOSS_BLOCK.defaultBlockState(), 1000, SHAPE_ROUND, 1.0, 0);
        };
    }

    private void placeVeinIndicators(ChunkAccess chunk, ProvinceVeinInstance vein, int minX, int minZ,
                                     int worldMinY, int worldMaxY) {
        ProvinceVeinTemplates.Template template = vein.template();
        BlockPos origin = vein.origin();
        int radius = vein.radius();
        int vertical = (int) Math.ceil(radius * VEIN_SQUASH);
        GardenTheme theme = gardenTheme();

        int gardens = 3 + (int) Math.floorMod(vein.seed() >>> 9, 4L) + theme.extraGardens();
        double[] gx = new double[gardens];
        double[] gz = new double[gardens];
        double[] gr = new double[gardens];
        double[] gAngle = new double[gardens];
        Material[] gMaterial = new Material[gardens];
        for (int g = 0; g < gardens; g++) {
            long h = positionHash(vein.seed() ^ 0x6A4DE11L, g, 7, g * 31);
            double angle = ((h & 0xFFFF) / 65535.0) * Math.PI * 2.0;
            double dist = (((h >>> 16) & 0xFFFF) / 65535.0) * radius * 0.7;
            gx[g] = origin.getX() + Math.cos(angle) * dist;
            gz[g] = origin.getZ() + Math.sin(angle) * dist;
            gr[g] = (5.0 + ((h >>> 32) & 0xFF) / 255.0 * 4.0) * theme.sizeMul();
            gAngle[g] = (((h >>> 8) & 0xFFFF) / 65535.0) * Math.PI;
            gMaterial[g] = pickMaterial(template, (int) (((h >>> 40) & 0xFFFFFF) / (double) 0x1000000 *
                    template.totalWeight()));
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                int garden = -1;
                double gardenDist = 0.0;
                for (int g = 0; g < gardens; g++) {
                    double dx = x - gx[g];
                    double dz = z - gz[g];
                    double d;
                    if (theme.shape() == SHAPE_STREAK) {

                        double u = dx * Math.cos(gAngle[g]) + dz * Math.sin(gAngle[g]);
                        double v = -dx * Math.sin(gAngle[g]) + dz * Math.cos(gAngle[g]);
                        d = Math.hypot(u / 1.7, v / 0.55);
                    } else {
                        d = Math.hypot(dx, dz);
                    }
                    if (d <= gr[g]) {
                        garden = g;
                        gardenDist = d;
                        break;
                    }
                }
                if (garden < 0) continue;

                int ground = worldMinY;
                boolean found = false;
                for (int y = worldMaxY - 2; y > worldMinY; y--) {
                    BlockState state = chunk.getBlockState(pos.set(x, y, z));
                    if (state.isAir() || state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) ||
                            state.canBeReplaced() && state.getFluidState().isEmpty()) {
                        continue;
                    }
                    ground = y;
                    found = state.getFluidState().isEmpty();
                    break;
                }
                if (!found) continue;

                double dxo = x - origin.getX();
                double dzo = z - origin.getZ();
                double flat = Math.sqrt(dxo * dxo + dzo * dzo);
                double reach = vertical * Math.sqrt(Math.max(0.0, 1.0 - (flat * flat) / ((double) radius * radius)));
                int veinTop = Math.min(template.maxY(), origin.getY() + (int) reach);
                double scale = Math.max(0.35, Math.min(1.0, 1.0 - Math.max(0, ground - veinTop) / 80.0));
                double reachHere = gr[garden] * scale;
                if (gardenDist > reachHere) continue;
                double falloff = 1.0 - gardenDist / reachHere;

                boolean hollow = theme.shape() == SHAPE_RING && falloff > 0.62;

                BlockState above = chunk.getBlockState(pos.set(x, ground + 1, z));
                if (!above.isAir() && !above.canBeReplaced()) continue;

                long h = positionHash(vein.seed() ^ 0x6A4DE12L, x, 0, z);
                double roll = ((h >>> 24) & 0xFFFFFF) / (double) 0x1000000;
                double density = (0.20 + 0.55 * falloff) * (0.5 + 0.5 * scale);

                if (falloff > 0.3 && (h & 3) != 0) {
                    chunk.setBlockState(pos.set(x, ground, z), (h & 16) == 0 ? theme.bed() : theme.bedAlt(), false);
                }

                if (falloff > 0.5 && Math.floorMod(h >>> 5, theme.accentOneIn()) == 0) {
                    chunk.setBlockState(pos.set(x, ground + 1, z), theme.accent(), false);
                    continue;
                }
                if (hollow) continue;

                if (theme.shape() == SHAPE_CLUMPS &&
                        ((positionHash(vein.seed(), Math.floorDiv(x, 3), 1, Math.floorDiv(z, 3)) >>> 40) & 3) == 0) {
                    continue;
                }
                if (theme.shape() == SHAPE_ROWS && Math.floorMod(x, 3) != 0) continue;

                net.minecraft.world.level.block.Block plant;
                if (roll < density * 0.60) {
                    plant = roseOf(gMaterial[garden]);
                } else if (roll < density * 0.85) {
                    Material other = pickMaterial(template, (int) (((h >>> 48) & 0xFFFF) / 65536.0 *
                            template.totalWeight()));
                    plant = roseOf(other);
                } else if (roll < density) {
                    chunk.setBlockState(pos.set(x, ground + 1, z), (h & 32) == 0 ? theme.filler() : theme.filler2(),
                            false);
                    continue;
                } else {
                    continue;
                }
                chunk.setBlockState(pos.set(x, ground + 1, z), plant.defaultBlockState(), false);
            }
        }
    }

    private static net.minecraft.world.level.block.Block roseOf(Material material) {
        net.minecraft.world.level.block.Block rose = ChemicalHelper.getBlock(
                net.phoenix.core.common.data.materials.PhoenixMaterialFlags.crystal_rose, material);
        if (rose == null || rose.defaultBlockState().isAir()) {
            rose = net.phoenix.core.common.block.PhoenixBlocks.CRYSTAL_ROSE.get();
        }
        return rose;
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

        boolean nearOrigin = cellX >= -1 && cellX <= 0 && cellZ >= -1 && cellZ <= 0;
        float roll = cellRandom.nextFloat();
        if (!nearOrigin && roll >= PROVINCE_VEIN_CHANCE) return null;

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
            column[i] = profile.sampler().sample(x, y, z) > 0 ? Blocks.STONE.defaultBlockState() :
                    Blocks.AIR.defaultBlockState();
        }
        return new NoiseColumn(minY, column);
    }

    @Override
    public void addDebugScreenInfo(List<String> list, RandomState randomState, BlockPos pos) {
        list.add("Discipline: " + disciplineId);
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState,
                                                        StructureManager structureManager, ChunkAccess chunk) {
        Executor executor = net.minecraft.Util.backgroundExecutor();
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

        net.minecraft.world.level.block.state.BlockState stone = Blocks.STONE.defaultBlockState();
        net.minecraft.world.level.block.state.BlockState deepslateState = Blocks.DEEPSLATE.defaultBlockState();
        net.minecraft.world.level.block.state.BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
        net.minecraft.world.level.block.state.BlockState lava = Blocks.LAVA.defaultBlockState();
        net.minecraft.world.level.block.state.BlockState water = Blocks.WATER.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        net.minecraft.world.level.chunk.LevelChunkSection[] sections = chunk.getSections();
        int seaLevel = profile.seaLevel();
        long rollSeed = seed ^ 0xDEE95L;

        final int cell = 4;
        int cellsY = (maxY - minY + cell - 1) / cell;
        int gridY = cellsY + 1;
        double[] grid = new double[5 * 5 * gridY];
        for (int gx = 0; gx < 5; gx++) {
            for (int gz = 0; gz < 5; gz++) {
                int base = (gx * 5 + gz) * gridY;
                for (int gy = 0; gy < gridY; gy++) {
                    grid[base + gy] = profile.sampler().sample(minX + gx * cell, Math.min(minY + gy * cell, maxY - 1),
                            minZ + gz * cell);
                }
            }
        }

        for (int x = minX; x < minX + 16; x++) {
            double fx = (x - minX) / (double) cell;
            int cx = (int) fx;
            fx -= cx;
            for (int z = minZ; z < minZ + 16; z++) {
                double fz = (z - minZ) / (double) cell;
                int cz = (int) fz;
                fz -= cz;
                int b00 = (cx * 5 + cz) * gridY;
                int b01 = (cx * 5 + cz + 1) * gridY;
                int b10 = ((cx + 1) * 5 + cz) * gridY;
                int b11 = ((cx + 1) * 5 + cz + 1) * gridY;
                int ground = minY - 1;
                for (int y = minY; y < maxY; y++) {
                    int gy = (y - minY) / cell;
                    double fy = (y - minY) / (double) cell - gy;
                    int g1 = Math.min(gy + 1, gridY - 1);
                    double d00 = grid[b00 + gy] + (grid[b00 + g1] - grid[b00 + gy]) * fy;
                    double d01 = grid[b01 + gy] + (grid[b01 + g1] - grid[b01 + gy]) * fy;
                    double d10 = grid[b10 + gy] + (grid[b10 + g1] - grid[b10 + gy]) * fy;
                    double d11 = grid[b11 + gy] + (grid[b11 + g1] - grid[b11 + gy]) * fy;
                    double d0 = d00 + (d01 - d00) * fz;
                    double d1 = d10 + (d11 - d10) * fz;
                    boolean solid = d0 + (d1 - d0) * fx > 0;
                    net.minecraft.world.level.block.state.BlockState state;

                    if (y == minY) {
                        state = bedrock;
                    } else if (!solid) {
                        if (y > minY + DEEP_LAVA_TOP) continue;
                        state = lava;
                    } else if (y < minY + BEDROCK_LAYERS || y < DEEPSLATE_TOP) {
                        double roll = ((positionHash(rollSeed, x, y, z) >>> 24) & 0xFFFFFF) / (double) 0x1000000;
                        if (y < minY + BEDROCK_LAYERS && roll < (minY + BEDROCK_LAYERS - y) / (double) BEDROCK_LAYERS) {
                            state = bedrock;
                        } else {
                            boolean deepslate = y < DEEPSLATE_FULL ||
                                    (y < DEEPSLATE_TOP &&
                                            roll < (DEEPSLATE_TOP - y) / (double) (DEEPSLATE_TOP - DEEPSLATE_FULL));
                            state = deepslate ? deepslateState : stone;
                        }
                    } else {
                        state = stone;
                    }

                    if (state == lava) {

                        chunk.setBlockState(pos.set(x, y, z), state, false);
                    } else {
                        sections[chunk.getSectionIndex(y)].setBlockState(x & 15, y & 15, z & 15, state, false);
                    }
                    ground = y;
                }

                for (int y = Math.min(seaLevel, maxY - 1); y > ground; y--) {
                    sections[chunk.getSectionIndex(y)].setBlockState(x & 15, y & 15, z & 15, water, false);
                }
            }
        }
        net.minecraft.world.level.levelgen.Heightmap.primeHeightmaps(chunk, java.util.EnumSet.of(
                net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG,
                net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG));
        return chunk;
    }

    public void applyWorldgenFeatures(WorldGenLevel level, int chunkX, int chunkZ) {
        SimplexNoise regionNoise = biomeRegionNoise;
        List<WorldgenProfile.BiomeDefinition> biomes = worldgenProfile.biomes.biomes;
        WorldgenApplier.BiomeLookup lookup = regionNoise == null || biomes.isEmpty() ? null :
                new WorldgenApplier.BiomeLookup() {

                    @Override
                    public String biomeIdAt(int x, int z) {
                        WorldgenProfile.BiomeDefinition def = groundBiome(biomes, regionNoise, x, z);
                        return def == null ? null : def.biomeId;
                    }

                    @Override
                    public net.minecraft.world.level.block.Block groundBlockAt(int x, int z) {
                        WorldgenProfile.BiomeDefinition def = groundBiome(biomes, regionNoise, x, z);
                        return def == null ? null : resolveBlockState(def.surfaceBlock, Blocks.STONE).getBlock();
                    }
                };
        WorldgenApplier.applyWorldgenProfile(level, worldgenProfile, null, chunkX, chunkZ, lookup);

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

    private void applyPhoenixSignature(WorldGenLevel level, int chunkX, int chunkZ) {}

    private void applySculkSignature(WorldGenLevel level, int chunkX, int chunkZ) {}

    private void applyVoidSignature(WorldGenLevel level, int chunkX, int chunkZ) {}

    private void applySealedASignature(WorldGenLevel level, int chunkX, int chunkZ) {}

    private void applySealedBSignature(WorldGenLevel level, int chunkX, int chunkZ) {}

    public WorldgenProfile getWorldgenProfile() {
        return worldgenProfile;
    }

    public String getDisciplineId() {
        return disciplineId;
    }
}
