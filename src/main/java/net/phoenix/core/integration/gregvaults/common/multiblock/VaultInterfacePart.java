package net.phoenix.core.integration.gregvaults.common.multiblock;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.ICapabilityTrait;
import com.gregtechceu.gtceu.api.machine.trait.MachineTrait;
import com.gregtechceu.gtceu.api.sync_system.annotations.RerenderOnChanged;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.common.mui.GTGuiTextures;
import com.gregtechceu.gtceu.common.mui.GTMuiWidgets;
import com.gregtechceu.gtceu.utils.ExtendedUseOnContext;
import com.gregtechceu.gtceu.utils.GTTransferUtils;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.EnumSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widgets.CycleButtonWidget;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VaultInterfacePart extends MultiblockPartMachine implements IMuiMachine {

    public enum ItemIoMode {

        DISABLED,
        INPUT,
        OUTPUT;

        public ItemIoMode next() {
            return switch (this) {
                case DISABLED -> INPUT;
                case INPUT -> OUTPUT;
                case OUTPUT -> DISABLED;
            };
        }

        public IO toCapabilityIO() {
            return switch (this) {
                case INPUT -> IO.IN;
                case OUTPUT -> IO.OUT;
                case DISABLED -> IO.NONE;
            };
        }

        public String displayName() {
            return switch (this) {
                case DISABLED -> "Disabled";
                case INPUT -> "Input";
                case OUTPUT -> "Output";
            };
        }
    }

    @Getter
    @SaveField
    @SyncToClient
    @RerenderOnChanged
    private boolean autoTransferItems = false;

    @Getter
    @SaveField
    @SyncToClient
    @RerenderOnChanged
    @Nullable
    private Direction itemFacing = null;

    @Getter
    @SaveField
    @SyncToClient
    @RerenderOnChanged
    private ItemIoMode itemIoMode = ItemIoMode.DISABLED;

    @Nullable
    private TickableSubscription autoTransferSubs;

    private final VaultItemHandlerTrait handlerTrait;

    public VaultInterfacePart(BlockEntityCreationInfo holder) {
        super(holder);
        this.handlerTrait = attachTrait(new VaultItemHandlerTrait());
    }

    @Override
    public boolean replacePartModelWhenFormed() {
        return isFormed();
    }

    @Override
    public void addedToController(MultiblockControllerMachine controller, String substructureName) {
        super.addedToController(controller, substructureName);
        handlerTrait.updateHandler();
        updateAutoTransferSubscription();
        notifyBlockUpdate();
    }

    @Override
    public void removedFromController(MultiblockControllerMachine controller) {
        super.removedFromController(controller);

        if (getControllers().isEmpty()) {
            handlerTrait.clearHandler();

            if (autoTransferSubs != null) {
                autoTransferSubs.unsubscribe();
                autoTransferSubs = null;
            }
        }

        notifyBlockUpdate();
    }

    private void updateAutoTransferSubscription() {
        if (autoTransferItems && itemFacing != null && itemIoMode != ItemIoMode.DISABLED && isFormed()) {
            autoTransferSubs = subscribeServerTick(autoTransferSubs, this::autoTransfer);
        } else if (autoTransferSubs != null) {
            autoTransferSubs.unsubscribe();
            autoTransferSubs = null;
        }
    }

    private void autoTransfer() {
        if (!isFormed() || !autoTransferItems || itemFacing == null || itemIoMode == ItemIoMode.DISABLED) return;
        if (getOffsetTimer() % 5 != 0) return;

        Level level = getLevel();
        VaultMachine vault = getVault();
        if (level == null || vault == null) return;

        GTTransferUtils.getAdjacentItemHandler(level, getBlockPos(), itemFacing).ifPresent(adjacent -> {
            vault.beginBatch();
            try {
                if (canInputItems()) {
                    GTTransferUtils.transferItemsFiltered(adjacent, vault.getItemHandler(), stack -> true);
                } else if (canOutputItems()) {
                    GTTransferUtils.transferItemsFiltered(vault.getItemHandler(), adjacent, stack -> true);
                }
            } finally {
                vault.endBatch();
            }
        });
    }

    public boolean canInputItems() {
        return itemIoMode == ItemIoMode.INPUT;
    }

    public boolean canOutputItems() {
        return itemIoMode == ItemIoMode.OUTPUT;
    }

    public void setAutoTransferItems(boolean enabled) {
        this.autoTransferItems = enabled;
        syncDataHolder.markClientSyncFieldDirty("autoTransferItems");
        updateAutoTransferSubscription();
        notifyBlockUpdate();
    }

    public void setItemFacing(@Nullable Direction facing) {
        this.itemFacing = facing;
        syncDataHolder.markClientSyncFieldDirty("itemFacing");
        updateAutoTransferSubscription();
        notifyBlockUpdate();
    }

    public void setItemIoMode(@NotNull ItemIoMode mode) {
        this.itemIoMode = mode;
        syncDataHolder.markClientSyncFieldDirty("itemIoMode");

        if (mode == ItemIoMode.DISABLED) {
            this.itemFacing = null;
            this.autoTransferItems = false;
            syncDataHolder.markClientSyncFieldDirty("itemFacing");
            syncDataHolder.markClientSyncFieldDirty("autoTransferItems");
        }

        updateAutoTransferSubscription();
        notifyBlockUpdate();
    }

    public void configureItemSide(@NotNull Direction side, @NotNull ItemIoMode mode) {
        this.itemFacing = mode == ItemIoMode.DISABLED ? null : side;
        this.itemIoMode = mode;
        syncDataHolder.markClientSyncFieldDirty("itemFacing");
        syncDataHolder.markClientSyncFieldDirty("itemIoMode");

        if (mode == ItemIoMode.DISABLED) {
            this.autoTransferItems = false;
            syncDataHolder.markClientSyncFieldDirty("autoTransferItems");
        }

        updateAutoTransferSubscription();
        notifyBlockUpdate();
    }

    public void refreshHandlerFromVault() {
        handlerTrait.updateHandler();
    }

    public void cycleItemMode(@NotNull Direction side) {
        if (itemFacing != side) {
            configureItemSide(side, ItemIoMode.INPUT);
        } else {
            configureItemSide(side, itemIoMode.next());
        }
    }

    @Override
    protected InteractionResult onScrewdriverClick(ExtendedUseOnContext context) {
        if (isRemote()) return InteractionResult.SUCCESS;

        Player player = context.getPlayer();
        Direction gridSide = context.getGridSide();

        if (player.isShiftKeyDown()) {
            if (itemFacing == gridSide && itemIoMode != ItemIoMode.DISABLED) {
                setAutoTransferItems(!autoTransferItems);
                player.displayClientMessage(Component.literal(
                        "Vault Interface Auto Transfer: " + (autoTransferItems ? "Enabled" : "Disabled")), true);
            } else {
                player.displayClientMessage(Component.literal("Select an item side before enabling auto transfer"),
                        true);
            }
        } else {
            cycleItemMode(gridSide);
            player.displayClientMessage(Component.literal(
                    itemIoMode == ItemIoMode.DISABLED ? "Vault Interface: disabled" :
                            "Vault Interface: " + itemIoMode.displayName() + " on " + gridSide.getName()),
                    true);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void buildMainUI(ParentWidget<?> mainWidget, PosGuiData guiData, PanelSyncManager syncManager,
                            UISettings settings) {
        mainWidget.child(Text.str("Vault Interface").asWidget().pos(10, 6));

        int startX = 10, startY = 22, spacing = 22;
        Direction[] sides = Direction.values();
        for (int i = 0; i < sides.length; i++) {
            Direction side = sides[i];
            int col = i % 3;
            int row = i / 3;

            EnumSyncValue<ItemIoMode> value = new EnumSyncValue<>(ItemIoMode.class,
                    () -> itemFacing == side ? itemIoMode : ItemIoMode.DISABLED,
                    mode -> configureItemSide(side, mode)).allowC2S();

            CycleButtonWidget button = new CycleButtonWidget()
                    .value(value)
                    .stateOverlay(ItemIoMode.DISABLED, IO.NONE.getUiTexture())
                    .stateOverlay(ItemIoMode.INPUT, IO.IN.getUiTexture())
                    .stateOverlay(ItemIoMode.OUTPUT, IO.OUT.getUiTexture())
                    .tooltipBuilder(r -> r.addLine(Component.literal(side.getName() + ": " +
                            (itemFacing == side ? itemIoMode.displayName() : "Disabled"))))
                    .pos(startX + col * spacing, startY + row * spacing)
                    .size(18, 18);

            mainWidget.child(button);
        }

        mainWidget.child(GTMuiWidgets.createToggleButton(
                this::isAutoTransferItems, this::setAutoTransferItems,
                GTGuiTextures.BUTTON_ITEM_OUTPUT, "gtceu.gui.item_auto_output")
                .pos(startX, startY + 2 * spacing + 6)
                .size(18, 18));
    }

    @Nullable
    private VaultMachine getVault() {
        return getControllers().stream()
                .filter(controller -> controller instanceof VaultMachine)
                .map(controller -> (VaultMachine) controller)
                .filter(VaultMachine::isFormed)
                .findFirst()
                .orElse(null);
    }

    public class VaultItemHandlerTrait extends MachineTrait implements ICapabilityTrait, IItemHandlerModifiable {

        @Nullable
        private ItemStackHandler delegate = null;

        public VaultItemHandlerTrait() {
            super();
            this.capabilityValidator = side -> delegate != null && isFormed() && itemFacing != null &&
                    itemIoMode != ItemIoMode.DISABLED && (side == null || side == itemFacing);
        }

        void updateHandler() {
            VaultMachine vault = getVault();
            delegate = vault != null ? vault.getItemHandler() : null;
        }

        void clearHandler() {
            delegate = null;
        }

        @Override
        public IO getCapabilityIO() {
            return itemIoMode.toCapabilityIO();
        }

        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            if (delegate != null && canInputItems()) {
                delegate.setStackInSlot(slot, stack);
            }
        }

        @Override
        public int getSlots() {
            return delegate != null ? delegate.getSlots() : 0;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return delegate != null ? delegate.getStackInSlot(slot) : ItemStack.EMPTY;
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return delegate != null && canInputItems() ? delegate.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return delegate != null && canOutputItems() ? delegate.extractItem(slot, amount, simulate) :
                    ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return delegate != null ? delegate.getSlotLimit(slot) : 0;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return delegate != null && canInputItems() && delegate.isItemValid(slot, stack);
        }
    }
}
