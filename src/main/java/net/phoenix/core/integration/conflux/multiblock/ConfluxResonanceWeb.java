package net.phoenix.core.integration.conflux.multiblock;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.misc.EnergyContainerList;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.phoenix.core.client.gui.hud.HudModel;
import net.phoenix.core.client.gui.hud.HudRecipes;
import net.phoenix.core.client.gui.hud.MachineHud;
import net.phoenix.core.integration.conflux.ConfluxDataType;
import net.phoenix.core.integration.conflux.pipe.ConfluxMultiHandlerCapability;
import net.phoenix.core.integration.conflux.pipe.ConfluxPipeBlockEntity;
import net.phoenix.core.integration.conflux.pipe.IConfluxMultiHandler;

import brachy.modularui.value.sync.BooleanSyncValue;
import brachy.modularui.value.sync.IntSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ConfluxResonanceWeb extends WorkableElectricMultiblockMachine {

    private static final int SCAN_INTERVAL = 100;
    private static final int SCAN_RADIUS = 24;
    private static final long RATE_PER_PIPE = 8L;
    private static final long BUFFER_PER = 500_000L;
    private static final long PUSH_RATE = 8_192L;
    private static final long EU_PER_TICK = 2_097_152L;

    private int totalPipeCount = 0;
    private int distinctTypeCount = 0;
    private boolean webActive = false;

    private final Map<ConfluxDataType, Long> buffer = new EnumMap<>(ConfluxDataType.class);
    private int scanCooldown = 0;

    public ConfluxResonanceWeb(BlockEntityCreationInfo holder) {
        super(holder);
        for (ConfluxDataType type : ConfluxDataType.values()) {
            buffer.put(type, 0L);
        }
        this.subscribeServerTick(() -> this.webTick());
    }

    private void webTick() {
        if (!isFormed() || getLevel() == null || isRemote()) return;

        List<IRecipeHandler<?>> energyCaps = this.getCapabilitiesFlat(IO.IN, EURecipeCapability.CAP);
        if (energyCaps.isEmpty()) {
            webActive = false;
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
            webActive = false;
            return;
        }
        energyList.removeEnergy(EU_PER_TICK);
        webActive = true;

        if (--scanCooldown <= 0) {
            scanNetwork();
            scanCooldown = SCAN_INTERVAL;
        }

        if (totalPipeCount > 0) {
            float diversityBonus = 1f + 0.25f * distinctTypeCount;
            long rateBase = (long) (totalPipeCount * RATE_PER_PIPE * diversityBonus);

            for (ConfluxDataType type : ConfluxDataType.values()) {
                if (!type.isAvailable()) continue;
                long space = BUFFER_PER - buffer.get(type);
                if (space > 0) {
                    buffer.merge(type, Math.min(rateBase, space), Long::sum);
                }
            }

            pushAllTypes();
        }
    }

    private void scanNetwork() {
        BlockPos center = getBlockPos();
        Map<ConfluxDataType, Integer> typeCounts = new EnumMap<>(ConfluxDataType.class);
        int total = 0;

        for (BlockPos scan : BlockPos.betweenClosed(
                center.offset(-SCAN_RADIUS, -8, -SCAN_RADIUS),
                center.offset(SCAN_RADIUS, 8, SCAN_RADIUS))) {
            BlockEntity be = getLevel().getBlockEntity(scan);
            if (be instanceof ConfluxPipeBlockEntity pipe) {
                total++;
                typeCounts.merge(pipe.getDataType(), 1, Integer::sum);
            }
        }

        totalPipeCount = total;
        distinctTypeCount = typeCounts.size();
        setChanged();
    }

    private void pushAllTypes() {
        BlockPos pos = getBlockPos();
        for (Direction dir : Direction.values()) {
            BlockEntity be = getLevel().getBlockEntity(pos.relative(dir));
            if (be == null) continue;

            IConfluxMultiHandler handler = ConfluxMultiHandlerCapability.find(be, dir.getOpposite());
            if (handler == null) continue;

            for (ConfluxDataType type : ConfluxDataType.values()) {
                long stored = buffer.get(type);
                if (stored <= 0) continue;
                long toSend = Math.min(stored, PUSH_RATE);
                long sent = handler.insert(type, toSend);
                if (sent > 0) {
                    buffer.merge(type, -sent, Long::sum);
                    setChanged();
                }
            }
        }
    }

    private void hudAction(int id, int value) {
        HudRecipes.handle(this, id);
    }

    @Override
    public brachy.modularui.screen.ModularPanel<?> buildUI(brachy.modularui.factory.PosGuiData data,
                                                           PanelSyncManager syncManager,
                                                           brachy.modularui.screen.UISettings settings) {
        syncManager.syncValue("web_active", 0, new BooleanSyncValue(() -> this.webActive, v -> this.webActive = v));
        syncManager.syncValue("total_pipes", 0,
                new IntSyncValue(() -> this.totalPipeCount, v -> this.totalPipeCount = v));
        syncManager.syncValue("distinct_types", 0, new IntSyncValue(() -> this.distinctTypeCount,
                v -> this.distinctTypeCount = v));
        return MachineHud.build(this, data, syncManager, settings, this::hudAction, this::hudModel);
    }

    private float diversity() {
        return 1f + 0.25f * distinctTypeCount;
    }

    private long ratePerType() {
        return (long) (totalPipeCount * RATE_PER_PIPE * diversity());
    }

    private HudModel hudModel() {
        return HudModel.of("Conflux Resonance Web")
                .formed(this::isFormed)
                .theme(net.phoenix.core.client.gui.hud.HudTheme.DATA)
                .hero(net.phoenix.core.client.gui.hud.Heroes.constellation(getBlockPos().asLong(),
                        () -> distinctTypeCount / 5.0f, net.phoenix.core.client.gui.hud.HudTheme.DATA.accent()), 36)
                .badge(() -> webActive ? new HudModel.Badge("SCANNING", HudModel.GREEN) :
                        new HudModel.Badge("OFFLINE", HudModel.RED))
                .subtitle(() -> "Reads the Axiom pipes within " + SCAN_RADIUS + " blocks")
                .cells(() -> {
                    var cells = new java.util.ArrayList<HudModel.Cell>();
                    cells.add(HudModel.cell("PIPES", String.format(java.util.Locale.ROOT, "%,d", totalPipeCount),
                            "in range", HudModel.CYAN));
                    cells.add(HudModel.cell("TYPES", distinctTypeCount + " / 5", "kinds of data", HudModel.PURPLE));
                    cells.add(HudModel.cell("DIVERSITY", String.format(java.util.Locale.ROOT, "+%.0f%%",
                            (diversity() - 1f) * 100), "bonus", HudModel.GOLD));
                    cells.add(
                            HudModel.cell("RATE / TYPE", String.format(java.util.Locale.ROOT, "%,d u/t", ratePerType()),
                                    "each type", HudModel.GREEN));
                    cells.add(HudModel.cell("POWER", String.format(java.util.Locale.ROOT, "%,d EU/t", EU_PER_TICK),
                            webActive ? "supplied" : "SHORT", webActive ? HudModel.GREEN : HudModel.RED));
                    return cells;
                })
                .bars(() -> java.util.List.of(new HudModel.Bar("Types active", distinctTypeCount / 5.0f,
                        distinctTypeCount + " / 5", HudModel.PURPLE)))
                .notes(() -> java.util.List.of(totalPipeCount > 0 ?
                        HudModel.line("Scans every " + SCAN_INTERVAL + " ticks.", HudModel.MID) :
                        HudModel.line("No Axiom pipes detected in range.", HudModel.GOLD)))
                .math(() -> java.util.List.of(
                        HudModel.heading("RATE"),
                        HudModel.line("diversity = 1 + 0.25 * types", HudModel.MID),
                        HudModel.line(String.format(java.util.Locale.ROOT, "= 1 + 0.25 * %d = %.2f", distinctTypeCount,
                                diversity()), HudModel.WHITE),
                        HudModel.line("rate per type = pipes * " + RATE_PER_PIPE + " * diversity", HudModel.MID),
                        HudModel.line(String.format(java.util.Locale.ROOT, "= %d * %d * %.2f = %,d u/t", totalPipeCount,
                                RATE_PER_PIPE, diversity(), ratePerType()), HudModel.WHITE),
                        HudModel.heading("LIMITS"),
                        HudModel.line(
                                String.format(java.util.Locale.ROOT, "buffer %,d per type, push %,d u/t", BUFFER_PER,
                                        PUSH_RATE),
                                HudModel.MID)))
                .graph("pipes", HudModel.CYAN, () -> (float) totalPipeCount, 0.0f)
                .graph("rate / type", HudModel.GREEN, () -> (float) ratePerType(), 0.0f)
                .graph("types", HudModel.PURPLE, () -> (float) distinctTypeCount, 5.0f);
    }

    @Override
    public void formStructure(@org.jetbrains.annotations.NotNull String substructureName) {
        super.formStructure(substructureName);
        scanCooldown = 0;
    }

    @Override
    public void invalidateStructure(@org.jetbrains.annotations.NotNull String substructureName) {
        super.invalidateStructure(substructureName);
        webActive = false;
        totalPipeCount = 0;
        distinctTypeCount = 0;
    }

    public int getTotalPipeCount() {
        return totalPipeCount;
    }

    public int getDistinctTypeCount() {
        return distinctTypeCount;
    }

    public boolean isWebActive() {
        return webActive;
    }
}
