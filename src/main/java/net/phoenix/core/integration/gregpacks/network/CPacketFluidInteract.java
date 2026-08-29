package net.phoenix.core.integration.gregpacks.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.gregpacks.common.block.OmniPackBlockEntity;
import net.phoenix.core.integration.gregpacks.common.inventory.OmniPackMenu;

import java.util.function.Supplier;

public class CPacketFluidInteract {

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

    public static void handle(CPacketFluidInteract msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
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
        ctx.get().setPacketHandled(true);
    }
}
