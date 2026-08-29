package net.phoenix.core.integration.drone;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.mui.MachineUIPanelBuilder;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.multiblock.MultiblockWorldSavedData;
import com.gregtechceu.gtceu.api.multiblock.pattern.PatternState;
import com.gregtechceu.gtceu.common.mui.GTGuiTextures;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.integration.drone.group.DroneConfig;
import net.phoenix.core.integration.drone.group.DroneControlTrait;
import net.phoenix.core.integration.drone.group.GroupDefinition;
import net.phoenix.core.integration.drone.mui.DroneTargetListSyncHandler;
import net.phoenix.core.integration.drone.network.DroneTargetView;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.drawable.ItemDrawable;
import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.BooleanSyncValue;
import brachy.modularui.value.sync.IntSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widget.ScrollWidget;
import brachy.modularui.widget.scroll.VerticalScrollData;
import brachy.modularui.widgets.ButtonWidget;
import brachy.modularui.widgets.ToggleButton;
import brachy.modularui.widgets.dynamic.DynamicHandler;
import brachy.modularui.widgets.dynamic.DynamicWidget;
import brachy.modularui.widgets.layout.Flow;

import java.util.ArrayList;
import java.util.List;

public class DroneControllerMachine extends MultiblockControllerMachine implements IMuiMachine {

    public static final int RADIUS = 24;
    private static final int SCAN_INTERVAL_TICKS = 20;

    private static final int MAX_ROWS = 18;
    private static final int ROW_H = 46;

    private final DroneControlTrait control;

    private TickableSubscription scanSub;
    private int tickCounter = 0;
    private List<DroneTargetView> currentTargets = List.of();
    private boolean targetsDirty = true;
    private int cutoffPriority = 0;

    public DroneControllerMachine(BlockEntityCreationInfo info) {
        super(info);
        this.control = attachTrait(new DroneControlTrait());
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) {
            scanSub = subscribeServerTick(scanSub, this::scanTick);
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (scanSub != null) {
            scanSub.unsubscribe();
            scanSub = null;
        }
    }

    private void scanTick() {
        if (++tickCounter < SCAN_INTERVAL_TICKS) return;
        tickCounter = 0;
        rescan();
    }

    public void forceRescan() {
        rescan();
    }

    private void rescan() {
        if (!isFormed() || !(getLevel() instanceof ServerLevel level)) return;

        List<DroneTargetView> views = new ArrayList<>();
        for (MultiblockControllerMachine controller : findNearby(level, getBlockPos())) {
            views.add(toView(controller, control.config()));
        }
        this.currentTargets = views;
        this.targetsDirty = true;
    }

    private void cutoffBelowPriority(int threshold) {
        if (!(getLevel() instanceof ServerLevel level)) return;
        DroneConfig config = control.config();
        for (MultiblockControllerMachine controller : findNearby(level, getBlockPos())) {
            if (!(controller instanceof WorkableMultiblockMachine workable)) continue;
            if (config.getEffectivePriority(controller.getBlockPos()) >= threshold) continue;
            workable.getRecipeLogic().interruptRecipe();
        }
        rescan();
    }

    @Override
    public MachineUIPanelBuilder getPanelBuilder(PosGuiData data, PanelSyncManager syncManager,
                                                 UISettings settings) {
        BooleanSyncValue cutTrigger = syncManager.getOrCreateSyncHandler("droneCutTrigger", BooleanSyncValue.class,
                () -> new BooleanSyncValue(() -> false, fired -> {
                    if (fired) cutoffBelowPriority(cutoffPriority);
                }).allowC2S(true));

        return MachineUIPanelBuilder.panelBuilder(this)
                .rightConfigurators(configurators -> configurators
                        .child(new ButtonWidget<>()
                                .size(18)
                                .background(GTGuiTextures.BUTTON)
                                .overlay(GTGuiTextures.CYCLE_BUTTON)
                                .tooltip(t -> t.addLine(Component.literal("Rescan now").withStyle(ChatFormatting.GRAY)))
                                .onMousePressed((ctx, btn) -> {
                                    if (!isRemote()) rescan();
                                    return true;
                                }))
                        .child(new ToggleButton()
                                .value(cutTrigger)
                                .size(18)
                                .background(GTGuiTextures.BUTTON_POWER[0])
                                .background(true, GTGuiTextures.BUTTON_POWER[1])
                                .tooltip(t -> t.addLine(Component
                                        .literal("Cut power below the current priority threshold")
                                        .withStyle(ChatFormatting.GRAY)))));
    }

