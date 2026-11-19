package net.phoenix.core.shop.network;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.shop.WorldShopData;

import java.util.UUID;

public record C2SRemoveShopEntryPacket(UUID entryId) implements CustomPacketPayload {

    public static final Type<C2SRemoveShopEntryPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_remove_shop_entry_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRemoveShopEntryPacket> STREAM_CODEC = StreamCodec
            .composite(
                    UUIDUtil.STREAM_CODEC, C2SRemoveShopEntryPacket::entryId,
                    C2SRemoveShopEntryPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SRemoveShopEntryPacket packet, IPayloadContext contextSupplier) {
        contextSupplier.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) contextSupplier.player());
            if (player == null || !player.hasPermissions(2)) return;

            WorldShopData data = WorldShopData.get(player.serverLevel());
            if (data.removeEntry(packet.entryId)) {
                C2SAddShopEntryPacket.broadcastSync(data);
            }
        });
    }
}
