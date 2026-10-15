package net.phoenix.core.integration.conflux.dimension.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.List;

public final class UndergroundBiomes {

    private static final int FLOOR_OF_THE_DEEP = 12;
    private static final int BELOW_SURFACE = 6;

    private static final int ROOF = 10;

    private static final double CHAMBER_THRESHOLD = 0.42;

    private static final double TUNNEL_WIDTH = 0.045;
    private static final int SHAFT_DEPTH = 70;

    private record Decor(BlockState state, float chance, Block... onlyOn) {}

    private record Spire(BlockState block, int minLength, int maxLength, float chance) {}

    private record Giant(BlockState stem, BlockState cap, BlockState glow, int minHeight, int maxHeight, int capRadius,
                         float chance) {}

    private record Cavern(String id, int minY, int maxY, BlockState[] floor, BlockState[] ceiling,
                          List<Decor> floorDecor,
                          List<Decor> ceilingDecor, List<Spire> spires, List<Spire> stalactites, Giant giant,
                          BlockState pond, float pondChance) {}

    private static BlockState lichenUp() {
        return Blocks.GLOW_LICHEN.defaultBlockState().setValue(MultifaceBlock.getFaceProperty(Direction.UP), true);
    }

    private static BlockState veinUp() {
        return Blocks.SCULK_VEIN.defaultBlockState().setValue(MultifaceBlock.getFaceProperty(Direction.UP), true);
    }

    private static BlockState veinDown() {
        return Blocks.SCULK_VEIN.defaultBlockState().setValue(MultifaceBlock.getFaceProperty(Direction.DOWN), true);
    }

