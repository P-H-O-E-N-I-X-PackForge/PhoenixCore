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
    private static final int DETAIL_W = 250;
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
    private static final int GROUP_H = 16;
    private int sidebarScroll = 0;
    private int sidebarMax = 0;
    private float detailScroll = 0;
    private int detailMax = 0;
    private List<RichSpan.Region> richRegions = List.of();
    private String mdSource = null;
    private List<RichBlock> mdBlocks = List.of();
    private final Set<String> mdExpanded = new java.util.HashSet<>();
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
        float neededW = SIDEBAR_W + DETAIL_W + MIN_CANVAS_W;
        float neededH = TAB_H + BOTTOM_H + MIN_CANVAS_H;
        uiScale = (width < neededW || height < neededH) ?
                Math.min(width / neededW, height / neededH) : 1f;
        uiScale = Math.max(0.1f, uiScale);
        vw = Math.round(width / uiScale);
        vh = Math.round(height / uiScale);

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
        return vw - SIDEBAR_W - DETAIL_W;
    }

    private int canvasLeft() {
        return SIDEBAR_W;
    }

    private int detailLeft() {
        return vw - DETAIL_W;
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
            renderDetailPanel(g, mx, my);
            renderBottomBar(g);
            super.render(g, mx, my, pt);
            g.pose().popPose();
            return;
        }

        activeRenderer.tick(dt, ctx);

        g.fill(0, 0, vw, vh, C_BG);
        renderSidebar(g, mx, my);
        renderTopBar(g, mx, my);
        renderCanvas(g, mx, my, ctx);
        AxiomShaderManager.applyPostChain();
        renderDetailPanel(g, mx, my);
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

    private record SideRow(String header, int treeIdx) {}

    private List<SideRow> sideRows() {
        List<String> order = new ArrayList<>(List.of("Foundations", "Discipline", "Dispensations", "Chronicle", "Space"));
        for (ResearchTree t : trees) if (!order.contains(t.group)) order.add(t.group);
        List<SideRow> rows = new ArrayList<>();
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
        return 40;
    }

    private int sideListBottom() {
        return bottomTop() - 4;
    }

    private void renderSidebar(GuiGraphics g, int mx, int my) {
        int h = bottomTop();
        g.fill(0, 0, SIDEBAR_W, h, C_PANEL);
        g.fill(SIDEBAR_W - 1, 0, SIDEBAR_W, h, C_BORDER);

        g.drawString(font, "\u00a7lAXIOM", 10, 7, C_TEXT, false);
        String disc = ClientResearchCache.getDisciplineInfo().disciplineTitle();
        g.drawString(font, disc == null ? "\u00a78No discipline chosen" : "\u00a77" + disc, 10, 19, C_DIM, false);
        g.fill(8, 33, SIDEBAR_W - 8, 34, C_BORDER);

        int top = sideListTop(), bot = sideListBottom();
        g.enableScissor(0, top, SIDEBAR_W - 1, bot);
        int y = top - sidebarScroll;
        int start = y;
        for (SideRow row : sideRows()) {
            if (row.header() != null) {
                g.drawString(font, "\u00a78" + row.header().toUpperCase(), 10, y + 5, C_DIM, false);
                y += GROUP_H;
                continue;
            }
            ResearchTree tree = trees.get(row.treeIdx());
            boolean active = row.treeIdx() == activeTreeIdx;
            boolean hover = mx >= 0 && mx < SIDEBAR_W - 1 && my >= Math.max(top, y) && my < Math.min(bot, y + ROW_H) &&
                    my >= y && my < y + ROW_H;
            if (active) {
                g.fill(4, y, SIDEBAR_W - 5, y + ROW_H - 2, 0xFF16162C);
                g.fill(4, y, 6, y + ROW_H - 2, C_ACCENT);
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
            g.fill(barX, barY, barX + filled, barY + 3, p[0] == p[1] && p[1] > 0 ? 0xFF55CC88 : C_ACCENT);
            String count = p[0] + "/" + p[1];
            g.drawString(font, "\u00a78" + count, SIDEBAR_W - 8 - font.width(count), y + 13, C_DIM, false);
            y += ROW_H;
        }
        sidebarMax = Math.max(0, (y - start) - (bot - top));
        sidebarScroll = Math.min(sidebarScroll, sidebarMax);
        g.disableScissor();
    }

    private int treeRowAt(double mx, double my) {
        if (mx < 0 || mx >= SIDEBAR_W - 1 || my < sideListTop() || my >= sideListBottom()) return -1;
        int y = sideListTop() - sidebarScroll;
        for (SideRow row : sideRows()) {
            if (row.header() != null) {
                y += GROUP_H;
                continue;
            }
            if (my >= y && my < y + ROW_H) return row.treeIdx();
            y += ROW_H;
        }
        return -1;
    }

    private void renderTopBar(GuiGraphics g, int mx, int my) {
        int cl = canvasLeft(), cw = canvasW();
        g.fill(cl, 0, cl + cw, TAB_H, C_HEADER);
        g.fill(cl, TAB_H - 1, cl + cw, TAB_H, C_BORDER);

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

    private List<RichBlock> markdown(String text) {
        if (!text.equals(mdSource)) {
            mdSource = text;
            mdBlocks = text.isEmpty() ? List.of() : WikiMarkdownParser.parse(text, false, true);
        }
        return mdBlocks;
    }

    private void drawRule(GuiGraphics g, int px, int y) {
        g.fill(px + 8, y, vw - 8, y + 1, C_BORDER);
    }

    private void renderDetailPanel(GuiGraphics g, int mx, int my) {
        int px = detailLeft();
        int pTop = 0;
        int pBot = bottomTop();
        int btnH = 18;
        int btnY = pBot - btnH - 10;
        int regionBot = btnY - 6;
        richRegions = List.of();

        g.fill(px, pTop, vw, pBot, C_PANEL);
        g.fill(px, pTop, px + 1, pBot, C_BORDER);

        if (selected == null) {
            renderTreeIntro(g, px, pTop, regionBot, mx, my);
            return;
        }

        Set<ResourceLocation> unlockedSet = ClientResearchCache.unlocked;
        boolean unlocked = ClientResearchCache.isUnlocked(selected.id);
        boolean lockedOut = ClientResearchCache.isLockedOut(selected.id);
        boolean isMystery = selected.hidden && !unlocked;
        boolean available = !unlocked && !lockedOut && selected.canUnlock(unlockedSet, ClientResearchCache.lockedOut);
        boolean canResearch = available && costsAffordable(selected);

        int x = px + 12;
        int maxW = DETAIL_W - 30;

        String titleStr = isMystery ? "???" : selected.title;
        int titleColor = unlocked ? 0xFF88BBFF : available ? 0xFF88EE88 : lockedOut ? 0xFF884444 : C_TEXT;
        g.drawString(font, font.plainSubstrByWidth(titleStr, DETAIL_W - 24 - (selected.isCommitmentNode ? 64 : 0)), x,
                pTop + 10, titleColor, false);
        if (selected.isCommitmentNode) {
            String badge = "\u00a76\u25c6 Commitment";
            g.drawString(font, badge, vw - font.width(badge) - 10, pTop + 10, C_TEXT, false);
        }
        String status = unlocked ? "\u00a79Researched" : lockedOut ? "\u00a74Sealed" : available ?
                (canResearch ? "\u00a7aReady to research" : "\u00a7eAvailable") : "\u00a78Locked";
        g.drawString(font, status, x, pTop + 21, C_TEXT, false);
        drawRule(g, px, pTop + 33);

        int regionTop = pTop + 38;
        ItemStack hoveredReward = ItemStack.EMPTY;
        g.enableScissor(px + 1, regionTop, vw, regionBot);
        int y = regionTop - (int) detailScroll;
        int startY = y;

        if (lockedOut) {
            for (String line : wrapText("\u00a78This path has been sealed by a prior choice.", maxW)) {
                g.drawString(font, line, x, y, C_TEXT, false);
                y += 11;
            }
            y += 4;
        } else if (isMystery) {
            if (!selected.hint.isEmpty()) {
                for (String line : wrapText("\u00a7o\u00a77" + selected.hint, maxW)) {
                    g.drawString(font, line, x, y, C_TEXT, false);
                    y += 11;
                }
                y += 4;
            }
        } else if (!selected.lore.isEmpty()) {
            List<RichBlock> blocks = markdown(selected.lore);
            int h = WikiRichTextRenderer.measureBlocksHeight(font, blocks, maxW, mdExpanded);
            richRegions = WikiRichTextRenderer.renderBlocks(g, font, blocks, x, y, maxW, 0, regionTop, regionBot,
                    C_ACCENT, mdExpanded);
            y += h + 8;
        }

        if (!selected.prerequisitesAny.isEmpty() && !unlocked && !lockedOut) {
            drawRule(g, px, y);
            y += 6;
            g.drawString(font, "\u00a77Unlock via any of:", x, y, C_DIM, false);
            y += 11;
            for (ResourceLocation anyId : selected.prerequisitesAny) {
                boolean met = unlockedSet.contains(anyId);
                String label = met ? "\u00a7a\u2714 " : "\u00a78\u25e6 ";
                String name = ResearchTreeRegistry.INSTANCE.getNode(anyId).map(n -> n.title).orElse(anyId.getPath());
                g.drawString(font, label + name, x + 6, y, C_TEXT, false);
                y += 10;
            }
            y += 4;
        }

        if (!selected.cost.isEmpty() && !lockedOut) {
            drawRule(g, px, y);
            y += 7;
            g.drawString(font, "\u00a7eCost", x, y, C_TEXT, false);
            y += 12;

            for (var entry : selected.cost.entrySet()) {
                ConfluxDataType type = entry.getKey();
                long cost = entry.getValue();
                long have = terminal != null ? ClientResearchCache.dataStored(type) : 0L;
                boolean met = have >= cost;

                g.drawString(font, type.displayComponent(), x + 4, y, C_TEXT, false);
                String amtStr = formatAmount(have) + " / " + formatAmount(cost);
                g.drawString(font, (met ? "\u00a7a" : "\u00a7c") + amtStr, x + maxW - font.width(amtStr), y, C_TEXT,
                        false);

                y += 11;
                g.fill(x, y, x + maxW, y + 3, 0xFF111122);
                int filled = (int) (maxW * Math.min(have / (float) cost, 1f));
                g.fill(x, y, x + filled, y + 3, met ? 0xFF33AA33 : 0xFF993333);
                y += 7;
            }
            y += 6;
        }

        List<ItemStack> rewards = new ArrayList<>();
        if (!isMystery && !lockedOut) {
            for (var unlock : selected.unlocks) {
                if (!"item".equals(unlock.type())) continue;
                ItemStack stack = ConfluxRewards.parse(unlock.value());
                if (!stack.isEmpty()) rewards.add(stack);
            }
        }
        if (!rewards.isEmpty()) {
            drawRule(g, px, y);
            y += 7;
            g.drawString(font, unlocked ? "\u00a7aRewards (claimed)" : "\u00a7aRewards", x, y, C_TEXT, false);
            y += 12;
            int ix = 0;
            for (ItemStack stack : rewards) {
                if (ix + 18 > maxW) {
                    ix = 0;
                    y += 20;
                }
                int ox = x + ix;
                g.fill(ox - 1, y - 1, ox + 17, y + 17, 0xFF141428);
                g.renderOutline(ox - 1, y - 1, 18, 18, unlocked ? 0xFF336633 : C_BORDER);
                g.renderItem(stack, ox, y);
                g.renderItemDecorations(font, stack, ox, y);
                if (mx >= ox - 1 && mx < ox + 17 && my >= y - 1 && my < y + 17 && my >= regionTop && my < regionBot) {
                    hoveredReward = stack;
                }
                ix += 20;
            }
            y += 24;
        }

        boolean otherUnlocks = false;
        for (var unlock : selected.unlocks) if (!"item".equals(unlock.type())) otherUnlocks = true;
        if (otherUnlocks && unlocked) {
            drawRule(g, px, y);
            y += 7;
            g.drawString(font, "\u00a7eUnlocks", x, y, C_TEXT, false);
            y += 12;
            for (var unlock : selected.unlocks) {
                String prefix = switch (unlock.type()) {
                    case "flag" -> "\u00a7b\u2691 ";
                    case "multiblock" -> "\u00a76\u2b21 ";
                    case "recipe_tag" -> "\u00a7d\u2697 ";
                    case "item" -> null;
                    default -> "\u00a77\u2022 ";
                };
                if (prefix == null) continue;
                for (String line : wrapText(prefix + unlock.value(), maxW - 8)) {
                    g.drawString(font, line, x + 4, y, C_TEXT, false);
                    y += 11;
                }
            }
        }

        int viewH = regionBot - regionTop;
        detailMax = Math.max(0, (y - startY) + 4 - viewH);
        detailScroll = Math.min(detailScroll, detailMax);
        g.disableScissor();
        drawScrollbar(g, vw - 5, regionTop, viewH, detailScroll, detailMax);

        int btnX = px + 10;
        int btnW = DETAIL_W - 20;
        if (unlocked) {
            g.fill(btnX, btnY, btnX + btnW, btnY + btnH, 0xFF0A1A0A);
            g.renderOutline(btnX, btnY, btnW, btnH, 0xFF336633);
            g.drawCenteredString(font, "\u00a7a\u2714 Already Researched", px + DETAIL_W / 2, btnY + 5, C_TEXT);
        } else if (lockedOut) {
            g.fill(btnX, btnY, btnX + btnW, btnY + btnH, 0xFF1A0808);
            g.renderOutline(btnX, btnY, btnW, btnH, 0xFF662222);
            g.drawCenteredString(font, "\u00a7c\u2718 Path Locked", px + DETAIL_W / 2, btnY + 5, C_TEXT);
        } else {
            boolean hov = mx >= btnX && mx < btnX + btnW && my >= btnY && my < btnY + btnH;
            int btnBg = canResearch ? (hov ? 0xFF1A3A1A : 0xFF112211) : 0xFF1A1020;
            int btnBrd = canResearch ? (hov ? 0xFF55CC55 : 0xFF338833) : 0xFF443344;
            g.fill(btnX, btnY, btnX + btnW, btnY + btnH, btnBg);
            g.renderOutline(btnX, btnY, btnW, btnH, btnBrd);
            String lbl = terminal == null ? "\u00a77No Terminal Connected" : isMystery ? "\u00a77Unknown Research" :
                    canResearch ? "\u00a7aResearch" : !available ? "\u00a77Prerequisites Missing" :
                            "\u00a77Insufficient Data";
            g.drawCenteredString(font, lbl, px + DETAIL_W / 2, btnY + 5, C_TEXT);
        }

        drawRegionTooltips(g, mx, my, hoveredReward);
    }

    private void renderTreeIntro(GuiGraphics g, int px, int pTop, int regionBot, int mx, int my) {
        if (trees.isEmpty()) {
            g.drawCenteredString(font, "§7No research trees loaded.", px + DETAIL_W / 2, pTop + 60, C_DIM);
            return;
        }
        ResearchTree tree = trees.get(activeTreeIdx);
        int[] p = progress(tree);
        int x = px + 12;
        int maxW = DETAIL_W - 30;
        g.drawString(font, font.plainSubstrByWidth(tree.title, DETAIL_W - 24), x, pTop + 10, C_TEXT, false);
        g.drawString(font, "\u00a78" + tree.group, x, pTop + 21, C_DIM, false);
        drawRule(g, px, pTop + 33);

        int regionTop = pTop + 38;
        g.enableScissor(px + 1, regionTop, vw, regionBot);
        int y = regionTop - (int) detailScroll;
        int startY = y;

        List<RichBlock> blocks = markdown(tree.description);
        int h = WikiRichTextRenderer.measureBlocksHeight(font, blocks, maxW, mdExpanded);
        richRegions = WikiRichTextRenderer.renderBlocks(g, font, blocks, x, y, maxW, 0, regionTop, regionBot, C_ACCENT,
                mdExpanded);
        y += h + 10;

        drawRule(g, px, y);
        y += 8;
        g.drawString(font, "\u00a7e" + p[0] + " of " + p[1] + " researched", x, y, C_TEXT, false);
        y += 12;
        g.fill(x, y, x + maxW, y + 4, 0xFF111122);
        int filled = p[1] == 0 ? 0 : (int) (maxW * (p[0] / (float) p[1]));
        g.fill(x, y, x + filled, y + 4, p[0] == p[1] && p[1] > 0 ? 0xFF55CC88 : C_ACCENT);
        y += 14;
        g.drawString(font, "\u00a78Select a node for details.", x, y, C_DIM, false);
        y += 12;

        int viewH = regionBot - regionTop;
        detailMax = Math.max(0, (y - startY) + 4 - viewH);
        detailScroll = Math.min(detailScroll, detailMax);
        g.disableScissor();
        drawScrollbar(g, vw - 5, regionTop, viewH, detailScroll, detailMax);
        drawRegionTooltips(g, mx, my, ItemStack.EMPTY);
    }

    private void drawScrollbar(GuiGraphics g, int x, int top, int viewH, float scroll, int max) {
        if (max <= 0) return;
        int total = viewH + max;
        int thumbH = Math.max(14, viewH * viewH / total);
        int thumbY = top + (int) ((viewH - thumbH) * (scroll / max));
        g.fill(x, top, x + 2, top + viewH, 0xFF111122);
        g.fill(x, thumbY, x + 2, thumbY + thumbH, C_ACCENT);
    }

    private void drawRegionTooltips(GuiGraphics g, int mx, int my, ItemStack hoveredReward) {
        if (!hoveredReward.isEmpty()) {
            g.renderTooltip(font, hoveredReward, mx, my);
            return;
        }
        for (RichSpan.Region r : richRegions) {
            if (!r.contains(mx, my)) continue;
            if (r.span() instanceof RichSpan.Tip t) {
                g.pose().pushPose();
                g.pose().translate(0f, 0f, 500f);
                g.renderTooltip(font, font.split(Component.literal(t.tooltip()), 240), mx, my);
                g.pose().popPose();
                break;
            }
            if (r.span() instanceof RichSpan.ItemIcon icon) {
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(icon.itemId());
                if (item != null) g.renderTooltip(font, new ItemStack(item).getHoverName(), mx, my);
                break;
            }
        }
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

        if (mx < SIDEBAR_W) {
            int row = treeRowAt(mx, my);
            if (row >= 0 && row != activeTreeIdx) {
                activeTreeIdx = row;
                selected = null;
                detailScroll = 0;
                resetPan();
            }
            return true;
        }

        if (mx >= detailLeft()) {
            int pBot = bottomTop();
            int btnH = 18;
            int btnY = pBot - btnH - 10;
            int btnX = detailLeft() + 10;
            int btnW = DETAIL_W - 20;
            if (my >= btnY && my < btnY + btnH && mx >= btnX && mx < btnX + btnW) {
                tryResearch();
                return true;
            }
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
                selected = (selected != null && selected.id.equals(hit.id)) ? null : hit;
                detailScroll = 0;
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
        if (mx < SIDEBAR_W) {
            sidebarScroll = (int) Math.max(0, Math.min(sidebarMax, sidebarScroll - delta * 24));
            return true;
        }
        if (mx >= detailLeft()) {
            detailScroll = (float) Math.max(0, Math.min(detailMax, detailScroll - delta * 20));
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
        if (kc == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
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

    private void tryResearch() {
        if (selected == null || terminal == null) return;
        if (selected.hidden && !selected.isVisible(ClientResearchCache.unlocked)) return;
        if (!costsAffordable(selected)) return;
        if (!selected.canUnlock(ClientResearchCache.unlocked, ClientResearchCache.lockedOut)) return;
        PacketDistributor.sendToServer(
                new C2SResearchUnlockPacket(selected.id, terminal.getBlockPos()));
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
