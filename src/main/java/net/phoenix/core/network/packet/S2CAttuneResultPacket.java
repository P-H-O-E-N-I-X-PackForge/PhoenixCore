package net.phoenix.core.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.vocal_resonance.puzzle.client.AttuneClient;

import java.util.function.Supplier;

public class S2CAttuneResultPacket {

    private final BlockPos pos;
    private final boolean passed;
    private final int grade;
    private final int perfect;
    private final int good;
    private final int total;

    public S2CAttuneResultPacket(BlockPos pos, boolean passed, int grade, int perfect, int good, int total) {
        this.pos = pos;
        this.passed = passed;
        this.grade = grade;
        this.perfect = perfect;
        this.good = good;
        this.total = total;
    }

    public S2CAttuneResultPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.passed = buf.readBoolean();
        this.grade = buf.readByte();
        this.perfect = buf.readVarInt();
        this.good = buf.readVarInt();
        this.total = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(passed);
        buf.writeByte(grade);
        buf.writeVarInt(perfect);
        buf.writeVarInt(good);
        buf.writeVarInt(total);
    }

    public static void handle(S2CAttuneResultPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (!FMLEnvironment.dist.isClient()) return;
            AttuneClient.result(msg.pos, msg.passed, msg.grade, msg.perfect, msg.good, msg.total);
        });
        ctx.get().setPacketHandled(true);
    }
}
