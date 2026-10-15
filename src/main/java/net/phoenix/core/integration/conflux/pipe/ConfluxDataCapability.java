package net.phoenix.core.integration.conflux.pipe;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.capability.GTCapability;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.phoenix.core.PhoenixCore;

import org.jetbrains.annotations.Nullable;

public final class ConfluxDataCapability {

    public static final BlockCapability<IConfluxDataHandler, Direction> DATA = BlockCapability
            .createSided(PhoenixCore.id("conflux_data"), IConfluxDataHandler.class);

    /** Implemented by block entities (and GT machines, which are block entities) that expose a data handler. */
    public interface Holder {

        @Nullable
        IConfluxDataHandler getConfluxDataHandler(@Nullable Direction side);
    }

    private ConfluxDataCapability() {}

    @Nullable
    public static IConfluxDataHandler find(BlockEntity be, Direction side) {
        if (be.getLevel() == null) return null;
        return be.getLevel().getCapability(DATA, be.getBlockPos(), be.getBlockState(), be, side);
    }

    public static void register(RegisterCapabilitiesEvent event) {
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block instanceof ConfluxPipeBlock) {
                event.registerBlock(GTCapability.CAPABILITY_COVERABLE, (level, pos, state, be, side) -> be instanceof ConfluxPipeBlockEntity pipe ?
                        pipe.getCoverContainer() : null, block);
            }
            if (block instanceof MetaMachineBlock || block instanceof ConfluxPipeBlock) {
                event.registerBlock(DATA, (level, pos, state, be, side) -> be instanceof Holder h ?
                        h.getConfluxDataHandler(side) : null, block);
            }
        }
    }
}
