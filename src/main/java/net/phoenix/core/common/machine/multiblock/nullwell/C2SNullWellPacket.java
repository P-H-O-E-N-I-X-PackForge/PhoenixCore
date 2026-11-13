package net.phoenix.core.common.machine.multiblock.nullwell;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2SNullWellPacket(BlockPos pos, int which, int value) implements CustomPacketPayload {

    public static final Type<C2SNullWellPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2s_null_well"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SNullWellPacket> STREAM_CODEC = StreamCodec.of(
            (buf, m) -> {
                buf.writeBlockPos(m.pos);
                buf.writeVarInt(m.which);
                buf.writeVarInt(m.value);
            }, buf -> new C2SNullWellPacket(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SNullWellPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5, msg.pos.getZ() + 0.5) > 64 * 64) {
                return;
            }
            if (MetaMachine.getMachine(player.serverLevel(), msg.pos) instanceof NullWellMachine machine &&
                    machine.isFormed()) {
                machine.control(msg.which, msg.value);
            }
        });
    }
}
