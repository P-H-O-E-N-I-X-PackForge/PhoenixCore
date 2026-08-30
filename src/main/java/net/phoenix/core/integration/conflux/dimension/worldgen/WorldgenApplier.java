package net.phoenix.core.integration.conflux.dimension.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;

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
                         new  ResourceLocation(blocks.grass))
                        .defaultBlockState(), 3);

                    for (int i = 1; i < 4; i++) {
                        BlockPos dirtPos = grassPos.below(i);
                        if (level.ensureCanWrite(dirtPos)) {
                            level.setBlock(dirtPos,
                                net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                                    new ResourceLocation(blocks.dirt))
                                .defaultBlockState(), 3);
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
        net.phoenix.core.integration.conflux.dimension.DisciplineTheme theme =
            net.phoenix.core.integration.conflux.dimension.DisciplineThemeRegistry.getTheme(disciplineId);

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
        return new DisciplineWorldgenConfig("phoenix", ores, new DisciplineWorldgenConfig.BiomeBlockPalette(new java.util.HashMap<>()),
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
        return new DisciplineWorldgenConfig("void", ores, new DisciplineWorldgenConfig.BiomeBlockPalette(new java.util.HashMap<>()),
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

        return new DisciplineWorldgenConfig("default", ores, new DisciplineWorldgenConfig.BiomeBlockPalette(new java.util.HashMap<>()),
            new DisciplineWorldgenConfig.SurfaceRuleSet[0],
            new DisciplineWorldgenConfig.StructureSet(new java.util.ArrayList<>(), 1.0f),
            new DisciplineWorldgenConfig.DecorationConfig(1.0f, 0.5f, 0.3f, new java.util.HashMap<>()),
            new DisciplineWorldgenConfig.CaveConfig(1.0f, 1.0f, true));
    }

    public static void applyWorldgenProfile(
            WorldGenLevel level,
            WorldgenProfile profile,
            @Nullable String currentStage,
            int chunkX,
            int chunkZ) {

        if (profile == null) return;

        if (profile.decorations != null) {
            applyDecorationsFromProfile(level, profile.decorations, level.getRandom(), chunkX, chunkZ);
            applyTrees(level, profile.decorations.trees, profile.decorations.treeFrequency,
                    level.getRandom(), chunkX, chunkZ);
        }

        if (profile.liquids != null) {
            applyLiquidFeatures(level, profile.liquids, chunkX, chunkZ);
        }

        SmallStructures.maybePlace(level, profile.disciplineId, chunkX, chunkZ);

        if (currentStage != null && profile.progression != null) {
            applyProgressionWorldgen(level, profile, currentStage, chunkX, chunkZ);
        }
    }

    // Every discipline preset names its flowers/shrubs/special decorations after fictional
    // flavor blocks ("fire_flower", "sculk_moss", "ethereal_crystal"...) that were never real
    // registry entries - BuiltInRegistries.BLOCK.get() silently resolved every one of them to
    // Blocks.AIR, and the "!= AIR" guard then skipped placing anything at all. This is why
    // discipline worldgen read as barren/grandiose despite the presets looking planted: nothing
    // but trees was ever actually placed. Mapping each flavor name to a real, thematically close
    // vanilla block (matching the same idea as TREE_PALETTES below) makes them place something.
    private static final java.util.Map<String, net.minecraft.world.level.block.state.BlockState> DECORATION_PALETTE =
            new java.util.HashMap<>();
    static {
        DECORATION_PALETTE.put("fire_flower", Blocks.TORCHFLOWER.defaultBlockState());
        DECORATION_PALETTE.put("lava_rose", Blocks.WITHER_ROSE.defaultBlockState());
        DECORATION_PALETTE.put("sculk_flower", Blocks.GLOW_LICHEN.defaultBlockState());
        DECORATION_PALETTE.put("glowing_vine", Blocks.GLOW_LICHEN.defaultBlockState());
        // CHORUS_FLOWER only survives on end stone or an existing chorus plant, not ordinary
        // ground - wrong fit now that void's primary biome is a walkable grass meadow.
        DECORATION_PALETTE.put("void_flower", Blocks.ALLIUM.defaultBlockState());
        DECORATION_PALETTE.put("ethereal_crystal", Blocks.SMALL_AMETHYST_BUD.defaultBlockState());
        DECORATION_PALETTE.put("copper_flower", Blocks.DANDELION.defaultBlockState());
        DECORATION_PALETTE.put("warped_flower", Blocks.WARPED_ROOTS.defaultBlockState());
        DECORATION_PALETTE.put("soul_lantern", Blocks.SOUL_LANTERN.defaultBlockState());
        // HANGING_ROOTS needs a solid ceiling above it to survive - wrong fit for ground-level
        // placement in open terrain (it would just pop off immediately), so a floor-resting
        // decorative carpet stands in instead.
        DECORATION_PALETTE.put("sculk_moss", Blocks.MOSS_CARPET.defaultBlockState());
        DECORATION_PALETTE.put("sculk_carpet", Blocks.SCULK_VEIN.defaultBlockState());
        DECORATION_PALETTE.put("copper_moss", Blocks.MOSS_CARPET.defaultBlockState());
        DECORATION_PALETTE.put("warped_moss", Blocks.WARPED_ROOTS.defaultBlockState());
        DECORATION_PALETTE.put("soul_vines", Blocks.TWISTING_VINES.defaultBlockState());
        DECORATION_PALETTE.put("ash_shrub", Blocks.DEAD_BUSH.defaultBlockState());
        DECORATION_PALETTE.put("void_shard", Blocks.END_ROD.defaultBlockState());
    }

    // level.getHeight(MOTION_BLOCKING, ...) returns the water surface, not the ocean/river
    // floor, in any column an ocean or river covers (MOTION_BLOCKING treats liquid as
    // "blocking"). Every ground decoration placement below computes its spot from that height
    // and only guards against pure open air, so without this check flowers/shrubs/trees/
    // structures would place themselves floating directly on top of the water.
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

    // Each discipline's "primary" grass biome uses a real log+leaves pair so trees actually
    // read as foliage; the original exotic combos (solid canopy blocks like sculk/amethyst/
    // copper) are kept for the rarer accent-biome tree species instead of being the only option.
    private static final java.util.Map<String, net.minecraft.world.level.block.state.BlockState[]> TREE_PALETTES =
            new java.util.HashMap<>();
    static {
        // GT's own rubber tree, added as a low-frequency species in every discipline (see the
        // "rubber_tree" TreeConfig entries in DisciplineWorldgenPresets) so rubber - a GT early/
        // mid-game progression requirement - is reachable regardless of which discipline a team
        // picks, instead of being locked out entirely by their choice.
        net.minecraft.world.level.block.Block rubberLog = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("gtceu:rubber_log"));
        net.minecraft.world.level.block.Block rubberLeaves = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("gtceu:rubber_leaves"));
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
    }

    // Leaves placed directly (not via a proper log-distance scan) default to distance=7, which
    // reads as "too far from any log" to the game's leaf-decay tick and would strip these
    // canopies block by block after generation. Marking them persistent turns that check off.
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

        // treeFrequency (a per-discipline "how wooded is this place" dial) drives how many
        // planting attempts happen in this chunk; each species' own frequency is just its
        // relative share of those attempts, not a second multiplier on top of it - the old
        // "chance = tree.frequency * treeFrequency, attempts = round(chance*6)" formula made
        // both knobs shrink the same number twice, so even "wooded" presets landed under one
        // tree every several chunks.
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
            WorldGenLevel level, BlockPos base, WorldgenProfile.TreeConfig tree, net.minecraft.util.RandomSource random) {
        net.minecraft.world.level.block.state.BlockState[] palette =
                TREE_PALETTES.getOrDefault(tree.treeType, TREE_PALETTES.get("dead_tree"));
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
        int radius = 2;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radius * radius + 1) continue;
                    BlockPos pos = top.offset(dx, dy, dz);
                    if (level.ensureCanWrite(pos) && level.isEmptyBlock(pos)) {
                        level.setBlock(pos, canopy, 3);
                    }
                }
            }
        }
    }

    // A lake only makes sense where the actual local terrain sits near the liquid's intended
    // level - otherwise, now that terrain height genuinely varies (mixed flat/hilly regions,
    // per-biome height differences), a lake center landing over a cliff or valley the fixed
    // waterLevel/lavaLevel never accounted for would place liquid nowhere near the real ground.
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

        int centerSurfaceY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
        if (Math.abs(centerSurfaceY - targetLevel) > LAKE_LEVEL_TOLERANCE) return;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;

                int px = x + dx;
                int pz = z + dz;
                // Follow each column's own real surface height rather than the lake's fixed
                // target level, so the liquid always sits on actual ground instead of floating
                // over whatever the terrain happens to do at that spot.
                int columnSurfaceY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, px, pz) - 1;
                BlockPos pos = new BlockPos(px, columnSurfaceY, pz);
                if (level.ensureCanWrite(pos)) {
                    level.setBlock(pos, liquid, 3);
                }
            }
        }
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

        WorldgenProfile.ProgressionProfile.WorldgenStage stage =
            profile.progression.stages.get(currentStage);

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
