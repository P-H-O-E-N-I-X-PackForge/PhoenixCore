package net.phoenix.core.integration.gregvaults.common.multiblock;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.gregvaults.common.blocks.CoreTier;
import net.phoenix.core.integration.gregvaults.common.blocks.VaultCoreBlock;
import net.phoenix.core.integration.gregvaults.mui.VaultMachineAccess;
import net.phoenix.core.integration.gregvaults.mui.VaultPanel;
import net.phoenix.core.integration.gregvaults.mui.VaultSession;
import net.phoenix.core.util.StackNbt;

import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.PanelSyncManager;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class VaultMachine extends MultiblockControllerMachine implements IMuiMachine {

    public enum VaultTier {

        BRONZE,
        STEEL,
        TITANIUM;

        public CoreTier maxCoreTier() {
            return switch (this) {
                case BRONZE -> CoreTier.MK1;
                case STEEL -> CoreTier.MK2;
                case TITANIUM -> CoreTier.MK3;
            };
        }

        public int baseSlots() {
            return switch (this) {
                case BRONZE -> PhoenixConfigs.INSTANCE.vaultValues.bronzeVault.bronzeBaseSlots;
                case STEEL -> PhoenixConfigs.INSTANCE.vaultValues.steelVault.steelBaseSlots;
                case TITANIUM -> PhoenixConfigs.INSTANCE.vaultValues.titaniumVault.titaniumBaseSlots;
            };
        }

        public boolean wirelessAllowed() {
            return switch (this) {
                case BRONZE -> PhoenixConfigs.INSTANCE.vaultValues.bronzeVault.bronzeWireless;
                case STEEL -> PhoenixConfigs.INSTANCE.vaultValues.steelVault.steelWireless;
                case TITANIUM -> PhoenixConfigs.INSTANCE.vaultValues.titaniumVault.titaniumWireless;
            };
        }
    }

    @Getter
    private final VaultTier vaultTier;

    @Getter
    @SaveField
    @SyncToClient
    private int totalSlots = 0;

    @Getter
    private ItemStackHandler itemHandler;

    @Getter
    private ItemStack[] savedCraftingGrid = new ItemStack[9];

    private int contentVersion = 0;

    private final Set<ServerPlayer> activeViewers = new HashSet<>();

    public VaultMachine(BlockEntityCreationInfo holder, VaultTier vaultTier) {
        super(holder);
        this.vaultTier = vaultTier;
        this.itemHandler = createHandler(0);
        Arrays.fill(this.savedCraftingGrid, ItemStack.EMPTY);
    }

    private ItemStackHandler createHandler(int size) {
        return new ItemStackHandler(size) {

            @Override
            protected void onContentsChanged(int slot) {
                contentVersion++;
                markAsChanged();
            }
        };
    }

    public void setSavedCraftingGrid(ItemStack[] grid) {
        this.savedCraftingGrid = grid;
        markAsChanged();
    }

    public int contentVersion() {
        return contentVersion;
    }

    public void addViewer(ServerPlayer player) {
        activeViewers.add(player);
    }

    public void removeViewer(ServerPlayer player) {
        activeViewers.remove(player);
    }

    public int getAvailableSlots() {
        int empty = 0;
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (itemHandler.getStackInSlot(i).isEmpty()) empty++;
        }
        return empty;
    }

    @Override
    public void formStructure(@NotNull String substructureName) {
        super.formStructure(substructureName);
        int newSlots = countSlots();
        totalSlots = newSlots;

        if (itemHandler.getSlots() != newSlots) {
            kickPlayersAndResize(newSlots);
        }

        for (MultiblockPartMachine part : getParts()) {
            if (part instanceof VaultInterfacePart iface) {
                iface.refreshHandlerFromVault();
            }
        }
    }

    @Override
    public void invalidateStructure(@NotNull String substructureName) {
        super.invalidateStructure(substructureName);
        kickPlayers();
        totalSlots = 0;
    }

    private void kickPlayers() {
        for (ServerPlayer sp : new ArrayList<>(activeViewers)) {
            sp.closeContainer();
        }
        activeViewers.clear();
    }

    private void kickPlayersAndResize(int newSize) {
        kickPlayers();
        resizeHandler(newSize);
    }

    private void resizeHandler(int newSize) {
        if (newSize < itemHandler.getSlots() && getLevel() instanceof ServerLevel serverLevel) {
            BlockPos pos = getBlockPos();
            for (int i = newSize; i < itemHandler.getSlots(); i++) {
                ItemStack overflow = itemHandler.getStackInSlot(i);
                if (overflow.isEmpty()) continue;
                while (!overflow.isEmpty()) {
                    int take = Math.min(overflow.getMaxStackSize(), overflow.getCount());
                    Block.popResource(serverLevel, pos, overflow.copyWithCount(take));
                    overflow.shrink(take);
                }
            }
        }

        ItemStackHandler newHandler = createHandler(newSize);
        int copyCount = Math.min(itemHandler.getSlots(), newSize);
        for (int i = 0; i < copyCount; i++) {
            newHandler.setStackInSlot(i, itemHandler.getStackInSlot(i));
        }
        itemHandler = newHandler;
        activeViewers.clear();
        markAsChanged();
    }

    @Override
    public boolean shouldOpenUI(Player player, net.minecraft.world.InteractionHand hand,
                                net.minecraft.world.phys.BlockHitResult hit) {
        return isFormed();
    }

    @Override
    public ModularPanel<?> buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        VaultSession session = new VaultSession(data.getPlayer(), new VaultMachineAccess(this, false));
        return VaultPanel.build(session, syncManager, settings, -1);
    }

    private void serializeItemsToTag(CompoundTag tag) {
        ListTag list = new ListTag();
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putInt("Slot", i);
            StackNbt.saveInto(stack, entry);
            list.add(entry);
        }
        tag.put("VaultSlots", list);

        ListTag craftList = new ListTag();
        for (int i = 0; i < savedCraftingGrid.length; i++) {
            ItemStack s = savedCraftingGrid[i];
            if (s == null || s.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putInt("Slot", i);
            StackNbt.saveInto(s, entry);
            craftList.add(entry);
        }
        tag.put("CraftingGrid", craftList);
    }

    private void deserializeItemsFromTag(CompoundTag tag) {
        if (tag.contains("VaultSlots", Tag.TAG_LIST)) {
            ListTag list = tag.getList("VaultSlots", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                int slot = entry.getInt("Slot");
                if (slot >= 0 && slot < itemHandler.getSlots()) {
                    itemHandler.setStackInSlot(slot, StackNbt.loadStack(entry));
                }
            }
        }

        Arrays.fill(savedCraftingGrid, ItemStack.EMPTY);
        if (tag.contains("CraftingGrid", Tag.TAG_LIST)) {
            ListTag craftList = tag.getList("CraftingGrid", Tag.TAG_COMPOUND);
            for (int i = 0; i < craftList.size(); i++) {
                CompoundTag entry = craftList.getCompound(i);
                int slot = entry.getInt("Slot");
                if (slot >= 0 && slot < savedCraftingGrid.length) {
                    savedCraftingGrid[slot] = StackNbt.loadStack(entry);
                }
            }
        }
    }

    @Override
    public void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        CompoundTag tag = new CompoundTag();
        serializeItemsToTag(tag);
        components.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(tag));
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);
        var data = componentInput.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (data != null) {
            itemHandler = createHandler(Math.max(totalSlots, 0));
            deserializeItemsFromTag(data.copyTag());
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (totalSlots > 0 && itemHandler.getSlots() != totalSlots) {
            itemHandler = createHandler(totalSlots);
        }
    }

    @Override
    public void modifyDrops(List<ItemStack> drops) {
        dropInventoryContents(drops);
    }

    private void dropInventoryContents(List<ItemStack> drops) {
        if (itemHandler == null || itemHandler.getSlots() <= 0) return;

        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            ItemStack stack = itemHandler.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            while (!stack.isEmpty()) {
                int take = Math.min(stack.getMaxStackSize(), stack.getCount());
                drops.add(stack.copyWithCount(take));
                stack.shrink(take);
            }
            itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        }
        markAsChanged();
    }

    private int countSlots() {
        if (getLevel() == null) return vaultTier.baseSlots();

        Direction facing = getFrontFacing();
        Direction upward = getUpwardsFacing();
        boolean flipped = isFlipped();

        Direction back = RelativeDirection.BACK.getRelativeFacing(facing, upward, flipped);
        Direction right = RelativeDirection.RIGHT.getRelativeFacing(facing, upward, flipped);
        Direction up = RelativeDirection.UP.getRelativeFacing(facing, upward, flipped);
        Direction left = right.getOpposite();
        Direction down = up.getOpposite();

        BlockPos origin = getBlockPos();
        int slots = vaultTier.baseSlots();

        for (int d = 1; d <= 3; d++) {
            for (int h = -1; h <= 1; h++) {
                for (int w = -1; w <= 1; w++) {
                    BlockPos p = origin
                            .relative(back, d)
                            .relative(h >= 0 ? up : down, Math.abs(h))
                            .relative(w >= 0 ? right : left, Math.abs(w));

                    BlockState s = getLevel().getBlockState(p);
                    if (s.getBlock() instanceof VaultCoreBlock core) {
                        CoreTier coreTier = core.getTier();
                        if (coreTier.level <= vaultTier.maxCoreTier().level) {
                            slots += PhoenixConfigs.getSlotValue(coreTier);
                        }
                    }
                }
            }
        }
        return slots;
    }
}
