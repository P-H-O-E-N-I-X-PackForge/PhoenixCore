package net.phoenix.core.integration.jade;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;

import net.minecraft.world.level.block.Block;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.jade.provider.FissionMachineProvider;
import net.phoenix.core.integration.jade.provider.HighPressurePlasmaArcFurnaceProvider;
import net.phoenix.core.integration.jade.provider.SourceHatchProvider;
import net.phoenix.core.integration.jade.provider.TeslaNetworkProvider;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class PhoenixJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        PhoenixCore.LOGGER.info("[PhoenixJade] register(common) called");

        registration.registerBlockDataProvider(new SourceHatchProvider(), MetaMachineBlockEntity.class);
        registration.registerBlockDataProvider(new TeslaNetworkProvider(), MetaMachineBlockEntity.class);
        registration.registerBlockDataProvider(new HighPressurePlasmaArcFurnaceProvider(),
                MetaMachineBlockEntity.class);
        registration.registerBlockDataProvider(new FissionMachineProvider(), MetaMachineBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        PhoenixCore.LOGGER.info("[PhoenixJade] register(client) called");

        registration.registerBlockComponent(new SourceHatchProvider(), Block.class);
        registration.registerBlockComponent(new TeslaNetworkProvider(), Block.class);
        registration.registerBlockComponent(new HighPressurePlasmaArcFurnaceProvider(), Block.class);
        registration.registerBlockComponent(new FissionMachineProvider(), Block.class);
    }
}
