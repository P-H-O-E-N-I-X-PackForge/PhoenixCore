package net.phoenix.core.integration.conflux.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

public final class SculkGrowth {

    private static final int RADIUS_BASE = 2;

    private SculkGrowth() {}

    public static int grow(ServerLevel level, ChunkPos chunk, int from, int to) {
        if (level.getChunkSource().getChunkNow(chunk.x, chunk.z) == null) return 0;
        int changed = 0;
        for (int step = from + 1; step <= to; step++) {
            RandomSource random = RandomSource.create(chunk.toLong() * 0x9E3779B97F4A7C15L + step * 31L);
            int seeds = 1 + step;
            for (int s = 0; s < seeds; s++) {
                int cx = chunk.getMinBlockX() + 2 + random.nextInt(12);
                int cz = chunk.getMinBlockZ() + 2 + random.nextInt(12);
                int radius = RADIUS_BASE + step + random.nextInt(2);
                changed += blob(level, cx, cz, radius, random);
            }
        }
        return changed;
    }

    private static int blob(ServerLevel level, int cx, int cz, int radius, RandomSource random) {
        BlockState sculk = Blocks.SCULK.defaultBlockState();
        BlockState vein = Blocks.SCULK_VEIN.defaultBlockState().setValue(MultifaceBlock.getFaceProperty(Direction.DOWN),
                true);
        int changed = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > radius + 0.5) continue;
                double chance = 0.9 - 0.7 * (d / (radius + 0.5));
                if (random.nextDouble() > chance) continue;

                int x = cx + dx;
                int z = cz + dz;
                if (!level.hasChunkAt(pos.set(x, 64, z))) continue;
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                pos.set(x, top, z);
                BlockState here = level.getBlockState(pos);
                if (!natural(here) || !level.getFluidState(pos).isEmpty()) continue;
                if (nearBuilding(level, x, top, z)) continue;

                if (d > radius * 0.65 && random.nextInt(3) == 0) {
                    pos.set(x, top + 1, z);
                    if (level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, vein, 2);
                        changed++;
                    }
                } else {
                    level.setBlock(pos.set(x, top, z), sculk, 2);
                    changed++;
                }
            }
        }
        return changed;
    }

    private static final int GUARD_XZ = 3;

    private static boolean nearBuilding(ServerLevel level, int x, int top, int z) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -GUARD_XZ; dx <= GUARD_XZ; dx++) {
            for (int dz = -GUARD_XZ; dz <= GUARD_XZ; dz++) {
                for (int dy = -3; dy <= 5; dy++) {
                    p.set(x + dx, top + dy, z + dz);
                    BlockState state = level.getBlockState(p);
                    if (!wild(state)) return true;
                    if (state.hasBlockEntity()) return true;
                }
            }
        }
        return false;
    }

    private static boolean wild(BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty()) return true;
        if (natural(state) || state.canBeReplaced()) return true;
        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.is(BlockTags.FLOWERS) ||
                state.is(BlockTags.SAPLINGS) || state.is(BlockTags.SNOW) || state.is(BlockTags.ICE) ||
                state.is(BlockTags.CORALS)) {
            return true;
        }
        if (state.is(Blocks.SCULK) || state.is(Blocks.SCULK_VEIN) || state.is(Blocks.MAGMA_BLOCK) ||
                state.is(Blocks.MOSS_CARPET) || state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.OBSIDIAN) ||
                state.is(Blocks.BLACKSTONE) || state.is(Blocks.BASALT) || state.is(Blocks.SMOOTH_BASALT)) {
            return true;
        }

        String path = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return path.contains("crystal_rose") || state.is(Blocks.END_ROD) || state.is(BlockTags.BASE_STONE_NETHER);
    }

    private static boolean natural(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.SAND) ||
                state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY) || state.is(Blocks.DEEPSLATE);
    }
}
