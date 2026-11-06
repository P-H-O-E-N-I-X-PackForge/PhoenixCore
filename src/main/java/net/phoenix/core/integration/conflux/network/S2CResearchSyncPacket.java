package net.phoenix.core.integration.conflux.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.conflux.ConfluxDataType;
import net.phoenix.core.integration.conflux.client.ClientResearchCache;

import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class S2CResearchSyncPacket implements CustomPacketPayload {

    public static final Type<S2CResearchSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2_c_research_sync_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CResearchSyncPacket> STREAM_CODEC = StreamCodec
            .of((buf, msg) -> S2CResearchSyncPacket.encode(msg, buf), S2CResearchSyncPacket::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final Set<ResourceLocation> unlocked;
    private final Set<ResourceLocation> lockedOut;
    private final Set<String> flags;
    @Nullable
    private final String disciplineId;
    @Nullable
    private final String disciplineTitle;
    private final boolean committed;
    private final Map<ConfluxDataType, Long> switchCost;
    private final Map<ConfluxDataType, Long> data;

    public S2CResearchSyncPacket(Set<ResourceLocation> unlocked, Set<ResourceLocation> lockedOut,
                                 Set<String> flags, @Nullable String disciplineId,
                                 @Nullable String disciplineTitle, boolean committed,
                                 Map<ConfluxDataType, Long> switchCost,
                                 Map<ConfluxDataType, Long> data) {
        this.data = data;
        this.unlocked = unlocked;
        this.lockedOut = lockedOut;
        this.flags = flags;
        this.disciplineId = disciplineId;
        this.disciplineTitle = disciplineTitle;
        this.committed = committed;
        this.switchCost = switchCost;
    }

    public static void encode(S2CResearchSyncPacket pkt, FriendlyByteBuf buf) {
        writeRLSet(buf, pkt.unlocked);
        writeRLSet(buf, pkt.lockedOut);
        buf.writeVarInt(pkt.flags.size());
        pkt.flags.forEach(buf::writeUtf);

        buf.writeBoolean(pkt.disciplineId != null);
        if (pkt.disciplineId != null) {
            buf.writeUtf(pkt.disciplineId);
            buf.writeUtf(pkt.disciplineTitle != null ? pkt.disciplineTitle : pkt.disciplineId);
        }
        buf.writeBoolean(pkt.committed);

        buf.writeVarInt(pkt.switchCost.size());
        pkt.switchCost.forEach((type, amt) -> {
            buf.writeUtf(type.id());
            buf.writeVarLong(amt);
        });

        buf.writeVarInt(pkt.data.size());
        pkt.data.forEach((type, amt) -> {
            buf.writeUtf(type.id());
            buf.writeVarLong(amt);
        });
    }

    public static S2CResearchSyncPacket decode(FriendlyByteBuf buf) {
        Set<ResourceLocation> unlocked = readRLSet(buf);
        Set<ResourceLocation> lockedOut = readRLSet(buf);
        Set<String> flags = readStringSet(buf);

        String discId = null;
        String discTitle = null;
        if (buf.readBoolean()) {
            discId = buf.readUtf();
            discTitle = buf.readUtf();
        }
        boolean committed = buf.readBoolean();

        int costSize = buf.readVarInt();
        Map<ConfluxDataType, Long> switchCost = new EnumMap<>(ConfluxDataType.class);
        for (int i = 0; i < costSize; i++) {
            String typeId = buf.readUtf();
            long amt = buf.readVarLong();
            for (ConfluxDataType t : ConfluxDataType.values()) {
                if (t.id().equals(typeId)) {
                    switchCost.put(t, amt);
                    break;
                }
            }
        }

        int dataSize = buf.readVarInt();
        Map<ConfluxDataType, Long> data = new EnumMap<>(ConfluxDataType.class);
        for (int i = 0; i < dataSize; i++) {
            String typeId = buf.readUtf();
            long amt = buf.readVarLong();
            for (ConfluxDataType t : ConfluxDataType.values()) {
                if (t.id().equals(typeId)) {
                    data.put(t, amt);
                    break;
                }
            }
        }

        return new S2CResearchSyncPacket(unlocked, lockedOut, flags, discId, discTitle, committed, switchCost, data);
    }

    public static void handle(S2CResearchSyncPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> applyOnClient(pkt));
    }

    @OnlyIn(Dist.CLIENT)
    private static void applyOnClient(S2CResearchSyncPacket pkt) {
        ClientResearchCache.update(pkt.unlocked, pkt.lockedOut, pkt.flags,
                pkt.disciplineId, pkt.disciplineTitle, pkt.committed, pkt.switchCost, pkt.data);
    }

    private static void writeRLSet(FriendlyByteBuf buf, Set<ResourceLocation> set) {
        buf.writeVarInt(set.size());
        set.forEach(buf::writeResourceLocation);
    }

    private static Set<ResourceLocation> readRLSet(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Set<ResourceLocation> set = new HashSet<>();
        for (int i = 0; i < size; i++) set.add(buf.readResourceLocation());
        return set;
    }

    private static Set<String> readStringSet(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Set<String> set = new HashSet<>();
        for (int i = 0; i < size; i++) set.add(buf.readUtf());
        return set;
    }
}
