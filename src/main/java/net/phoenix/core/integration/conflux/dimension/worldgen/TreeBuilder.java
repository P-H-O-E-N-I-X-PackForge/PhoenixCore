package net.phoenix.core.integration.conflux.dimension.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class TreeBuilder {

    private TreeBuilder() {}

    private enum Shape {
        ROUND,
        CONE,
        SPIRE,
        UMBRELLA,
        STUMP
    }

    private record Style(Shape shape, BlockState trunk, BlockState[] leaves, BlockState[] accents, float accentChance,
                         boolean leafy) {}

    private static final Map<String, Style> STYLES = new HashMap<>();

    private static BlockState b(Block block) {
        return block.defaultBlockState();
    }

    private static BlockState[] arr(Block... blocks) {
        BlockState[] out = new BlockState[blocks.length];
        for (int i = 0; i < blocks.length; i++) out[i] = b(blocks[i]);
        return out;
    }

    private static final BlockState[] NONE = new BlockState[0];

    private static void leafy(String type, Shape shape, Block trunk, Block[] leaves, Block[] accents,
                              float accentChance) {
        STYLES.put(type, new Style(shape, b(trunk), arr(leaves), arr(accents), accentChance, true));
    }

    private static void solid(String type, Shape shape, Block trunk, Block crown) {
        STYLES.put(type, new Style(shape, b(trunk), arr(crown), NONE, 0f, false));
    }

    private static Block resolve(String id, Block fallback) {
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
        return block == Blocks.AIR ? fallback : block;
    }

    static {
        leafy("rubber_tree", Shape.ROUND, resolve("gtceu:rubber_log", Blocks.OAK_LOG),
                new Block[] { resolve("gtceu:rubber_leaves", Blocks.OAK_LEAVES) }, new Block[0], 0f);
        leafy("phoenix_oak", Shape.ROUND, Blocks.OAK_LOG, new Block[] { Blocks.OAK_LEAVES, Blocks.AZALEA_LEAVES },
                new Block[0], 0f);
        leafy("fire_tree", Shape.UMBRELLA, Blocks.CRIMSON_STEM, new Block[] { Blocks.ACACIA_LEAVES },
                new Block[] { Blocks.SHROOMLIGHT, Blocks.NETHER_WART_BLOCK }, 0.22f);
        leafy("dead_tree", Shape.STUMP, Blocks.CRIMSON_STEM, new Block[] { Blocks.ACACIA_LEAVES },
                new Block[] { Blocks.NETHER_WART_BLOCK }, 0.3f);
        leafy("moss_oak", Shape.ROUND, Blocks.DARK_OAK_LOG,
                new Block[] { Blocks.DARK_OAK_LEAVES, Blocks.AZALEA_LEAVES },
                new Block[] { Blocks.FLOWERING_AZALEA_LEAVES }, 0.1f);
        leafy("sculk_tree", Shape.ROUND, Blocks.DARK_OAK_LOG,
                new Block[] { Blocks.DARK_OAK_LEAVES, Blocks.MANGROVE_LEAVES }, new Block[] { Blocks.SCULK }, 0.12f);
        leafy("cherry_tree", Shape.ROUND, Blocks.CHERRY_LOG, new Block[] { Blocks.CHERRY_LEAVES }, new Block[0], 0f);
        leafy("crystalline_tree", Shape.ROUND, Blocks.DARK_OAK_LOG,
                new Block[] { Blocks.FLOWERING_AZALEA_LEAVES, Blocks.AZALEA_LEAVES },
                new Block[] { Blocks.AMETHYST_BLOCK }, 0.16f);
        leafy("birch_grove", Shape.ROUND, Blocks.BIRCH_LOG, new Block[] { Blocks.BIRCH_LEAVES }, new Block[0], 0f);
        leafy("metal_tree", Shape.CONE, Blocks.STRIPPED_SPRUCE_LOG, new Block[] { Blocks.SPRUCE_LEAVES },
                new Block[] { Blocks.COPPER_BLOCK, Blocks.EXPOSED_COPPER }, 0.12f);
        leafy("warped_tree", Shape.UMBRELLA, Blocks.WARPED_STEM,
                new Block[] { Blocks.JUNGLE_LEAVES, Blocks.MANGROVE_LEAVES },
                new Block[] { Blocks.WARPED_WART_BLOCK }, 0.4f);
        leafy("marsh_tree", Shape.UMBRELLA, Blocks.MANGROVE_LOG, new Block[] { Blocks.MANGROVE_LEAVES },
                new Block[0], 0f);
        leafy("thicket_pine", Shape.CONE, Blocks.DARK_OAK_LOG,
                new Block[] { Blocks.DARK_OAK_LEAVES, Blocks.SPRUCE_LEAVES }, new Block[0], 0f);
        leafy("frost_pine", Shape.CONE, Blocks.SPRUCE_LOG, new Block[] { Blocks.SPRUCE_LEAVES },
                new Block[] { Blocks.SNOW_BLOCK }, 0.25f);
        leafy("glow_tree", Shape.UMBRELLA, Blocks.CRIMSON_STEM, new Block[] { Blocks.JUNGLE_LEAVES },
                new Block[] { Blocks.SHROOMLIGHT }, 0.3f);

        STYLES.put("mushroom_tree", new Style(Shape.UMBRELLA, b(Blocks.MUSHROOM_STEM),
                arr(Blocks.BROWN_MUSHROOM_BLOCK), arr(Blocks.RED_MUSHROOM_BLOCK), 0.2f, false));
        solid("amethyst_spire", Shape.CONE, Blocks.QUARTZ_PILLAR, Blocks.AMETHYST_BLOCK);
        solid("obsidian_spire", Shape.SPIRE, Blocks.OBSIDIAN, Blocks.MAGMA_BLOCK);
        solid("basalt_column", Shape.SPIRE, Blocks.BASALT, Blocks.BLACKSTONE);
        solid("sculk_spire", Shape.SPIRE, Blocks.DEEPSLATE, Blocks.SCULK);
        solid("antenna", Shape.SPIRE, Blocks.IRON_BARS, Blocks.LIGHTNING_ROD);
        solid("charred_stump", Shape.STUMP, Blocks.STRIPPED_DARK_OAK_LOG, Blocks.AIR);
        solid("rusted_pylon", Shape.STUMP, Blocks.OXIDIZED_COPPER, Blocks.AIR);
        solid("bone_tree", Shape.STUMP, Blocks.BONE_BLOCK, Blocks.AIR);
    }

    private record Claim(int x, int y, int z, int height, int reach) {}

    private static final int REACH = 4;
    private static final int LEDGER_LIMIT = 40_000;

    private static final Map<String, Map<Long, CopyOnWriteArrayList<Claim>>> LEDGER = new ConcurrentHashMap<>();

    private static long chunkKey(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
    }

    private static boolean overlaps(Claim c, int x, int y, int z, int reach) {
        int r = c.reach() + reach;
        return Math.abs(x - c.x()) <= r && Math.abs(z - c.z()) <= r && y >= c.y() - 1 && y <= c.y() + c.height() + 4;
    }

    private static boolean blocked(String dimension, int x, int y, int z, int height) {
        Map<Long, CopyOnWriteArrayList<Claim>> chunks = LEDGER.get(dimension);
        if (chunks == null) return false;
        int cx = x >> 4, cz = z >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                var list = chunks.get(chunkKey(cx + dx, cz + dz));
                if (list == null) continue;
                for (Claim c : list) {

                    if (overlaps(c, x, y, z, 1) || overlaps(c, x, y + height, z, 1)) return true;
                }
            }
        }
        return false;
    }

    private static void claim(String dimension, int x, int y, int z, int height) {
        Map<Long, CopyOnWriteArrayList<Claim>> chunks = LEDGER.computeIfAbsent(dimension,
                k -> new ConcurrentHashMap<>());
        if (chunks.size() > LEDGER_LIMIT) chunks.clear();
        chunks.computeIfAbsent(chunkKey(x >> 4, z >> 4), k -> new CopyOnWriteArrayList<>())
                .add(new Claim(x, y, z, height, REACH));
    }

    public static boolean covers(String dimension, int x, int y, int z) {
        Map<Long, CopyOnWriteArrayList<Claim>> chunks = LEDGER.get(dimension);
        if (chunks == null) return false;
        var list = chunks.get(chunkKey(x >> 4, z >> 4));
        if (list == null) return false;
        for (Claim c : list) {
            if (Math.abs(x - c.x()) <= c.reach() && Math.abs(z - c.z()) <= c.reach() && y >= c.y() &&
                    y <= c.y() + c.height() + 4) {
                return true;
            }
        }
        return false;
    }

    public static boolean isFoliage(BlockState state) {
        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.is(BlockTags.WART_BLOCKS)) return true;
        Block block = state.getBlock();
        return block == Blocks.SHROOMLIGHT || block == Blocks.BROWN_MUSHROOM_BLOCK ||
                block == Blocks.RED_MUSHROOM_BLOCK || block == Blocks.MUSHROOM_STEM ||
                block == Blocks.COPPER_BLOCK || block == Blocks.EXPOSED_COPPER || block == Blocks.OXIDIZED_COPPER ||
                block == Blocks.BONE_BLOCK || block == Blocks.IRON_BARS || block == Blocks.LIGHTNING_ROD ||
                block == Blocks.QUARTZ_PILLAR;
    }

    public static void place(WorldGenLevel level, BlockPos base, WorldgenProfile.TreeConfig tree,
                             RandomSource random) {
        Style style = STYLES.getOrDefault(tree.treeType, STYLES.get("dead_tree"));

        int span = Math.max(0, tree.maxHeight - tree.minHeight);
        int height = tree.minHeight + (span > 0 ? random.nextInt(span + 1) : 0);
        if (height < 1) return;

        BlockState below = level.getBlockState(base.below());
        if (below.isAir() || isFoliage(below)) return;
        String dimension = level.getLevel().dimension().location().toString();
        if (blocked(dimension, base.getX(), base.getY(), base.getZ(), height)) return;
        claim(dimension, base.getX(), base.getY(), base.getZ(), height);

        BlockState leaf = style.leaves().length == 0 ? Blocks.AIR.defaultBlockState() :
                style.leaves()[random.nextInt(style.leaves().length)];
        boolean leafyShape = style.shape() == Shape.ROUND || style.shape() == Shape.UMBRELLA ||
                style.shape() == Shape.CONE;

        BlockPos top = growTrunk(level, base, height, style, leafyShape, random);
        boolean crown = !leaf.isAir();
        switch (style.shape()) {
            case STUMP -> stump(level, base, height, style, random);
            case SPIRE -> {
                if (!crown) break;
                put(level, top, crownBlock(style, leaf, random));
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    put(level, top.below().relative(dir), crownBlock(style, leaf, random));
                }
            }
            case CONE -> {
                if (crown) cone(level, top, height, style, leaf, random);
            }
            case UMBRELLA -> {
                if (crown) umbrella(level, top, style, leaf, random);
            }
            default -> {
                if (crown) round(level, top, height, style, leaf, random);
            }
        }
    }

    private static BlockPos growTrunk(WorldGenLevel level, BlockPos base, int height, Style style,
                                      boolean leafyShape, RandomSource random) {
        Direction lean = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        boolean leans = leafyShape && height >= 5 && random.nextInt(100) < 28;
        boolean thick = leafyShape && style.shape() != Shape.CONE && height >= 9 && random.nextInt(100) < 40;
        int firstBend = Math.max(2, height / 3);
        int secondBend = Math.max(firstBend + 2, (height * 2) / 3);
        int shiftX = 0, shiftZ = 0;
        BlockPos last = base;
        for (int i = 0; i < height; i++) {
            if (leans && (i == firstBend || i == secondBend)) {
                shiftX += lean.getStepX();
                shiftZ += lean.getStepZ();
            }
            BlockPos pos = base.offset(shiftX, i, shiftZ);
            put(level, pos, style.trunk(), true);
            if (thick && i < height * 6 / 10) {
                put(level, pos.east(), style.trunk(), true);
                put(level, pos.south(), style.trunk(), true);
                put(level, pos.east().south(), style.trunk(), true);
            }
            last = pos;
        }
        return last.above();
    }

    private static void stump(WorldGenLevel level, BlockPos base, int height, Style style, RandomSource random) {
        int branches = 1 + random.nextInt(2);
        for (int i = 0; i < branches && height > 2; i++) {
            BlockPos stub = base.above(1 + random.nextInt(height - 1))
                    .relative(Direction.Plane.HORIZONTAL.getRandomDirection(random));
            if (!level.ensureCanWrite(stub) || !level.isEmptyBlock(stub)) continue;
            level.setBlock(stub, style.trunk(), 3);

            if (style.leafy() && style.leaves().length > 0 && random.nextInt(100) < 55) {
                BlockState tuft = style.leaves()[random.nextInt(style.leaves().length)];
                put(level, stub.above(), asLeaf(tuft));
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    if (random.nextInt(3) == 0) put(level, stub.relative(dir), asLeaf(tuft));
                }
            }
        }
    }

    private static void round(WorldGenLevel level, BlockPos top, int height, Style style, BlockState leaf,
                              RandomSource random) {
        int big = height >= 8 ? 3 : 2;
        switch (random.nextInt(4)) {
            case 0 -> ellipsoid(level, top, big, Math.max(2, big - 1), big, style, leaf, random);
            case 1 -> {
                boolean wide = random.nextBoolean();
                ellipsoid(level, top, wide ? big + 1 : big, big - 1, wide ? big : big + 1, style, leaf, random);
            }
            case 2 -> {

                ellipsoid(level, top.below(1), big + 1, 1, big + 1, style, leaf, random);
                ellipsoid(level, top.above(1), big, 1, big, style, leaf, random);
                ellipsoid(level, top.above(3), Math.max(1, big - 1), 1, Math.max(1, big - 1), style, leaf, random);
            }
            default -> {

                Direction side = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                ellipsoid(level, top, big, big - 1, big, style, leaf, random);
                BlockPos second = top.relative(side, big - 1).below(1);
                ellipsoid(level, second, 2, 2, 2, style, leaf, random);
                ellipsoid(level, top.above(2), 1, 1, 1, style, leaf, random);
            }
        }
    }

    private static void umbrella(WorldGenLevel level, BlockPos top, Style style, BlockState leaf,
                                 RandomSource random) {
        switch (random.nextInt(3)) {
            case 0 -> {

                ellipsoid(level, top, 4, 1, 4, style, leaf, random);
                for (int dx = -4; dx <= 4; dx++) {
                    for (int dz = -4; dz <= 4; dz++) {
                        int d2 = dx * dx + dz * dz;
                        if (d2 >= 10 && d2 <= 16) put(level, top.offset(dx, -1, dz), crownBlock(style, leaf, random));
                    }
                }
            }
            case 1 -> {

                ellipsoid(level, top.below(1), 4, 1, 4, style, leaf, random);
                ellipsoid(level, top, 3, 1, 3, style, leaf, random);
                ellipsoid(level, top.above(1), 2, 1, 2, style, leaf, random);
                put(level, top.above(2), crownBlock(style, leaf, random));
            }
            default -> {

                ellipsoid(level, top, 3, 1, 3, style, leaf, random);
                for (int dx = -3; dx <= 3; dx++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        int d2 = dx * dx + dz * dz;
                        if (d2 < 6 || d2 > 10 || random.nextInt(100) >= 55) continue;
                        int drop = 1 + random.nextInt(3);
                        for (int k = 1; k <= drop; k++) {
                            put(level, top.offset(dx, -k, dz), crownBlock(style, leaf, random));
                        }
                    }
                }
            }
        }
    }

    private static void cone(WorldGenLevel level, BlockPos top, int height, Style style, BlockState leaf,
                             RandomSource random) {
        BlockPos base = top.below(height);
        int build = random.nextInt(3);
        int maxR = build == 1 ? 2 : build == 2 ? 3 : Math.min(4, 2 + height / 4);
        for (int dy = 2; dy <= height + 1; dy++) {
            int r;
            if (build == 2) {

                int cycle = (dy - 2) % 4;
                r = cycle == 3 ? 0 : 3 - cycle;
                if (dy > 2 + (height - 1) * 0.6) r -= 1;
            } else {
                r = Math.min(maxR, (height + 2 - dy) / 2) + (dy % 2 == 0 && build == 0 ? 1 : 0);
                r = Math.min(r, maxR);
            }
            if (r <= 0) continue;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz > r * r + r / 2) continue;
                    put(level, base.above(dy).offset(dx, 0, dz), crownBlock(style, leaf, random));
                }
            }
        }
        put(level, top.above(1), crownBlock(style, leaf, random));
    }

    private static void ellipsoid(WorldGenLevel level, BlockPos center, int rx, int ry, int rz, Style style,
                                  BlockState leaf, RandomSource random) {
        double ax = Math.max(rx, 0.5) + 0.35, ay = Math.max(ry, 0.5) + 0.35, az = Math.max(rz, 0.5) + 0.35;
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dy = -ry; dy <= ry; dy++) {
                for (int dz = -rz; dz <= rz; dz++) {
                    double d = (dx * dx) / (ax * ax) + (dy * dy) / (ay * ay) + (dz * dz) / (az * az);
                    if (d > 1.0) continue;
                    if (d > 0.7 && random.nextInt(100) < 22) continue;
                    put(level, center.offset(dx, dy, dz), crownBlock(style, leaf, random));
                }
            }
        }
    }

    private static BlockState crownBlock(Style style, BlockState leaf, RandomSource random) {
        if (style.accents().length > 0 && random.nextFloat() < style.accentChance()) {
            return style.accents()[random.nextInt(style.accents().length)];
        }
        if (style.leaves().length > 1 && random.nextInt(100) < 15) {
            return asLeaf(style.leaves()[random.nextInt(style.leaves().length)]);
        }
        return asLeaf(leaf);
    }

    private static BlockState asLeaf(BlockState state) {
        return state.getBlock() instanceof LeavesBlock ? state.setValue(LeavesBlock.PERSISTENT, true) : state;
    }

    private static void put(WorldGenLevel level, BlockPos pos, BlockState state) {
        put(level, pos, state, false);
    }

    private static void put(WorldGenLevel level, BlockPos pos, BlockState state, boolean overwrite) {
        if (!level.ensureCanWrite(pos)) return;
        if (overwrite || level.isEmptyBlock(pos)) level.setBlock(pos, state, 3);
    }
}
