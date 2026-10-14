package net.phoenix.core.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.vocal_resonance.puzzle.AcousticChart;
import net.phoenix.core.integration.vocal_resonance.puzzle.client.AttuneClient;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Opens the attunement game on the player's screen with the chart to play. */
public class S2CAttuneStartPacket {

    private final BlockPos pos;
    private final int bpm;
    private final List<Integer> laneNotes;
    private final List<AcousticChart.Beat> beats;

    public S2CAttuneStartPacket(BlockPos pos, AcousticChart chart) {
        this.pos = pos;
        this.bpm = chart.bpm();
        this.laneNotes = chart.laneNotes();
        this.beats = chart.beats();
    }

    public S2CAttuneStartPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
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
        buf.writeVarInt(bpm);
        buf.writeVarInt(laneNotes.size());
        for (int note : laneNotes) buf.writeVarInt(note);
        buf.writeVarInt(beats.size());
        for (AcousticChart.Beat beat : beats) {
            buf.writeVarInt(beat.tick());
            buf.writeByte(beat.lane());
        }
    }

    public static void handle(S2CAttuneStartPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (!FMLEnvironment.dist.isClient()) return;
            AttuneClient.open(msg.pos, msg.bpm, msg.laneNotes, msg.beats);
        });
        ctx.get().setPacketHandled(true);
    }
}
