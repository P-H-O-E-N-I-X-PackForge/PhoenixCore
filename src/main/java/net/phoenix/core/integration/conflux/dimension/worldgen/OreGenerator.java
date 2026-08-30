package net.phoenix.core.integration.conflux.dimension.worldgen;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;

import java.util.*;

public class OreGenerator {

    public static void generateOres(
            net.minecraft.world.level.WorldGenLevel level,
            DisciplineWorldgenConfig.OreConfig[] ores,
            net.minecraft.util.RandomSource random,
            int chunkX,
            int chunkZ) {

        for (DisciplineWorldgenConfig.OreConfig oreConfig : ores) {
            generateOreVeins(level, oreConfig, random, chunkX, chunkZ);
        }
    }

    private static void generateOreVeins(
            net.minecraft.world.level.WorldGenLevel level,
            DisciplineWorldgenConfig.OreConfig config,
            net.minecraft.util.RandomSource random,
            int chunkX,
            int chunkZ) {

        Block oreBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .get(config.oreBlock);
        if (oreBlock == Blocks.AIR) return;

        int startX = chunkX * 16;
        int startZ = chunkZ * 16;

        for (int i = 0; i < config.veinsPerChunk; i++) {
            int x = startX + random.nextInt(16);
            int z = startZ + random.nextInt(16);
            int y = config.minY + random.nextInt(config.maxY - config.minY);

            generateVein(level, oreBlock, config.veinSize, x, y, z, random);
        }
    }

    private static void generateVein(
            net.minecraft.world.level.WorldGenLevel level,
            Block oreBlock,
            int veinSize,
            int centerX,
            int centerY,
            int centerZ,
            net.minecraft.util.RandomSource random) {

        float radius = veinSize / 2.0f;

        for (int i = 0; i < veinSize; i++) {
            double offsetX = centerX + random.nextGaussian() * radius;
            double offsetY = centerY + random.nextGaussian() * (radius * 0.5);
            double offsetZ = centerZ + random.nextGaussian() * radius;

            if (Math.sqrt(
                    Math.pow(offsetX - centerX, 2) +
                    Math.pow(offsetY - centerY, 2) +
                    Math.pow(offsetZ - centerZ, 2)) < radius) {

                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(
                        (int) offsetX, (int) offsetY, (int) offsetZ);

                if (level.getBlockState(pos).is(net.minecraft.tags.BlockTags.STONE_ORE_REPLACEABLES)) {
                    level.setBlock(pos, oreBlock.defaultBlockState(), 3);
                }
            }
        }
    }
}
