package net.phoenix.core.integration.ars_nouveau.api.capability;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.phoenix.core.PhoenixCore;

import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import org.jetbrains.annotations.Nullable;

public interface ISourceProviderCapability {

    BlockCapability<ISourceProviderCapability, Direction> CAPABILITY = BlockCapability
            .createSided(PhoenixCore.id("source_provider"), ISourceProviderCapability.class);

    ISourceTile getSource();

    @Nullable
    static ISourceProviderCapability find(BlockEntity be) {
        if (be.getLevel() == null) return null;
        return be.getLevel().getCapability(CAPABILITY, be.getBlockPos(), be.getBlockState(), be, null);
    }

    static void register(RegisterCapabilitiesEvent event) {
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block instanceof MetaMachineBlock) {
                event.registerBlock(CAPABILITY,
                        (level, pos, state, be, side) -> be instanceof ISourceProviderCapability p ?
                                p : null,
                        block);
            }
        }
    }
}
