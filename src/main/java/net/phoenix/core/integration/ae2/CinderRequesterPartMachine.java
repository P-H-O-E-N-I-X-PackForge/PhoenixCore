package net.phoenix.core.integration.ae2;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.integration.ae2.machine.MEBusPartMachine;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.item.cinder.CinderCoreItem;
import net.phoenix.core.common.item.cinder.CinderSchemaData;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.api.drawable.Text;
import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.UISettings;
import brachy.modularui.screen.viewport.GuiContext;
import brachy.modularui.theme.WidgetTheme;
import brachy.modularui.value.sync.IntSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.value.sync.SyncHandlers;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widgets.ButtonWidget;
import brachy.modularui.widgets.layout.Flow;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.slot.ModularSlot;
import brachy.modularui.widgets.slot.SlotGroup;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;

import javax.annotation.ParametersAreNonnullByDefault;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Design doc feature #5 - the requester/auto-restock companion block. Per the user's own answer when
 * this was designed, keeps <i>ready-to-build material kits</i> in stock (raw materials already sitting
 * in the network, sufficient to deploy N multiblocks right now), not placed structures - deployment
 * stays a deliberate player action via a Cinder Core/Atlas.
 * <p>
 * A "kit" is defined by whatever {@link CinderCoreItem} is socketed into the template slot (read-only
 * reference, never consumed or moved - same slot-as-recipe-source idea {@code CinderForgeHatchPartMachine}
 * uses for its own CORE_SLOT, just not packaged/removed here). "Ready kits" = the floor, over every
 * required block, of (current network stock / per-kit count) - i.e. complete sets already on hand, not
 * a projection of what AE2 <i>could</i> craft - matching "ready-to-build" literally.
 * <p>
 * Extends {@link MEBusPartMachine} for the exact same reason {@code CinderForgeHatchPartMachine} does:
 * it's the established, verified-working base class in this codebase for "a GTCEu block that's also a
 * real AE2 grid node with a per-tick {@code autoIO()} hook" - the precedent the original design doc
 * called out for this exact feature. Unlike a Cinder Atlas (a portable item with no grid node of its
 * own, borrowing a wireless access point's), this machine has a genuine {@code IManagedGridNode} via
 * {@link #getMainNode()}, so its crafting requests need no such workaround.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CinderRequesterPartMachine extends MEBusPartMachine {

    public static final int TEMPLATE_SLOT = 0;

    private static final int SCAN_INTERVAL_TICKS = 100;

    private static final int COLOR_TITLE = 0xFFFFE0A8;
    private static final int COLOR_LABEL = 0xFFA88FD9;
    private static final int COLOR_PANEL_BG = 0xEE1C1730;
    private static final int COLOR_BG_TOP = 0xFF17101F;
    private static final int COLOR_BG_BOTTOM = 0xFF0B0712;

    @SaveField
    @SyncToClient
    private int keepStocked = 1;
    @SaveField
    @SyncToClient
    private int batchSize = 1;

    @SyncToClient
    private int readyKits = 0;
    @SyncToClient
    private String statusLine = "Idle.";

    private int scanTimer = 0;

    public CinderRequesterPartMachine(BlockEntityCreationInfo info) {
        super(info, IO.NONE, new NotifiableItemStackHandler(1, IO.NONE, IO.NONE));
    }

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

    private record BackgroundGradient() implements IDrawable {

        @Override
        public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme widgetTheme) {
            context.getGraphics().fillGradient(x, y, x + width, y + height, COLOR_BG_TOP, COLOR_BG_BOTTOM);
        }
    }

    @Override
    public void buildMainUI(ParentWidget<?> mainWidget, PosGuiData guiData, PanelSyncManager syncManager,
                            UISettings settings) {
        var storage = getInventory().storage;
        int slotSize = ItemSlot.SIZE;

        mainWidget.background(new BackgroundGradient());
        mainWidget.child(Text.str("Cinder Requester").asWidget().pos(8, 6).size(200, 10).color(COLOR_TITLE));

        mainWidget.child(Text.str("TEMPLATE").asWidget().pos(8, 20).size(80, 10).color(COLOR_LABEL));
        SlotGroup templateGroup = new SlotGroup("cinderTemplate", 1, SlotGroup.STORAGE_SLOT_PRIO, true);
        ModularSlot templateSlot = SyncHandlers.itemSlot(storage, TEMPLATE_SLOT)
                .slotGroup(templateGroup)
                .filter(stack -> stack.getItem() instanceof CinderCoreItem);
        mainWidget.child(new ItemSlot().slot(templateSlot)
                .pos(8, 32).size(slotSize, slotSize)
                .background(new FlatPanel(COLOR_PANEL_BG, COLOR_TITLE)));

        mainWidget.child(Text.dynamic(() -> Component.literal(templateStatusLine())).asWidget()
                .pos(8 + slotSize + 6, 32).size(200, 30).color(COLOR_LABEL));

        int rowY = 32 + slotSize + 16;
        mainWidget.child(Text.str("Keep Stocked (kits)").asWidget().pos(8, rowY).size(160, 10).color(COLOR_LABEL));
        mainWidget.child(buildStepper(syncManager, "cinderKeepStocked", () -> keepStocked,
                v -> keepStocked = Math.max(0, v), 8, rowY + 12));

        int rowY2 = rowY + 32;
        mainWidget.child(Text.str("Batch Size (kits)").asWidget().pos(8, rowY2).size(160, 10).color(COLOR_LABEL));
        mainWidget.child(buildStepper(syncManager, "cinderBatchSize", () -> batchSize,
                v -> batchSize = Math.max(1, v), 8, rowY2 + 12));

        int rowY3 = rowY2 + 32;
        mainWidget.child(Text.dynamic(() -> Component.literal("Ready: " + readyKits + " / " + keepStocked))
                .asWidget().pos(8, rowY3).size(160, 10).color(COLOR_LABEL));
        mainWidget.child(Text.dynamic(() -> Component.literal(statusLine)).asWidget()
                .pos(8, rowY3 + 12).size(280, 10).color(COLOR_LABEL));
    }

    private Flow buildStepper(PanelSyncManager syncManager, String key, IntSupplier getter, IntConsumer setter,
                              int x, int y) {
        IntSyncValue sync = syncManager.getOrCreateSyncHandler(key, IntSyncValue.class,
                () -> new IntSyncValue(getter, setter).allowC2S(true));
        Flow row = Flow.row().pos(x, y).size(140, 14);
        row.child(new ButtonWidget<>().pos(0, 0).size(14, 14).overlay(Text.str("-").asIcon())
                .onMousePressed((ctx, btn) -> {
                    sync.setIntValue(Math.max(0, sync.getIntValue() - 1), true, true);
                    return true;
                }));
        row.child(Text.dynamic(() -> Component.literal(String.valueOf(sync.getIntValue()))).asWidget()
                .pos(18, 2).size(40, 10).color(COLOR_LABEL));
        row.child(new ButtonWidget<>().pos(60, 0).size(14, 14).overlay(Text.str("+").asIcon())
                .onMousePressed((ctx, btn) -> {
                    sync.setIntValue(sync.getIntValue() + 1, true, true);
                    return true;
                }));
        return row;
    }

    private String templateStatusLine() {
        ItemStack template = getInventory().storage.getStackInSlot(TEMPLATE_SLOT);
        if (template.isEmpty()) return "No template Core - insert a configured one to define a kit.";
        if (!(template.getItem() instanceof CinderCoreItem)) return "That item isn't a Rebirth Cinder Core.";
        var definition = CinderSchemaData.getTargetDefinition(template);
        if (definition == null) return "Template Core isn't configured yet.";
        return "Kit: " + definition.getBlock().getName().getString();
    }

    @Override
    public void autoIO() {
        if (!isWorkingEnabled() || !shouldSyncME()) return;
        if (!updateMEStatus()) return;

        IGrid grid = getMainNode().getGrid();
        if (grid == null) return;

        if (++scanTimer < SCAN_INTERVAL_TICKS) return;
        scanTimer = 0;

        ItemStack template = getInventory().storage.getStackInSlot(TEMPLATE_SLOT);
        if (template.isEmpty() || !(template.getItem() instanceof CinderCoreItem) ||
                CinderSchemaData.getTargetId(template) == null) {
            readyKits = 0;
            statusLine = "No template configured.";
            return;
        }

        Reference2IntMap<Block> perKit = CinderSchemaData.getCachedRequiredBlocks(template);
        if (perKit.isEmpty()) {
            readyKits = 0;
            statusLine = "Template has no material requirements.";
            return;
        }

        KeyCounter stock = grid.getStorageService().getCachedInventory();
        int kits = Integer.MAX_VALUE;
        for (var entry : perKit.reference2IntEntrySet()) {
            AEItemKey key = AEItemKey.of(entry.getKey());
            if (key == null) {
                kits = 0;
                break;
            }
            kits = Math.min(kits, (int) (stock.get(key) / entry.getIntValue()));
        }
        readyKits = Math.max(kits, 0);

        if (readyKits >= keepStocked) {
            statusLine = "Stocked.";
            return;
        }

        ICraftingService craftingService = grid.getCraftingService();
        for (var entry : perKit.reference2IntEntrySet()) {
            AEItemKey key = AEItemKey.of(entry.getKey());
            if (key != null && craftingService.isRequesting(key)) {
                statusLine = "Below threshold - request already in flight.";
                return;
            }
        }

        if (!(getLevel() instanceof ServerLevel level)) return;

        statusLine = "Below threshold - requesting " + batchSize + " more kit(s).";
        submitRestockBatch(level, grid, craftingService, perKit, batchSize);
    }

    private void submitRestockBatch(ServerLevel level, IGrid grid, ICraftingService craftingService,
                                    Reference2IntMap<Block> perKit, int kits) {
        IActionSource source = getActionSource();
        ICraftingSimulationRequester requester = () -> source;
        MinecraftServer server = level.getServer();

        for (var entry : perKit.reference2IntEntrySet()) {
            AEItemKey key = AEItemKey.of(entry.getKey());
            if (key == null) continue;
            long amount = (long) entry.getIntValue() * kits;

            Future<ICraftingPlan> future = craftingService.beginCraftingCalculation(level, requester, key, amount,
                    CalculationStrategy.CRAFT_LESS);

            CompletableFuture.runAsync(() -> {
                ICraftingPlan plan;
                try {
                    plan = future.get();
                } catch (Exception e) {
                    PhoenixCore.LOGGER.warn("[CinderRequester] Crafting calculation failed for {}: {}", key,
                            e.toString());
                    plan = null;
                }

                ICraftingPlan finalPlan = plan;
                server.execute(() -> {
                    if (finalPlan == null) {
                        statusLine = "Calculation failed for " + key + " - see log.";
                        return;
                    }
                    // Diagnostic logging (2026-09-16) - same reasoning as
                    // CinderAtlasWirelessLink#requestMissingMaterials, added after a report of a batch
                    // reporting success with no actual network activity despite spare CPU capacity and
                    // real patterns existing.
                    PhoenixCore.LOGGER.info(
                            "[CinderRequester] Plan for {}x{}: simulation={} bytes={} missingItems={} " +
                                    "patternSteps={}",
                            amount, key, finalPlan.simulation(), finalPlan.bytes(), finalPlan.missingItems(),
                            finalPlan.patternTimes().size());

                    // Matches AE2's own CraftConfirmMenu#cpuMatches (verified via bytecode) - also
                    // requires the CPU's available storage cover the plan's byte size, not just idle.
                    ICraftingCPU cpu = craftingService.getCpus().stream()
                            .filter(c -> !c.isBusy() && c.getAvailableStorage() >= finalPlan.bytes())
                            .findFirst().orElse(null);
                    if (cpu == null) {
                        statusLine = "No idle, big-enough crafting CPU for " + key + ".";
                        return;
                    }
                    ICraftingSubmitResult result = craftingService.submitJob(finalPlan, null, cpu, true, source);
                    PhoenixCore.LOGGER.info(
                            "[CinderRequester] submitJob for {}x{}: successful={} errorCode={} errorDetail={} " +
                                    "link={}",
                            amount, key, result.successful(), result.errorCode(), result.errorDetail(),
                            result.link());
                    statusLine = result.successful() ? "Requested " + amount + "x " + key + "." :
                            "Couldn't submit " + key + ": " + result.errorCode() +
                                    (result.errorDetail() != null ? " (" + result.errorDetail() + ")" : "");
                });
            });
        }
    }
}
