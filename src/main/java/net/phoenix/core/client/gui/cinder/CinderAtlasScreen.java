package net.phoenix.core.client.gui.cinder;

import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.phoenix.core.common.item.cinder.CinderAtlasData;
import net.phoenix.core.common.item.cinder.CinderAtlasItem;
import net.phoenix.core.common.item.cinder.CinderAtlasUpgrades;
import net.phoenix.core.common.item.cinder.CinderSchemaData;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.network.packet.C2SCinderAtlasClearSlotPacket;
import net.phoenix.core.network.packet.C2SCinderAtlasOpenCraftingTerminalPacket;
import net.phoenix.core.network.packet.C2SCinderAtlasRenameLoadoutPacket;
import net.phoenix.core.network.packet.C2SCinderAtlasRequestMaterialsPacket;
import net.phoenix.core.network.packet.C2SCinderAtlasSetActiveLoadoutPacket;
import net.phoenix.core.network.packet.C2SCinderAtlasSetActiveSlotPacket;
import net.phoenix.core.network.packet.C2SCinderAtlasSetSlotTargetPacket;
import net.phoenixvine.wiki.theme.PhoenixTheme;

import com.mojang.blaze3d.systems.RenderSystem;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The Cinder Atlas's loadout editor - see
 * {@code docs/content/Development/Systems/cinder_atlas_deployment_tool.md}. This is the "tabs + slot
 * grid + rename + assign" slice only: pick a loadout, pick a slot in it, pick a target for that slot
 * from the same search list {@link CinderConfiguratorScreen} uses. No sort/pagination controls and no
 * Sound/Preview/HUD toggles from the reference UI yet - those either need systems that don't exist yet
 * (Preview reuses the ghost-preview pipeline, which isn't wired to the Atlas at all) or aren't load
 * -bearing enough to add as inert placeholders. Deep per-slot configuration (slice repeats, block
 * preferences) isn't here either - a slot only gets a target for now, the same starting point a fresh
 * Cinder Core has before its own Configurator screen lets you refine it.
 * <p>
 * Upgrades ({@link CinderAtlasUpgrades}) are shown here read-only (icon strip + wireless range) but
 * installed/removed through the Cinder Forge multiblock instead of this screen - see
 * {@code CinderForgeMachine}'s Atlas/Upgrade slots - matching the existing "interact with the Forge to
 * configure a Cinder item" convention this codebase already uses for Cores, rather than adding a second,
 * inconsistent way to do the same kind of thing.
 * <p>
 * Follows {@link CinderConfiguratorScreen}'s exact structural pattern: a hand-rolled {@code Screen}
 * (no ModularUI - this is a client-only item screen, not a machine GUI), a flat list of click regions
 * checked in {@code mouseClicked}, {@link PhoenixTheme} for colors, and local-edit-then-packet for
 * persistence (this screen holds the player's actual live-held stack reference, so editing its NBT
 * through {@link CinderAtlasData}/{@link CinderSchemaData} updates what's visibly held immediately;
 * the packets just replicate the same edit onto the server's authoritative copy).
 * <p>
 * The panel's gradient background, pulsing title underline (with a traveling "spark" along it), and the
 * glow halo around the active loadout tab / selected slot are ported from {@code PhoenixThemeEditorScreen}
 * (`net.phoenixvine.wiki.theme`, the phoenixwiki library this project depends on) - its
 * {@code animPulse}/{@code blend} helpers and glow-border technique are that library's actual visual
 * signature (used for its own "active" node and animated connector spark), reused here rather than
 * inventing a new flourish style, so the Atlas reads as part of the same suite instead of a flat GTCEu
 * panel with borrowed colors. All motion respects {@link PhoenixTheme#isReduceMotion()}.
 */
public class CinderAtlasScreen extends Screen {

    private static final int PANEL_W = 460;
    private static final int PANEL_H = 420;
    private static final int PAD = 12;
    private static final int SECTION_GAP = 12;
    private static final int TAB_H = 20;
    private static final int SLOT_SIZE = 44;
    private static final int SLOT_GAP = 6;
    private static final int TARGET_ROW_H = 20;
    private static final int BUTTON_H = 20;
    private static final int UPGRADE_ICON_SIZE = 18;
    private static final int UPGRADE_ICON_GAP = 5;
    private static final int UPGRADE_STRIP_H = 20;
    private static final int FIELD_H = 16;

    private int cPanel, cBorder, cAccent, cText, cTextDim, cRowBg, cRowHover, cSelected;

    private record ClickRegion(int x, int y, int w, int h, Runnable action) {

        boolean contains(double px, double py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }
    }

    private final InteractionHand hand;
    private final List<ClickRegion> clickRegions = new ArrayList<>();

    private ItemStack atlasStack = ItemStack.EMPTY;
    private int activeLoadout = 0;
    private @Nullable Integer selectedSlot = null;

    private @Nullable EditBox renameBox;
    private @Nullable EditBox filterBox;

    private final List<MultiblockMachineDefinition> allTargets = new ArrayList<>();
    private final List<MultiblockMachineDefinition> filteredTargets = new ArrayList<>();
    private int targetScrollY = 0;

    public CinderAtlasScreen(InteractionHand hand) {
        super(Component.literal("Cinder Atlas"));
        this.hand = hand;
    }

    public static void open(InteractionHand hand) {
        Minecraft.getInstance().setScreen(new CinderAtlasScreen(hand));
    }

    private int panelLeft() {
        return (width - PANEL_W) / 2;
    }

    private int panelTop() {
        return Math.max(16, (height - PANEL_H) / 2);
    }

    // --- vertical layout, each section's Y derived from the one before it so init() and render()
    // never duplicate the same magic numbers and drift apart. ---

    private int titleY(int top) {
        return top + 10;
    }

    private int tabsY(int top) {
        return top + 30;
    }

    private int loadoutNameLabelY(int top) {
        return tabsY(top) + TAB_H + SECTION_GAP;
    }

    private int renameBoxY(int top) {
        return loadoutNameLabelY(top) + 10;
    }

    private int slotsLabelY(int top) {
        return renameBoxY(top) + FIELD_H + SECTION_GAP;
    }

    private int slotsY(int top) {
        return slotsLabelY(top) + 12;
    }

    private int upgradesY(int top) {
        return slotsY(top) + SLOT_SIZE + SECTION_GAP;
    }

    private int filterBoxY(int top) {
        return upgradesY(top) + UPGRADE_STRIP_H + SECTION_GAP;
    }

    private int targetListTop(int top) {
        return filterBoxY(top) + FIELD_H + SECTION_GAP;
    }

    private void refreshTheme() {
        PhoenixTheme t = PhoenixTheme.current();
        cPanel = (t.panel.getColor() & 0x00FFFFFF) | 0xEC000000;
        cBorder = (t.border.getColor() & 0x00FFFFFF) | 0xFF000000;
        cAccent = (t.accent.getColor() & 0x00FFFFFF) | 0xFF000000;
        cText = (t.text.getColor() & 0x00FFFFFF) | 0xFF000000;
        cTextDim = (t.textDim.getColor() & 0x00FFFFFF) | 0xFF000000;
        cRowBg = (t.bg.getColor() & 0x00FFFFFF) | 0xD0000000;
        cRowHover = (t.header.getColor() & 0x00FFFFFF) | 0xD0000000;
        cSelected = (t.accent.getColor() & 0x00FFFFFF) | 0x50000000;
    }

    @Override
    protected void init() {
        super.init();
        refreshTheme();

        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        atlasStack = player.getItemInHand(hand);
        if (!(atlasStack.getItem() instanceof CinderAtlasItem)) return;

        activeLoadout = CinderAtlasData.getActiveLoadout(atlasStack);
        selectedSlot = CinderAtlasData.getActiveSlot(atlasStack);

        int left = panelLeft();
        int top = panelTop();

        renameBox = new EditBox(font, left + PAD, renameBoxY(top), 260, FIELD_H, Component.literal("Loadout name"));
        renameBox.setBordered(true);
        renameBox.setMaxLength(32);
        renameBox.setValue(CinderAtlasData.getLoadoutName(atlasStack, activeLoadout));
        addRenderableWidget(renameBox);

        filterBox = new EditBox(font, left + PAD, filterBoxY(top), PANEL_W - PAD * 2, FIELD_H,
                Component.literal("Filter"));
        filterBox.setBordered(true);
        filterBox.setMaxLength(64);
        filterBox.setResponder(s -> filterTargets());
        filterBox.setHint(Component.literal("Search design, multiblock or machine ID..."));
        addRenderableWidget(filterBox);

        populateTargetList();
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

    private void switchLoadout(int loadoutIndex) {
        if (loadoutIndex == activeLoadout) return;
        activeLoadout = loadoutIndex;
        selectedSlot = null;
        CinderAtlasData.setActiveLoadout(atlasStack, loadoutIndex);
        PhoenixNetwork.CHANNEL.sendToServer(new C2SCinderAtlasSetActiveLoadoutPacket(hand, loadoutIndex));
        if (renameBox != null) renameBox.setValue(CinderAtlasData.getLoadoutName(atlasStack, activeLoadout));
    }

    private void selectSlot(int slotIndex) {
        selectedSlot = selectedSlot != null && selectedSlot == slotIndex ? null : slotIndex;
        if (selectedSlot != null) {
            // Persisted as the slot that actually deploys in-world (see CinderDeploySource), not just
            // this screen's own highlight state - deselecting (click-to-toggle-off above) intentionally
            // leaves the last selection as the deploy target rather than clearing it to "nothing".
            CinderAtlasData.setActiveSlot(atlasStack, slotIndex);
            PhoenixNetwork.CHANNEL.sendToServer(new C2SCinderAtlasSetActiveSlotPacket(hand, slotIndex));
        }
    }

    private void renameActiveLoadout() {
        if (renameBox == null) return;
        String name = renameBox.getValue().trim();
        if (name.isEmpty()) return;
        CinderAtlasData.setLoadoutName(atlasStack, activeLoadout, name);
        PhoenixNetwork.CHANNEL.sendToServer(new C2SCinderAtlasRenameLoadoutPacket(hand, activeLoadout, name));
    }

    private void assignTargetToSelectedSlot(MultiblockMachineDefinition chosen) {
        if (selectedSlot == null) return;
        CompoundTag slotTag = CinderAtlasData.getOrCreateSlotTag(atlasStack, activeLoadout, selectedSlot);
        CinderSchemaData.setTarget(slotTag, chosen);
        PhoenixNetwork.CHANNEL.sendToServer(
                new C2SCinderAtlasSetSlotTargetPacket(hand, activeLoadout, selectedSlot, chosen.getId()));
    }

    private void clearSelectedSlot() {
        if (selectedSlot == null) return;
        CinderAtlasData.clearSlot(atlasStack, activeLoadout, selectedSlot);
        PhoenixNetwork.CHANNEL.sendToServer(new C2SCinderAtlasClearSlotPacket(hand, activeLoadout, selectedSlot));
    }

    /** Design doc feature #4 - "prepare ahead" - real, headless auto-request of whatever the active
     *  slot is still missing from the linked network, independent of any deploy attempt. Result is
     *  reported back via chat as each item's crafting calculation resolves (see
     *  {@code CinderAtlasWirelessLink#requestMissingMaterials}), not shown in this screen directly. */
    private void requestMaterials() {
        PhoenixNetwork.CHANNEL.sendToServer(new C2SCinderAtlasRequestMaterialsPacket(hand));
    }

    /** Manual escape hatch alongside {@link #requestMaterials()} - opens AE2's real crafting terminal so
     *  the player can browse network stock or request specific items themselves. */
    private void openCraftingTerminal() {
        PhoenixNetwork.CHANNEL.sendToServer(new C2SCinderAtlasOpenCraftingTerminalPacket(hand));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        clickRegions.clear();

        if (!(atlasStack.getItem() instanceof CinderAtlasItem)) {
            onClose();
            return;
        }

        int left = panelLeft();
        int top = panelTop();

        drawPanel(g, left, top, PANEL_W, PANEL_H);
        g.drawString(font, "Cinder Atlas", left + PAD, titleY(top), cAccent, false);
        renderTitleUnderline(g, left, top);

        renderTabs(g, mouseX, mouseY, left, top);

        g.drawString(font, "Loadout name", left + PAD, loadoutNameLabelY(top), cTextDim, false);
        if (renameBox != null) {
            drawButton(g, mouseX, mouseY, left + PAD + 268, renameBoxY(top), 72, FIELD_H, "Rename",
                    this::renameActiveLoadout);
        }

        g.drawString(font, "Pattern slots", left + PAD, slotsLabelY(top), cTextDim, false);
        ItemStack hoveredSlot = renderSlots(g, mouseX, mouseY, left, slotsY(top));
        ItemStack hoveredUpgrade = renderUpgrades(g, mouseX, mouseY, left, upgradesY(top));
        renderTargetList(g, mouseX, mouseY, left, targetListTop(top));

        int buttonY = top + PANEL_H - BUTTON_H - PAD;
        drawButton(g, mouseX, mouseY, left + PAD, buttonY, 130, BUTTON_H, "Request Materials",
                this::requestMaterials);
        drawButton(g, mouseX, mouseY, left + PAD + 138, buttonY, 118, BUTTON_H, "Open Terminal",
                this::openCraftingTerminal);

        drawButton(g, mouseX, mouseY, left + PANEL_W - PAD - 76, buttonY, 76, BUTTON_H, "Close",
                this::onClose);

        // Drawn last, after every other row/list that might otherwise render on top of it.
        ItemStack hoveredIcon = hoveredSlot != null ? hoveredSlot : hoveredUpgrade;
        if (hoveredIcon != null) g.renderTooltip(font, hoveredIcon, mouseX, mouseY);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderTabs(GuiGraphics g, int mouseX, int mouseY, int left, int top) {
        int capacity = CinderAtlasUpgrades.getLoadoutCapacity(atlasStack);
        int y = tabsY(top);
        int totalW = PANEL_W - PAD * 2;
        int tabW = (totalW - (capacity - 1) * 4) / capacity;
        for (int i = 0; i < capacity; i++) {
            int x = left + PAD + i * (tabW + 4);
            boolean active = i == activeLoadout;
            boolean hovered = mouseX >= x && mouseX < x + tabW && mouseY >= y && mouseY < y + TAB_H;
            if (active) drawGlowBorder(g, x, y, tabW, TAB_H, cAccent);
            g.fill(x, y, x + tabW, y + TAB_H, active ? cSelected : (hovered ? cRowHover : cRowBg));
            drawBorder(g, x, y, tabW, TAB_H, active ? cAccent : cBorder);
            String label = truncate(CinderAtlasData.getLoadoutName(atlasStack, i), tabW - 10);
            g.drawCenteredString(font, label, x + tabW / 2, y + 6, active ? cAccent : cText);

            int loadoutIndex = i;
            clickRegions.add(new ClickRegion(x, y, tabW, TAB_H, () -> switchLoadout(loadoutIndex)));
        }
    }

    /** Returns the hovered slot's icon (for the caller to draw its tooltip last, same convention as
     *  {@link #renderUpgrades} below) - a slot's target is a real block, shown as its own item icon
     *  instead of just the block's name, with the name itself still available as a hover tooltip. */
    private @Nullable ItemStack renderSlots(GuiGraphics g, int mouseX, int mouseY, int left, int y) {
        ItemStack hoveredIcon = null;
        for (int i = 0; i < CinderAtlasData.SLOT_COUNT; i++) {
            int x = left + PAD + i * (SLOT_SIZE + SLOT_GAP);
            boolean configured = CinderAtlasData.isSlotConfigured(atlasStack, activeLoadout, i);
            boolean selected = selectedSlot != null && selectedSlot == i;
            boolean hovered = mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE;

            if (selected) drawGlowBorder(g, x, y, SLOT_SIZE, SLOT_SIZE, cAccent);
            g.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, selected ? cSelected : (hovered ? cRowHover : cRowBg));
            drawBorder(g, x, y, SLOT_SIZE, SLOT_SIZE, selected ? cAccent : cBorder);

            g.drawString(font, String.valueOf(i + 1), x + 3, y + 3, cTextDim, false);

            ItemStack icon = ItemStack.EMPTY;
            if (configured) {
                CompoundTag slotTag = CinderAtlasData.peekSlotTag(atlasStack, activeLoadout, i);
                var definition = slotTag != null ? CinderSchemaData.getTargetDefinition(slotTag) : null;
                if (definition != null) icon = new ItemStack(definition.getBlock());
            }
            if (!icon.isEmpty()) {
                g.renderItem(icon, x + (SLOT_SIZE - 16) / 2, y + (SLOT_SIZE - 16) / 2);
                if (hovered) hoveredIcon = icon;
            } else {
                g.drawCenteredString(font, "empty", x + SLOT_SIZE / 2, y + SLOT_SIZE / 2 - 4, cTextDim);
            }

            int slotIndex = i;
            clickRegions.add(new ClickRegion(x, y, SLOT_SIZE, SLOT_SIZE, () -> selectSlot(slotIndex)));
        }

        if (selectedSlot != null && CinderAtlasData.isSlotConfigured(atlasStack, activeLoadout, selectedSlot)) {
            int clearX = left + PAD + CinderAtlasData.SLOT_COUNT * (SLOT_SIZE + SLOT_GAP) + 6;
            drawButton(g, mouseX, mouseY, clearX, y, 64, SLOT_SIZE, "Clear", this::clearSelectedSlot);
        }
        return hoveredIcon;
    }

    /**
     * Read-only - installing/removing upgrades happens at the Cinder Forge multiblock (see
     * {@code CinderForgeMachine}), matching how the Forge is already this codebase's "assemble/configure
     * a Cinder item" station rather than adding a second, competing way to do the same kind of thing
     * from this handheld screen. Still shows a hover tooltip on each installed icon (its name/effect
     * description from {@link net.phoenix.core.common.item.cinder.CinderAtlasUpgradeItem#appendHoverText})
     * - being read-only doesn't mean unlabeled. Returns the hovered stack rather than drawing its
     * tooltip directly, so the caller can draw it last and avoid it being overdrawn by the target list
     * rendered afterward.
     */
    private @Nullable ItemStack renderUpgrades(GuiGraphics g, int mouseX, int mouseY, int left, int y) {
        g.drawString(font, "Upgrades", left + PAD, y, cTextDim, false);

        ItemStack hovered = null;
        int slotsX = left + PAD + font.width("Upgrades") + 10;
        for (int i = 0; i < CinderAtlasData.UPGRADE_SLOT_COUNT; i++) {
            int x = slotsX + i * (UPGRADE_ICON_SIZE + UPGRADE_ICON_GAP);
            ItemStack upgrade = CinderAtlasData.getUpgradeSlot(atlasStack, i);
            boolean isHovered = mouseX >= x && mouseX < x + UPGRADE_ICON_SIZE && mouseY >= y - 2 &&
                    mouseY < y - 2 + UPGRADE_ICON_SIZE;

            g.fill(x, y - 2, x + UPGRADE_ICON_SIZE, y - 2 + UPGRADE_ICON_SIZE, cRowBg);
            drawBorder(g, x, y - 2, UPGRADE_ICON_SIZE, UPGRADE_ICON_SIZE, isHovered && !upgrade.isEmpty() ? cAccent :
                    cBorder);
            if (!upgrade.isEmpty()) {
                g.renderItem(upgrade, x + 1, y - 1);
                if (isHovered) hovered = upgrade;
            }
        }

        double range = CinderAtlasUpgrades.getWirelessRange(atlasStack);
        String rangeText = Double.isInfinite(range) ? "Range: unlimited" : "Range: " + (int) range + " blocks";
        g.drawString(font, rangeText, slotsX + CinderAtlasData.UPGRADE_SLOT_COUNT *
                (UPGRADE_ICON_SIZE + UPGRADE_ICON_GAP) + 10, y + 5, cTextDim, false);

        return hovered;
    }

    private void renderTargetList(GuiGraphics g, int mouseX, int mouseY, int left, int listY) {
        int listX = left + PAD;
        int listW = PANEL_W - PAD * 2;
        int listH = panelTop() + PANEL_H - BUTTON_H - PAD * 2 - listY;

        String hint = selectedSlot == null ? "Select a slot above, then pick a design to assign it." : null;
        if (hint != null) {
            g.drawString(font, hint, listX, listY, cTextDim, false);
            listY += 14;
            listH -= 14;
        }

        if (filteredTargets.isEmpty()) {
            g.drawString(font, allTargets.isEmpty() ? "No buildable multiblocks found." : "No matches.", listX,
                    listY, cTextDim, false);
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
                g.fill(listX, y, listX + listW, y + TARGET_ROW_H - 2, hovered ? cRowHover : cRowBg);
                g.drawString(font, truncate(candidate.getBlock().getName().getString(), listW - 10), listX + 5,
                        y + 6, hovered ? cText : cTextDim, false);

                int rowY = y;
                clickRegions.add(new ClickRegion(listX, rowY, listW, TARGET_ROW_H - 2,
                        () -> assignTargetToSelectedSlot(candidate)));
            }
            y += TARGET_ROW_H;
        }

        RenderSystem.disableScissor();
    }

    private void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
        g.fillGradient(x, y, x + w, y + h, cPanel, blend(cPanel, 0xFF000000, 0.35f));
        drawBorder(g, x, y, w, h, cBorder);
    }

    /** Title-row flourish ported from {@code PhoenixThemeEditorScreen}'s header underline: a pulsing
     *  accent line under the title, with a brighter "spark" traveling along it - the same technique that
     *  library uses for its own animated connector line, reused verbatim rather than approximated. */
    private void renderTitleUnderline(GuiGraphics g, int left, int top) {
        int lineX0 = left + PAD;
        int lineX1 = lineX0 + font.width("Cinder Atlas");
        int lineY = titleY(top) + 10;

        float basePulse = animPulse(0.7f, 0.3f, 900.0);
        int lineA = Math.min(255, (int) (0xFF * basePulse));
        g.fill(lineX0, lineY, lineX1, lineY + 1, (lineA << 24) | (cAccent & 0xFFFFFF));

        if (!PhoenixTheme.isReduceMotion() && lineX1 > lineX0) {
            float sparkT = (float) ((System.currentTimeMillis() / 900.0) % 1.0);
            int sparkX = lineX0 + (int) (sparkT * (lineX1 - lineX0));
            float sparkPulse = animPulse(0.75f, 0.25f, 200.0);
            int sparkA = Math.min(255, (int) (0xFF * sparkPulse));
            g.fill(sparkX - 1, lineY - 1, sparkX + 2, lineY + 2, (sparkA << 24) | (cAccent & 0xFFFFFF));
        }
    }

    /** Soft halo around an "active"/"selected" element, same technique as
     *  {@code PhoenixThemeEditorScreen}'s glowing active-node border - an inset-by-2 pulsing-alpha border
     *  drawn behind the element's own solid border. */
    private void drawGlowBorder(GuiGraphics g, int x, int y, int w, int h, int rgb) {
        float pulse = animPulse(0.6f, 0.4f, 500.0);
        int glowA = Math.min(255, (int) (0x55 * pulse));
        drawBorder(g, x - 2, y - 2, w + 4, h + 4, (glowA << 24) | (rgb & 0xFFFFFF));
    }

    private static float animPulse(float base, float amplitude, double periodDivisor) {
        if (PhoenixTheme.isReduceMotion()) return base;
        return base + amplitude * (float) Math.sin(System.currentTimeMillis() / periodDivisor);
    }

    private static int blend(int a, int b, float t) {
        int aa = (a >> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int ra = (int) (aa + (ba - aa) * t), rr = (int) (ar + (br - ar) * t);
        int rg = (int) (ag + (bg - ag) * t), rb = (int) (ab + (bb - ab) * t);
        return (ra << 24) | (rr << 16) | (rg << 8) | rb;
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
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int listY = targetListTop(panelTop());
        int listH = panelTop() + PANEL_H - BUTTON_H - PAD * 2 - listY;
        int maxScroll = Math.max(0, filteredTargets.size() * TARGET_ROW_H - listH);
        targetScrollY = Math.max(0, Math.min(targetScrollY - (int) (delta * 12), maxScroll));
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
}
