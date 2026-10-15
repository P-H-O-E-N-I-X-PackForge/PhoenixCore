package net.phoenix.core.integration.gregvaults.network;


import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.DistExecutor;

import net.phoenix.core.integration.gregvaults.client.ClientVaultPacketHandlers;

import java.util.function.Supplier;

public class SPacketVaultContents implements CustomPacketPayload {
    public static final Type<SPacketVaultContents> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s_packet_vault_contents"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SPacketVaultContents> STREAM_CODEC = StreamCodec.of((buf, msg) -> SPacketVaultContents.encode(msg, buf), SPacketVaultContents::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    private final int containerId;
    private final ItemStack[] stacks;

    public SPacketVaultContents(int containerId, ItemStack[] stacks) {
        this.containerId = containerId;
        this.stacks = stacks;
    }

    public int getContainerId() {
        return containerId;
    }

    public ItemStack[] getStacks() {
        return stacks;
    }

    public static void encode(SPacketVaultContents packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.containerId);
        buf.writeVarInt(packet.stacks.length);
        for (ItemStack stack : packet.stacks) buf.writeItem(stack);
    }

    public static SPacketVaultContents decode(FriendlyByteBuf buf) {
        int containerId = buf.readVarInt();
        int size = buf.readVarInt();
        ItemStack[] stacks = new ItemStack[size];
        for (int i = 0; i < size; i++) stacks[i] = buf.readItem();
        return new SPacketVaultContents(containerId, stacks);
    }

    public static void handle(SPacketVaultContents packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) ClientVaultPacketHandlers.handleContents(packet);
        });
    }
}
