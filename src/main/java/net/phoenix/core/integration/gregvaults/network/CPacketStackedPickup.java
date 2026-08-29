package net.phoenix.core.integration.gregvaults.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.gregvaults.client.screen.AbstractVaultMenu;
import net.phoenix.core.integration.gregvaults.client.screen.VaultSlot;

import java.util.function.Supplier;

public class CPacketStackedPickup {

    private final int slotIndex;
    private final boolean half;

    public CPacketStackedPickup(int slotIndex, boolean half) {
        this.slotIndex = slotIndex;
        this.half = half;
    }

    public static void encode(CPacketStackedPickup p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.slotIndex);
        buf.writeBoolean(p.half);
    }

    public static CPacketStackedPickup decode(FriendlyByteBuf buf) {
        return new CPacketStackedPickup(buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(CPacketStackedPickup packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (!(player.containerMenu instanceof AbstractVaultMenu menu)) return;

            int idx = packet.slotIndex;
            if (idx < 0 || idx >= menu.slots.size()) return;
            Slot slot = menu.slots.get(idx);
            if (!(slot instanceof VaultSlot vs) || !vs.isAggregated()) return;

            ItemStack visible = slot.getItem();
            if (visible.isEmpty()) return;

            int amount = packet.half ? Math.max(1, slot.getMaxStackSize() / 2) : slot.getMaxStackSize();

            ItemStack carried = player.containerMenu.getCarried();
            if (!carried.isEmpty()) {
                if (!ItemStack.isSameItemSameTags(carried, visible)) return;
                amount = Math.min(amount, carried.getMaxStackSize() - carried.getCount());
            }

            if (amount <= 0) return;

            menu.doStackedPickup(slot, amount);
        });
        ctx.get().setPacketHandled(true);
    }
}
