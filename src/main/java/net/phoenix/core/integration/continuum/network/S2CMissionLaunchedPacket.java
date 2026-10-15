package net.phoenix.core.integration.continuum.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.continuum.client.ContinuumClientHooks;

public class S2CMissionLaunchedPacket implements CustomPacketPayload {

    public static final Type<S2CMissionLaunchedPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2_c_mission_launched_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMissionLaunchedPacket> STREAM_CODEC = StreamCodec
            .ofMember(S2CMissionLaunchedPacket::encode, S2CMissionLaunchedPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final boolean accepted;
    private final ResourceLocation destination;
    private final BlockPos pad;
    private final String message;

    public S2CMissionLaunchedPacket(boolean accepted, ResourceLocation destination, BlockPos pad, String message) {
        this.accepted = accepted;
        this.destination = destination;
        this.pad = pad;
        this.message = message;
    }

    public S2CMissionLaunchedPacket(FriendlyByteBuf buf) {
        this.accepted = buf.readBoolean();
        this.destination = buf.readResourceLocation();
        this.pad = buf.readBlockPos();
        this.message = buf.readUtf(256);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(accepted);
        buf.writeResourceLocation(destination);
        buf.writeBlockPos(pad);
        buf.writeUtf(message, 256);
    }

    public static void handle(S2CMissionLaunchedPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) {
                ContinuumClientHooks.missionResult(msg.accepted, msg.destination, msg.pad, msg.message);
            }
        });
    }
}
