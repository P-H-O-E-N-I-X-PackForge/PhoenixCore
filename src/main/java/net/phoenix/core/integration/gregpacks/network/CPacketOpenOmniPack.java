package net.phoenix.core.integration.gregpacks.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.gregpacks.common.inventory.OpenPackHelper;
import net.phoenix.core.integration.gregpacks.common.item.OmniPackItem;
import net.phoenix.core.integration.gregpacks.common.item.OmniPackTier;
import net.phoenix.core.integration.gregpacks.common.registry.OmniPackBlockItem;

import java.util.function.Supplier;

public class CPacketOpenOmniPack {

    public CPacketOpenOmniPack() {}

    public static void encode(CPacketOpenOmniPack msg, FriendlyByteBuf buf) {}

    public static CPacketOpenOmniPack decode(FriendlyByteBuf buf) {
        return new CPacketOpenOmniPack();
    }

    public static void handle(CPacketOpenOmniPack msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            if (net.minecraftforge.fml.ModList.get().isLoaded("curios")) {
                top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(inv -> {
                    var slots = inv.getCurios();
                    if (!slots.containsKey("back")) return;
                    var handler = slots.get("back").getStacks();
                    for (int i = 0; i < handler.getSlots(); i++) {
                        ItemStack stack = handler.getStackInSlot(i);
                        OmniPackTier tier = getTier(stack);
                        if (tier == null) continue;
                        OpenPackHelper.open(player, stack, tier, -1);
                        return;
                    }
                });
                return;
            }

            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                OmniPackTier tier = getTier(stack);
                if (tier == null) continue;
                OpenPackHelper.open(player, stack, tier, i);
                return;
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static OmniPackTier getTier(ItemStack stack) {
        if (stack.getItem() instanceof OmniPackItem packItem) return packItem.getTier();
        if (stack.getItem() instanceof OmniPackBlockItem blockItem) return blockItem.getBlock().getTier();
        return null;
    }
}
