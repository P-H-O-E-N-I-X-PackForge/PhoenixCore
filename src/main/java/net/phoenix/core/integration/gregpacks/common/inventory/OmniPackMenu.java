package net.phoenix.core.integration.gregpacks.common.inventory;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.gregpacks.common.OmniPackTickHandler;
import net.phoenix.core.integration.gregpacks.common.block.OmniPackBlockEntity;
import net.phoenix.core.integration.gregpacks.common.fluid.FluidNBTHelper;
import net.phoenix.core.integration.gregpacks.common.item.OmniPackTier;
import net.phoenix.core.integration.gregpacks.common.item.UpgradeItem;
import net.phoenix.core.integration.gregpacks.common.item.UpgradeType;
import net.phoenix.core.integration.gregpacks.common.upgrade.UpgradeEffects;

import lombok.Setter;

import javax.annotation.Nullable;

public class OmniPackMenu extends AbstractContainerMenu {

    public static final int SLOT_SIZE = 18;
    public static final int PACK_START_X = 8;
    public static final int PACK_START_Y = 22;
    public static final int COLS = 9;
    private final int packSlots;
    private final int maxUpgrades;
    private int packSlotIndex = -1;
    private final OmniPackInventory packInventory;
    private final OmniPackInventory upgradeInventory;
    private final OmniPackTier tier;
    private final ContainerData fluidData = new SimpleContainerData(4);
    private final ContainerData energyData = new SimpleContainerData(4);

    private ServerPlayer serverPlayer = null;
    private OmniPackBlockEntity sourceBlockEntity = null;

    @Setter
    private String syncedFluidKey = "";

    public OmniPackMenu(int containerId, Inventory playerInventory,
                        OmniPackInventory packInventory, OmniPackInventory upgradeInventory,
                        OmniPackTier tier) {
        super(GregPacksMenus.OMNIPACK_MENU.get(), containerId);
        this.tier = tier;
        this.upgradeInventory = upgradeInventory;
        this.maxUpgrades = upgradeInventory.getContainerSize();
        this.packSlots = packInventory.getContainerSize();
        this.packInventory = packInventory;

        addPackSlots();
        addUpgradeSlots();
        addPlayerSlots(playerInventory);
        addDataSlots(fluidData);
        addDataSlots(energyData);
        syncFluidData();
    }

