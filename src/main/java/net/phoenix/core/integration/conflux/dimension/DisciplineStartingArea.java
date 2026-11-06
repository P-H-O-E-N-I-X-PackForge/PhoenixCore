package net.phoenix.core.integration.conflux.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.loot.LootTable;
import net.phoenix.core.PhoenixCore;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class DisciplineStartingArea {

    private static final BlockPos ORIGIN = new BlockPos(0, 64, 0);
    private static final int MAX_SEARCH = 192;

    public static final class Spots extends SavedData {

        private final Map<String, BlockPos> spots = new HashMap<>();

        static Spots get(ServerLevel level) {
            return level.getServer().overworld().getDataStorage()
                    .computeIfAbsent(new SavedData.Factory<>(Spots::new, Spots::load), "phoenixcore_conflux_spawns");
        }

        private static Spots load(CompoundTag tag, HolderLookup.Provider registries) {
            Spots data = new Spots();
            ListTag list = tag.getList("Spots", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag t = list.getCompound(i);
                data.spots.put(t.getString("Discipline"), new BlockPos(t.getInt("X"), t.getInt("Y"), t.getInt("Z")));
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            spots.forEach((discipline, pos) -> {
                CompoundTag t = new CompoundTag();
                t.putString("Discipline", discipline);
                t.putInt("X", pos.getX());
                t.putInt("Y", pos.getY());
                t.putInt("Z", pos.getZ());
                list.add(t);
            });
            tag.put("Spots", list);
            return tag;
        }
    }

    public static BlockPos ensure(ServerLevel level, String disciplineId) {
        Spots spots = Spots.get(level);
        BlockPos stand = spots.spots.get(disciplineId);
        if (stand == null) {
            stand = findSpot(level);
            spots.spots.put(disciplineId, stand);
            spots.setDirty();
        }
        BlockPos chest = stand.relative(Direction.EAST, 2);
        if (!level.getBlockState(chest).is(Blocks.CHEST)) placeChest(level, stand, chest, disciplineId);
        return stand;
    }

    private static int surface(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }

    private static BlockPos findSpot(ServerLevel level) {
        int sea = level.getSeaLevel();
        for (int r = 0; r <= MAX_SEARCH; r += 8) {
            int steps = r == 0 ? 1 : Math.max(8, r / 2);
            for (int i = 0; i < steps; i++) {
                double angle = i * Math.PI * 2.0 / steps;
                int x = ORIGIN.getX() + (int) Math.round(Math.cos(angle) * r);
                int z = ORIGIN.getZ() + (int) Math.round(Math.sin(angle) * r);
                int top = surface(level, x, z);
                BlockPos ground = new BlockPos(x, top - 1, z);
                BlockState state = level.getBlockState(ground);
                if (top - 1 <= sea || state.isAir() || !level.getFluidState(ground).isEmpty()) continue;
                int east = surface(level, x + 2, z);
                if (Math.abs(east - top) > 2 || east - 1 <= sea) continue;
                return new BlockPos(x, top, z);
            }
        }
        int top = surface(level, ORIGIN.getX(), ORIGIN.getZ());
        return new BlockPos(ORIGIN.getX(), Math.max(top, sea + 1), ORIGIN.getZ());
    }

    private static void placeChest(ServerLevel level, BlockPos stand, BlockPos chest, String disciplineId) {
        BlockPos chestAt = new BlockPos(chest.getX(), stand.getY(), chest.getZ());
        for (BlockPos p : new BlockPos[] { stand, chestAt }) {
            if (level.getBlockState(p.below()).isAir() || !level.getFluidState(p.below()).isEmpty()) {
                level.setBlock(p.below(), Blocks.STONE.defaultBlockState(), 3);
            }
            for (int up = 0; up < 3; up++) level.setBlock(p.above(up), Blocks.AIR.defaultBlockState(), 3);
        }
        level.setBlock(chestAt, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST), 3);
        if (level.getBlockEntity(chestAt) instanceof RandomizableContainerBlockEntity container) {
            ResourceKey<LootTable> table = ResourceKey.create(Registries.LOOT_TABLE,
                    PhoenixCore.id("chests/conflux/start/" + disciplineId));
            container.setLootTable(table, level.getRandom().nextLong());
        }
    }

    public static class StartingAreaConfig {

        public final String disciplineId;
        public final String displayName;
        public final int platformColor;
        public final String[] loreLinesLines;

        public StartingAreaConfig(String disciplineId, String displayName, int platformColor, String[] loreLines) {
            this.disciplineId = disciplineId;
            this.displayName = displayName;
            this.platformColor = platformColor;
            this.loreLinesLines = loreLines;
        }
    }

    public static final StartingAreaConfig[] CONFIGS = {
            new StartingAreaConfig("phoenix",
                    "The Phoenix",
                    0xFFFF8800,
                    new String[] {
                            "Welcome, Seeker of the Flame.",
                            "The path ahead burns with possibility.",
                            "Rise from the ashes of your choice."
                    }),

            new StartingAreaConfig("sculk",
                    "The Sculk",
                    0xFF00CC88,
                    new String[] {
                            "The network acknowledges your presence.",
                            "You are connected. Always.",
                            "Listen to what grows in the dark."
                    }),

            new StartingAreaConfig("void",
                    "The Void",
                    0xFF6633FF,
                    new String[] {
                            "Space itself bends to your will.",
                            "In the void, all possibilities exist.",
                            "Choose your perspective carefully."
                    }),

            new StartingAreaConfig("sealed_a",
                    "Sealed A - Catalyst",
                    0xFF9D4EDD,
                    new String[] {
                            "Something stirs beyond the seal.",
                            "Will you be its catalyst or its cage?",
                            "The choice corrupts either way."
                    }),

            new StartingAreaConfig("sealed_b",
                    "Sealed B - Extinction",
                    0xFF556B2F,
                    new String[] {
                            "All things return to silence.",
                            "You are the instrument of ending.",
                            "Or the herald of rebirth."
                    })
    };

    @Nullable
    public static StartingAreaConfig getConfig(String disciplineId) {
        for (StartingAreaConfig config : CONFIGS) {
            if (config.disciplineId.equals(disciplineId)) {
                return config;
            }
        }
        return null;
    }
}
