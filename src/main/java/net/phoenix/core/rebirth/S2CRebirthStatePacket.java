package net.phoenix.core.rebirth;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.conflux.research.ResearchTeamHelper;

public record S2CRebirthStatePacket(BlockPos forge, boolean open, int boundMask, int[] ring, int aligned, int attempts,
                                    boolean closed, boolean reborn, boolean ignited, String message)
        implements CustomPacketPayload {

    public static final Type<S2CRebirthStatePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2c_rebirth_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CRebirthStatePacket> STREAM_CODEC = StreamCodec
            .of((buf, msg) -> encode(msg, buf), S2CRebirthStatePacket::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static S2CRebirthStatePacket of(ServerPlayer player, BlockPos forge, boolean open, boolean ignited,
                                           String message) {
        RebirthData.Team t = RebirthData.get(player.serverLevel()).team(ResearchTeamHelper.getTeamId(player));
        int mask = 0;
        for (int i = 0; i < Capstone.COUNT; i++) if (t.bound[i]) mask |= 1 << i;
        return new S2CRebirthStatePacket(forge, open, mask, t.ring, t.lastAligned, t.attempts, t.closed, t.reborn,
                ignited, message);
    }

    public static void send(ServerPlayer player, S2CRebirthStatePacket packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public boolean isBound(int capstone) {
        return (boundMask >> capstone & 1) != 0;
    }

    private static void encode(S2CRebirthStatePacket m, FriendlyByteBuf buf) {
        buf.writeBlockPos(m.forge);
        buf.writeBoolean(m.open);
        buf.writeVarInt(m.boundMask);
        buf.writeVarIntArray(m.ring);
        buf.writeVarInt(m.aligned);
        buf.writeVarInt(m.attempts);
        buf.writeBoolean(m.closed);
        buf.writeBoolean(m.reborn);
        buf.writeBoolean(m.ignited);
        buf.writeUtf(m.message, 256);
    }

    private static S2CRebirthStatePacket decode(FriendlyByteBuf buf) {
        return new S2CRebirthStatePacket(buf.readBlockPos(), buf.readBoolean(), buf.readVarInt(),
                buf.readVarIntArray(64), buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean(),
                buf.readBoolean(), buf.readUtf(256));
    }

    public static void handle(S2CRebirthStatePacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> net.phoenix.core.client.gui.forge.PhoenixForgeClientHandler.accept(msg));
    }
}
