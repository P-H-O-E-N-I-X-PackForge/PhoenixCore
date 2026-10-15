package net.phoenix.core.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.vocal_resonance.puzzle.AcousticChart;
import net.phoenix.core.integration.vocal_resonance.puzzle.client.AttuneClient;

import java.util.ArrayList;
import java.util.List;

public class S2CAttuneStartPacket implements CustomPacketPayload {

    public static final Type<S2CAttuneStartPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2_c_attune_start_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CAttuneStartPacket> STREAM_CODEC = StreamCodec
            .ofMember(S2CAttuneStartPacket::encode, S2CAttuneStartPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final BlockPos pos;
    private final int difficulty;
    private final int bpm;
    private final List<Integer> laneNotes;
    private final List<AcousticChart.Beat> beats;

    public S2CAttuneStartPacket(BlockPos pos, int difficulty, AcousticChart chart) {
        this.pos = pos;
        this.difficulty = difficulty;
        this.bpm = chart.bpm();
        this.laneNotes = chart.laneNotes();
        this.beats = chart.beats();
    }

    public S2CAttuneStartPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.difficulty = buf.readByte();
        this.bpm = buf.readVarInt();
        int lanes = buf.readVarInt();
        this.laneNotes = new ArrayList<>(lanes);
        for (int i = 0; i < lanes; i++) laneNotes.add(buf.readVarInt());
        int count = buf.readVarInt();
        this.beats = new ArrayList<>(count);
        for (int i = 0; i < count; i++) beats.add(new AcousticChart.Beat(buf.readVarInt(), buf.readByte()));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeByte(difficulty);
        buf.writeVarInt(bpm);
        buf.writeVarInt(laneNotes.size());
        for (int note : laneNotes) buf.writeVarInt(note);
        buf.writeVarInt(beats.size());
        for (AcousticChart.Beat beat : beats) {
            buf.writeVarInt(beat.tick());
            buf.writeByte(beat.lane());
        }
    }

    public static void handle(S2CAttuneStartPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!FMLEnvironment.dist.isClient()) return;
            AttuneClient.open(msg.pos, msg.difficulty, msg.bpm, msg.laneNotes, msg.beats);
        });
    }
}
