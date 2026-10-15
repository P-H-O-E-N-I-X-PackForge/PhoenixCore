package net.phoenix.core.integration.gregpacks.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SPacketJetpackThrust {

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

    public static void handle(SPacketJetpackThrust msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> applyThrust(msg));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void applyThrust(SPacketJetpackThrust msg) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        player.setDeltaMovement(msg.velX, msg.velY, msg.velZ);
        player.fallDistance = 0f;
    }
}
