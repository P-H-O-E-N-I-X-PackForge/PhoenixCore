package net.phoenix.core.integration.conflux.signature;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record S2CSignaturePacket(List<Entry> entries) implements CustomPacketPayload {

    public record Entry(SignatureDefinition definition, double value) {}

    public static final Type<S2CSignaturePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2c_signature"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CSignaturePacket> STREAM_CODEC = StreamCodec.of(
            (buf, msg) -> {
                buf.writeVarInt(msg.entries.size());
                for (Entry e : msg.entries) {
                    SignatureDefinition d = e.definition();
                    buf.writeUtf(d.id());
                    buf.writeUtf(d.discipline());
                    buf.writeUtf(d.title());
                    buf.writeUtf(d.description(), 1024);
                    buf.writeInt(d.color());
                    buf.writeUtf(d.icon());
                    buf.writeDouble(d.max());
                    buf.writeDouble(d.start());
                    buf.writeDouble(d.gainPerMinute());
                    buf.writeDouble(d.decayPerMinute());
                    buf.writeVarInt(d.levels().size());
                    for (var level : d.levels()) {
                        buf.writeDouble(level.at());
                        buf.writeUtf(level.name());
                    }
                    buf.writeDouble(e.value());
                }
            },
            buf -> {
                int n = buf.readVarInt();
                List<Entry> entries = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    String id = buf.readUtf();
                    String discipline = buf.readUtf();
                    String title = buf.readUtf();
                    String description = buf.readUtf(1024);
                    int color = buf.readInt();
                    String icon = buf.readUtf();
                    double max = buf.readDouble(), start = buf.readDouble(), gain = buf.readDouble(),
                            decay = buf.readDouble();
                    int levelCount = buf.readVarInt();
                    List<SignatureDefinition.Level> levels = new ArrayList<>(levelCount);
                    for (int l = 0; l < levelCount; l++) {
                        levels.add(new SignatureDefinition.Level(buf.readDouble(), buf.readUtf()));
                    }
                    entries.add(new Entry(new SignatureDefinition(id, discipline, title, description, color, icon, max,
                            start, gain, decay, levels), buf.readDouble()));
                }
                return new S2CSignaturePacket(entries);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CSignaturePacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) ClientSignatureCache.accept(msg.entries());
        });
    }
}
