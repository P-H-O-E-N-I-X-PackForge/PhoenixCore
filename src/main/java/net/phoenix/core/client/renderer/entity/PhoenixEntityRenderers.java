package net.phoenix.core.client.renderer.entity;


import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.entity.PhoenixEntities;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PhoenixEntityRenderers {

    private PhoenixEntityRenderers() {}

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(TetoSlimeRenderer.BODY, TetoSlimeModel::createBodyLayer);
        event.registerLayerDefinition(TetoSlimeRenderer.OUTER, TetoSlimeModel::createOuterLayer);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(PhoenixEntities.TETO_SLIME.get(), TetoSlimeRenderer::new);
    }
}
