package net.phoenix.core.shop.network;


import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import net.phoenix.core.shop.WorldShopData;

import java.util.UUID;
import java.util.function.Supplier;

public class C2SRemoveShopEntryPacket implements CustomPacketPayload {
    public static final Type<C2SRemoveShopEntryPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_remove_shop_entry_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRemoveShopEntryPacket> STREAM_CODEC = StreamCodec.of((buf, msg) -> C2SRemoveShopEntryPacket.encode(msg, buf), C2SRemoveShopEntryPacket::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    private final UUID entryId;

    public C2SRemoveShopEntryPacket(UUID entryId) {
        this.entryId = entryId;
    }

    public static void encode(C2SRemoveShopEntryPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.entryId);
    }

    public static C2SRemoveShopEntryPacket decode(FriendlyByteBuf buf) {
        return new C2SRemoveShopEntryPacket(buf.readUUID());
    }

    public static void handle(C2SRemoveShopEntryPacket packet, IPayloadContext contextSupplier) {
        IPayloadContext context = contextSupplier;
        context.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) context.player());
            if (player == null || !player.hasPermissions(2)) return;

            WorldShopData data = WorldShopData.get(player.serverLevel());
            if (data.removeEntry(packet.entryId)) {
                C2SAddShopEntryPacket.broadcastSync(data);
            }
        });
        context.setPacketHandled(true);
    }
}
