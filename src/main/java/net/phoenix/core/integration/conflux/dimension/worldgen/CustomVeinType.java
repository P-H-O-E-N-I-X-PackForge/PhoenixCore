package net.phoenix.core.integration.conflux.dimension.worldgen;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;

public abstract class CustomVeinType {

    protected final String name;
    protected final String description;

    public CustomVeinType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public abstract void generate(
                                  WorldGenLevel level,
                                  int chunkX,
                                  int chunkZ,
                                  RandomSource random,
                                  VeinGenerationContext context);

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public static class VeinGenerationContext {

        public final String materialName;
        public final int minY;
        public final int maxY;
        public final int density;
        public final BlockState blockState;

        public VeinGenerationContext(
                                     String materialName,
                                     int minY,
                                     int maxY,
                                     int density,
                                     BlockState blockState) {
            this.materialName = materialName;
            this.minY = minY;
            this.maxY = maxY;
            this.density = density;
            this.blockState = blockState;
        }
    }
}
