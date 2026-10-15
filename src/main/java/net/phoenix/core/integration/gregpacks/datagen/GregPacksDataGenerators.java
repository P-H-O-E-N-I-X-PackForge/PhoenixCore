package net.phoenix.core.integration.gregpacks.datagen;

import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.phoenix.core.PhoenixCore;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class GregPacksDataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        PackOutput packOutput = event.getGenerator().getPackOutput();

        if (event.includeServer()) {
            event.getGenerator().addProvider(
                    true,
                    GregPacksLootTableProvider.create(packOutput, event.getLookupProvider()));
            event.getGenerator().addProvider(true, new GregPacksCuriosProvider(packOutput));
        }
    }
}
