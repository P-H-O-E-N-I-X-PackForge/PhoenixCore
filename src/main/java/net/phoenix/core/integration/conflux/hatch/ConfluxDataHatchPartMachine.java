package net.phoenix.core.integration.conflux.hatch;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredIOPartMachine;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.phoenix.core.client.gui.hud.HudModel;
import net.phoenix.core.client.gui.hud.HudTheme;
import net.phoenix.core.client.gui.hud.MachineHud;
import net.phoenix.core.integration.conflux.ConfluxDataType;
import net.phoenix.core.integration.conflux.pipe.ConfluxDataCapability;
import net.phoenix.core.integration.conflux.pipe.ConfluxMultiHandlerCapability;
import net.phoenix.core.integration.conflux.pipe.IConfluxDataHandler;
import net.phoenix.core.integration.conflux.pipe.IConfluxMultiHandler;

import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.LongSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ConfluxDataHatchPartMachine extends TieredIOPartMachine
                                         implements ConfluxMultiHandlerCapability.Holder, IMuiMachine {

    public static final long CAPACITY = 64_000L;

    public static final long PUSH_RATE = 4_096L;

    private final IO io;

    @SaveField
    @SyncToClient
    private long material = 0L;
    @SaveField
    @SyncToClient
    private long biological = 0L;
    @SaveField
    @SyncToClient
    private long energetic = 0L;
    @SaveField
    @SyncToClient
    private long computational = 0L;
    @SaveField
    @SyncToClient
    private long arcane = 0L;

    private final IConfluxMultiHandler handler;

    @Nullable
    private TickableSubscription tickSub;

    public ConfluxDataHatchPartMachine(BlockEntityCreationInfo info, int tier, IO io) {
        super(info, tier, io);
        this.io = io;
        this.handler = buildHandler();
    }

    public IO getIo() {
        return io;
    }

    public long stored(ConfluxDataType type) {
        return switch (type) {
            case MATERIAL -> material;
            case BIOLOGICAL -> biological;
            case ENERGETIC -> energetic;
            case COMPUTATIONAL -> computational;
            case ARCANE -> arcane;
        };
    }

    private void set(ConfluxDataType type, long value) {
        switch (type) {
            case MATERIAL -> material = value;
            case BIOLOGICAL -> biological = value;
            case ENERGETIC -> energetic = value;
            case COMPUTATIONAL -> computational = value;
            case ARCANE -> arcane = value;
        }
        getSyncDataHolder().markClientSyncFieldDirty(type.name().toLowerCase(Locale.ROOT));
        setChanged();
    }

    public long deposit(ConfluxDataType type, long amount) {
        long kept = Math.max(0L, Math.min(amount, CAPACITY - stored(type)));
        if (kept > 0) set(type, stored(type) + kept);
        return kept;
    }

    public long withdraw(ConfluxDataType type, long amount) {
        long given = Math.max(0L, Math.min(amount, stored(type)));
        if (given > 0) set(type, stored(type) - given);
        return given;
    }

    private IConfluxMultiHandler buildHandler() {
        return new IConfluxMultiHandler() {

            @Override
            public long insert(ConfluxDataType type, long amount) {
                return io == IO.IN ? deposit(type, amount) : 0L;
            }

            @Override
            public long extract(ConfluxDataType type, long amount) {
                return 0L;
            }

            @Override
            public long getStored(ConfluxDataType type) {
                return stored(type);
            }

            @Override
            public long getCapacity(ConfluxDataType type) {
                return CAPACITY;
            }
        };
    }

    @Override
    public @Nullable IConfluxMultiHandler getConfluxMultiHandler(@Nullable Direction side) {
        return handler;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (io == IO.OUT && !isRemote()) tickSub = subscribeServerTick(tickSub, this::sendOn);
    }

    private void sendOn() {
        if (getLevel() == null || isRemote()) return;
        BlockPos pos = getBlockPos();
        for (Direction dir : Direction.values()) {
            BlockEntity be = getLevel().getBlockEntity(pos.relative(dir));
            if (be == null || be instanceof ConfluxDataHatchPartMachine) continue;

            IConfluxDataHandler single = ConfluxDataCapability.find(be, dir.getOpposite());
            IConfluxMultiHandler multi = single == null ? ConfluxMultiHandlerCapability.find(be, dir.getOpposite()) :
                    null;
            if (single == null && multi == null) continue;

            for (ConfluxDataType type : ConfluxDataType.values()) {
                long held = stored(type);
                if (held <= 0) continue;
                long budget = Math.min(held, PUSH_RATE);
                long sent;
                if (single != null) {
                    sent = single.getDataType() == type ? single.insert(budget) : 0L;
                } else {
                    sent = multi.insert(type, budget);
                }
                if (sent > 0) withdraw(type, sent);
            }
        }
    }

    @Override
    public brachy.modularui.screen.ModularPanel<?> buildUI(PosGuiData data, PanelSyncManager syncManager,
                                                           UISettings settings) {
        syncManager.syncValue("material", 0, new LongSyncValue(() -> material, v -> material = v));
        syncManager.syncValue("biological", 0, new LongSyncValue(() -> biological, v -> biological = v));
        syncManager.syncValue("energetic", 0, new LongSyncValue(() -> energetic, v -> energetic = v));
        syncManager.syncValue("computational", 0, new LongSyncValue(() -> computational, v -> computational = v));
        syncManager.syncValue("arcane", 0, new LongSyncValue(() -> arcane, v -> arcane = v));
        return MachineHud.build(this, data, syncManager, settings, (id, value) -> {}, this::hudModel);
    }

    private HudModel hudModel() {
        boolean input = io == IO.IN;
        return HudModel.of("Conflux Data " + (input ? "Input" : "Output") + " Hatch")
                .theme(HudTheme.DATA)
                .formed(() -> true)
                .hero(net.phoenix.core.client.gui.hud.Heroes.dataFlow(
                        net.phoenix.core.client.gui.hud.Heroes.eased(() -> {
                            long most = 0;
                            for (ConfluxDataType t : ConfluxDataType.values()) most = Math.max(most, stored(t));
                            return most / (float) CAPACITY;
                        }), 3, 0xFF_FFC44D, HudTheme.DATA.accent()), 30)
                .badge(() -> input ? new HudModel.Badge("INPUT", HudModel.CYAN) :
                        new HudModel.Badge("OUTPUT", HudModel.PURPLE))
                .subtitle(() -> input ? "Feeds data to the machine" : "Sends the machine's data on")
                .cells(() -> {
                    List<HudModel.Cell> cells = new ArrayList<>();
                    for (ConfluxDataType type : ConfluxDataType.values()) {
                        cells.add(HudModel.cell(type.displayName.toUpperCase(Locale.ROOT),
                                String.format(Locale.ROOT, "%,d", stored(type)),
                                "of " + String.format(Locale.ROOT, "%,d", CAPACITY), HudModel.GOLD));
                    }
                    return cells;
                })
                .notes(() -> List.of(HudModel.line(input ? "Pipe data in. Holds all five types." :
                        "Pipe data out. Holds all five types.", HudModel.MID)));
    }
}
