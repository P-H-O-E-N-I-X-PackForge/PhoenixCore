package net.phoenix.core.integration.gregvaults.common.multiblock;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.utils.ExtendedUseOnContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;

import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.gregvaults.client.screen.VaultContainerMenu;
import net.phoenix.core.integration.gregvaults.common.blocks.CoreTier;
import net.phoenix.core.integration.gregvaults.common.blocks.VaultCoreBlock;
import net.phoenix.core.integration.gregvaults.network.SPacketVaultContents;
import net.phoenix.core.integration.gregvaults.network.SPacketVaultDelta;
import net.phoenix.core.integration.gregvaults.network.VaultNetwork;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class VaultMachine extends MultiblockControllerMachine {

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

    public enum BatchSyncMode {
        AUTO,
        DELTA_ONLY,
        FULL_SNAPSHOT
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

    private int batchDepth = 0;
    private BatchSyncMode batchSyncMode = BatchSyncMode.AUTO;
    private final Set<Integer> dirtySlots = new HashSet<>();

    private final Set<ServerPlayer> activeViewers = new HashSet<>();
    private final Map<UUID, ItemStack[]> lastSentByViewer = new HashMap<>();

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
                markAsChanged();
                if (batchDepth > 0) {
                    dirtySlots.add(slot);
                } else {
                    notifySlotChanged(slot);
                }
            }
        };
    }

    public void setSavedCraftingGrid(ItemStack[] grid) {
        this.savedCraftingGrid = grid;
        markAsChanged();
    }

    private ItemStack[] getLastSentItems(ServerPlayer viewer) {
        return lastSentByViewer.computeIfAbsent(viewer.getUUID(), k -> {
            ItemStack[] arr = new ItemStack[itemHandler.getSlots()];
            Arrays.fill(arr, ItemStack.EMPTY);
            return arr;
        });
    }

    public void addViewer(ServerPlayer player) {
        activeViewers.add(player);
    }

    public void removeViewer(ServerPlayer player) {
        activeViewers.remove(player);
        lastSentByViewer.remove(player.getUUID());
    }

    public void beginBatch() {
        beginBatch(BatchSyncMode.AUTO);
    }

    public void beginBatch(BatchSyncMode mode) {
        BatchSyncMode requestedMode = mode == null ? BatchSyncMode.AUTO : mode;
        if (batchDepth == 0) {
            dirtySlots.clear();
            batchSyncMode = requestedMode;
        } else if (requestedMode == BatchSyncMode.FULL_SNAPSHOT) {
            batchSyncMode = BatchSyncMode.FULL_SNAPSHOT;
        } else if (requestedMode == BatchSyncMode.DELTA_ONLY && batchSyncMode != BatchSyncMode.FULL_SNAPSHOT) {
            batchSyncMode = BatchSyncMode.DELTA_ONLY;
        }
        batchDepth++;
    }

    public void endBatch() {
        if (batchDepth <= 0) {
            batchDepth = 0;
            batchSyncMode = BatchSyncMode.AUTO;
            dirtySlots.clear();
            return;
        }

        batchDepth--;
        if (batchDepth > 0) return;

        if (dirtySlots.isEmpty() || activeViewers.isEmpty()) {
            dirtySlots.clear();
            batchSyncMode = BatchSyncMode.AUTO;
            return;
        }

        if (batchSyncMode == BatchSyncMode.FULL_SNAPSHOT || dirtySlots.size() > 256) {
            sendFullSnapshot(new ArrayList<>(activeViewers));
        } else {
            sendDirtySlotDeltas();
        }
        dirtySlots.clear();
        batchSyncMode = BatchSyncMode.AUTO;
    }

    private void sendDirtySlotDeltas() {
        for (ServerPlayer sp : activeViewers) {
            SPacketVaultDelta.Builder builder = new SPacketVaultDelta.Builder(sp.containerMenu.containerId);
            for (int slot : dirtySlots) {
                buildDeltaEntry(builder, slot, sp);
            }
            if (!builder.isEmpty()) {
                VaultNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), builder.build());
            }
        }
    }

    private void notifySlotChanged(int slot) {
        if (activeViewers.isEmpty()) return;
        for (ServerPlayer sp : activeViewers) {
            SPacketVaultDelta.Builder builder = new SPacketVaultDelta.Builder(sp.containerMenu.containerId);
            buildDeltaEntry(builder, slot, sp);
            if (!builder.isEmpty()) {
                VaultNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), builder.build());
            }
        }
    }

    private void buildDeltaEntry(SPacketVaultDelta.Builder builder, int slot, ServerPlayer viewer) {
        ItemStack current = itemHandler.getStackInSlot(slot);
        ItemStack[] sent = getLastSentItems(viewer);
        ItemStack last = (slot < sent.length) ? sent[slot] : ItemStack.EMPTY;

        if (current.isEmpty()) {
            if (!last.isEmpty()) {
                builder.addRemoved(slot);
                sent[slot] = ItemStack.EMPTY;
            }
        } else if (!last.isEmpty() && ItemStack.isSameItemSameTags(current, last)) {
            if (current.getCount() != last.getCount()) {
                builder.addCountOnly(slot, current.getCount());
                sent[slot] = current.copy();
            }
        } else {
            builder.addFull(slot, current.copy());
            sent[slot] = current.copy();
        }
    }

    private void sendFullSnapshot(List<ServerPlayer> watching) {
        ItemStack[] stacks = new ItemStack[itemHandler.getSlots()];
        for (int i = 0; i < stacks.length; i++) {
            stacks[i] = itemHandler.getStackInSlot(i).copy();
        }
        for (ServerPlayer sp : watching) {
            VaultNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> sp),
                    new SPacketVaultContents(sp.containerMenu.containerId, stacks));
            ItemStack[] sent = getLastSentItems(sp);
            for (int i = 0; i < stacks.length && i < sent.length; i++) {
                sent[i] = stacks[i].copy();
            }
        }
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
        lastSentByViewer.clear();
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
        lastSentByViewer.clear();
        markAsChanged();
    }

    @Override
    public InteractionResult onUse(ExtendedUseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (!isFormed()) return InteractionResult.PASS;
            final int[] windowIdHolder = { -1 };
            MenuProvider provider = new SimpleMenuProvider(
                    (windowId, playerInv, p) -> {
                        windowIdHolder[0] = windowId;
                        VaultContainerMenu menu = new VaultContainerMenu(windowId, playerInv, itemHandler, VaultMachine.this);
                        menu.initCraftingGrid(savedCraftingGrid);
                        menu.setOnGridClose(grid -> {
                            savedCraftingGrid = grid;
                            markAsChanged();
                        });
                        return menu;
                    },
                    Component.translatable("gui.gregvaults.vault"));
            NetworkHooks.openScreen(serverPlayer, provider, buf -> buf.writeInt(totalSlots));
            if (windowIdHolder[0] >= 0) {
                sendFullContents(serverPlayer, windowIdHolder[0]);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public void sendFullContents(ServerPlayer player, int containerId) {
        ItemStack[] stacks = new ItemStack[itemHandler.getSlots()];
        for (int i = 0; i < stacks.length; i++) {
            stacks[i] = itemHandler.getStackInSlot(i).copy();
        }
        VaultNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SPacketVaultContents(containerId, stacks));
        ItemStack[] sent = new ItemStack[stacks.length];
        for (int i = 0; i < stacks.length; i++) {
            sent[i] = stacks[i].copy();
        }
        lastSentByViewer.put(player.getUUID(), sent);
        activeViewers.add(player);
    }

    private void serializeItemsToTag(CompoundTag tag) {
        ListTag list = new ListTag();
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putInt("Slot", i);
            stack.save(entry);
            list.add(entry);
        }
        tag.put("VaultSlots", list);

        ListTag craftList = new ListTag();
        for (int i = 0; i < savedCraftingGrid.length; i++) {
            ItemStack s = savedCraftingGrid[i];
            if (s == null || s.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putInt("Slot", i);
            s.save(entry);
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
                    itemHandler.setStackInSlot(slot, ItemStack.of(entry));
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
                    savedCraftingGrid[slot] = ItemStack.of(entry);
                }
            }
        }
    }

    @Override
    public void saveToItem(CompoundTag tag, boolean isPickBlock) {
        super.saveToItem(tag, isPickBlock);
        serializeItemsToTag(tag);
    }

    @Override
    public void loadFromItem(CompoundTag tag) {
        super.loadFromItem(tag);
        itemHandler = createHandler(Math.max(totalSlots, 0));
        deserializeItemsFromTag(tag);
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
