package net.phoenix.core.integration.continuum.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.continuum.client.ContinuumClientHooks;
import net.phoenix.core.integration.continuum.data.ContinuumData;

import java.util.LinkedHashMap;
import java.util.Map;

public class S2CContinuumDataPacket implements CustomPacketPayload {

    public static final Type<S2CContinuumDataPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2_c_continuum_data_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CContinuumDataPacket> STREAM_CODEC = StreamCodec
            .ofMember(S2CContinuumDataPacket::encode, S2CContinuumDataPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static final int MAX_JSON = 32767;

    private final ContinuumData.Snapshot snapshot;

    public S2CContinuumDataPacket(ContinuumData.Snapshot snapshot) {
        this.snapshot = snapshot;
    }

    public S2CContinuumDataPacket(FriendlyByteBuf buf) {
        this.snapshot = new ContinuumData.Snapshot(readMap(buf), readMap(buf));
    }

    public void encode(FriendlyByteBuf buf) {
        writeMap(buf, snapshot.systems());
        writeMap(buf, snapshot.bodies());
    }

    private static void writeMap(FriendlyByteBuf buf, Map<ResourceLocation, String> map) {
        buf.writeVarInt(map.size());
        map.forEach((id, json) -> {
            buf.writeResourceLocation(id);
            buf.writeUtf(json, MAX_JSON);
        });
    }

    private static Map<ResourceLocation, String> readMap(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        Map<ResourceLocation, String> map = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            map.put(buf.readResourceLocation(), buf.readUtf(MAX_JSON));
        }
        return map;
    }

    public static void handle(S2CContinuumDataPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) ContinuumClientHooks.acceptData(msg.snapshot);
        });
    }
}
