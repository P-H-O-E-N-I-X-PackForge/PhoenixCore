package net.phoenix.core.integration.gregpacks.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class SPacketJetpackThrust implements CustomPacketPayload {

    public static final Type<SPacketJetpackThrust> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s_packet_jetpack_thrust"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SPacketJetpackThrust> STREAM_CODEC = StreamCodec
            .of((buf, msg) -> SPacketJetpackThrust.encode(msg, buf), SPacketJetpackThrust::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final double velX;
    private final double velY;
    private final double velZ;

    public SPacketJetpackThrust(double velX, double velY, double velZ) {
        this.velX = velX;
        this.velY = velY;
        this.velZ = velZ;
    }

    public static void encode(SPacketJetpackThrust msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.velX);
        buf.writeDouble(msg.velY);
        buf.writeDouble(msg.velZ);
    }

    public static SPacketJetpackThrust decode(FriendlyByteBuf buf) {
        return new SPacketJetpackThrust(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public static void handle(SPacketJetpackThrust msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> applyThrust(msg));
    }

    @OnlyIn(Dist.CLIENT)
    private static void applyThrust(SPacketJetpackThrust msg) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        player.setDeltaMovement(msg.velX, msg.velY, msg.velZ);
        player.fallDistance = 0f;
    }
}
