package net.phoenix.core.integration.ae2;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.common.mui.GTGuiTextures;
import com.gregtechceu.gtceu.integration.ae2.machine.MEPatternBufferPartMachine;
import com.gregtechceu.gtceu.utils.ExtendedUseOnContext;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.mixin.ae2.MEPatternBufferResizeAccessor;

import appeng.api.crafting.IPatternDetails;
import appeng.api.inventories.InternalInventory;
import appeng.crafting.pattern.EncodedPatternItem;
import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.BooleanSyncValue;
import brachy.modularui.value.sync.IntSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.value.sync.SyncHandlers;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widgets.ButtonWidget;
import brachy.modularui.widgets.layout.Flow;
import brachy.modularui.widgets.layout.Grid;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.slot.SlotGroup;

public class ExpandedPatternBufferPartMachine extends MEPatternBufferPartMachine {

    public static final int SLOTS = 72;
    private static final int COLUMNS = 9;
    private static final int TANK_CAPACITY = 32_000;

    public enum ComparatorMode {

        FILLED,
        BUSY,
        STORED,
        ONLINE;

        public ComparatorMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    @SaveField
    @SyncToClient
    private int comparatorMode = 0;

    public ExpandedPatternBufferPartMachine(BlockEntityCreationInfo info) {
        super(info);
        var resize = (MEPatternBufferResizeAccessor) this;

        getPatternInventory().setSize(SLOTS);

        InternalSlot[] built = this.internalInventory;
        InternalSlot[] slots = new InternalSlot[SLOTS];
        for (int i = 0; i < SLOTS; i++) {
            if (i < built.length) {
                slots[i] = built[i];
            } else {
                slots[i] = new InternalSlot();
                slots[i].setOnContentsChanged(resize::phoenix$onSlotChanged);
            }
        }
        resize.phoenix$setInternalInventory(slots);
        resize.phoenix$setPatternSlotDetails(new IPatternDetails[SLOTS]);
        resize.phoenix$setInternalPatternInventory(new InternalInventory() {

            @Override
            public int size() {
                return SLOTS;
            }

            @Override
            public ItemStack getStackInSlot(int slotIndex) {
                return getPatternInventory().getStackInSlot(slotIndex);
            }

            @Override
            public void setItemDirect(int slotIndex, ItemStack stack) {
                getPatternInventory().setStackInSlot(slotIndex, stack);
                getPatternInventory().onContentsChanged(slotIndex);
                onPatternChange(slotIndex);
            }
        });

        NotifiableFluidTank tank = getShareTank();
        for (int i = 0; i < tank.getTanks(); i++) {
            ((com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank) tank.getStorages()[i])
                    .setCapacity(TANK_CAPACITY);
        }
    }

    public ComparatorMode getComparatorMode() {
        return ComparatorMode.values()[Math.floorMod(comparatorMode, ComparatorMode.values().length)];
    }

    private void cycleComparatorMode() {
        comparatorMode = getComparatorMode().next().ordinal();
        updateComparator();
    }

    private void updateComparator() {
        if (getLevel() != null && !getLevel().isClientSide()) {
            getLevel().updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
        }
    }

    @Override
    public int getAnalogOutputSignal() {
        switch (getComparatorMode()) {
            case BUSY -> {
                int busy = 0;
                for (InternalSlot slot : this.internalInventory) {
                    if (slot != null && !slot.isEmpty()) busy++;
                }
                return Math.min(15, busy);
            }
            case STORED -> {
                return canRefund() ? 15 : 0;
            }
            case ONLINE -> {
                return this.isOnline ? 15 : 0;
            }
            default -> {
                CustomItemStackHandler patterns = getPatternInventory();
                int filled = 0;
                for (int i = 0; i < patterns.getSlots(); i++) {
                    if (!patterns.getStackInSlot(i).isEmpty()) filled++;
                }
                return filled == 0 ? 0 : 1 + (filled * 14) / patterns.getSlots();
            }
        }
    }

    @Override
    protected InteractionResult onScrewdriverClick(ExtendedUseOnContext context) {
        var player = context.getPlayer();
        if (player != null && player.isCrouching()) {
            if (!isRemote()) {
                cycleComparatorMode();
                player.displayClientMessage(Component.translatable("gui.phoenixcore.expanded_buffer.comparator",
                        Component.translatable("gui.phoenixcore.expanded_buffer.comparator." +
                                getComparatorMode().name().toLowerCase())),
                        true);
            }
            return InteractionResult.sidedSuccess(isRemote());
        }
        return super.onScrewdriverClick(context);
    }

    public boolean sortPatterns() {
        if (isRemote() || isBusy()) return false;
        CustomItemStackHandler patterns = getPatternInventory();
        java.util.List<ItemStack> found = new java.util.ArrayList<>();
        for (int i = 0; i < patterns.getSlots(); i++) {
            ItemStack stack = patterns.getStackInSlot(i);
            if (!stack.isEmpty()) found.add(stack.copy());
        }
        found.sort(java.util.Comparator.comparing(this::sortKey));
        for (int i = 0; i < patterns.getSlots(); i++) {
            patterns.setStackInSlot(i, i < found.size() ? found.get(i) : ItemStack.EMPTY);
        }
        for (int i = 0; i < patterns.getSlots(); i++) {
            patterns.onContentsChanged(i);
            onPatternChange(i);
        }
        return true;
    }

