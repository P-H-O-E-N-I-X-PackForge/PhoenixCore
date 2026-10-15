package net.phoenix.core.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.NetworkEvent;
import net.phoenix.core.integration.conflux.dimension.physics.ClientImpulses;
import net.phoenix.core.integration.conflux.dimension.physics.PhysicsImpulse;

import java.util.function.Supplier;

/** A landform's nudge to the receiving player's own motion (an updraft, a conveyor, a bounce, low gravity). */
public class S2CPhysicsImpulsePacket {

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

    public static void handle(S2CPhysicsImpulsePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (!FMLEnvironment.dist.isClient()) return;
            ClientImpulses.apply(msg.impulse);
        });
        ctx.get().setPacketHandled(true);
    }
}
