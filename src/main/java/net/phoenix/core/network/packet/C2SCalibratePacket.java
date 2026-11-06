package net.phoenix.core.network.packet;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.astral.puzzle.AstralCalibratedMachine;
import net.phoenix.core.integration.astral.puzzle.StarChart;

import java.util.ArrayList;
import java.util.List;

public class C2SCalibratePacket implements CustomPacketPayload {

    public static final Type<C2SCalibratePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_calibrate_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SCalibratePacket> STREAM_CODEC = StreamCodec
            .ofMember(C2SCalibratePacket::encode, C2SCalibratePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final BlockPos pos;
    private final List<int[]> lines;

    public C2SCalibratePacket(BlockPos pos, List<int[]> lines) {
        this.pos = pos;
        this.lines = lines;
    }

    public C2SCalibratePacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        int count = Math.min(buf.readVarInt(), StarChart.MAX_LINES);
        this.lines = new ArrayList<>(count);
        for (int i = 0; i < count; i++) lines.add(new int[] { buf.readByte(), buf.readByte() });
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(lines.size());
        for (int[] l : lines) {
            buf.writeByte(l[0]);
            buf.writeByte(l[1]);
        }
    }

    public static void handle(C2SCalibratePacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;
            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5, msg.pos.getZ() + 0.5) > 64) return;
            if (MetaMachine.getMachine(player.level(), msg.pos) instanceof AstralCalibratedMachine machine) {
                machine.attempt(player, msg.lines);
            }
        });
    }
}
