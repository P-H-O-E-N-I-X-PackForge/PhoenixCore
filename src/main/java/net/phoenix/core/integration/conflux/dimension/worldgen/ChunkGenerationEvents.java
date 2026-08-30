package net.phoenix.core.integration.conflux.dimension.worldgen;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.phoenix.core.integration.conflux.dimension.DisciplineChunkGenerator;

@Mod.EventBusSubscriber(modid = "phoenixcore", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChunkGenerationEvents {

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        LevelChunk chunk = (LevelChunk) event.getChunk();
        Level level = chunk.getLevel();

        if (!(level instanceof ServerLevel serverLevel) || level.isClientSide) return;

        if (!isDisciplineDimension(level)) return;

        if (!(serverLevel.getChunkSource().getGenerator() instanceof DisciplineChunkGenerator generator)) {
            return;
        }

        WorldgenProfile profile = generator.getWorldgenProfile();
        if (profile == null) return;

        applySignatureFeatures(level, generator.getDisciplineId(), chunk.getPos().x, chunk.getPos().z);
    }

    private static boolean isDisciplineDimension(Level level) {
        String dimensionId = level.dimension().location().toString();
        return dimensionId.startsWith("phoenixcore:conflux/");
    }

    private static void applySignatureFeatures(Level level, String disciplineId, int chunkX, int chunkZ) {
        switch (disciplineId) {
            case "phoenix" -> applyPhoenixSignature(level, chunkX, chunkZ);
            case "sculk" -> applySculkSignature(level, chunkX, chunkZ);
            case "void" -> applyVoidSignature(level, chunkX, chunkZ);
            case "sealed_a" -> applySealedASignature(level, chunkX, chunkZ);
            case "sealed_b" -> applySealedBSignature(level, chunkX, chunkZ);
        }
    }

    private static void applyPhoenixSignature(Level level, int chunkX, int chunkZ) {

    }

    private static void applySculkSignature(Level level, int chunkX, int chunkZ) {

    }

    private static void applyVoidSignature(Level level, int chunkX, int chunkZ) {

    }

    private static void applySealedASignature(Level level, int chunkX, int chunkZ) {

    }

    private static void applySealedBSignature(Level level, int chunkX, int chunkZ) {

    }
}
