package net.phoenix.core.client.cinder;

import com.gregtechceu.gtceu.api.multiblock.util.BlockInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.phoenix.core.common.item.cinder.CinderCoreItem;
import net.phoenix.core.common.item.cinder.CinderSchemaData;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.network.packet.C2SCinderCommitPacket;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Client-only "where is this Core about to build, and would it actually work" state - not synced,
 * matching this codebase's established convention of keeping this kind of transient UX state off the
 * network (see the wing-suit charging sparks). Continuously tracks wherever the player is currently
 * aiming while holding a configured Core (see {@code CinderPreviewTickHandler}, which drives
 * {@link #updateFromHover} every client tick off the vanilla crosshair pick result), the same way
 * vanilla shows its own block-placement ghost outline - no click needed to move it. Right-clicking
 * commits whatever's currently previewed (sends {@link C2SCinderCommitPacket} and lets the server redo
 * the resolution authoritatively - this is display-only, never trusted for the actual build).
 * <p>
 * The resolved placement is cached rather than recomputed every frame - resolving a schema re-runs
 * GTCEu's full pattern-resolution algorithm, which is too expensive to call at 60fps. It's refreshed
 * whenever the preview moves and periodically afterward (see {@link #tick}) so Configurator edits or
 * stocked-material changes eventually show up without needing to look away and back.
 */
public final class CinderPreviewState {

    public static final CinderPreviewState INSTANCE = new CinderPreviewState();

    private static final Direction UP_FACING = Direction.UP;
    private static final int REFRESH_INTERVAL_TICKS = 20;

    /** How long a "not enough materials" rejection keeps the force-build window open for a follow-up
     *  right-click at the same spot - see {@link #armForceBuild}. */
    private static final int FORCE_WINDOW_TICKS = 60;

    private boolean active;
    private InteractionHand hand;
    private BlockPos anchor;
    private Direction facing = Direction.NORTH;

    private @Nullable CinderSchemaData.ResolvedPlacement resolved;
    private boolean materialsSufficient;
    private int refreshCooldown;

    private @Nullable BlockPos forceArmedAnchor;
    private int forceArmedTicksLeft;

    private CinderPreviewState() {}

    /** Called every client tick from {@code CinderPreviewTickHandler} while the player aims at a block
     *  with a configured Core in hand. Only forces an immediate re-resolve when the aimed-at position
     *  actually changed - called 20x/second, so unconditionally resolving here would defeat the whole
     *  point of caching it (see class doc). */
    public void updateFromHover(InteractionHand hand, BlockPos anchor, Direction facing) {
        boolean moved = !active || this.hand != hand || !anchor.equals(this.anchor) || this.facing != facing;
        this.active = true;
        this.hand = hand;
        this.anchor = anchor;
        this.facing = facing;
        if (moved) this.refreshCooldown = 0;
    }

    public void cancel() {
        this.active = false;
        this.resolved = null;
    }

    public void commit() {
        if (!active) return;
        boolean force = isForceArmed();
        PhoenixNetwork.CHANNEL.sendToServer(new C2SCinderCommitPacket(hand, anchor, facing, UP_FACING, false, force));
        forceArmedAnchor = null;
        cancel();
    }

    /** Called when the server rejects a commit specifically for insufficient materials - opens a short
     *  window in which a second right-click at the same spot sends {@code force=true} instead of
     *  repeating the same doomed strict attempt, backing "right-click twice to build with whatever's
     *  stocked, leaving gaps for what's missing." */
    public void armForceBuild() {
        forceArmedAnchor = anchor;
        forceArmedTicksLeft = FORCE_WINDOW_TICKS;
    }

    private boolean isForceArmed() {
        return forceArmedAnchor != null && forceArmedTicksLeft > 0 && forceArmedAnchor.equals(anchor);
    }

    public boolean isActive() {
        return active;
    }

    public InteractionHand getHand() {
        return hand;
    }

    public @Nullable BlockPos getAnchor() {
        return anchor;
    }

    /** Called every render frame. Validates the held item still matches, and periodically re-resolves
     *  the schema so the preview reflects live Configurator/material changes. */
    public void tick() {
        if (forceArmedAnchor != null) {
            if (forceArmedTicksLeft > 0) {
                forceArmedTicksLeft--;
            } else {
                forceArmedAnchor = null;
            }
        }

        if (!active) return;
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            cancel();
            return;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof CinderCoreItem) || CinderSchemaData.getTargetId(stack) == null) {
            cancel();
            return;
        }

        if (resolved == null || refreshCooldown <= 0) {
            // Resolution runs inside a RenderLevelStageEvent handler - Forge's event bus logs and
            // swallows an uncaught exception from a listener rather than crashing, which would mean a
            // resolution failure here silently kills the preview forever with no visible sign anything
            // went wrong. Catching and logging explicitly turns that into an actual diagnosable error.
            try {
                resolved = CinderSchemaData.resolvePlacement(stack, anchor, facing, UP_FACING, false);
                materialsSufficient = resolved != null && CinderSchemaData.hasSufficientMaterials(stack,
                        resolved.blockCounts());
            } catch (Exception e) {
                net.phoenix.core.PhoenixCore.LOGGER.error(
                        "[CinderPreview] Failed to resolve build preview at {} (target {})", anchor,
                        CinderSchemaData.getTargetId(stack), e);
                resolved = null;
                materialsSufficient = false;
            }
            refreshCooldown = REFRESH_INTERVAL_TICKS;
        } else {
            refreshCooldown--;
        }
    }

    public @Nullable Map<BlockPos, BlockInfo> getPlacements() {
        return resolved != null ? resolved.placements() : null;
    }

    public boolean isValid() {
        return resolved != null && materialsSufficient;
    }
}
