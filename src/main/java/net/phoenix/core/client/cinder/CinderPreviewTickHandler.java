package net.phoenix.core.client.cinder;

import com.gregtechceu.gtceu.api.multiblock.util.BlockInfo;
import com.gregtechceu.gtceu.client.mui.schema.MutableSchema;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.client.renderer.cinder.CinderStructureGhostRenderer;
import net.phoenix.core.common.item.cinder.CinderDeploySource;

import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class CinderPreviewTickHandler {

    private static final int PREVIEW_DISPLAY_TICKS = 4;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        CinderStructureGhostRenderer.INSTANCE.clientTick();

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null || mc.screen != null) {
            CinderPreviewState.INSTANCE.cancel();
            return;
        }

        InteractionHand hand = findConfiguredHand(player);
        if (hand == null) {
            CinderPreviewState.INSTANCE.cancel();
            return;
        }

        if (!(mc.hitResult instanceof BlockHitResult blockHit) || blockHit.getType() != HitResult.Type.BLOCK) {
            CinderPreviewState.INSTANCE.cancel();
            return;
        }

        var anchor = blockHit.getBlockPos().relative(blockHit.getDirection());
        Direction facing = player.getDirection();
        CinderPreviewState.INSTANCE.updateFromHover(hand, anchor, facing);

        CinderPreviewState.INSTANCE.tick();
        showGhostPreview(anchor, CinderPreviewState.INSTANCE.getPlacements(), CinderPreviewState.INSTANCE.isValid());
    }

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        CinderStructureGhostRenderer.INSTANCE.draw(event.getPoseStack(), mc.renderBuffers().bufferSource(),
                mc.gameRenderer.getMainCamera(), event.getStage(), mc.getFrameTime());
    }

    /**
     * Fix for the long-standing "preview does not show" issue: {@link MutableSchema}'s blocks must be
     * keyed in structure-local space (relative to wherever the renderer will translate the whole preview
     * to), not world-absolute - confirmed by decompiling GTCEu's own {@code PatternPreviewRenderer} (the
     * class this renderer is forked from): its compile step translates each block purely by its own
     * schema-local {@code pos}, with the real world position applied exactly once, separately, as the
     * {@code controllerPos}/{@code anchorPos} argument passed into {@code showPreview}. This method used
     * to key the schema with {@code CinderSchemaData#resolvePlacement}'s world-absolute positions
     * unchanged and then ask the schema to auto-detect its own controller position - meaning every block
     * rendered at (real anchor + real world position), doubled and displaced far from the actual build
     * site, and the auto-detected controller position depended on the fake preview level correctly
     * instantiating a real multiblock controller block entity, which isn't guaranteed. Both are avoided
     * here: positions are explicitly localized to {@code anchor} before building the schema, and the
     * already-known real anchor is passed directly instead of trusting {@code schema.getControllerPos()}.
     */
    private static void showGhostPreview(BlockPos anchor, @Nullable Map<BlockPos, BlockInfo> placements,
                                         boolean valid) {
        if (placements == null || placements.isEmpty()) return;

        Long2ReferenceMap<BlockState> blocks = new Long2ReferenceOpenHashMap<>(placements.size());
        for (var entry : placements.entrySet()) {
            BlockPos local = entry.getKey().subtract(anchor);
            blocks.put(local.asLong(), entry.getValue().getBlockState());
        }

        MutableSchema schema = new MutableSchema(blocks);
        CinderStructureGhostRenderer.INSTANCE.showPreview(anchor, schema, valid, PREVIEW_DISPLAY_TICKS);
    }

    private static @Nullable InteractionHand findConfiguredHand(Player player) {
        ItemStack main = player.getMainHandItem();
        if (CinderDeploySource.hasConfiguredTarget(main)) return InteractionHand.MAIN_HAND;
        ItemStack off = player.getOffhandItem();
        if (CinderDeploySource.hasConfiguredTarget(off)) return InteractionHand.OFF_HAND;
        return null;
    }
}
