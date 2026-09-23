package net.phoenix.core.network.packet;

import com.gregtechceu.gtceu.api.multiblock.util.BlockInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.phoenix.core.common.block.cinder.CinderBlocks;
import net.phoenix.core.common.block.cinder.CinderConstructionBlockEntity;
import net.phoenix.core.common.block.cinder.CinderVisualEffects;
import net.phoenix.core.common.item.cinder.CinderCoreItem;
import net.phoenix.core.integration.ae2.CinderAtlasWirelessLink;
import net.phoenix.core.common.item.cinder.CinderDeploySource;
import net.phoenix.core.common.item.cinder.CinderSchemaData;
import net.phoenix.core.network.PhoenixNetwork;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;

import java.util.Map;
import java.util.function.Supplier;

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
            CompoundTag tag = CinderDeploySource.resolveDeployTag(stack);
            if (tag == null) return;
            if (!(player.level() instanceof ServerLevel level)) return;

            if (!level.isLoaded(msg.anchor) || player.distanceToSqr(msg.anchor.getX() + 0.5, msg.anchor.getY() + 0.5,
                    msg.anchor.getZ() + 0.5) > 64 * 64) {
                reject(player, "Too far away to build there.", false);
                return;
            }

            CinderSchemaData.ResolvedPlacement resolved = CinderSchemaData.resolvePlacement(tag, msg.anchor,
                    msg.facing, msg.upFacing, msg.flipped);
            if (resolved == null || resolved.placements().isEmpty()) {
                reject(player, "Nothing configured to build.", false);
                return;
            }

            // Authoritative "not into the ground" check - resolvePlacement is purely geometric and never
            // looks at the actual world, so without this a footprint that dips into a hillside or floor
            // would silently overwrite that terrain instead of being refused. Checked before
            // materials/force so an obstructed site is never buildable even with a stocked force-build.
            if (!CinderSchemaData.isSiteClear(level, resolved.placements())) {
                reject(player, "That location is obstructed - clear the space first.", false);
                return;
            }

            boolean sufficient = CinderDeploySource.hasSufficientMaterials(stack, level, player,
                    resolved.blockCounts());

            Map<BlockPos, BlockInfo> placements;
            Reference2IntMap<Block> consumed;
            if (sufficient) {
                placements = resolved.placements();
                consumed = resolved.blockCounts();
            } else if (msg.force && stack.getItem() instanceof CinderCoreItem) {
                // Force-partial-build only exists for a Cinder Core's own physically-stocked
                // materials budget (see CinderSchemaData#resolvePartialPlacements) - an Atlas has no
                // such budget to partially draw from yet, so it always falls through to the plain
                // rejection below instead, even if msg.force was set.
                CinderSchemaData.PartialPlacement partial = CinderSchemaData.resolvePartialPlacements(stack,
                        resolved);
                if (partial.placements().isEmpty()) {
                    reject(player, "No stocked materials available to build any of this.", false);
                    return;
                }
                placements = partial.placements();
                consumed = partial.consumed();
            } else if (stack.getItem() instanceof CinderCoreItem) {
                reject(player, "Not enough stocked materials to build this. Right-click again to build with " +
                        "what you have.", true);
                return;
            } else {
                var accessPoint = CinderAtlasWirelessLink.getLinkedAccessPoint(stack, level);
                if (accessPoint == null) {
                    reject(player, "This Atlas isn't linked to an AE2 network.", true);
                } else if (!CinderAtlasWirelessLink.isInRange(stack, accessPoint, player)) {
                    reject(player, "Too far from the linked access point - move closer or install a Range " +
                            "Extender upgrade.", true);
                } else {
                    boolean requested = CinderAtlasWirelessLink.requestMissingMaterials(player, stack, level,
                            resolved.blockCounts());
                    reject(player, requested ?
                            "Not enough materials available - requesting the shortfall from the network." :
                            "Not enough materials available on the linked network.", true);
                }
                return;
            }

            CinderDeploySource.consumeMaterials(stack, level, consumed, player);

            BlockInfo anchorInfo = placements.get(msg.anchor);
            if (anchorInfo == null) {

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
