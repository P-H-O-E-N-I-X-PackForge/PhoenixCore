package net.phoenix.core.integration.ae2;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.phoenix.core.PhoenixCore;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ExpandedBufferChunkLoader {

    public static final TicketController CONTROLLER = new TicketController(PhoenixCore.id("expanded_pattern_buffer"));

    private ExpandedBufferChunkLoader() {}

    @SubscribeEvent
    public static void register(RegisterTicketControllersEvent event) {
        event.register(CONTROLLER);
    }

    public static void set(ServerLevel level, BlockPos buffer, boolean load) {
        ChunkPos chunk = new ChunkPos(buffer);
        CONTROLLER.forceChunk(level, buffer, chunk.x, chunk.z, load, true);
    }
}
