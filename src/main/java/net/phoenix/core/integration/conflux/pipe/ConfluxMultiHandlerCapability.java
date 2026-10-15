package net.phoenix.core.integration.conflux.pipe;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.phoenix.core.PhoenixCore;

import org.jetbrains.annotations.Nullable;

public final class ConfluxMultiHandlerCapability {

    public static final BlockCapability<IConfluxMultiHandler, Direction> MULTI_DATA = BlockCapability
            .createSided(PhoenixCore.id("conflux_multi_data"), IConfluxMultiHandler.class);

    public interface Holder {

        @Nullable
        IConfluxMultiHandler getConfluxMultiHandler(@Nullable Direction side);
    }

    private ConfluxMultiHandlerCapability() {}

    @Nullable
    public static IConfluxMultiHandler find(BlockEntity be, Direction side) {
        if (be.getLevel() == null) return null;
        return be.getLevel().getCapability(MULTI_DATA, be.getBlockPos(), be.getBlockState(), be, side);
    }

    public static void register(RegisterCapabilitiesEvent event) {
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block instanceof MetaMachineBlock || block.getClass().getName().startsWith("net.phoenix.core")) {
                event.registerBlock(MULTI_DATA, (level, pos, state, be, side) -> be instanceof Holder h ?
                        h.getConfluxMultiHandler(side) : null, block);
            }
        }
    }
}
