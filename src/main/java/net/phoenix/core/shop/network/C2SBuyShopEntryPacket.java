package net.phoenix.core.shop.network;


import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import net.phoenix.core.common.data.item.PhoenixItems;
import net.phoenix.core.shop.ShopEntry;
import net.phoenix.core.shop.WorldShopData;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class C2SBuyShopEntryPacket implements CustomPacketPayload {
    public static final Type<C2SBuyShopEntryPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_buy_shop_entry_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SBuyShopEntryPacket> STREAM_CODEC = StreamCodec.of((buf, msg) -> C2SBuyShopEntryPacket.encode(msg, buf), C2SBuyShopEntryPacket::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    private final UUID entryId;

    public C2SBuyShopEntryPacket(UUID entryId) {
        this.entryId = entryId;
    }

    public static void encode(C2SBuyShopEntryPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.entryId);
    }

    public static C2SBuyShopEntryPacket decode(FriendlyByteBuf buf) {
        return new C2SBuyShopEntryPacket(buf.readUUID());
    }

    public static void handle(C2SBuyShopEntryPacket packet, IPayloadContext contextSupplier) {
        IPayloadContext context = contextSupplier;
        context.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) context.player());
            if (player == null) return;

            WorldShopData data = WorldShopData.get(player.serverLevel());
            Optional<ShopEntry> match = data.getEntries().stream().filter(e -> e.id.equals(packet.entryId))
                    .findFirst();
            if (match.isEmpty()) {
                player.displayClientMessage(Component.literal("That shop entry no longer exists.")
                        .withStyle(ChatFormatting.RED), false);
                return;
            }

            ShopEntry entry = match.get();
            int have = player.getInventory().countItem(PhoenixItems.PHOENIX_FEATHER.get());
            if (have < entry.cost) {
                player.displayClientMessage(
                        Component.literal("Not enough Phoenix Feathers (" + have + " / " + entry.cost + ").")
                                .withStyle(ChatFormatting.RED),
                        false);
                return;
            }

            player.getInventory().clearOrCountMatchingItems(
                    stack -> stack.getItem() == PhoenixItems.PHOENIX_FEATHER.get(), entry.cost,
                    player.inventoryMenu.getCraftSlots());

            for (var reward : entry.rewards) reward.grant(player);
            player.displayClientMessage(
                    Component.literal("Purchased ").withStyle(ChatFormatting.GOLD)
                            .append(Component.literal(entry.name).withStyle(ChatFormatting.YELLOW)),
                    false);
        });
    }
}
