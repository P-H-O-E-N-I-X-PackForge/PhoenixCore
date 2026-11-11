package net.phoenix.core.integration.conflux.panoptic;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record S2CPanopticSolutionPacket(BlockPos controller, int[] values, List<String> lines)
        implements CustomPacketPayload {

    public static final Type<S2CPanopticSolutionPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2c_panoptic_solution"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CPanopticSolutionPacket> STREAM_CODEC = StreamCodec
            .of((buf, msg) -> encode(msg, buf), S2CPanopticSolutionPacket::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(S2CPanopticSolutionPacket m, FriendlyByteBuf buf) {
        buf.writeBlockPos(m.controller);
        buf.writeVarIntArray(m.values);
        buf.writeVarInt(m.lines.size());
        for (String s : m.lines) buf.writeUtf(s, 512);
    }

    private static S2CPanopticSolutionPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int[] values = buf.readVarIntArray(64);
        int n = buf.readVarInt();
        List<String> lines = new ArrayList<>(n);
        for (int i = 0; i < n; i++) lines.add(buf.readUtf(512));
        return new S2CPanopticSolutionPacket(pos, values, lines);
    }

    public static void handle(S2CPanopticSolutionPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> net.phoenix.core.integration.conflux.client.PanopticClientHandler.solution(msg));
    }
}
