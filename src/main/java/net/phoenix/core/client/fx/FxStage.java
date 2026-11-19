package net.phoenix.core.client.fx;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.client.renderer.machine.GlowDraw;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Draws the shader renders of the machines from the level render instead of from their block entities. A block entity
 * is only drawn while its own block is on screen, so a render that is bigger than the block, as these are, would pop
 * out of existence the moment the controller left the edge of the view. This finds the machines around the player once
 * in a while and draws each every frame no matter where the controller is.
 *
 * A render opts in by registering the machine class and a function that draws it. While that function runs,
 * {@link #active()} is true, so the same render method can tell a call from here from a call from the block-entity
 * pass.
 */
@EventBusSubscriber(modid = PhoenixCore.MOD_ID, value = Dist.CLIENT)
public final class FxStage {

    public interface Drawer {

        void draw(MetaMachine machine, float partialTick, PoseStack poseStack, MultiBufferSource buffer);
    }

    private record Entry(Class<? extends MetaMachine> type, double range, Drawer drawer) {}

    private record Found(MetaMachine machine, Entry entry) {}

    private static final List<Entry> ENTRIES = new CopyOnWriteArrayList<>();
    private static volatile List<Found> found = List.of();
    private static boolean active;
    private static int ticks;
    private static int lastDrawn = -1;
    private static boolean failed;
    private static int lastFound = -1;

    private FxStage() {}

    public static void register(Class<? extends MetaMachine> type, double range, Drawer drawer) {
        ENTRIES.add(new Entry(type, range, drawer));
    }

    /** True while a render is being called from the level render rather than from a block entity. */
    public static boolean active() {
        return active;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        found = List.of();
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (++ticks < 10) return;
        ticks = 0;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || ENTRIES.isEmpty()) {
            found = List.of();
            return;
        }
        int radius = Math.min(8, Math.max(2, mc.options.getEffectiveRenderDistance()));
        int cx = mc.player.chunkPosition().x;
        int cz = mc.player.chunkPosition().z;
        List<Found> out = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                LevelChunk chunk = mc.level.getChunkSource().getChunkNow(cx + dx, cz + dz);
                if (chunk == null) continue;
                for (BlockPos pos : chunk.getBlockEntities().keySet()) {
                    MetaMachine machine = MetaMachine.getMachine(mc.level, pos);
                    if (machine == null) continue;
                    for (Entry e : ENTRIES) {
                        if (e.type().isInstance(machine)) {
                            out.add(new Found(machine, e));
                            break;
                        }
                    }
                }
            }
        }
        if (out.size() != lastFound) {
            lastFound = out.size();
            PhoenixCore.LOGGER.info("[FxStage] found {} machines with shader renders nearby", out.size());
        }
        found = out;
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        List<Found> list = found;
        Minecraft mc = Minecraft.getInstance();
        // Right after the solid things are drawn, before water and glass, a copy of the depth is taken. The machines
        // are
        // drawn after the translucent blocks, so those lie over them, and stop at that copy, so only solid things hide
        // them.
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            if (!list.isEmpty() && mc.level != null) SceneDepth.capture();
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (list.isEmpty()) return;
        if (mc.level == null) return;

        net.phoenix.core.client.renderer.machine.FxVolume.adapt(mc.getFps());
        Vec3 cam = event.getCamera().getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        Matrix4fStack stack = RenderSystem.getModelViewStack();
        stack.pushMatrix();
        stack.set(new Matrix4f(event.getModelViewMatrix()));
        RenderSystem.applyModelViewMatrix();
        // the projection is set from the event too, rather than trusting what the render left in place
        Matrix4f oldProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting oldSorting = RenderSystem.getVertexSorting();
        RenderSystem.setProjectionMatrix(new Matrix4f(event.getProjectionMatrix()), VertexSorting.DISTANCE_TO_ORIGIN);
        active = true;
        int drawn = 0;
        try {
            for (Found f : list) {
                MetaMachine machine = f.machine();
                if (machine.getLevel() != mc.level) continue;
                BlockPos pos = machine.getBlockPos();
                double dx = pos.getX() + 0.5 - cam.x;
                double dy = pos.getY() + 0.5 - cam.y;
                double dz = pos.getZ() + 0.5 - cam.z;
                double range = f.entry().range() + 8.0;
                if (dx * dx + dy * dy + dz * dz > range * range) continue;
                PoseStack poseStack = new PoseStack();
                poseStack.translate(pos.getX() - cam.x, pos.getY() - cam.y, pos.getZ() - cam.z);
                try {
                    f.entry().drawer().draw(machine, partial, poseStack, buffers);
                    drawn++;
                } catch (RuntimeException ex) {
                    if (!failed) PhoenixCore.LOGGER.warn("[FxStage] drawing {} failed", machine.getBlockPos(), ex);
                    failed = true;
                }
            }
        } finally {
            active = false;
            stack.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(oldProjection, oldSorting);
        }
        if (drawn != lastDrawn) {
            lastDrawn = drawn;
            PhoenixCore.LOGGER.info("[FxStage] drawing {} machine renders ({} known nearby)", drawn, list.size());
        }
        buffers.endBatch(GlowDraw.DARK);
        buffers.endBatch(GlowDraw.GLOW);
        SceneDepth.release();
    }
}
