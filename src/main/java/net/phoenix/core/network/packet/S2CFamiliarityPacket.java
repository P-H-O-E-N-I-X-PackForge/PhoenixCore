package net.phoenix.core.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.solaris.ClientFamiliarity;

public class S2CFamiliarityPacket implements CustomPacketPayload {

    public static final Type<S2CFamiliarityPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2c_familiarity"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CFamiliarityPacket> STREAM_CODEC = StreamCodec
            .ofMember(S2CFamiliarityPacket::encode, S2CFamiliarityPacket::new);

    private final String dimension;
    private final int centerX;
    private final int centerZ;
    private final int radius;
    private final byte[] levels;

    public S2CFamiliarityPacket(String dimension, int centerX, int centerZ, int radius, byte[] levels) {
        this.dimension = dimension;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = radius;
        this.levels = levels;
    }

    public S2CFamiliarityPacket(FriendlyByteBuf buf) {
        this.dimension = buf.readUtf(128);
        this.centerX = buf.readInt();
        this.centerZ = buf.readInt();
        this.radius = buf.readByte();
        this.levels = buf.readByteArray();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(dimension, 128);
        buf.writeInt(centerX);
        buf.writeInt(centerZ);
        buf.writeByte(radius);
        buf.writeByteArray(levels);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CFamiliarityPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!FMLEnvironment.dist.isClient()) return;
            ClientFamiliarity.apply(msg.dimension, msg.centerX, msg.centerZ, msg.radius, msg.levels);
        });
    }
}
