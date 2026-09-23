package net.phoenix.core.network.packet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.common.item.cinder.CinderAtlasData;
import net.phoenix.core.common.item.cinder.CinderAtlasItem;
import net.phoenix.core.common.item.cinder.CinderSchemaData;
import net.phoenix.core.integration.ae2.CinderAtlasWirelessLink;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;

import java.util.function.Supplier;

/**
 * Manual "prepare ahead" trigger (design doc feature #4) - real, headless auto-request of whatever the
 * active loadout's active slot is still missing from its linked AE2 network, independent of any deploy
 * attempt. Position-independent (uses {@link CinderSchemaData#getRequiredBlocks}, not a resolved
 * in-world placement), since the player isn't necessarily aiming at a valid build site when preparing
 * ahead.
 */
public class C2SCinderAtlasRequestMaterialsPacket {

    private final InteractionHand hand;

    public C2SCinderAtlasRequestMaterialsPacket(InteractionHand hand) {
        this.hand = hand;
    }

    public C2SCinderAtlasRequestMaterialsPacket(FriendlyByteBuf buf) {
        this.hand = buf.readEnum(InteractionHand.class);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(hand);
    }

    public static void handle(C2SCinderAtlasRequestMaterialsPacket msg, Supplier<NetworkEvent.Context> ctxGetter) {
        NetworkEvent.Context ctx = ctxGetter.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ItemStack stack = player.getItemInHand(msg.hand);
            if (!(stack.getItem() instanceof CinderAtlasItem)) return;
            if (!(player.level() instanceof ServerLevel level)) return;

            int loadout = CinderAtlasData.getActiveLoadout(stack);
            int slot = CinderAtlasData.getActiveSlot(stack);
            CompoundTag tag = CinderAtlasData.peekSlotTag(stack, loadout, slot);
            if (tag == null || CinderSchemaData.getTargetId(tag) == null) {
                player.displayClientMessage(Component.literal("This slot isn't configured."), true);
                return;
            }

            Reference2IntMap<net.minecraft.world.level.block.Block> required = CinderSchemaData.getRequiredBlocks(tag);
            if (required.isEmpty()) return;

            var accessPoint = CinderAtlasWirelessLink.getLinkedAccessPoint(stack, level);
            if (accessPoint == null) {
                player.displayClientMessage(
                        Component.literal("This Atlas isn't linked to an AE2 network - bind it with a " +
                                "Security Terminal first."),
                        true);
                return;
            }
            if (!CinderAtlasWirelessLink.isInRange(stack, accessPoint, player)) {
                player.displayClientMessage(
                        Component.literal("Too far from the linked access point - move closer or install a " +
                                "Range Extender upgrade."),
                        true);
                return;
            }

            CinderAtlasWirelessLink.requestMissingMaterials(player, stack, level, required);
        });
        ctx.setPacketHandled(true);
    }
}
