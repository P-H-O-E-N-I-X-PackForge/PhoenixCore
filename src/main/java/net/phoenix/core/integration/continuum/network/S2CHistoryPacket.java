package net.phoenix.core.integration.continuum.network;


import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.fml.loading.FMLEnvironment;

import net.phoenix.core.integration.continuum.client.ContinuumClientState;
import net.phoenix.core.integration.continuum.common.MissionLog;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class S2CHistoryPacket implements CustomPacketPayload {
    public static final Type<S2CHistoryPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2_c_history_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CHistoryPacket> STREAM_CODEC = StreamCodec.ofMember(S2CHistoryPacket::encode, S2CHistoryPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


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

    public static void handle(S2CHistoryPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) ContinuumClientState.acceptHistory(msg.entries);
        });
        }
}