    @Override
    public void buildMainUI(ParentWidget<?> mainWidget, PosGuiData guiData, PanelSyncManager syncManager,
                            UISettings settings) {
        if (!isRemote()) rescan();
        else mainWidget.background(new net.phoenix.core.integration.drone.client.DroneUIBackground());

        DroneTargetListSyncHandler listSync = syncManager.getOrCreateSyncHandler("droneTargets",
                DroneTargetListSyncHandler.class,
                () -> new DroneTargetListSyncHandler(() -> {
                    boolean dirty = targetsDirty;
                    targetsDirty = false;
                    return dirty;
                }, () -> currentTargets));

        List<BooleanSyncValue> toggleSlots = new ArrayList<>(MAX_ROWS);
        List<BooleanSyncValue> cycleSlots = new ArrayList<>(MAX_ROWS);
        for (int i = 0; i < MAX_ROWS; i++) {
            final int idx = i;
            toggleSlots.add(syncManager.getOrCreateSyncHandler("droneToggle" + idx, BooleanSyncValue.class,
                    () -> new BooleanSyncValue(
                            () -> idx < currentTargets.size() && currentTargets.get(idx).workingEnabled(),
                            enabled -> {
                                if (idx >= currentTargets.size()) return;
                                DroneTargetView v = currentTargets.get(idx);
                                if (MetaMachine.getMachine(getLevel(),
                                        v.pos()) instanceof WorkableMultiblockMachine target) {
                                    target.getRecipeLogic().setWorkingEnabled(enabled);
                                    rescan();
                                }
                            }).allowC2S(true)));
            cycleSlots.add(syncManager.getOrCreateSyncHandler("droneCycle" + idx, BooleanSyncValue.class,
                    () -> new BooleanSyncValue(() -> false, fired -> {
                        if (!fired || idx >= currentTargets.size()) return;
                        DroneTargetView v = currentTargets.get(idx);
                        if (MetaMachine.getMachine(getLevel(), v.pos()) instanceof WorkableMultiblockMachine target &&
                                target.getRecipeTypes().length > 1) {
                            target.cycleActiveRecipeType();
                            rescan();
                        }
                    }).allowC2S(true)));
        }

        DynamicHandler listHandler = new DynamicHandler()
                .widgetProvider(() -> buildTargetListWidget(listSync.getTargets(), toggleSlots, cycleSlots));
        listSync.onChanged(listHandler::notifyUpdate);

        mainWidget.child(Text.str("Drone Control - range " + RADIUS)
                .asWidget()
                .pos(8, 6)
                .size(200, 10)
                .color(0xFFFFE0A8));

        mainWidget.child(buildCutoffRow(syncManager));

        mainWidget.child(new ScrollWidget<>(new VerticalScrollData())
                .pos(4, 44)
                .size(300, ROW_H * 5)
                .child(new DynamicWidget<>().clientOnlyHandler(listHandler)));
    }

    private Flow buildCutoffRow(PanelSyncManager syncManager) {
        IntSyncValue cutoffValue = syncManager.getOrCreateSyncHandler("droneCutoff", IntSyncValue.class,
                () -> new IntSyncValue(() -> cutoffPriority, v -> cutoffPriority = v).allowC2S(true));

        Flow row = Flow.row().pos(4, 20).size(300, 18);
        row.child(new ButtonWidget<>()
                .pos(0, 0)
                .size(16, 16)
                .background(GTGuiTextures.BUTTON)
                .overlay(GTGuiTextures.BUTTON_THROTTLE_MINUS)
                .tooltip(t -> t.addLine(Component.literal("Lower the cutoff priority threshold")
                        .withStyle(ChatFormatting.GRAY)))
                .onMousePressed((ctx, btn) -> {
                    cutoffValue.setIntValue(Math.max(0, cutoffValue.getIntValue() - 1), true, true);
                    return true;
                }));
        row.child(Text.dynamic(() -> Component.literal("Cutoff: " + cutoffValue.getIntValue()))
                .asWidget()
                .pos(20, 3)
                .size(90, 10));
        row.child(new ButtonWidget<>()
                .pos(114, 0)
                .size(16, 16)
                .background(GTGuiTextures.BUTTON)
                .overlay(GTGuiTextures.BUTTON_THROTTLE_PLUS)
                .tooltip(t -> t.addLine(Component.literal("Raise the cutoff priority threshold")
                        .withStyle(ChatFormatting.GRAY)))
                .onMousePressed((ctx, btn) -> {
                    cutoffValue.setIntValue(Math.min(10, cutoffValue.getIntValue() + 1), true, true);
                    return true;
                }));

        return row;
    }

    private Flow buildTargetListWidget(List<DroneTargetView> targets, List<BooleanSyncValue> toggleSlots,
                                       List<BooleanSyncValue> cycleSlots) {
        Flow col = Flow.col().size(300, Math.max(1, Math.min(targets.size(), MAX_ROWS)) * ROW_H);
        if (targets.isEmpty()) {
            col.child(Text.str("No formed multiblocks in range.").asWidget().pos(2, 2).color(0xFFA88FD9));
            return col;
        }
        for (int i = 0; i < targets.size() && i < MAX_ROWS; i++) {
            col.child(buildTargetRow(targets.get(i), i, toggleSlots.get(i), cycleSlots.get(i)));
        }
        return col;
    }

