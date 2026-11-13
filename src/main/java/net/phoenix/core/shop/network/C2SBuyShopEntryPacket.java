package net.phoenix.core.shop.network;

import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.common.data.item.PhoenixItems;
import net.phoenix.core.shop.ShopEntry;
import net.phoenix.core.shop.WorldShopData;

import java.util.Optional;
import java.util.UUID;

public record C2SBuyShopEntryPacket(UUID entryId) implements CustomPacketPayload {

    public static final Type<C2SBuyShopEntryPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_buy_shop_entry_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SBuyShopEntryPacket> STREAM_CODEC = StreamCodec
            .composite(
                    UUIDUtil.STREAM_CODEC, C2SBuyShopEntryPacket::entryId,
                    C2SBuyShopEntryPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
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
            if (!player.hasPermissions(2) &&
                    !net.phoenixvine.wiki.api.gate.SuiteGates.isOpen(player, entry.requires)) {
                player.displayClientMessage(net.phoenixvine.wiki.api.gate.SuiteGates.denialFor(entry.requires), false);
                return;
            }
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
