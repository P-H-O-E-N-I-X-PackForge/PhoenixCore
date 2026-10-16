package net.phoenix.core.integration.conflux.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.phoenix.core.integration.conflux.ConfluxDataType;
import net.minecraft.client.gui.components.EditBox;
import net.phoenix.core.integration.conflux.client.render.*;
import net.phoenix.core.integration.conflux.client.render.discipline.DisciplineRendererRegistry;
import net.phoenix.core.integration.conflux.network.C2SResearchUnlockPacket;
import net.phoenix.core.integration.conflux.research.ResearchNode;
import net.phoenix.core.integration.conflux.research.ResearchTree;
import net.phoenix.core.integration.conflux.research.ResearchTreeRegistry;
import net.phoenix.core.integration.conflux.terminal.ResearchTerminalBlockEntity;

import net.minecraft.world.item.ItemStack;
import net.phoenix.core.integration.conflux.research.ConfluxRewards;
import net.phoenixvine.wiki.client.rich.RichBlock;
import net.phoenixvine.wiki.client.rich.RichSpan;
import net.phoenixvine.wiki.client.rich.WikiMarkdownParser;
import net.phoenixvine.wiki.client.rich.WikiRichTextRenderer;

import com.mojang.blaze3d.systems.RenderSystem;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class ResearchTerminalScreen extends Screen {
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    private static final int TAB_H = 24;
    private static final int SIDEBAR_W = 150;
    private static final int BOTTOM_H = 48;
    private static final int NODE_R = 22;

    private static final int C_BG = 0xFF07070F;
    private static final int C_CANVAS = 0xFF09090E;
    private static final int C_PANEL = 0xFF0F0F1C;
    private static final int C_HEADER = 0xFF0B0B18;
    private static final int C_BORDER = 0xFF252540;
    private static final int C_ACCENT = 0xFF5577FF;
    private static final int C_TEXT = 0xFFCCCCEE;
    private static final int C_DIM = 0xFF555577;

    @Nullable
    private final ResearchTerminalBlockEntity terminal;

    private float panX = 0, panY = 0;
    private static final int ROW_H = 30;
    private static final int TEXT_PIN = 0xFFFFDD55;
    private static final int GROUP_H = 16;
    private EditBox searchBox;
    private boolean readyOnly = false;
    private boolean skipChar = false;
    private ResearchNode pendingFocus = null;
    private int sidebarScroll = 0;
    private int sidebarMax = 0;

    private static final float MIN_ZOOM = 0.08f;
    private float zoom = 1.0f;
    private boolean dragging = false;
    private double lastDragX, lastDragY;

    private ResearchNode selected = null;
    private int activeTreeIdx = 0;
    private List<ResearchTree> trees = new ArrayList<>();

    private final MotionClock clock = new MotionClock();
    private final IntensityController intensity = new IntensityController();
    private DisciplineRenderer activeRenderer = DisciplineRendererRegistry.getDefault();
    private String lastDisciplineId = null;
    private String shownDiscipline = null;
    private final Set<ResourceLocation> preloaded = new java.util.HashSet<>();
    private long lastFrameNanos = -1;

    private static final int MIN_CANVAS_W = 320;
    private static final int MIN_CANVAS_H = 240;
    private float uiScale = 1f;
    private int vw, vh;

    public ResearchTerminalScreen(ResearchTerminalBlockEntity terminal) {
        super(Component.literal("Axiom Research Terminal"));
        this.terminal = terminal;
    }

    public ResearchTerminalScreen() {
        this(null);
    }

    @Override
    protected void init() {
        float neededW = SIDEBAR_W + MIN_CANVAS_W;
        float neededH = TAB_H + BOTTOM_H + MIN_CANVAS_H;
        uiScale = (width < neededW || height < neededH) ?
                Math.min(width / neededW, height / neededH) : 1f;
        uiScale = Math.max(0.1f, uiScale);
        vw = Math.round(width / uiScale);
        vh = Math.round(height / uiScale);

        String keep = searchBox == null ? "" : searchBox.getValue();
        searchBox = new EditBox(font, 8, 38, SIDEBAR_W - 16, 14, Component.literal("Search"));
        searchBox.setMaxLength(40);
        searchBox.setHint(Component.literal("\u00a78Search research  ( / )"));
        searchBox.setBordered(true);
        searchBox.setValue(keep);
        searchBox.setResponder(v -> sidebarScroll = 0);
        addRenderableWidget(searchBox);

        rebuildTrees();
        resetPan();
        lastFrameNanos = -1;

        syncRenderer();
    }

    private void rebuildTrees() {
        String chosen = ClientResearchCache.getDisciplineInfo().disciplineId();
        ResourceLocation activeId = trees.isEmpty() || activeTreeIdx >= trees.size() ? null :
                trees.get(activeTreeIdx).id;

        List<ResearchTree> visible = new ArrayList<>();
        for (ResearchTree tree : ResearchTreeRegistry.INSTANCE.getAllTrees()) {
            if (!tree.isVisibleTo(chosen)) continue;
            visible.add(tree);
        }
        trees = visible;
        shownDiscipline = chosen;

        activeTreeIdx = 0;
        if (activeId != null) {
            for (int i = 0; i < trees.size(); i++) {
                if (trees.get(i).id.equals(activeId)) activeTreeIdx = i;
            }
        }
    }

    @Override
    public void onClose() {
        AxiomShaderManager.release();
        super.onClose();
    }

    private void resetPan() {
        panX = canvasW() / 2f;
        panY = canvasH() / 2f;
    }

    private int canvasW() {
        return vw - sideW();
    }

    private int accent() {
        return ConfluxTheme.accent();
    }

    private boolean sidebarOpen = true;
    private static final int RAIL_W = 18;

    private int sideW() {
        return sidebarOpen ? SIDEBAR_W : RAIL_W;
    }

    private int[] toggleBounds() {
        return sidebarOpen ? new int[] { SIDEBAR_W - 19, 5, SIDEBAR_W - 6, 18 } : new int[] { 3, 5, 15, 18 };
    }

    private void toggleSidebar() {
        int before = canvasLeft();
        sidebarOpen = !sidebarOpen;
        // keep the tree where it is on screen while the canvas grows or shrinks around it
        panX += before - canvasLeft();
        if (searchBox != null) {
            searchBox.visible = sidebarOpen;
            if (!sidebarOpen) searchBox.setFocused(false);
        }
    }

    private int canvasLeft() {
        return sideW();
    }


    private int canvasH() {
        return vh - TAB_H - BOTTOM_H;
    }

    private int canvasTop() {
        return TAB_H;
    }

    private int bottomTop() {
        return vh - BOTTOM_H;
    }

    @Override
    public void render(GuiGraphics g, int rmx, int rmy, float pt) {
        long now = System.nanoTime();
        float dt = lastFrameNanos < 0 ? 0.016f : (float) ((now - lastFrameNanos) / 1_000_000_000.0);
        lastFrameNanos = now;
        dt = Math.min(dt, 0.1f);

        if (!java.util.Objects.equals(shownDiscipline, ClientResearchCache.getDisciplineInfo().disciplineId())) {
            rebuildTrees();
        }
        preloadNextShader();

        syncRenderer();

        clock.tick(dt);
        intensity.tick(dt);
        AxiomShaderManager.tick(dt);
        pushShaderData(rmx, rmy);

        int mx = Math.round(rmx / uiScale);
        int my = Math.round(rmy / uiScale);

        g.pose().pushPose();
        g.pose().scale(uiScale, uiScale, 1f);

        int cw = canvasW(), ct = canvasTop(), cl = canvasLeft();
        boolean hoveringCanvas = mx >= cl && mx < cl + cw && my >= ct && my < ct + canvasH();
        if (hoveringCanvas) intensity.onHover();
        else intensity.onHoverEnd();

        RenderContext ctx = buildContext(mx, my);
        if (ctx == null) {
            g.fill(0, 0, vw, vh, C_BG);
            renderSidebar(g, mx, my);
            renderTopBar(g, mx, my);
            renderBottomBar(g);
            super.render(g, mx, my, pt);
            g.pose().popPose();
            return;
        }

        if (pendingFocus != null) {
            float[] fp = activeRenderer.nodePos(pendingFocus, ctx);
            panX = canvasW() / 2f - fp[0] * zoom;
            panY = canvasH() / 2f - fp[1] * zoom;
            pendingFocus = null;
        }

        activeRenderer.tick(dt, ctx);

        g.fill(0, 0, vw, vh, C_BG);
        renderSidebar(g, mx, my);
        renderTopBar(g, mx, my);
        renderCanvas(g, mx, my, ctx);
        AxiomShaderManager.applyPostChain();
        g.flush();
        renderTreeCard(g, mx, my);
        g.flush();
        renderBottomBar(g);
        super.render(g, mx, my, pt);

        g.pose().popPose();
    }

    private void preloadNextShader() {
        for (ResearchTree tree : trees) {
            String disc = tree.isDisciplineTree() ? tree.discipline : null;
            ResourceLocation location = DisciplineRendererRegistry.get(disc).shaderLocation();
            if (location == null || preloaded.contains(location)) continue;
            preloaded.add(location);
            AxiomShaderManager.preload(location);
            return;
        }
    }

    private void syncRenderer() {
        if (trees.isEmpty()) return;
        ResearchTree tree = trees.get(activeTreeIdx);
        String discId = tree.isDisciplineTree() ? tree.discipline : null;
        if (!java.util.Objects.equals(discId, lastDisciplineId)) {
            lastDisciplineId = discId;
            activeRenderer = DisciplineRendererRegistry.get(discId);
            activeRenderer.onActivate(clock);
            AxiomShaderManager.activate(activeRenderer.shaderLocation());
        }
    }

    private RenderContext buildContext(int mx, int my) {
        if (trees.isEmpty()) return null;
        ResearchTree tree = trees.get(activeTreeIdx);
        Set<ResourceLocation> unlocked = ClientResearchCache.unlocked;
        Set<ResourceLocation> lockedOut = ClientResearchCache.lockedOut;
        int ct = canvasTop();
        float cmx = (float) ((mx - canvasLeft() - panX) / zoom);
        float cmy = (float) ((my - ct - panY) / zoom);
        return new RenderContext(tree, unlocked, lockedOut, selected,
                cmx, cmy, canvasW(), canvasH(), clock, intensity);
    }

    private record SideRow(String header, int treeIdx, ResearchNode pinNode) {

        SideRow(String header, int treeIdx) {
            this(header, treeIdx, null);
        }
    }

    private List<SideRow> sideRows() {
        List<String> order = new ArrayList<>(List.of("Foundations", "Discipline", "Dispensations", "Chronicle", "Space"));
        for (ResearchTree t : trees) if (!order.contains(t.group)) order.add(t.group);
        List<SideRow> rows = new ArrayList<>();
        List<SideRow> pinned = new ArrayList<>();
        for (int i = 0; i < trees.size(); i++) {
            for (ResearchNode n : trees.get(i).getNodes()) {
                if (!ConfluxPins.isPinned(n.id)) continue;
                if (n.hidden && !ClientResearchCache.isUnlocked(n.id)) continue;
                pinned.add(new SideRow(null, i, n));
            }
        }
        if (!pinned.isEmpty()) {
            rows.add(new SideRow("Pinned", -1));
            rows.addAll(pinned);
        }
        for (String group : order) {
            boolean any = false;
            for (int i = 0; i < trees.size(); i++) {
                if (!trees.get(i).group.equals(group)) continue;
                if (!any) {
                    rows.add(new SideRow(group, -1));
                    any = true;
                }
                rows.add(new SideRow(null, i));
            }
        }
        return rows;
    }

    /** {done, total, anyAvailable} for a tree. */
    private int[] progress(ResearchTree tree) {
        int done = 0, total = 0, avail = 0;
        for (ResearchNode n : tree.getNodes()) {
            total++;
            if (ClientResearchCache.isUnlocked(n.id)) done++;
            else if (!ClientResearchCache.isLockedOut(n.id) && n.isVisible(ClientResearchCache.unlocked) &&
                    n.canUnlock(ClientResearchCache.unlocked, ClientResearchCache.lockedOut)) avail = 1;
        }
        return new int[] { done, total, avail };
    }

    private int sideListTop() {
        return 72;
    }

    private boolean resultsMode() {
        return readyOnly || (searchBox != null && !searchBox.getValue().isBlank());
    }

    private record Hit(int treeIdx, ResearchNode node, boolean affordable, boolean done) {}

    private List<Hit> hits() {
        String q = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase();
        Set<ResourceLocation> unlockedSet = ClientResearchCache.unlocked;
        List<Hit> out = new ArrayList<>();
        for (int i = 0; i < trees.size(); i++) {
            ResearchTree tree = trees.get(i);
            for (ResearchNode n : tree.getNodes()) {
                boolean done = ClientResearchCache.isUnlocked(n.id);
                if (n.hidden && !done) continue;
                if (!n.isVisible(unlockedSet)) continue;
                if (readyOnly && (done || ClientResearchCache.isLockedOut(n.id) ||
                        !n.canUnlock(unlockedSet, ClientResearchCache.lockedOut))) continue;
                if (!q.isEmpty() && !(n.title + " " + n.lore + " " + tree.title).toLowerCase().contains(q)) continue;
                out.add(new Hit(i, n, costsAffordable(n), done));
            }
        }
        out.sort((a, b) -> Boolean.compare(b.affordable(), a.affordable()));
        return out.size() > 150 ? out.subList(0, 150) : out;
    }

    private void focusHit(Hit hit) {
        activeTreeIdx = hit.treeIdx();
        selected = hit.node();
        pendingFocus = hit.node();
    }

    private int[] chipBounds(int which) {
        int w = (SIDEBAR_W - 22) / 2;
        int x = 8 + which * (w + 6);
        return new int[] { x, 55, x + w, 67 };
    }

    private int sideListBottom() {
        return bottomTop() - 4;
    }

    private void renderToggle(GuiGraphics g, int mx, int my) {
        int[] tb = toggleBounds();
        boolean hov = mx >= tb[0] && mx < tb[2] && my >= tb[1] && my < tb[3];
        g.fill(tb[0], tb[1], tb[2], tb[3], hov ? 0x33FFFFFF : 0x14FFFFFF);
        g.drawCenteredString(font, sidebarOpen ? "\u25c0" : "\u25b6", (tb[0] + tb[2]) / 2, tb[1] + 3, hov ? C_TEXT : accent());
    }

    private void renderSidebar(GuiGraphics g, int mx, int my) {
        int h = bottomTop();
        if (searchBox != null) searchBox.visible = sidebarOpen;
        if (!sidebarOpen) {
            g.fill(0, 0, RAIL_W, h, C_PANEL);
            g.fill(RAIL_W - 1, 0, RAIL_W, h, C_BORDER);
            g.fillGradient(0, 0, RAIL_W - 1, 34, (accent() & 0xFFFFFF) | 0x40000000, 0x00000000);
            renderToggle(g, mx, my);
            return;
        }
        g.fill(0, 0, SIDEBAR_W, h, C_PANEL);
        g.fill(SIDEBAR_W - 1, 0, SIDEBAR_W, h, C_BORDER);

        g.fillGradient(0, 0, SIDEBAR_W - 1, 34, (accent() & 0xFFFFFF) | 0x40000000, 0x00000000);
        g.drawString(font, "\u00a7lAXIOM", 10, 7, C_TEXT, false);
        String disc = ClientResearchCache.getDisciplineInfo().disciplineTitle();
        g.drawString(font, disc == null ? "\u00a78No discipline chosen" : "\u00a77" + disc, 10, 19, C_DIM, false);
        g.fill(8, 33, SIDEBAR_W - 8, 34, C_BORDER);
        renderToggle(g, mx, my);

        for (int c = 0; c < 2; c++) {
            int[] cb = chipBounds(c);
            boolean on = (c == 1) == readyOnly;
            boolean hv = mx >= cb[0] && mx < cb[2] && my >= cb[1] && my < cb[3];
            g.fill(cb[0], cb[1], cb[2], cb[3], on ? (accent() & 0xFFFFFF) | 0x55000000 : hv ? 0xFF14142A : 0xFF0C0C18);
            g.renderOutline(cb[0], cb[1], cb[2] - cb[0], cb[3] - cb[1], on ? accent() : C_BORDER);
            g.drawCenteredString(font, c == 0 ? "All" : "\u00a7a\u25cf\u00a7r Ready", (cb[0] + cb[2]) / 2, cb[1] + 2,
                    on ? C_TEXT : C_DIM);
        }

        int top = sideListTop(), bot = sideListBottom();
        g.enableScissor(0, top, SIDEBAR_W - 1, bot);
        int y = top - sidebarScroll;
        int start = y;
        if (resultsMode()) {
            List<Hit> hits = hits();
            if (hits.isEmpty()) g.drawString(font, "\u00a78No matches.", 12, y + 6, C_DIM, false);
            for (Hit hit : hits) {
                boolean hover = mx >= 0 && mx < SIDEBAR_W - 1 && my >= Math.max(top, y) && my < Math.min(bot, y + ROW_H);
                boolean current = selected != null && selected.id.equals(hit.node().id);
                if (current) g.fill(4, y, SIDEBAR_W - 5, y + ROW_H - 2, 0xFF16162C);
                else if (hover) g.fill(4, y, SIDEBAR_W - 5, y + ROW_H - 2, 0xFF11111F);
                int col = hit.done() ? 0xFF88BBFF : hit.affordable() ? 0xFF88EE88 : C_TEXT;
                g.drawString(font, font.plainSubstrByWidth(hit.node().title, SIDEBAR_W - 26), 12, y + 4, col, false);
                g.drawString(font, "\u00a78" + font.plainSubstrByWidth(trees.get(hit.treeIdx()).title,
                        SIDEBAR_W - 26), 12, y + 15, C_DIM, false);
                y += ROW_H;
            }
            sidebarMax = Math.max(0, (y - start) - (bot - top));
            sidebarScroll = Math.min(sidebarScroll, sidebarMax);
            g.disableScissor();
            return;
        }
        for (SideRow row : sideRows()) {
            if (row.header() != null) {
                g.drawString(font, "\u00a78" + row.header().toUpperCase(), 10, y + 5, C_DIM, false);
                y += GROUP_H;
                continue;
            }
            if (row.pinNode() != null) {
                boolean pinHover = mx >= 0 && mx < SIDEBAR_W - 1 && my >= Math.max(top, y) && my < Math.min(bot, y + ROW_H);
                boolean current = selected != null && selected.id.equals(row.pinNode().id);
                if (current) g.fill(4, y, SIDEBAR_W - 5, y + ROW_H - 2, 0xFF16162C);
                else if (pinHover) g.fill(4, y, SIDEBAR_W - 5, y + ROW_H - 2, 0xFF11111F);
                boolean done = ClientResearchCache.isUnlocked(row.pinNode().id);
                g.drawString(font, "\u00a7e\u2605", 10, y + 4, TEXT_PIN, false);
                g.drawString(font, font.plainSubstrByWidth(row.pinNode().title, SIDEBAR_W - 38), 22, y + 4,
                        done ? 0xFF88BBFF : C_TEXT, false);
                g.drawString(font, "\u00a78" + font.plainSubstrByWidth(trees.get(row.treeIdx()).title, SIDEBAR_W - 38), 22,
                        y + 15, C_DIM, false);
                y += ROW_H;
                continue;
            }
            ResearchTree tree = trees.get(row.treeIdx());
            boolean active = row.treeIdx() == activeTreeIdx;
            boolean hover = mx >= 0 && mx < SIDEBAR_W - 1 && my >= Math.max(top, y) && my < Math.min(bot, y + ROW_H) &&
                    my >= y && my < y + ROW_H;
            if (active) {
                g.fill(4, y, SIDEBAR_W - 5, y + ROW_H - 2, 0xFF16162C);
                g.fill(4, y, 6, y + ROW_H - 2, accent());
            } else if (hover) {
                g.fill(4, y, SIDEBAR_W - 5, y + ROW_H - 2, 0xFF11111F);
            }
            int[] p = progress(tree);
            String title = font.plainSubstrByWidth(tree.title, SIDEBAR_W - 38);
            g.drawString(font, title, 12, y + 4, active ? C_TEXT : hover ? 0xFF9999BB : 0xFF8888AA, false);
            if (p[2] == 1) g.drawString(font, "\u00a7a\u25cf", SIDEBAR_W - 16, y + 4, C_TEXT, false);

            int barX = 12, barW = SIDEBAR_W - 56, barY = y + 17;
            g.fill(barX, barY, barX + barW, barY + 3, 0xFF111122);
            int filled = p[1] == 0 ? 0 : (int) (barW * (p[0] / (float) p[1]));
            g.fill(barX, barY, barX + filled, barY + 3, p[0] == p[1] && p[1] > 0 ? 0xFF55CC88 : accent());
            String count = p[0] + "/" + p[1];
            g.drawString(font, "\u00a78" + count, SIDEBAR_W - 8 - font.width(count), y + 13, C_DIM, false);
            y += ROW_H;
        }
        sidebarMax = Math.max(0, (y - start) - (bot - top));
        sidebarScroll = Math.min(sidebarScroll, sidebarMax);
        g.disableScissor();
    }

    private Hit hitRowAt(double mx, double my) {
        if (mx < 0 || mx >= SIDEBAR_W - 1 || my < sideListTop() || my >= sideListBottom()) return null;
        int y = sideListTop() - sidebarScroll;
        for (Hit hit : hits()) {
            if (my >= y && my < y + ROW_H) return hit;
            y += ROW_H;
        }
        return null;
    }

    private SideRow rowAt(double mx, double my) {
        if (mx < 0 || mx >= SIDEBAR_W - 1 || my < sideListTop() || my >= sideListBottom()) return null;
        int y = sideListTop() - sidebarScroll;
        for (SideRow row : sideRows()) {
            if (row.header() != null) {
                y += GROUP_H;
                continue;
            }
            if (my >= y && my < y + ROW_H) return row;
            y += ROW_H;
        }
        return null;
    }

    private void renderTopBar(GuiGraphics g, int mx, int my) {
        int cl = canvasLeft(), cw = canvasW();
        g.fill(cl, 0, cl + cw, TAB_H, C_HEADER);
        g.fill(cl, TAB_H - 1, cl + cw, TAB_H, (accent() & 0xFFFFFF) | 0x88000000);

        int[] ab = researchAllBounds();
        if (!trees.isEmpty()) {
            ResearchTree tree = trees.get(activeTreeIdx);
            int avail = ab[0] - (cl + 10) - 8;
            g.drawString(font, font.plainSubstrByWidth(tree.title, avail), cl + 10, 3, C_TEXT, false);
            String desc = tree.description.replaceAll("[*_`#>]", "").replace('\n', ' ');
            g.drawString(font, font.plainSubstrByWidth(desc, avail), cl + 10, 13, C_DIM, false);
        }

        boolean allHover = terminal != null && mx >= ab[0] && mx < ab[2] && my >= ab[1] && my < ab[3];
        g.fill(ab[0], ab[1], ab[2], ab[3], terminal == null ? C_HEADER : allHover ? 0xFF1A3A1A : 0xFF112211);
        g.renderOutline(ab[0], ab[1], ab[2] - ab[0], ab[3] - ab[1], terminal == null ? C_BORDER : 0xFF338833);
        g.drawCenteredString(font, "Research All", (ab[0] + ab[2]) / 2, ab[1] + (ab[3] - ab[1] - 8) / 2,
                terminal == null ? C_DIM : 0xFF88EE88);

        int[] wb = wikiButtonBounds();
        boolean wikiHover = mx >= wb[0] && mx < wb[2] && my >= wb[1] && my < wb[3];
        g.fill(wb[0], wb[1], wb[2], wb[3], wikiHover ? 0xFF2A2A4A : C_HEADER);
        g.drawCenteredString(font, "?", (wb[0] + wb[2]) / 2, wb[1] + (wb[3] - wb[1] - 8) / 2,
                wikiHover ? C_TEXT : C_DIM);
    }

    private int[] researchAllBounds() {
        int w = font.width("Research All") + 12;
        int right = canvasLeft() + canvasW() - 22;
        return new int[] { right - w, 3, right, TAB_H - 3 };
    }

    private int[] wikiButtonBounds() {
        int right = canvasLeft() + canvasW();
        return new int[] { right - 18, 3, right - 2, TAB_H - 3 };
    }

    private void renderCanvas(GuiGraphics g, int mx, int my, RenderContext ctx) {
        int cw = canvasW(), ch = canvasH(), ct = canvasTop(), cl = canvasLeft();
        g.pose().pushPose();
        g.pose().translate(cl, 0, 0);
        g.fill(0, ct, cw, ct + ch, C_CANVAS);
        g.fill(cw - 1, ct, cw, ct + ch, C_BORDER);

        if (trees.isEmpty()) {
            g.drawCenteredString(font, "\u00a77No research trees loaded.", cw / 2, ct + ch / 2 - 4, C_TEXT);
            g.pose().popPose();
            return;
        }

        double guiScale = minecraft.getWindow().getGuiScale();
        double realCt = ct * uiScale, realCh = ch * uiScale, realCw = cw * uiScale, realCl = cl * uiScale;
        RenderSystem.enableScissor(
                (int) (realCl * guiScale),
                (int) ((height - realCt - realCh) * guiScale),
                (int) (realCw * guiScale),
                (int) (realCh * guiScale));

        g.pose().pushPose();
        g.pose().translate(0, ct, 0);
        activeRenderer.renderBackground(g, ctx);
        g.pose().popPose();

        g.pose().pushPose();
        g.pose().translate(panX, ct + panY, 0);
        g.pose().scale(zoom, zoom, 1);

        // zoomed far out, one-pixel strokes and 16px icons shimmer as they are resampled, so the tree switches to a
        // plain overview with strokes that never get thinner than a device pixel
        float deviceScale = zoom * uiScale * (float) guiScale;
        boolean overview = deviceScale < 0.8f;
        if (overview) {
            renderOverview(g, ctx, deviceScale);
        } else {
            activeRenderer.renderEdges(g, ctx);
            activeRenderer.renderNodes(g, ctx);

            for (ResearchNode node : ctx.tree().getNodes()) {
                if (!node.isVisible(ctx.unlocked())) continue;
                float[] pos = activeRenderer.nodePos(node, ctx);
                drawNodeLabel(g, node, (int) pos[0], (int) pos[1], ctx);
                if (ConfluxPins.isPinned(node.id)) {
                    g.drawCenteredString(font, "\u00a7e\u2605", (int) pos[0], (int) pos[1] - NODE_R - 12, TEXT_PIN);
                }
            }
        }

        g.pose().popPose();

        if (!overview) {
            g.pose().pushPose();
            g.pose().translate(panX, ct + panY, 0);
            g.pose().scale(zoom, zoom, 1);
            activeRenderer.renderForeground(g, ctx);
            g.pose().popPose();
        }

        RenderSystem.disableScissor();
        g.pose().popPose();
    }

    private static final int CARD_W = 168;
    private boolean cardOpen = true;
    private float cardScroll = 0;
    private int cardMax = 0;
    private int[] cardRect = new int[4];
    private String cardSource = null;
    private List<RichBlock> cardBlocks = List.of();

    /** Overview of the active tree floating over the canvas corner: description, progress, what is ready. */
    private void renderTreeCard(GuiGraphics g, int lx, int my) {
        cardRect = new int[4];
        if (trees.isEmpty()) return;
        ResearchTree tree = trees.get(activeTreeIdx);
        int[] p = progress(tree);
        // absolute coordinates: the rich text renderer clips against them and misjudges a translated pose
        int x = canvasLeft() + 8, y = canvasTop() + 8;
        int acc = accent();
        boolean headerHover = lx >= x && lx < x + CARD_W && my >= y && my < y + 18;

        if (!cardOpen) {
            g.fill(x, y, x + CARD_W, y + 18, 0xD00B0B18);
            g.renderOutline(x, y, CARD_W, 18, headerHover ? acc : C_BORDER);
            g.drawString(font, "\u00a77\u25b8 " + font.plainSubstrByWidth(tree.title, CARD_W - 70), x + 6, y + 5, C_TEXT, false);
            String c = p[0] + "/" + p[1];
            g.drawString(font, "\u00a78" + c, x + CARD_W - 6 - font.width(c), y + 5, C_DIM, false);
            cardRect = new int[] { x, y, x + CARD_W, y + 18 };
            return;
        }

        int bodyMax = 44;
        List<RichBlock> blocks = cardBlocksFor(tree.description);
        int contentH = WikiRichTextRenderer.measureBlocksHeight(font, blocks, CARD_W - 20, new java.util.HashSet<>());
        int bodyH = Math.min(bodyMax, contentH);
        cardMax = Math.max(0, contentH - bodyH);
        cardScroll = Math.min(cardScroll, cardMax);
        int total = 18 + 8 + bodyH + 8 + 22;
        g.fill(x, y, x + CARD_W, y + total, 0xD00B0B18);
        g.renderOutline(x, y, CARD_W, total, (acc & 0xFFFFFF) | 0xAA000000);
        g.fillGradient(x + 1, y + 1, x + CARD_W - 1, y + 18, (acc & 0xFFFFFF) | 0x40000000, 0x00000000);
        g.drawString(font, "\u00a7f\u25be " + font.plainSubstrByWidth(tree.title, CARD_W - 20), x + 6, y + 5, C_TEXT, false);

        int by = y + 22;
        g.enableScissor(x + 1, by, x + CARD_W - 1, by + bodyH);
        WikiRichTextRenderer.renderBlocks(g, font, blocks, x + 10, by, CARD_W - 20, (int) cardScroll, by, by + bodyH, acc,
                new java.util.HashSet<>());
        g.disableScissor();
        if (cardMax > 0) {
            int th = Math.max(10, bodyH * bodyH / (bodyH + cardMax));
            int ty = by + (int) ((bodyH - th) * (cardScroll / cardMax));
            g.fill(x + CARD_W - 4, by, x + CARD_W - 2, by + bodyH, 0xFF111122);
            g.fill(x + CARD_W - 4, ty, x + CARD_W - 2, ty + th, acc);
        }

        int py = by + bodyH + 6;
        g.fill(x + 10, py, x + CARD_W - 10, py + 4, 0xFF111122);
        int filled = p[1] == 0 ? 0 : (int) ((CARD_W - 20) * (p[0] / (float) p[1]));
        g.fill(x + 10, py, x + 10 + filled, py + 4, p[0] == p[1] && p[1] > 0 ? 0xFF55CC88 : acc);
        String line = p[0] + " of " + p[1] + " researched" + (p[2] == 1 ? "  \u00a7a\u25cf ready" : "");
        g.drawString(font, "\u00a77" + line, x + 10, py + 8, C_TEXT, false);
        cardRect = new int[] { x, y, x + CARD_W, y + total };
    }

    private List<RichBlock> cardBlocksFor(String text) {
        if (!text.equals(cardSource)) {
            cardSource = text;
            cardBlocks = text.isEmpty() ? List.of() : WikiMarkdownParser.parse(text, false, true);
        }
        return cardBlocks;
    }

    private void renderOverview(GuiGraphics g, RenderContext ctx, float deviceScale) {
        int line = Math.max(2, (int) Math.ceil(1.6f / deviceScale));
        int half = Math.max(NODE_R / 2, (int) Math.ceil(3.0f / deviceScale));

        for (ResearchNode node : ctx.tree().getNodes()) {
            if (!node.isVisible(ctx.unlocked())) continue;
            float[] dst = activeRenderer.nodePos(node, ctx);
            for (ResourceLocation prereqId : node.prerequisites) {
                var prereq = ctx.tree().getNode(prereqId).orElse(null);
                if (prereq == null || !prereq.isVisible(ctx.unlocked())) continue;
                float[] src = activeRenderer.nodePos(prereq, ctx);
                int lineColor = ctx.isUnlocked(prereqId) ? 0xFF4A6FA8 : 0xFF2E3550;
                int x1 = Math.round(src[0]), y1 = Math.round(src[1]);
                int x2 = Math.round(dst[0]), y2 = Math.round(dst[1]);
                int midY = (y1 + y2) / 2;
                int h = line / 2;
                g.fill(x1 - h, Math.min(y1, midY), x1 - h + line, Math.max(y1, midY) + line, lineColor);
                g.fill(Math.min(x1, x2), midY - h, Math.max(x1, x2) + line, midY - h + line, lineColor);
                g.fill(x2 - h, Math.min(midY, y2), x2 - h + line, Math.max(midY, y2) + line, lineColor);
            }
        }

        for (ResearchNode node : ctx.tree().getNodes()) {
            if (!node.isVisible(ctx.unlocked())) continue;
            float[] pos = activeRenderer.nodePos(node, ctx);
            boolean unlocked = ctx.isUnlocked(node.id);
            boolean lockedOut = ctx.isLockedOut(node.id);
            boolean mystery = node.hidden && !unlocked;
            int color = lockedOut ? 0xFF8A3030 : mystery ? 0xFF555566 : unlocked ? 0xFF5A9AE0 :
                    ctx.isAvailable(node) ? 0xFF66CC66 : 0xFF3A4560;
            if (ctx.isSelected(node)) color = 0xFFFFFFFF;
            int cx = Math.round(pos[0]), cy = Math.round(pos[1]);
            g.fill(cx - half, cy - half, cx + half, cy + half, color);
        }
    }

    private void drawNodeLabel(GuiGraphics g, ResearchNode node, int cx, int cy, RenderContext ctx) {
        boolean unlocked = ctx.isUnlocked(node.id);
        boolean lockedOut = ctx.isLockedOut(node.id);
        boolean available = ctx.isAvailable(node);
        boolean isMystery = node.hidden && !unlocked;

        int r = NODE_R;
        String rawLabel = isMystery ? "§8???" : lockedOut ? "§c" + node.title : node.title;
        String stripped = rawLabel.replaceAll("§.", "");
        String label = stripped.length() > 16 ? rawLabel.substring(0, rawLabel.startsWith("§") ? 17 : 15) + "…" :
                rawLabel;
        int lw = font.width(label);

        g.drawString(font, label, cx - lw / 2 + 1, cy + r + 4, 0xFF000000, false);
        g.drawString(font, label, cx - lw / 2, cy + r + 3,
                unlocked ? C_TEXT : available ? 0xFF88CC88 : C_DIM, false);
    }

    private void renderBottomBar(GuiGraphics g) {
        int y0 = bottomTop();
        g.fill(0, y0, vw, vh, C_HEADER);
        g.fill(0, y0, vw, y0 + 1, C_BORDER);

        List<ConfluxDataType> available = new ArrayList<>();
        for (ConfluxDataType t : ConfluxDataType.values())
            if (t.isAvailable()) available.add(t);
        if (available.isEmpty()) return;

        int padding = 10;
        int totalW = vw - padding * 2;
        int slotW = totalW / available.size();
        int barH = 6;
        int x = padding;
        int textY = y0 + 6;
        int barY = textY + 11;

        for (ConfluxDataType type : available) {
            long stored = terminal != null ? ClientResearchCache.dataStored(type) : 0L;
            long capacity = terminal != null ? terminal.getCapacity(type) : 1_000_000L;
            int col = typeBarColor(type);

            g.drawString(font, type.displayComponent(), x, textY, C_TEXT, false);
            String amtStr = formatAmount(stored);
            int amtW = font.width(amtStr);
            g.drawString(font, "§7" + amtStr, x + slotW - amtW - 2, textY, C_TEXT, false);

            int barW = slotW - 4;
            int filled = capacity > 0 ? (int) (barW * stored / (float) capacity) : 0;
            g.fill(x, barY, x + barW, barY + barH, 0xFF111122);
            if (filled > 0) g.fill(x, barY, x + filled, barY + barH, col);
            g.renderOutline(x, barY, barW, barH, C_BORDER);

            x += slotW;
        }
    }

    @Override
    public boolean mouseClicked(double rmx, double rmy, int btn) {
        double mx = rmx / uiScale;
        double my = rmy / uiScale;
        if (super.mouseClicked(mx, my, btn)) return true;

        int cw = canvasW(), ct = canvasTop(), ch = canvasH(), cl = canvasLeft();

        if (my < TAB_H && mx >= cl && mx < cl + cw) {
            int[] wb = wikiButtonBounds();
            if (mx >= wb[0] && mx < wb[2] && my >= wb[1] && my < wb[3]) {
                Minecraft.getInstance().setScreen(new ConfluxWikiScreen(this));
                return true;
            }

            int[] ab = researchAllBounds();
            if (terminal != null && !trees.isEmpty() && mx >= ab[0] && mx < ab[2] && my >= ab[1] && my < ab[3]) {
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                        new net.phoenix.core.integration.conflux.network.C2SResearchAllPacket(
                                trees.get(activeTreeIdx).id, terminal.getBlockPos()));
                return true;
            }
            return false;
        }

        int[] tb = toggleBounds();
        if (mx >= tb[0] && mx < tb[2] && my >= tb[1] && my < tb[3]) {
            toggleSidebar();
            return true;
        }
        if (mx < sideW()) {
            if (!sidebarOpen) return true;
            for (int c = 0; c < 2; c++) {
                int[] cb = chipBounds(c);
                if (mx >= cb[0] && mx < cb[2] && my >= cb[1] && my < cb[3]) {
                    readyOnly = c == 1;
                    sidebarScroll = 0;
                    return true;
                }
            }
            if (resultsMode()) {
                Hit hit = hitRowAt(mx, my);
                if (hit != null) focusHit(hit);
                return true;
            }
            SideRow row = rowAt(mx, my);
            if (row == null) return true;
            if (row.pinNode() != null) {
                focusHit(new Hit(row.treeIdx(), row.pinNode(), false, false));
            } else if (row.treeIdx() != activeTreeIdx) {
                activeTreeIdx = row.treeIdx();
                cardScroll = 0;
                selected = null;
                resetPan();
            }
            return true;
        }

        if (cardRect[2] > 0 && mx >= cardRect[0] && mx < cardRect[2] && my >= cardRect[1] && my < cardRect[3]) {
            if (my < cardRect[1] + 18) cardOpen = !cardOpen;
            return true;
        }
        if (mx >= cl && mx < cl + cw && my >= ct && my < ct + ch && !trees.isEmpty()) {
            ResearchTree tree = trees.get(activeTreeIdx);
            Set<ResourceLocation> unlockedSet = ClientResearchCache.unlocked;
            float cx = (float) ((mx - cl - panX) / zoom);
            float cy = (float) ((my - ct - panY) / zoom);
            RenderContext ctx = buildContext((int) mx, (int) my);
            ResearchNode hit = null;
            if (ctx != null) {
                for (ResearchNode node : tree.getNodes()) {
                    if (!node.isVisible(unlockedSet)) continue;
                    if (activeRenderer.hitsNode(node, cx, cy, ctx)) {
                        hit = node;
                        break;
                    }
                }
            }
            if (hit != null) {
                selected = hit;
                Minecraft.getInstance().setScreen(new ResearchNodeScreen(this, tree, hit, terminal));
            } else {
                selected = null;
                if (btn == 0) {
                    dragging = true;
                    lastDragX = mx;
                    lastDragY = my;
                }
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double rmx, double rmy, int btn) {
        if (btn == 0) dragging = false;
        return super.mouseReleased(rmx / uiScale, rmy / uiScale, btn);
    }

    @Override
    public boolean mouseDragged(double rmx, double rmy, int btn, double dragX, double dragY) {
        double mx = rmx / uiScale;
        double my = rmy / uiScale;
        if (dragging && btn == 0) {
            panX += (float) (mx - lastDragX);
            panY += (float) (my - lastDragY);
            lastDragX = mx;
            lastDragY = my;
            return true;
        }
        return super.mouseDragged(mx, my, btn, dragX / uiScale, dragY / uiScale);
    }

    @Override
    public boolean mouseScrolled(double rmx, double rmy, double scrollX, double delta) {
        double mx = rmx / uiScale;
        double my = rmy / uiScale;
        if (mx < sideW()) {
            if (!sidebarOpen) return true;
            sidebarScroll = (int) Math.max(0, Math.min(sidebarMax, sidebarScroll - delta * 24));
            return true;
        }
        if (cardOpen && cardMax > 0 && cardRect[2] > 0 && mx >= cardRect[0] &&
                mx < cardRect[2] && my >= cardRect[1] && my < cardRect[3]) {
            cardScroll = (float) Math.max(0, Math.min(cardMax, cardScroll - delta * 14));
            return true;
        }
        if (mx >= canvasLeft() && mx < canvasLeft() + canvasW() && my >= canvasTop()) {
            float oldZoom = zoom;
            zoom = Math.max(MIN_ZOOM, Math.min(2.5f, zoom * (float) Math.pow(1.15, delta)));
            float factor = zoom / oldZoom;
            double lx = mx - canvasLeft();
            panX = (float) (lx - factor * (lx - panX));
            panY = (float) (my - canvasTop() - factor * (my - canvasTop() - panY));
            return true;
        }
        return super.mouseScrolled(mx, my, 0.0, delta);
    }

    @Override
    public boolean keyPressed(int kc, int sc, int mod) {
        if (searchBox != null && searchBox.isFocused()) {
            if (kc == GLFW.GLFW_KEY_ESCAPE) {
                if (!searchBox.getValue().isEmpty()) searchBox.setValue("");
                else searchBox.setFocused(false);
                return true;
            }
            searchBox.keyPressed(kc, sc, mod);
            return true;
        }
        if (searchBox != null && (kc == GLFW.GLFW_KEY_SLASH || (kc == GLFW.GLFW_KEY_F && (mod & GLFW.GLFW_MOD_CONTROL) != 0))) {
            setFocused(searchBox);
            searchBox.setFocused(true);
            skipChar = kc == GLFW.GLFW_KEY_SLASH;
            return true;
        }
        if (kc == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (kc == GLFW.GLFW_KEY_TAB) {
            toggleSidebar();
            return true;
        }
        float step = 30f / zoom;
        if (kc == GLFW.GLFW_KEY_W || kc == GLFW.GLFW_KEY_UP) {
            panY += step;
            return true;
        }
        if (kc == GLFW.GLFW_KEY_S || kc == GLFW.GLFW_KEY_DOWN) {
            panY -= step;
            return true;
        }
        if (kc == GLFW.GLFW_KEY_A || kc == GLFW.GLFW_KEY_LEFT) {
            panX += step;
            return true;
        }
        if (kc == GLFW.GLFW_KEY_D || kc == GLFW.GLFW_KEY_RIGHT) {
            panX -= step;
            return true;
        }
        return super.keyPressed(kc, sc, mod);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (skipChar) {
            skipChar = false;
            return true;
        }
        return super.charTyped(c, mods);
    }

    private boolean costsAffordable(ResearchNode node) {
        if (terminal == null) return false;
        return node.cost.entrySet().stream()
                .allMatch(e -> ClientResearchCache.dataStored(e.getKey()) >= e.getValue());
    }

    public boolean hasTerminal() {
        return terminal != null;
    }

    private void pushShaderData(int rmx, int rmy) {
        if (!AxiomShaderManager.isActive() || trees.isEmpty()) return;
        ResearchTree tree = trees.get(activeTreeIdx);

        float sw = (float) width;
        float sh = (float) height;
        int ct = canvasTop();

        int mx = Math.round(rmx / uiScale);
        int my = Math.round(rmy / uiScale);

        RenderContext sCtx = buildContext(mx, my);
        if (sCtx == null) return;
        java.util.List<float[]> nodePosScreen = new java.util.ArrayList<>();
        java.util.List<Float> heatStrList = new java.util.ArrayList<>();
        float[] heatStrAll = activeRenderer.nodeHeatStrength(sCtx);
        int nodeIdx = 0;
        for (ResearchNode node : tree.getNodes()) {
            if (!node.isVisible(ClientResearchCache.unlocked)) continue;
            if (nodePosScreen.size() >= 8) break;
            float[] cp = activeRenderer.nodePos(node, sCtx);

            float sx = (canvasLeft() + panX + cp[0] * zoom) * uiScale;
            float sy = (ct + panY + cp[1] * zoom) * uiScale;
            nodePosScreen.add(new float[] { sx, sy });
            float str = (heatStrAll != null && nodeIdx < heatStrAll.length) ? heatStrAll[nodeIdx] : 1f;
            heatStrList.add(str);
            nodeIdx++;
        }

        int nc = nodePosScreen.size();
        float[] flatNodes = new float[16];
        float[] flatStr = new float[8];
        for (int i = 0; i < nc; i++) {
            flatNodes[i * 2] = nodePosScreen.get(i)[0];
            flatNodes[i * 2 + 1] = nodePosScreen.get(i)[1];
            flatStr[i] = heatStrList.get(i);
        }

        float[] ripOrig = new float[8];
        float[] ripAge = new float[0];
        int ripCount = activeRenderer.rippleCount();
        if (ripCount > 0) {
            float[] origCanvas = activeRenderer.rippleOriginsCanvas();
            ripAge = activeRenderer.rippleAges();
            for (int i = 0; i < Math.min(ripCount, 4); i++) {
                ripOrig[i * 2] = (canvasLeft() + panX + origCanvas[i * 2] * zoom) * uiScale;
                ripOrig[i * 2 + 1] = (ct + panY + origCanvas[i * 2 + 1] * zoom) * uiScale;
            }
        }

        AxiomShaderManager.pushFrameData(
                sw, sh, (float) rmx, (float) rmy,
                flatNodes, nc, flatStr,
                ripOrig, ripAge, ripCount);
    }

    private int typeBarColor(ConfluxDataType type) {
        return switch (type) {
            case MATERIAL -> 0xFFCC8800;
            case BIOLOGICAL -> 0xFF22AA44;
            case ENERGETIC -> 0xFF2299CC;
            case COMPUTATIONAL -> 0xFF9933CC;
            case ARCANE -> 0xFF660099;
        };
    }

    private String formatAmount(long v) {
        if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000.0);
        if (v >= 1_000) return String.format("%.1fk", v / 1_000.0);
        return String.valueOf(v);
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String stripped = text.replaceAll("§.", "");
        String[] words = stripped.split(" ");
        String[] colored = text.split(" ");
        StringBuilder cur = new StringBuilder();
        int wi = 0;
        for (String word : words) {
            String coloredWord = wi < colored.length ? colored[wi] : word;
            if (font.width(cur + coloredWord) > maxWidth && !cur.isEmpty()) {
                lines.add(cur.toString().trim());
                cur.setLength(0);
            }
            cur.append(coloredWord).append(" ");
            wi++;
        }
        if (!cur.isEmpty()) lines.add(cur.toString().trim());
        return lines;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
