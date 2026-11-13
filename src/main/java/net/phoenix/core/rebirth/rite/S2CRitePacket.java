package net.phoenix.core.rebirth.rite;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.rebirth.Capstone;

public record S2CRitePacket(int capstone, BlockPos pos, int status, int[] data, int[] extra, String message)
        implements CustomPacketPayload {

    public static final int ACTIVE = 0;
    public static final int BOUND = 1;
    public static final int FAILED = 2;
    public static final int OPEN = 3;

    public static final Type<S2CRitePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2c_rite"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CRitePacket> STREAM_CODEC = StreamCodec.of(
            (buf, m) -> {
                buf.writeVarInt(m.capstone);
                buf.writeBlockPos(m.pos);
                buf.writeVarInt(m.status);
                buf.writeVarIntArray(m.data);
                buf.writeVarIntArray(m.extra);
                buf.writeUtf(m.message, 256);
            }, buf -> new S2CRitePacket(buf.readVarInt(), buf.readBlockPos(), buf.readVarInt(), buf.readVarIntArray(64),
                    buf.readVarIntArray(64), buf.readUtf(256)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static S2CRitePacket open(Capstone capstone, BlockPos pos, Rite rite, String message) {
        return new S2CRitePacket(capstone.ordinal(), pos, OPEN, rite.data(), new int[0], message);
    }

    public static S2CRitePacket of(Capstone capstone, BlockPos pos, int status, int[] data, int[] extra,
                                   String message) {
        return new S2CRitePacket(capstone.ordinal(), pos, status, data, extra, message);
    }

    public static void send(ServerPlayer player, S2CRitePacket packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void handle(S2CRitePacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> net.phoenix.core.client.gui.forge.rite.RiteClientHandler.accept(msg));
    }
}
