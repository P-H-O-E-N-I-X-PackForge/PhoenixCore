package net.phoenix.core.integration.conflux.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

import org.jetbrains.annotations.Nullable;

public class DisciplineStartingArea {

    public static final BlockPos ANCHOR = new BlockPos(0, 64, 0);

    private final String disciplineId;
    private final BlockPos spawnPos;

    public DisciplineStartingArea(String disciplineId) {
        this.disciplineId = disciplineId;
        this.spawnPos = ANCHOR;
    }

    public void generateStartingArea(net.minecraft.server.level.ServerLevel level) {
        DisciplineTheme theme = DisciplineThemeRegistry.getTheme(disciplineId);
        if (theme == null) return;

        createPlatform(level);

        addStarterChests(level);

        addLoreSigns(level, theme);
    }

    private void createPlatform(net.minecraft.server.level.ServerLevel level) {
        BlockPos center = spawnPos;

        for (int x = -8; x < 8; x++) {
            for (int z = -8; z < 8; z++) {
                BlockPos pos = center.offset(x, 0, z);
                level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
            }
        }

        for (int x = -6; x < 6; x += 2) {
            for (int z = -6; z < 6; z += 2) {
                BlockPos pos = center.offset(x, 1, z);
                level.setBlock(pos, Blocks.STONE_BRICKS.defaultBlockState(), 3);
            }
        }

        for (int x = -8; x < 8; x++) {
            for (int z = -8; z < 8; z++) {
                for (int y = 1; y < 4; y++) {
                    BlockPos pos = center.offset(x, y, z);
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private void addStarterChests(net.minecraft.server.level.ServerLevel level) {
        BlockPos center = spawnPos;

        BlockPos chest1 = center.offset(-3, 1, 0);
        BlockPos chest2 = center.offset(3, 1, 0);

        level.setBlock(chest1, Blocks.CHEST.defaultBlockState(), 3);
        level.setBlock(chest2, Blocks.CHEST.defaultBlockState(), 3);

        addChestLoot(level, chest1, disciplineId);
        addChestLoot(level, chest2, disciplineId);
    }

    private void addChestLoot(net.minecraft.server.level.ServerLevel level, BlockPos pos, String disciplineId) {}

    private void addLoreSigns(net.minecraft.server.level.ServerLevel level, DisciplineTheme theme) {
        BlockPos center = spawnPos;

        BlockPos textPos = center.offset(0, 2, -6);
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
