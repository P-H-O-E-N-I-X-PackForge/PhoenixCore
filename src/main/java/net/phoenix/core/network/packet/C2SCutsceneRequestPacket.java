package net.phoenix.core.network.packet;


import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import net.phoenix.core.common.block.cinema.Cutscenes;

import java.util.function.Supplier;

public class C2SCutsceneRequestPacket implements CustomPacketPayload {
    public static final Type<C2SCutsceneRequestPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_cutscene_request_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SCutsceneRequestPacket> STREAM_CODEC = StreamCodec.ofMember(C2SCutsceneRequestPacket::encode, C2SCutsceneRequestPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    private final ResourceLocation cutscene;

    public C2SCutsceneRequestPacket(ResourceLocation cutscene) {
        this.cutscene = cutscene;
    }

    public C2SCutsceneRequestPacket(FriendlyByteBuf buf) {
        this.cutscene = buf.readResourceLocation();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(cutscene);
    }

    public static void handle(C2SCutsceneRequestPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player != null) Cutscenes.play(player, msg.cutscene);
        });
        }
}
