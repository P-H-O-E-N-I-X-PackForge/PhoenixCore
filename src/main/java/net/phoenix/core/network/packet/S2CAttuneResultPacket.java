package net.phoenix.core.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.vocal_resonance.puzzle.client.AttuneClient;
import net.phoenix.core.network.PacketCodecs;

public record S2CAttuneResultPacket(BlockPos pos, boolean passed, int grade, int perfect, int good, int total)
        implements CustomPacketPayload {

    public static final Type<S2CAttuneResultPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2_c_attune_result_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CAttuneResultPacket> STREAM_CODEC = StreamCodec
            .composite(
                    BlockPos.STREAM_CODEC, S2CAttuneResultPacket::pos,
                    ByteBufCodecs.BOOL, S2CAttuneResultPacket::passed,
                    PacketCodecs.BYTE_INT, S2CAttuneResultPacket::grade,
                    ByteBufCodecs.VAR_INT, S2CAttuneResultPacket::perfect,
                    ByteBufCodecs.VAR_INT, S2CAttuneResultPacket::good,
                    ByteBufCodecs.VAR_INT, S2CAttuneResultPacket::total,
                    S2CAttuneResultPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CAttuneResultPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!FMLEnvironment.dist.isClient()) return;
            AttuneClient.result(msg.pos(), msg.passed(), msg.grade(), msg.perfect(), msg.good(), msg.total());
        });
    }
}
