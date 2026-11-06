package net.phoenix.core.integration.conflux.dimension.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import org.jetbrains.annotations.Nullable;

public class WorldgenApplier {

    public static void applyWorldgen(
                                     WorldGenLevel level,
                                     DisciplineWorldgenConfig config,
                                     @Nullable String currentStage,
                                     net.minecraft.util.RandomSource random,
                                     int chunkX,
                                     int chunkZ) {
        if (config.ores.length > 0) {
            OreGenerator.generateOres(level, config.ores, random, chunkX, chunkZ);
        }

        if (currentStage != null) {
            DisciplineWorldgenConfig.BiomeBlockPalette.BlockTypeSet blocks = config.getBlocksForStage(currentStage);
            if (blocks != null) {
                applySurfaceBlocks(level, config, blocks, chunkX, chunkZ);
            }
        }

        if (config.decorations != null) {
            applyDecorations(level, config.decorations, random, chunkX, chunkZ);
        }
    }

    private static void applySurfaceBlocks(
                                           WorldGenLevel level,
                                           DisciplineWorldgenConfig config,
                                           DisciplineWorldgenConfig.BiomeBlockPalette.BlockTypeSet blocks,
                                           int chunkX,
                                           int chunkZ) {
        int minX = chunkX * 16;
        int minZ = chunkZ * 16;

        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {

                int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
                if (y <= level.getMinBuildHeight() + 1) continue;

                BlockPos surfacePos = new BlockPos(x, y, z);
                BlockPos grassPos = surfacePos.above();

                if (level.ensureCanWrite(grassPos)) {

                    level.setBlock(grassPos,
                            net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                                    ResourceLocation.parse(blocks.grass))
                                    .defaultBlockState(),
                            3);

                    for (int i = 1; i < 4; i++) {
                        BlockPos dirtPos = grassPos.below(i);
                        if (level.ensureCanWrite(dirtPos)) {
                            level.setBlock(dirtPos,
                                    net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                                            ResourceLocation.parse(blocks.dirt))
                                            .defaultBlockState(),
                                    3);
                        }
                    }
                }
            }
        }
    }

    public static void applyDecorations(
                                        WorldGenLevel level,
                                        DisciplineWorldgenConfig.DecorationConfig decorations,
                                        net.minecraft.util.RandomSource random,
                                        int chunkX,
                                        int chunkZ) {
        int minX = chunkX * 16;
        int minZ = chunkZ * 16;

        for (int i = 0; i < 16 * 16 * decorations.grassDensity; i++) {
            int x = minX + random.nextInt(16);
            int z = minZ + random.nextInt(16);
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
            if (y <= level.getMinBuildHeight() + 1) continue;

            BlockPos grassPos = new BlockPos(x, y, z);
            if (level.ensureCanWrite(grassPos) && level.isEmptyBlock(grassPos)) {
                level.setBlock(grassPos, Blocks.TALL_GRASS.defaultBlockState(), 3);
            }
        }

        for (String decoration : decorations.customDecorations.keySet()) {
            int count = decorations.customDecorations.get(decoration);
            for (int i = 0; i < count; i++) {
                int x = minX + random.nextInt(16);
                int z = minZ + random.nextInt(16);
                int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
                if (y <= level.getMinBuildHeight() + 1) continue;

                BlockPos pos = new BlockPos(x, y, z);
                if (level.ensureCanWrite(pos) && level.isEmptyBlock(pos)) {
                    try {
                        var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                                ResourceLocation.parse(decoration));
                        if (block != Blocks.AIR) {
                            level.setBlock(pos, block.defaultBlockState(), 3);
                        }
                    } catch (Exception e) {

                    }
                }
            }
        }
    }

    public static void applyCaveModifications(
                                              DisciplineWorldgenConfig config) {
        if (!config.caves.enabled) {

            return;
        }
    }

    @Nullable
    public static DisciplineWorldgenConfig getConfigForDiscipline(String disciplineId) {
        net.phoenix.core.integration.conflux.dimension.DisciplineTheme theme = net.phoenix.core.integration.conflux.dimension.DisciplineThemeRegistry
                .getTheme(disciplineId);

        if (theme == null) return null;

        return createDefaultConfig(disciplineId);
    }

    private static DisciplineWorldgenConfig createDefaultConfig(String disciplineId) {
        return switch (disciplineId) {
            case "phoenix" -> createPhoenixConfig();
            case "sculk" -> createSculkConfig();
            case "void" -> createVoidConfig();
            case "sealed_a", "sealed_b" -> createSealedConfig();
            default -> createDefaultConfig();
        };
    }

    private static DisciplineWorldgenConfig createPhoenixConfig() {
        DisciplineWorldgenConfig.OreConfig[] ores = {
                new DisciplineWorldgenConfig.OreConfig(
                        ResourceLocation.parse("minecraft:iron_ore"),
                        8, 20, 0, 64, "minecraft:stone")
        };
        return new DisciplineWorldgenConfig("phoenix", ores,
                new DisciplineWorldgenConfig.BiomeBlockPalette(new java.util.HashMap<>()),
                new DisciplineWorldgenConfig.SurfaceRuleSet[0],
                new DisciplineWorldgenConfig.StructureSet(new java.util.ArrayList<>(), 1.0f),
                new DisciplineWorldgenConfig.DecorationConfig(0.5f, 0.2f, 0.1f, new java.util.HashMap<>()),
                new DisciplineWorldgenConfig.CaveConfig(1.0f, 1.0f, true));
    }

    private static DisciplineWorldgenConfig createSculkConfig() {
        return createDefaultConfig();
    }

    private static DisciplineWorldgenConfig createVoidConfig() {
        DisciplineWorldgenConfig.OreConfig[] ores = {};
        return new DisciplineWorldgenConfig("void", ores,
                new DisciplineWorldgenConfig.BiomeBlockPalette(new java.util.HashMap<>()),
                new DisciplineWorldgenConfig.SurfaceRuleSet[0],
                new DisciplineWorldgenConfig.StructureSet(new java.util.ArrayList<>(), 0.0f),
                new DisciplineWorldgenConfig.DecorationConfig(0.0f, 0.0f, 0.0f, new java.util.HashMap<>()),
                new DisciplineWorldgenConfig.CaveConfig(0.0f, 0.0f, false));
    }

    private static DisciplineWorldgenConfig createSealedConfig() {
        return createDefaultConfig();
    }

    private static DisciplineWorldgenConfig createDefaultConfig() {
        DisciplineWorldgenConfig.OreConfig[] ores = {
                new DisciplineWorldgenConfig.OreConfig(
                        ResourceLocation.parse("minecraft:iron_ore"),
                        8, 10, 0, 64, "minecraft:stone"),
                new DisciplineWorldgenConfig.OreConfig(
                        ResourceLocation.parse("minecraft:coal_ore"),
                        16, 20, 0, 128, "minecraft:stone")
        };

        return new DisciplineWorldgenConfig("default", ores,
                new DisciplineWorldgenConfig.BiomeBlockPalette(new java.util.HashMap<>()),
                new DisciplineWorldgenConfig.SurfaceRuleSet[0],
                new DisciplineWorldgenConfig.StructureSet(new java.util.ArrayList<>(), 1.0f),
                new DisciplineWorldgenConfig.DecorationConfig(1.0f, 0.5f, 0.3f, new java.util.HashMap<>()),
                new DisciplineWorldgenConfig.CaveConfig(1.0f, 1.0f, true));
    }

    @FunctionalInterface
    public interface BiomeLookup {

        @Nullable
        String biomeIdAt(int x, int z);

        @Nullable
        default Block groundBlockAt(int x, int z) {
            return null;
        }
    }

    public static void applyWorldgenProfile(
                                            WorldGenLevel level,
                                            WorldgenProfile profile,
                                            @Nullable String currentStage,
                                            int chunkX,
                                            int chunkZ) {
        applyWorldgenProfile(level, profile, currentStage, chunkX, chunkZ, null);
    }

    public static void applyWorldgenProfile(
                                            WorldGenLevel level,
                                            WorldgenProfile profile,
                                            @Nullable String currentStage,
                                            int chunkX,
                                            int chunkZ,
                                            @Nullable BiomeLookup biomes) {
        if (profile == null) return;

        if (profile.liquids != null) {
            applyLiquidFeatures(level, profile.liquids, chunkX, chunkZ);
        }

        if (profile.decorations != null) {
            applyBiomeVegetation(level, profile.decorations, biomes, level.getRandom(), chunkX, chunkZ);
        }

        SmallStructures.maybePlace(level, profile.disciplineId, chunkX, chunkZ);

        if (currentStage != null && profile.progression != null) {
            applyProgressionWorldgen(level, profile, currentStage, chunkX, chunkZ);
        }
    }

    private static final java.util.Map<String, net.minecraft.world.level.block.state.BlockState> DECORATION_PALETTE = new java.util.HashMap<>();
    static {
        DECORATION_PALETTE.put("fire_flower", Blocks.TORCHFLOWER.defaultBlockState());
        DECORATION_PALETTE.put("lava_rose", Blocks.WITHER_ROSE.defaultBlockState());
        DECORATION_PALETTE.put("sculk_flower", Blocks.GLOW_LICHEN.defaultBlockState());
        DECORATION_PALETTE.put("glowing_vine", Blocks.GLOW_LICHEN.defaultBlockState());

        DECORATION_PALETTE.put("void_flower", Blocks.ALLIUM.defaultBlockState());
        DECORATION_PALETTE.put("ethereal_crystal", Blocks.SMALL_AMETHYST_BUD.defaultBlockState());
        DECORATION_PALETTE.put("copper_flower", Blocks.DANDELION.defaultBlockState());
        DECORATION_PALETTE.put("warped_flower", Blocks.WARPED_ROOTS.defaultBlockState());
        DECORATION_PALETTE.put("soul_lantern", Blocks.SOUL_LANTERN.defaultBlockState());

        DECORATION_PALETTE.put("sculk_moss", Blocks.MOSS_CARPET.defaultBlockState());
        DECORATION_PALETTE.put("sculk_carpet", Blocks.SCULK_VEIN.defaultBlockState());
        DECORATION_PALETTE.put("copper_moss", Blocks.MOSS_CARPET.defaultBlockState());
        DECORATION_PALETTE.put("warped_moss", Blocks.WARPED_ROOTS.defaultBlockState());
        DECORATION_PALETTE.put("soul_vines", Blocks.TWISTING_VINES.defaultBlockState());
        DECORATION_PALETTE.put("ash_shrub", Blocks.DEAD_BUSH.defaultBlockState());
        DECORATION_PALETTE.put("void_shard", Blocks.END_ROD.defaultBlockState());
    }

    private static boolean isAboveWater(WorldGenLevel level, BlockPos groundPos) {
        return !level.getBlockState(groundPos.below()).getFluidState().isEmpty();
    }

    @Nullable
    private static net.minecraft.world.level.block.state.BlockState resolveDecoration(String name) {
        net.minecraft.world.level.block.state.BlockState mapped = DECORATION_PALETTE.get(name);
        if (mapped != null) return mapped;

        try {
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(name));
            return block != Blocks.AIR ? block.defaultBlockState() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static void applyDecorationsFromProfile(
                                                   WorldGenLevel level,
                                                   WorldgenProfile.DecorationProfile decorations,
                                                   net.minecraft.util.RandomSource random,
                                                   int chunkX,
                                                   int chunkZ) {
        int minX = chunkX * 16;
        int minZ = chunkZ * 16;

        for (String flower : decorations.flowers) {
            net.minecraft.world.level.block.state.BlockState state = resolveDecoration(flower);
            if (state == null) continue;

            int flowersToSpawn = Math.max(1, Math.round(16 * 16 * decorations.flowerFrequency * 0.03f));
            for (int i = 0; i < flowersToSpawn; i++) {
                int x = minX + random.nextInt(16);
                int z = minZ + random.nextInt(16);
                int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
                if (y <= level.getMinBuildHeight() + 1) continue;

                BlockPos pos = new BlockPos(x, y, z);
                if (level.ensureCanWrite(pos) && level.isEmptyBlock(pos) && !isAboveWater(level, pos)) {
                    level.setBlock(pos, state, 3);
                }
            }
        }

        for (String shrub : decorations.shrubs) {
            net.minecraft.world.level.block.state.BlockState state = resolveDecoration(shrub);
            if (state == null) continue;

            int shrubsToSpawn = Math.max(1, Math.round(16 * 16 * decorations.vegetationDensity * 0.03f));
            for (int i = 0; i < shrubsToSpawn; i++) {
                int x = minX + random.nextInt(16);
                int z = minZ + random.nextInt(16);
                int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
                if (y <= level.getMinBuildHeight() + 1) continue;

                BlockPos pos = new BlockPos(x, y, z);
                if (level.ensureCanWrite(pos) && level.isEmptyBlock(pos) && !isAboveWater(level, pos)) {
                    level.setBlock(pos, state, 3);
                }
            }
        }
    }

    private static void applyBiomeVegetation(WorldGenLevel level, WorldgenProfile.DecorationProfile profile,
                                             @Nullable BiomeLookup biomes, net.minecraft.util.RandomSource random,
                                             int chunkX, int chunkZ) {
        int minX = chunkX * 16;
        int minZ = chunkZ * 16;
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();

        int treeAttempts = Math.max(1, Math.round(profile.treeFrequency * 12 * 2.5f));
        for (int i = 0; i < treeAttempts; i++) {
            int x = minX + random.nextInt(16);
            int z = minZ + random.nextInt(16);
            BiomeKits.Kit kit = biomes == null ? null : BiomeKits.get(biomes.biomeIdAt(x, z));
            java.util.List<WorldgenProfile.TreeConfig> trees = kit != null ? kit.trees() : profile.trees;
            float mul = kit != null ? kit.treeMul() : 1.0f;
            if (trees == null || trees.isEmpty() || mul <= 0f || random.nextFloat() > mul / 2.5f) continue;

            WorldgenProfile.TreeConfig tree = trees.get(random.nextInt(trees.size()));
            if (random.nextFloat() > tree.frequency) continue;

            BlockPos base = groundPos(level, biomes, x, z);
            if (base == null || !level.ensureCanWrite(base) || !level.isEmptyBlock(base)) continue;
            if (isAboveWater(level, base) || underTree(level, base)) continue;
            placeTree(level, base, tree, random);
        }

        placeGroundCover(level, profile, biomes, random, minX, minZ, probe, true);
        placeGroundCover(level, profile, biomes, random, minX, minZ, probe, false);
    }

    @Nullable
    private static BlockPos groundPos(WorldGenLevel level, @Nullable BiomeLookup biomes, int x, int z) {
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= level.getMinBuildHeight() + 1) return null;
        BlockPos pos = new BlockPos(x, y, z);
        net.minecraft.world.level.block.state.BlockState below = level.getBlockState(pos.below());
        if (below.isAir()) return null;
        Block expected = biomes == null ? null : biomes.groundBlockAt(x, z);
        if (expected != null && below.getBlock() != expected) return null;
        return pos;
    }

    private static boolean underTree(WorldGenLevel level, BlockPos base) {
        for (int dy = 0; dy <= 3; dy++) {
            net.minecraft.world.level.block.state.BlockState s = level.getBlockState(base.above(dy));
            if (s.is(net.minecraft.tags.BlockTags.LEAVES) || s.is(net.minecraft.tags.BlockTags.LOGS)) return true;
        }
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            net.minecraft.world.level.block.state.BlockState s = level.getBlockState(base.relative(dir));
            if (s.is(net.minecraft.tags.BlockTags.LOGS)) return true;
        }
        return false;
    }

    private static void placeGroundCover(WorldGenLevel level, WorldgenProfile.DecorationProfile profile,
                                         @Nullable BiomeLookup biomes, net.minecraft.util.RandomSource random,
                                         int minX, int minZ, BlockPos.MutableBlockPos probe, boolean flowers) {
        float freq = flowers ? profile.flowerFrequency : profile.vegetationDensity;
        int attempts = Math.max(1, Math.round(256 * freq * 0.03f * 4));
        for (int i = 0; i < attempts; i++) {
            int x = minX + random.nextInt(16);
            int z = minZ + random.nextInt(16);
            BiomeKits.Kit kit = biomes == null ? null : BiomeKits.get(biomes.biomeIdAt(x, z));
            java.util.List<String> names = kit != null ? (flowers ? kit.flowers() : kit.shrubs()) :
                    (flowers ? profile.flowers : profile.shrubs);
            float mul = kit != null ? (flowers ? kit.flowerMul() : kit.shrubMul()) : 1.0f;
            if (names == null || names.isEmpty() || mul <= 0f || random.nextFloat() > mul / 2.0f) continue;

            net.minecraft.world.level.block.state.BlockState state = resolveDecoration(
                    names.get(random.nextInt(names.size())));
            if (state == null) continue;

            BlockPos pos = groundPos(level, biomes, x, z);
            if (pos != null && level.ensureCanWrite(pos) && level.isEmptyBlock(pos) && !isAboveWater(level, pos) &&
                    state.canSurvive(level, pos)) {
                level.setBlock(pos, state, 3);
            }
        }
    }

    private static net.minecraft.world.level.block.state.BlockState persistentLeaves(
                                                                                     net.minecraft.world.level.block.Block leaves) {
        return leaves.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
    }

    public static void applyTrees(
                                  WorldGenLevel level,
                                  java.util.List<WorldgenProfile.TreeConfig> trees,
                                  float treeFrequency,
                                  net.minecraft.util.RandomSource random,
                                  int chunkX,
                                  int chunkZ) {
        if (trees == null || trees.isEmpty() || treeFrequency <= 0f) return;

        int minX = chunkX * 16;
        int minZ = chunkZ * 16;

        int attempts = Math.max(1, Math.round(treeFrequency * 12));
        for (int i = 0; i < attempts; i++) {
            WorldgenProfile.TreeConfig tree = trees.get(random.nextInt(trees.size()));
            if (random.nextFloat() > tree.frequency) continue;

            int x = minX + random.nextInt(16);
            int z = minZ + random.nextInt(16);
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
            if (y <= level.getMinBuildHeight() + 1) continue;

            BlockPos base = new BlockPos(x, y, z);
            if (!level.ensureCanWrite(base) || !level.isEmptyBlock(base)) continue;
            if (level.getBlockState(base.below()).isAir() || isAboveWater(level, base)) continue;

            placeTree(level, base, tree, random);
        }
    }

    private static void placeTree(
                                  WorldGenLevel level, BlockPos base, WorldgenProfile.TreeConfig tree,
                                  net.minecraft.util.RandomSource random) {
        TreeBuilder.place(level, base, tree, random);
    }

    private static final int LAKE_LEVEL_TOLERANCE = 6;

    private static void applyLiquidFeatures(
                                            WorldGenLevel level,
                                            WorldgenProfile.LiquidProfile liquids,
                                            int chunkX,
                                            int chunkZ) {
        net.minecraft.util.RandomSource random = level.getRandom();

        if (random.nextFloat() < liquids.waterLakeFrequency) {
            placeLake(level, random, chunkX, chunkZ, liquids.waterLevel,
                    3 + random.nextInt(4), Blocks.WATER.defaultBlockState());
        }

        if (random.nextFloat() < liquids.lavaLakeFrequency) {
            placeLake(level, random, chunkX, chunkZ, liquids.lavaLevel,
                    2 + random.nextInt(3), Blocks.LAVA.defaultBlockState());
        }
    }

    private static void placeLake(
                                  WorldGenLevel level,
                                  net.minecraft.util.RandomSource random,
                                  int chunkX, int chunkZ,
                                  int targetLevel, int radius,
                                  net.minecraft.world.level.block.state.BlockState liquid) {
        int x = (chunkX * 16) + random.nextInt(16);
        int z = (chunkZ * 16) + random.nextInt(16);

        int centerSurfaceY = groundY(level, x, z);
        if (Math.abs(centerSurfaceY - targetLevel) > LAKE_LEVEL_TOLERANCE) return;

        int rim = radius + 1;
        int rimMin = Integer.MAX_VALUE;
        int rimMax = Integer.MIN_VALUE;
        for (int dx = -rim; dx <= rim; dx++) {
            for (int dz = -rim; dz <= rim; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > rim * rim || d2 <= radius * radius) continue;
                int y = groundY(level, x + dx, z + dz);
                if (!level.getBlockState(new BlockPos(x + dx, y, z + dz)).getFluidState().isEmpty()) return;
                rimMin = Math.min(rimMin, y);
                rimMax = Math.max(rimMax, y);
            }
        }
        if (rimMax - rimMin > 2) return;
        int surface = rimMin;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > radius) continue;

                int px = x + dx;
                int pz = z + dz;
                int top = groundY(level, px, pz);
                int depth = 1 + (int) Math.round((1.0 - dist / radius) * 2.0);

                for (int y = top; y > surface; y--) {
                    BlockPos pos = new BlockPos(px, y, pz);
                    if (level.ensureCanWrite(pos)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                }
                for (int y = surface; y > surface - depth; y--) {
                    BlockPos pos = new BlockPos(px, y, pz);
                    if (level.ensureCanWrite(pos)) level.setBlock(pos, liquid, 2);
                }
            }
        }
    }

    private static int groundY(WorldGenLevel level, int x, int z) {
        return level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
    }

    private static void applyProgressionWorldgen(
                                                 WorldGenLevel level,
                                                 WorldgenProfile profile,
                                                 String currentStage,
                                                 int chunkX,
                                                 int chunkZ) {
        if (profile.progression == null || profile.progression.stages == null) {
            return;
        }

        WorldgenProfile.ProgressionProfile.WorldgenStage stage = profile.progression.stages.get(currentStage);

        if (stage == null) return;

        if (!stage.newDecorations.isEmpty()) {
            applyStageDecorations(level, stage.newDecorations, chunkX, chunkZ);
        }
    }

    private static void applyStageDecorations(
                                              WorldGenLevel level,
                                              java.util.List<String> decorations,
                                              int chunkX,
                                              int chunkZ) {
        int minX = chunkX * 16;
        int minZ = chunkZ * 16;
        net.minecraft.util.RandomSource random = level.getRandom();

        for (String decoration : decorations) {
            int count = 2 + random.nextInt(3);
            for (int i = 0; i < count; i++) {
                int x = minX + random.nextInt(16);
                int z = minZ + random.nextInt(16);
                int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
                if (y <= level.getMinBuildHeight() + 1) continue;

                BlockPos pos = new BlockPos(x, y, z);
                if (level.ensureCanWrite(pos) && level.isEmptyBlock(pos)) {
                    try {
                        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(decoration));
                        if (block != Blocks.AIR) {
                            level.setBlock(pos, block.defaultBlockState(), 3);
                        }
                    } catch (Exception e) {

                    }
                }
            }
        }
    }
}
