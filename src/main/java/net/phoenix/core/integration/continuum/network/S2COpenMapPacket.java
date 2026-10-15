package net.phoenix.core.integration.continuum.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.continuum.client.ContinuumClientHooks;

import java.util.function.Supplier;

public class S2COpenMapPacket {

    private final BlockPos pad;

    public S2COpenMapPacket(BlockPos pad) {
        this.pad = pad;
    }

    public S2COpenMapPacket(FriendlyByteBuf buf) {
        this.pad = buf.readBlockPos();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pad);
    }

    public static void handle(S2COpenMapPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) ContinuumClientHooks.openMap(msg.pad);
        });
        ctx.get().setPacketHandled(true);
    }
}
