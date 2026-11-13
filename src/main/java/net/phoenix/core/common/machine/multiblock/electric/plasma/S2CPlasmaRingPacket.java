package net.phoenix.core.common.machine.multiblock.electric.plasma;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.common.machine.multiblock.electric.HighPressurePlasmaArcFurnaceMachine;
import net.phoenix.core.rebirth.RebirthNetwork;

public record S2CPlasmaRingPacket(BlockPos pos, boolean open, int state, int par, int moves, int solves,
                                  int shieldKey, int shieldHealth, int cooldownTicks, boolean linked, String message,
                                  boolean solved)
        implements CustomPacketPayload {

    public static final Type<S2CPlasmaRingPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2c_plasma_ring"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CPlasmaRingPacket> STREAM_CODEC = StreamCodec
            .of((buf, msg) -> encode(msg, buf), S2CPlasmaRingPacket::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static S2CPlasmaRingPacket of(HighPressurePlasmaArcFurnaceMachine m, boolean open, String message,
                                         boolean solved) {
        return new S2CPlasmaRingPacket(m.getBlockPos(), open, m.getRingState(), m.getRingPar(), m.getRingMoves(),
                m.getRingSolves(), m.getShieldType().key, m.getShieldHealth(), m.getShieldCooldownTimer(),
                RebirthNetwork.allows(m), message, solved);
    }

    public static void send(ServerPlayer player, S2CPlasmaRingPacket packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    private static void encode(S2CPlasmaRingPacket m, FriendlyByteBuf buf) {
        buf.writeBlockPos(m.pos);
        buf.writeBoolean(m.open);
        buf.writeVarInt(m.state);
        buf.writeVarInt(m.par);
        buf.writeVarInt(m.moves);
        buf.writeVarInt(m.solves);
        buf.writeVarInt(m.shieldKey);
        buf.writeVarInt(m.shieldHealth);
        buf.writeVarInt(m.cooldownTicks);
        buf.writeBoolean(m.linked);
        buf.writeUtf(m.message, 256);
        buf.writeBoolean(m.solved);
    }

    private static S2CPlasmaRingPacket decode(FriendlyByteBuf buf) {
        return new S2CPlasmaRingPacket(buf.readBlockPos(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readBoolean(), buf.readUtf(256), buf.readBoolean());
    }

    public static void handle(S2CPlasmaRingPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> net.phoenix.core.client.gui.plasma.PlasmaRingClientHandler.accept(msg));
    }
}
