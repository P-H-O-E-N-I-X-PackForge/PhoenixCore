package net.phoenix.core.integration.conflux.dimension.sky;


import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.phoenix.core.PhoenixCore;

@EventBusSubscriber(modid = "phoenixcore", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class DisciplineSkyEffectsRegistrar {

    @SubscribeEvent
    public static void onRegisterDimensionSpecialEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(PhoenixCore.id("discipline"), new DisciplineSkyEffects());
    }
}
