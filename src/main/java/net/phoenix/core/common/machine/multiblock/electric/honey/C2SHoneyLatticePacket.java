package net.phoenix.core.common.machine.multiblock.electric.honey;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.common.machine.multiblock.electric.HoneyCrystallizationChamberMachine;

public record C2SHoneyLatticePacket(BlockPos pos, int action, int cell) implements CustomPacketPayload {

    public static final int PRESS = 0;
    public static final int CLEAR = 1;
    public static final int NEW_PATTERN = 2;

    public static final Type<C2SHoneyLatticePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2s_honey_lattice"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SHoneyLatticePacket> STREAM_CODEC = StreamCodec.of(
            (buf, msg) -> {
                buf.writeBlockPos(msg.pos);
                buf.writeVarInt(msg.action);
                buf.writeVarInt(msg.cell);
            }, buf -> new C2SHoneyLatticePacket(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SHoneyLatticePacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5, msg.pos.getZ() + 0.5) > 64 * 64) {
                return;
            }
            HoneyCrystallizationChamberMachine machine = HoneyCrystallizationChamberMachine.at(player.level(),
                    msg.pos);
            if (machine == null || !machine.isFormed()) return;

            String message = "";
            boolean solved = false;
            switch (msg.action) {
                case PRESS -> {
                    var result = machine.latticePress(msg.cell);
                    message = result.message();
                    solved = result.solved();
                }
                case CLEAR -> machine.latticeClear();
                case NEW_PATTERN -> {
                    machine.latticeNewPattern();
                    message = "A new pattern forms.";
                }
                default -> {}
            }
            S2CHoneyLatticePacket.send(player, S2CHoneyLatticePacket.of(machine, false, message, solved));
        });
    }
}
