package net.phoenix.core.integration.astral.puzzle;


import net.neoforged.fml.common.EventBusSubscriber;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.phoenix.core.integration.astral.skein.AstralThreadHatchPartMachine;
import net.phoenix.core.common.data.materials.AstralMaterials;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.network.packet.C2SOpenCalibrationPacket;
import net.phoenix.core.network.packet.S2COpenCalibrationPacket;
import net.phoenix.core.network.packet.S2CCalibrationResultPacket;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.drawable.ItemDrawable;
import brachy.modularui.screen.RichTooltip;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.ButtonWidget;
import brachy.modularui.widgets.TextWidget;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@EventBusSubscriber(modid = "phoenixcore")
public abstract class AstralCalibratedMachine extends WorkableElectricMultiblockMachine implements IMuiMachine {

    public static final float ALIGNED_DURATION = 0.85f;
    private static final int WRONG_COOLDOWN_TICKS = 40;

    @SaveField
    private long chartSeed = 0L;

    @SaveField
    @SyncToClient
    private boolean calibrated = false;

    @SaveField
    @SyncToClient
    private boolean aligned = false;

    @SaveField
    private long cooldownUntil = 0L;

    private @Nullable StarChart chart;

    protected AstralCalibratedMachine(BlockEntityCreationInfo info) {
        super(info);
    }

