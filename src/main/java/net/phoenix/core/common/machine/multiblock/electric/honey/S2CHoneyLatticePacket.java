package net.phoenix.core.common.machine.multiblock.electric.honey;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.common.machine.multiblock.electric.HoneyCrystallizationChamberMachine;

public record S2CHoneyLatticePacket(BlockPos pos, boolean open, int state, int target, int moves, int par,
                                    int charges, int solves, String message, boolean solved)
        implements CustomPacketPayload {

    public static final Type<S2CHoneyLatticePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2c_honey_lattice"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CHoneyLatticePacket> STREAM_CODEC = StreamCodec
            .of((buf, msg) -> encode(msg, buf), S2CHoneyLatticePacket::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static S2CHoneyLatticePacket of(HoneyCrystallizationChamberMachine machine, boolean open, String message,
                                           boolean solved) {
        return new S2CHoneyLatticePacket(machine.getBlockPos(), open, machine.getLatticeState(),
                machine.getLatticeTarget(), machine.getLatticeMoves(), machine.getLatticePar(),
                machine.getSeedCharges(), machine.getLatticeSolves(), message, solved);
    }

    public static void send(ServerPlayer player, S2CHoneyLatticePacket packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    private static void encode(S2CHoneyLatticePacket m, FriendlyByteBuf buf) {
        buf.writeBlockPos(m.pos);
        buf.writeBoolean(m.open);
        buf.writeVarInt(m.state);
        buf.writeVarInt(m.target);
        buf.writeVarInt(m.moves);
        buf.writeVarInt(m.par);
        buf.writeVarInt(m.charges);
        buf.writeVarInt(m.solves);
        buf.writeUtf(m.message, 256);
        buf.writeBoolean(m.solved);
    }

    private static S2CHoneyLatticePacket decode(FriendlyByteBuf buf) {
        return new S2CHoneyLatticePacket(buf.readBlockPos(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readUtf(256),
                buf.readBoolean());
    }

    public static void handle(S2CHoneyLatticePacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> net.phoenix.core.client.gui.honey.HoneyLatticeClientHandler.accept(msg));
    }
}
