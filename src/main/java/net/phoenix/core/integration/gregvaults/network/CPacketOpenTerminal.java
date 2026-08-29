package net.phoenix.core.integration.gregvaults.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.gregvaults.common.items.WirelessTerminalItem;

import java.util.function.Supplier;

public class CPacketOpenTerminal {

    private final int slot;
    private final boolean offhand;

    public CPacketOpenTerminal(int slot, boolean offhand) {
        this.slot = slot;
        this.offhand = offhand;
    }

    public static void encode(CPacketOpenTerminal packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.slot);
        buf.writeBoolean(packet.offhand);
    }

    public static CPacketOpenTerminal decode(FriendlyByteBuf buf) {
        return new CPacketOpenTerminal(buf.readInt(), buf.readBoolean());
    }

    public static void handle(CPacketOpenTerminal packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ItemStack stack;
            if (packet.offhand) {
                stack = player.getOffhandItem();
            } else if (packet.slot >= 0 && packet.slot < player.getInventory().getContainerSize()) {
                stack = player.getInventory().getItem(packet.slot);
            } else {
                return;
            }

            if (!(stack.getItem() instanceof WirelessTerminalItem)) return;

            InteractionHand hand = packet.offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            stack.getItem().use(player.level(), player, hand);
        });
        ctx.get().setPacketHandled(true);
    }
}
