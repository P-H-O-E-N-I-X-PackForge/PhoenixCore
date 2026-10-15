package net.phoenix.core.integration.gregvaults.network;


import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import net.phoenix.core.integration.gregvaults.client.screen.AbstractVaultMenu;
import net.phoenix.core.integration.gregvaults.client.screen.VaultSlot;

import java.util.function.Supplier;

public class CPacketStackedPickup implements CustomPacketPayload {
    public static final Type<CPacketStackedPickup> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c_packet_stacked_pickup"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CPacketStackedPickup> STREAM_CODEC = StreamCodec.of((buf, msg) -> CPacketStackedPickup.encode(msg, buf), CPacketStackedPickup::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


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

    public static void handle(CPacketStackedPickup packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
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
                if (!ItemStack.isSameItemSameComponents(carried, visible)) return;
                amount = Math.min(amount, carried.getMaxStackSize() - carried.getCount());
            }

            if (amount <= 0) return;

            menu.doStackedPickup(slot, amount);
        });
        }
}
