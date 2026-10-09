package net.phoenix.core.integration.continuum.machine;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.common.machine.multiblock.part.EnergyHatchPartMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ItemBusPartMachine;
import com.gregtechceu.gtceu.utils.ExtendedUseOnContext;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import net.phoenix.core.integration.continuum.network.S2COpenMapPacket;
import net.phoenix.core.network.PhoenixNetwork;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class LaunchPadMachine extends MultiblockControllerMachine {

    public LaunchPadMachine(BlockEntityCreationInfo info) {
        super(info);
    }

    private List<NotifiableItemStackHandler> inputInventories() {
        List<NotifiableItemStackHandler> inventories = new ArrayList<>();
        for (var part : getParts()) {
            if (!(part instanceof ItemBusPartMachine bus)) continue;
            NotifiableItemStackHandler inventory = bus.getInventory();
            if (inventory.handlerIO == IO.IN || inventory.handlerIO == IO.BOTH) inventories.add(inventory);
        }
        return inventories;
    }

    public int count(Predicate<ItemStack> matches) {
        int total = 0;
        for (var inventory : inputInventories()) {
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (!stack.isEmpty() && matches.test(stack)) total += stack.getCount();
            }
        }
        return total;
    }

    public ItemStack peek(Predicate<ItemStack> matches) {
        for (var inventory : inputInventories()) {
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (!stack.isEmpty() && matches.test(stack)) return stack.copyWithCount(1);
            }
        }
        return ItemStack.EMPTY;
    }

    public List<ItemStack> take(Predicate<ItemStack> matches, int amount) {
        List<ItemStack> taken = new ArrayList<>();
        int remaining = amount;
        for (var inventory : inputInventories()) {
            for (int slot = 0; slot < inventory.getSlots() && remaining > 0; slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (stack.isEmpty() || !matches.test(stack)) continue;

                ItemStack extracted = inventory.extractItemInternal(slot, Math.min(remaining, stack.getCount()), false);
                if (extracted.isEmpty()) continue;
                remaining -= extracted.getCount();
                taken.add(extracted);
            }
        }
        return taken;
    }

    private List<NotifiableItemStackHandler> inventories(boolean output) {
        List<NotifiableItemStackHandler> inventories = new ArrayList<>();
        for (var part : getParts()) {
            if (!(part instanceof ItemBusPartMachine bus)) continue;
            NotifiableItemStackHandler inventory = bus.getInventory();
            IO io = inventory.handlerIO;
            if (io == IO.BOTH || io == (output ? IO.OUT : IO.IN)) inventories.add(inventory);
        }
        return inventories;
    }

    private static ItemStack insertInto(ItemStack stack, List<NotifiableItemStackHandler> inventories) {
        ItemStack rest = stack.copy();
        for (var inventory : inventories) {
            for (int slot = 0; slot < inventory.getSlots() && !rest.isEmpty(); slot++) {
                rest = inventory.insertItemInternal(slot, rest, false);
            }
            if (rest.isEmpty()) break;
        }
        return rest;
    }

    public ItemStack insertOutput(ItemStack stack) {
        return insertInto(stack, inventories(true));
    }

    public ItemStack insertInput(ItemStack stack) {
        return insertInto(stack, inventories(false));
    }

    public int padTier() {
        int tier = 0;
        for (var part : getParts()) {
            if (part instanceof EnergyHatchPartMachine hatch) tier = Math.max(tier, hatch.getTier());
        }
        return tier;
    }

    public long storedEnergy() {
        long total = 0;
        for (var part : getParts()) {
            if (part instanceof EnergyHatchPartMachine hatch) total += hatch.energyContainer.getEnergyStored();
        }
        return total;
    }

    public boolean drainEnergy(long amount) {
        if (amount <= 0) return true;
        if (storedEnergy() < amount) return false;

        long remaining = amount;
        for (var part : getParts()) {
            if (remaining <= 0) break;
            if (!(part instanceof EnergyHatchPartMachine hatch)) continue;

            long take = Math.min(remaining, hatch.energyContainer.getEnergyStored());
            if (take <= 0) continue;
            hatch.energyContainer.changeEnergy(-take);
            remaining -= take;
        }
        return true;
    }

    @Override
    public InteractionResult onUse(ExtendedUseOnContext context) {
        Player player = context.getPlayer();
        if (!isFormed() || player == null || player.isShiftKeyDown()) return super.onUse(context);

        Level level = context.getLevel();
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            PhoenixNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new S2COpenMapPacket(getBlockPos()));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
