package net.phoenix.core.client.gui.cinder;

import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.mui.MultiblockSchemaInfo;
import com.gregtechceu.gtceu.api.multiblock.MultiPredicate;
import com.gregtechceu.gtceu.api.multiblock.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.multiblock.pattern.PatternSlice;
import com.gregtechceu.gtceu.api.multiblock.predicates.BasePredicate;
import com.gregtechceu.gtceu.api.multiblock.util.BlockInfo;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.common.item.cinder.CinderCoreItem;
import net.phoenix.core.common.item.cinder.CinderSchemaData;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.network.packet.C2SCinderClearTargetPacket;
import net.phoenix.core.network.packet.C2SCinderConfigPacket;
import net.phoenix.core.network.packet.C2SCinderSetTargetPacket;
import net.phoenixvine.wiki.theme.PhoenixTheme;

import com.mojang.blaze3d.systems.RenderSystem;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class CinderConfiguratorScreen extends Screen {

    private static final int PANEL_W = 420;
    private static final int PANEL_H = 320;
    private static final int ROW_H = 20;
    private static final int DROPDOWN_ROW_W = 140;
    private static final int DROPDOWN_ITEM_H = 16;
    private static final int TARGET_ROW_H = 18;
    private static final int BUTTON_H = 18;

    private int cPanel, cBorder, cAccent, cText, cTextDim, cOk, cBad, cRowBg, cRowHover, cDropdownBg;

    private record ClickRegion(int x, int y, int w, int h, Runnable action) {

        boolean contains(double px, double py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }
    }

    private record SliceRow(int sliceIndex, String label, int min, int max) {}

    private record PredicateChoiceRow(char predicateChar, MultiPredicate predicate, BasePredicate basePredicate,
                                      String label) {}

    private final InteractionHand hand;
    private final List<ClickRegion> clickRegions = new ArrayList<>();
    private final CinderStructurePreview structurePreview = new CinderStructurePreview();

    private ItemStack cinderCoreStack = ItemStack.EMPTY;
    private @Nullable MultiblockMachineDefinition definition;
    private @Nullable BlockPattern pattern;
    private @Nullable MultiblockSchemaInfo schemaInfo;

    private final List<SliceRow> sliceRows = new ArrayList<>();
    private final List<PredicateChoiceRow> predicateRows = new ArrayList<>();

    private int scrollY = 0;
    private int contentHeight = 0;

    private @Nullable PredicateChoiceRow openDropdown = null;

    private final List<MultiblockMachineDefinition> allTargets = new ArrayList<>();
    private final List<MultiblockMachineDefinition> filteredTargets = new ArrayList<>();
    private @Nullable EditBox filterBox;
    private int targetScrollY = 0;

    private int previewX, previewY, previewW, previewH;

    public CinderConfiguratorScreen(InteractionHand hand) {
        super(Component.literal("Rebirth Cinder Core"));
        this.hand = hand;
    }

    public static void open(InteractionHand hand) {
        Minecraft.getInstance().setScreen(new CinderConfiguratorScreen(hand));
    }

    private int panelLeft() {
        return (width - PANEL_W) / 2;
    }

    private int panelTop() {
        return Math.max(16, (height - PANEL_H) / 2);
    }

    private void refreshTheme() {
        PhoenixTheme t = PhoenixTheme.current();
        cPanel = (t.panel.getColor() & 0x00FFFFFF) | 0xEC000000;
        cBorder = (t.border.getColor() & 0x00FFFFFF) | 0xFF000000;
        cAccent = (t.accent.getColor() & 0x00FFFFFF) | 0xFF000000;
        cText = (t.text.getColor() & 0x00FFFFFF) | 0xFF000000;
        cTextDim = (t.textDim.getColor() & 0x00FFFFFF) | 0xFF000000;
        cOk = (t.done.getColor() & 0x00FFFFFF) | 0xFF000000;
        cBad = (t.locked.getColor() & 0x00FFFFFF) | 0xFF000000;
        cRowBg = (t.bg.getColor() & 0x00FFFFFF) | 0xD0000000;
        cRowHover = (t.header.getColor() & 0x00FFFFFF) | 0xD0000000;
        cDropdownBg = (t.bg.getColor() & 0x00FFFFFF) | 0xFF000000;
    }

    @Override
    protected void init() {
        super.init();
        refreshTheme();

        int left = panelLeft();
        int top = panelTop();
        filterBox = new EditBox(font, left + 8, top + 22, PANEL_W - 16, 14, Component.literal("Filter"));
        filterBox.setBordered(true);
        filterBox.setMaxLength(64);
        filterBox.setResponder(s -> filterTargets());
        filterBox.setHint(Component.literal("Search multiblocks..."));
        addRenderableWidget(filterBox);

        reload();
    }

    private void reload() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        cinderCoreStack = player.getItemInHand(hand);

        sliceRows.clear();
        predicateRows.clear();
        pattern = null;

        if (!(cinderCoreStack.getItem() instanceof CinderCoreItem)) {
            definition = null;
            schemaInfo = null;
            return;
        }

        definition = CinderSchemaData.getTargetDefinition(cinderCoreStack);
        schemaInfo = definition != null ? CinderSchemaData.resolveSchema(cinderCoreStack) : null;
        if (definition == null || schemaInfo == null) {
            populateTargetList();
            return;
        }
        if (filterBox != null) filterBox.setVisible(false);

        var patternSupplier = definition.getStructurePatterns().get(MultiblockControllerMachine.DEFAULT_STRUCTURE);
        if (patternSupplier == null || !(patternSupplier.get() instanceof BlockPattern blockPattern)) return;
        pattern = blockPattern;

        for (int i = 0; i < pattern.getSlices().length; i++) {
            PatternSlice slice = pattern.getSlices()[i];
            if (slice.getMinRepeats() == slice.getMaxRepeats()) continue;
            sliceRows.add(new SliceRow(i, "Layer " + (i + 1) + " repeats", slice.getMinRepeats(),
                    slice.getMaxRepeats()));
        }

        for (var entry : pattern.getPredicates().char2ObjectEntrySet()) {
            char c = entry.getCharKey();
            MultiPredicate predicate = entry.getValue();
            if (predicate.isAny() || predicate.isAir()) continue;
            for (BasePredicate base : predicate.expand()) {

                if (base.getCandidates().size() <= 1) continue;
                predicateRows.add(new PredicateChoiceRow(c, predicate, base, "[" + c + "] " + base.getTypeName()));
            }
        }
    }

    private void populateTargetList() {
        allTargets.clear();
        for (MachineDefinition candidate : GTRegistries.MACHINES.values()) {
            if (!(candidate instanceof MultiblockMachineDefinition multiblock)) continue;
            var patternSupplier = multiblock.getStructurePatterns().get(MultiblockControllerMachine.DEFAULT_STRUCTURE);
            if (patternSupplier == null) continue;
            if (patternSupplier.get() instanceof BlockPattern) {
                allTargets.add(multiblock);
            }
        }
        allTargets.sort(Comparator.comparing(d -> d.getBlock().getName().getString()));
        if (filterBox != null) filterBox.setVisible(true);
        filterTargets();
    }

    private void filterTargets() {
        String query = filterBox != null ? filterBox.getValue().trim().toLowerCase(Locale.ROOT) : "";
        filteredTargets.clear();
        for (MultiblockMachineDefinition candidate : allTargets) {
            if (query.isEmpty() ||
                    candidate.getBlock().getName().getString().toLowerCase(Locale.ROOT).contains(query) ||
                    candidate.getId().getPath().toLowerCase(Locale.ROOT).contains(query)) {
                filteredTargets.add(candidate);
            }
        }
        targetScrollY = 0;
    }

    private void pickTarget(MultiblockMachineDefinition chosen) {
        CinderSchemaData.setTarget(cinderCoreStack, chosen);
        PhoenixNetwork.CHANNEL.sendToServer(new C2SCinderSetTargetPacket(hand, chosen.getId()));
        reload();
    }

    private void changeTarget() {
        CinderSchemaData.clearTarget(cinderCoreStack);
        PhoenixNetwork.CHANNEL.sendToServer(new C2SCinderClearTargetPacket(hand));
        reload();
    }

    private void resetToDefaults() {
        if (definition == null || schemaInfo == null) return;
        schemaInfo.getUserSliceRepeats().clear();
        if (schemaInfo.getStructureHelper() != null) {
            schemaInfo.getStructureHelper().getBlockPreferences().clear();
        }
        for (int i = 0; i < pattern.getSlices().length; i++) {
            schemaInfo.getUserSliceRepeats().put(i, pattern.getSlices()[i].getMinRepeats());
        }
        refreshResolution();
    }

    private void refreshResolution() {
        if (definition == null || schemaInfo == null) return;
        schemaInfo.refreshSchema(definition, Direction.NORTH, Direction.UP, false, null);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        clickRegions.clear();

        int left = panelLeft();
        int top = panelTop();
        int panelH = PANEL_H;

        drawPanel(g, left, top, PANEL_W, panelH);
        g.drawString(font, "Rebirth Cinder Core", left + 8, top + 7, cAccent, false);

        if (definition == null || schemaInfo == null || pattern == null) {
            previewW = 0;
            previewH = 0;
            renderTargetPicker(g, mouseX, mouseY, left, top, panelH);
            super.render(g, mouseX, mouseY, partialTick);
            return;
        }

        g.drawString(font, definition.getBlock().getName().getString(), left + 8, top + 20, cText, false);

        int leftX = left + 8;
        int leftW = 234;
        int rightX = leftX + leftW + 8;
        int rightW = PANEL_W - leftW - 24;
        int contentY = top + 36;
        int buttonY = top + panelH - BUTTON_H - 6;
        int contentH = buttonY - 6 - contentY;

        double scale = Minecraft.getInstance().getWindow().getGuiScale();
        RenderSystem.enableScissor((int) (leftX * scale), (int) ((height - contentY - contentH) * scale),
                (int) (leftW * scale), (int) (contentH * scale));

        int y = contentY - scrollY;
        contentHeight = 0;
        for (SliceRow row : sliceRows) {
            y = renderSliceRow(g, mouseX, mouseY, leftX, leftW, y, row);
        }
        for (PredicateChoiceRow row : predicateRows) {
            y = renderPredicateRow(g, mouseX, mouseY, leftX, leftW, y, row);
        }
        contentHeight = y - (contentY - scrollY);

        RenderSystem.disableScissor();

        previewX = rightX;
        previewY = contentY;
        previewW = rightW;
        previewH = 150;
        drawBorder(g, previewX - 1, previewY - 1, previewW + 2, previewH + 2, cBorder);
        g.fill(previewX, previewY, previewX + previewW, previewY + previewH, cRowBg);
        structurePreview.render(g, previewX, previewY, previewW, previewH, schemaInfo.getStructureBlocks(),
                partialTick);
        g.drawString(font, "(drag to rotate)", previewX + 4, previewY + previewH - 10, cTextDim, false);

        int summaryY = previewY + previewH + 8;
        int summaryH = buttonY - 6 - summaryY;
        renderMaterialSummary(g, rightX, rightW, summaryY, summaryH);

        drawButton(g, mouseX, mouseY, left + 8, buttonY, 90, BUTTON_H, "Change Target", this::changeTarget);
        drawButton(g, mouseX, mouseY, left + 8 + 96, buttonY, 70, BUTTON_H, "Reset", this::resetToDefaults);
        drawButton(g, mouseX, mouseY, left + PANEL_W - 78, buttonY, 70, BUTTON_H, "Confirm", () -> {
            if (definition != null && schemaInfo != null) {
                PhoenixNetwork.CHANNEL.sendToServer(
                        C2SCinderConfigPacket.fromSchema(hand, definition, schemaInfo));
            }
            onClose();
        });

        if (openDropdown != null) {
            renderDropdownOverlay(g, mouseX, mouseY, leftX, leftW, contentY, contentH);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderTargetPicker(GuiGraphics g, int mouseX, int mouseY, int left, int top, int panelH) {
        int listX = left + 8;
        int listW = PANEL_W - 16;
        int listY = top + 40;
        int listH = panelH - 48;

        if (filteredTargets.isEmpty()) {
            g.drawString(font, allTargets.isEmpty() ? "No buildable multiblocks found." : "No matches.",
                    listX, listY, cTextDim, false);
            return;
        }

        double scale = Minecraft.getInstance().getWindow().getGuiScale();
        RenderSystem.enableScissor((int) (listX * scale), (int) ((height - listY - listH) * scale),
                (int) (listW * scale), (int) (listH * scale));

        int maxScroll = Math.max(0, filteredTargets.size() * TARGET_ROW_H - listH);
        targetScrollY = Math.min(targetScrollY, maxScroll);
        int y = listY - targetScrollY;

        for (MultiblockMachineDefinition candidate : filteredTargets) {
            if (y + TARGET_ROW_H >= listY && y < listY + listH) {
                boolean hovered = mouseX >= listX && mouseX < listX + listW && mouseY >= y &&
                        mouseY < y + TARGET_ROW_H && mouseY >= listY && mouseY < listY + listH;
                g.fill(listX, y, listX + listW, y + TARGET_ROW_H - 1, hovered ? cRowHover : cRowBg);
                g.drawString(font, truncate(candidate.getBlock().getName().getString(), listW - 8), listX + 4,
                        y + 5, hovered ? cText : cTextDim, false);

                int rowY = y;
                clickRegions.add(new ClickRegion(listX, rowY, listW, TARGET_ROW_H - 1,
                        () -> pickTarget(candidate)));
            }
            y += TARGET_ROW_H;
        }

        RenderSystem.disableScissor();
    }

    private int renderSliceRow(GuiGraphics g, int mouseX, int mouseY, int x, int w, int y, SliceRow row) {
        boolean hovered = mouseY >= y && mouseY < y + ROW_H;
        g.fill(x, y, x + w, y + ROW_H, hovered ? cRowHover : cRowBg);
        g.drawString(font, row.label(), x + 4, y + 6, cTextDim, false);

        int value = schemaInfo.getUserSliceRepeats().getOrDefault(row.sliceIndex(), row.min());
        int stepperX = x + w - 70;

        drawButton(g, mouseX, mouseY, stepperX, y + 2, 16, 16, "-", () -> {
            int cur = schemaInfo.getUserSliceRepeats().getOrDefault(row.sliceIndex(), row.min());
            if (cur > row.min()) {
                schemaInfo.getUserSliceRepeats().put(row.sliceIndex(), cur - 1);
                refreshResolution();
            }
        });
        g.drawCenteredString(font, String.valueOf(value), stepperX + 34, y + 6, cText);
        drawButton(g, mouseX, mouseY, stepperX + 52, y + 2, 16, 16, "+", () -> {
            int cur = schemaInfo.getUserSliceRepeats().getOrDefault(row.sliceIndex(), row.min());
            if (cur < row.max()) {
                schemaInfo.getUserSliceRepeats().put(row.sliceIndex(), cur + 1);
                refreshResolution();
            }
        });

        return y + ROW_H + 1;
    }

    private int renderPredicateRow(GuiGraphics g, int mouseX, int mouseY, int x, int w, int y,
                                   PredicateChoiceRow row) {
        boolean hovered = mouseY >= y && mouseY < y + ROW_H;
        g.fill(x, y, x + w, y + ROW_H, hovered ? cRowHover : cRowBg);
        String label = truncate(row.label(), w - DROPDOWN_ROW_W - 12);
        g.drawString(font, label, x + 4, y + 6, cTextDim, false);

        BlockInfo selected = pickSelected(row);
        String selectedLabel = selected != null ? selected.getItemStackForm().getHoverName().getString() : "?";

        int ddX = x + w - DROPDOWN_ROW_W;
        boolean isOpen = row.equals(openDropdown);
        drawButton(g, mouseX, mouseY, ddX, y + 2, DROPDOWN_ROW_W, ROW_H - 4,
                truncate(selectedLabel, DROPDOWN_ROW_W - 16) + (isOpen ? " ▲" : " ▼"),
                () -> openDropdown = isOpen ? null : row);

        return y + ROW_H + 1;
    }

    private @Nullable BlockInfo pickSelected(PredicateChoiceRow row) {
        if (schemaInfo == null || schemaInfo.getStructureHelper() == null)
            return row.basePredicate().getFirstCandidate()
                    .orElse(null);
        BlockInfo chosen = schemaInfo.getStructureHelper().getBlockPreferences().get(row.predicate(),
                row.basePredicate());
        return chosen != null ? chosen : row.basePredicate().getFirstCandidate().orElse(null);
    }

    private void renderDropdownOverlay(GuiGraphics g, int mouseX, int mouseY, int contentX, int contentW,
                                       int contentY, int contentH) {
        PredicateChoiceRow row = openDropdown;
        List<BlockInfo> candidates = row.basePredicate().getCandidates();

        int y = contentY - scrollY;
        int rowTop = -1;
        for (SliceRow s : sliceRows) {
            y += ROW_H + 1;
        }
        for (PredicateChoiceRow p : predicateRows) {
            if (p.equals(row)) {
                rowTop = y;
                break;
            }
            y += ROW_H + 1;
        }
        if (rowTop == -1) {
            openDropdown = null;
            return;
        }

        int ddX = contentX + contentW - DROPDOWN_ROW_W;
        int ddY = rowTop + ROW_H;
        int itemsH = Math.min(candidates.size() * DROPDOWN_ITEM_H, 8 * DROPDOWN_ITEM_H);

        g.pose().pushPose();
        g.pose().translate(0, 0, 300.0);
        g.fill(ddX, ddY, ddX + DROPDOWN_ROW_W, ddY + itemsH, cDropdownBg);
        drawBorder(g, ddX, ddY, DROPDOWN_ROW_W, itemsH, cBorder);

        for (int i = 0; i < candidates.size() && i * DROPDOWN_ITEM_H < itemsH; i++) {
            BlockInfo candidate = candidates.get(i);
            int itemY = ddY + i * DROPDOWN_ITEM_H;
            boolean hovered = mouseX >= ddX && mouseX < ddX + DROPDOWN_ROW_W && mouseY >= itemY &&
                    mouseY < itemY + DROPDOWN_ITEM_H;
            if (hovered) g.fill(ddX, itemY, ddX + DROPDOWN_ROW_W, itemY + DROPDOWN_ITEM_H, cRowHover);
            String name = truncate(candidate.getItemStackForm().getHoverName().getString(), DROPDOWN_ROW_W - 8);
            g.drawString(font, name, ddX + 4, itemY + 4, hovered ? cText : cTextDim, false);

            clickRegions.add(new ClickRegion(ddX, itemY, DROPDOWN_ROW_W, DROPDOWN_ITEM_H, () -> {
                if (schemaInfo != null) {
                    schemaInfo.putPredicatePreference(row.predicate(), row.basePredicate(), candidate);
                    refreshResolution();
                }
                openDropdown = null;
            }));
        }
        g.pose().popPose();
    }

    private void renderMaterialSummary(GuiGraphics g, int x, int w, int y, int h) {
        g.fill(x, y, x + w, y + h, cRowBg);
        drawBorder(g, x, y, w, h, cBorder);
        g.drawString(font, "Materials needed", x + 4, y + 4, cAccent, false);

        if (schemaInfo == null) return;

        var required = schemaInfo.getBlockCounts();
        var stocked = CinderSchemaData.tallyStocked(cinderCoreStack);

        int line = 0;
        int maxLines = (h - 16) / 10;
        for (var entry : required.reference2IntEntrySet()) {
            if (line >= maxLines) {
                g.drawString(font, "...and more (see final confirm report)", x + 4, y + 14 + line * 10, cTextDim,
                        false);
                break;
            }
            int need = entry.getIntValue();
            int have = stocked.getOrDefault(entry.getKey(), 0);
            boolean ok = have >= need;
            String text = entry.getKey().getName().getString() + ": " + have + "/" + need;
            g.drawString(font, truncate(text, w - 8), x + 4, y + 14 + line * 10, ok ? cOk : cBad, false);
            line++;
        }
    }

    private void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, cPanel);
        drawBorder(g, x, y, w, h, cBorder);
    }

    private void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void drawButton(GuiGraphics g, int mouseX, int mouseY, int x, int y, int w, int h, String label,
                            Runnable action) {
        boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        g.fill(x, y, x + w, y + h, hovered ? cRowHover : cRowBg);
        drawBorder(g, x, y, w, h, hovered ? cAccent : cBorder);
        g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, hovered ? cAccent : cText);
        clickRegions.add(new ClickRegion(x, y, w, h, action));
    }

    private String truncate(String s, int maxPx) {
        if (s == null) return "";
        while (font.width(s) > maxPx && s.length() > 2) {
            s = s.substring(0, s.length() - 2) + "…";
        }
        return s;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int i = clickRegions.size() - 1; i >= 0; i--) {
                ClickRegion region = clickRegions.get(i);
                if (region.contains(mouseX, mouseY)) {
                    region.action().run();
                    return true;
                }
            }
            if (openDropdown != null) {
                openDropdown = null;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && mouseX >= previewX && mouseX < previewX + previewW && mouseY >= previewY &&
                mouseY < previewY + previewH) {
            structurePreview.mouseDragged(dragX, dragY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= previewX && mouseX < previewX + previewW && mouseY >= previewY &&
                mouseY < previewY + previewH) {
            structurePreview.mouseScrolled(delta);
            return true;
        }
        if (definition == null || schemaInfo == null || pattern == null) {
            int listH = PANEL_H - 48;
            int maxScroll = Math.max(0, filteredTargets.size() * TARGET_ROW_H - listH);
            targetScrollY = Math.max(0, Math.min(targetScrollY - (int) (delta * 12), maxScroll));
            return true;
        }
        int panelH = PANEL_H;
        int buttonY = panelTop() + panelH - BUTTON_H - 6;
        int contentY = panelTop() + 36;
        int maxScroll = Math.max(0, contentHeight - (buttonY - 6 - contentY));
        scrollY = Math.max(0, Math.min(scrollY - (int) (delta * 12), maxScroll));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void removed() {
        structurePreview.close();
        super.removed();
    }
}
