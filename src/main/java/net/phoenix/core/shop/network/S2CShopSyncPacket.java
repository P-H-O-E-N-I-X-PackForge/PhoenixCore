package net.phoenix.core.shop.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import net.phoenix.core.shop.RewardSpec;
import net.phoenix.core.shop.ShopEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class S2CShopSyncPacket {

    public record ShopEntryView(UUID id, String name, ItemStack icon, int cost, List<String> rewardDescriptions,
            List<RewardSpec> rewardSpecs, String category) {}

    private final List<ShopEntryView> entries;

    public S2CShopSyncPacket(List<ShopEntry> entries) {
        this.entries = new ArrayList<>();
        for (ShopEntry e : entries) {
            List<String> descriptions = e.rewards.stream().map(r -> r.describe().getString())
                    .collect(Collectors.toList());
            List<RewardSpec> specs = e.rewards.stream().map(r -> r.toSpec()).collect(Collectors.toList());
            this.entries.add(new ShopEntryView(e.id, e.name, e.icon, e.cost, descriptions, specs, e.category));
        }
    }

    private S2CShopSyncPacket(List<ShopEntryView> entries, boolean ignored) {
        this.entries = entries;
    }

    public static void encode(S2CShopSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entries.size());
        for (ShopEntryView view : packet.entries) {
            buf.writeUUID(view.id());
            buf.writeUtf(view.name());
            buf.writeItem(view.icon());
            buf.writeVarInt(view.cost());
            buf.writeUtf(view.category());
            buf.writeVarInt(view.rewardDescriptions().size());
            for (String desc : view.rewardDescriptions()) buf.writeUtf(desc);
            buf.writeVarInt(view.rewardSpecs().size());
            for (RewardSpec spec : view.rewardSpecs()) {
                buf.writeUtf(spec.type());
                buf.writeUtf(spec.param());
                buf.writeItem(spec.itemParam());
            }
        }
    }

    public static S2CShopSyncPacket decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<ShopEntryView> views = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            UUID id = buf.readUUID();
            String name = buf.readUtf();
            ItemStack icon = buf.readItem();
            int cost = buf.readVarInt();
            String category = buf.readUtf();
            int rewardCount = buf.readVarInt();
            List<String> descriptions = new ArrayList<>(rewardCount);
            for (int j = 0; j < rewardCount; j++) descriptions.add(buf.readUtf());
            int specCount = buf.readVarInt();
            List<RewardSpec> specs = new ArrayList<>(specCount);
            for (int j = 0; j < specCount; j++) {
                specs.add(new RewardSpec(buf.readUtf(), buf.readUtf(), buf.readItem()));
            }
            views.add(new ShopEntryView(id, name, icon, cost, descriptions, specs, category));
        }
        return new S2CShopSyncPacket(views, true);
    }

    public static void handle(S2CShopSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> net.phoenix.core.shop.client.PhoenixShopScreen.openOrUpdate(packet.entries)));
        context.setPacketHandled(true);
    }
}
