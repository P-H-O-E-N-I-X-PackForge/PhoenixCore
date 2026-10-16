package net.phoenix.core.integration.conflux.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import org.jetbrains.annotations.Nullable;

final class UnderwaterDecor {

    private UnderwaterDecor() {}

    private static final Block[] CORAL_BLOCKS = { Blocks.TUBE_CORAL_BLOCK, Blocks.BRAIN_CORAL_BLOCK,
            Blocks.BUBBLE_CORAL_BLOCK, Blocks.FIRE_CORAL_BLOCK, Blocks.HORN_CORAL_BLOCK };
    private static final Block[] CORALS = { Blocks.TUBE_CORAL, Blocks.BRAIN_CORAL, Blocks.BUBBLE_CORAL,
            Blocks.FIRE_CORAL, Blocks.HORN_CORAL };
    private static final Block[] CORAL_FANS = { Blocks.TUBE_CORAL_FAN, Blocks.BRAIN_CORAL_FAN,
            Blocks.BUBBLE_CORAL_FAN, Blocks.FIRE_CORAL_FAN, Blocks.HORN_CORAL_FAN };

    static void apply(ChunkAccess chunk, String discipline, @Nullable SimplexNoise noise, long seed, int minY,
                      int maxY, int seaLevel) {
        ChunkPos cp = chunk.getPos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = cp.getMinBlockX(); x < cp.getMinBlockX() + 16; x++) {
            for (int z = cp.getMinBlockZ(); z < cp.getMinBlockZ() + 16; z++) {
                int top = Math.min(seaLevel, maxY - 1);
                if (chunk.getBlockState(pos.set(x, top, z)).getBlock() != Blocks.WATER) continue;

                int floor = top;
                while (floor > minY && chunk.getBlockState(pos.set(x, floor, z)).getBlock() == Blocks.WATER) floor--;
                BlockState floorState = chunk.getBlockState(pos.set(x, floor, z));
                if (floorState.isAir() || !floorState.getFluidState().isEmpty()) continue;

                int depth = top - floor;
                long h = hash(seed, x, z);
                double patch = noise == null ? 0.0 : noise.getValue(x * 0.045 + 311.0, 0, z * 0.045 - 97.0);
                double garden = noise == null ? 0.0 : noise.getValue(x * 0.03 - 640.0, 0, z * 0.03 + 211.0);
                double rough = noise == null ? 0.0 : noise.getValue(x * 0.11, 0, z * 0.11 + 55.0);

                BlockState cover = coverFor(discipline, patch, rough, depth);
                chunk.setBlockState(pos.set(x, floor, z), cover, false);
                for (int d = 1; d <= 2 && floor - d >= minY; d++) {
                    BlockState under = chunk.getBlockState(pos.set(x, floor - d, z));
                    if (under.is(Blocks.STONE) || under.is(Blocks.SAND) || under.is(Blocks.GRAVEL)) {
                        chunk.setBlockState(pos, cover.is(Blocks.MAGMA_BLOCK) ? Blocks.BASALT.defaultBlockState() :
                                cover, false);
                    }
                }
                boolean soft = cover.is(Blocks.SAND) || cover.is(Blocks.GRAVEL) || cover.is(Blocks.CLAY) ||
                        cover.is(Blocks.SCULK) || cover.is(Blocks.MUD);

                double roll = ((h >>> 20) & 0xFFFFF) / (double) 0x100000;
                int above = floor + 1;
                if (above >= top - 1) continue;

                if (depth >= 3 && depth <= 22 && garden > 0.35 && roll < 0.11 && !"sculk".equals(discipline)) {
                    coral(chunk, pos, x, above, z, h);
                } else if (soft && depth >= 4 && garden < -0.15 && roll < 0.22) {
                    kelp(chunk, pos, x, above, z, top, h);
                } else if (soft && depth <= 30 && roll < 0.34) {
                    seagrass(chunk, pos, x, above, z, top, h);
                } else if (soft && depth <= 18 && roll > 0.985) {
                    chunk.setBlockState(pos.set(x, above, z), Blocks.SEA_PICKLE.defaultBlockState()
                            .setValue(SeaPickleBlock.PICKLES, 1 + (int) ((h >>> 8) & 3))
                            .setValue(BlockStateProperties.WATERLOGGED, true), false);
                } else if ("sculk".equals(discipline) && soft && roll > 0.96) {
                    chunk.setBlockState(pos.set(x, above, z), Blocks.SCULK_VEIN.defaultBlockState()
                            .setValue(MultifaceBlock.getFaceProperty(Direction.DOWN), true)
                            .setValue(BlockStateProperties.WATERLOGGED, true), false);
                }
            }
        }
    }

    private static BlockState coverFor(String discipline, double patch, double rough, int depth) {
        boolean deep = depth > 14;
        return switch (discipline) {
            case "phoenix" -> patch > 0.45 ? Blocks.MAGMA_BLOCK.defaultBlockState() :
                    patch > 0.0 ? Blocks.BLACKSTONE.defaultBlockState() :
                            rough > 0.25 ? Blocks.BASALT.defaultBlockState() : Blocks.GRAVEL.defaultBlockState();
            case "sculk" -> patch > 0.3 ? Blocks.SCULK.defaultBlockState() :
                    patch < -0.35 ? Blocks.MUD.defaultBlockState() : Blocks.DEEPSLATE.defaultBlockState();
            case "void" -> patch > 0.3 ? Blocks.OBSIDIAN.defaultBlockState() :
                    patch < -0.3 ? Blocks.END_STONE.defaultBlockState() : Blocks.GRAVEL.defaultBlockState();
            default -> {
                if (patch > 0.4) yield Blocks.CLAY.defaultBlockState();
                if (patch < -0.4) yield Blocks.GRAVEL.defaultBlockState();
                if (deep && rough > 0.35) yield Blocks.MUD.defaultBlockState();
                yield deep ? Blocks.GRAVEL.defaultBlockState() : Blocks.SAND.defaultBlockState();
            }
        };
    }

    private static void kelp(ChunkAccess chunk, BlockPos.MutableBlockPos pos, int x, int y, int z, int top, long h) {
        int height = Math.min(3 + (int) ((h >>> 12) & 7), top - y - 1);
        if (height < 2) return;
        BlockState plant = Blocks.KELP_PLANT.defaultBlockState();
        for (int i = 0; i < height - 1; i++) chunk.setBlockState(pos.set(x, y + i, z), plant, false);
        chunk.setBlockState(pos.set(x, y + height - 1, z),
                Blocks.KELP.defaultBlockState().setValue(BlockStateProperties.AGE_25, 12 + (int) ((h >>> 4) & 7)),
                false);
    }

    private static void seagrass(ChunkAccess chunk, BlockPos.MutableBlockPos pos, int x, int y, int z, int top,
                                 long h) {
        if (((h >>> 6) & 3) == 0 && y + 1 < top) {
            chunk.setBlockState(pos.set(x, y, z), Blocks.TALL_SEAGRASS.defaultBlockState()
                    .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER), false);
            chunk.setBlockState(pos.set(x, y + 1, z), Blocks.TALL_SEAGRASS.defaultBlockState()
                    .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), false);
        } else {
            chunk.setBlockState(pos.set(x, y, z), Blocks.SEAGRASS.defaultBlockState(), false);
        }
    }

    private static void coral(ChunkAccess chunk, BlockPos.MutableBlockPos pos, int x, int y, int z, long h) {
        int kind = (int) ((h >>> 3) & 0xFFFF) % CORAL_BLOCKS.length;
        int style = (int) ((h >>> 30) & 3);
        chunk.setBlockState(pos.set(x, y - 1, z), CORAL_BLOCKS[kind].defaultBlockState(), false);
        if (style == 0) {
            chunk.setBlockState(pos.set(x, y, z), CORAL_BLOCKS[kind].defaultBlockState(), false);
            chunk.setBlockState(pos.set(x, y + 1, z), CORALS[kind].defaultBlockState(), false);
        } else if (style == 1) {
            chunk.setBlockState(pos.set(x, y, z), CORAL_FANS[kind].defaultBlockState(), false);
        } else {
            chunk.setBlockState(pos.set(x, y, z), CORALS[kind].defaultBlockState(), false);
        }
    }

    private static long hash(long seed, int x, int z) {
        long h = seed ^ 0x9E3779B97F4A7C15L;
        h ^= x * 0xC2B2AE3D27D4EB4FL;
        h = Long.rotateLeft(h, 27) * 0x165667B19E3779F9L;
        h ^= z * 0x85EBCA77C2B2AE63L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        return h ^ (h >>> 32);
    }
}
