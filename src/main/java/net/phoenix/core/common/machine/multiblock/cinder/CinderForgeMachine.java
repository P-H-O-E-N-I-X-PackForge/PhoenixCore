package net.phoenix.core.common.machine.multiblock.cinder;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.mui.MultiblockSchemaInfo;
import com.gregtechceu.gtceu.api.multiblock.MultiPredicate;
import com.gregtechceu.gtceu.api.multiblock.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.multiblock.pattern.PatternSlice;
import com.gregtechceu.gtceu.api.multiblock.predicates.BasePredicate;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.phoenix.core.common.data.PTags;
import net.phoenix.core.common.item.cinder.CinderCoreItem;
import net.phoenix.core.common.item.cinder.CinderSchemaData;
import net.phoenix.core.integration.ae2.CinderForgeHatchPartMachine;

import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.api.drawable.Text;
import brachy.modularui.drawable.ItemDrawable;
import brachy.modularui.drawable.SchemaRenderer;
import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.UISettings;
import brachy.modularui.screen.viewport.GuiContext;
import brachy.modularui.theme.WidgetTheme;
import brachy.modularui.value.sync.BooleanSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.value.sync.StringSyncValue;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widget.ScrollWidget;
import brachy.modularui.widget.scroll.VerticalScrollData;
import brachy.modularui.widgets.ButtonWidget;
import brachy.modularui.widgets.SchemaWidget;
import brachy.modularui.widgets.dynamic.DynamicHandler;
import brachy.modularui.widgets.dynamic.DynamicWidget;
import brachy.modularui.widgets.layout.Flow;
import brachy.modularui.widgets.textfield.TextFieldWidget;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class CinderForgeMachine extends MultiblockControllerMachine implements IMuiMachine {

    private static final int ROW_H = 18;
    private static final int MAX_TARGET_ROWS = 20;
    private static final int MAX_PREDICATE_ROWS = 16;
    private static final int MAX_CANDIDATES_PER_ROW = 16;
    private static final int MAX_SLICE_ROWS = 8;

    private static final int COLOR_TITLE = 0xFFFFE0A8;
    private static final int COLOR_LABEL = 0xFFA88FD9;
    private static final int COLOR_OK = 0xFF5CFF7A;
    private static final int COLOR_WARN = 0xFFFFD95C;
    private static final int COLOR_BAD = 0xFFFF6B5C;
    private static final int COLOR_DIM = 0xFF8C86A8;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_PANEL_BG = 0xEE1C1730;
    private static final int COLOR_PANEL_BORDER = 0xFF4A3F7A;
    private static final int COLOR_ROW_BG = 0xCC241D3D;
    private static final int COLOR_BG_TOP = 0xFF17101F;
    private static final int COLOR_BG_BOTTOM = 0xFF0B0712;

    private String targetFilter = "";
    private final List<MultiblockMachineDefinition> allTargets = new ArrayList<>();

    private int expandedPredicateRow = -1;

    private final NotifiableItemStackHandler tier1Inventory;

    public CinderForgeMachine(BlockEntityCreationInfo info) {
        super(info);
        this.tier1Inventory = attachTrait(new NotifiableItemStackHandler(CinderForgeHatchPartMachine.TOTAL_SLOTS,
                IO.BOTH, IO.BOTH));
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

    private record StatusBadge(int accentColor) implements IDrawable {

        @Override
        public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme widgetTheme) {
            var g = context.getGraphics();
            g.fill(x, y, x + width, y + height, 0x44_000000);
            g.fill(x, y, x + width, y + 2, (0xBB << 24) | (accentColor & 0xFFFFFF));
            g.fill(x, y, x + 1, y + height, (0x88 << 24) | (accentColor & 0xFFFFFF));
            g.fill(x + width - 1, y, x + width, y + height, (0x88 << 24) | (accentColor & 0xFFFFFF));
            g.fill(x, y + height - 1, x + width, y + height, (0x88 << 24) | (accentColor & 0xFFFFFF));
        }
    }

    private record BadgeState(String label, int color) {}

    private record BackgroundGradient() implements IDrawable {

        @Override
        public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme widgetTheme) {
            context.getGraphics().fillGradient(x, y, x + width, y + height, COLOR_BG_TOP, COLOR_BG_BOTTOM);
        }
    }

    private record PredicateRow(char predicateChar, int baseIndex, MultiPredicate predicate, BasePredicate base,
                                String label) {}

    private BadgeState badgeState(ItemStack core) {
        if (core.isEmpty() || !(core.getItem() instanceof CinderCoreItem)) {
            return new BadgeState("WAITING", COLOR_DIM);
        }
        if (CinderSchemaData.getTargetId(core) == null) return new BadgeState("UNCONFIGURED", COLOR_WARN);
        return !CinderSchemaData.tallyStocked(core).isEmpty() ? new BadgeState("READY", COLOR_OK) :
                new BadgeState("GATHERING", COLOR_WARN);
    }

    private @Nullable CinderForgeHatchPartMachine getHatch() {
        for (var part : getParts()) {
            if (part instanceof CinderForgeHatchPartMachine hatch) return hatch;
        }
        return null;
    }

    private @Nullable CinderForgeTier currentTier() {
        var tier3 = getPatternState("tier3");
        if (tier3 != null && tier3.isFormed()) return CinderForgeTier.TIER3;
        var tier2 = getPatternState("tier2");
        if (tier2 != null && tier2.isFormed()) return CinderForgeTier.TIER2;
        var tier1 = getPatternState("tier1");
        if (tier1 != null && tier1.isFormed()) return CinderForgeTier.TIER1;
        return null;
    }

    enum CinderForgeTier {

        TIER1(PTags.CINDER_FORGE_TIER1_ALLOWED, false),
        TIER2(PTags.CINDER_FORGE_TIER2_ALLOWED, true),
        TIER3(null, true);

        final @Nullable TagKey<Block> allowedTag;

        final boolean automated;

        CinderForgeTier(@Nullable TagKey<Block> allowedTag, boolean automated) {
            this.allowedTag = allowedTag;
            this.automated = automated;
        }
    }

    private void populateAllTargets() {
        allTargets.clear();
        TagKey<Block> allowedTag = null;
        CinderForgeTier tier = currentTier();
        if (tier != null) allowedTag = tier.allowedTag;

        for (MachineDefinition candidate : GTRegistries.MACHINES.values()) {
            if (!(candidate instanceof MultiblockMachineDefinition multiblock)) continue;
            var patternSupplier = multiblock.getStructurePatterns()
                    .get(com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine.DEFAULT_STRUCTURE);
            if (patternSupplier == null) continue;
            if (!(patternSupplier.get() instanceof BlockPattern)) continue;
            if (allowedTag != null && !multiblock.getBlock().defaultBlockState().is(allowedTag)) continue;
            allTargets.add(multiblock);
        }
        allTargets.sort(Comparator.comparing(d -> d.getBlock().getName().getString()));
    }

    private List<MultiblockMachineDefinition> filteredTargets() {
        String query = targetFilter.trim().toLowerCase(Locale.ROOT);
        List<MultiblockMachineDefinition> out = new ArrayList<>();
        for (MultiblockMachineDefinition def : allTargets) {
            if (query.isEmpty() || def.getBlock().getName().getString().toLowerCase(Locale.ROOT).contains(query) ||
                    def.getId().getPath().toLowerCase(Locale.ROOT).contains(query)) {
                out.add(def);
            }
        }
        return out;
    }

    private String statusLine(ItemStack core) {
        CinderForgeTier tier = currentTier();
        String tierLabel = tier == null ? "Cinder Forge" : "Cinder Forge [" + tier.name() + "]";

        if (core.isEmpty() || !(core.getItem() instanceof CinderCoreItem)) {
            return tierLabel + " - waiting for the ME network to supply a Core";
        }
        var def = CinderSchemaData.getTargetDefinition(core);
        if (def == null) return tierLabel + " - Core awaiting configuration";
        boolean filled = !CinderSchemaData.tallyStocked(core).isEmpty();
        return tierLabel + " - " + def.getBlock().getName().getString() + (filled ? " (ready)" : " (gathering)");
    }

    @Override
    public void buildMainUI(ParentWidget<?> mainWidget, PosGuiData guiData, PanelSyncManager syncManager,
                            UISettings settings) {
        populateAllTargets();
        mainWidget.background(new BackgroundGradient());

        CinderForgeTier tier = currentTier();
        CinderForgeHatchPartMachine hatch = tier == CinderForgeTier.TIER1 ? null : getHatch();
        NotifiableItemStackHandler inventory = tier == CinderForgeTier.TIER1 ? tier1Inventory :
                hatch != null ? hatch.getInventory() : null;

        ItemStack badgeCore = inventory == null ? ItemStack.EMPTY :
                inventory.getStackInSlot(CinderForgeHatchPartMachine.CORE_SLOT);
        mainWidget.child(Text.dynamic(() -> Component.literal(statusLine(badgeCore)))
                .asWidget().pos(8, 6).size(220, 10).color(COLOR_TITLE));

        BadgeState badge = badgeState(badgeCore);
        int badgeW = 8 * badge.label().length() + 10;
        mainWidget.child(new brachy.modularui.widget.Widget<>()
                .pos(400 - badgeW, 4).size(badgeW, 12)
                .background(new StatusBadge(badge.color())));
        mainWidget.child(Text.str(badge.label()).asWidget()
                .pos(400 - badgeW + 5, 6).size(badgeW - 6, 10).color(badge.color()));

        if (tier == null) {
            mainWidget.child(Text.str("Structure not formed.")
                    .asWidget().pos(8, 24).size(390, 10).color(COLOR_BAD));

            mainWidget.child(Text.str("Tier 1: small box, casing only, no ME hatch - manual only.")
                    .asWidget().pos(8, 38).size(390, 10).color(COLOR_LABEL));
            mainWidget.child(Text.str("Tier 2: larger hive shape with exactly 1 ME hatch - auto-packages.")
                    .asWidget().pos(8, 50).size(390, 10).color(COLOR_LABEL));
            mainWidget.child(Text.str("Tier 3: same hive shape with 2-4 ME hatches - parallel Cores.")
                    .asWidget().pos(8, 62).size(390, 10).color(COLOR_LABEL));
            return;
        }
        if (tier != CinderForgeTier.TIER1 && hatch == null) {
            mainWidget.child(Text.str("Structure formed but the Cinder Forge hatch is missing - " +
                    "this shouldn't be reachable, report it.").asWidget().pos(8, 24).size(390, 20).color(COLOR_BAD));
            return;
        }

        ParentWidget<?> contentContainer = mainWidget;
        int contentY = 22;
        if (tier == CinderForgeTier.TIER1) {
            contentY = buildTier1SlotSection(mainWidget, syncManager);

            Flow wrapper = Flow.col().pos(0, contentY).size(400, 500);
            mainWidget.child(wrapper);
            contentContainer = wrapper;
            contentY = 0;
        }

        ItemStack core = inventory.getStackInSlot(CinderForgeHatchPartMachine.CORE_SLOT);
        if (core.isEmpty() || !(core.getItem() instanceof CinderCoreItem)) {
            contentContainer.child(Text.str(tier == CinderForgeTier.TIER1 ? "No Core in the slot above yet." :
                    "No Core in the hatch right now.").asWidget().pos(8, contentY).size(390, 10).color(COLOR_LABEL));
            return;
        }

        if (CinderSchemaData.getTargetId(core) == null) {
            buildTargetPicker(contentContainer, syncManager, inventory);
        } else {
            buildConfigurator(contentContainer, syncManager, inventory, core);
        }
    }

    private int buildTier1SlotSection(ParentWidget<?> mainWidget, PanelSyncManager syncManager) {
        int slotSize = brachy.modularui.widgets.slot.ItemSlot.SIZE;
        var storage = tier1Inventory.storage;

        mainWidget.child(Text.str("CORE").asWidget().pos(8, 22).size(60, 10).color(COLOR_LABEL));
        var coreGroup = new brachy.modularui.widgets.slot.SlotGroup("cinderForgeTier1Core", 1,
                brachy.modularui.widgets.slot.SlotGroup.STORAGE_SLOT_PRIO, true);
        var coreSlot = brachy.modularui.value.sync.SyncHandlers
                .itemSlot(storage, CinderForgeHatchPartMachine.CORE_SLOT).slotGroup(coreGroup)
                .filter(stack -> stack.getItem() instanceof CinderCoreItem);
        mainWidget.child(new brachy.modularui.widgets.slot.ItemSlot().slot(coreSlot)
                .pos(8, 34).size(slotSize, slotSize)
                .background(new FlatPanel(COLOR_PANEL_BG, COLOR_TITLE)));

        BooleanSyncValue packageNow = syncManager.getOrCreateSyncHandler("cinderTier1PackageNow",
                BooleanSyncValue.class, () -> new BooleanSyncValue(() -> false, fired -> {
                    if (!fired) return;
                    CinderSchemaData.tryCraftCore(storage, CinderForgeHatchPartMachine.CORE_SLOT,
                            CinderForgeHatchPartMachine.CORE_SLOT + 1, CinderForgeHatchPartMachine.MATERIAL_SLOTS);
                    tier1Inventory.onContentsChanged();
                }).allowC2S(true));
        mainWidget.child(new ButtonWidget<>()
                .pos(8 + slotSize + 6, 34).size(100, 14)
                .overlay(Text.str("Package Now").asIcon())
                .onMousePressed((ctx, btn) -> {
                    packageNow.setBoolValue(true, true, true);
                    return true;
                }));
        mainWidget.child(Text.dynamic(() -> Component.literal("No automation on Tier 1 - materials only fill a " +
                "Core when you press Package Now.")).asWidget()
                .pos(8 + slotSize + 6, 52).size(280, 20).color(COLOR_DIM));

        int materialsY = 34 + slotSize + 14;
        mainWidget.child(Text.str("MATERIALS").asWidget().pos(8, materialsY).size(120, 10).color(COLOR_LABEL));

        int cols = 9;
        int rows = (CinderForgeHatchPartMachine.MATERIAL_SLOTS + cols - 1) / cols;
        int gridY = materialsY + 12;
        Flow grid = Flow.col().pos(8, gridY).size(cols * slotSize, rows * slotSize)
                .background(new FlatPanel(COLOR_PANEL_BG, COLOR_PANEL_BORDER));
        var materialsGroup = new brachy.modularui.widgets.slot.SlotGroup("cinderForgeTier1Materials", cols,
                brachy.modularui.widgets.slot.SlotGroup.STORAGE_SLOT_PRIO, true);
        for (int i = 0; i < CinderForgeHatchPartMachine.MATERIAL_SLOTS; i++) {
            int col = i % cols;
            int row = i / cols;
            var slot = brachy.modularui.value.sync.SyncHandlers
                    .itemSlot(storage, CinderForgeHatchPartMachine.CORE_SLOT + 1 + i).slotGroup(materialsGroup);
            grid.child(new brachy.modularui.widgets.slot.ItemSlot().slot(slot)
                    .pos(col * slotSize, row * slotSize).size(slotSize, slotSize));
        }
        mainWidget.child(grid);

        return gridY + rows * slotSize + 10;
    }

    private void buildTargetPicker(ParentWidget<?> mainWidget, PanelSyncManager syncManager,
                                   NotifiableItemStackHandler inventory) {
        StringSyncValue filterSync = new StringSyncValue(() -> targetFilter, val -> targetFilter = val);
        syncManager.syncValue("cinderTargetFilter", filterSync);

        BooleanSyncValue[] pickSlots = new BooleanSyncValue[MAX_TARGET_ROWS];
        for (int i = 0; i < MAX_TARGET_ROWS; i++) {
            final int idx = i;
            pickSlots[i] = syncManager.getOrCreateSyncHandler("cinderPick" + idx, BooleanSyncValue.class,
                    () -> new BooleanSyncValue(() -> false, fired -> {
                        if (!fired) return;
                        var targets = filteredTargets();
                        if (idx >= targets.size()) return;
                        ItemStack core = inventory.getStackInSlot(CinderForgeHatchPartMachine.CORE_SLOT);
                        if (core.isEmpty()) return;
                        CinderSchemaData.setTarget(core, targets.get(idx));
                        inventory.onContentsChanged();
                    }).allowC2S(true));
        }

        DynamicHandler listHandler = new DynamicHandler()
                .widgetProvider(() -> buildTargetListWidget(filteredTargets(), pickSlots));

        mainWidget.child(new TextFieldWidget()
                .pos(8, 22).size(240, 14)
                .value(filterSync)
                .hintText(Component.literal("Search multiblocks...")));

        mainWidget.child(new ButtonWidget<>()
                .pos(252, 22).size(46, 14)
                .overlay(Text.str("Filter").asIcon())
                .onMousePressed((ctx, btn) -> {
                    filterSync.setStringValue(targetFilter, true, true);
                    listHandler.notifyUpdate();
                    return true;
                }));

        mainWidget.child(new ScrollWidget<>(new VerticalScrollData())
                .pos(4, 42)
                .size(392, ROW_H * 10)
                .background(new FlatPanel(COLOR_PANEL_BG, COLOR_PANEL_BORDER))
                .child(new DynamicWidget<>().clientOnlyHandler(listHandler)));
    }

    private Flow buildTargetListWidget(List<MultiblockMachineDefinition> targets, BooleanSyncValue[] pickSlots) {
        Flow col = Flow.col().size(392, Math.max(1, Math.min(targets.size(), MAX_TARGET_ROWS)) * ROW_H);
        if (targets.isEmpty()) {
            col.child(Text.str("No matches.").asWidget().pos(2, 2).color(COLOR_LABEL));
            return col;
        }
        for (int i = 0; i < targets.size() && i < MAX_TARGET_ROWS; i++) {
            MultiblockMachineDefinition def = targets.get(i);
            BooleanSyncValue pick = pickSlots[i];
            Flow row = Flow.row().pos(0, i * ROW_H).size(392, ROW_H - 1)
                    .background(new FlatPanel(COLOR_ROW_BG, 0));
            row.child(new brachy.modularui.widget.Widget<>().pos(0, 0).size(2, ROW_H - 1)
                    .background(new FlatPanel(COLOR_TITLE, 0)));
            row.child(new ItemDrawable(def.getBlock().asItem()).asWidget().pos(5, 1).size(16, 16));
            row.child(new ButtonWidget<>()
                    .pos(24, 0).size(367, ROW_H - 1)
                    .overlay(Text.str(def.getBlock().getName().getString()).asIcon())
                    .onMousePressed((ctx, btn) -> {
                        pick.setBoolValue(true, true, true);
                        return true;
                    }));
            col.child(row);
        }
        return col;
    }

    private void buildConfigurator(ParentWidget<?> mainWidget, PanelSyncManager syncManager,
                                   NotifiableItemStackHandler inventory, ItemStack core) {
        MultiblockMachineDefinition definition = CinderSchemaData.getTargetDefinition(core);
        MultiblockSchemaInfo schemaInfo = CinderSchemaData.resolveSchema(core);
        if (definition == null || schemaInfo == null) return;
        var patternSupplier = definition.getStructurePatterns()
                .get(com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine.DEFAULT_STRUCTURE);
        if (patternSupplier == null || !(patternSupplier.get() instanceof BlockPattern pattern)) return;

        BooleanSyncValue changeTarget = syncManager.getOrCreateSyncHandler("cinderChangeTarget",
                BooleanSyncValue.class, () -> new BooleanSyncValue(() -> false, fired -> {
                    if (!fired) return;
                    ItemStack c = inventory.getStackInSlot(CinderForgeHatchPartMachine.CORE_SLOT);
                    if (c.isEmpty()) return;
                    CinderSchemaData.clearTarget(c);
                    inventory.onContentsChanged();
                }).allowC2S(true));

        mainWidget.child(new ButtonWidget<>()
                .pos(8, 22).size(96, 14)
                .overlay(Text.str("Change Target").asIcon())
                .onMousePressed((ctx, btn) -> {
                    changeTarget.setBoolValue(true, true, true);
                    return true;
                }));

        if (isRemote()) {
            mainWidget.child(Text.str("PREVIEW").asWidget().pos(240, 22).size(120, 10).color(COLOR_LABEL));
            SchemaRenderer renderer = new SchemaRenderer(schemaInfo.getMapSchema());
            schemaInfo.setRenderer(renderer);
            renderer.camera().setPosAndLookAt(0, 0, -10, schemaInfo.getMapSchema().getCenter());
            SchemaWidget schemaWidget = renderer.asWidget()
                    .enableDragRotation(true)
                    .enableScrollScaling(true)
                    .enableDragTranslation(false)
                    .pos(240, 36).size(152, 140)
                    .background(new FlatPanel(0, COLOR_PANEL_BORDER));
            schemaInfo.setMultiSchema(schemaWidget);
            mainWidget.child(schemaWidget);
        }

        List<Integer> variableSlices = new ArrayList<>();
        for (int i = 0; i < pattern.getSlices().length; i++) {
            PatternSlice slice = pattern.getSlices()[i];
            if (slice.getMinRepeats() != slice.getMaxRepeats()) variableSlices.add(i);
        }
        BooleanSyncValue[] sliceMinus = new BooleanSyncValue[MAX_SLICE_ROWS];
        BooleanSyncValue[] slicePlus = new BooleanSyncValue[MAX_SLICE_ROWS];
        for (int row = 0; row < MAX_SLICE_ROWS; row++) {
            final int sliceIndex = row < variableSlices.size() ? variableSlices.get(row) : -1;
            sliceMinus[row] = syncManager.getOrCreateSyncHandler("cinderSliceMinus" + row, BooleanSyncValue.class,
                    () -> new BooleanSyncValue(() -> false, fired -> {
                        if (!fired || sliceIndex < 0) return;
                        ItemStack c = inventory.getStackInSlot(CinderForgeHatchPartMachine.CORE_SLOT);
                        if (!c.isEmpty()) CinderSchemaData.adjustSliceRepeat(c, sliceIndex, -1);
                        inventory.onContentsChanged();
                    }).allowC2S(true));
            slicePlus[row] = syncManager.getOrCreateSyncHandler("cinderSlicePlus" + row, BooleanSyncValue.class,
                    () -> new BooleanSyncValue(() -> false, fired -> {
                        if (!fired || sliceIndex < 0) return;
                        ItemStack c = inventory.getStackInSlot(CinderForgeHatchPartMachine.CORE_SLOT);
                        if (!c.isEmpty()) CinderSchemaData.adjustSliceRepeat(c, sliceIndex, 1);
                        inventory.onContentsChanged();
                    }).allowC2S(true));
        }

        Flow sliceCol = Flow.col().pos(8, 38).size(224, ROW_H * Math.max(1, variableSlices.size()))
                .background(new FlatPanel(COLOR_PANEL_BG, COLOR_PANEL_BORDER));
        for (int row = 0; row < variableSlices.size() && row < MAX_SLICE_ROWS; row++) {
            int sliceIndex = variableSlices.get(row);
            int current = schemaInfo.getUserSliceRepeats().getOrDefault(sliceIndex,
                    pattern.getSlices()[sliceIndex].getMinRepeats());
            Flow r = Flow.row().pos(0, row * ROW_H).size(224, ROW_H - 1)
                    .background(new FlatPanel(COLOR_ROW_BG, 0));
            r.child(Text.str("Layer " + (sliceIndex + 1) + " repeats").asWidget().pos(0, 4).size(120, 10)
                    .color(COLOR_LABEL));
            BooleanSyncValue minus = sliceMinus[row];
            BooleanSyncValue plus = slicePlus[row];
            r.child(new ButtonWidget<>().pos(124, 0).size(14, 14).overlay(Text.str("-").asIcon())
                    .onMousePressed((ctx, btn) -> {
                        minus.setBoolValue(true, true, true);
                        return true;
                    }));
            r.child(Text.str(String.valueOf(current)).asWidget().pos(142, 4).size(20, 10).color(COLOR_TEXT));
            r.child(new ButtonWidget<>().pos(166, 0).size(14, 14).overlay(Text.str("+").asIcon())
                    .onMousePressed((ctx, btn) -> {
                        plus.setBoolValue(true, true, true);
                        return true;
                    }));
            sliceCol.child(r);
        }
        mainWidget.child(sliceCol);

        List<PredicateRow> predicateRows = new ArrayList<>();
        for (var entry : pattern.getPredicates().char2ObjectEntrySet()) {
            char c = entry.getCharKey();
            MultiPredicate predicate = entry.getValue();
            if (predicate.isAny() || predicate.isAir()) continue;
            var expanded = predicate.expand();
            for (int b = 0; b < expanded.size(); b++) {
                BasePredicate base = expanded.get(b);
                if (base.getCandidates().size() <= 1) continue;
                predicateRows.add(new PredicateRow(c, b, predicate, base, "[" + c + "] " + base.getTypeName()));
            }
        }
        if (expandedPredicateRow >= predicateRows.size()) expandedPredicateRow = -1;

        DynamicHandler predRowsHandler = new DynamicHandler();

        BooleanSyncValue[] expandToggle = new BooleanSyncValue[MAX_PREDICATE_ROWS];
        for (int row = 0; row < MAX_PREDICATE_ROWS; row++) {
            final int rowIndex = row;
            expandToggle[row] = syncManager.getOrCreateSyncHandler("cinderPredExpand" + row, BooleanSyncValue.class,
                    () -> new BooleanSyncValue(() -> false, fired -> {
                        if (!fired) return;
                        expandedPredicateRow = expandedPredicateRow == rowIndex ? -1 : rowIndex;
                        predRowsHandler.notifyUpdate();
                    }).allowC2S(true));
        }

        BooleanSyncValue[] candidatePick = new BooleanSyncValue[MAX_CANDIDATES_PER_ROW];
        for (int ci = 0; ci < MAX_CANDIDATES_PER_ROW; ci++) {
            final int candidateIndex = ci;
            candidatePick[ci] = syncManager.getOrCreateSyncHandler("cinderPredPick" + ci, BooleanSyncValue.class,
                    () -> new BooleanSyncValue(() -> false, fired -> {
                        if (!fired || expandedPredicateRow < 0 || expandedPredicateRow >= predicateRows.size()) {
                            return;
                        }
                        PredicateRow prow = predicateRows.get(expandedPredicateRow);
                        ItemStack c = inventory.getStackInSlot(CinderForgeHatchPartMachine.CORE_SLOT);
                        if (!c.isEmpty()) {
                            CinderSchemaData.setPredicateCandidate(c, prow.predicateChar(), prow.baseIndex(),
                                    candidateIndex);
                        }
                        expandedPredicateRow = -1;
                        inventory.onContentsChanged();
                        predRowsHandler.notifyUpdate();
                    }).allowC2S(true));
        }

        predRowsHandler.widgetProvider(
                () -> buildPredicateListWidget(predicateRows, inventory, expandToggle, candidatePick));

        mainWidget.child(new ScrollWidget<>(new VerticalScrollData())
                .pos(8, 38 + ROW_H * Math.max(1, variableSlices.size()) + 6)
                .size(224, ROW_H * 8)
                .background(new FlatPanel(COLOR_PANEL_BG, COLOR_PANEL_BORDER))
                .child(new DynamicWidget<>().clientOnlyHandler(predRowsHandler)));

        var required = schemaInfo.getBlockCounts();
        var stocked = CinderSchemaData.tallyStocked(core);
        Flow summaryCol = Flow.col().pos(240, 182).size(152, ROW_H * Math.max(1, required.size()))
                .background(new FlatPanel(COLOR_PANEL_BG, COLOR_PANEL_BORDER));
        int row = 0;
        for (var reqEntry : required.reference2IntEntrySet()) {
            if (row >= 10) {
                summaryCol.child(Text.str("...and more").asWidget().pos(4, row * ROW_H).color(COLOR_LABEL));
                break;
            }
            Block block = reqEntry.getKey();
            int need = reqEntry.getIntValue();
            int have = stocked.getOrDefault(block, 0);
            boolean ok = have >= need;
            Flow r = Flow.row().pos(0, row * ROW_H).size(152, ROW_H - 1)
                    .background(new FlatPanel(COLOR_ROW_BG, 0));
            r.child(new brachy.modularui.widget.Widget<>().pos(0, 0).size(2, ROW_H - 1)
                    .background(new FlatPanel(ok ? COLOR_OK : COLOR_BAD, 0)));
            r.child(new ItemDrawable(block.asItem()).asWidget().pos(5, 1).size(16, 16));
            r.child(Text.str(block.getName().getString() + " " + have + "/" + need)
                    .asWidget().pos(24, 4).size(126, 10).color(ok ? COLOR_OK : COLOR_BAD));
            summaryCol.child(r);
            row++;
        }
        mainWidget.child(summaryCol);
    }

    private Flow buildPredicateListWidget(List<PredicateRow> predicateRows, NotifiableItemStackHandler inventory,
                                          BooleanSyncValue[] expandToggle, BooleanSyncValue[] candidatePick) {
        ItemStack core = inventory.getStackInSlot(CinderForgeHatchPartMachine.CORE_SLOT);

        int extraH = 0;
        if (expandedPredicateRow >= 0 && expandedPredicateRow < predicateRows.size()) {
            var expandedCandidates = predicateRows.get(expandedPredicateRow).base().getCandidates();
            extraH = Math.min(MAX_CANDIDATES_PER_ROW, expandedCandidates.size()) * ROW_H;
        }

        Flow col = Flow.col().size(216, Math.max(1, predicateRows.size()) * ROW_H + extraH);
        if (predicateRows.isEmpty()) {
            col.child(Text.str("No configurable parts for this structure.").asWidget().pos(2, 2).color(COLOR_LABEL));
            return col;
        }

        int y = 0;
        for (int row = 0; row < predicateRows.size() && row < MAX_PREDICATE_ROWS; row++) {
            PredicateRow prow = predicateRows.get(row);
            var candidates = prow.base().getCandidates();
            int currentIndex = CinderSchemaData.getPredicateCandidateIndex(core, prow.predicate(), prow.base());
            String currentLabel = currentIndex < candidates.size() ?
                    candidates.get(currentIndex).getItemStackForm().getHoverName().getString() : "?";
            boolean isExpanded = row == expandedPredicateRow;

            Flow r = Flow.row().pos(0, y).size(216, ROW_H - 1).background(new FlatPanel(COLOR_ROW_BG, 0));
            r.child(Text.str(prow.label()).asWidget().pos(2, 4).size(84, 10).color(COLOR_LABEL));
            BooleanSyncValue toggle = expandToggle[row];
            r.child(new ButtonWidget<>().pos(88, 0).size(128, ROW_H - 1)
                    .overlay(Text.str(truncate(currentLabel, 15) + (isExpanded ? " ▲" : " ▼")).asIcon())
                    .onMousePressed((ctx, btn) -> {
                        toggle.setBoolValue(true, true, true);
                        return true;
                    }));
            col.child(r);
            y += ROW_H;

            if (!isExpanded) continue;
            for (int ci = 0; ci < candidates.size() && ci < MAX_CANDIDATES_PER_ROW; ci++) {
                var candidate = candidates.get(ci);
                boolean selected = ci == currentIndex;
                BooleanSyncValue pick = candidatePick[ci];
                Flow cr = Flow.row().pos(8, y).size(208, ROW_H - 1)
                        .background(new FlatPanel(COLOR_ROW_BG, selected ? COLOR_TITLE : 0));
                cr.child(new ItemDrawable(candidate.getBlockState().getBlock().asItem()).asWidget()
                        .pos(1, 1).size(16, 16));
                cr.child(new ButtonWidget<>().pos(20, 0).size(188, ROW_H - 1)
                        .overlay(Text.str(candidate.getItemStackForm().getHoverName().getString()).asIcon())
                        .onMousePressed((ctx, btn) -> {
                            pick.setBoolValue(true, true, true);
                            return true;
                        }));
                col.child(cr);
                y += ROW_H;
            }
        }
        return col;
    }

    private static String truncate(String s, int maxChars) {
        return s.length() <= maxChars ? s : s.substring(0, Math.max(0, maxChars - 1)) + "…";
    }
}
