package net.phoenix.core.integration.drone;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.phoenix.core.PhoenixCore;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class DroneChunkLoader {

    public static final TicketController CONTROLLER = new TicketController(PhoenixCore.id("drone_keep_running"));

    private DroneChunkLoader() {}

    @SubscribeEvent
    public static void register(RegisterTicketControllersEvent event) {
        event.register(CONTROLLER);
    }

    public static void set(ServerLevel level, BlockPos drone, long chunk, boolean load) {
        CONTROLLER.forceChunk(level, drone, net.minecraft.world.level.ChunkPos.getX(chunk),
                net.minecraft.world.level.ChunkPos.getZ(chunk), load, true);
    }
}
