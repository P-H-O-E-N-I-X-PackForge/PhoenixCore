package net.phoenix.core.client;

import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderManager;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.client.renderer.NukePrimedRenderer;
import net.phoenix.core.client.renderer.gui.SourceHatchScreen;
import net.phoenix.core.client.renderer.machine.*;
import net.phoenix.core.common.block.PhoenixBlocks;
import net.phoenix.core.common.registry.PhoenixFissionEntities;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class PhoenixClient {

    private PhoenixClient() {}

    public static void init(IEventBus modBus) {
        DynamicRenderManager.register(PhoenixCore.id("eye_of_harmony"), EyeOfHarmonyRender.TYPE);
        DynamicRenderManager.register(PhoenixCore.id("artificial_star"), ArtificialStarRender.TYPE);
        DynamicRenderManager.register(PhoenixCore.id("plasma_arc_furnace"), PlasmaArcFurnaceRender.TYPE);
        DynamicRenderManager.register(PhoenixCore.id("custom_fluid"), CustomFluidRender.TYPE);
        DynamicRenderManager.register(PhoenixCore.id("helical_fusion"), HelicalFusionRenderer.TYPE);
        DynamicRenderManager.register(PhoenixCore.id("honey_chamber"), HoneyChamberDynamicRender.TYPE);
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(EyeOfHarmonyRender.SPACE_SHELL_MODEL_RL);
        event.register(EyeOfHarmonyRender.STAR_MODEL_RL);
        EyeOfHarmonyRender.ORBIT_OBJECTS_RL.forEach(event::register);
        event.register(ArtificialStarRender.ARTIFICIAL_STAR_MODEL_RL);
        event.register(PlasmaArcFurnaceRender.RINGS_MODEL_RL);
        event.register(PlasmaArcFurnaceRender.SPHERE_MODEL_RL);
    }

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(PhoenixCore.SOURCE_HATCH_MENU.get(), SourceHatchScreen::new);
            ItemBlockRenderTypes.setRenderLayer(PhoenixBlocks.COIL_TRUE_HEAT_STABLE.get(), RenderType.cutoutMipped());
            EntityRenderers.register(PhoenixFissionEntities.NUKE_PRIMED.get(), NukePrimedRenderer::new);
        });
    }
}
