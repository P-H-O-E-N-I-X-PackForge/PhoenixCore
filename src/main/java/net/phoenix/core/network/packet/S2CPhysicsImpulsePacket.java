package net.phoenix.core.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.conflux.dimension.physics.ClientImpulses;
import net.phoenix.core.integration.conflux.dimension.physics.PhysicsImpulse;

public class S2CPhysicsImpulsePacket implements CustomPacketPayload {

    public static final Type<S2CPhysicsImpulsePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2_c_physics_impulse_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CPhysicsImpulsePacket> STREAM_CODEC = StreamCodec
            .ofMember(S2CPhysicsImpulsePacket::encode, S2CPhysicsImpulsePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final PhysicsImpulse impulse;

    public S2CPhysicsImpulsePacket(PhysicsImpulse impulse) {
        this.impulse = impulse;
    }

    public S2CPhysicsImpulsePacket(FriendlyByteBuf buf) {
        this.impulse = new PhysicsImpulse(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readBoolean());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeFloat((float) impulse.dx());
        buf.writeFloat((float) impulse.dy());
        buf.writeFloat((float) impulse.dz());
        buf.writeFloat((float) impulse.capXZ());
        buf.writeFloat((float) impulse.capUp());
        buf.writeFloat((float) impulse.setUp());
        buf.writeFloat((float) impulse.scaleXZ());
        buf.writeBoolean(impulse.resetFall());
    }

    public static void handle(S2CPhysicsImpulsePacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!FMLEnvironment.dist.isClient()) return;
            ClientImpulses.apply(msg.impulse);
        });
    }
}
