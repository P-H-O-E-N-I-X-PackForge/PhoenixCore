package net.phoenix.core.integration.continuum.network;


import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import net.phoenix.core.integration.continuum.common.RocketRefit;

import java.util.function.Supplier;

public class C2SRefitPacket implements CustomPacketPayload {
    public static final Type<C2SRefitPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_refit_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRefitPacket> STREAM_CODEC = StreamCodec.ofMember(C2SRefitPacket::encode, C2SRefitPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    private final RocketRefit.Action action;
    private final String key;

    public C2SRefitPacket(RocketRefit.Action action, String key) {
        this.action = action;
        this.key = key;
    }

    public C2SRefitPacket(FriendlyByteBuf buf) {
        this.action = RocketRefit.Action.values()[Math.max(0,
                Math.min(buf.readByte(), RocketRefit.Action.values().length - 1))];
        this.key = buf.readUtf(64);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(action.ordinal());
        buf.writeUtf(key, 64);
    }

    public static void handle(C2SRefitPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;
            String error = RocketRefit.apply(player, msg.action, msg.key);
            if (error != null) player.displayClientMessage(Component.literal(error), true);
        });
        }
}