    public OmniPackMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory,
                new OmniPackInventory(buf.readShort()),
                new OmniPackInventory(buf.readByte()),
                OmniPackTier.values()[buf.readByte()]);
    }

    private void addPackSlots() {
        for (int i = 0; i < packSlots; i++) {
            int col = i % COLS;
            int row = i / COLS;
            addSlot(new HideableSlot(packInventory, i,
                    PACK_START_X + col * SLOT_SIZE,
                    PACK_START_Y + row * SLOT_SIZE));
        }
    }

    private void addUpgradeSlots() {
        final OmniPackInventory upgradeInvRef = upgradeInventory;
        for (int i = 0; i < maxUpgrades; i++) {
            final int slotIdx = i;
            addSlot(new HideableSlot(upgradeInventory, i, 0, 0) {

                @Override
                public boolean mayPlace(ItemStack stack) {
                    if (!(stack.getItem() instanceof UpgradeItem upgradeItem)) return false;
                    UpgradeType incoming = upgradeItem.getUpgradeType();
                    for (int j = 0; j < maxUpgrades; j++) {
                        if (j == slotIdx) continue;
                        ItemStack other = upgradeInvRef.getItem(j);
                        if (!other.isEmpty() && other.getItem() instanceof UpgradeItem otherUpgrade &&
                                otherUpgrade.getUpgradeType() == incoming) {
                            return false;
                        }
                    }
                    return true;
                }

                @Override
                public boolean mayPickup(Player player) {
                    ItemStack current = upgradeInvRef.getItem(slotIdx);
                    if (current.isEmpty() || !(current.getItem() instanceof UpgradeItem upgradeItem))
                        return true;

                    PhoenixConfigs.UpgradeConfigs cfg = PhoenixConfigs.INSTANCE.ModuleValues;

                    int thisBonus = switch (upgradeItem.getUpgradeType()) {
                        case ITEM_CAPACITY_I -> cfg.itemModule1Bonus;
                        case ITEM_CAPACITY_II -> cfg.itemModule2Bonus;
                        case ITEM_CAPACITY_III -> cfg.itemModule3Bonus;
                        default -> 0;
                    };
                    if (thisBonus == 0) return true;
                    int slotsWithout = tier.defaultSlots;
                    for (int j = 0; j < maxUpgrades; j++) {
                        if (j == slotIdx) continue;
                        ItemStack other = upgradeInvRef.getItem(j);
                        if (other.isEmpty() || !(other.getItem() instanceof UpgradeItem otherUpgrade))
                            continue;
                        slotsWithout += switch (otherUpgrade.getUpgradeType()) {
                            case ITEM_CAPACITY_I -> cfg.itemModule1Bonus;
                            case ITEM_CAPACITY_II -> cfg.itemModule2Bonus;
                            case ITEM_CAPACITY_III -> cfg.itemModule3Bonus;
                            default -> 0;
                        };
                    }

                    for (int j = slotsWithout; j < packSlots; j++) {
                        if (!OmniPackMenu.this.packInventory.getItem(j).isEmpty())
                            return false;
                    }
                    return true;
                }
            });
        }
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);

        if (serverPlayer == null) return;
        if (container != upgradeInventory) return;

        int newSlotCount = computeCurrentSlotCount();
        if (newSlotCount == packSlots) return;

        int slot = packSlotIndex;
        if (slot < 0) return;

        ItemStack packStack = sourceStack != null ? sourceStack :
                (packSlotIndex >= 0 ? serverPlayer.getInventory().getItem(packSlotIndex) : ItemStack.EMPTY);
        if (packStack.isEmpty()) return;

        packInventory.saveToItem(packStack);
        upgradeInventory.saveUpgradesToItem(packStack);
        serverPlayer.getInventory().setChanged();

        serverPlayer.getServer().execute(() -> OpenPackHelper.open(serverPlayer, packStack, tier, packSlotIndex));
    }

    private int computeCurrentSlotCount() {
        PhoenixConfigs.UpgradeConfigs cfg = PhoenixConfigs.INSTANCE.ModuleValues;
        int total = tier.defaultSlots;
        for (int i = 0; i < maxUpgrades; i++) {
            ItemStack stack = upgradeInventory.getItem(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof UpgradeItem upgradeItem)) continue;
            total += switch (upgradeItem.getUpgradeType()) {
                case ITEM_CAPACITY_I -> cfg.itemModule1Bonus;
                case ITEM_CAPACITY_II -> cfg.itemModule2Bonus;
                case ITEM_CAPACITY_III -> cfg.itemModule3Bonus;
                default -> 0;
            };
        }
        return total;
    }

    private void addPlayerSlots(Inventory playerInventory) {
        int packRows = (int) Math.ceil(packSlots / (double) COLS);
        int playerInvY = PACK_START_Y + packRows * SLOT_SIZE + 8;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new HideableSlot(playerInventory,
                        col + row * 9 + 9,
                        PACK_START_X + col * SLOT_SIZE,
                        playerInvY + row * SLOT_SIZE));
            }
        }
        int hotbarY = playerInvY + 3 * SLOT_SIZE + 4;
        for (int col = 0; col < 9; col++) {
            addSlot(new HideableSlot(playerInventory, col,
                    PACK_START_X + col * SLOT_SIZE, hotbarY));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            int upgradeStart = packSlots;
            int upgradeEnd = packSlots + maxUpgrades;
            int playerEnd = slots.size();

            if (index < upgradeStart) {
                if (!moveItemStackTo(slotStack, upgradeEnd, playerEnd, true))
                    return ItemStack.EMPTY;
            } else if (index < upgradeEnd) {
                if (!moveItemStackTo(slotStack, upgradeEnd, playerEnd, true))
                    return ItemStack.EMPTY;
            } else {
                if (slotStack.getItem() instanceof UpgradeItem) {
                    if (!moveItemStackTo(slotStack, upgradeStart, upgradeEnd, false))
                        return ItemStack.EMPTY;
                } else {
                    if (!moveItemStackTo(slotStack, 0, upgradeStart, false))
                        return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    private String lastSentFluidKey = null;

    private void syncFluidData() {
        if (serverPlayer == null) return;

        FluidStack fluid;
        if (sourceBlockEntity != null) {
            fluid = sourceBlockEntity.getFluid();
        } else {
            ItemStack packStack = getActiveStack();
            if (packStack.isEmpty()) return;
            fluid = FluidNBTHelper.getFluid(packStack);
        }

        int amount = fluid.getAmount();
        int capacity = getTankCapacity();

        fluidData.set(0, (amount >> 16) & 0xFFFF);
        fluidData.set(1, amount & 0xFFFF);
        fluidData.set(2, (capacity >> 16) & 0xFFFF);
        fluidData.set(3, capacity & 0xFFFF);

        String key = fluid.isEmpty() ? "" :
                ForgeRegistries.FLUIDS
                        .getKey(fluid.getFluid()).toString();
        if (!key.equals(lastSentFluidKey)) {
            lastSentFluidKey = key;
            net.phoenix.core.integration.gregpacks.network.GregPacksNetwork.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new net.phoenix.core.integration.gregpacks.network.SPacketFluidSync(key));
        }
    }

    @Nullable
    public IFluidHandler getFluidHandler() {
        if (sourceBlockEntity != null) {
            return sourceBlockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER).orElse(null);
        }
        ItemStack stack = getActiveStack();
        return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
    }

    private void syncEnergyData() {
        if (serverPlayer == null) return;

        long storedEU;
        long maxEU;

        if (sourceBlockEntity != null) {
            storedEU = sourceBlockEntity.getStoredEU();
            maxEU = new UpgradeEffects(tier, sourceBlockEntity.getUpgradeInventory()).totalEnergyStorage;
        } else {
            ItemStack packStack = getActiveStack();
            if (packStack.isEmpty()) return;
            storedEU = OmniPackTickHandler.getStoredEU(packStack);
            maxEU = new UpgradeEffects(tier, upgradeInventory).totalEnergyStorage;
        }

        int euScaled = (int) Math.min(storedEU / 100, Integer.MAX_VALUE);
        int maxScaled = (int) Math.min(maxEU / 100, Integer.MAX_VALUE);

        energyData.set(0, (euScaled >> 16) & 0xFFFF);
        energyData.set(1, euScaled & 0xFFFF);
        energyData.set(2, (maxScaled >> 16) & 0xFFFF);
        energyData.set(3, maxScaled & 0xFFFF);
    }

    public ItemStack getActiveStack() {
        if (sourceStack != null && !sourceStack.isEmpty()) return sourceStack;
        if (packSlotIndex >= 0) return serverPlayer.getInventory().getItem(packSlotIndex);
        return ItemStack.EMPTY;
    }

    public long getActiveStoredEU() {
        if (sourceBlockEntity != null) return sourceBlockEntity.getStoredEU();
        ItemStack stack = getActiveStack();
        return stack.isEmpty() ? 0L : OmniPackTickHandler.getStoredEU(stack);
    }

    public int getTankCapacity() {
        UpgradeEffects effects = new UpgradeEffects(tier, upgradeInventory);
        return effects.totalFluidStorage;
    }

    public int getSyncedFluidAmount() {
        return (fluidData.get(0) << 16) | (fluidData.get(1) & 0xFFFF);
    }

    public int getSyncedFluidCapacity() {
        return (fluidData.get(2) << 16) | (fluidData.get(3) & 0xFFFF);
    }

    public long getSyncedEU() {
        return ((long) ((energyData.get(0) << 16) | (energyData.get(1) & 0xFFFF))) * 100L;
    }

    public long getSyncedMaxEU() {
        return ((long) ((energyData.get(2) << 16) | (energyData.get(3) & 0xFFFF))) * 100L;
    }

    public Fluid getSyncedFluid() {
        if (syncedFluidKey.isEmpty()) return null;
        return ForgeRegistries.FLUIDS.getValue(new ResourceLocation(syncedFluidKey));
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void broadcastChanges() {
        syncFluidData();
        syncEnergyData();
        super.broadcastChanges();
    }

    public void setServerPlayer(ServerPlayer player) {
        this.serverPlayer = player;
    }

    private ItemStack sourceStack = null;

    public void setSourceStack(ItemStack stack) {
        this.sourceStack = stack;
    }

    public ItemStack getSourceStack() {
        return sourceStack;
    }

    public void setSourceBlockEntity(OmniPackBlockEntity be) {
        this.sourceBlockEntity = be;
    }

    public OmniPackBlockEntity getSourceBlockEntity() {
        return sourceBlockEntity;
    }

    public OmniPackInventory getPackInventory() {
        return packInventory;
    }

    public OmniPackInventory getUpgradeInventory() {
        return upgradeInventory;
    }

    public OmniPackTier getTier() {
        return tier;
    }

    public int getPackSlots() {
        return packSlots;
    }

    public int getMaxUpgrades() {
        return maxUpgrades;
    }

    public int getPackSlotIndex() {
        return packSlotIndex;
    }

    public void setPackSlotIndex(int index) {
        this.packSlotIndex = index;
    }

    public HideableSlot getHideableSlot(int index) {
        return (HideableSlot) slots.get(index);
    }

    public Container getContainer() {
        return this.packInventory;
    }
}
