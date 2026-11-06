package net.phoenix.core.client.renderer.cinema.cutscene;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.client.renderer.cinema.cutscene.background.CutsceneBackgrounds;
import net.phoenix.core.common.block.cinema.Cutscenes;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CutsceneClientEvents {

    private CutsceneClientEvents() {}

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(CutsceneBackgrounds.PresetLoader.INSTANCE);
        event.registerReloadListener(CutsceneManager.INSTANCE);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        Cutscenes.knownCutscenes = CutsceneManager.INSTANCE::ids;
    }
}
