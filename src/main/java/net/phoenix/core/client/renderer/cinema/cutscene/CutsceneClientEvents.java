package net.phoenix.core.client.renderer.cinema.cutscene;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.client.renderer.cinema.cutscene.background.CutsceneBackgrounds;
import net.phoenix.core.common.block.cinema.Cutscenes;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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
