package net.phoenix.core.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record S2CCinderCommitRejectedPacket(String reason, boolean insufficientMaterials)
        implements CustomPacketPayload {

    public static final Type<S2CCinderCommitRejectedPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2_c_cinder_commit_rejected_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CCinderCommitRejectedPacket> STREAM_CODEC = StreamCodec
            .composite(
                    ByteBufCodecs.stringUtf8(256), S2CCinderCommitRejectedPacket::reason,
                    ByteBufCodecs.BOOL, S2CCinderCommitRejectedPacket::insufficientMaterials,
                    S2CCinderCommitRejectedPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CCinderCommitRejectedPacket msg, IPayloadContext ctxGetter) {
        IPayloadContext ctx = ctxGetter;
        ctx.enqueueWork(() -> {
            if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
                var state = net.phoenix.core.client.cinder.CinderPreviewState.INSTANCE;
                if (msg.insufficientMaterials()) {
                    state.armForceBuild();
                }
                state.cancel();
                var player = Minecraft.getInstance().player;
                if (player != null) {
                    player.displayClientMessage(Component.literal(msg.reason()), true);
                }
            }
        });
    }
}
