package net.phoenix.core.integration.continuum.network;


import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLEnvironment;

import net.phoenix.core.integration.continuum.client.ContinuumClientState;

import java.util.function.Supplier;

public class S2CPadStockPacket implements CustomPacketPayload {
    public static final Type<S2CPadStockPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2_c_pad_stock_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CPadStockPacket> STREAM_CODEC = StreamCodec.ofMember(S2CPadStockPacket::encode, S2CPadStockPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    private final BlockPos pad;
    private final boolean buses;
    private final ItemStack rocket;
    private final int probes;
    private final int kits;
    private final int rockets;

    public S2CPadStockPacket(BlockPos pad, boolean buses, ItemStack rocket, int probes, int kits, int rockets) {
        this.rockets = rockets;
        this.pad = pad;
        this.buses = buses;
        this.rocket = rocket;
        this.probes = probes;
        this.kits = kits;
    }

    public S2CPadStockPacket(RegistryFriendlyByteBuf buf) {
        this.pad = buf.readBlockPos();
        this.buses = buf.readBoolean();
        this.rocket = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        this.probes = buf.readVarInt();
        this.kits = buf.readVarInt();
        this.rockets = buf.readVarInt();
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pad);
        buf.writeBoolean(buses);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, rocket);
        buf.writeVarInt(probes);
        buf.writeVarInt(kits);
        buf.writeVarInt(rockets);
    }

    public static void handle(S2CPadStockPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) {
                ContinuumClientState.acceptPadStock(msg.pad, msg.buses, msg.rocket, msg.probes, msg.kits,
                        msg.rockets);
            }
        });
        }
}
