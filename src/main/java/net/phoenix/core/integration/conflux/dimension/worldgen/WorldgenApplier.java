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

                BlockPos surfacePos = new BlockPos(x, y, z);
                BlockPos grassPos = surfacePos.above();

                if (level.ensureCanWrite(grassPos)) {

                    level.setBlock(grassPos,
                            net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                                    new ResourceLocation(blocks.grass))
                                    .defaultBlockState(),
                            3);

                    for (int i = 1; i < 4; i++) {
                        BlockPos dirtPos = grassPos.below(i);
                        if (level.ensureCanWrite(dirtPos)) {
                            level.setBlock(dirtPos,
                                    net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                                            new ResourceLocation(blocks.dirt))
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

                BlockPos pos = new BlockPos(x, y, z);
                if (level.ensureCanWrite(pos) && level.isEmptyBlock(pos)) {
                    try {
                        var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                                new ResourceLocation(decoration));
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
                        new ResourceLocation("minecraft:iron_ore"),
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
                        new ResourceLocation("minecraft:iron_ore"),
                        8, 10, 0, 64, "minecraft:stone"),
                new DisciplineWorldgenConfig.OreConfig(
                        new ResourceLocation("minecraft:coal_ore"),
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

            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
            BlockPos base = new BlockPos(x, y, z);
            if (!level.ensureCanWrite(base) || !level.isEmptyBlock(base)) continue;
            if (level.getBlockState(base.below()).isAir() || isAboveWater(level, base)) continue;
            placeTree(level, base, tree, random);
        }

        placeGroundCover(level, profile, biomes, random, minX, minZ, probe, true);
        placeGroundCover(level, profile, biomes, random, minX, minZ, probe, false);
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

            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
            BlockPos pos = probe.set(x, y, z).immutable();
            if (level.ensureCanWrite(pos) && level.isEmptyBlock(pos) && !isAboveWater(level, pos) &&
                    state.canSurvive(level, pos)) {
                level.setBlock(pos, state, 3);
            }
        }
    }

    private enum Shape {
        ROUND,
        
        CONE,
        
        SPIRE,
        
        UMBRELLA,
        
        STUMP
    }

    private static final java.util.Map<String, Shape> TREE_SHAPES = new java.util.HashMap<>();
    static {
        TREE_SHAPES.put("fire_tree", Shape.UMBRELLA);
        TREE_SHAPES.put("dead_tree", Shape.STUMP);
        TREE_SHAPES.put("charred_stump", Shape.STUMP);
        TREE_SHAPES.put("rusted_pylon", Shape.STUMP);
        TREE_SHAPES.put("bone_tree", Shape.STUMP);
        TREE_SHAPES.put("obsidian_spire", Shape.SPIRE);
        TREE_SHAPES.put("basalt_column", Shape.SPIRE);
        TREE_SHAPES.put("sculk_spire", Shape.SPIRE);
        TREE_SHAPES.put("crystalline_tree", Shape.SPIRE);
        TREE_SHAPES.put("antenna", Shape.SPIRE);
        TREE_SHAPES.put("amethyst_spire", Shape.CONE);
        TREE_SHAPES.put("thicket_pine", Shape.CONE);
        TREE_SHAPES.put("frost_pine", Shape.CONE);
        TREE_SHAPES.put("mushroom_tree", Shape.UMBRELLA);
        TREE_SHAPES.put("marsh_tree", Shape.UMBRELLA);
        TREE_SHAPES.put("warped_tree", Shape.UMBRELLA);
        TREE_SHAPES.put("glow_tree", Shape.UMBRELLA);
    }

    private static final java.util.Map<String, net.minecraft.world.level.block.state.BlockState[]> TREE_PALETTES = new java.util.HashMap<>();
    static {

        net.minecraft.world.level.block.Block rubberLog = BuiltInRegistries.BLOCK
                .get(ResourceLocation.parse("gtceu:rubber_log"));
        net.minecraft.world.level.block.Block rubberLeaves = BuiltInRegistries.BLOCK
                .get(ResourceLocation.parse("gtceu:rubber_leaves"));
        TREE_PALETTES.put("rubber_tree", new net.minecraft.world.level.block.state.BlockState[] {
                rubberLog.defaultBlockState(), persistentLeaves(rubberLeaves) });

        TREE_PALETTES.put("phoenix_oak", new net.minecraft.world.level.block.state.BlockState[] {
                Blocks.OAK_LOG.defaultBlockState(), persistentLeaves(Blocks.OAK_LEAVES) });
        TREE_PALETTES.put("dead_tree", new net.minecraft.world.level.block.state.BlockState[] {
                Blocks.CRIMSON_STEM.defaultBlockState(), Blocks.NETHER_WART_BLOCK.defaultBlockState() });
        TREE_PALETTES.put("fire_tree", new net.minecraft.world.level.block.state.BlockState[] {
                Blocks.CRIMSON_STEM.defaultBlockState(), Blocks.SHROOMLIGHT.defaultBlockState() });
        TREE_PALETTES.put("moss_oak", new net.minecraft.world.level.block.state.BlockState[] {
                Blocks.DARK_OAK_LOG.defaultBlockState(), persistentLeaves(Blocks.DARK_OAK_LEAVES) });
        TREE_PALETTES.put("sculk_tree", new net.minecraft.world.level.block.state.BlockState[] {
                Blocks.DARK_OAK_LOG.defaultBlockState(), Blocks.SCULK.defaultBlockState() });
        TREE_PALETTES.put("cherry_tree", new net.minecraft.world.level.block.state.BlockState[] {
                Blocks.CHERRY_LOG.defaultBlockState(), persistentLeaves(Blocks.CHERRY_LEAVES) });
        TREE_PALETTES.put("crystalline_tree", new net.minecraft.world.level.block.state.BlockState[] {
                Blocks.DARK_OAK_LOG.defaultBlockState(), Blocks.AMETHYST_BLOCK.defaultBlockState() });
        TREE_PALETTES.put("birch_grove", new net.minecraft.world.level.block.state.BlockState[] {
                Blocks.BIRCH_LOG.defaultBlockState(), persistentLeaves(Blocks.BIRCH_LEAVES) });
        TREE_PALETTES.put("metal_tree", new net.minecraft.world.level.block.state.BlockState[] {
                Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), Blocks.COPPER_BLOCK.defaultBlockState() });
        TREE_PALETTES.put("warped_tree", new net.minecraft.world.level.block.state.BlockState[] {
                Blocks.WARPED_STEM.defaultBlockState(), Blocks.WARPED_WART_BLOCK.defaultBlockState() });
        TREE_PALETTES.put("charred_stump", new net.minecraft.world.level.block.state.BlockState[] {
                Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(), Blocks.AIR.defaultBlockState() });
        TREE_PALETTES.put("obsidian_spire", new net.minecraft.world.level.block.state.BlockState[] {
                Blocks.OBSIDIAN.defaultBlockState(), Blocks.MAGMA_BLOCK.defaultBlockState() });
        TREE_PALETTES.put("basalt_column", new net.minecraft.world.level.block.state.BlockState[] {
                Blocks.BASALT.defaultBlockState(), Blocks.BLACKSTONE.defaultBlockState() });
        TREE_PALETTES.put("sculk_spire", new net.minecraft.world.level.block.state.BlockState[] {
                Blocks.DEEPSLATE.defaultBlockState(), Blocks.SCULK.defaultBlockState() });
        TREE_PALETTES.put("mushroom_tree", new net.minecraft.world.level.block.state.BlockState[] {
                Blocks.MUSHROOM_STEM.defaultBlockState(), Blocks.BROWN_MUSHROOM_BLOCK.defaultBlockState() });
        TREE_PALETTES.put("marsh_tree", new net.minecraft.world.level.block.state.BlockState[] {
                Blocks.MANGROVE_LOG.defaultBlockState(), persistentLeaves(Blocks.MANGROVE_LEAVES) });
        TREE_PALETTES.put("thicket_pine", new net.minecraft.world.level.block.state.BlockState[] {
                Blocks.DARK_OAK_LOG.defaultBlockState(), persistentLeaves(Blocks.DARK_OAK_LEAVES) });
        TREE_PALETTES.put("amethyst_spire", new net.minecraft.world.level.block.state.BlockState[] {
                Blocks.QUARTZ_PILLAR.defaultBlockState(), Blocks.AMETHYST_BLOCK.defaultBlockState() });
        TREE_PALETTES.put("frost_pine", new net.minecraft.world.level.block.state.BlockState[] {
                Blocks.SPRUCE_LOG.defaultBlockState(), Blocks.SNOW_BLOCK.defaultBlockState() });
        TREE_PALETTES.put("antenna", new net.minecraft.world.level.block.state.BlockState[] {
                Blocks.IRON_BARS.defaultBlockState(), Blocks.LIGHTNING_ROD.defaultBlockState() });
        TREE_PALETTES.put("rusted_pylon", new net.minecraft.world.level.block.state.BlockState[] {
                Blocks.OXIDIZED_COPPER.defaultBlockState(), Blocks.AIR.defaultBlockState() });
        TREE_PALETTES.put("bone_tree", new net.minecraft.world.level.block.state.BlockState[] {
                Blocks.BONE_BLOCK.defaultBlockState(), Blocks.AIR.defaultBlockState() });
        TREE_PALETTES.put("glow_tree", new net.minecraft.world.level.block.state.BlockState[] {
                Blocks.CRIMSON_STEM.defaultBlockState(), Blocks.SHROOMLIGHT.defaultBlockState() });
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

            BlockPos base = new BlockPos(x, y, z);
            if (!level.ensureCanWrite(base) || !level.isEmptyBlock(base)) continue;
            if (level.getBlockState(base.below()).isAir() || isAboveWater(level, base)) continue;

            placeTree(level, base, tree, random);
        }
    }

    private static void placeTree(
                                  WorldGenLevel level, BlockPos base, WorldgenProfile.TreeConfig tree,
                                  net.minecraft.util.RandomSource random) {
        net.minecraft.world.level.block.state.BlockState[] palette = TREE_PALETTES.getOrDefault(tree.treeType,
                TREE_PALETTES.get("dead_tree"));
        net.minecraft.world.level.block.state.BlockState trunk = palette[0];
        net.minecraft.world.level.block.state.BlockState canopy = palette[1];

        int span = Math.max(0, tree.maxHeight - tree.minHeight);
        int height = tree.minHeight + (span > 0 ? random.nextInt(span + 1) : 0);
        if (height < 1) return;

        for (int i = 0; i < height; i++) {
            BlockPos pos = base.above(i);
            if (level.ensureCanWrite(pos)) {
                level.setBlock(pos, trunk, 3);
            }
        }

        BlockPos top = base.above(height);
        boolean hasCanopy = !canopy.isAir();
        switch (TREE_SHAPES.getOrDefault(tree.treeType, Shape.ROUND)) {
            case STUMP -> {
                
                int branches = 1 + random.nextInt(2);
                for (int b = 0; b < branches && height > 2; b++) {
                    BlockPos stub = base.above(1 + random.nextInt(height - 1))
                            .relative(net.minecraft.core.Direction.Plane.HORIZONTAL.getRandomDirection(random));
                    if (level.ensureCanWrite(stub) && level.isEmptyBlock(stub)) level.setBlock(stub, trunk, 3);
                }
            }
            case SPIRE -> {
                if (!hasCanopy) break;
                put(level, top, canopy);
                for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                    put(level, top.below().relative(dir), canopy);
                }
            }
            case CONE -> {
                if (!hasCanopy) break;
                for (int dy = 2; dy <= height + 1; dy++) {
                    int r = Math.min(3, (height + 2 - dy) / 2);
                    for (int dx = -r; dx <= r; dx++) {
                        for (int dz = -r; dz <= r; dz++) {
                            if (dx * dx + dz * dz > r * r + r / 2) continue;
                            put(level, base.above(dy).offset(dx, 0, dz), canopy);
                        }
                    }
                }
            }
            case UMBRELLA -> {
                if (!hasCanopy) break;
                for (int dx = -3; dx <= 3; dx++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        int d2 = dx * dx + dz * dz;
                        if (d2 <= 10) put(level, top.offset(dx, 0, dz), canopy);
                        if (d2 >= 5 && d2 <= 9) put(level, top.offset(dx, -1, dz), canopy);
                    }
                }
            }
            default -> {
                if (!hasCanopy) break;
                int radius = 2;
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -radius; dz <= radius; dz++) {
                            if (dx * dx + dy * dy + dz * dz > radius * radius + 1) continue;
                            put(level, top.offset(dx, dy, dz), canopy);
                        }
                    }
                }
            }
        }
    }

    private static void put(WorldGenLevel level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        if (level.ensureCanWrite(pos) && level.isEmptyBlock(pos)) level.setBlock(pos, state, 3);
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

                BlockPos pos = new BlockPos(x, y, z);
                if (level.ensureCanWrite(pos) && level.isEmptyBlock(pos)) {
                    try {
                        Block block = BuiltInRegistries.BLOCK.get(new ResourceLocation(decoration));
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
