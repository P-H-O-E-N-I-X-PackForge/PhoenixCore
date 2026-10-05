package net.phoenix.core.integration.conflux.dimension.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.integration.conflux.dimension.effects.DimensionEffectsManager;

import com.mojang.blaze3d.vertex.PoseStack;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = "phoenixcore", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ConfluxClientEvents {

    // How often (in client ticks) the ambient particle/lighting/physics systems update - these are
    // background atmosphere, not something that needs full 20/tick precision, and running them every
    // tick was never the intent (see ParticleEffectSystem's rescaled densities).
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
            DisciplineSkyRenderer.renderCustomSky(poseStack, mc.level, event.getPartialTick());
        }
    }

    // Used to be an empty body - DimensionEffectsManager (ambient particles/lighting/physics per
    // discipline) was fully implemented but never actually ticked or told which discipline to
    // initialize for, so none of it ever ran despite this handler being correctly registered.
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

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
