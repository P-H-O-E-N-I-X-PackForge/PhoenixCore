package net.phoenix.core.integration.ae2;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.mui.MultiblockSchemaInfo;
import com.gregtechceu.gtceu.integration.ae2.machine.MEBusPartMachine;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.MEStorage;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

import net.phoenix.core.common.item.cinder.CinderCoreItem;
import net.phoenix.core.common.item.cinder.CinderSchemaData;

import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.api.drawable.Text;
import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.UISettings;
import brachy.modularui.screen.viewport.GuiContext;
import brachy.modularui.theme.WidgetTheme;
import brachy.modularui.value.sync.BooleanSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.value.sync.SyncHandlers;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widgets.ButtonWidget;
import brachy.modularui.widgets.layout.Flow;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.slot.ModularSlot;
import brachy.modularui.widgets.slot.SlotGroup;

import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.MethodsReturnNonnullByDefault;

/**
 * The Cinder Forge's one required part: pulls an unconfigured (empty) Cinder Core and whatever raw
 * materials its schema needs straight from the ME network, stocks the Core in place once it has
 * everything, and pushes the finished Core back out - entirely autonomous, no Pattern Encoding
 * Terminal step needed at all (see the Rebirth Cinder Core design notes on why a fixed-output GT
 * recipe/pattern doesn't fit "give back the same item, transformed").
 * <p>
 * Built directly against AE2's low-level grid storage API ({@code MEStorage#insert}/{@code extract})
 * the same way {@link METagInputBusPartMachine} already does in this codebase, rather than the
 * higher-level {@code ExportOnlyAEItemList} config-slot system that class also uses - that system is
 * built for player-configured whitelists, not "search the network for anything matching a computed
 * schema," which is what this needs.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CinderForgeHatchPartMachine extends MEBusPartMachine {

    public static final int CORE_SLOT = 0;
    public static final int MATERIAL_SLOTS = 54;
    public static final int TOTAL_SLOTS = MATERIAL_SLOTS + 1;

    private static final int SCAN_INTERVAL_TICKS = 20;
    private static final int MATERIAL_GRID_COLS = 9;

    // Same shared palette/card-background pattern as the Cinder Forge controller and Drone
    // controller UIs, so the whole Cinder Forge system reads as one coherent style.
    private static final int COLOR_TITLE = 0xFFFFE0A8;
    private static final int COLOR_LABEL = 0xFFA88FD9;
    private static final int COLOR_PANEL_BG = 0xEE1C1730;
    private static final int COLOR_PANEL_BORDER = 0xFF4A3F7A;
    private static final int COLOR_BG_TOP = 0xFF17101F;
    private static final int COLOR_BG_BOTTOM = 0xFF0B0712;

    private int scanTimer = 0;

    public CinderForgeHatchPartMachine(BlockEntityCreationInfo info) {
        super(info, IO.IN, new NotifiableItemStackHandler(TOTAL_SLOTS, IO.BOTH, IO.BOTH));
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

    /** Same whole-panel dark gradient as the Cinder Forge controller UI - see its class doc for why
     *  this is needed on top of the card backgrounds below (otherwise they just float on GTCEu's
     *  default light chrome). */
    private record BackgroundGradient() implements IDrawable {

        @Override
        public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme widgetTheme) {
            context.getGraphics().fillGradient(x, y, x + width, y + height, COLOR_BG_TOP, COLOR_BG_BOTTOM);
        }
    }

    /**
     * Own minimal slot-grid UI, deliberately NOT the inherited {@code ItemBusPartMachine} default:
     * that base sizes its slot grid from a tier formula ({@code (1 + min(9, tier))^2}, which for UHV
     * renders a 10x10 = 100-slot grid with no relation to this hatch's real 55-slot handler - 45 of
     * those slots aren't backed by any real storage at all, and nothing distinguishes slot 0 (the
     * Core slot) from any of the 54 material slots around it. A player trying to manually place a
     * Core into a uniform grid of 100 identical-looking slots has no way to know which one is the
     * right one - which is exactly why it looked like the hatch "wasn't seeing" a Core that had
     * actually just been placed into the wrong slot. This lays out the real 55 slots only, with the
     * Core slot clearly separated, labeled, and accent-bordered.
     */
    @Override
    public void buildMainUI(ParentWidget<?> mainWidget, PosGuiData guiData, PanelSyncManager syncManager,
                            UISettings settings) {
        var storage = getInventory().storage;
        int slotSize = ItemSlot.SIZE;

        mainWidget.background(new BackgroundGradient());
        mainWidget.child(Text.str("Cinder Forge ME Interface").asWidget()
                .pos(8, 6).size(200, 10).color(COLOR_TITLE));

        mainWidget.child(Text.str("CORE").asWidget().pos(8, 20).size(60, 10).color(COLOR_LABEL));
        SlotGroup coreGroup = new SlotGroup("cinderCore", 1, SlotGroup.STORAGE_SLOT_PRIO, true);
        ModularSlot coreSlot = SyncHandlers.itemSlot(storage, CORE_SLOT).slotGroup(coreGroup)
                .filter(stack -> stack.getItem() instanceof CinderCoreItem);
        mainWidget.child(new ItemSlot().slot(coreSlot)
                .pos(8, 32).size(slotSize, slotSize)
                .background(new FlatPanel(COLOR_PANEL_BG, COLOR_TITLE)));

        // This UI otherwise has zero feedback of its own about whether the slot above actually
        // contains a real, recognized Core - a player has no way to tell "it's genuinely empty" from
        // "something's there but the system isn't picking it up" without this line.
        mainWidget.child(Text.dynamic(() -> Component.literal(coreStatusLine())).asWidget()
                .pos(8 + slotSize + 6, 32).size(180, 20).color(COLOR_LABEL));

        // Manual override - autoIO() already attempts this every tick on its own, but this gives
        // instant feedback instead of waiting up to 20 ticks, and is the one and only way to trigger
        // it at all on a Cinder Forge tier that doesn't auto-run (see the tiered Cinder Forge design).
        BooleanSyncValue packageNow = syncManager.getOrCreateSyncHandler("cinderPackageNow",
                BooleanSyncValue.class, () -> new BooleanSyncValue(() -> false, fired -> {
                    if (!fired) return;
                    CinderSchemaData.tryCraftCore(getInventory().storage, CORE_SLOT, CORE_SLOT + 1, MATERIAL_SLOTS);
                    getInventory().onContentsChanged();
                }).allowC2S(true));
        mainWidget.child(new ButtonWidget<>()
                .pos(8 + slotSize + 6, 54).size(100, 14)
                .overlay(Text.str("Package Now").asIcon())
                .onMousePressed((ctx, btn) -> {
                    packageNow.setBoolValue(true, true, true);
                    return true;
                }));

        // Below both the Core slot (which ends at 32 + slotSize) and the status text + Package Now
        // button beside it (which together run to y=68) - whichever is taller wins the gap.
        int materialsY = Math.max(32 + slotSize + 14, 76);
        mainWidget.child(Text.str("MATERIALS").asWidget().pos(8, materialsY).size(120, 10).color(COLOR_LABEL));

        int rows = (MATERIAL_SLOTS + MATERIAL_GRID_COLS - 1) / MATERIAL_GRID_COLS;
        int gridY = materialsY + 12;
        Flow grid = Flow.col().pos(8, gridY)
                .size(MATERIAL_GRID_COLS * slotSize, rows * slotSize)
                .background(new FlatPanel(COLOR_PANEL_BG, COLOR_PANEL_BORDER));

        SlotGroup materialsGroup = new SlotGroup("cinderMaterials", MATERIAL_GRID_COLS,
                SlotGroup.STORAGE_SLOT_PRIO, true);
        for (int i = 0; i < MATERIAL_SLOTS; i++) {
            int col = i % MATERIAL_GRID_COLS;
            int row = i / MATERIAL_GRID_COLS;
            ModularSlot slot = SyncHandlers.itemSlot(storage, CORE_SLOT + 1 + i).slotGroup(materialsGroup);
            grid.child(new ItemSlot().slot(slot).pos(col * slotSize, row * slotSize).size(slotSize, slotSize));
        }
        mainWidget.child(grid);
    }

    /** Same checks {@code CinderForgeMachine}'s own status line uses, just phrased for "what's in
     *  this specific slot right now" rather than the whole structure's state. */
    private String coreStatusLine() {
        ItemStack core = getInventory().storage.getStackInSlot(CORE_SLOT);
        if (core.isEmpty()) return "No Core in this slot yet.";
        if (!(core.getItem() instanceof CinderCoreItem)) return "That item isn't a Rebirth Cinder Core.";

        var definition = CinderSchemaData.getTargetDefinition(core);
        if (definition == null) {
            return "Core detected - not configured yet. Shift-right-click the Core itself to pick a target.";
        }
        boolean filled = !CinderSchemaData.tallyStocked(core).isEmpty();
        return "Core detected: " + definition.getBlock().getName().getString() +
                (filled ? " (ready)" : " (gathering materials)");
    }

    @Override
    public void autoIO() {
        if (!isWorkingEnabled() || !shouldSyncME()) return;
        if (!updateMEStatus()) return;
        if (getMainNode().getGrid() == null) return;

        MEStorage storage = getMainNode().getGrid().getStorageService().getInventory();

        if (++scanTimer >= SCAN_INTERVAL_TICKS) {
            scanTimer = 0;
            pullCoreIfNeeded(storage);
            pullMaterialsIfNeeded(storage);
        }

        tryCraft();
        pushFilledCoreIfReady(storage);

        updateInventorySubscription();
    }

    private void pullCoreIfNeeded(MEStorage storage) {
        var inventory = getInventory();
        if (!inventory.getStackInSlot(CORE_SLOT).isEmpty()) return;

        for (var entry : storage.getAvailableStacks()) {
            if (!(entry.getKey() instanceof AEItemKey key)) continue;
            if (!(key.getItem() instanceof CinderCoreItem)) continue;

            ItemStack sample = key.toStack(1);
            if (CinderSchemaData.getTargetId(sample) == null) continue;
            if (!CinderSchemaData.tallyStocked(sample).isEmpty()) continue;

            long extracted = storage.extract(key, 1, Actionable.MODULATE, getActionSource());
            if (extracted > 0) {
                inventory.setStackInSlot(CORE_SLOT, key.toStack((int) extracted));
                return;
            }
        }
    }

    private void pullMaterialsIfNeeded(MEStorage storage) {
        var inventory = getInventory();
        ItemStack core = inventory.getStackInSlot(CORE_SLOT);
        if (core.isEmpty() || !(core.getItem() instanceof CinderCoreItem)) return;
        if (CinderSchemaData.getTargetId(core) == null) return;

        MultiblockSchemaInfo schemaInfo = CinderSchemaData.resolveSchema(core);
        if (schemaInfo == null) return;
        Reference2IntMap<Block> required = new Reference2IntOpenHashMap<>(schemaInfo.getBlockCounts());
        if (required.isEmpty()) return;

        Reference2IntMap<Block> haveLocally = new Reference2IntOpenHashMap<>();
        for (int i = 0; i < MATERIAL_SLOTS; i++) {
            ItemStack stack = inventory.getStackInSlot(CORE_SLOT + 1 + i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) continue;
            haveLocally.merge(blockItem.getBlock(), stack.getCount(), Integer::sum);
        }

        for (var entry : required.reference2IntEntrySet()) {
            int still = entry.getIntValue() - haveLocally.getOrDefault(entry.getKey(), 0);
            if (still <= 0) continue;

            AEItemKey key = AEItemKey.of(new ItemStack(entry.getKey().asItem()));
            if (key == null) continue;
            long extracted = storage.extract(key, still, Actionable.MODULATE, getActionSource());
            if (extracted <= 0) continue;

            long remaining = extracted;
            for (int i = 0; i < MATERIAL_SLOTS && remaining > 0; i++) {
                int slot = CORE_SLOT + 1 + i;
                ItemStack stack = inventory.getStackInSlot(slot);
                if (stack.isEmpty()) {
                    int put = (int) Math.min(remaining, stack.getMaxStackSize());
                    inventory.setStackInSlot(slot, new ItemStack(entry.getKey().asItem(), put));
                    remaining -= put;
                } else if (stack.getItem() == entry.getKey().asItem() &&
                        stack.getCount() < stack.getMaxStackSize()) {
                    int room = stack.getMaxStackSize() - stack.getCount();
                    int put = (int) Math.min(remaining, room);
                    stack.grow(put);
                    inventory.setStackInSlot(slot, stack);
                    remaining -= put;
                }
            }
        }
    }

    /** Delegates to the shared logic in {@link CinderSchemaData#tryCraftCore} so this and a manual
     *  "Package Now" button (see {@link #buildMainUI}) never drift into two different behaviors. */
    private void tryCraft() {
        CinderSchemaData.tryCraftCore(getInventory().storage, CORE_SLOT, CORE_SLOT + 1, MATERIAL_SLOTS);
    }

    private void pushFilledCoreIfReady(MEStorage storage) {
        var inventory = getInventory();
        ItemStack core = inventory.getStackInSlot(CORE_SLOT);
        if (core.isEmpty() || !(core.getItem() instanceof CinderCoreItem)) return;
        if (CinderSchemaData.tallyStocked(core).isEmpty()) return;

        GenericStack stack = GenericStack.fromItemStack(core);
        if (stack == null) return;
        long inserted = storage.insert(stack.what(), stack.amount(), Actionable.MODULATE, getActionSource());
        if (inserted >= stack.amount()) {
            inventory.setStackInSlot(CORE_SLOT, ItemStack.EMPTY);
        }
    }
}
