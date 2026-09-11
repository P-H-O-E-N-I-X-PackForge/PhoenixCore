package net.phoenix.core.network.packet;

import com.gregtechceu.gtceu.api.multiblock.util.BlockInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;

import net.phoenix.core.common.block.cinder.CinderBlocks;
import net.phoenix.core.common.block.cinder.CinderConstructionBlockEntity;
import net.phoenix.core.common.block.cinder.CinderVisualEffects;
import net.phoenix.core.common.item.cinder.CinderCoreItem;
import net.phoenix.core.common.item.cinder.CinderSchemaData;
import net.phoenix.core.network.PhoenixNetwork;

import java.util.Map;
import java.util.function.Supplier;

/**
 * The client's "build it here" commit - re-resolves everything server-side rather than trusting the
 * client's cached preview (see {@link net.phoenix.core.client.cinder.CinderPreviewState} for why that
 * cache exists and why it's display-only). On success, consumes only the materials actually used from
 * the Core's stocked inventory and hands the resulting placements off to a
 * {@link CinderConstructionBlockEntity} to build. The Core itself is never destroyed - it returns to
 * an empty-but-still-configured state (target/config NBT untouched, materials drained) so it can be
 * restocked and built again without recrafting. If materials are insufficient, a normal attempt is
 * rejected outright; a follow-up commit sent with {@code force=true} (see
 * {@link net.phoenix.core.client.cinder.CinderPreviewState#armForceBuild}) instead builds only the
 * positions currently-stocked materials actually cover, leaving gaps for whatever's missing.
 */
public class C2SCinderCommitPacket {

    private final InteractionHand hand;
    private final BlockPos anchor;
    private final Direction facing;
    private final Direction upFacing;
    private final boolean flipped;
    private final boolean force;

    public C2SCinderCommitPacket(InteractionHand hand, BlockPos anchor, Direction facing, Direction upFacing,
                                 boolean flipped, boolean force) {
        this.hand = hand;
        this.anchor = anchor;
        this.facing = facing;
        this.upFacing = upFacing;
        this.flipped = flipped;
        this.force = force;
    }

    public C2SCinderCommitPacket(FriendlyByteBuf buf) {
        this.hand = buf.readEnum(InteractionHand.class);
        this.anchor = buf.readBlockPos();
        this.facing = buf.readEnum(Direction.class);
        this.upFacing = buf.readEnum(Direction.class);
        this.flipped = buf.readBoolean();
        this.force = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(hand);
        buf.writeBlockPos(anchor);
        buf.writeEnum(facing);
        buf.writeEnum(upFacing);
        buf.writeBoolean(flipped);
        buf.writeBoolean(force);
    }

    public static void handle(C2SCinderCommitPacket msg, Supplier<NetworkEvent.Context> ctxGetter) {
        NetworkEvent.Context ctx = ctxGetter.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ItemStack stack = player.getItemInHand(msg.hand);
            if (!(stack.getItem() instanceof CinderCoreItem)) return;
            if (!(player.level() instanceof ServerLevel level)) return;

            if (!level.isLoaded(msg.anchor) || player.distanceToSqr(msg.anchor.getX() + 0.5, msg.anchor.getY() + 0.5,
                    msg.anchor.getZ() + 0.5) > 64 * 64) {
                reject(player, "Too far away to build there.", false);
                return;
            }

            CinderSchemaData.ResolvedPlacement resolved = CinderSchemaData.resolvePlacement(stack, msg.anchor,
                    msg.facing, msg.upFacing, msg.flipped);
            if (resolved == null || resolved.placements().isEmpty()) {
                reject(player, "This Core has nothing configured to build.", false);
                return;
            }

            boolean sufficient = CinderSchemaData.hasSufficientMaterials(stack, resolved.blockCounts());

            Map<BlockPos, BlockInfo> placements;
            Reference2IntMap<Block> consumed;
            if (sufficient) {
                placements = resolved.placements();
                consumed = resolved.blockCounts();
            } else if (msg.force) {
                CinderSchemaData.PartialPlacement partial = CinderSchemaData.resolvePartialPlacements(stack,
                        resolved);
                if (partial.placements().isEmpty()) {
                    reject(player, "No stocked materials available to build any of this.", false);
                    return;
                }
                placements = partial.placements();
                consumed = partial.consumed();
            } else {
                reject(player, "Not enough stocked materials to build this. Right-click again to build with " +
                        "what you have.", true);
                return;
            }

            CinderSchemaData.consumeMaterials(stack, consumed);
            // The Core is never destroyed - its target/config NBT is untouched, so it returns to an
            // empty-but-still-configured state and can be restocked and built again without recrafting.

            BlockInfo anchorInfo = placements.get(msg.anchor);
            if (anchorInfo == null) {
                // Either the controller's own resolved position should always be part of the structure
                // (falling back here keeps this from silently no-oping if that assumption is ever
                // wrong for some pattern shape), or - for a partial force-build - the controller's own
                // block type simply wasn't stocked; either way an empty temp entry is the safe fallback.
                anchorInfo = BlockInfo.EMPTY;
            }

            CinderVisualEffects.playCommitShatter(level, msg.anchor);

            level.setBlockAndUpdate(msg.anchor, CinderBlocks.CINDER_CONSTRUCTION.get().defaultBlockState());
            if (level.getBlockEntity(msg.anchor) instanceof CinderConstructionBlockEntity be) {
                be.beginConstruction(placements, anchorInfo);
            }
        });
        ctx.setPacketHandled(true);
    }

    private static void reject(ServerPlayer player, String reason, boolean insufficientMaterials) {
        PhoenixNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new S2CCinderCommitRejectedPacket(reason, insufficientMaterials));
    }
}
