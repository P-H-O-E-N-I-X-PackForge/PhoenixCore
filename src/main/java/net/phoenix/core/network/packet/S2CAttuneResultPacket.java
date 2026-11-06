package net.phoenix.core.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.vocal_resonance.puzzle.client.AttuneClient;

public class S2CAttuneResultPacket implements CustomPacketPayload {

    public static final Type<S2CAttuneResultPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2_c_attune_result_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CAttuneResultPacket> STREAM_CODEC = StreamCodec
            .ofMember(S2CAttuneResultPacket::encode, S2CAttuneResultPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

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

    public static void handle(S2CAttuneResultPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!FMLEnvironment.dist.isClient()) return;
            AttuneClient.result(msg.pos, msg.passed, msg.grade, msg.perfect, msg.good, msg.total);
        });
    }
}
