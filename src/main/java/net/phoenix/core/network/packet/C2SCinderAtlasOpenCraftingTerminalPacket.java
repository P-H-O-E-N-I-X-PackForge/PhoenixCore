package net.phoenix.core.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.common.item.cinder.CinderAtlasItem;
import net.phoenix.core.integration.ae2.CinderAtlasWirelessLink;

public class C2SCinderAtlasOpenCraftingTerminalPacket implements CustomPacketPayload {

    public static final Type<C2SCinderAtlasOpenCraftingTerminalPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_cinder_atlas_open_crafting_terminal_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SCinderAtlasOpenCraftingTerminalPacket> STREAM_CODEC = StreamCodec
            .ofMember(C2SCinderAtlasOpenCraftingTerminalPacket::encode, C2SCinderAtlasOpenCraftingTerminalPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final InteractionHand hand;

    public C2SCinderAtlasOpenCraftingTerminalPacket(InteractionHand hand) {
        this.hand = hand;
    }

    public C2SCinderAtlasOpenCraftingTerminalPacket(FriendlyByteBuf buf) {
        this.hand = buf.readEnum(InteractionHand.class);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(hand);
    }

    public static void handle(C2SCinderAtlasOpenCraftingTerminalPacket msg, IPayloadContext ctxGetter) {
        IPayloadContext ctx = ctxGetter;
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;
            ItemStack stack = player.getItemInHand(msg.hand);
            if (!(stack.getItem() instanceof CinderAtlasItem)) return;
            if (!(player.level() instanceof ServerLevel level)) return;

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

            CinderAtlasItem.openCraftingTerminal(player, msg.hand);
        });
    }
}
