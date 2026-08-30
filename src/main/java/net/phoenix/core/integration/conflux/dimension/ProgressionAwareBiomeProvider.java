package net.phoenix.core.integration.conflux.dimension;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.UUID;
import java.util.stream.Stream;

public class ProgressionAwareBiomeProvider extends BiomeSource {

    private final String disciplineId;
    private final UUID teamId;
    private final DisciplineTheme theme;
    private final Holder<Biome> defaultBiome;

    public ProgressionAwareBiomeProvider(
            String disciplineId,
            UUID teamId,
            Holder<Biome> defaultBiome) {
        super();
        this.disciplineId = disciplineId;
        this.teamId = teamId;
        this.defaultBiome = defaultBiome;

        DisciplineTheme theme = DisciplineThemeRegistry.getTheme(disciplineId);
        if (theme == null) {
            throw new IllegalArgumentException("Unknown discipline theme: " + disciplineId);
        }
        this.theme = theme;
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return null;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(defaultBiome);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        return getBiomeForProgression(x, z);
    }

    private Holder<Biome> getBiomeForProgression(int quartX, int quartZ) {
        
        return defaultBiome;
    }

    @Override
    public void addDebugInfo(java.util.List<String> list, net.minecraft.core.BlockPos blockPos,
                             Climate.Sampler sampler) {
        list.add("Discipline: " + disciplineId);
        list.add("Team: " + teamId);
    }
}