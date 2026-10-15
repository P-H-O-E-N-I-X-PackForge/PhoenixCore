package net.phoenix.core.integration.conflux.dimension.client;



import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.phoenix.core.integration.conflux.dimension.effects.DimensionEffectsManager;

import com.mojang.blaze3d.vertex.PoseStack;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = "phoenixcore", value = Dist.CLIENT)
public class ConfluxClientEvents {

    private static final int EFFECTS_UPDATE_INTERVAL = 4;

    @Nullable
    private static String lastInitializedDiscipline;

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        String currentDiscipline = ClientDisciplineProgressionCache.getCurrentDiscipline();
        if (currentDiscipline != null) {
            PoseStack poseStack = event.getPoseStack();
            DisciplineSkyRenderer.renderCustomSky(poseStack, mc.level, event.getPartialTick().getGameTimeDeltaPartialTick(false));
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onClientTick(ClientTickEvent.Post event) {

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (mc.level.getGameTime() % EFFECTS_UPDATE_INTERVAL != 0) return;

        String currentDiscipline = ClientDisciplineProgressionCache.getCurrentDiscipline();
        if (currentDiscipline == null) return;

        DimensionEffectsManager manager = DimensionEffectsManager.getInstance();
        if (!currentDiscipline.equals(lastInitializedDiscipline)) {
            manager.initializeForDimension(currentDiscipline);
            lastInitializedDiscipline = currentDiscipline;
        }

        manager.tick(mc.level, mc.player);
    }
}
