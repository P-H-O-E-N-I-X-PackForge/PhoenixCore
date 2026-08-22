package net.phoenix.core.integration.conflux.dimension;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.PhoenixCore;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID)
public class ChunkCapabilityHandler {

    @SubscribeEvent
    public static void attachChunkCapabilities(AttachCapabilitiesEvent<LevelChunk> event) {
        if (!event.getObject().getCapability(ChunkProgressionProvider.CHUNK_PROGRESSION_CAP).isPresent()) {
            event.addCapability(
                  ResourceLocation.fromNamespaceAndPath("phoenixcore", "chunk_progression"),
                    new ChunkProgressionProvider()
            );
        }
    }
}