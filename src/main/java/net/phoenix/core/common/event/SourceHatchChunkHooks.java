package net.phoenix.core.common.event;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.machine.multiblock.part.special.SourceHatchPartMachine;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SourceHatchChunkHooks {

    private SourceHatchChunkHooks() {}

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) return;

        chunk.getBlockEntities().forEach((pos, be) -> {
            if (be instanceof MetaMachineBlockEntity metaBE &&
                    metaBE.getMetaMachine() instanceof SourceHatchPartMachine) {
                SourceHatchTracker.add(level.dimension(), pos);
            }
        });
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getChunk() instanceof LevelChunk chunk)) return;

        chunk.getBlockEntities().forEach((pos, be) -> {
            if (be instanceof MetaMachineBlockEntity metaBE &&
                    metaBE.getMetaMachine() instanceof SourceHatchPartMachine) {
                SourceHatchTracker.remove(level.dimension(), pos);
            }
        });
    }
}
