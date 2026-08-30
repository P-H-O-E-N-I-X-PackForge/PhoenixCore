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
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import com.gregtechceu.gtceu.api.data.worldgen.GTOreDefinition;
import com.gregtechceu.gtceu.api.data.worldgen.ores.GeneratedVein;
import com.gregtechceu.gtceu.api.data.worldgen.ores.OreBlockPlacer;
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

/**
 * One shared, permanent world per discipline (like the vanilla Nether/End) - every team that
 * picks a given discipline enters the same dimension instance, keyed only by discipline id.
 * This is required, not just a design choice: GT registers each ore vein against a fixed
 * dimension key at mod load, so a per-team dynamically-created dimension (whose key isn't
 * known until a team actually creates it) can never match a vein's registered dimension set -
 * GT ore veins silently never generated under the old per-team-instance design.
 */
public class DisciplineChunkGenerator extends ChunkGenerator {
    public static final Codec<DisciplineChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
            Codec.STRING.fieldOf("discipline_id").forGetter(gen -> gen.disciplineId),
            Codec.LONG.fieldOf("seed").forGetter(gen -> gen.seed)
    ).apply(instance, instance.stable(DisciplineChunkGenerator::new)));

    // Built eagerly from the codec's placeholder seed - datapack dimension JSON is decoded
    // before the real world seed is known, so this only supplies minY/maxY/seaLevel (fixed
    // per discipline, independent of seed value) for the handful of ChunkGenerator methods
    // that run without a RandomState. Actual terrain shape comes from worldSeededProfile below.
    private final TerrainProfile terrainProfile;
    private final WorldgenProfile worldgenProfile;
    private final String disciplineId;
    private final long seed;

    // Lazily built the first time a RandomState (carrying the real, per-world seed) becomes
    // available, so terrain genuinely varies between playthroughs instead of every world/save
    // generating identical terrain from the fixed seed baked into the dimension JSON.
    private volatile TerrainProfile worldSeededProfile;

    // GT places its registered ore veins (see GTVeinPlacement/DefaultDisciplineOres) via a mixin
    // injected into the TAIL of ChunkGenerator#applyBiomeDecoration - but that mixin is woven
    // into the base class's OWN method body, and applyBiomeDecoration below completely overrides
    // that method rather than extending it. Virtual dispatch means the base class's version
    // (carrying the mixin's injected call) never runs at all for this subclass, so every
    // registered vein was silently never placed. OrePlacer is GT's actual public ore-placement
    // API (what the mixin itself calls) - invoking it directly here gets real vein placement
    // without pulling in the rest of vanilla's applyBiomeDecoration (which is what needed
    // avoiding in the first place, for the tree-duplication reason below). One instance per
    // chunk generator, not per chunk - it caches in-progress vein state across chunks internally.
    private final com.gregtechceu.gtceu.api.data.worldgen.ores.OrePlacer orePlacer =
            new com.gregtechceu.gtceu.api.data.worldgen.ores.OrePlacer();

    // Phoenix's noise-based ore province system (see placeProvinceOres below) - never used by
    // the other 4 disciplines, which still register through GT's normal per-dimension vein
    // pipeline. Keyed by cell coordinate, not chunk: a cell is much bigger than a chunk, so many
    // chunks share (and must agree on) the same cell's vein without recomputing it from scratch
    // each time. computeIfAbsent below is what actually fills it in.
    private static final int PROVINCE_CELL_SIZE = 160;
    private static final float PROVINCE_VEIN_CHANCE = 0.4f;
    private final Map<Long, GeneratedVein> provinceCellCache = new ConcurrentHashMap<>();

    // Picks which of the discipline's authored BiomeDefinitions covers a given column, so a
    // dimension reads as several distinct regions (its own biome list already existed with
    // real per-biome surface blocks and colors, just unused beyond the single "primary" one).
    // Same lazy-from-RandomState treatment as the terrain profile, for the same reason.
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

    /**
     * Derives a terrain profile from the real world seed carried by {@link RandomState} rather
     * than the placeholder seed baked into the dimension JSON, so terrain actually differs
     * between worlds. RandomState exposes no raw seed accessor, so a seed-derived
     * PositionalRandomFactory (namespaced per discipline, so each discipline still varies
     * independently within the same world) stands in for it. Cached after first use - rebuilding
     * only constructs a handful of noise samplers, done once per dimension per server run, not
     * per chunk.
     */
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

    // How much extra weight the primary (plain, walkable grass/dirt) biome gets over each
    // exotic accent biome when picking a winner below - not a literal area percentage, just how
    // hard the scale is tipped in its favor. These dimensions are meant to replace the overworld
    // outright, so most of the world needs to actually be liveable, with "cool" biome variety
    // showing up as pockets rather than everywhere. Taking the max of several independent noise
    // fields tends to produce a higher value the more fields there are (order statistics), so
    // with 6-7 accent biomes now competing (up from 1-2) this needs to sit noticeably higher
    // than it would with only a couple of accents, or the primary stops actually being primary.
    private static final double PRIMARY_BIOME_BIAS = 0.65;

    /**
     * Picks a biome region for this column by sampling one independent noise field per biome
     * (same noise source, offset to a different coordinate per biome so the fields don't
     * correlate) and taking whichever scores highest, with the primary biome's field pre-biased
     * upward. Unlike slicing a single noise value into ordered bands - which forces every accent
     * biome into the same fixed position relative to the others, reading as nested rings instead
     * of natural terrain - independent fields let any biome border any other and form organically
     * shaped, randomly scattered patches, the same trick behind most "biome splatting" techniques.
     */
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
        // Each discipline uses a genuinely different terrain algorithm, not just different
        // amplitude/frequency numbers on the same heightmap noise - otherwise every dimension
        // reads as the same rolling-hill shape no matter the theme.
        return switch (disciplineId) {
            case "phoenix" -> TerrainProfile.builder("phoenix")
                    .seed(seed)
                    .baseY(72).amplitude(100).frequency(0.004).octaves(6)
                    .style(TerrainProfile.Style.RIDGED) // sharp volcanic peaks, not round hills
                    .ocean(0.0007, 0.18, 25).river(0.0025, 0.07, 5)
                    .caves(true).build();

            // sculk and void used VOLUMETRIC as their base terrain shape - a true 3D density
            // field, which reads as disconnected floating blobs/cave honeycomb rather than
            // normal walkable ground. These dimensions are meant to replace the overworld
            // outright, so the base shape now uses ordinary rolling terrain (matching every
            // other discipline); the cavernous/floating character still comes through via
            // caves, surface theming and decorations instead of the terrain math itself.
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
                    .style(TerrainProfile.Style.TERRACED).terraceStep(6.0) // stacked industrial platforms
                    .ocean(0.0007, 0.17, 22).river(0.0024, 0.07, 4)
                    .caves(true).build();

            case "sealed_b" -> TerrainProfile.builder("sealed_b")
                    .seed(seed).baseY(65).amplitude(75)
                    .frequency(0.0032).octaves(6)
                    .style(TerrainProfile.Style.WARPED).warpStrength(20.0) // distorted, reality-glitch terrain
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
        List<WorldgenProfile.BiomeDefinition> biomes = worldgenProfile.biomes.biomes;
        if (biomes.isEmpty()) return;

        SimplexNoise regionNoise = resolveBiomeRegionNoise(randomState);
        // Only used for its (cheap, already-cached) waterMask() below, not the expensive
        // sampler() density chain - resolveTerrainProfile just returns the cached instance.
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
                // fillFromNoise already ran this exact density scan for this same chunk and wrote
                // STONE wherever it was positive - re-sampling the (expensive: terrain + water
                // mask + two cave-carving layers) density function here again was pure duplicated
                // work. Reading the blocks fillFromNoise already placed finds the identical
                // "topmost solid" answer for a fraction of the cost. Checking specifically for
                // STONE (not just "non-air") matters: fillFromNoise also fills river/ocean
                // columns with WATER above the rock, and the original density scan (testing
                // sample()>0, which is false for water) would skip straight past that water to
                // the real rock surface underneath - a plain "non-air" check would instead stop
                // at the water and paint grass on top of it.
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
                    // Real lake/river/ocean beds are sand and gravel, not the same grass a dry
                    // hillside gets - painting the biome's ordinary surface block under the water
                    // (what happened before this check existed) is exactly what makes a river
                    // read as "water poured onto normal ground" instead of an actual formed body
                    // of water with its own distinct floor.
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
        // Deliberately skips super.applyBiomeDecoration(...) - the biome_source here is a
        // "minecraft:fixed" pointing at a real vanilla biome (badlands, dark_forest,
        // cherry_grove, stony_peaks, warped_forest) purely for ambient/color purposes, but that
        // super call would also run THAT biome's own full feature pipeline, including its
        // natural tree generation - vanilla oak/dark oak/cherry/warped fungus trees placing
        // themselves right alongside our own custom ones. This dimension owns its own
        // decoration entirely via applyWorldgenFeatures below.
        //
        // orePlacer.placeOres(...) runs last, matching where GT's own mixin would have placed it
        // (TAIL of applyBiomeDecoration, i.e. after every other decoration) - see the orePlacer
        // field comment for why this direct call is needed at all instead of the mixin firing on
        // its own.
        ChunkPos chunkPos = chunk.getPos();
        applyWorldgenFeatures(level, chunkPos.x, chunkPos.z);
        orePlacer.placeOres(level, this, chunk);
        if ("phoenix".equals(disciplineId) || "void".equals(disciplineId) || "sculk".equals(disciplineId)) {
            placeProvinceOres(level, chunk);
        }
    }

    /**
     * Phoenix-only ore placement, entirely independent of GT's own biome-keyed vein registry
     * (see {@link ProvinceVeinTemplates} for why). The dimension is divided into a grid of
     * {@link #PROVINCE_CELL_SIZE}-block cells; each one independently and deterministically
     * (so results never depend on generation order or which chunk happens to trigger them
     * first) rolls whether a vein originates inside it and, if so, which ore. Which ore is
     * eligible is decided by sampling the exact same region noise {@link #buildSurface} uses
     * (via {@link #selectBiome}), so a vein's ore always matches the ground above it - walking
     * into a region visually reads as walking into that region's resources too.
     */
    private void placeProvinceOres(WorldGenLevel level, ChunkAccess chunk) {
        List<WorldgenProfile.BiomeDefinition> biomes = worldgenProfile.biomes.biomes;
        SimplexNoise regionNoise = biomeRegionNoise;
        if (biomes.isEmpty() || regionNoise == null) return;

        ChunkPos chunkPos = chunk.getPos();
        // A vein's origin can land in a neighboring cell and still spread into this chunk, so
        // every cell touching this chunk's 1-cell-radius neighborhood needs checking, not just
        // the one the chunk itself falls in.
        int minCellX = Math.floorDiv(chunkPos.getMinBlockX(), PROVINCE_CELL_SIZE) - 1;
        int maxCellX = Math.floorDiv(chunkPos.getMaxBlockX(), PROVINCE_CELL_SIZE) + 1;
        int minCellZ = Math.floorDiv(chunkPos.getMinBlockZ(), PROVINCE_CELL_SIZE) - 1;
        int maxCellZ = Math.floorDiv(chunkPos.getMaxBlockZ(), PROVINCE_CELL_SIZE) + 1;

        RandomSource placeRandom = new XoroshiroRandomSource(level.getSeed() ^ chunkPos.toLong());
        try (BulkSectionAccess access = new BulkSectionAccess(level)) {
            for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
                for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                    int finalCellX = cellX;
                    int finalCellZ = cellZ;
                    GeneratedVein vein = provinceCellCache.computeIfAbsent(cellKey(cellX, cellZ),
                            k -> generateCellVein(level, biomes, regionNoise, finalCellX, finalCellZ));
                    if (vein == null) continue;
                    orePlacer.placeVein(chunkPos, placeRandom, access, vein, null);
                }
            }
        }
    }

    private static long cellKey(int cellX, int cellZ) {
        return ((long) cellX << 32) ^ (cellZ & 0xFFFFFFFFL);
    }

    @Nullable
    private GeneratedVein generateCellVein(WorldGenLevel level, List<WorldgenProfile.BiomeDefinition> biomes,
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
        GTOreDefinition template = ProvinceVeinTemplates.get(oreId);
        if (template == null) return null;

        Optional<BlockPos> origin = template.range().getPositions(
                new PlacementContext(level, this, Optional.empty()),
                cellRandom, new BlockPos(originX, 0, originZ)).findFirst();
        if (origin.isEmpty()) return null;

        Map<BlockPos, OreBlockPlacer> blocks = template.veinGenerator()
                .generate(level, cellRandom, template, origin.get());
        if (blocks.isEmpty()) return null;

        return new GeneratedVein(new ChunkPos(origin.get()), ConfluxWorldGenLayers.CONFLUX_STONE, blocks);
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

                    // Water only fills the gap between this column's actual surface and its
                    // local water surface, stopping the instant it hits solid ground - never
                    // continuing down into a cave below that surface. That surface is global sea
                    // level for oceans, but the LOCAL land height for rivers - a river running
                    // through a hillside sits at that hillside's height, not at global sea level,
                    // so using a single fixed sea-level fill height for rivers was flooding every
                    // river-through-high-terrain crossing into a solid wall of water. isWaterColumn()
                    // also only fires for the clear core of an ocean/river band, not "any air
                    // below the surface", so a cave mouth that happens to open up nearby stays dry.
                    if (waterMask != null && waterMask.isWaterColumn(x, z)) {
                        // The density field's "topmost solid block" is effectively a floor(), not
                        // a round-to-nearest - a height of 72.9 still has its highest solid block
                        // at y=72 (72.9-72=0.9>0 solid, 72.9-73=-0.1<0 air). Using Math.round()
                        // here bumped the water's starting row a block above the natural bank
                        // for every column whose fractional height was >= 0.5 - roughly half of
                        // them - which is exactly the "water sits one block too high" pattern.
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
        }, executor);
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