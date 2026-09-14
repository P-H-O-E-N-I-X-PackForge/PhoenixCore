package net.phoenix.core.shop.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.shop.WorldShopData;

import java.util.UUID;
import java.util.function.Supplier;

public class C2SRemoveShopEntryPacket {

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

    public static void handle(C2SRemoveShopEntryPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.hasPermissions(2)) return;

            WorldShopData data = WorldShopData.get(player.serverLevel());
            if (data.removeEntry(packet.entryId)) {
                C2SAddShopEntryPacket.broadcastSync(data);
            }
        });
        context.setPacketHandled(true);
    }
}
