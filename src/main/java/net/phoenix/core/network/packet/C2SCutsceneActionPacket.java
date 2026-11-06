package net.phoenix.core.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.common.block.cinema.CutsceneActions;

public class C2SCutsceneActionPacket implements CustomPacketPayload {

    public static final Type<C2SCutsceneActionPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_cutscene_action_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SCutsceneActionPacket> STREAM_CODEC = StreamCodec
            .ofMember(C2SCutsceneActionPacket::encode, C2SCutsceneActionPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final ResourceLocation cutscene;
    private final ResourceLocation action;

    public C2SCutsceneActionPacket(ResourceLocation cutscene, ResourceLocation action) {
        this.cutscene = cutscene;
        this.action = action;
    }

    public C2SCutsceneActionPacket(FriendlyByteBuf buf) {
        this.cutscene = buf.readResourceLocation();
        this.action = buf.readResourceLocation();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(cutscene);
        buf.writeResourceLocation(action);
    }

    public static void handle(C2SCutsceneActionPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player != null) CutsceneActions.trigger(player, msg.cutscene, msg.action);
        });
    }
}
