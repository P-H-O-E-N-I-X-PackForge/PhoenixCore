package net.phoenix.core.shop.network;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.shop.RewardSpec;
import net.phoenix.core.shop.ShopEntry;
import net.phoenix.core.shop.WorldShopData;
import net.phoenix.core.shop.reward.CommandShopReward;
import net.phoenix.core.shop.reward.ItemShopReward;
import net.phoenix.core.shop.reward.ResearchUnlockShopReward;
import net.phoenix.core.shop.reward.ShopReward;
import net.phoenix.core.shop.reward.ThreadShopReward;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class C2SAddShopEntryPacket {

    private final String name;
    private final ItemStack icon;
    private final int cost;
    private final List<RewardSpec> rewardSpecs;
    private final String category;
    private final UUID editId;

    public C2SAddShopEntryPacket(String name, ItemStack icon, int cost, List<RewardSpec> rewardSpecs,
            String category) {
        this(name, icon, cost, rewardSpecs, category, null);
    }

    public C2SAddShopEntryPacket(String name, ItemStack icon, int cost, List<RewardSpec> rewardSpecs,
            String category, UUID editId) {
        this.name = name;
        this.icon = icon;
        this.cost = cost;
        this.rewardSpecs = rewardSpecs;
        this.category = category;
        this.editId = editId;
    }

    public static void encode(C2SAddShopEntryPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.name);
        buf.writeItem(packet.icon);
        buf.writeVarInt(packet.cost);
        buf.writeUtf(packet.category);
        buf.writeVarInt(packet.rewardSpecs.size());
        for (RewardSpec spec : packet.rewardSpecs) {
            buf.writeUtf(spec.type());
            buf.writeUtf(spec.param());
            buf.writeItem(spec.itemParam());
        }
        buf.writeBoolean(packet.editId != null);
        if (packet.editId != null) buf.writeUUID(packet.editId);
    }

    public static C2SAddShopEntryPacket decode(FriendlyByteBuf buf) {
        String name = buf.readUtf();
        ItemStack icon = buf.readItem();
        int cost = buf.readVarInt();
        String category = buf.readUtf();
        int count = buf.readVarInt();
        List<RewardSpec> specs = new ArrayList<>(count);
        for (int i = 0; i < count; i++) specs.add(new RewardSpec(buf.readUtf(), buf.readUtf(), buf.readItem()));
        UUID editId = buf.readBoolean() ? buf.readUUID() : null;
        return new C2SAddShopEntryPacket(name, icon, cost, specs, category, editId);
    }

    public static void handle(C2SAddShopEntryPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.hasPermissions(2)) return;

            List<ShopReward> rewards = new ArrayList<>();
            for (RewardSpec spec : packet.rewardSpecs) {
                ShopReward reward = buildReward(spec);
                if (reward == null) {
                    player.displayClientMessage(
                            Component.literal("Invalid reward parameters for type: " + spec.type())
                                    .withStyle(ChatFormatting.RED),
                            false);
                    return;
                }
                rewards.add(reward);
            }
            if (rewards.isEmpty()) {
                player.displayClientMessage(
                        Component.literal("An entry needs at least one reward.").withStyle(ChatFormatting.RED),
                        false);
                return;
            }

            WorldShopData data = WorldShopData.get(player.serverLevel());
            if (packet.editId != null) {
                data.replaceEntry(packet.editId, new ShopEntry(packet.editId, packet.name, packet.icon.copy(),
                        packet.cost, rewards, packet.category));
            } else {
                data.addEntry(ShopEntry.create(packet.name, packet.icon.copy(), packet.cost, rewards,
                        packet.category));
            }

            broadcastSync(data);
        });
        context.setPacketHandled(true);
    }

    private static ShopReward buildReward(RewardSpec spec) {
        return switch (spec.type()) {
            case ItemShopReward.TYPE -> spec.itemParam().isEmpty() ? null : new ItemShopReward(spec.itemParam().copy());
            case CommandShopReward.TYPE -> new CommandShopReward(spec.param());
            case ResearchUnlockShopReward.TYPE -> new ResearchUnlockShopReward(spec.param());
            case ThreadShopReward.TYPE -> parseThread(spec.param());
            default -> null;
        };
    }

    private static ShopReward parseThread(String param) {
        try {
            return new ThreadShopReward(Integer.parseInt(param.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static void broadcastSync(WorldShopData data) {
        S2CShopSyncPacket sync = new S2CShopSyncPacket(data.getEntries());
        PhoenixNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), sync);
    }
}
