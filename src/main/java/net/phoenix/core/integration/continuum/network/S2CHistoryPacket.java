package net.phoenix.core.integration.continuum.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.continuum.client.ContinuumClientState;
import net.phoenix.core.integration.continuum.common.MissionLog;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** The team's mission history, oldest first. */
public class S2CHistoryPacket {

    private final List<MissionLog.Entry> entries;

    public S2CHistoryPacket(List<MissionLog.Entry> entries) {
        this.entries = List.copyOf(entries);
    }

    public S2CHistoryPacket(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<MissionLog.Entry> list = new ArrayList<>();
        for (int i = 0; i < count; i++) list.add(MissionLog.Entry.read(buf));
        this.entries = list;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entries.size());
        for (MissionLog.Entry entry : entries) entry.write(buf);
    }

    public static void handle(S2CHistoryPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) ContinuumClientState.acceptHistory(msg.entries);
        });
        ctx.get().setPacketHandled(true);
    }
}