    private static BlockState clusterDown() {
        return Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.DOWN);
    }

    private static BlockState rodDown() {
        return Blocks.END_ROD.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.DOWN);
    }

    private static BlockState hangingLantern(boolean soul) {
        return (soul ? Blocks.SOUL_LANTERN : Blocks.LANTERN).defaultBlockState().setValue(LanternBlock.HANGING, true);
    }

    private static BlockState s(Block block) {
        return block.defaultBlockState();
    }

    private static BlockState[] mix(Block... blocks) {
        BlockState[] out = new BlockState[blocks.length];
        for (int i = 0; i < blocks.length; i++) out[i] = blocks[i].defaultBlockState();
        return out;
    }

    private static Decor decor(BlockState state, float chance, Block... onlyOn) {
        return new Decor(state, chance, onlyOn);
    }

    private static Spire spire(Block block, int min, int max, float chance) {
        return new Spire(block.defaultBlockState(), min, max, chance);
    }

    private static List<Cavern> cavernsOf(String dimension) {
        return switch (dimension) {
            case "phoenix" -> List.of(
                    new Cavern("magma_cavern", -50, 40,
                            mix(Blocks.BASALT, Blocks.BASALT, Blocks.BLACKSTONE, Blocks.BLACKSTONE, Blocks.MAGMA_BLOCK,
                                    Blocks.SMOOTH_BASALT),
                            mix(Blocks.BLACKSTONE, Blocks.BLACKSTONE, Blocks.BASALT),
                            List.of(), List.of(decor(s(Blocks.SHROOMLIGHT), 0.006f)),
                            List.of(spire(Blocks.BASALT, 2, 5, 0.012f)),
                            List.of(spire(Blocks.BLACKSTONE, 2, 6, 0.016f)),
                            null, s(Blocks.LAVA), 0.5f),
                    new Cavern("obsidian_hollow", -45, 60,
                            mix(Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.BLACKSTONE, Blocks.BLACKSTONE),
                            mix(Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN),
                            List.of(), List.of(decor(s(Blocks.CRYING_OBSIDIAN), 0.01f)),
                            List.of(spire(Blocks.OBSIDIAN, 3, 8, 0.01f)), List.of(spire(Blocks.OBSIDIAN, 2, 5, 0.012f)),
                            null, null, 0f),
                    new Cavern("sulfur_vents", -40, 30,
                            mix(Blocks.YELLOW_TERRACOTTA, Blocks.YELLOW_TERRACOTTA, Blocks.ORANGE_TERRACOTTA,
                                    Blocks.WHITE_TERRACOTTA),
                            mix(Blocks.YELLOW_TERRACOTTA, Blocks.ORANGE_TERRACOTTA),
                            List.of(), List.of(decor(s(Blocks.SHROOMLIGHT), 0.004f)),
                            List.of(spire(Blocks.RAW_GOLD_BLOCK, 1, 3, 0.012f)), List.of(),
                            null, null, 0f));
            case "sculk" -> List.of(
                    new Cavern("sculk_cavern", -50, 50,
                            mix(Blocks.SCULK, Blocks.SCULK, Blocks.SCULK, Blocks.DEEPSLATE, Blocks.TUFF),
                            mix(Blocks.SCULK, Blocks.DEEPSLATE, Blocks.DEEPSLATE),
                            List.of(decor(veinDown(), 0.15f)),
                            List.of(decor(veinUp(), 0.03f), decor(hangingLantern(true), 0.003f)),
                            List.of(spire(Blocks.DEEPSLATE, 2, 4, 0.006f)), List.of(spire(Blocks.TUFF, 2, 5, 0.01f)),
                            null, null, 0f),
                    new Cavern("lush_hollow", -40, 55,
                            mix(Blocks.MOSS_BLOCK, Blocks.MOSS_BLOCK, Blocks.MOSS_BLOCK, Blocks.CLAY,
                                    Blocks.ROOTED_DIRT),
                            mix(Blocks.MOSS_BLOCK, Blocks.MOSS_BLOCK, Blocks.DEEPSLATE),
                            List.of(decor(s(Blocks.MOSS_CARPET), 0.30f), decor(s(Blocks.FLOWERING_AZALEA), 0.015f)),
                            List.of(decor(s(Blocks.HANGING_ROOTS), 0.07f), decor(lichenUp(), 0.05f)),
                            List.of(), List.of(), null, s(Blocks.WATER), 0.6f),
                    new Cavern("fungal_deep", -55, 20,
                            mix(Blocks.MYCELIUM, Blocks.MYCELIUM, Blocks.PODZOL, Blocks.PODZOL, Blocks.MUD),
                            mix(Blocks.DEEPSLATE, Blocks.DEEPSLATE, Blocks.TUFF),
                            List.of(decor(s(Blocks.RED_MUSHROOM), 0.06f, Blocks.MYCELIUM, Blocks.PODZOL, Blocks.MUD),
                                    decor(s(Blocks.BROWN_MUSHROOM), 0.06f, Blocks.MYCELIUM, Blocks.PODZOL, Blocks.MUD)),
                            List.of(), List.of(), List.of(),
                            new Giant(s(Blocks.MUSHROOM_STEM), s(Blocks.BROWN_MUSHROOM_BLOCK), s(Blocks.SHROOMLIGHT), 5,
                                    9, 3, 0.01f),
                            null, 0f));
            case "void" -> List.of(
                    new Cavern("geode_cavern", -55, 50,
                            mix(Blocks.CALCITE, Blocks.CALCITE, Blocks.AMETHYST_BLOCK, Blocks.SMOOTH_BASALT,
                                    Blocks.TUFF),
                            mix(Blocks.CALCITE, Blocks.CALCITE, Blocks.SMOOTH_BASALT),
                            List.of(decor(s(Blocks.AMETHYST_CLUSTER), 0.05f),
                                    decor(s(Blocks.SMALL_AMETHYST_BUD), 0.06f)),
                            List.of(decor(clusterDown(), 0.03f), decor(s(Blocks.SEA_LANTERN), 0.003f)),
                            List.of(spire(Blocks.AMETHYST_BLOCK, 2, 5, 0.01f)), List.of(), null, null, 0f),
                    new Cavern("chorus_grotto", -45, 55,
                            mix(Blocks.END_STONE, Blocks.END_STONE, Blocks.END_STONE, Blocks.END_STONE_BRICKS),
                            mix(Blocks.END_STONE, Blocks.END_STONE),
                            List.of(decor(s(Blocks.CHORUS_FLOWER), 0.02f, Blocks.END_STONE, Blocks.END_STONE_BRICKS)),
                            List.of(decor(rodDown(), 0.006f)),
                            List.of(spire(Blocks.PURPUR_PILLAR, 2, 6, 0.006f)), List.of(), null, null, 0f),
                    new Cavern("starlit_deep", -55, 10,
                            mix(Blocks.OBSIDIAN, Blocks.BLACKSTONE, Blocks.BLACKSTONE, Blocks.CRYING_OBSIDIAN),
                            mix(Blocks.OBSIDIAN, Blocks.BLACKSTONE),
                            List.of(decor(s(Blocks.SMALL_AMETHYST_BUD), 0.04f)),
                            List.of(decor(s(Blocks.SEA_LANTERN), 0.004f)),
                            List.of(spire(Blocks.OBSIDIAN, 2, 4, 0.01f)), List.of(), null, null, 0f));
            case "sealed_a" -> List.of(
                    new Cavern("rust_warren", -50, 45,
                            mix(Blocks.OXIDIZED_COPPER, Blocks.WEATHERED_COPPER, Blocks.EXPOSED_COPPER, Blocks.GRAVEL,
                                    Blocks.COBBLESTONE),
                            mix(Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS),
                            List.of(),
                            List.of(decor(s(Blocks.CHAIN), 0.02f), decor(s(Blocks.IRON_BARS), 0.02f),
                                    decor(hangingLantern(false), 0.006f)),
                            List.of(spire(Blocks.OXIDIZED_COPPER, 1, 4, 0.01f)), List.of(), null, s(Blocks.WATER),
                            0.4f),
                    new Cavern("bunker_tunnels", -45, 30,
                            mix(Blocks.SMOOTH_STONE, Blocks.SMOOTH_STONE, Blocks.STONE_BRICKS, Blocks.GRAY_CONCRETE),
                            mix(Blocks.GRAY_CONCRETE, Blocks.GRAY_CONCRETE, Blocks.STONE_BRICKS),
                            List.of(), List.of(decor(hangingLantern(false), 0.01f), decor(s(Blocks.IRON_BARS), 0.03f)),
                            List.of(spire(Blocks.IRON_BLOCK, 2, 5, 0.004f)), List.of(), null, null, 0f),
                    new Cavern("coolant_cave", -50, 20,
                            mix(Blocks.PACKED_ICE, Blocks.PACKED_ICE, Blocks.STONE, Blocks.STONE, Blocks.BLUE_ICE),
                            mix(Blocks.STONE, Blocks.PACKED_ICE),
                            List.of(), List.of(decor(s(Blocks.SEA_LANTERN), 0.003f)),
                            List.of(spire(Blocks.BLUE_ICE, 1, 3, 0.008f)),
                            List.of(spire(Blocks.PACKED_ICE, 2, 5, 0.012f)),
                            null, s(Blocks.WATER), 0.6f));
            case "sealed_b" -> List.of(
                    new Cavern("soul_cavern", -50, 40,
                            mix(Blocks.SOUL_SAND, Blocks.SOUL_SAND, Blocks.SOUL_SOIL, Blocks.SOUL_SOIL, Blocks.BASALT),
                            mix(Blocks.BASALT, Blocks.BLACKSTONE),
                            List.of(decor(s(Blocks.SOUL_FIRE), 0.012f, Blocks.SOUL_SAND, Blocks.SOUL_SOIL)),
                            List.of(decor(hangingLantern(true), 0.004f)),
                            List.of(spire(Blocks.BONE_BLOCK, 2, 6, 0.008f)), List.of(), null, null, 0f),
                    new Cavern("warped_grotto", -45, 55,
                            mix(Blocks.WARPED_NYLIUM, Blocks.WARPED_NYLIUM, Blocks.WARPED_NYLIUM, Blocks.NETHERRACK,
                                    Blocks.NETHERRACK),
                            mix(Blocks.NETHERRACK, Blocks.NETHERRACK, Blocks.WARPED_WART_BLOCK),
                            List.of(decor(s(Blocks.WARPED_ROOTS), 0.15f, Blocks.WARPED_NYLIUM),
                                    decor(s(Blocks.WARPED_FUNGUS), 0.03f, Blocks.WARPED_NYLIUM),
                                    decor(s(Blocks.NETHER_SPROUTS), 0.12f, Blocks.WARPED_NYLIUM)),
                            List.of(decor(s(Blocks.SHROOMLIGHT), 0.008f)), List.of(), List.of(),
                            new Giant(s(Blocks.WARPED_STEM), s(Blocks.WARPED_WART_BLOCK), s(Blocks.SHROOMLIGHT), 5, 10,
                                    3,
                                    0.008f),
                            null, 0f),
                    new Cavern("magma_vents", -55, 30,
                            mix(Blocks.NETHERRACK, Blocks.NETHERRACK, Blocks.MAGMA_BLOCK, Blocks.BASALT,
                                    Blocks.BLACKSTONE),
                            mix(Blocks.NETHERRACK, Blocks.NETHER_BRICKS),
                            List.of(), List.of(decor(s(Blocks.GLOWSTONE), 0.005f)),
                            List.of(spire(Blocks.BASALT, 2, 5, 0.01f)), List.of(spire(Blocks.NETHERRACK, 2, 4, 0.01f)),
                            null, s(Blocks.LAVA), 0.6f));
            default -> List.of();
        };
    }

    private static boolean solid(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && !state.canBeReplaced();
    }

    private static boolean rock(BlockState state) {
        return state.is(BlockTags.STONE_ORE_REPLACEABLES) || state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
    }

    private static double roll(long h, int slot) {
        return ((h >>> (slot * 16)) & 0xFFFF) / 65536.0;
    }

    private static Cavern cavernAt(List<Cavern> caverns, SimplexNoise noise, int x, int y, int z) {
        double n = noise.getValue(x * 0.012 + 21000.0, y * 0.03, z * 0.012 - 13000.0);

        if (n < -0.1) return null;
        int index = Math.min(caverns.size() - 1, (int) ((n + 0.1) / 1.1 * caverns.size()));
        Cavern cavern = caverns.get(Math.max(0, index));
        return y >= cavern.minY() && y <= cavern.maxY() ? cavern : null;
    }

    private static boolean hollow(SimplexNoise noise, int x, int y, int z) {
        if (noise.getValue(x * 0.017 + 5000.0, y * 0.032, z * 0.017 + 9000.0) > CHAMBER_THRESHOLD) return true;
        double first = noise.getValue(x * 0.011 - 4000.0, y * 0.016 + 77.0, z * 0.011 + 1500.0);
        if (Math.abs(first) > TUNNEL_WIDTH) return false;
        return Math.abs(noise.getValue(x * 0.011 + 6000.0, y * 0.016 - 99.0, z * 0.011 - 2500.0)) < TUNNEL_WIDTH;
    }

    public static void apply(ChunkAccess chunk, String dimension, SimplexNoise noise, long seed, int minY, int maxY) {
        List<Cavern> caverns = cavernsOf(dimension);
        if (caverns.isEmpty() || noise == null) return;

        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int lowest = minY + FLOOR_OF_THE_DEEP;

        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                int surface = maxY - 2;
                while (surface > minY && chunk.getBlockState(pos.set(x, surface, z)).isAir()) surface--;

                for (int y = surface - ROOF; y >= lowest + 2; y--) {
                    BlockState here = chunk.getBlockState(pos.set(x, y, z));
                    if (!solid(here) || here.is(Blocks.BEDROCK) || cavernAt(caverns, noise, x, y, z) == null) continue;
                    if (hollow(noise, x, y, z)) chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                }

                if (surface > minY + 40 && noise.getValue(x * 0.04 + 333.0, 0.0, z * 0.04 - 777.0) > 0.88) {
                    for (int y = surface; y >= Math.max(lowest + 2, surface - SHAFT_DEPTH); y--) {
                        BlockState here = chunk.getBlockState(pos.set(x, y, z));
                        if (here.isAir()) {
                            if (y < surface - 8) break;
                            continue;
                        }
                        if (here.is(Blocks.BEDROCK) || !here.getFluidState().isEmpty()) break;
                        chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                    }
                }

                for (int y = surface - BELOW_SURFACE; y >= lowest; y--) {
                    if (!chunk.getBlockState(pos.set(x, y, z)).isAir()) continue;
                    BlockState below = chunk.getBlockState(pos.set(x, y - 1, z));
                    BlockState above = chunk.getBlockState(pos.set(x, y + 1, z));
                    boolean floor = solid(below);
                    boolean ceiling = solid(above);
                    if (!floor && !ceiling) continue;

                    Cavern cavern = cavernAt(caverns, noise, x, y, z);
                    if (cavern == null) continue;
                    long h = Landforms.mix(seed ^ (y * 0x9E3779B97F4A7C15L), x, z);

                    if (floor && rock(below)) {
                        chunk.setBlockState(pos.set(x, y - 1, z),
                                cavern.floor()[(int) (roll(h, 0) * cavern.floor().length)],
                                false);
                        BlockState top = chunk.getBlockState(pos.set(x, y - 1, z));
                        decorate(chunk, pos, cavern.floorDecor(), top.getBlock(), x, y, z, roll(h, 1));
                        for (Spire sp : cavern.spires()) {
                            if (roll(h, 2) < sp.chance()) {
                                int length = sp.minLength() +
                                        (int) (roll(h, 3) * (sp.maxLength() - sp.minLength() + 1));
                                for (int k = 0; k < length; k++) {
                                    if (!chunk.getBlockState(pos.set(x, y + k, z)).isAir()) break;
                                    chunk.setBlockState(pos, sp.block(), false);
                                }
                                break;
                            }
                        }
                        if (cavern.giant() != null && roll(h, 2) < cavern.giant().chance()) {
                            giant(chunk, cavern.giant(), minX, minZ, x, y, z, h);
                        }
                    }
                    if (ceiling && rock(above)) {
                        chunk.setBlockState(pos.set(x, y + 1, z),
                                cavern.ceiling()[(int) (roll(h, 1) * cavern.ceiling().length)], false);
                        decorate(chunk, pos, cavern.ceilingDecor(), null, x, y, z, roll(h, 3));
                        for (Spire sp : cavern.stalactites()) {
                            if (roll(h, 0) < sp.chance()) {
                                int length = sp.minLength() +
                                        (int) (roll(h, 2) * (sp.maxLength() - sp.minLength() + 1));
                                for (int k = 0; k < length; k++) {
                                    if (!chunk.getBlockState(pos.set(x, y - k, z)).isAir()) break;
                                    chunk.setBlockState(pos, sp.block(), false);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
        pond(chunk, caverns, noise, seed, minX, minZ, minY, maxY, lowest);
    }

    private static void decorate(ChunkAccess chunk, BlockPos.MutableBlockPos pos, List<Decor> decors, Block floorBlock,
                                 int x, int y, int z, double roll) {
        double cumulative = 0.0;
        for (Decor d : decors) {
            cumulative += d.chance();
            if (roll >= cumulative) continue;
            if (floorBlock != null && d.onlyOn().length > 0) {
                boolean ok = false;
                for (Block allowed : d.onlyOn()) ok |= allowed == floorBlock;
                if (!ok) return;
            }
            if (chunk.getBlockState(pos.set(x, y, z)).isAir()) chunk.setBlockState(pos, d.state(), false);
            return;
        }
    }

    private static void giant(ChunkAccess chunk, Giant g, int minX, int minZ, int x, int y, int z, long h) {
        int cap = g.capRadius();
        if (x - cap < minX || x + cap > minX + 15 || z - cap < minZ || z + cap > minZ + 15) return;
        int height = g.minHeight() + (int) (roll(h, 3) * (g.maxHeight() - g.minHeight() + 1));
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int k = 0; k <= height; k++) {
            if (!chunk.getBlockState(pos.set(x, y + k, z)).isAir()) return;
        }
        for (int k = 0; k < height; k++) chunk.setBlockState(pos.set(x, y + k, z), g.stem(), false);
        for (int dx = -cap; dx <= cap; dx++) {
            for (int dz = -cap; dz <= cap; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > cap + 0.4) continue;
                double f = d / (cap + 0.4);
                int under = y + height - (int) Math.round(2.0 * f * f);
                int over = y + height + (int) Math.round(cap * 0.6 * Math.sqrt(Math.max(0.0, 1.0 - f * f)));
                for (int yy = under; yy <= over; yy++) {
                    if (!chunk.getBlockState(pos.set(x + dx, yy, z + dz)).isAir()) continue;
                    boolean light = yy == under && ((dx + dz) & 1) == 0 && d > 0.5;
                    chunk.setBlockState(pos, light ? g.glow() : g.cap(), false);
                }
            }
        }
    }

    private static void pond(ChunkAccess chunk, List<Cavern> caverns, SimplexNoise noise, long seed, int minX, int minZ,
                             int minY, int maxY, int lowest) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int attempt = 0; attempt < 8; attempt++) {
            long h = Landforms.mix(seed ^ 0x504F4E44L ^ attempt, minX, minZ);
            int x0 = minX + 5 + (int) (roll(h, 0) * 6);
            int z0 = minZ + 5 + (int) (roll(h, 1) * 6);
            int radius = 2 + (int) (roll(h, 2) * 3);

            int surface = maxY - 2;
            while (surface > minY && chunk.getBlockState(pos.set(x0, surface, z0)).isAir()) surface--;

            for (int y = surface - BELOW_SURFACE; y >= lowest + 4; y--) {
                if (!chunk.getBlockState(pos.set(x0, y, z0)).isAir() ||
                        !solid(chunk.getBlockState(pos.set(x0, y - 1, z0)))) {
                    continue;
                }
                Cavern cavern = cavernAt(caverns, noise, x0, y, z0);
                if (cavern == null || cavern.pond() == null || roll(h, 3) >= cavern.pondChance()) break;
                if (!chunk.getBlockState(pos.set(x0, y + 1, z0)).isAir() ||
                        !chunk.getBlockState(pos.set(x0, y + 2, z0)).isAir()) {
                    break;
                }

                boolean sound = true;
                for (int dx = -radius - 2; dx <= radius + 2 && sound; dx++) {
                    for (int dz = -radius - 2; dz <= radius + 2 && sound; dz++) {
                        if (Math.sqrt(dx * dx + dz * dz) > radius + 2.0) continue;
                        sound = solid(chunk.getBlockState(pos.set(x0 + dx, y - 3, z0 + dz)));
                    }
                }
                if (!sound) break;

                BlockState rim = cavern.floor()[0];
                for (int dx = -radius - 2; dx <= radius + 2; dx++) {
                    for (int dz = -radius - 2; dz <= radius + 2; dz++) {
                        double d = Math.sqrt(dx * dx + dz * dz);
                        if (d > radius + 2.0) continue;
                        int x = x0 + dx;
                        int z = z0 + dz;

                        for (int k = 0; k <= 2; k++) {
                            if (solid(chunk.getBlockState(pos.set(x, y + k, z)))) {
                                chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                            }
                        }
                        if (d <= radius + 0.4) {
                            chunk.setBlockState(pos.set(x, y - 1, z), cavern.pond(), false);
                            chunk.setBlockState(pos.set(x, y - 2, z), cavern.pond(), false);
                        } else {

                            chunk.setBlockState(pos.set(x, y - 1, z), rim, false);
                        }
                    }
                }
                return;
            }
        }
    }
}
