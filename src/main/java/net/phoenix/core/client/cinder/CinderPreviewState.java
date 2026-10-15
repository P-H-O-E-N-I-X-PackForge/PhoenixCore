package net.phoenix.core.client.cinder;

import com.gregtechceu.gtceu.api.multiblock.util.BlockInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.common.item.cinder.CinderDeploySource;
import net.phoenix.core.common.item.cinder.CinderSchemaData;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.network.packet.C2SCinderCommitPacket;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class CinderPreviewState {

    public static final CinderPreviewState INSTANCE = new CinderPreviewState();

    private static final Direction UP_FACING = Direction.UP;
    private static final int REFRESH_INTERVAL_TICKS = 20;

    private static final int FORCE_WINDOW_TICKS = 60;

    private boolean active;
    private InteractionHand hand;
    private BlockPos anchor;
    private Direction facing = Direction.NORTH;

    private @Nullable CinderSchemaData.ResolvedPlacement resolved;
    private boolean materialsSufficient;
    private boolean siteClear;
    private int refreshCooldown;

    private @Nullable BlockPos forceArmedAnchor;
    private int forceArmedTicksLeft;

    private CinderPreviewState() {}

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
        CompoundTag tag = CinderDeploySource.resolveDeployTag(stack);
        if (tag == null || CinderSchemaData.getTargetId(tag) == null) {
            cancel();
            return;
        }

        if (resolved == null || refreshCooldown <= 0) {

            try {
                resolved = CinderSchemaData.resolvePlacement(tag, anchor, facing, UP_FACING, false);
                materialsSufficient = resolved != null && CinderSchemaData.hasSufficientMaterials(stack,
                        resolved.blockCounts());
                siteClear = resolved != null && CinderSchemaData.isSiteClear(player.level(), resolved.placements());
            } catch (Exception e) {
                net.phoenix.core.PhoenixCore.LOGGER.error(
                        "[CinderPreview] Failed to resolve build preview at {} (target {})", anchor,
                        CinderSchemaData.getTargetId(tag), e);
                resolved = null;
                materialsSufficient = false;
                siteClear = false;
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
        return resolved != null && materialsSufficient && siteClear;
    }
}
