package net.phoenix.core.integration.gregpacks.common.block;

import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.phoenix.core.integration.gregpacks.common.OmniPackTickHandler;
import net.phoenix.core.integration.gregpacks.common.fluid.FluidNBTHelper;
import net.phoenix.core.integration.gregpacks.common.inventory.OmniPackInventory;
import net.phoenix.core.integration.gregpacks.common.inventory.OmniPackMenu;
import net.phoenix.core.integration.gregpacks.common.item.OmniPackTier;
import net.phoenix.core.integration.gregpacks.common.registry.GregPacksBlockEntities;
import net.phoenix.core.integration.gregpacks.common.registry.GregPacksBlocks;
import net.phoenix.core.integration.gregpacks.common.upgrade.UpgradeEffects;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class OmniPackBlockEntity extends BlockEntity implements MenuProvider {

    private final OmniPackTier tier;
    private OmniPackInventory packInventory;
    private OmniPackInventory upgradeInventory;
    private FluidStack fluid = FluidStack.EMPTY;
    private long storedEU = 0L;

    @Setter
    private boolean pickedUp = false;

    private final LazyOptional<IItemHandler> itemCap;
    private final LazyOptional<IFluidHandler> fluidCap;

    public OmniPackBlockEntity(BlockPos pos, BlockState state, OmniPackTier tier) {
        super(GregPacksBlockEntities.getTypeForTier(tier), pos, state);
        this.tier = tier;
        this.packInventory = new OmniPackInventory(tier.defaultSlots);
        this.upgradeInventory = new OmniPackInventory(tier.defaultMaxUpgrades);

        this.itemCap = LazyOptional.of(() -> new InvWrapper(packInventory));

        this.fluidCap = LazyOptional.of(() -> new IFluidHandler() {

            @Override
            public int getTanks() {
                return 1;
            }

            @Override
            public @NotNull FluidStack getFluidInTank(int tank) {
                return fluid != null ? fluid : FluidStack.EMPTY;
            }

            @Override
            public int getTankCapacity(int tank) {
                return (int) new UpgradeEffects(tier, upgradeInventory).totalFluidStorage;
            }

            @Override
            public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
                return fluid.isEmpty() || fluid.isFluidEqual(stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (resource.isEmpty()) return 0;
                if (!fluid.isEmpty() && !fluid.isFluidEqual(resource)) return 0;
                int space = getTankCapacity(0) - fluid.getAmount();
                int filled = Math.min(space, resource.getAmount());
                if (filled <= 0) return 0;
                if (action.execute()) {
                    if (fluid.isEmpty()) {
                        fluid = new FluidStack(resource, filled);
                    } else {
                        fluid.setAmount(fluid.getAmount() + filled);
                    }
                    setChanged();
                }
                return filled;
            }

            @Override
            public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
                if (resource.isEmpty() || fluid.isEmpty() || !resource.isFluidEqual(fluid))
                    return FluidStack.EMPTY;
                return drain(resource.getAmount(), action);
            }

            @Override
            public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
                if (fluid.isEmpty() || maxDrain <= 0) return FluidStack.EMPTY;
                int drained = Math.min(fluid.getAmount(), maxDrain);
                FluidStack result = new FluidStack(fluid, drained);
                if (action.execute()) {
                    fluid.setAmount(fluid.getAmount() - drained);
                    if (fluid.getAmount() <= 0) fluid = FluidStack.EMPTY;
                    setChanged();
                }
                return result;
            }
        });
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.gregpacks.omnipack");
    }

    @Override
    public AbstractContainerMenu createMenu(int windowId, @NotNull Inventory playerInv, @NotNull Player player) {
        UpgradeEffects effects = new UpgradeEffects(tier, upgradeInventory);

        if (effects.totalSlots != packInventory.getContainerSize()) {
            OmniPackInventory resized = new OmniPackInventory(effects.totalSlots);
            for (int i = 0; i < Math.min(effects.totalSlots, packInventory.getContainerSize()); i++) {
                resized.setItem(i, packInventory.getItem(i));
            }
            packInventory = resized;
        }

        OmniPackMenu menu = new OmniPackMenu(windowId, playerInv, packInventory, upgradeInventory, tier);
        menu.setSourceBlockEntity(this);
        if (player instanceof ServerPlayer sp) menu.setServerPlayer(sp);
        return menu;
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCap.cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();

        if (cap == ForgeCapabilities.ENERGY) {
            return LazyOptional.of(() -> new IEnergyStorage() {
                @Override public int getEnergyStored() { return (int) Math.min(storedEU, Integer.MAX_VALUE); }
                @Override public int getMaxEnergyStored() {
                    return (int) Math.min(
                            new UpgradeEffects(tier, upgradeInventory).totalEnergyStorage,
                            Integer.MAX_VALUE);
                }
                @Override public int receiveEnergy(int max, boolean sim) {
                    long space = (long) getMaxEnergyStored() - storedEU;
                    int accepted = (int) Math.min(space, max);
                    if (!sim && accepted > 0) {
                        storedEU += accepted;
                        setChanged();
                    }
                    return accepted;
                }
                @Override public int extractEnergy(int max, boolean sim) { return 0; }
                @Override public boolean canExtract() { return false; }
                @Override public boolean canReceive() { return true; }
            }).cast();
        }
        return super.getCapability(cap, side);
    }

    public void loadFromItemStack(ItemStack stack) {
        UpgradeEffects effects = new UpgradeEffects(tier,
                OmniPackInventory.fromUpgradeItem(stack, tier.defaultMaxUpgrades));

        this.packInventory = OmniPackInventory.fromItem(stack, effects.totalSlots);
        this.upgradeInventory = OmniPackInventory.fromUpgradeItem(stack, tier.defaultMaxUpgrades);
        this.storedEU = OmniPackTickHandler.getStoredEU(stack);
        this.fluid = FluidNBTHelper.getFluid(stack);
        setChanged();
    }

    public ItemStack toItemStack() {
        ItemStack stack = new ItemStack(GregPacksBlocks.getItemForTier(tier));
        packInventory.saveToItem(stack);
        upgradeInventory.saveUpgradesToItem(stack);

        if (storedEU > 0) {
            OmniPackTickHandler.setStoredEU(stack, storedEU,
                    new UpgradeEffects(tier, upgradeInventory).totalEnergyStorage);
        }
        if (!fluid.isEmpty()) {
            FluidNBTHelper.setFluid(stack, fluid);
        }
        return stack;
    }

    public void setFluid(FluidStack newFluid) {
        this.fluid = (newFluid == null) ? FluidStack.EMPTY : newFluid;
        setChanged();
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", packInventory.serializeToTag());
        tag.put("Upgrades", upgradeInventory.serializeToTag());
        if (storedEU > 0) tag.putLong("StoredEU", storedEU);
        if (!fluid.isEmpty()) tag.put("Fluid", fluid.writeToNBT(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        packInventory.deserializeFromTag(tag.getList("Items", 10));
        upgradeInventory.deserializeFromTag(tag.getList("Upgrades", 10));
        storedEU = tag.getLong("StoredEU");
        fluid = tag.contains("Fluid")
                ? FluidStack.loadFluidStackFromNBT(tag.getCompound("Fluid"))
                : FluidStack.EMPTY;

    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCap.invalidate();
        fluidCap.invalidate();
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(getBlockPos()).inflate(1.0);
    }

    public FluidStack getFluid() {
        return this.fluid != null ? this.fluid : FluidStack.EMPTY;
    }

    public boolean isPickedUp() { return pickedUp; }
    public OmniPackTier getTier() { return tier; }
    public OmniPackInventory getPackInventory() { return packInventory; }
    public OmniPackInventory getUpgradeInventory() { return upgradeInventory; }
    public long getStoredEU() { return storedEU; }
}