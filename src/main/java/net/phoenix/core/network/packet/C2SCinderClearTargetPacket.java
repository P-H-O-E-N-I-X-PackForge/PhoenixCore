package net.phoenix.core.network.packet;


import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import net.phoenix.core.common.item.cinder.CinderCoreItem;
import net.phoenix.core.common.item.cinder.CinderSchemaData;

import java.util.function.Supplier;

public class C2SCinderClearTargetPacket implements CustomPacketPayload {
    public static final Type<C2SCinderClearTargetPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_cinder_clear_target_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SCinderClearTargetPacket> STREAM_CODEC = StreamCodec.ofMember(C2SCinderClearTargetPacket::encode, C2SCinderClearTargetPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    private final InteractionHand hand;

    public C2SCinderClearTargetPacket(InteractionHand hand) {
        this.hand = hand;
    }

    public C2SCinderClearTargetPacket(FriendlyByteBuf buf) {
        this.hand = buf.readEnum(InteractionHand.class);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(hand);
    }

    public static void handle(C2SCinderClearTargetPacket msg, IPayloadContext ctxGetter) {
        IPayloadContext ctx = ctxGetter;
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;
            ItemStack stack = player.getItemInHand(msg.hand);
            if (!(stack.getItem() instanceof CinderCoreItem)) return;
            CinderSchemaData.clearTarget(stack);
        });
        ctx.setPacketHandled(true);
    }
}
