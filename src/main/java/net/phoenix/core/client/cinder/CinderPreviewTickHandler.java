package net.phoenix.core.client.cinder;

import com.gregtechceu.gtceu.api.multiblock.util.BlockInfo;
import com.gregtechceu.gtceu.client.mui.schema.MutableSchema;

import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;

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
import net.phoenix.core.common.item.cinder.CinderCoreItem;
import net.phoenix.core.common.item.cinder.CinderSchemaData;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Drives {@link CinderPreviewState} continuously off the vanilla crosshair pick result - the same way
 * vanilla itself decides where its own block-placement ghost outline goes - instead of requiring an
 * explicit click to move the preview, then hands the resolved placement off to
 * {@link CinderStructureGhostRenderer} - a fork of GTCEu's own in-world ghost renderer (the exact one
 * an unformed multiblock controller uses for its own shift-right-click preview), with a green/red
 * validity tint added on top, instead of a hand-rolled bounding-box outline. That renderer's
 * {@code showPreview} only stays visible for the duration passed in, so this re-issues the call every
 * tick while active to keep it alive continuously; it naturally fades out within one short cycle of
 * the preview going inactive, with nothing else to explicitly cancel. Also drives that renderer's own
 * {@code clientTick}/{@code draw} - GTCEu's copy is driven by its own internal client event listener,
 * which only knows about GTCEu's own singleton, not this fork. Right-clicking (see
 * {@code CinderCoreItem}) only ever commits whatever this has already resolved.
 */
@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class CinderPreviewTickHandler {

    /** Short enough to disappear quickly once the preview stops refreshing it, long enough (with a
     *  re-issue every tick) to never visibly flicker while it's still active. */
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
        showGhostPreview(CinderPreviewState.INSTANCE.getPlacements(), CinderPreviewState.INSTANCE.isValid());
    }

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        CinderStructureGhostRenderer.INSTANCE.draw(event.getPoseStack(), mc.renderBuffers().bufferSource(),
                mc.gameRenderer.getMainCamera(), event.getStage(), mc.getFrameTime());
    }

    /** Builds GTCEu's own {@link MutableSchema} from the resolved placement and hands it to
     *  {@link CinderStructureGhostRenderer} - mirroring {@code MultiblockControllerMachine#onUse}'s
     *  exact call sequence (construct the schema from the block map first, then read the anchor
     *  position back off the schema itself via {@code getControllerPos()} rather than supplying one
     *  directly, since that's the one GTCEu's own code actually uses). */
    private static void showGhostPreview(@Nullable Map<BlockPos, BlockInfo> placements, boolean valid) {
        if (placements == null || placements.isEmpty()) return;

        Long2ReferenceMap<BlockState> blocks = new Long2ReferenceOpenHashMap<>(placements.size());
        for (var entry : placements.entrySet()) {
            blocks.put(entry.getKey().asLong(), entry.getValue().getBlockState());
        }

        MutableSchema schema = new MutableSchema(blocks);
        CinderStructureGhostRenderer.INSTANCE.showPreview(schema.getControllerPos(), schema, valid,
                PREVIEW_DISPLAY_TICKS);
    }

    /** Main hand takes priority, matching vanilla's own main-hand-first item-use ordering. */
    private static @Nullable InteractionHand findConfiguredHand(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof CinderCoreItem && CinderSchemaData.getTargetId(main) != null) {
            return InteractionHand.MAIN_HAND;
        }
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof CinderCoreItem && CinderSchemaData.getTargetId(off) != null) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }
}