    private Flow buildTargetRow(DroneTargetView view, int rowIndex, BooleanSyncValue toggle,
                                BooleanSyncValue cycle) {
        Flow row = Flow.row().pos(0, rowIndex * ROW_H).size(300, ROW_H - 2);

        row.child(new ItemDrawable(view.icon()).asWidget().pos(2, 4).size(18, 18));

        int textX = 24;
        row.child(Text.str(view.name()).asWidget().pos(textX, 2).size(180, 10).color(0xFFFFFFFF));
        String statusLabel = view.controllable() ? (view.workingEnabled() ? view.status() : "DISABLED") : "N/A";
        row.child(Text.str(statusLabel).asWidget().pos(textX, 13).size(180, 10).color(statusColor(view)));
        String groupLabel = view.groupName().isEmpty() ? "Ungrouped" : view.groupName();
        row.child(Text.str(groupLabel + " - Priority " + view.priority())
                .asWidget()
                .pos(textX, 24)
                .size(180, 10)
                .color(0xFFA88FD9));
        if (view.electric()) {
            row.child(Text.str("In " + view.inputVoltage() + " / Out " + view.outputVoltage() + " EU/t")
                    .asWidget()
                    .pos(textX, 34)
                    .size(180, 10)
                    .color(0xFFA88FD9));
        }

        if (view.controllable()) {
            row.child(new ToggleButton()
                    .value(toggle)
                    .pos(210, 4)
                    .size(16, 16)
                    .background(GTGuiTextures.BUTTON_POWER[0])
                    .background(true, GTGuiTextures.BUTTON_POWER[1])
                    .tooltip(t -> t.addLine(Component
                            .literal(
                                    view.workingEnabled() ? "Enabled - click to disable" : "Disabled - click to enable")
                            .withStyle(ChatFormatting.GRAY))));
        }

        if (view.multiRecipe()) {
            row.child(new ToggleButton()
                    .value(cycle)
                    .pos(232, 4)
                    .size(16, 16)
                    .background(GTGuiTextures.BUTTON)
                    .overlay(GTGuiTextures.CYCLE_BUTTON)
                    .tooltip(t -> t.addLine(
                            Component.literal("Recipe mode: " + view.recipeTypeName())
                                    .withStyle(ChatFormatting.GRAY))));
        }

        return row;
    }

    private int statusColor(DroneTargetView view) {
        if (!view.controllable()) return 0xFFA88FD9;
        if (!view.workingEnabled()) return 0xFFFF6B5C;
        return switch (view.status()) {
            case "WORKING" -> 0xFF5CFF7A;
            case "WAITING" -> 0xFFFFD95C;
            case "SUSPEND" -> 0xFFFF9C5C;
            default -> 0xFFA88FD9;
        };
    }

    public DroneControlTrait getControlTrait() {
        return control;
    }

    public static List<MultiblockControllerMachine> findNearby(ServerLevel level, BlockPos center) {
        List<MultiblockControllerMachine> out = new ArrayList<>();
        double radiusSq = (double) RADIUS * RADIUS;
        for (var states : MultiblockWorldSavedData.getOrCreate(level).mapping.values()) {
            for (PatternState state : states) {
                if (!state.isFormed()) continue;
                MultiblockControllerMachine controller = state.getController();
                BlockPos controllerPos = state.getControllerPos();
                if (controller == null || controllerPos == null) continue;
                if (controllerPos.equals(center)) continue;
                if (controllerPos.distSqr(center) > radiusSq) continue;
                out.add(controller);
            }
        }
        return out;
    }

    public static DroneTargetView toView(MultiblockControllerMachine controller, DroneConfig config) {
        BlockPos pos = controller.getBlockPos();
        String name = controller.getBlockState().getBlock().getName().getString();
        ItemStack icon = new ItemStack(controller.getBlockState().getBlock().asItem());

        boolean controllable = false;
        boolean workingEnabled = false;
        String status = "N/A";
        int progress = 0;
        int duration = 0;
        String recipeTypeName = "";
        boolean multiRecipe = false;
        boolean electric = false;
        long inputVoltage = 0;
        long outputVoltage = 0;

        if (controller instanceof WorkableMultiblockMachine workable) {
            controllable = true;
            workingEnabled = workable.getRecipeLogic().isWorkingEnabled();
            status = workable.getRecipeLogic().getStatus().name();
            progress = workable.getRecipeLogic().getProgress();
            duration = workable.getRecipeLogic().getDuration();
            multiRecipe = workable.getRecipeTypes().length > 1;
            recipeTypeName = workable.getRecipeType().registryName.getPath();
        }
        if (controller instanceof WorkableElectricMultiblockMachine electricMachine) {
            electric = true;
            var container = electricMachine.getEnergyContainer();
            inputVoltage = container.getInputVoltage();
            outputVoltage = container.getOutputVoltage();
        }

        String groupName = "";
        for (GroupDefinition group : config.groups) {
            if (group.id().equals(config.getAssignment(pos).groupId())) {
                groupName = group.name();
                break;
            }
        }
        int priority = config.getAssignment(pos).priority();

        return new DroneTargetView(pos, name, icon, controllable, workingEnabled, status, progress, duration,
                recipeTypeName, multiRecipe, electric, inputVoltage, outputVoltage, groupName, priority);
    }
}
