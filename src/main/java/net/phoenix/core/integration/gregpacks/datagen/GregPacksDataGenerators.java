package net.phoenix.core.integration.gregpacks.datagen;

import net.minecraft.data.PackOutput;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.PhoenixCore;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class GregPacksDataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        PackOutput packOutput = event.getGenerator().getPackOutput();

        if (event.includeServer()) {
            event.getGenerator().addProvider(
                    true,
                    GregPacksLootTableProvider.create(packOutput));
            event.getGenerator().addProvider(true, new GregPacksCuriosProvider(packOutput));
        }
    }
}