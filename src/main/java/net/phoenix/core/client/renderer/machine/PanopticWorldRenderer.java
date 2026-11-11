package net.phoenix.core.client.renderer.machine;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.conflux.panoptic.PanopticEngineMachine;

import com.mojang.blaze3d.vertex.PoseStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID, value = Dist.CLIENT)
public final class PanopticWorldRenderer {

    private PanopticWorldRenderer() {}

    private static final double RANGE = 256.0;
    private static final Set<PanopticEngineMachine> ENGINES = new LinkedHashSet<>();

    public static synchronized void add(PanopticEngineMachine engine) {
        ENGINES.add(engine);
    }

    public static synchronized void remove(PanopticEngineMachine engine) {
        ENGINES.remove(engine);
    }

    private static synchronized List<PanopticEngineMachine> all() {
        return new ArrayList<>(ENGINES);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        synchronized (PanopticWorldRenderer.class) {
            ENGINES.clear();
        }
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Vec3 camera = event.getCamera().getPosition();
        var buffers = mc.renderBuffers().bufferSource();
        boolean drew = false;

        for (PanopticEngineMachine engine : all()) {
            if (!engine.isFormed() || engine.getLevel() != mc.level) continue;
            BlockPos pos = engine.getBlockPos();
            if (pos.distToCenterSqr(camera) > RANGE * RANGE) continue;

            AABB reach = new AABB(pos).inflate(34, 0, 34).expandTowards(0, 62, 0);
            if (!event.getFrustum().isVisible(reach)) continue;

            PoseStack ps = event.getPoseStack();
            ps.pushPose();
            ps.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
            int light = LevelRenderer.getLightColor(mc.level, pos.above());
            PanopticTreeRender.draw(engine, event.getPartialTick().getGameTimeDeltaPartialTick(false), ps, buffers,
                    light, Math.sqrt(pos.distToCenterSqr(camera)));
            ps.popPose();
            drew = true;
        }
        if (drew) buffers.endBatch(RenderType.entityCutoutNoCull(PanopticTreeRender.texture()));
    }
}
