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
import net.phoenix.core.integration.conflux.dimension.worldgen.VeinPlan;

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

    private static final int PROVINCE_CELL_SIZE = 96;
    private static final float PROVINCE_VEIN_CHANCE = 0.85f;

    public static final double SAME_KIND_GAP = 200.0;
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

    private static java.util.List<net.phoenix.core.common.worldgen.TerrainRegions.Spec> sculkRegions() {
        return java.util.List.of(

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("sculk_forest",
                        TerrainProfile.Style.HEIGHTMAP, 68, 40, 0.004, 5, 0, 0.3),

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("sculk_ridge", TerrainProfile.Style.RIDGED,
                        86, 82, 0.006, 5, 0, 0.0),

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("sculk_depths", TerrainProfile.Style.WARPED,
                        56, 32, 0.005, 4, 20.0, 0.05),

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("sculk_shriek",
                        TerrainProfile.Style.HEIGHTMAP, 74, 58, 0.0055, 5, 0, 0.0),

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("sculk_hollow",
                        TerrainProfile.Style.HEIGHTMAP, 63, 28, 0.0045, 4, 0, 0.05),

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("sculk_marsh",
                        TerrainProfile.Style.HEIGHTMAP, 56, 10, 0.003, 3, 0, 0.1),

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("sculk_thicket", TerrainProfile.Style.WARPED,
                        71, 38, 0.0048, 4, 18.0, 0.0));
    }

    private static java.util.List<net.phoenix.core.common.worldgen.TerrainRegions.Spec> phoenixRegions() {
        return java.util.List.of(

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("phoenix_plains",
                        TerrainProfile.Style.HEIGHTMAP, 70, 36, 0.004, 5, 0, 0.3),

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("phoenix_peaks", TerrainProfile.Style.RIDGED,
                        96, 112, 0.005, 6, 0, 0.0),

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("phoenix_crimson",
                        TerrainProfile.Style.TERRACED, 78, 40, 0.0035, 4, 9.0, 0.05),

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("phoenix_ashlands",
                        TerrainProfile.Style.HEIGHTMAP, 60, 28, 0.0035, 4, 0, 0.15),

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("phoenix_sulfur_fields",
                        TerrainProfile.Style.WARPED, 68, 34, 0.005, 4, 24.0, 0.0),

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("phoenix_basalt_flats",
                        TerrainProfile.Style.TERRACED, 58, 12, 0.003, 3, 4.0, 0.05),

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("phoenix_scorched_grove",
                        TerrainProfile.Style.HEIGHTMAP, 76, 36, 0.0045, 5, 0, 0.0));
    }

    private static java.util.List<net.phoenix.core.common.worldgen.TerrainRegions.Spec> voidRegions() {
        return java.util.List.of(

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("void_island", TerrainProfile.Style.HEIGHTMAP,
                        70, 38, 0.004, 5, 0, 0.45),

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("void_crystal_field",
                        TerrainProfile.Style.RIDGED, 76, 78, 0.006, 5, 0, 0.0),

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("void_amethyst_grove",
                        TerrainProfile.Style.WARPED, 64, 42, 0.005, 4, 26.0, 0.0),

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("void_starlit_meadow",
                        TerrainProfile.Style.TERRACED, 88, 32, 0.003, 4, 7.0, 0.0),

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("void_lavender_fields",
                        TerrainProfile.Style.HEIGHTMAP, 60, 9, 0.003, 3, 0, 0.1),

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("void_drift", TerrainProfile.Style.RIDGED,
                        68, 34, 0.008, 4, 0, -0.05));
    }

    public String regionAt(RandomState randomState, int x, int z) {
        TerrainProfile profile = resolveTerrainProfile(randomState);
        return profile.regions() == null ? null : profile.regions().biomeAt(x, z);
    }

    public double chasmMargin(RandomState randomState, int x, int z) {
        var mask = resolveTerrainProfile(randomState).chasms();
        return mask == null ? Double.NaN : mask.margin(x, z);
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

    private WorldgenProfile.BiomeDefinition regionBiome(List<WorldgenProfile.BiomeDefinition> biomes, int x, int z) {
        TerrainProfile profile = worldSeededProfile != null ? worldSeededProfile : terrainProfile;
        if (profile.regions() == null) return null;
        String id = profile.regions().biomeAt(x, z);
        for (WorldgenProfile.BiomeDefinition def : biomes) {
            if (def.biomeId.equals(id)) return def;
        }
        return null;
    }

    private WorldgenProfile.BiomeDefinition groundBiome(List<WorldgenProfile.BiomeDefinition> biomes,
                                                        SimplexNoise regionNoise, int x, int z) {
        WorldgenProfile.BiomeDefinition ofTerrain = regionBiome(biomes, x, z);
        if (ofTerrain != null) return ofTerrain;
        BiomePick pick = pickBiome(biomes, worldgenProfile.biomes.primaryBiome, regionNoise, x, z);
        if (pick == null) return null;
        WorldgenProfile.BiomeDefinition chosen = pick.best();

        if (pick.second() != null && pick.margin() < BIOME_BLEND_BAND) {

            long hh = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL);
            hh ^= hh >>> 33;
            hh *= 0xFF51AFD7ED558CCDL;
            hh ^= hh >>> 33;
            hh *= 0xC4CEB9FE1A85EC53L;
            hh ^= hh >>> 33;
            int dither = (int) (hh & 1023);
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
                    .flatShare(0.40)
                    .seaLevel(34)
                    .regions(phoenixRegions())
                    .basins(0.0032, 0.85, 14)
                    .caves(true).build();

            case "sculk" -> TerrainProfile.builder("sculk")
                    .seed(seed).baseY(66).amplitude(45)
                    .frequency(0.0035).octaves(5)
                    .style(TerrainProfile.Style.HEIGHTMAP)
                    .seaLevel(57)
                    .regions(sculkRegions())
                    .caves(true).build();

            case "void" -> TerrainProfile.builder("void")
                    .seed(seed).baseY(68).amplitude(55)
                    .frequency(0.003).octaves(5)
                    .style(TerrainProfile.Style.HEIGHTMAP)
                    .seaLevel(55)
                    .regions(voidRegions())
                    .chasms(0.0075, 0.53)
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
        return groundHeight(profile, x, z, levelHeightAccessor.getMinBuildHeight(),
                levelHeightAccessor.getMaxBuildHeight());
    }

    private static final int GROUND_CACHE_BITS = 19;
    private static final int GROUND_CACHE_MASK = (1 << GROUND_CACHE_BITS) - 1;
    private final java.util.concurrent.atomic.AtomicLongArray groundCache = new java.util.concurrent.atomic.AtomicLongArray(
            1 << GROUND_CACHE_BITS);
    private static final int GROUND_COARSE = 4;

    private static final long COORD_MASK = (1L << 26) - 1;
    private static final long HEIGHT_MASK = (1L << 12) - 1;

    private static int groundSlot(int x, int z) {
        long h = (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL);
        h ^= h >>> 32;
        h *= 0xD6E8FEB86659FD93L;
        h ^= h >>> 29;
        return (int) h & GROUND_CACHE_MASK;
    }

    private int groundHeight(TerrainProfile profile, int x, int z, int minY, int maxY) {
        int span = maxY - minY;
        boolean cacheable = span > 0 && span < HEIGHT_MASK && Math.abs(x) < (1 << 25) && Math.abs(z) < (1 << 25);
        int slot = 0;
        long tag = 0;
        if (cacheable) {
            slot = groundSlot(x, z);
            tag = ((x & COORD_MASK) << 38) | ((z & COORD_MASK) << 12);
            long entry = groundCache.get(slot);
            if (entry != 0 && (entry & ~HEIGHT_MASK) == tag) return minY + (int) ((entry & HEIGHT_MASK) - 1);
        }

        int top = maxY - 1;
        int start = Math.min(top, profile.groundCeiling());
        if (start < top && profile.sampler().sample(x, start, z) > 0) {

            start = top;
        }

        int result = minY;
        for (int y = start; y >= minY; y -= GROUND_COARSE) {
            if (profile.sampler().sample(x, y, z) > 0) {
                result = y + 1;
                for (int above = Math.min(top, y + GROUND_COARSE - 1); above > y; above--) {
                    if (profile.sampler().sample(x, above, z) > 0) {
                        result = above + 1;
                        break;
                    }
                }
                break;
            }
        }
        if (result == minY && profile.sampler().sample(x, minY, z) > 0) result = minY + 1;

        if (cacheable) {

            groundCache.set(slot, tag | (long) (result - minY + 1));
        }
        return result;
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
                int scanFrom = Math.min(maxY - 1,
                        chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, x, z) + 1);
                for (int y = scanFrom; y >= minY; y--) {
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

                    boolean fire = disciplineId.equals("phoenix");
                    surface = (fire ? Blocks.BASALT : Blocks.SAND).defaultBlockState();
                    subSurface = (fire ? Blocks.BLACKSTONE : Blocks.GRAVEL).defaultBlockState();
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
        shapeLandforms(chunk, level.getLevel().getChunkSource().randomState());
        carveChasmCracks(chunk, level.getLevel().getChunkSource().randomState());
        glowCracks(chunk, level.getLevel().getChunkSource().randomState());
        sculkNetwork(chunk, level.getLevel().getChunkSource().randomState());
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
        LandformPhysics.tick(level, landforms, getSeaLevel(), (gx, gz) -> groundHeight(profile, gx, gz, minY, maxY));
    }

    public java.util.List<net.phoenix.core.integration.conflux.dimension.worldgen.Landforms.Sighting> landformSightingsNear(
                                                                                                                            net.minecraft.server.level.ServerLevel level,
                                                                                                                            int x,
                                                                                                                            int z,
                                                                                                                            int margin) {
        if (!landforms.enabled()) return java.util.List.of();
        TerrainProfile profile = resolveTerrainProfile(level.getChunkSource().randomState());
        int minY = getMinY();
        int maxY = minY + getGenDepth();
        return landforms.sightingsNear(x, z, margin, getSeaLevel(),
                (gx, gz) -> groundHeight(profile, gx, gz, minY, maxY));
    }

    public boolean landformNear(int x, int z, int margin, RandomState randomState) {
        TerrainProfile profile = resolveTerrainProfile(randomState);
        int minY = getMinY();
        int maxY = minY + getGenDepth();
        return landforms.occupies(x, z, margin, getSeaLevel(), (gx, gz) -> groundHeight(profile, gx, gz, minY, maxY));
    }

    private void sculkNetwork(ChunkAccess chunk, RandomState randomState) {
        if (!disciplineId.equals("sculk")) return;
        SimplexNoise noise = resolveBiomeRegionNoise(randomState);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int sea = getSeaLevel();
        BlockState sculk = Blocks.SCULK.defaultBlockState();
        BlockState vein = Blocks.SCULK_VEIN.defaultBlockState()
                .setValue(
                        net.minecraft.world.level.block.MultifaceBlock
                                .getFaceProperty(net.minecraft.core.Direction.DOWN),
                        true);
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                double wx = noise.getValue(x * 0.018 + 90.0, 0, z * 0.018) * 14.0;
                double wz = noise.getValue(x * 0.018, 0, z * 0.018 - 90.0) * 14.0;
                double main = Math.abs(noise.getValue((x + wx) * 0.012 + 2100.0, 0, (z + wz) * 0.012 - 1500.0));
                double branch = Math.abs(noise.getValue((x - wz) * 0.03 - 700.0, 0, (z + wx) * 0.03 + 300.0));
                double infected = noise.getValue(x * 0.006 - 5200.0, 0, z * 0.006 + 3100.0);

                boolean core = main < 0.028 || (branch < 0.02 && main < 0.2);
                boolean fringe = !core && (main < 0.06 || (branch < 0.04 && main < 0.2));
                long h = (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL);
                h ^= h >>> 29;
                h *= 0xBF58476D1CE4E5B9L;
                h ^= h >>> 32;

                double rim = 0.38 + (((h >>> 3) & 255) / 255.0 - 0.5) * 0.08;
                boolean patch = infected > rim && (infected > rim + 0.1 || (h & 3) != 0);
                if (!core && !fringe && !patch) continue;

                int top = chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
                if (top <= sea) continue;
                BlockState here = chunk.getBlockState(pos.set(x, top, z));
                if (here.isAir() || !here.getFluidState().isEmpty()) continue;

                if (core || patch) {
                    chunk.setBlockState(pos, sculk, false);
                    if (core && (h >>> 8 & 63) == 0) {
                        int height = 3 + (int) (h >>> 20 & 7);
                        for (int k = 1; k <= height; k++) {
                            chunk.setBlockState(pos.set(x, top + k, z), sculk, false);
                        }
                    }
                } else {

                    long pick = h >>> 40 & 7;
                    if (pick < 2) chunk.setBlockState(pos, Blocks.DEEPSLATE.defaultBlockState(), false);
                    else if (pick < 4) chunk.setBlockState(pos, Blocks.TUFF.defaultBlockState(), false);
                    if (chunk.getBlockState(pos.set(x, top + 1, z)).isAir()) {
                        chunk.setBlockState(pos, vein, false);
                    }
                }
            }
        }
    }

    private void glowCracks(ChunkAccess chunk, RandomState randomState) {
        if (!disciplineId.equals("phoenix")) return;
        SimplexNoise noise = resolveBiomeRegionNoise(randomState);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int sea = getSeaLevel();
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                double wx = noise.getValue(x * 0.02 + 40.0, 0, z * 0.02) * 9.0;
                double wz = noise.getValue(x * 0.02, 0, z * 0.02 - 40.0) * 9.0;
                double line = Math.abs(noise.getValue((x + wx) * 0.028 + 1300.0, 0, (z + wz) * 0.028 - 800.0));
                if (line > 0.02) continue;
                int top = chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
                if (top <= sea) continue;
                net.minecraft.world.level.block.state.BlockState here = chunk.getBlockState(pos.set(x, top, z));
                if (here.isAir() || !here.getFluidState().isEmpty()) continue;
                String biome = regionBiomeId(x, z);
                if (biome != null && (biome.equals("phoenix_plains") || biome.equals("phoenix_scorched_grove")))
                    continue;
                chunk.setBlockState(pos, Blocks.MAGMA_BLOCK.defaultBlockState(), false);
            }
        }
    }

    private String regionBiomeId(int x, int z) {
        TerrainProfile profile = worldSeededProfile != null ? worldSeededProfile : terrainProfile;
        return profile.regions() == null ? null : profile.regions().biomeAt(x, z);
    }

    private void carveChasmCracks(ChunkAccess chunk, RandomState randomState) {
        TerrainProfile profile = resolveTerrainProfile(randomState);
        var mask = profile.chasms();
        if (mask == null) return;
        int minY = getMinY();
        int maxY = minY + getGenDepth();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                int depth = mask.crackDepth(x, z);
                if (depth <= 0) continue;
                int top = maxY - 1;
                while (top > minY && (chunk.getBlockState(pos.set(x, top, z)).isAir() ||
                        !chunk.getBlockState(pos).getFluidState().isEmpty())) {
                    top--;
                }
                if (top <= minY + depth + 2) continue;
                for (int y = top; y > top - depth && y > minY + 1; y--) {
                    chunk.setBlockState(pos.set(x, y, z), Blocks.AIR.defaultBlockState(), false);
                }
            }
        }
    }

    private void shapeLandforms(ChunkAccess chunk, RandomState randomState) {
        if (!landforms.enabled()) return;

        TerrainProfile profile = resolveTerrainProfile(randomState);
        int minY = getMinY();
        int maxY = minY + getGenDepth();
        landforms.apply(chunk, minY, maxY, getSeaLevel(), (x, z) -> groundHeight(profile, x, z, minY, maxY));
    }

    private void placeProvinceOres(WorldGenLevel level, ChunkAccess chunk) {
        String dimensionKey = level.getLevel().dimension().location().toString();
        List<WorldgenProfile.BiomeDefinition> biomes = worldgenProfile.biomes.biomes;
        SimplexNoise regionNoise = biomeRegionNoise;
        if (biomes.isEmpty() || regionNoise == null) return;

        ChunkPos chunkPos = chunk.getPos();
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        int maxX = minX + 15;
        int maxZ = minZ + 15;

        int minCellX = Math.floorDiv(minX, PROVINCE_CELL_SIZE) - 2;
        int maxCellX = Math.floorDiv(maxX, PROVINCE_CELL_SIZE) + 2;
        int minCellZ = Math.floorDiv(minZ, PROVINCE_CELL_SIZE) - 2;
        int maxCellZ = Math.floorDiv(maxZ, PROVINCE_CELL_SIZE) + 2;

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
                int reach = (int) Math.ceil(radius * LODE_REACH);
                if (origin.getX() + reach < minX || origin.getX() - reach > maxX || origin.getZ() + reach < minZ ||
                        origin.getZ() - reach > maxZ) {
                    continue;
                }

                placeVeinInChunk(chunk, vein, minX, minZ, worldMinY, worldMaxY);
                placeVeinIndicators(chunk, vein, minX, minZ, worldMinY, worldMaxY, dimensionKey);
            }
        }
    }

    private static final double VEIN_SQUASH = 0.6;

    public static final int MAX_LODES = 6;

    private static final double LODE_REACH = 1.3;

    private record Lode(Material material, double cx, double cy, double cz, double rx, double ry, double rz,
                        double phaseA, double phaseB, double phaseC) {}

    static List<Lode> lodesOf(BlockPos origin, int radius, long seed, ProvinceVeinTemplates.Template template) {
        var layers = template.layers();
        int count = Math.min(MAX_LODES, layers.size());
        List<Lode> out = new java.util.ArrayList<>(count);

        int start = (int) Math.floorMod(seed >>> 20, (long) layers.size());
        double base = ((positionHash(seed ^ 0x10DEL, 1, 2, 3) >>> 20) & 0xFFFF) / 65535.0 * Math.PI * 2.0;
        int satellites = Math.max(0, count - 1);
        for (int i = 0; i < count; i++) {
            var layer = layers.get((start + i) % layers.size());
            long h = positionHash(seed ^ 0x10DE5L, i, 9, i * 17);
            double stretch = 0.85 + (((h >>> 8) & 0xFF) / 255.0) * 0.5;
            double cx, cz, size, cy;
            if (i == 0) {
                size = radius * 0.40;
                cx = origin.getX();
                cz = origin.getZ();
                cy = origin.getY();
            } else {
                double angle = base + (i - 1) * (Math.PI * 2.0 / satellites) + (((h & 0xFFFF) / 65535.0) - 0.5) * 0.5;
                double dist = radius * (0.68 + 0.10 * (((h >>> 16) & 0xFFFF) / 65535.0));
                size = radius * (0.22 + 0.03 * layer.weight()) + (((h >>> 32) & 0xFFFF) / 65535.0) * radius * 0.03;
                cx = origin.getX() + Math.cos(angle) * dist;
                cz = origin.getZ() + Math.sin(angle) * dist;
                double wander = Math.min(radius * VEIN_SQUASH, 12) * 0.5;
                cy = origin.getY() + (((h >>> 48) & 0xFF) / 255.0 - 0.5) * 2.0 * wander;
            }
            double ry = size * 0.55;
            cy = Math.max(template.minY() + ry, Math.min(template.maxY() - ry, cy));
            out.add(new Lode(layer.material(), cx, cy, cz, size * stretch, ry, size / stretch,
                    (h & 0xFFFF) / 65535.0 * 6.28, ((h >>> 12) & 0xFFFF) / 65535.0 * 6.28,
                    ((h >>> 24) & 0xFFFF) / 65535.0 * 6.28));
        }
        return out;
    }

    private static void placeVeinInChunk(ChunkAccess chunk, ProvinceVeinInstance vein,
                                         int minX, int minZ, int worldMinY, int worldMaxY) {
        ProvinceVeinTemplates.Template template = vein.template();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (Lode lode : lodesOf(vein.origin(), vein.radius(), vein.seed(), template)) {

            double reachX = lode.rx() * 1.3, reachY = lode.ry() * 1.3, reachZ = lode.rz() * 1.3;
            if (lode.cx() + reachX < minX || lode.cx() - reachX > minX + 15 || lode.cz() + reachZ < minZ ||
                    lode.cz() - reachZ > minZ + 15) {
                continue;
            }
            int loY = Math.max(worldMinY, Math.max(template.minY(), (int) Math.floor(lode.cy() - reachY)));
            int hiY = Math.min(worldMaxY - 1, Math.min(template.maxY(), (int) Math.ceil(lode.cy() + reachY)));
            if (loY > hiY) continue;

            for (int x = Math.max(minX, (int) Math.floor(lode.cx() - reachX)); x <= Math.min(minX + 15,
                    (int) Math.ceil(lode.cx() + reachX)); x++) {
                for (int z = Math.max(minZ, (int) Math.floor(lode.cz() - reachZ)); z <= Math.min(minZ + 15,
                        (int) Math.ceil(lode.cz() + reachZ)); z++) {
                    for (int y = loY; y <= hiY; y++) {
                        double nx = (x + 0.5 - lode.cx()) / lode.rx();
                        double ny = (y + 0.5 - lode.cy()) / lode.ry();
                        double nz = (z + 0.5 - lode.cz()) / lode.rz();

                        double wobble = 1.0 + 0.16 * Math.sin(x * 0.31 + lode.phaseA()) * Math.cos(z * 0.27 +
                                lode.phaseB()) + 0.12 * Math.sin(y * 0.45 + lode.phaseC());
                        double d2 = (nx * nx + ny * ny + nz * nz) / (wobble * wobble);
                        if (d2 > 1.0) continue;

                        pos.set(x, y, z);
                        BlockState current = chunk.getBlockState(pos);
                        if (!current.is(ProvinceVeinTemplates.STONE_TAG) &&
                                !current.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES))
                            continue;

                        double fill = Math.min(1.0, 1.1 - 0.65 * d2);
                        long hash = positionHash(vein.seed(), x, y, z);
                        if (((hash >>> 40) & 0xFFFFFF) / (double) 0x1000000 >= fill) continue;

                        Optional<TagPrefix> prefix = ChemicalHelper.getOrePrefix(current);
                        if (prefix.isEmpty()) continue;
                        net.minecraft.world.level.block.Block toPlace = ChemicalHelper.getBlock(prefix.get(),
                                lode.material());
                        if (toPlace == null || toPlace.defaultBlockState().isAir()) continue;

                        chunk.setBlockState(pos, toPlace.defaultBlockState(), false);
                    }
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
                    Blocks.MAGMA_BLOCK.defaultBlockState(), 40, SHAPE_STREAK, 1.25, 0);

            case "sculk" -> new GardenTheme(Blocks.SCULK.defaultBlockState(), Blocks.MOSS_BLOCK.defaultBlockState(),
                    Blocks.MOSS_CARPET.defaultBlockState(), Blocks.FLOWERING_AZALEA.defaultBlockState(),
                    Blocks.VERDANT_FROGLIGHT.defaultBlockState(), 60, SHAPE_RING, 1.5, 0);

            case "void" -> new GardenTheme(Blocks.END_STONE.defaultBlockState(),
                    Blocks.PURPUR_BLOCK.defaultBlockState(),
                    Blocks.CHORUS_FLOWER.defaultBlockState(), Blocks.CHORUS_PLANT.defaultBlockState(),
                    Blocks.AMETHYST_BLOCK.defaultBlockState(), 25, SHAPE_CLUMPS, 1.4, 3);

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
                                     int worldMinY, int worldMaxY, String dimensionKey) {
        ProvinceVeinTemplates.Template template = vein.template();
        BlockPos origin = vein.origin();
        int radius = vein.radius();
        int vertical = (int) Math.ceil(radius * VEIN_SQUASH);
        GardenTheme theme = gardenTheme();

        List<Lode> lodes = lodesOf(origin, radius, vein.seed(), template);
        if (lodes.isEmpty()) return;
        int gardens = lodes.size() + theme.extraGardens();
        double[] gx = new double[gardens];
        double[] gz = new double[gardens];
        double[] gr = new double[gardens];
        double[] gAngle = new double[gardens];
        Material[] gMaterial = new Material[gardens];
        Lode[] gLode = new Lode[gardens];
        for (int g = 0; g < gardens; g++) {
            Lode lode = lodes.get(g < lodes.size() ? g : (g - lodes.size()) % Math.min(2, lodes.size()));
            long h = positionHash(vein.seed() ^ 0x6A4DE11L, g, 7, g * 31);
            double angle = ((h & 0xFFFF) / 65535.0) * Math.PI * 2.0;
            double span = Math.min(lode.rx(), lode.rz());
            double dist = (g < lodes.size() ? 0.35 * (((h >>> 16) & 0xFFFF) / 65535.0) :
                    0.3 + 0.4 * (((h >>> 16) & 0xFFFF) / 65535.0)) * span;
            gx[g] = lode.cx() + Math.cos(angle) * dist;
            gz[g] = lode.cz() + Math.sin(angle) * dist;

            gr[g] = Math.min((9.0 + ((h >>> 32) & 0xFF) / 255.0 * 6.0) * theme.sizeMul(), Math.max(7.0, span * 1.1));
            gAngle[g] = (((h >>> 8) & 0xFFFF) / 65535.0) * Math.PI;
            gMaterial[g] = lode.material();
            gLode[g] = lode;
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
                            state.canBeReplaced() && state.getFluidState().isEmpty() ||
                            net.phoenix.core.integration.conflux.dimension.worldgen.TreeBuilder.isFoliage(state) ||
                            net.phoenix.core.integration.conflux.dimension.worldgen.TreeBuilder.covers(dimensionKey, x,
                                    y, z)) {
                        continue;
                    }
                    ground = y;
                    found = state.getFluidState().isEmpty();
                    break;
                }
                if (!found) continue;

                Lode below = gLode[garden];
                int veinTop = (int) Math.min(template.maxY(), below.cy() + below.ry());
                double scale = Math.max(0.35, Math.min(1.0, 1.0 - Math.max(0, ground - veinTop) / 80.0));
                double reachHere = gr[garden] * scale;
                if (gardenDist > reachHere) continue;
                double falloff = 1.0 - gardenDist / reachHere;

                boolean hollow = theme.shape() == SHAPE_RING && falloff > 0.62;

                BlockState above = chunk.getBlockState(pos.set(x, ground + 1, z));
                if (!above.isAir() && !above.canBeReplaced()) continue;

                long h = positionHash(vein.seed() ^ 0x6A4DE12L, x, 0, z);
                double roll = ((h >>> 24) & 0xFFFFFF) / (double) 0x1000000;
                double density = Math.min(0.95, (0.40 + 0.60 * falloff) * (0.6 + 0.4 * scale));

                if (falloff > 0.3 && (h & 3) != 0) {
                    chunk.setBlockState(pos.set(x, ground, z), (h & 16) == 0 ? theme.bed() : theme.bedAlt(), false);
                }

                if (gardenDist < 0.8) {

                    for (int k = 1; k <= 4; k++) {
                        chunk.setBlockState(pos.set(x, ground + k, z), Blocks.END_ROD.defaultBlockState(), false);
                    }
                    continue;
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

                    Material other = nearestLodeMaterial(lodes, x, z, gMaterial[garden]);
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

    private static Material nearestLodeMaterial(List<Lode> lodes, int x, int z, Material fallback) {
        Material best = fallback;
        double bestScore = 1.5;
        for (Lode lode : lodes) {
            double nx = (x + 0.5 - lode.cx()) / lode.rx();
            double nz = (z + 0.5 - lode.cz()) / lode.rz();
            double score = Math.sqrt(nx * nx + nz * nz);
            if (score < bestScore) {
                bestScore = score;
                best = lode.material();
            }
        }
        return best;
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

    private List<String> veinKinds(List<WorldgenProfile.BiomeDefinition> biomes) {
        java.util.LinkedHashSet<String> kinds = new java.util.LinkedHashSet<>();
        for (WorldgenProfile.BiomeDefinition biome : biomes) kinds.addAll(biome.oreVeins);
        return List.copyOf(kinds);
    }

    @Nullable
    private ProvinceVeinInstance generateCellVein(List<WorldgenProfile.BiomeDefinition> biomes,
                                                  SimplexNoise regionNoise, int cellX, int cellZ) {
        List<String> kinds = veinKinds(biomes);
        if (kinds.isEmpty()) return null;
        if (!VeinPlan.stays(seed, cellX, cellZ, PROVINCE_CELL_SIZE, kinds.size(), PROVINCE_VEIN_CHANCE, SAME_KIND_GAP,
                (cx, cz) -> cx >= -1 && cx <= 0 && cz >= -1 && cz <= 0)) {
            return null;
        }

        String oreId = kinds.get(VeinPlan.kind(seed, cellX, cellZ, kinds.size()));
        ProvinceVeinTemplates.Template template = ProvinceVeinTemplates.get(oreId);
        if (template == null) return null;

        RandomSource cellRandom = new XoroshiroRandomSource(
                seed ^ ((long) cellX * 341873128712L) ^ ((long) cellZ * 132897987541L) ^ 0x50484F454E4958L);
        int[] origin = VeinPlan.origin(seed, cellX, cellZ, PROVINCE_CELL_SIZE);
        int radius = template.minRadius() + cellRandom.nextInt(template.maxRadius() - template.minRadius() + 1);
        int originY = template.minY() + cellRandom.nextInt(template.maxY() - template.minY() + 1);
        long veinSeed = cellRandom.nextLong();

        return new ProvinceVeinInstance(new BlockPos(origin[0], originY, origin[1]), radius, veinSeed, template);
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
        net.minecraft.world.level.block.state.BlockState water = (disciplineId.equals("phoenix") ? Blocks.LAVA :
                Blocks.WATER).defaultBlockState();
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

        int[] topLayer = new int[16];
        for (int cx = 0; cx < 4; cx++) {
            for (int cz = 0; cz < 4; cz++) {
                int c00 = (cx * 5 + cz) * gridY;
                int c01 = (cx * 5 + cz + 1) * gridY;
                int c10 = ((cx + 1) * 5 + cz) * gridY;
                int c11 = ((cx + 1) * 5 + cz + 1) * gridY;
                int top = gridY - 1;
                while (top > 0 && grid[c00 + top] <= 0 && grid[c01 + top] <= 0 && grid[c10 + top] <= 0 &&
                        grid[c11 + top] <= 0) {
                    top--;
                }
                topLayer[cx * 4 + cz] = top;
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
                boolean open = profile.chasms() != null &&
                        interp(grid, b00, b01, b10, b11, fx, fz, minY, minY + 1, cell, gridY) < 0 &&
                        interp(grid, b00, b01, b10, b11, fx, fz, minY, minY + 40, cell, gridY) < 0 &&
                        interp(grid, b00, b01, b10, b11, fx, fz, minY, minY + 100, cell, gridY) < 0;
                boolean deepOpen = profile.chasms() != null &&
                        interp(grid, b00, b01, b10, b11, fx, fz, minY, minY + 1, cell, gridY) < 0 &&
                        interp(grid, b00, b01, b10, b11, fx, fz, minY, minY + 20, cell, gridY) < 0;
                int columnTop = topLayer[cx * 4 + cz];
                for (int y = minY; y < maxY; y++) {
                    if (open) break;
                    int gy = (y - minY) / cell;

                    if (gy > columnTop && y > minY + DEEP_LAVA_TOP) break;
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
                        if (deepOpen) continue;
                        state = bedrock;
                    } else if (!solid) {
                        if (deepOpen || y > minY + DEEP_LAVA_TOP) continue;
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

                for (int y = open ? ground : Math.min(seaLevel, maxY - 1); y > ground; y--) {
                    sections[chunk.getSectionIndex(y)].setBlockState(x & 15, y & 15, z & 15, water, false);
                }
            }
        }
        net.minecraft.world.level.levelgen.Heightmap.primeHeightmaps(chunk, java.util.EnumSet.of(
                net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG,
                net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG));
        return chunk;
    }

    private static double interp(double[] grid, int b00, int b01, int b10, int b11, double fx, double fz, int minY,
                                 int y,
                                 int cell, int gridY) {
        int gy = (y - minY) / cell;
        double fy = (y - minY) / (double) cell - gy;
        int g1 = Math.min(gy + 1, gridY - 1);
        gy = Math.min(gy, gridY - 1);
        double d00 = grid[b00 + gy] + (grid[b00 + g1] - grid[b00 + gy]) * fy;
        double d01 = grid[b01 + gy] + (grid[b01 + g1] - grid[b01 + gy]) * fy;
        double d10 = grid[b10 + gy] + (grid[b10 + g1] - grid[b10 + gy]) * fy;
        double d11 = grid[b11 + gy] + (grid[b11 + g1] - grid[b11 + gy]) * fy;
        double d0 = d00 + (d01 - d00) * fz;
        double d1 = d10 + (d11 - d10) * fz;
        return d0 + (d1 - d0) * fx;
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
