package net.phoenix.core.integration.conflux.multiblock;

import net.phoenix.core.client.gui.hud.HudModel;
import net.phoenix.core.client.gui.hud.HudRecipes;
import net.phoenix.core.client.gui.hud.MachineHud;
import brachy.modularui.value.sync.BooleanSyncValue;
import brachy.modularui.value.sync.DoubleSyncValue;
import brachy.modularui.value.sync.FloatSyncValue;
import brachy.modularui.value.sync.IntSyncValue;
import brachy.modularui.value.sync.LongSyncValue;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.sync_system.annotations.RerenderOnChanged;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.utils.ExtendedUseOnContext;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.phoenix.core.integration.conflux.ConfluxDataType;
import net.phoenix.core.integration.conflux.pipe.ConfluxDataCapability;
import net.phoenix.core.integration.conflux.pipe.ConfluxMultiHandlerCapability;
import net.phoenix.core.integration.conflux.pipe.IConfluxDataHandler;
import net.phoenix.core.integration.conflux.pipe.IConfluxMultiHandler;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.TextWidget;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ConfluxCascadeEngine extends WorkableElectricMultiblockMachine
                                  implements ConfluxDataCapability.Holder {

    private static final int FUEL_TICKS = 400;
    private static final long BASE_RATE = 200L;
    private static final float MAX_MULT = 10f;
    private static final long EU_PER_TICK = 524_288L;
    private static final long BUFFER = 100_000L;
    private static final long PUSH_RATE = 4_096L;
    private static final float MOMENTUM_GAIN = 1f / 500f;
    private static final float MOMENTUM_DECAY = 0.05f;

    @SaveField
    private float momentum = 0f;

    @SaveField
    private int fuelTicks = 0;

    @SaveField
    private long dataBuffer = 0L;

    @SaveField
    @SyncToClient
    private ConfluxDataType outputType = ConfluxDataType.MATERIAL;

    @SaveField
    @SyncToClient
    @RerenderOnChanged
    private boolean engineActive = false;

    @SaveField
    private final NotifiableItemStackHandler fuelSlot;

    private final IConfluxDataHandler outputHandler;

    public ConfluxCascadeEngine(BlockEntityCreationInfo holder) {
        super(holder);
        this.fuelSlot = new NotifiableItemStackHandler(1, IO.IN);
        this.outputHandler = makeOutputHandler();
        subscribeServerTick(this::engineTick);
    }

    private void engineTick() {
        if (!isFormed() || getLevel() == null || isRemote()) return;

        var energy = getEnergyContainer();
        if (energy == null || energy.getEnergyStored() < EU_PER_TICK) {
            stall();
            return;
        }
        energy.removeEnergy(EU_PER_TICK);

        if (fuelTicks <= 0) {
            var stack = fuelSlot.getStackInSlot(0);
            if (!stack.isEmpty()) {
                fuelSlot.extractItem(0, 1, false);
                fuelTicks = FUEL_TICKS;
                setChanged();
            }
        }

        if (fuelTicks <= 0) {
            stall();
            return;
        }
        fuelTicks--;

        momentum = Math.min(1f, momentum + MOMENTUM_GAIN);
        engineActive = true;
        setChanged();

        float multiplier = 1f + (MAX_MULT - 1f) * momentum;
        long produced = (long) (BASE_RATE * multiplier);
        long space = BUFFER - dataBuffer;
        if (space > 0) dataBuffer += Math.min(produced, space);

        if (dataBuffer > 0) pushToNetwork();
    }

    private void stall() {
        if (momentum > 0) {
            momentum = Math.max(0f, momentum - MOMENTUM_DECAY);
            setChanged();
        }
        if (engineActive) {
            engineActive = false;
            setChanged();
        }
        if (dataBuffer > 0) pushToNetwork();
    }

    private void pushToNetwork() {
        BlockPos pos = getBlockPos();
        long budget = Math.min(dataBuffer, PUSH_RATE);

        for (Direction dir : Direction.values()) {
            if (budget <= 0) break;
            BlockEntity be = getLevel().getBlockEntity(pos.relative(dir));
            if (be == null) continue;

            IConfluxDataHandler h = ConfluxDataCapability.find(be, dir.getOpposite());
            if (h != null) {
                if (h.getDataType() == outputType) {
                    long sent = h.insert(budget);
                    dataBuffer -= sent;
                    budget -= sent;
                }
                continue;
            }

            IConfluxMultiHandler multiCap = ConfluxMultiHandlerCapability.find(be, dir.getOpposite());
            if (multiCap != null) {
                long sent = multiCap.insert(outputType, budget);
                dataBuffer -= sent;
                budget -= sent;
            }
        }
        if (budget < PUSH_RATE) setChanged();
    }

    @Override
    protected InteractionResult onScrewdriverClick(ExtendedUseOnContext ctx) {
        var player = ctx.getPlayer();
        if (player != null && player.isCrouching() && !isRemote()) {
            ConfluxDataType[] types = ConfluxDataType.values();
            outputType = types[(outputType.ordinal() + 1) % types.length];
            player.sendSystemMessage(Component.literal("Output type: ")
                    .append(outputType.displayComponent()));
            return InteractionResult.SUCCESS;
        }
        return super.onScrewdriverClick(ctx);
    }

    private IConfluxDataHandler makeOutputHandler() {
        return new IConfluxDataHandler() {

            @Override
            public ConfluxDataType getDataType() {
                return outputType;
            }

            @Override
            public long insert(long amount) {
                return 0;
            }

            @Override
            public long extract(long amount) {
                long given = Math.min(amount, dataBuffer);
                dataBuffer -= given;
                return given;
            }

            @Override
            public long getStored() {
                return dataBuffer;
            }

            @Override
            public long getCapacity() {
                return BUFFER;
            }
        };
    }

    @Override
    public @Nullable IConfluxDataHandler getConfluxDataHandler(@Nullable Direction side) {
        return outputHandler;
    }

    private void hudAction(int id, int value) {
        HudRecipes.handle(this, id);
    }

    @Override
    public brachy.modularui.screen.ModularPanel<?> buildUI(brachy.modularui.factory.PosGuiData data,
                                                           PanelSyncManager syncManager,
                                                           brachy.modularui.screen.UISettings settings) {
        syncManager.syncValue("momentum", 0, new FloatSyncValue(() -> this.momentum, v -> this.momentum = v));
        syncManager.syncValue("fuel_ticks", 0, new IntSyncValue(() -> this.fuelTicks, v -> this.fuelTicks = v));
        syncManager.syncValue("data_buffer", 0, new LongSyncValue(() -> this.dataBuffer, v -> this.dataBuffer = v));
        return MachineHud.build(this, data, syncManager, settings, this::hudAction, this::hudModel);
    }

    private float multiplier() {
        return 1f + (MAX_MULT - 1f) * momentum;
    }

    private HudModel hudModel() {
        return HudModel.of("Conflux Cascade Engine")
                .formed(this::isFormed)
                .theme(net.phoenix.core.client.gui.hud.HudTheme.DATA)
                .hero(net.phoenix.core.client.gui.hud.Heroes.dataFlow(() -> momentum, 3, 0xFF_FFC44D, net.phoenix.core.client.gui.hud.HudTheme.DATA.accent()), 30)
                .badge(() -> engineActive ? new HudModel.Badge("RUNNING", HudModel.GREEN) :
                        new HudModel.Badge("STALLED", HudModel.RED))
                .subtitle(() -> "Spins up on fuel and pushes " + outputType.displayName + " data")
                .cells(() -> {
                    var cells = new java.util.ArrayList<HudModel.Cell>();
                    cells.add(HudModel.cell("MOMENTUM", String.format(java.util.Locale.ROOT, "%.1f%%", momentum * 100f),
                            momentum >= 1f ? "full" : "spinning up", HudModel.GOLD));
                    cells.add(HudModel.cell("OUTPUT", String.format(java.util.Locale.ROOT, "%,d u/t",
                            (long) (BASE_RATE * multiplier())), outputType.displayName, HudModel.CYAN));
                    cells.add(HudModel.cell("MULTIPLIER", String.format(java.util.Locale.ROOT, "x%.2f", multiplier()),
                            "max x" + MAX_MULT, HudModel.PURPLE));
                    cells.add(HudModel.cell("BUFFER", String.format(java.util.Locale.ROOT, "%,d", dataBuffer),
                            String.format(java.util.Locale.ROOT, "of %,d", (long) BUFFER), HudModel.BLUE));
                    cells.add(HudModel.cell("FUEL", fuelTicks + " ticks", fuelTicks > 0 ? "burning" : "none",
                            fuelTicks > 0 ? HudModel.GREEN : HudModel.RED));
                    return cells;
                })
                .bars(() -> java.util.List.of(
                        new HudModel.Bar("Momentum", momentum, String.format(java.util.Locale.ROOT, "%.0f%%", momentum * 100f),
                                HudModel.GOLD),
                        new HudModel.Bar("Data buffer", dataBuffer / (float) BUFFER,
                                String.format(java.util.Locale.ROOT, "%.0f%%", dataBuffer * 100.0 / BUFFER),
                                HudModel.BLUE)))
                .notes(() -> java.util.List.of(momentum < 1f ?
                        HudModel.line("Full momentum in " + (int) ((1f - momentum) / MOMENTUM_GAIN) + " ticks.",
                                HudModel.MID) : HudModel.line("Full momentum.", HudModel.GREEN)))
                .math(() -> java.util.List.of(
                        HudModel.heading("OUTPUT"),
                        HudModel.line("multiplier = 1 + (max - 1) * momentum", HudModel.MID),
                        HudModel.line(String.format(java.util.Locale.ROOT, "= 1 + (%.1f - 1) * %.2f = %.2f", MAX_MULT,
                                momentum, multiplier()), HudModel.WHITE),
                        HudModel.line("rate = base * multiplier", HudModel.MID),
                        HudModel.line(String.format(java.util.Locale.ROOT, "= %,d * %.2f = %,d u/t", (long) BASE_RATE,
                                multiplier(), (long) (BASE_RATE * multiplier())), HudModel.WHITE),
                        HudModel.heading("MOMENTUM"),
                        HudModel.line("gains " + MOMENTUM_GAIN + " a tick while fuelled", HudModel.MID)))
                .graph("momentum %", HudModel.GOLD, () -> momentum * 100f, 100.0f)
                .graph("output u/t", HudModel.CYAN, () -> BASE_RATE * multiplier(), 0.0f)
                .graph("buffer %", HudModel.BLUE, () -> dataBuffer * 100.0f / BUFFER, 100.0f);
    }

    @Override
    public void onUnload() {
        super.onUnload();
    }

    public ConfluxDataType getOutputType() {
        return outputType;
    }

    public float getMomentum() {
        return momentum;
    }

    public boolean isEngineActive() {
        return engineActive;
    }
}
