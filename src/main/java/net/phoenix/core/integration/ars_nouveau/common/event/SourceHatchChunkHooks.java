package net.phoenix.core.integration.ars_nouveau.common.event;


import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.ars_nouveau.common.data.multiblock.part.source.SourceHatchPartMachine;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID)
public final class SourceHatchChunkHooks {

    private SourceHatchChunkHooks() {}

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) return;

        chunk.getBlockEntities().forEach((pos, be) -> {

            if (be instanceof SourceHatchPartMachine) {
                SourceHatchTracker.add(level.dimension(), pos);
            }
        });
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) return;

        chunk.getBlockEntities().forEach((pos, be) -> {

            if (be instanceof SourceHatchPartMachine) {
                SourceHatchTracker.remove(level.dimension(), pos);
            }
        });
    }
}
