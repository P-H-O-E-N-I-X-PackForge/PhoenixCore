package net.phoenix.core.integration.gregpacks.network;


import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.ForgeCapabilities;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import net.phoenix.core.integration.gregpacks.common.block.OmniPackBlockEntity;
import net.phoenix.core.integration.gregpacks.common.inventory.OmniPackMenu;

import java.util.function.Supplier;

public class CPacketFluidInteract implements CustomPacketPayload {
    public static final Type<CPacketFluidInteract> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c_packet_fluid_interact"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CPacketFluidInteract> STREAM_CODEC = StreamCodec.of((buf, msg) -> CPacketFluidInteract.encode(msg, buf), CPacketFluidInteract::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    private final boolean isLeftClick;

    public CPacketFluidInteract(boolean isLeftClick) {
        this.isLeftClick = isLeftClick;
    }

    public static void encode(CPacketFluidInteract msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.isLeftClick);
    }

    public static CPacketFluidInteract decode(FriendlyByteBuf buf) {
        return new CPacketFluidInteract(buf.readBoolean());
    }

    public static void handle(CPacketFluidInteract msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null || !(player.containerMenu instanceof OmniPackMenu menu)) return;

            ItemStack carried = menu.getCarried();
            if (carried.isEmpty()) return;

            IFluidHandler packHandler = menu.getFluidHandler();
            if (packHandler == null) return;

            FluidActionResult result = FluidActionResult.FAILURE;

            if (msg.isLeftClick) {
                result = FluidUtil.tryEmptyContainer(carried, packHandler, Integer.MAX_VALUE, player, true);

                if (!result.isSuccess()) {
                    result = carried.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).map(itemHandler -> {

                        int moved = FluidUtil.tryFluidTransfer(packHandler, itemHandler, Integer.MAX_VALUE, true)
                                .getAmount();
                        return moved > 0 ? new FluidActionResult(itemHandler.getContainer()) :
                                FluidActionResult.FAILURE;
                    }).orElse(FluidActionResult.FAILURE);
                }
            } else {
                result = carried.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).map(itemHandler -> {
                    int moved = FluidUtil.tryFluidTransfer(itemHandler, packHandler, Integer.MAX_VALUE, true)
                            .getAmount();
                    return moved > 0 ? new FluidActionResult(itemHandler.getContainer()) : FluidActionResult.FAILURE;
                }).orElse(FluidActionResult.FAILURE);

                if (!result.isSuccess()) {
                    result = FluidUtil.tryFillContainer(carried, packHandler, Integer.MAX_VALUE, player, true);
                }
            }

            if (result.isSuccess()) {
                menu.setCarried(result.getResult());
                menu.broadcastChanges();

                OmniPackBlockEntity be = menu.getSourceBlockEntity();
                if (be != null) {
                    be.setChanged();
                    player.level().sendBlockUpdated(
                            be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
                }
            }
        });
        }
}
