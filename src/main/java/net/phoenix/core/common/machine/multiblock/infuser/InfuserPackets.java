package net.phoenix.core.common.machine.multiblock.infuser;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class InfuserPackets {

    private InfuserPackets() {}

    public record Open(BlockPos pos) implements CustomPacketPayload {

        public static final Type<Open> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2c_infuser_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = StreamCodec
                .of((buf, m) -> buf.writeBlockPos(m.pos), buf -> new Open(buf.readBlockPos()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Control(BlockPos pos, int which, int value) implements CustomPacketPayload {

        public static final Type<Control> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2s_infuser"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Control> CODEC = StreamCodec.of((buf, m) -> {
            buf.writeBlockPos(m.pos);
            buf.writeVarInt(m.which);
            buf.writeVarInt(m.value);
        }, buf -> new Control(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static void onOpen(Open msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> net.phoenix.core.client.infuser.InfuserClientHandler.open(msg.pos()));
    }

    private static void onControl(Control msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5, msg.pos.getZ() + 0.5) > 64 * 64) {
                return;
            }
            if (MetaMachine.getMachine(player.serverLevel(), msg.pos) instanceof PhoenixInfuserMachine m &&
                    m.isFormed()) {
                m.control(msg.which, msg.value);
            }
        });
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Open.TYPE, Open.CODEC, InfuserPackets::onOpen);
        registrar.playToServer(Control.TYPE, Control.CODEC, InfuserPackets::onControl);
    }
}
