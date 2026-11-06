package net.phoenix.core.common.machine.electric;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.common.machine.electric.BatteryBufferMachine;
import com.gregtechceu.gtceu.common.machine.trait.AutoOutputTrait;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AutoChargerMachine extends BatteryBufferMachine {

    public AutoChargerMachine(BlockEntityCreationInfo info, int tier, int slots) {
        super(info, tier, slots, AMPS_PER_BATTERY_CHARGER, 0L);
        attachPersistentTrait("autoOutput", new AutoOutputTrait(List.of(new ChargedOnly(getBatteryInventory())),
                List.of()));
    }

    public static boolean isFullyCharged(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var electric = GTCapabilityHelper.getElectricItem(stack);
        if (electric != null) return electric.getCharge() >= electric.getMaxCharge();
        var fe = GTCapabilityHelper.getForgeEnergyItem(stack);
        return fe != null && fe.getEnergyStored() >= fe.getMaxEnergyStored();
    }

    private record ChargedOnly(IItemHandler inner) implements IItemHandler {

        @Override
        public int getSlots() {
            return inner.getSlots();
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            ItemStack stack = inner.getStackInSlot(slot);
            return isFullyCharged(stack) ? stack : ItemStack.EMPTY;
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return isFullyCharged(inner.getStackInSlot(slot)) ? inner.extractItem(slot, amount, simulate) :
                    ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return inner.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return false;
        }
    }
}
