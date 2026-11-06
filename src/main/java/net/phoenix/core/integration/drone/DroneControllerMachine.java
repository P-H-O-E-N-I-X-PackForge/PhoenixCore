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
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.common.mui.GTGuiTextures;
import com.gregtechceu.gtceu.utils.ExtendedUseOnContext;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.integration.drone.group.DroneConfig;
import net.phoenix.core.integration.drone.group.DroneControlTrait;
import net.phoenix.core.integration.drone.group.GroupDefinition;
import net.phoenix.core.integration.drone.mui.DroneTargetListSyncHandler;
import net.phoenix.core.integration.drone.network.DroneTargetView;

import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.api.drawable.Text;
import brachy.modularui.drawable.ItemDrawable;
import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.UISettings;
import brachy.modularui.screen.viewport.GuiContext;
import brachy.modularui.theme.WidgetTheme;
import brachy.modularui.value.sync.BooleanSyncValue;
import brachy.modularui.value.sync.IntSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.value.sync.StringSyncValue;
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
    public static final int MODULE_SLOTS = 3;

    @SaveField
    @SyncToClient
    private String modules = "";
    private static final int SCAN_INTERVAL_TICKS = 20;

    private static final int MAX_ROWS = 18;
    private static final int ROW_H = 46;

    private static final int COLOR_TITLE = 0xFFFFE0A8;
    private static final int COLOR_LABEL = 0xFFA88FD9;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_OK = 0xFF5CFF7A;
    private static final int COLOR_WARN = 0xFFFFD95C;
    private static final int COLOR_BAD = 0xFFFF6B5C;
    private static final int COLOR_PANEL_BG = 0xEE1C1730;
    private static final int COLOR_PANEL_BORDER = 0xFF4A3F7A;
    private static final int COLOR_ROW_BG = 0xCC241D3D;

    private record FlatPanel(int fillColor, int borderColor) implements IDrawable {

        @Override
        public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme widgetTheme) {
            var g = context.getGraphics();
            g.fill(x, y, x + width, y + height, fillColor);
            if (borderColor != 0) {
                g.fill(x, y, x + width, y + 1, borderColor);
                g.fill(x, y + height - 1, x + width, y + height, borderColor);
                g.fill(x, y, x + 1, y + height, borderColor);
                g.fill(x + width - 1, y, x + width, y + height, borderColor);
            }
        }
    }

    private final DroneControlTrait control;

    private List<String> moduleIds() {
        List<String> out = new ArrayList<>();
        for (String id : modules.split(",")) {
            if (!id.isBlank()) out.add(id.trim());
        }
        return out;
    }

    public int range() {
        int range = RADIUS;
        for (String id : moduleIds()) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl != null && BuiltInRegistries.ITEM.get(rl) instanceof DroneRangeModuleItem module) {
                range += module.bonus();
            }
        }
        return range;
    }

    public int moduleCount() {
        return moduleIds().size();
    }

    @Override
    public InteractionResult onUseWithItem(ExtendedUseOnContext context) {
        ItemStack held = context.getItemInHand();
        var player = context.getPlayer();
        if (player != null && held.getItem() instanceof DroneRangeModuleItem) {
            if (!isRemote()) {
                if (moduleCount() >= MODULE_SLOTS) {
                    player.displayClientMessage(Component
                            .literal("All " + MODULE_SLOTS +
                                    " module slots are full (crouch and use an empty hand to take one out).")
                            .withStyle(ChatFormatting.RED), true);
                } else {
                    List<String> ids = moduleIds();
                    ids.add(BuiltInRegistries.ITEM.getKey(held.getItem()).toString());
                    modules = String.join(",", ids);
                    if (!player.isCreative()) held.shrink(1);
                    previousTargets = null;
                    forceRescan();
                    player.displayClientMessage(Component.literal("Module installed: range is now " + range())
                            .withStyle(ChatFormatting.AQUA), true);
                }
            }
            return InteractionResult.sidedSuccess(isRemote());
        }
        return super.onUseWithItem(context);
    }

    @Override
    public InteractionResult onUse(ExtendedUseOnContext context) {
        var player = context.getPlayer();
        if (player != null && player.isCrouching() && context.getItemInHand().isEmpty() && moduleCount() > 0) {
            if (!isRemote()) {
                List<String> ids = moduleIds();
                String last = ids.remove(ids.size() - 1);
                modules = String.join(",", ids);
                ResourceLocation rl = ResourceLocation.tryParse(last);
                if (rl != null) player.getInventory().placeItemBackInInventory(
                        new ItemStack(BuiltInRegistries.ITEM.get(rl)));
                previousTargets = null;
                forceRescan();
                player.displayClientMessage(Component.literal("Module removed: range is now " + range())
                        .withStyle(ChatFormatting.AQUA), true);
            }
            return InteractionResult.sidedSuccess(isRemote());
        }
        return super.onUse(context);
    }

    private TickableSubscription scanSub;
    private int tickCounter = 0;
    private List<DroneTargetView> currentTargets = List.of();
    private boolean targetsDirty = true;
    private int cutoffPriority = 0;

    @SaveField
    private boolean alerts = true;

    @SaveField
    private boolean autoShed = false;

    @SaveField
    private String shed = "";

    @SaveField
    private boolean keepLoaded = false;

    @SaveField
    private String heldChunks = "";

    private String search = "";
    private static final int MAX_HELD_CHUNKS = 16;
    private static final int MAX_SEARCH_HITS = 150;
    private static final long STUCK_TICKS = 200;
    private static final long REALERT_TICKS = 3600;
    private static final double SHED_BELOW = 0.10;
    private static final double RESTORE_ABOVE = 0.40;
    private final java.util.Map<BlockPos, Long> stuckSince = new java.util.HashMap<>();
    private final java.util.Map<BlockPos, Long> lastAlert = new java.util.HashMap<>();
    private java.util.Set<BlockPos> previousTargets = null;

    public DroneControllerMachine(BlockEntityCreationInfo info) {
        super(info);
        this.control = attachTrait(new DroneControlTrait());
    }

    @Override
    public void onLoad() {
        super.onLoad();
        unloading = false;
        if (!isRemote()) {
            scanSub = subscribeServerTick(scanSub, this::scanTick);
        }
    }

    @Override
    public void onUnload() {
        unloading = true;
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
        for (MultiblockControllerMachine controller : findNearby(level, getBlockPos(), range())) {
            views.add(toView(controller, control.config()));
        }
        this.currentTargets = views;
        this.targetsDirty = true;
        watchForProblems(level, views);
        if (autoShed) shedForPower(level);
        syncChunkLoading(level, views);
        sendSearch(level, views);
        sendOverlay(level, views);
    }

    private void alert(ServerLevel level, BlockPos at, String title, String message, boolean severe) {
        var packet = new net.phoenix.core.integration.drone.network.S2CDroneAlertPacket(at, title, message, severe);
        BlockPos here = getBlockPos();
        double reach = range() + 64.0;
        for (var player : level.players()) {
            if (player.distanceToSqr(here.getX(), here.getY(), here.getZ()) > reach * reach) continue;
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, packet);
        }
    }

    private void watchForProblems(ServerLevel level, List<DroneTargetView> views) {
        long now = level.getGameTime();
        java.util.Set<BlockPos> current = new java.util.HashSet<>();
        for (DroneTargetView v : views) {
            current.add(v.pos());
            boolean stuck = v.controllable() && v.workingEnabled() &&
                    (v.status().equals("WAITING") || v.status().equals("SUSPEND"));
            if (!stuck) {
                stuckSince.remove(v.pos());
                lastAlert.remove(v.pos());
                continue;
            }
            long since = stuckSince.computeIfAbsent(v.pos(), k -> now);
            if (!alerts || now - since < STUCK_TICKS) continue;
            Long last = lastAlert.get(v.pos());
            if (last != null && now - last < REALERT_TICKS) continue;
            lastAlert.put(v.pos(), now);
            boolean suspended = v.status().equals("SUSPEND");
            alert(level, v.pos(), v.name() + (suspended ? " is suspended" : " is waiting"),
                    (suspended ? "Out of power or blocked" : "Missing input, output space or power") + " at " +
                            v.pos().toShortString(),
                    suspended);
        }
        if (previousTargets != null && alerts) {
            for (BlockPos old : previousTargets) {
                if (current.contains(old)) continue;
                if (MetaMachine.getMachine(level, old) instanceof MultiblockControllerMachine lost &&
                        !lost.isFormed()) {
                    alert(level, old, "Structure lost", lost.getBlockState().getBlock().getName().getString() +
                            " at " + old.toShortString() + " is no longer formed", true);
                }
            }
        }
        stuckSince.keySet().retainAll(current);
        lastAlert.keySet().retainAll(current);
        previousTargets = current;
    }

    private java.util.Set<Long> heldChunkSet() {
        java.util.Set<Long> out = new java.util.HashSet<>();
        for (String part : heldChunks.split(",")) {
            if (part.isBlank()) continue;
            try {
                out.add(Long.parseLong(part.trim()));
            } catch (NumberFormatException ignored) {}
        }
        return out;
    }

    private void releaseChunks(ServerLevel level) {
        for (long chunk : heldChunkSet()) DroneChunkLoader.set(level, getBlockPos(), chunk, false);
        heldChunks = "";
    }

    private void syncChunkLoading(ServerLevel level, List<DroneTargetView> views) {
        java.util.Set<Long> held = heldChunkSet();
        if (!keepLoaded) {
            if (!held.isEmpty()) releaseChunks(level);
            return;
        }
        java.util.LinkedHashSet<Long> wanted = new java.util.LinkedHashSet<>();
        wanted.add(new net.minecraft.world.level.ChunkPos(getBlockPos()).toLong());
        List<DroneTargetView> sorted = new ArrayList<>(views);
        BlockPos here = getBlockPos();
        sorted.sort(java.util.Comparator.comparingDouble(v -> v.pos().distSqr(here)));
        for (DroneTargetView v : sorted) {
            if (wanted.size() >= MAX_HELD_CHUNKS) break;
            wanted.add(new net.minecraft.world.level.ChunkPos(v.pos()).toLong());
        }
        for (long chunk : held) {
            if (!wanted.contains(chunk)) DroneChunkLoader.set(level, here, chunk, false);
        }
        for (long chunk : wanted) {
            if (!held.contains(chunk)) DroneChunkLoader.set(level, here, chunk, true);
        }
        StringBuilder sb = new StringBuilder();
        for (long chunk : wanted) {
            if (sb.length() > 0) sb.append(',');
            sb.append(chunk);
        }
        heldChunks = sb.toString();
    }

    @Override
    public void invalidateStructure(@org.jetbrains.annotations.NotNull String name) {
        super.invalidateStructure(name);
        if (!unloading && !isRemote() && getLevel() instanceof ServerLevel level && !heldChunks.isEmpty())
            releaseChunks(level);
    }

    private boolean unloading = false;
    private boolean searchWasActive = false;

    private void sendSearch(ServerLevel level, List<DroneTargetView> views) {
        String q = search.trim().toLowerCase(java.util.Locale.ROOT).replace(' ', '_');
        if (q.isEmpty()) {
            if (searchWasActive) {
                searchWasActive = false;
                broadcastSearch(level,
                        new net.phoenix.core.integration.drone.network.S2CDroneSearchPacket(getBlockPos(),
                                "", List.of()));
            }
            return;
        }
        searchWasActive = true;
        List<net.phoenix.core.integration.drone.network.S2CDroneSearchPacket.Hit> hits = new ArrayList<>();
        java.util.Set<BlockPos> seen = new java.util.HashSet<>();
        for (DroneTargetView v : views) {
            if (!(MetaMachine.getMachine(level, v.pos()) instanceof MultiblockControllerMachine controller)) continue;
            List<BlockPos> spots = new ArrayList<>();
            spots.add(controller.getBlockPos());
            for (var part : controller.getParts()) spots.add(part.self().getBlockPos());
            for (BlockPos spot : spots) {
                if (hits.size() >= MAX_SEARCH_HITS) break;
                if (!seen.add(spot)) continue;
                String found = searchAt(level, spot, q);
                if (found != null) {
                    hits.add(new net.phoenix.core.integration.drone.network.S2CDroneSearchPacket.Hit(spot, found));
                }
            }
        }
        broadcastSearch(level,
                new net.phoenix.core.integration.drone.network.S2CDroneSearchPacket(getBlockPos(), search.trim(),
                        hits));
    }

    private String searchAt(ServerLevel level, BlockPos spot, String q) {
        StringBuilder out = new StringBuilder();
        var items = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, spot, null);
        if (items != null) {
            java.util.Map<String, Long> totals = new java.util.LinkedHashMap<>();
            for (int i = 0; i < items.getSlots(); i++) {
                ItemStack stack = items.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                String name = stack.getHoverName().getString();
                String key = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
                if (name.toLowerCase(java.util.Locale.ROOT).replace(' ', '_').contains(q) || key.contains(q)) {
                    totals.merge(name, (long) stack.getCount(), Long::sum);
                }
            }
            totals.forEach((n, c) -> out.append(out.length() > 0 ? ", " : "").append(n).append(" x")
                    .append(String.format("%,d", c)));
        }
        var fluids = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, spot,
                null);
        if (fluids != null) {
            java.util.Map<String, Long> totals = new java.util.LinkedHashMap<>();
            for (int i = 0; i < fluids.getTanks(); i++) {
                var fluid = fluids.getFluidInTank(i);
                if (fluid.isEmpty()) continue;
                String name = fluid.getHoverName().getString();
                String key = BuiltInRegistries.FLUID.getKey(fluid.getFluid()).getPath();
                if (name.toLowerCase(java.util.Locale.ROOT).replace(' ', '_').contains(q) || key.contains(q)) {
                    totals.merge(name, (long) fluid.getAmount(), Long::sum);
                }
            }
            totals.forEach((n, c) -> out.append(out.length() > 0 ? ", " : "").append(n).append(" ")
                    .append(String.format("%,d", c)).append(" mB"));
        }
        return out.length() == 0 ? null : out.toString();
    }

    private void broadcastSearch(ServerLevel level,
                                 net.phoenix.core.integration.drone.network.S2CDroneSearchPacket packet) {
        BlockPos here = getBlockPos();
        double reach = range() + 64.0;
        for (var player : level.players()) {
            if (player.distanceToSqr(here.getX(), here.getY(), here.getZ()) > reach * reach) continue;
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, packet);
        }
    }

    private java.util.Set<BlockPos> shedSet() {
        java.util.Set<BlockPos> out = new java.util.HashSet<>();
        for (String part : shed.split(";")) {
            String[] xyz = part.split(",");
            if (xyz.length != 3) continue;
            try {
                out.add(new BlockPos(Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2])));
            } catch (NumberFormatException ignored) {}
        }
        return out;
    }

    private void saveShed(java.util.Set<BlockPos> set) {
        StringBuilder sb = new StringBuilder();
        for (BlockPos p : set) {
            if (sb.length() > 0) sb.append(';');
            sb.append(p.getX()).append(',').append(p.getY()).append(',').append(p.getZ());
        }
        shed = sb.toString();
    }

    private void shedForPower(ServerLevel level) {
        long stored = 0, capacity = 0;
        for (MultiblockControllerMachine controller : findNearby(level, getBlockPos(), range())) {
            if (controller instanceof WorkableElectricMultiblockMachine electric) {
                var container = electric.getEnergyContainer();
                stored += container.getEnergyStored();
                capacity += container.getEnergyCapacity();
            }
        }
        if (capacity <= 0) return;
        double fill = stored / (double) capacity;
        java.util.Set<BlockPos> paused = shedSet();

        if (paused.isEmpty() && fill < SHED_BELOW && cutoffPriority > 0) {
            DroneConfig config = control.config();
            for (MultiblockControllerMachine controller : findNearby(level, getBlockPos(), range())) {
                if (!(controller instanceof WorkableMultiblockMachine workable)) continue;
                if (config.getEffectivePriority(controller.getBlockPos()) >= cutoffPriority) continue;
                if (!workable.getRecipeLogic().isWorkingEnabled()) continue;
                workable.getRecipeLogic().setWorkingEnabled(false);
                paused.add(controller.getBlockPos().immutable());
            }
            if (!paused.isEmpty()) {
                saveShed(paused);
                alert(level, getBlockPos(), "Power low",
                        "Paused " + paused.size() + " machine(s) below priority " + cutoffPriority + " (buffers at " +
                                Math.round(fill * 100) + "%)",
                        true);
            }
        } else if (!paused.isEmpty() && fill > RESTORE_ABOVE) {
            for (BlockPos p : paused) {
                if (MetaMachine.getMachine(level, p) instanceof WorkableMultiblockMachine workable) {
                    workable.getRecipeLogic().setWorkingEnabled(true);
                }
            }
            alert(level, getBlockPos(), "Power restored",
                    "Resumed " + paused.size() + " machine(s) (buffers at " + Math.round(fill * 100) + "%)", false);
            saveShed(new java.util.HashSet<>());
        }
    }

    private void sendFleet(ServerLevel level, List<DroneTargetView> views) {
        List<net.phoenix.core.integration.drone.network.S2CDroneFleetPacket.Target> targets = new ArrayList<>();
        for (DroneTargetView v : views) {
            targets.add(new net.phoenix.core.integration.drone.network.S2CDroneFleetPacket.Target(v.pos(),
                    net.phoenix.core.integration.drone.network.DroneStatus.colour(v)));
        }
        var packet = new net.phoenix.core.integration.drone.network.S2CDroneFleetPacket(getBlockPos(),
                1 + moduleCount(), targets);
        BlockPos here = getBlockPos();
        for (var player : level.players()) {
            if (player.distanceToSqr(here.getX(), here.getY(), here.getZ()) > 96.0 * 96.0) continue;
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, packet);
        }
    }

    private void sendOverlay(ServerLevel level, List<DroneTargetView> views) {
        sendFleet(level, views);
        double reach = range() + 64.0;
        net.phoenix.core.integration.drone.network.S2CDroneOverlayPacket packet = null;
        for (var player : level.players()) {
            if (player.distanceToSqr(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ()) > reach * reach)
                continue;
            var wand = DroneItems.DRONE_WAND.get();
            if (!player.getMainHandItem().is(wand) && !player.getOffhandItem().is(wand)) continue;
            if (packet == null) {
                packet = new net.phoenix.core.integration.drone.network.S2CDroneOverlayPacket(getBlockPos(), range(),
                        views);
            }
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, packet);
        }
    }

    private void cutoffBelowPriority(int threshold) {
        if (!(getLevel() instanceof ServerLevel level)) return;
        DroneConfig config = control.config();
        for (MultiblockControllerMachine controller : findNearby(level, getBlockPos(), range())) {
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
    public brachy.modularui.screen.ModularPanel<?> buildUI(PosGuiData data, PanelSyncManager syncManager,
                                                           UISettings settings) {
        return net.phoenix.core.client.gui.hud.MachineHud.build(this, data, syncManager, settings, (id, value) -> {},
                this::hudModel, new net.phoenix.core.client.gui.hud.MachineHud.Classic("C", 308, 144,
                        content -> buildMainUI(content, data, syncManager, settings)));
    }

    private net.phoenix.core.client.gui.hud.HudModel hudModel() {
        var T = net.phoenix.core.client.gui.hud.HudTheme.DRONE;
        return net.phoenix.core.client.gui.hud.HudModel.of("Drone Control")
                .theme(T)
                .hero(net.phoenix.core.client.gui.hud.Heroes.radar(() -> targets().size(), T.accent()), 40)
                .formed(this::isFormed)
                .badge(() -> targets().isEmpty() ? new net.phoenix.core.client.gui.hud.HudModel.Badge("NO TARGETS",
                        net.phoenix.core.client.gui.hud.HudModel.DIM) :
                        new net.phoenix.core.client.gui.hud.HudModel.Badge("LINKED",
                                net.phoenix.core.client.gui.hud.HudModel.GREEN))
                .subtitle(() -> "Watches formed multiblocks within " + range() + " blocks")
                .cells(() -> {
                    var list = targets();
                    long running = list.stream().filter(v -> v.controllable() && v.workingEnabled()).count();
                    long paused = list.stream().filter(v -> v.controllable() && !v.workingEnabled()).count();
                    var cells = new ArrayList<net.phoenix.core.client.gui.hud.HudModel.Cell>();
                    cells.add(net.phoenix.core.client.gui.hud.HudModel.cell("TARGETS", String.valueOf(list.size()),
                            "in range", net.phoenix.core.client.gui.hud.HudModel.CYAN));
                    cells.add(net.phoenix.core.client.gui.hud.HudModel.cell("ENABLED", String.valueOf(running),
                            "working allowed", net.phoenix.core.client.gui.hud.HudModel.GREEN));
                    cells.add(net.phoenix.core.client.gui.hud.HudModel.cell("DISABLED", String.valueOf(paused),
                            "switched off", paused > 0 ? net.phoenix.core.client.gui.hud.HudModel.GOLD :
                                    net.phoenix.core.client.gui.hud.HudModel.DIM));
                    long draw = 0, gen = 0;
                    for (DroneTargetView v : list) {
                        if (v.euPerTick() > 0) draw += v.euPerTick();
                        else gen -= v.euPerTick();
                    }
                    cells.add(net.phoenix.core.integration.drone.network.DronePowerCell.cell(draw, gen));
                    return cells;
                })
                .bars(() -> {
                    var list = targets();
                    long controllable = list.stream().filter(DroneTargetView::controllable).count();
                    long running = list.stream().filter(v -> v.controllable() && v.workingEnabled()).count();
                    var bufs = list.stream().filter(v -> v.bufferPercent() >= 0)
                            .mapToInt(DroneTargetView::bufferPercent)
                            .toArray();
                    int avg = bufs.length == 0 ? 0 : (int) java.util.Arrays.stream(bufs).average().orElse(0);
                    return List.of(new net.phoenix.core.client.gui.hud.HudModel.Bar("Enabled share",
                            controllable == 0 ? 0.0f : running / (float) controllable, running + " / " + controllable,
                            net.phoenix.core.client.gui.hud.HudModel.GREEN),
                            new net.phoenix.core.client.gui.hud.HudModel.Bar("Power buffers", avg / 100f,
                                    bufs.length == 0 ? "none" : avg + "%",
                                    avg < 15 ? net.phoenix.core.client.gui.hud.HudModel.GOLD :
                                            net.phoenix.core.client.gui.hud.HudModel.CYAN));
                })
                .notes(() -> List.of(net.phoenix.core.client.gui.hud.HudModel.line(
                        "Cut power below priority " + cutoffPriority + "; rows are on the C tab.",
                        net.phoenix.core.client.gui.hud.HudModel.MID)));
    }

    private List<DroneTargetView> targets() {
        return clientList != null ? clientList.getTargets() : currentTargets;
    }

    private DroneTargetListSyncHandler clientList;

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

        if (isRemote()) clientList = listSync;
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

        mainWidget.child(Text.str("Drone Control - range " + range())
                .asWidget()
                .pos(8, 6)
                .size(200, 10)
                .color(COLOR_TITLE));

        mainWidget.child(buildCutoffRow(syncManager)
                .background(new FlatPanel(COLOR_PANEL_BG, COLOR_PANEL_BORDER)));

        mainWidget.child(new ScrollWidget<>(new VerticalScrollData())
                .pos(4, 44)
                .size(300, ROW_H * 5)
                .background(new FlatPanel(COLOR_PANEL_BG, COLOR_PANEL_BORDER))
                .child(new DynamicWidget<>().clientOnlyHandler(listHandler)));
    }

    private Flow buildCutoffRow(PanelSyncManager syncManager) {
        IntSyncValue cutoffValue = syncManager.getOrCreateSyncHandler("droneCutoff", IntSyncValue.class,
                () -> new IntSyncValue(() -> cutoffPriority, v -> cutoffPriority = v).allowC2S(true));

        Flow row = Flow.row().pos(4, 20).size(300, 18);
        row.child(new ButtonWidget<>()
                .pos(2, 1)
                .size(16, 16)
                .background(GTGuiTextures.BUTTON)
                .overlay(Text.str("-").asIcon())
                .tooltip(t -> t.addLine(Component.literal("Lower the cutoff priority threshold")
                        .withStyle(ChatFormatting.GRAY)))
                .onMousePressed((ctx, btn) -> {
                    cutoffValue.setIntValue(Math.max(0, cutoffValue.getIntValue() - 1), true, true);
                    return true;
                }));
        row.child(Text.dynamic(() -> Component.literal("Cutoff: " + cutoffValue.getIntValue()))
                .asWidget()
                .pos(22, 4)
                .size(90, 10)
                .color(COLOR_TEXT));
        row.child(new ButtonWidget<>()
                .pos(116, 1)
                .size(16, 16)
                .background(GTGuiTextures.BUTTON)
                .overlay(Text.str("+").asIcon())
                .tooltip(t -> t.addLine(Component.literal("Raise the cutoff priority threshold")
                        .withStyle(ChatFormatting.GRAY)))
                .onMousePressed((ctx, btn) -> {
                    cutoffValue.setIntValue(Math.min(10, cutoffValue.getIntValue() + 1), true, true);
                    return true;
                }));

        BooleanSyncValue alertsValue = syncManager.getOrCreateSyncHandler("droneAlerts", BooleanSyncValue.class,
                () -> new BooleanSyncValue(() -> alerts, v -> alerts = v).allowC2S(true));
        BooleanSyncValue shedValue = syncManager.getOrCreateSyncHandler("droneAutoShed", BooleanSyncValue.class,
                () -> new BooleanSyncValue(() -> autoShed, v -> autoShed = v).allowC2S(true));
        BooleanSyncValue keepValue = syncManager.getOrCreateSyncHandler("droneKeepLoaded", BooleanSyncValue.class,
                () -> new BooleanSyncValue(() -> keepLoaded, v -> {
                    keepLoaded = v;
                    if (!isRemote()) rescan();
                }).allowC2S(true));
        row.child(new ToggleButton()
                .value(keepValue)
                .pos(190, 1)
                .size(16, 16)
                .background(GTGuiTextures.BUTTON)
                .overlay(Text.dynamic(() -> Component.literal("K").withStyle(
                        keepValue.getBoolValue() ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY)).asIcon())
                .tooltip(t -> t.addLine(Component.literal(
                        "Keep running: hold this drone's and the nearest 15 watched " + "machines' chunks loaded")
                        .withStyle(ChatFormatting.GRAY))));
        StringSyncValue searchValue = new StringSyncValue(() -> search, v -> {
            search = v;
            if (!isRemote()) rescan();
        });
        syncManager.syncValue("droneSearch", searchValue);
        row.child(new brachy.modularui.widgets.textfield.TextFieldWidget()
                .pos(210, 2)
                .size(88, 14)
                .value(searchValue)
                .hintText(Component.literal("Find item/fluid...")));
        row.child(new ToggleButton()
                .value(alertsValue)
                .pos(150, 1)
                .size(16, 16)
                .background(GTGuiTextures.BUTTON)
                .overlay(Text.dynamic(() -> Component.literal("A").withStyle(
                        alertsValue.getBoolValue() ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY)).asIcon())
                .tooltip(
                        t -> t.addLine(Component.literal("Alerts: toast when a machine is stuck or loses its structure")
                                .withStyle(ChatFormatting.GRAY))));
        row.child(new ToggleButton()
                .value(shedValue)
                .pos(170, 1)
                .size(16, 16)
                .background(GTGuiTextures.BUTTON)
                .overlay(Text.dynamic(() -> Component.literal("S").withStyle(
                        shedValue.getBoolValue() ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY)).asIcon())
                .tooltip(
                        t -> t.addLine(Component
                                .literal("Load shedding: pause machines below the cutoff priority " +
                                        "while power buffers are under 10%, resume over 40%")
                                .withStyle(ChatFormatting.GRAY))));

        return row;
    }

    private Flow buildTargetListWidget(List<DroneTargetView> targets, List<BooleanSyncValue> toggleSlots,
                                       List<BooleanSyncValue> cycleSlots) {
        Flow col = Flow.col().size(300, Math.max(1, Math.min(targets.size(), MAX_ROWS)) * ROW_H);
        if (targets.isEmpty()) {
            col.child(Text.str("No formed multiblocks in range.").asWidget().pos(4, 4).color(COLOR_LABEL));
            return col;
        }
        for (int i = 0; i < targets.size() && i < MAX_ROWS; i++) {
            col.child(buildTargetRow(targets.get(i), i, toggleSlots.get(i), cycleSlots.get(i)));
        }
        return col;
    }

    private Flow buildTargetRow(DroneTargetView view, int rowIndex, BooleanSyncValue toggle,
                                BooleanSyncValue cycle) {
        Flow row = Flow.row().pos(0, rowIndex * ROW_H).size(300, ROW_H - 2)
                .background(new FlatPanel(COLOR_ROW_BG, 0));

        row.child(new ItemDrawable(view.icon()).asWidget().pos(3, 4).size(18, 18));

        int textX = 25;
        row.child(Text.str(view.name()).asWidget().pos(textX, 3).size(180, 10).color(COLOR_TEXT));
        String statusLabel = view.controllable() ? (view.workingEnabled() ? view.status() : "DISABLED") : "N/A";
        row.child(Text.str(statusLabel).asWidget().pos(textX, 14).size(180, 10).color(statusColor(view)));
        String groupLabel = view.groupName().isEmpty() ? "Ungrouped" : view.groupName();
        row.child(Text.str(groupLabel + " - Priority " + view.priority())
                .asWidget()
                .pos(textX, 25)
                .size(180, 10)
                .color(COLOR_LABEL));
        if (view.electric()) {
            row.child(Text.str("In " + view.inputVoltage() + " / Out " + view.outputVoltage() + " EU/t")
                    .asWidget()
                    .pos(textX, 35)
                    .size(180, 10)
                    .color(COLOR_LABEL));
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
        if (!view.controllable()) return COLOR_LABEL;
        if (!view.workingEnabled()) return COLOR_BAD;
        return switch (view.status()) {
            case "WORKING" -> COLOR_OK;
            case "WAITING" -> COLOR_WARN;
            case "SUSPEND" -> 0xFFFF9C5C;
            default -> COLOR_LABEL;
        };
    }

    public DroneControlTrait getControlTrait() {
        return control;
    }

    public static List<MultiblockControllerMachine> findNearby(ServerLevel level, BlockPos center, int range) {
        List<MultiblockControllerMachine> out = new ArrayList<>();
        double radiusSq = (double) range * range;
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
            recipeTypeName = workable.getRecipeType().id.getPath();
        }
        if (controller instanceof WorkableElectricMultiblockMachine electricMachine) {
            electric = true;
            var container = electricMachine.getEnergyContainer();
            inputVoltage = container.getInputVoltage();
            outputVoltage = container.getOutputVoltage();
        }

        long euPerTick = 0;
        int bufferPercent = -1;
        if (controller instanceof WorkableMultiblockMachine w && w.getRecipeLogic().isWorking()) {
            var recipe = w.getRecipeLogic().getLastRecipe();
            if (recipe != null) {
                euPerTick = recipe.getInputEUt().getTotalEU() - recipe.getOutputEUt().getTotalEU();
            }
        }
        if (controller instanceof WorkableElectricMultiblockMachine e2) {
            long cap = e2.getEnergyContainer().getEnergyCapacity();
            if (cap > 0) bufferPercent = (int) (e2.getEnergyContainer().getEnergyStored() * 100 / cap);
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
                recipeTypeName, multiRecipe, electric, inputVoltage, outputVoltage, groupName, priority, euPerTick,
                bufferPercent);
    }
}
