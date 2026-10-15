package net.phoenix.core.integration.gregvaults.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.gregvaults.client.ClientVaultPacketHandlers;

import java.util.function.Supplier;

public class SPacketVaultContents {

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

    public static void handle(SPacketVaultContents packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientVaultPacketHandlers.handleContents(packet)));
        ctx.get().setPacketHandled(true);
    }
}
