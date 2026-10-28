package net.phoenix.core.integration.conflux.multiblock;

import net.phoenix.core.client.gui.hud.HudModel;
import net.phoenix.core.client.gui.hud.HudRecipes;
import net.phoenix.core.client.gui.hud.MachineHud;
import brachy.modularui.value.sync.FloatSyncValue;
import brachy.modularui.value.sync.IntSyncValue;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.misc.EnergyContainerList;
import com.gregtechceu.gtceu.utils.ExtendedUseOnContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.phoenix.core.integration.conflux.ConfluxDataType;
import net.phoenix.core.integration.conflux.pipe.ConfluxDataCapability;
import net.phoenix.core.integration.conflux.pipe.ConfluxMultiHandlerCapability;
import net.phoenix.core.integration.conflux.pipe.IConfluxDataHandler;
import net.phoenix.core.integration.conflux.pipe.IConfluxMultiHandler;
import net.phoenix.core.integration.conflux.research.PlayerResearchCapability;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.BooleanSyncValue;
import brachy.modularui.value.sync.DoubleSyncValue;
import brachy.modularui.value.sync.LongSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.TextWidget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ConfluxHarmonicLens extends WorkableElectricMultiblockMachine
                                 implements ConfluxDataCapability.Holder {

    private static final double BASE_EFFICIENCY = 0.70;
    private static final double MAX_BONUS = 0.30;
    private static final double BONUS_PER_NODE = 0.005 / 50.0;
    private static final long INPUT_BUFFER = 200_000L;
    private static final long OUTPUT_BUFFER = 200_000L;
    private static final long EU_PER_TICK = 524_288L;
    private static final long THROUGHPUT = 2_048L;
    private static final int RESEARCH_SCAN_INTERVAL = 100;

    private ConfluxDataType inputType = ConfluxDataType.MATERIAL;
    private ConfluxDataType outputType = ConfluxDataType.COMPUTATIONAL;
    private long inputBuffer = 0L;
    private long outputBuffer = 0L;
    private boolean lensActive = false;
    private double currentEfficiency = BASE_EFFICIENCY;

    @Nullable
    private IConfluxDataHandler inputHandler;
    private int researchScanCooldown = 0;

    public ConfluxHarmonicLens(BlockEntityCreationInfo holder) {
        super(holder);
        rebuildInputCap();
        this.subscribeServerTick(() -> this.lensTick());
    }

    private void lensTick() {
        if (!isFormed() || getLevel() == null || isRemote()) return;

        List<IRecipeHandler<?>> energyCaps = this.getCapabilitiesFlat(IO.IN, EURecipeCapability.CAP);
        if (energyCaps.isEmpty()) {
            lensActive = false;
            return;
        }

        List<com.gregtechceu.gtceu.api.capability.IEnergyContainer> containers = new ArrayList<>();
        for (IRecipeHandler<?> handler : energyCaps) {
            if (handler instanceof com.gregtechceu.gtceu.api.capability.IEnergyContainer container) {
                containers.add(container);
            }
        }

        EnergyContainerList energyList = new EnergyContainerList(containers);
        if (energyList.getEnergyStored() < EU_PER_TICK) {
            lensActive = false;
            return;
        }
        energyList.removeEnergy(EU_PER_TICK);
        lensActive = true;

        if (--researchScanCooldown <= 0) {
            currentEfficiency = BASE_EFFICIENCY + computeResearchBonus();
            researchScanCooldown = RESEARCH_SCAN_INTERVAL;
        }

        long canConvert = Math.min(inputBuffer, THROUGHPUT);
        if (canConvert > 0) {
            long converted = (long) (canConvert * currentEfficiency);
            inputBuffer -= canConvert;
            long space = OUTPUT_BUFFER - outputBuffer;
            outputBuffer += Math.min(converted, space);
            setChanged();
        }

        if (outputBuffer > 0) pushOutput();
    }

    private double computeResearchBonus() {
        if (!(getLevel() instanceof ServerLevel serverLevel)) return 0;
        double bonus = 0;
        AABB range = new AABB(getBlockPos()).inflate(16);
        for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, range)) {
            var data = PlayerResearchCapability.get(player);
            if (data != null) bonus += data.getUnlockedCount() * BONUS_PER_NODE;
        }
        return Math.min(bonus, MAX_BONUS);
    }

    private void pushOutput() {
        BlockPos pos = getBlockPos();
        long budget = Math.min(outputBuffer, THROUGHPUT);

        for (Direction dir : Direction.values()) {
            if (budget <= 0) break;
            BlockEntity be = getLevel().getBlockEntity(pos.relative(dir));
            if (be == null) continue;

            IConfluxDataHandler h = ConfluxDataCapability.find(be, dir.getOpposite());
            if (h != null) {
                if (h.getDataType() == outputType) {
                    long sent = h.insert(budget);
                    outputBuffer -= sent;
                    budget -= sent;
                }
                continue;
            }

            IConfluxMultiHandler multi = ConfluxMultiHandlerCapability.find(be, dir.getOpposite());
            if (multi != null) {
                long sent = multi.insert(outputType, budget);
                outputBuffer -= sent;
                budget -= sent;
            }
        }
        if (budget < THROUGHPUT) setChanged();
    }

    @Override
    public InteractionResult onUseWithItem(ExtendedUseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        ItemStack heldStack = player.getItemInHand(context.getHand());

        if (com.gregtechceu.gtceu.api.item.tool.GTToolType.SCREWDRIVER.is(heldStack)) {
            if (isRemote()) return InteractionResult.SUCCESS;

            ConfluxDataType[] types = ConfluxDataType.values();
            if (player.isCrouching()) {
                int next = inputType.ordinal();
                do {
                    next = (next + 1) % types.length;
                } while (types[next] == outputType);
                inputType = types[next];
                rebuildInputCap();
                player.sendSystemMessage(Component.literal("Input type: ").append(inputType.displayComponent()));
            } else {
                int next = outputType.ordinal();
                do {
                    next = (next + 1) % types.length;
                } while (types[next] == inputType);
                outputType = types[next];
                player.sendSystemMessage(Component.literal("Output type: ").append(outputType.displayComponent()));
            }
            setChanged();
            return InteractionResult.SUCCESS;
        }
        return super.onUseWithItem(context);
    }

    private void rebuildInputCap() {
        inputHandler = new IConfluxDataHandler() {

            @Override
            public ConfluxDataType getDataType() {
                return inputType;
            }

            @Override
            public long insert(long amount) {
                long space = INPUT_BUFFER - inputBuffer;
                long taken = Math.min(amount, space);
                inputBuffer += taken;
                if (taken > 0) setChanged();
                return taken;
            }

            @Override
            public long extract(long amount) {
                return 0;
            }

            @Override
            public long getStored() {
                return inputBuffer;
            }

            @Override
            public long getCapacity() {
                return INPUT_BUFFER;
            }
        };
    }

    @Override
    public @Nullable IConfluxDataHandler getConfluxDataHandler(@Nullable Direction side) {
        return inputHandler;
    }

    private void hudAction(int id, int value) {
        HudRecipes.handle(this, id);
    }

    @Override
    public brachy.modularui.screen.ModularPanel<?> buildUI(brachy.modularui.factory.PosGuiData data,
                                                           PanelSyncManager syncManager,
                                                           brachy.modularui.screen.UISettings settings) {
        syncManager.syncValue("lens_active", 0, new BooleanSyncValue(() -> this.lensActive, v -> this.lensActive = v));
        syncManager.syncValue("input_buffer", 0, new LongSyncValue(() -> this.inputBuffer, v -> this.inputBuffer = v));
        syncManager.syncValue("output_buffer", 0, new LongSyncValue(() -> this.outputBuffer, v -> this.outputBuffer = v));
        syncManager.syncValue("efficiency", 0, new DoubleSyncValue(() -> this.currentEfficiency,
                v -> this.currentEfficiency = v));
        return MachineHud.build(this, data, syncManager, settings, this::hudAction, this::hudModel);
    }

    private HudModel hudModel() {
        return HudModel.of("Conflux Harmonic Lens")
                .formed(this::isFormed)
                .theme(net.phoenix.core.client.gui.hud.HudTheme.DATA)
                .hero(net.phoenix.core.client.gui.hud.Heroes.arcGauge(() -> (float) currentEfficiency, net.phoenix.core.client.gui.hud.HudTheme.DATA.accent()), 38)
                .badge(() -> lensActive ? new HudModel.Badge("TRANSMUTING", HudModel.GREEN) :
                        new HudModel.Badge("OFFLINE", HudModel.RED))
                .subtitle(() -> inputType.displayName + " data in, " + outputType.displayName + " data out")
                .cells(() -> {
                    var cells = new java.util.ArrayList<HudModel.Cell>();
                    cells.add(HudModel.cell("EFFICIENCY", String.format(java.util.Locale.ROOT, "%.1f%%",
                            currentEfficiency * 100), String.format(java.util.Locale.ROOT, "+%.1f%% research",
                                    (currentEfficiency - BASE_EFFICIENCY) * 100), HudModel.GOLD));
                    cells.add(HudModel.cell("INPUT", inputType.displayName, String.format(java.util.Locale.ROOT, "%,d held",
                            inputBuffer), HudModel.BLUE));
                    cells.add(HudModel.cell("OUTPUT", outputType.displayName, String.format(java.util.Locale.ROOT,
                            "%,d held", outputBuffer), HudModel.CYAN));
                    cells.add(HudModel.cell("THROUGHPUT", String.format(java.util.Locale.ROOT, "%,d u/t", THROUGHPUT),
                            "per tick", HudModel.PURPLE));
                    cells.add(HudModel.cell("POWER", String.format(java.util.Locale.ROOT, "%,d EU/t", EU_PER_TICK),
                            lensActive ? "supplied" : "SHORT", lensActive ? HudModel.GREEN : HudModel.RED));
                    return cells;
                })
                .bars(() -> java.util.List.of(
                        new HudModel.Bar("Input buffer", inputBuffer / (float) INPUT_BUFFER,
                                String.format(java.util.Locale.ROOT, "%,d / %,d", inputBuffer, INPUT_BUFFER), HudModel.BLUE),
                        new HudModel.Bar("Output buffer", outputBuffer / (float) OUTPUT_BUFFER,
                                String.format(java.util.Locale.ROOT, "%,d / %,d", outputBuffer, OUTPUT_BUFFER),
                                HudModel.CYAN)))
                .notes(() -> java.util.List.of(HudModel.line("Screwdriver cycles the types. Research nearby adds efficiency.",
                        HudModel.MID)))
                .math(() -> java.util.List.of(
                        HudModel.heading("EFFICIENCY"),
                        HudModel.line(String.format(java.util.Locale.ROOT, "base %.0f%% + research bonus (max %.0f%%)",
                                BASE_EFFICIENCY * 100, MAX_BONUS * 100), HudModel.MID),
                        HudModel.line(String.format(java.util.Locale.ROOT, "now %.1f%% = %.1f%% + %.1f%%",
                                currentEfficiency * 100, BASE_EFFICIENCY * 100, (currentEfficiency - BASE_EFFICIENCY) * 100),
                                HudModel.WHITE),
                        HudModel.line("each research node adds " + String.format(java.util.Locale.ROOT, "%.3f%%",
                                BONUS_PER_NODE * 100), HudModel.DIM),
                        HudModel.heading("TRANSMUTING"),
                        HudModel.line("out = in * efficiency, " + THROUGHPUT + " u/t at most", HudModel.MID),
                        HudModel.line(String.format(java.util.Locale.ROOT, "%,d u in -> %,d u out per second", THROUGHPUT * 20,
                                (long) (THROUGHPUT * 20 * currentEfficiency)), HudModel.WHITE)))
                .graph("efficiency %", HudModel.GOLD, () -> (float) (currentEfficiency * 100), 100.0f)
                .graph("input %", HudModel.BLUE, () -> inputBuffer * 100.0f / INPUT_BUFFER, 100.0f)
                .graph("output %", HudModel.CYAN, () -> outputBuffer * 100.0f / OUTPUT_BUFFER, 100.0f);
    }

    @Override
    public void invalidateStructure(@org.jetbrains.annotations.NotNull String substructureName) {
        super.invalidateStructure(substructureName);
        lensActive = false;
        inputHandler = null;
    }

    @Override
    public void formStructure(@org.jetbrains.annotations.NotNull String substructureName) {
        super.formStructure(substructureName);
        rebuildInputCap();
    }

    @Override
    public void onUnload() {
        super.onUnload();
        inputHandler = null;
    }

    public ConfluxDataType getInputType() {
        return inputType;
    }

    public ConfluxDataType getOutputType() {
        return outputType;
    }

    public double getCurrentEfficiency() {
        return currentEfficiency;
    }

    public boolean isLensActive() {
        return lensActive;
    }
}
