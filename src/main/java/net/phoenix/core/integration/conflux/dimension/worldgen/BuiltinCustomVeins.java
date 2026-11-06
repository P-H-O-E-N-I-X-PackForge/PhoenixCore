package net.phoenix.core.integration.conflux.dimension.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;

class ClusterVeinType extends CustomVeinType {

    public ClusterVeinType() {
        super("cluster", "Random dense clusters of ore throughout the chunk");
    }

    @Override
    public void generate(
                         WorldGenLevel level,
                         int chunkX,
                         int chunkZ,
                         RandomSource random,
                         VeinGenerationContext context) {
        int clustersPerChunk = Math.max(1, context.density / 2);

        for (int c = 0; c < clustersPerChunk; c++) {
            int x = (chunkX * 16) + random.nextInt(16);
            int z = (chunkZ * 16) + random.nextInt(16);
            int y = context.minY + random.nextInt(context.maxY - context.minY);

            int radius = 3 + random.nextInt(3);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (dx * dx + dy * dy + dz * dz <= radius * radius) {
                            BlockPos pos = new BlockPos(x + dx, y + dy, z + dz);
                            if (level.ensureCanWrite(pos)) {
                                level.setBlock(pos, context.blockState, 3);
                            }
                        }
                    }
                }
            }
        }
    }
}

class PillarVeinType extends CustomVeinType {

    public PillarVeinType() {
        super("pillar", "Vertical ore pillars rising from deep underground");
    }

    @Override
    public void generate(
                         WorldGenLevel level,
                         int chunkX,
                         int chunkZ,
                         RandomSource random,
                         VeinGenerationContext context) {
        int pillarsPerChunk = Math.max(1, context.density);

        for (int p = 0; p < pillarsPerChunk; p++) {
            int x = (chunkX * 16) + random.nextInt(16);
            int z = (chunkZ * 16) + random.nextInt(16);
            int baseY = context.minY + random.nextInt(Math.max(1, context.maxY - context.minY - 10));

            int height = 5 + random.nextInt(8);
            int width = random.nextBoolean() ? 1 : 2;

            for (int y = baseY; y < baseY + height && y < context.maxY; y++) {
                for (int dx = -width; dx <= width; dx++) {
                    for (int dz = -width; dz <= width; dz++) {
                        BlockPos pos = new BlockPos(x + dx, y, z + dz);
                        if (level.ensureCanWrite(pos)) {
                            level.setBlock(pos, context.blockState, 3);
                        }
                    }
                }
            }
        }
    }
}

class ScatteredVeinType extends CustomVeinType {

    public ScatteredVeinType() {
        super("scattered", "Sparse scattered ore blocks for discovery");
    }

    @Override
    public void generate(
                         WorldGenLevel level,
                         int chunkX,
                         int chunkZ,
                         RandomSource random,
                         VeinGenerationContext context) {
        int blocksPerChunk = 3 + (context.density * 2);

        for (int b = 0; b < blocksPerChunk; b++) {
            int x = (chunkX * 16) + random.nextInt(16);
            int z = (chunkZ * 16) + random.nextInt(16);
            int y = context.minY + random.nextInt(context.maxY - context.minY);

            BlockPos pos = new BlockPos(x, y, z);
            if (level.ensureCanWrite(pos)) {
                level.setBlock(pos, context.blockState, 3);
            }
        }
    }
}

class NetworkVeinType extends CustomVeinType {

    public NetworkVeinType() {
        super("network", "Interconnected network of ore veins");
    }

    @Override
    public void generate(
                         WorldGenLevel level,
                         int chunkX,
                         int chunkZ,
                         RandomSource random,
                         VeinGenerationContext context) {
        int networksPerChunk = Math.max(1, context.density / 3);

        for (int n = 0; n < networksPerChunk; n++) {
            int startX = (chunkX * 16) + random.nextInt(16);
            int startZ = (chunkZ * 16) + random.nextInt(16);
            int startY = context.minY + random.nextInt(context.maxY - context.minY);

            int segments = 8 + random.nextInt(8);
            int x = startX;
            int y = startY;
            int z = startZ;

            for (int s = 0; s < segments; s++) {

                BlockPos pos = new BlockPos(x, y, z);
                if (level.ensureCanWrite(pos)) {
                    level.setBlock(pos, context.blockState, 3);
                }

                int dx = random.nextInt(3) - 1;
                int dy = random.nextInt(3) - 1;
                int dz = random.nextInt(3) - 1;

                x += dx;
                y += dy;
                z += dz;

                y = Math.max(context.minY, Math.min(context.maxY, y));
            }
        }
    }
}

class BlobVeinType extends CustomVeinType {

    public BlobVeinType() {
        super("blob", "Massive spherical ore blobs");
    }

    @Override
    public void generate(
                         WorldGenLevel level,
                         int chunkX,
                         int chunkZ,
                         RandomSource random,
                         VeinGenerationContext context) {
        int blobsPerChunk = Math.max(1, (int) (context.density * 0.5f));

        for (int b = 0; b < blobsPerChunk; b++) {
            int x = (chunkX * 16) + random.nextInt(16);
            int z = (chunkZ * 16) + random.nextInt(16);
            int y = context.minY + random.nextInt(context.maxY - context.minY);

            int radius = 8 + random.nextInt(9);

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dz = -radius; dz <= radius; dz++) {

                        int distSquared = dx * dx + dy * dy + dz * dz;
                        if (distSquared <= radius * radius) {
                            BlockPos pos = new BlockPos(x + dx, y + dy, z + dz);
                            if (level.ensureCanWrite(pos) &&
                                    pos.getY() >= context.minY &&
                                    pos.getY() <= context.maxY &&
                                    random.nextFloat() < 0.85f) {
                                level.setBlock(pos, context.blockState, 3);
                            }
                        }
                    }
                }
            }
        }
    }
}
