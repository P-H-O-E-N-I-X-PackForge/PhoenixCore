package net.phoenix.core.integration.jade;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import net.minecraft.world.level.block.Block;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.gregpacks.common.block.OmniPackBlock;
import net.phoenix.core.integration.gregpacks.common.block.OmniPackBlockEntity;
import net.phoenix.core.integration.jade.provider.*;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class PhoenixJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        PhoenixCore.LOGGER.info("[PhoenixJade] register(common) called");

        registration.registerBlockDataProvider(new SourceMachineProvider(), MetaMachine.class);
        registration.registerBlockDataProvider(new SourceTankJadeProvider(), MetaMachine.class);
        registration.registerBlockDataProvider(new SourceHatchProvider(), MetaMachine.class);
        registration.registerBlockDataProvider(new TeslaNetworkProvider(), MetaMachine.class);
        registration.registerBlockDataProvider(new HighPressurePlasmaArcFurnaceProvider(), MetaMachine.class);
        registration.registerBlockDataProvider(new ThreadedRecipeOutputProvider(), MetaMachine.class);
        registration.registerBlockDataProvider(new AstralThreadHatchProvider(), MetaMachine.class);
        registration.registerBlockDataProvider(new JadeGregPackProvider(), OmniPackBlockEntity.class);
        registration.registerBlockDataProvider(new VaultProvider(), MetaMachine.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        PhoenixCore.LOGGER.info("[PhoenixJade] register(client) called");

        registration.registerBlockComponent(new SourceMachineProvider(), Block.class);
        registration.registerBlockComponent(new SourceTankJadeProvider(), Block.class);
        registration.registerBlockComponent(new SourceHatchProvider(), Block.class);
        registration.registerBlockComponent(new TeslaNetworkProvider(), Block.class);
        registration.registerBlockComponent(new HighPressurePlasmaArcFurnaceProvider(), Block.class);
        registration.registerBlockComponent(new ThreadedRecipeOutputProvider(), Block.class);
        registration.registerBlockComponent(new AstralThreadHatchProvider(), Block.class);
        registration.registerBlockComponent(new JadeGregPackProvider(), OmniPackBlock.class);
        registration.registerBlockComponent(new VaultProvider(), Block.class);
    }
}
