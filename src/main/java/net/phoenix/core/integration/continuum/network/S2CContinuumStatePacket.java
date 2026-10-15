package net.phoenix.core.integration.continuum.network;


import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.fml.loading.FMLEnvironment;

import net.phoenix.core.integration.continuum.client.ContinuumClientHooks;
import net.phoenix.core.integration.continuum.common.ContinuumStateSnapshot;

import java.util.function.Supplier;

public class S2CContinuumStatePacket implements CustomPacketPayload {
    public static final Type<S2CContinuumStatePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2_c_continuum_state_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CContinuumStatePacket> STREAM_CODEC = StreamCodec.ofMember(S2CContinuumStatePacket::encode, S2CContinuumStatePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    private final ContinuumStateSnapshot snapshot;

    public S2CContinuumStatePacket(ContinuumStateSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public S2CContinuumStatePacket(FriendlyByteBuf buf) {
        this.snapshot = ContinuumStateSnapshot.read(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        snapshot.write(buf);
    }

    public static void handle(S2CContinuumStatePacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) ContinuumClientHooks.acceptState(msg.snapshot);
        });
        }
}