    public abstract StarChart.Kind chartKind();

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote() && chartSeed == 0L) {
            chartSeed = ThreadLocalRandom.current().nextLong() | 1L;
            calibrated = true;
            markAsChanged();
        }
    }

    @SubscribeEvent
    public static void onPlaced(BlockEvent.EntityPlaceEvent event) {
        LevelAccessor level = event.getLevel();
        if (level.isClientSide()) return;
        BlockPos pos = event.getPos();
        if (MetaMachine.getMachine(level, pos) instanceof AstralCalibratedMachine machine) {
            machine.freshlyPlaced();
        }
    }

    private void freshlyPlaced() {
        chartSeed = ThreadLocalRandom.current().nextLong() | 1L;
        chart = null;
        calibrated = false;
        aligned = false;
        markAsChanged();
    }

    public long getChartSeed() {
        return chartSeed;
    }

    public boolean isCalibrated() {
        return calibrated;
    }

    public boolean isAligned() {
        return aligned;
    }

    public StarChart chart() {
        if (chart == null) chart = StarChart.generate(chartSeed, chartKind());
        return chart;
    }

    public int favouredPhase() {
        return favouredPhaseOf(chartSeed);
    }

    public static int favouredPhaseOf(long seed) {
        return (int) Math.floorMod(seed >>> 7, 8L);
    }

    private static final String[] PHASE_NAMES = { "Full Moon", "Waning Gibbous", "Last Quarter", "Waning Crescent",
            "New Moon", "Waxing Crescent", "First Quarter", "Waxing Gibbous" };

    public static String phaseName(int phase) {
        return PHASE_NAMES[Math.floorMod(phase, 8)];
    }

    private String scatter() {
        StringBuilder lost = new StringBuilder();
        for (var part : getParts()) {
            for (var trait : part.getAllTraits()) {
                if (trait instanceof NotifiableFluidTank tank && tank.getHandlerIO() == IO.IN) {
                    var drained = tank.drain(new FluidStack(AstralMaterials.SKEIN.getFluid(), 72),
                            IFluidHandler.FluidAction.EXECUTE);
                    if (!drained.isEmpty()) lost.append(drained.getAmount()).append(" mB of Skein ");
                }
            }
            if (part instanceof AstralThreadHatchPartMachine hatch && hatch.getThreadContainer() != null &&
                    hatch.getThreadContainer().getHandlerIO() == IO.IN) {
                int gone = Math.min(250, hatch.getThreadContainer().getThread());
                if (gone > 0) {
                    hatch.getThreadContainer().removeThread(gone);
                    lost.append(gone).append(" Thread ");
                }
            }
        }
        return lost.toString().trim();
    }

    @Override
    public boolean beforeWorking(@Nullable GTRecipe recipe) {
        if (!calibrated) return false;
        return super.beforeWorking(recipe);
    }

    public static ModifierFunction alignedModifier(MetaMachine machine, GTRecipe recipe) {
        if (machine instanceof AstralCalibratedMachine astral && astral.aligned) {
            return ModifierFunction.builder().durationMultiplier(ALIGNED_DURATION).build();
        }
        return ModifierFunction.IDENTITY;
    }

    public void openCalibration(ServerPlayer player) {
        if (!isFormed()) {
            player.displayClientMessage(Component.literal("§cThe machine has to be built first."), true);
            return;
        }
        StarChart c = chart();
        PacketDistributor.sendToPlayer(player, new S2COpenCalibrationPacket(getBlockPos(), c.kind(), c.packedStars(), calibrated));
    }

    public void attempt(ServerPlayer player, List<int[]> drawn) {
        if (!isFormed() || getLevel() == null) return;
        if (drawn.size() > StarChart.MAX_LINES) return;
        if (getLevel().getGameTime() < cooldownUntil) {
            player.displayClientMessage(Component.literal("§cThe stars are still settling."), true);
            return;
        }
        StarChart c = chart();
        for (int[] l : drawn) {
            if (l.length != 2 || l[0] < 0 || l[1] < 0 || l[0] >= c.starCount() || l[1] >= c.starCount()) return;
        }
        int right = c.correct(drawn);
        boolean solved = c.solvedBy(drawn);
        String lost = "";
        if (solved) {
            calibrated = true;
            aligned = getLevel().getMoonPhase() == favouredPhase();
            markAsChanged();
        } else {
            cooldownUntil = getLevel().getGameTime() + WRONG_COOLDOWN_TICKS;
            if (!calibrated) lost = scatter();
            markAsChanged();
        }
        PacketDistributor.sendToPlayer(player, new S2CCalibrationResultPacket(getBlockPos(), solved, aligned && solved, right, c.lineCount(),
                        drawn.size(), lost));
    }

    @Override
    public List<IWidget> getWidgetsForDisplay(PanelSyncManager syncManager) {
        List<IWidget> widgets = super.getWidgetsForDisplay(syncManager);
        if (!isFormed()) return widgets;
        widgets.add(new TextWidget<>(Text.dynamic(() -> calibrated ?
                Component.literal("§7Calibration: §acalibrated" + (aligned ? " §b(aligned, faster)" : "")) :
                Component.literal("§7Calibration: §cuncalibrated §7- it will not run"))));
        widgets.add(new TextWidget<>(Text.dynamic(() -> {
            int stored = 0;
            int capacity = 0;
            int hatches = 0;
            for (var part : getParts()) {
                if (part instanceof AstralThreadHatchPartMachine hatch && hatch.getThreadContainer() != null) {
                    stored += hatch.getThreadContainer().getThread();
                    capacity += hatch.getThreadContainer().getMaxThread();
                    hatches++;
                }
            }
            if (hatches == 0) return Component.literal("§7Thread: §8no thread hatch fitted");
            int pct = (int) (stored * 100L / Math.max(1, capacity));
            String bar = "|".repeat(pct / 10) + "§8" + "|".repeat(10 - pct / 10);
            return Component.literal("§7Thread: §d" + bar + " §f" + stored + " / " + capacity + " §7(" + hatches +
                    (hatches == 1 ? " hatch)" : " hatches)"));
        })));
        widgets.add(new ButtonWidget<>()
                .overlay(new ItemDrawable(new ItemStack(Items.SPYGLASS)).asIcon().size(16))
                .tooltip(new RichTooltip().addLine(Text.of(Component
                        .literal(calibrated ? "Recalibrate (draws a new chart over the old)" :
                                "Open the star chart to calibrate this machine"))))
                .onMousePressed((context, button) -> {
                    PacketDistributor.sendToServer(new C2SOpenCalibrationPacket(getBlockPos()));
                    return true;
                }));
        return widgets;
    }
}
