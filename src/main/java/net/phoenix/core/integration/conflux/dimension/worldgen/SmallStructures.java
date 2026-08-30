package net.phoenix.core.integration.conflux.dimension.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.phoenix.core.PhoenixCore;

/**
 * Small scattered ruins/outposts, one per discipline theme - the concrete "someone lives (or
 * lived) here" signal a dimension needs to read as liveable rather than untouched wilderness.
 * Deliberately simple (a few walls, a doorway, a chest) rather than a full NBT structure-
 * template system - the goal is scattered signs of habitation, not monuments.
 */
public final class SmallStructures {

    // ~1 per ~85 chunks (~1350 blocks apart on average) - scattered, not a village grid.
    private static final float CHANCE_PER_CHUNK = 0.012f;

    private SmallStructures() {}

    public static void maybePlace(WorldGenLevel level, String disciplineId, int chunkX, int chunkZ) {
        RandomSource random = level.getRandom();
        if (random.nextFloat() >= CHANCE_PER_CHUNK) return;

        int x = chunkX * 16 + 4 + random.nextInt(8);
        int z = chunkZ * 16 + 4 + random.nextInt(8);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        BlockPos base = new BlockPos(x, y, z);

        if (!level.ensureCanWrite(base)) return;
        // MOTION_BLOCKING reports the water surface, not the lake/river bed, in a water column -
        // skip rather than build a ruin floating on top of the water.
        if (!level.getBlockState(base.below()).getFluidState().isEmpty()) return;

        buildRuin(level, base, themeFor(disciplineId), random);
    }

    private record Theme(BlockState wall, BlockState floor, BlockState light) {}

    private static Theme themeFor(String disciplineId) {
        return switch (disciplineId) {
            case "phoenix" -> new Theme(Blocks.BLACKSTONE.defaultBlockState(),
                    Blocks.POLISHED_BLACKSTONE.defaultBlockState(), Blocks.LANTERN.defaultBlockState());
            case "sculk" -> new Theme(Blocks.DEEPSLATE_BRICKS.defaultBlockState(),
                    Blocks.COBBLED_DEEPSLATE.defaultBlockState(), Blocks.SOUL_LANTERN.defaultBlockState());
            case "void" -> new Theme(Blocks.PURPUR_BLOCK.defaultBlockState(),
                    Blocks.END_STONE_BRICKS.defaultBlockState(), Blocks.END_ROD.defaultBlockState());
            case "sealed_a" -> new Theme(Blocks.IRON_BLOCK.defaultBlockState(),
                    Blocks.GRAY_CONCRETE.defaultBlockState(), Blocks.LANTERN.defaultBlockState());
            case "sealed_b" -> new Theme(Blocks.WARPED_PLANKS.defaultBlockState(),
                    Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.SOUL_LANTERN.defaultBlockState());
            default -> new Theme(Blocks.STONE_BRICKS.defaultBlockState(),
                    Blocks.STONE.defaultBlockState(), Blocks.TORCH.defaultBlockState());
        };
    }

    private static void buildRuin(WorldGenLevel level, BlockPos base, Theme theme, RandomSource random) {
        int size = 2 + random.nextInt(2); // 2 or 3 -> 5x5 or 7x7 footprint

        for (int dx = -size; dx <= size; dx++) {
            for (int dz = -size; dz <= size; dz++) {
                BlockPos floorPos = base.offset(dx, -1, dz);
                if (level.ensureCanWrite(floorPos) && !level.getBlockState(floorPos).isAir()) {
                    level.setBlock(floorPos, theme.floor(), 3);
                }
            }
        }

        boolean doorOnX = random.nextBoolean();
        for (int dx = -size; dx <= size; dx++) {
            for (int dz = -size; dz <= size; dz++) {
                boolean edge = dx == -size || dx == size || dz == -size || dz == size;
                if (!edge) continue;

                // Ruined look: randomly skip wall segments instead of a pristine box, so it
                // reads as an abandoned outpost rather than a freshly-built one.
                if (random.nextFloat() < 0.2f) continue;

                boolean isDoorGap = doorOnX ? (dz == 0 && dx == -size) : (dx == 0 && dz == -size);

                for (int dy = 0; dy < 3; dy++) {
                    if (isDoorGap && dy < 2) continue;

                    BlockPos wallPos = base.offset(dx, dy, dz);
                    if (level.ensureCanWrite(wallPos)) {
                        level.setBlock(wallPos, theme.wall(), 3);
                    }
                }
            }
        }

        // Standing lanterns/torches/end rods all need solid support directly below them - the
        // interior is hollow above the floor, so this has to sit at floor level (dy 0), not
        // floating partway up the room, or it fails to place/immediately drops as an item.
        BlockPos lightPos = base.offset(0, 0, 0);
        if (level.ensureCanWrite(lightPos)) {
            level.setBlock(lightPos, theme.light(), 3);
        }

        BlockPos chestPos = base.offset(0, 0, size - 1);
        if (level.ensureCanWrite(chestPos)) {
            level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
            if (level.getBlockEntity(chestPos) instanceof RandomizableContainerBlockEntity chestEntity) {
                chestEntity.setLootTable(PhoenixCore.id("chests/ethereal_spawn"), chestPos.asLong());
            }
        }
    }
}
