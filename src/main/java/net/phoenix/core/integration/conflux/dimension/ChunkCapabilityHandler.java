package net.phoenix.core.integration.conflux.dimension;


import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.event.AttachCapabilitiesEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.phoenix.core.PhoenixCore;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID)
public class ChunkCapabilityHandler {

    @SubscribeEvent
    public static void attachChunkCapabilities(AttachCapabilitiesEvent<LevelChunk> event) {
        if (!event.getObject().getCapability(ChunkProgressionProvider.CHUNK_PROGRESSION_CAP).isPresent()) {
            event.addCapability(
                    ResourceLocation.fromNamespaceAndPath("phoenixcore", "chunk_progression"),
                    new ChunkProgressionProvider());
        }
    }
}
