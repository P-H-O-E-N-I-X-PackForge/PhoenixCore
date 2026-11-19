package net.phoenix.core.integration.ars_nouveau.conflux;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.TieredEnergyMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.trait.MachineTrait;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableEnergyContainer;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.phoenix.conflux.ConfluxDataType;
import net.phoenix.conflux.client.ConfluxTheme;
import net.phoenix.conflux.common.pipe.IConfluxDataHandler;
import net.phoenix.conflux.common.pipe.IConfluxMultiHandler;
import net.phoenix.core.integration.ars_nouveau.api.capability.ISourceProviderCapability;
import net.phoenix.core.integration.ars_nouveau.api.machine.trait.NotifiableSourceContainer;
import net.phoenixvine.wiki.ui.hud.Heroes;
import net.phoenixvine.wiki.ui.hud.HudModel;
import net.phoenixvine.wiki.ui.hud.HudTheme;
import net.phoenixvine.wiki.ui.hud.MachineHud;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.UISettings;
import brachy.modularui.utils.Alignment;
import brachy.modularui.value.sync.BooleanSyncValue;
import brachy.modularui.value.sync.IntSyncValue;
import brachy.modularui.value.sync.LongSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.value.sync.SyncHandlers;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widgets.SlotGroupWidget;
import brachy.modularui.widgets.layout.Flow;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.slot.SlotGroup;
import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public class ConfluxProducerMachine extends TieredEnergyMachine implements IMuiMachine, ISourceProviderCapability {

    public static final TagKey<Item> BIOMATERIAL = ConfluxFuels.BIOMATERIAL;

    private static final long BASE_RATE = 4L;

    private static final long BUFFER_PER_TIER = 1024L;
    private static final long PUSH_PER_TIER = 32L;

    protected final ConfluxStorageTrait storageTrait;

    @SaveField
    @SyncToClient
    protected final NotifiableItemStackHandler fuelSlot;

    @Nullable
    private final NotifiableSourceContainer sourceContainer;

    @Getter
    private final ConfluxDataType dataType;
    private final IConfluxDataHandler dataHandler;

    @Nullable
    private TickableSubscription tickSub;

    private boolean supplied = false;

    public ConfluxProducerMachine(BlockEntityCreationInfo holder, int tier, ConfluxDataType dataType) {
        super(holder, tier, dataType == ConfluxDataType.ENERGETIC ?
                NotifiableEnergyContainer.receiverContainer(GTValues.V[tier] * 64L, GTValues.V[tier], 1L) :

                NotifiableEnergyContainer.receiverContainer(0L, GTValues.V[tier], 0L));

        this.dataType = dataType;

        ConfluxStorageTrait localStorage = new ConfluxStorageTrait();
        this.attachPersistentTrait("conflux_storage", localStorage);
        this.storageTrait = localStorage;

        NotifiableItemStackHandler localFuel = new NotifiableItemStackHandler(1, IO.IN, IO.IN);
        localFuel.setFilter(this::isFuel);
        this.attachTrait(localFuel);
        this.fuelSlot = localFuel;

        if (dataType == ConfluxDataType.ARCANE) {
            this.sourceContainer = attachTrait(
                    new NotifiableSourceContainer(IO.IN, 2000 * tier, 500 * tier));
        } else {
            this.sourceContainer = null;
        }

        this.dataHandler = this.buildDataHandler();
    }

    public static boolean usesItems(ConfluxDataType type) {
        return ConfluxFuels.usesItems(type);
    }

    public boolean isFuel(ItemStack stack) {
        return ConfluxFuels.isFuel(dataType, stack);
    }

    private long buffer() {
        return BUFFER_PER_TIER * tier;
    }

    private long pushRate() {
        return PUSH_PER_TIER * tier;
    }

    private long euPerTick() {
        return 8L * (1L << (tier - 1));
    }

    private int sourcePerTick() {
        return 2 * tier;
    }

    @Override
    @Nullable
    public ISourceTile getSource() {
        return sourceContainer;
    }

    public boolean takesSource() {
        return sourceContainer != null;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        tickSub = subscribeServerTick(tickSub, this::tick);
    }

    protected void tick() {
        if (getLevel() == null || isRemote()) return;

        boolean running = switch (dataType) {
            case ENERGETIC -> {
                boolean has = energyContainer.getEnergyStored() >= euPerTick();
                if (has) energyContainer.removeEnergy(euPerTick());
                yield has;
            }
            case ARCANE -> {
                boolean has = sourceContainer != null && sourceContainer.getSource() >= sourcePerTick();
                if (has) sourceContainer.removeSource(sourcePerTick());
                yield has;
            }
            default -> {
                if (storageTrait.getFuelTicks() <= 0) consumeFuel();
                boolean has = storageTrait.getFuelTicks() > 0;
                if (has) storageTrait.decrementFuelTick();
                yield has;
            }
        };
        supplied = running;

        if (running) {
            long space = buffer() - storageTrait.getStored();
            long made = Math.round(BASE_RATE * tier * (usesItems(dataType) ? storageTrait.getQuality() : 1.0f));
            if (space > 0) storageTrait.addStored(Math.min(made, space));
        }
        storageTrait.setActive(running && storageTrait.getStored() > 0);

        if (storageTrait.getStored() > 0) {
            pushOutput();
        }
    }

    protected void consumeFuel() {
        ItemStack stack = fuelSlot.getStackInSlot(0);
        if (stack.isEmpty() || !isFuel(stack)) return;
        int ticks = ConfluxFuels.ticks(dataType, stack);
        float quality = ConfluxFuels.quality(dataType, stack);
        ItemStack taken = fuelSlot.extractItemInternal(0, 1, false);
        if (taken.isEmpty()) return;
        storageTrait.setFuelTicks(ticks);
        storageTrait.setQuality(quality);
    }

    private void pushOutput() {
        BlockPos pos = getBlockPos();
        long budget = Math.min(storageTrait.getStored(), pushRate());

        for (Direction dir : Direction.values()) {
            if (budget <= 0) break;
            BlockEntity be = getLevel().getBlockEntity(pos.relative(dir));
            if (be == null) continue;

            IConfluxDataHandler singleHandler = net.phoenix.conflux.common.pipe.ConfluxDataCapability
                    .find(be, dir.getOpposite());
            if (singleHandler != null) {
                if (singleHandler.getDataType() == dataType) {
                    long sent = singleHandler.insert(budget);
                    if (sent > 0) {
                        storageTrait.addStored(-sent);
                        budget -= sent;
                    }
                }
                continue;
            }

            IConfluxMultiHandler multiHandler = net.phoenix.conflux.common.pipe.ConfluxMultiHandlerCapability
                    .find(be, dir.getOpposite());
            if (multiHandler != null) {
                long sent = multiHandler.insert(dataType, budget);
                if (sent > 0) {
                    storageTrait.addStored(-sent);
                    budget -= sent;
                }
            }
        }
    }

    private IConfluxDataHandler buildDataHandler() {
        return new IConfluxDataHandler() {

            @Override
            public ConfluxDataType getDataType() {
                return dataType;
            }

            @Override
            public long insert(long amount) {
                return 0;
            }

            @Override
            public long extract(long amount) {
                long given = Math.min(amount, storageTrait.getStored());
                if (given > 0) storageTrait.addStored(-given);
                return given;
            }

            @Override
            public long getStored() {
                return storageTrait.getStored();
            }

            @Override
            public long getCapacity() {
                return buffer();
            }
        };
    }

    public IConfluxDataHandler getConfluxDataHandler() {
        return this.dataHandler;
    }

    public boolean isActive() {
        return storageTrait.isActive();
    }

    public static String needTitle(ConfluxDataType type) {
        return switch (type) {
            case MATERIAL -> "Any material item";
            case BIOLOGICAL -> "Biomaterial";
            case ENERGETIC -> "Electricity";
            case COMPUTATIONAL -> "Circuits";
            case ARCANE -> "Source";
        };
    }

    public static String needDetail(ConfluxDataType type, int tier) {
        return switch (type) {
            case MATERIAL -> "Ingots, dusts, gems and plates. More material lasts longer.";
            case BIOLOGICAL -> "Plants, seeds, saplings and bio chaff.";
            case ENERGETIC -> (8L * (1L << (tier - 1))) + " EU per tick.";
            case COMPUTATIONAL -> "Any circuit, 20 seconds each. Better circuits make more data.";
            case ARCANE -> (2 * tier) + " Source per tick, from a Source pipe or relay.";
        };
    }

    @Override
    public brachy.modularui.screen.ModularPanel<?> buildUI(PosGuiData data, PanelSyncManager syncManager,
                                                           UISettings settings) {
        syncManager.syncValue("stored", 0, new LongSyncValue(() -> storageTrait.getStored(),
                v -> storageTrait.stored = v));
        syncManager.syncValue("fuel_ticks", 0, new IntSyncValue(() -> storageTrait.getFuelTicks(),
                v -> storageTrait.fuelTicks = v));
        syncManager.syncValue("quality", 0, new brachy.modularui.value.sync.FloatSyncValue(
                () -> storageTrait.getQuality(), v -> storageTrait.quality = v));
        syncManager.syncValue("active", 0, new BooleanSyncValue(() -> storageTrait.isActive(),
                v -> storageTrait.active = v));
        syncManager.syncValue("supplied", 0, new BooleanSyncValue(() -> this.supplied, v -> this.supplied = v));
        if (sourceContainer != null) {
            syncManager.syncValue("source", 0, new IntSyncValue(() -> sourceContainer.getSource(),
                    v -> sourceContainer.setSource(v)));
        }
        return net.phoenix.core.util.PatternErrorUi.with(this, syncManager, MachineHud.build(this, data, syncManager,
                settings, (id, value) -> {}, this::hudModel,
                new MachineHud.Classic("F", 176, usesItems(dataType) ? 146 : 70, content -> buildInputTab(content))));
    }

    private HudModel hudModel() {
        int accent = ConfluxTheme.typeColor(dataType);
        long rate = BASE_RATE * tier;
        return HudModel.of(GTValues.VN[tier] + " " + dataType.displayName + " Data Producer")
                .theme(HudTheme.DATA)
                .formed(() -> true)
                .hero(Heroes.arcGauge(Heroes.eased(() -> storageTrait.isActive() ?
                        Math.max(0.25f, storageTrait.getStored() / (float) buffer()) : 0.0f), accent), 38)
                .badge(() -> storageTrait.isActive() ? new HudModel.Badge("PRODUCING", HudModel.GREEN) :
                        new HudModel.Badge("WAITING", HudModel.GOLD))
                .subtitle(() -> storageTrait.isActive() ? "making " + dataType.displayName + " data" :
                        "needs " + needTitle(dataType).toLowerCase(Locale.ROOT))
                .cells(() -> {
                    var cells = new java.util.ArrayList<HudModel.Cell>();
                    cells.add(HudModel.cell("OUTPUT", rate + " u/t", dataType.displayName + " data", HudModel.CYAN));
                    cells.add(HudModel.cell("BUFFER", String.format(Locale.ROOT, "%,d", storageTrait.getStored()),
                            "of " + String.format(Locale.ROOT, "%,d", buffer()), HudModel.BLUE));
                    cells.add(HudModel.cell("NEEDS", needTitle(dataType), needDetail(dataType, tier),
                            supplied ? HudModel.GREEN : HudModel.RED));
                    if (usesItems(dataType)) {
                        cells.add(HudModel.cell("BURNING", storageTrait.getFuelTicks() > 0 ?
                                String.format(Locale.ROOT, "%.1f s", storageTrait.getFuelTicks() / 20.0) : "nothing",
                                (usesItems(dataType) && dataType == ConfluxDataType.COMPUTATIONAL ?
                                        String.format(Locale.ROOT, "x%.2f data", storageTrait.getQuality()) :
                                        "left on this item"),
                                storageTrait.getFuelTicks() > 0 ? HudModel.GREEN : HudModel.RED));
                    } else if (sourceContainer != null) {
                        cells.add(HudModel.cell("SOURCE", String.format(Locale.ROOT, "%,d", sourceContainer
                                .getSource()), "of " +
                                        String.format(Locale.ROOT, "%,d", sourceContainer
                                                .getMaxSource()),
                                sourceContainer.getSource() >= sourcePerTick() ? HudModel.GREEN : HudModel.RED));
                    }
                    return cells;
                })
                .bars(() -> java.util.List.of(new HudModel.Bar("Buffer", storageTrait.getStored() / (float) buffer(),
                        String.format(Locale.ROOT, "%,d / %,d", storageTrait.getStored(), buffer()), HudModel.CYAN)))
                .notes(() -> java.util.List.of(HudModel.line(
                        "Pushes up to " + pushRate() + " u/t into any pipe or machine that takes " +
                                dataType.displayName + " data.",
                        HudModel.MID)));
    }

    private void buildInputTab(ParentWidget<?> main) {
        Flow column = Flow.col()
                .widthRel(1f)
                .coverChildren()
                .crossAxisAlignment(Alignment.CrossAxis.CENTER)
                .childPadding(4)
                .margin(0, 6);

        column.child(Text.str(needTitle(dataType).toUpperCase(Locale.ROOT)).asWidget()
                .color(ConfluxTheme.typeColor(dataType)).size(160, 10));
        column.child(Text.str(needDetail(dataType, tier)).asWidget().color(0xFF8C93A8).size(170, 10));

        if (usesItems(dataType)) {
            var group = new SlotGroup("conflux_producer_fuel", 1, SlotGroup.STORAGE_SLOT_PRIO, true);
            var slot = SyncHandlers.itemSlot(fuelSlot.storage, 0).slotGroup(group).filter(this::isFuel);
            column.child(new ItemSlot().slot(slot).size(ItemSlot.SIZE, ItemSlot.SIZE));
            main.child(SlotGroupWidget.playerInventory(false).posRel(Alignment.BottomCenter).marginBottom(6));
        } else if (dataType == ConfluxDataType.ENERGETIC) {
            column.child(Text.str("Connect an energy source to the machine.").asWidget().color(0xFFB0B8C8)
                    .size(170, 10));
        } else {
            column.child(Text.str("Run a Source pipe or relay into the machine.").asWidget().color(0xFFB0B8C8)
                    .size(170, 10));
        }
        main.child(column.posRel(Alignment.TopCenter));
    }

    protected static class ConfluxStorageTrait extends MachineTrait {

        @SaveField
        @SyncToClient
        private long stored = 0L;
        @SaveField
        @SyncToClient
        private int fuelTicks = 0;
        @SaveField
        @SyncToClient
        private boolean active = false;
        @SaveField
        @SyncToClient
        private float quality = 1.0f;

        public ConfluxStorageTrait() {
            super();
        }

        public long getStored() {
            return stored;
        }

        public int getFuelTicks() {
            return fuelTicks;
        }

        public boolean isActive() {
            return active;
        }

        public float getQuality() {
            return quality;
        }

        public void setQuality(float next) {
            quality = next;
            getSyncDataHolder().markClientSyncFieldDirty("quality");
            markAsChanged();
        }

        public void addStored(long delta) {
            stored += delta;
            getSyncDataHolder().markClientSyncFieldDirty("stored");
            markAsChanged();
        }

        public void setFuelTicks(int ticks) {
            fuelTicks = ticks;
            getSyncDataHolder().markClientSyncFieldDirty("fuelTicks");
            markAsChanged();
        }

        public void decrementFuelTick() {
            fuelTicks--;
            getSyncDataHolder().markClientSyncFieldDirty("fuelTicks");
        }

        public void setActive(boolean next) {
            if (active == next) return;
            active = next;
            getSyncDataHolder().markClientSyncFieldDirty("active");
            markAsChanged();
        }
    }
}