    private String sortKey(ItemStack pattern) {
        if (pattern.getItem() instanceof EncodedPatternItem encoded) {
            ItemStack out = encoded.getOutput(pattern);
            if (!out.isEmpty()) return out.getHoverName().getString().toLowerCase();
        }
        return pattern.getHoverName().getString().toLowerCase();
    }

    private static final long WANT_TICKS = 100;

    private final java.util.Map<Integer, java.util.Map<ExpandedPatternBufferProxyPartMachine, Long>> wanted = new java.util.HashMap<>();

    public int indexOf(InternalSlot slot) {
        InternalSlot[] slots = this.internalInventory;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) return i;
        }
        return -1;
    }

    public boolean mayUse(ExpandedPatternBufferProxyPartMachine proxy, int slot) {
        if (!proxy.allows(slot)) return false;
        var demand = wanted.get(slot);
        if (demand == null || getLevel() == null) return true;
        long now = getLevel().getGameTime();
        for (var entry : demand.entrySet()) {
            var other = entry.getKey();
            if (other != proxy && !other.isRemoved() && other.getPriority() > proxy.getPriority() &&
                    now - entry.getValue() < WANT_TICKS) {
                return false;
            }
        }
        return true;
    }

    public void markWanted(ExpandedPatternBufferProxyPartMachine proxy, int slot) {
        if (getLevel() == null) return;
        wanted.computeIfAbsent(slot, k -> new java.util.WeakHashMap<>()).put(proxy, getLevel().getGameTime());
    }

    private void updateChunkLoading() {
        if (!(getLevel() instanceof ServerLevel server) ||
                !PhoenixConfigs.INSTANCE.features.expandedBufferKeepsChunkLoaded)
            return;
        boolean linked = !((MEPatternBufferResizeAccessor) this).phoenix$getProxyPositions().isEmpty();
        ExpandedBufferChunkLoader.set(server, getBlockPos(), linked);
    }

    @Override
    public void addProxy(com.gregtechceu.gtceu.integration.ae2.machine.MEPatternBufferProxyPartMachine proxy) {
        super.addProxy(proxy);
        updateChunkLoading();
    }

    @Override
    public void removeProxy(com.gregtechceu.gtceu.integration.ae2.machine.MEPatternBufferProxyPartMachine proxy) {
        super.removeProxy(proxy);
        updateChunkLoading();
    }

    @Override
    public void onMachineDestroyed() {
        super.onMachineDestroyed();
        if (getLevel() instanceof ServerLevel server) ExpandedBufferChunkLoader.set(server, getBlockPos(), false);
    }

    @Override
    public void buildMainUI(ParentWidget<?> parent, PosGuiData guiData, PanelSyncManager syncManager,
                            UISettings settings) {
        SlotGroup slotGroup = new SlotGroup("pattern_slots", COLUMNS, 0, true);
        BooleanSyncValue online = new BooleanSyncValue(() -> this.isOnline, value -> this.isOnline = value);
        syncManager.syncValue("is_online", online);

        syncManager.syncValue("comparator_mode",
                new IntSyncValue(() -> comparatorMode, value -> comparatorMode = value));
        syncManager.registerSyncedAction("sort_patterns", false, true, buf -> sortPatterns());
        syncManager.registerSyncedAction("cycle_comparator", false, true, buf -> cycleComparatorMode());

        Flow toolbar = Flow.row().coverChildren();
        ButtonWidget<?> sort = new ButtonWidget<>().size(50, 12)
                .background(GTGuiTextures.BACKGROUND_INVERSE)
                .overlay(brachy.modularui.api.drawable.Text.lang("gui.phoenixcore.expanded_buffer.sort"));
        sort.onMousePressed((context, button) -> {
            syncManager.callSyncedAction("sort_patterns");
            return true;
        });
        ButtonWidget<?> mode = new ButtonWidget<>().size(80, 12).marginLeft(4)
                .background(GTGuiTextures.BACKGROUND_INVERSE)
                .overlay(brachy.modularui.api.drawable.Text.dynamic(() -> Component.translatable(
                        "gui.phoenixcore.expanded_buffer.comparator." + getComparatorMode().name().toLowerCase())));
        mode.onMousePressed((context, button) -> {
            syncManager.callSyncedAction("cycle_comparator");
            return true;
        });
        toolbar.child(sort);
        toolbar.child(mode);

        Flow column = Flow.col().coverChildren();
        column.child(Text_online(online));
        column.child(toolbar);
        column.child(new Grid()
                .height(18 * (SLOTS / COLUMNS)).marginTop(4)
                .minElementMargin(0, 0)
                .minColWidth(18)
                .minRowHeight(18)
                .leftRel(0.5f)
                .gridOfSizeWidth(SLOTS, COLUMNS, (x, y, index) -> new ItemSlot()
                        .slot(SyncHandlers.itemSlot(getPatternInventory(), index)
                                .slotGroup(slotGroup)
                                .accessibility(true, true)
                                .filter(stack -> stack.getItem() instanceof EncodedPatternItem)
                                .changeListener((newItem, onlyAmountChanged, client, init) -> onPatternChange(index)))
                        .background(GTGuiTextures.SLOT, GTGuiTextures.PATTERN_OVERLAY)));
        parent.child(column.center());
    }

    private static brachy.modularui.api.widget.IWidget Text_online(BooleanSyncValue online) {
        return brachy.modularui.api.drawable.Text
                .dynamic(() -> online.getBoolValue() ? Component.translatable("gtceu.gui.me_network.online") :
                        Component.translatable("gtceu.gui.me_network.offline"))
                .asWidget().marginTop(2).marginBottom(4);
    }
}
