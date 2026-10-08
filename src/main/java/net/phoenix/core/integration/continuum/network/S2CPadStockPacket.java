package net.phoenix.core.integration.continuum.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.continuum.client.ContinuumClientState;

import java.util.function.Supplier;

/** What the launch pad has loaded: the first rocket in its item input buses and how many probes and kits. */
public class S2CPadStockPacket {

    private final BlockPos pad;
    private final boolean buses;
    private final ItemStack rocket;
    private final int probes;
    private final int kits;

    public S2CPadStockPacket(BlockPos pad, boolean buses, ItemStack rocket, int probes, int kits) {
        this.pad = pad;
        this.buses = buses;
        this.rocket = rocket;
        this.probes = probes;
        this.kits = kits;
    }

    public S2CPadStockPacket(FriendlyByteBuf buf) {
        this.pad = buf.readBlockPos();
        this.buses = buf.readBoolean();
        this.rocket = buf.readItem();
        this.probes = buf.readVarInt();
        this.kits = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pad);
        buf.writeBoolean(buses);
        buf.writeItem(rocket);
        buf.writeVarInt(probes);
        buf.writeVarInt(kits);
    }

    public static void handle(S2CPadStockPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) {
                ContinuumClientState.acceptPadStock(msg.pad, msg.buses, msg.rocket, msg.probes, msg.kits);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
