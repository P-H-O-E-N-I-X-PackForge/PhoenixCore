package net.phoenix.core.integration.conflux.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.phoenix.core.integration.conflux.dimension.DisciplineChunkGenerator;

import java.util.Optional;

public class ConfluxStructure extends Structure {

    public static final Codec<ConfluxStructure> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            settingsCodec(instance),
            Codec.STRING.fieldOf("kind").forGetter(s -> s.kind),
            Codec.STRING.fieldOf("theme").forGetter(s -> s.theme)).apply(instance, ConfluxStructure::new));

    private final String kind;
    private final String theme;

    public ConfluxStructure(StructureSettings settings, String kind, String theme) {
        super(settings);
        this.kind = kind;
        this.theme = theme;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ConfluxPiece.Kind k = ConfluxPiece.Kind.byName(kind);
        ChunkPos chunk = context.chunkPos();
        int cx = chunk.getMiddleBlockX();
        int cz = chunk.getMiddleBlockZ();
        ChunkGenerator generator = context.chunkGenerator();
        LevelHeightAccessor heights = context.heightAccessor();
        RandomState randomState = context.randomState();

        int reach = Math.max(k.radiusX(), k.radiusZ());
        int low = Integer.MAX_VALUE;
        int high = Integer.MIN_VALUE;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int h = generator.getFirstOccupiedHeight(cx + dx * k.radiusX(), cz + dz * k.radiusZ(),
                        Heightmap.Types.WORLD_SURFACE_WG, heights, randomState);
                low = Math.min(low, h);
                high = Math.max(high, h);
            }
        }
        
        if (high - low > k.maxSlope()) return Optional.empty();
        if (low <= generator.getSeaLevel() + 2) return Optional.empty();
        if (generator instanceof DisciplineChunkGenerator discipline &&
                discipline.landformNear(cx, cz, reach + 8, randomState)) {
            return Optional.empty();
        }

        long seed = context.random().nextLong();
        BlockPos origin = new BlockPos(cx, high, cz);
        return Optional.of(new GenerationStub(origin,
                builder -> builder.addPiece(new ConfluxPiece(k, theme, origin, seed))));
    }

    @Override
    public StructureType<?> type() {
        return ConfluxStructures.CONFLUX.get();
    }
}
