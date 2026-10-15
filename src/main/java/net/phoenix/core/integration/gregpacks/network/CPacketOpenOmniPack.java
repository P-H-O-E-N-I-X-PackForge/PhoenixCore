package net.phoenix.core.integration.gregpacks.network;


import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import net.phoenix.core.integration.gregpacks.common.inventory.OpenPackHelper;
import net.phoenix.core.integration.gregpacks.common.item.OmniPackItem;
import net.phoenix.core.integration.gregpacks.common.item.OmniPackTier;
import net.phoenix.core.integration.gregpacks.common.registry.OmniPackBlockItem;

import java.util.function.Supplier;

public class CPacketOpenOmniPack implements CustomPacketPayload {
    public static final Type<CPacketOpenOmniPack> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c_packet_open_omni_pack"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CPacketOpenOmniPack> STREAM_CODEC = StreamCodec.of((buf, msg) -> CPacketOpenOmniPack.encode(msg, buf), CPacketOpenOmniPack::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    public CPacketOpenOmniPack() {}

    public static void encode(CPacketOpenOmniPack msg, FriendlyByteBuf buf) {}

    public static CPacketOpenOmniPack decode(FriendlyByteBuf buf) {
        return new CPacketOpenOmniPack();
    }

    public static void handle(CPacketOpenOmniPack msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;

            if (net.neoforged.fml.ModList.get().isLoaded("curios")) {
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
        }

    private static OmniPackTier getTier(ItemStack stack) {
        if (stack.getItem() instanceof OmniPackItem packItem) return packItem.getTier();
        if (stack.getItem() instanceof OmniPackBlockItem blockItem) return blockItem.getBlock().getTier();
        return null;
    }
}
