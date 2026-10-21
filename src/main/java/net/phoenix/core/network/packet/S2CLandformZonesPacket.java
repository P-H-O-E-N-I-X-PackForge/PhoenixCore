package net.phoenix.core.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.conflux.dimension.worldgen.Landforms;
import net.phoenix.core.integration.solaris.ClientLandformZones;

import java.util.ArrayList;
import java.util.List;

/** The physics-effect zones of the landforms a team has found, for the map to draw. Replaces the client's list. */
public class S2CLandformZonesPacket implements CustomPacketPayload {

    public static final Type<S2CLandformZonesPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2c_landform_zones"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CLandformZonesPacket> STREAM_CODEC = StreamCodec
            .ofMember(S2CLandformZonesPacket::encode, S2CLandformZonesPacket::new);

    public record ZoneDef(String dimension, Landforms.Zone zone) {}

    private final List<ZoneDef> zones;

    public S2CLandformZonesPacket(List<ZoneDef> zones) {
        this.zones = zones;
    }

    public S2CLandformZonesPacket(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        this.zones = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String dimension = buf.readUtf(128);
            Landforms.ZoneType type = Landforms.ZoneType.values()[buf.readByte()];
            Landforms.ZoneShape shape = Landforms.ZoneShape.values()[buf.readByte()];
            zones.add(new ZoneDef(dimension, new Landforms.Zone(type, shape, buf.readDouble(), buf.readDouble(),
                    buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readLong())));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(zones.size());
        for (ZoneDef def : zones) {
            Landforms.Zone zone = def.zone();
            buf.writeUtf(def.dimension(), 128);
            buf.writeByte(zone.type().ordinal());
            buf.writeByte(zone.shape().ordinal());
            buf.writeDouble(zone.x());
            buf.writeDouble(zone.z());
            buf.writeDouble(zone.a());
            buf.writeDouble(zone.b());
            buf.writeDouble(zone.theta());
            buf.writeLong(zone.phase());
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CLandformZonesPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!FMLEnvironment.dist.isClient()) return;
            ClientLandformZones.set(msg.zones);
        });
    }
}
