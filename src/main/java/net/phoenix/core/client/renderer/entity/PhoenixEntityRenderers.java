package net.phoenix.core.client.renderer.entity;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.entity.PhoenixEntities;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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
