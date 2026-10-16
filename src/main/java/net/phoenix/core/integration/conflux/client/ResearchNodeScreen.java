package net.phoenix.core.integration.conflux.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.phoenix.core.integration.conflux.ConfluxDataType;
import net.phoenix.core.integration.conflux.network.C2SResearchUnlockPacket;
import net.phoenix.core.integration.conflux.research.ResearchNode;
import net.phoenix.core.integration.conflux.research.ResearchTree;
import net.phoenix.core.integration.conflux.research.ResearchTreeRegistry;
import net.phoenix.core.integration.conflux.research.ResearchUnlock;
import net.phoenix.core.integration.conflux.terminal.ResearchTerminalBlockEntity;
import net.phoenixvine.wiki.client.rich.RichBlock;
import net.phoenixvine.wiki.client.rich.RichSpan;
import net.phoenixvine.wiki.client.rich.WikiMarkdownParser;
import net.phoenixvine.wiki.client.rich.WikiRichTextRenderer;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.phoenix.core.integration.conflux.client.ConfluxTheme.*;

@OnlyIn(Dist.CLIENT)
public class ResearchNodeScreen extends Screen {

    private static boolean rememberFullscreen = false;

    private record Btn(int x0, int y0, int x1, int y1, Runnable action, @Nullable String tip) {

        boolean hit(double x, double y) {
            return x >= x0 && x < x1 && y >= y0 && y < y1;
        }
    }

    private final Screen parent;
    private final ResearchTree tree;
    private final ResearchNode node;
    @Nullable
    private final ResearchTerminalBlockEntity terminal;

    private boolean fullscreen = rememberFullscreen;
    private boolean connectionsOpen = false;

    private float loreScroll = 0, sideScroll = 0;
    private int loreMax = 0, sideMax = 0;
    private int[] loreRect = new int[4], sideRect = new int[4];

    private final List<Btn> buttons = new ArrayList<>();
    private List<RichSpan.Region> regions = List.of();
    private final Set<String> expanded = new HashSet<>();
    private String mdSource = null;
    private List<RichBlock> mdBlocks = List.of();
    private ItemStack hoveredStack = ItemStack.EMPTY;
    private String hoveredTip = null;
    private int cardX, cardY, cardW, cardH;

    private boolean unlocked, lockedOut, mystery, available, canResearch;
    private NodeYield cachedYield;
    private boolean yieldFor;
    private float yieldScroll = 0;
    private int yieldMax = 0;
    private int[] yieldRect = new int[4];

    public ResearchNodeScreen(Screen parent, ResearchTree tree, ResearchNode node,
                              @Nullable ResearchTerminalBlockEntity terminal) {
        super(Component.literal(node.title));
        this.parent = parent;
        this.tree = tree;
        this.node = node;
        this.terminal = terminal;
    }

    @Override
    protected void init() {
        if (parent.width != width || parent.height != height) parent.init(minecraft, width, height);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {}

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private void refreshState() {
        Set<ResourceLocation> done = ClientResearchCache.unlocked;
        unlocked = ClientResearchCache.isUnlocked(node.id);
        lockedOut = ClientResearchCache.isLockedOut(node.id);
        mystery = node.hidden && !unlocked;
        available = !unlocked && !lockedOut && node.canUnlock(done, ClientResearchCache.lockedOut);
        canResearch = available && affordable();
        if (cachedYield == null || yieldFor != unlocked) {
            yieldFor = unlocked;
            cachedYield = mystery || lockedOut ? new NodeYield(List.of(), List.of(), List.of(), List.of(), List.of(),
                    List.of()) : NodeYield.of(node, unlocked);
        }
    }

    private boolean affordable() {
        if (terminal == null) return false;
        for (var e : node.cost.entrySet()) if (ClientResearchCache.dataStored(e.getKey()) < e.getValue()) return false;
        return true;
    }

    private void research() {
        if (terminal == null || !canResearch || mystery) return;
        PacketDistributor.sendToServer(new C2SResearchUnlockPacket(node.id, terminal.getBlockPos()));
    }

    private List<RichBlock> markdown(String text) {
        if (!text.equals(mdSource)) {
            mdSource = text;
            mdBlocks = text.isEmpty() ? List.of() : WikiMarkdownParser.parse(text, false, true);
        }
        return mdBlocks;
    }

    private String loreText() {
        if (lockedOut) return "*This path has been sealed by a prior choice.*";
        if (mystery) return node.hint.isEmpty() ? "*Nothing is known about this yet.*" : "*" + node.hint + "*";
        return node.lore;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        ConfluxTheme.refresh();
        refreshState();
        buttons.clear();
        hoveredStack = ItemStack.EMPTY;
        hoveredTip = null;
        regions = List.of();

        if (fullscreen) {
            g.fill(0, 0, width, height, BG);
            renderFull(g, mx, my);
        } else {
            parent.render(g, -10000, -10000, pt);
            g.flush();

            g.pose().pushPose();
            g.pose().translate(0, 0, 600);
            g.fill(0, 0, width, height, 0xB0000008);
            renderCompact(g, mx, my);
            g.flush();
            g.pose().popPose();
        }

        g.flush();
        g.pose().pushPose();
        g.pose().translate(0, 0, 1000);
        if (connectionsOpen) renderConnections(g, mx, my);
        if (!hoveredStack.isEmpty()) {
            g.renderTooltip(font, hoveredStack, mx, my);
        } else if (hoveredTip != null) {
            g.renderTooltip(font, Component.literal(hoveredTip), mx, my);
        } else {
            for (RichSpan.Region r : regions) {
                if (!r.contains(mx, my)) continue;
                if (r.span() instanceof RichSpan.Tip t) {
                    g.renderTooltip(font, font.split(Component.literal(t.tooltip()), 240), mx, my);
                } else if (r.span() instanceof RichSpan.ItemIcon icon) {
                    var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(icon.itemId());
                    if (item != null) g.renderTooltip(font, new ItemStack(item).getHoverName(), mx, my);
                }
                break;
            }
        }
        g.pose().popPose();
    }

    private void button(int x0, int y0, int x1, int y1, Runnable action, @Nullable String tip, int mx, int my) {
        buttons.add(new Btn(x0, y0, x1, y1, action, tip));
        if (tip != null && mx >= x0 && mx < x1 && my >= y0 && my < y1) hoveredTip = tip;
    }

    private int statusColor() {
        return unlocked ? 0xFF88BBFF : lockedOut ? 0xFF884444 : available ? (canResearch ? 0xFF88EE88 : 0xFFDDCC66) :
                DIM;
    }

    private String statusText() {
        return unlocked ? "Researched" : lockedOut ? "Sealed" : available ? (canResearch ? "Ready to research" :
                "Available") : "Locked";
    }

    private void renderHeader(GuiGraphics g, int x, int y, int w, int mx, int my) {
        int acc = accent();
        g.fillGradient(x, y, x + w, y + 36, withAlpha(acc, 0x40), withAlpha(acc, 0x00));
        g.fill(x, y + 35, x + w, y + 36, withAlpha(acc, 0x99));

        int ix = x + 10, iy = y + 8;
        float pulse = canResearch ? 0.5f + 0.5f * (float) Math.sin(net.minecraft.Util.getMillis() / 380.0) : 0f;
        if (canResearch) {
            int glow = (int) (28 + 60 * pulse);
            g.fill(ix - 6, iy - 6, ix + 22, iy + 22, withAlpha(statusColor(), glow));
        }
        g.fill(ix - 3, iy - 3, ix + 19, iy + 19, withAlpha(acc, 0x30));
        g.renderOutline(ix - 3, iy - 3, 22, 22, canResearch ? withAlpha(statusColor(), (int) (150 + 105 * pulse)) :
                statusColor());
        if (mystery) {
            g.drawCenteredString(font, "?", ix + 8, iy + 4, TEXT);
        } else {
            g.renderItem(icon(node.icon), ix, iy);
        }

        int tx = x + 38;
        int right = x + w - 94;
        String title = mystery ? "???" : node.title;
        g.drawString(font, font.plainSubstrByWidth(title, right - tx), tx, y + 8, TEXT, false);
        g.drawString(font, font.plainSubstrByWidth("§8" + tree.title + "  ·  " + statusText(), right - tx), tx,
                y + 21, statusColor(), false);
        if (node.isCommitmentNode) {
            g.drawString(font, "§6◆", right - 8, y + 8, TEXT, false);
        }

        int bx = x + w - 18;
        boolean closeHov = mx >= bx && mx < bx + 14 && my >= y + 6 && my < y + 20;
        g.fill(bx, y + 6, bx + 14, y + 20, closeHov ? 0x33FFFFFF : 0x11FFFFFF);
        g.drawCenteredString(font, "×", bx + 7, y + 9, closeHov ? 0xFFFF8888 : DIM);
        button(bx, y + 6, bx + 14, y + 20, this::onClose, "Close", mx, my);

        int fx = bx - 18;
        boolean fsHov = mx >= fx && mx < fx + 14 && my >= y + 6 && my < y + 20;
        g.fill(fx, y + 6, fx + 14, y + 20, fsHov ? 0x33FFFFFF : 0x11FFFFFF);
        g.drawCenteredString(font, fullscreen ? "▣" : "□", fx + 7, y + 9, fsHov ? TEXT : acc);
        button(fx, y + 6, fx + 14, y + 20, () -> {
            fullscreen = !fullscreen;
            rememberFullscreen = fullscreen;
            loreScroll = 0;
            sideScroll = 0;
        }, fullscreen ? "Shrink to a card" : "Expand to fullscreen", mx, my);

        int cx = fx - 18;
        boolean conHov = mx >= cx && mx < cx + 14 && my >= y + 6 && my < y + 20;
        g.fill(cx, y + 6, cx + 14, y + 20, conHov || connectionsOpen ? 0x33FFFFFF : 0x11FFFFFF);
        g.drawCenteredString(font, "≡", cx + 7, y + 9, conHov || connectionsOpen ? 0xFF55CCFF : DIM);
        button(cx, y + 6, cx + 14, y + 20, () -> connectionsOpen = !connectionsOpen, "Requires / unlocks", mx, my);

        int sx = cx - 18;
        boolean pinned = ConfluxPins.isPinned(node.id);
        boolean pinHov = mx >= sx && mx < sx + 14 && my >= y + 6 && my < y + 20;
        g.fill(sx, y + 6, sx + 14, y + 20, pinHov ? 0x33FFFFFF : 0x11FFFFFF);
        g.drawCenteredString(font, pinned ? "\u2605" : "\u2606", sx + 7, y + 9, pinned ? 0xFFFFDD55 : DIM);
        button(sx, y + 6, sx + 14, y + 20, () -> ConfluxPins.toggle(node.id), pinned ? "Unpin" : "Pin to the sidebar",
                mx,
                my);
    }

    private int drawCosts(GuiGraphics g, int x, int y, int w, boolean big) {
        if (node.cost.isEmpty() || lockedOut) return y;
        int startY = y;
        g.drawString(font, "§eData cost", x, y, TEXT, false);
        y += big ? 14 : 12;
        for (var entry : node.cost.entrySet()) {
            ConfluxDataType type = entry.getKey();
            long cost = entry.getValue();
            long have = terminal != null ? ClientResearchCache.dataStored(type) : 0L;
            boolean met = have >= cost;
            g.drawString(font, type.displayComponent(), x + 2, y, TEXT, false);
            String amt = amount(have) + " / " + amount(cost);
            g.drawString(font, (met ? "§a" : "§c") + amt, x + w - font.width(amt), y, TEXT, false);
            y += 10;
            int bh = big ? 4 : 3;
            g.fill(x, y, x + w, y + bh, 0xFF111122);
            int filled = (int) (w * Math.min(have / (float) cost, 1f));
            int col = typeColor(type);
            g.fill(x, y, x + filled, y + bh, met ? col : withAlpha(col, 0x99));
            y += bh + (big ? 7 : 4);
        }
        return y + (y == startY ? 0 : 2);
    }

    private int drawPrereqs(GuiGraphics g, int x, int y, int w) {
        if (unlocked || lockedOut) return y;
        if (node.prerequisites.isEmpty() && node.prerequisitesAny.isEmpty()) return y;
        Set<ResourceLocation> done = ClientResearchCache.unlocked;
        g.drawString(font, "§7Requires", x, y, TEXT, false);
        y += 11;
        for (ResourceLocation id : node.prerequisites) {
            y = prereqRow(g, x, y, w, id, done.contains(id));
        }
        if (!node.prerequisitesAny.isEmpty()) {
            g.drawString(font, "§8any one of:", x + 4, y, TEXT, false);
            y += 10;
            for (ResourceLocation id : node.prerequisitesAny) y = prereqRow(g, x + 4, y, w - 4, id, done.contains(id));
        }
        return y + 4;
    }

    private int prereqRow(GuiGraphics g, int x, int y, int w, ResourceLocation id, boolean met) {
        String name = ResearchTreeRegistry.INSTANCE.getNode(id).map(n -> n.hidden && !met ? "???" : n.title)
                .orElse(id.getPath());
        g.drawString(font, font.plainSubstrByWidth((met ? "§a✔ " : "§8◦ ") + name, w - 6), x + 4, y,
                TEXT, false);
        return y + 10;
    }

    private int iconSection(@Nullable GuiGraphics g, int x, int y, int w, int mx, int my, String label,
                            List<ItemStack> stacks, int outline) {
        if (stacks.isEmpty()) return y;
        if (g != null) g.drawString(font, label, x, y, TEXT, false);
        y += 12;
        int ix = 0;
        for (ItemStack stack : stacks) {
            if (ix + 18 > w) {
                ix = 0;
                y += 20;
            }
            int ox = x + ix;
            if (g != null) {
                g.fill(ox - 1, y - 1, ox + 17, y + 17, 0xFF141428);
                g.renderOutline(ox - 1, y - 1, 18, 18, outline);
                g.renderItem(stack, ox, y);
                g.renderItemDecorations(font, stack, ox, y);
                if (mx >= ox - 1 && mx < ox + 17 && my >= y - 1 && my < y + 17) hoveredStack = stack;
            }
            ix += 20;
        }
        return y + 24;
    }

    private int textSection(@Nullable GuiGraphics g, int x, int y, int w, String label, List<String> lines, int limit) {
        if (lines.isEmpty()) return y;
        if (g != null) g.drawString(font, label, x, y, TEXT, false);
        y += 12;
        int shown = 0;
        for (String line : lines) {
            if (shown++ >= limit) {
                if (g != null) g.drawString(font, "\u00a78+" + (lines.size() - limit) + " more", x + 4, y, TEXT, false);
                y += 10;
                break;
            }
            if (g != null) g.drawString(font, font.plainSubstrByWidth(line, w - 6), x + 4, y, TEXT, false);
            y += 10;
        }
        return y + 4;
    }

    private int leadsSection(@Nullable GuiGraphics g, int x, int y, int w, int mx, int my, int limit) {
        List<ResearchNode> leads = cachedYield.leadsTo();
        if (leads.isEmpty()) return y;
        if (g != null) g.drawString(font, "\u00a7bLeads to", x, y, TEXT, false);
        y += 12;
        int shown = 0;
        for (ResearchNode n : leads) {
            if (shown++ >= limit) {
                if (g != null) g.drawString(font, "\u00a78+" + (leads.size() - limit) + " more", x + 4, y, TEXT, false);
                y += 10;
                break;
            }
            boolean done = ClientResearchCache.isUnlocked(n.id);
            boolean visible = !n.hidden || done;
            if (g != null) {
                boolean hov = visible && mx >= x && mx < x + w && my >= y - 1 && my < y + 9;
                if (hov) g.fill(x, y - 1, x + w, y + 9, 0x22FFFFFF);
                String name = visible ? n.title : "???";
                g.drawString(font, font.plainSubstrByWidth((done ? "\u00a7a\u2714 " : "\u00a78\u25e6 ") + name, w - 6),
                        x + 4,
                        y, visible ? TEXT : DIM, false);
                if (visible) button(x, y - 1, x + w, y + 9, () -> openNode(n), null, mx, my);
            }
            y += 10;
        }
        return y + 4;
    }

    private int drawYield(@Nullable GuiGraphics g, int x, int y, int w, int mx, int my, boolean compact) {
        NodeYield yd = cachedYield;
        if (yd == null || yd.isEmpty()) return y;
        y = iconSection(g, x, y, w, mx, my, unlocked ? "\u00a7aRewards (claimed)" : "\u00a7aRewards", yd.rewards(),
                unlocked ? 0xFF336633 : BORDER);
        y = iconSection(g, x, y, w, mx, my, "\u00a7dUnlocks crafting", yd.recipeItems(), withAlpha(0xDD66FF, 0xAA));
        y = textSection(g, x, y, w, "\u00a7dRecipes", yd.recipeLines(), compact ? 3 : 99);
        y = iconSection(g, x, y, w, mx, my, "\u00a76Unlocks machines", yd.machines(), withAlpha(0xFFAA33, 0xAA));
        y = textSection(g, x, y, w, "\u00a7eAlso", yd.otherLines(), compact ? 2 : 99);
        return leadsSection(g, x, y, w, mx, my, compact ? 3 : 99);
    }

    private void drawLore(GuiGraphics g, int x, int y, int w, int h, boolean compact) {
        loreRect = new int[] { x, y, x + w, y + h };
        List<RichBlock> blocks = markdown(loreText());
        int contentH = WikiRichTextRenderer.measureBlocksHeight(font, blocks, w - 6, expanded);
        loreMax = Math.max(0, contentH - h);
        loreScroll = Math.min(loreScroll, loreMax);
        g.enableScissor(x, y, x + w, y + h);
        regions = WikiRichTextRenderer.renderBlocks(g, font, blocks, x, y, w - 6, (int) loreScroll, y, y + h, accent(),
                expanded);
        g.disableScissor();
        if (loreMax > 0) scrollbar(g, x + w - 2, y, h, loreScroll, loreMax);
    }

    private void scrollbar(GuiGraphics g, int x, int top, int viewH, float scroll, int max) {
        int total = viewH + max;
        int thumbH = Math.max(14, viewH * viewH / total);
        int thumbY = top + (int) ((viewH - thumbH) * (scroll / max));
        g.fill(x, top, x + 2, top + viewH, 0xFF111122);
        g.fill(x, thumbY, x + 2, thumbY + thumbH, accent());
    }

    private void drawResearchButton(GuiGraphics g, int x, int y, int w, int h, int mx, int my) {
        if (unlocked) {
            g.fill(x, y, x + w, y + h, 0xFF0A1A0A);
            g.renderOutline(x, y, w, h, 0xFF336633);
            g.drawCenteredString(font, "§a✔ Researched", x + w / 2, y + (h - 8) / 2, TEXT);
            return;
        }
        if (lockedOut) {
            g.fill(x, y, x + w, y + h, 0xFF1A0808);
            g.renderOutline(x, y, w, h, 0xFF662222);
            g.drawCenteredString(font, "§c✘ Path sealed", x + w / 2, y + (h - 8) / 2, TEXT);
            return;
        }
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + h;
        int bg = canResearch ? (hov ? 0xFF1A3A1A : 0xFF112211) : 0xFF1A1020;
        int brd = canResearch ? (hov ? 0xFF55CC55 : 0xFF338833) : 0xFF443344;
        g.fill(x, y, x + w, y + h, bg);
        g.renderOutline(x, y, w, h, brd);
        String label = terminal == null ? "§7No terminal connected" : mystery ? "§7Unknown research" :
                canResearch ? "§aResearch  [R]" : !available ? "§7Prerequisites missing" :
                        "§7Insufficient data";
        g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, TEXT);
        if (canResearch) button(x, y, x + w, y + h, this::research, null, mx, my);
    }

    private @Nullable String archiveId() {
        if (!unlocked || ArchiveLinks.opener == null) return null;
        for (ResearchUnlock u : node.unlocks) if ("archive".equals(u.type())) return u.value();
        return null;
    }

    private void drawLoreButton(GuiGraphics g, int x, int y, int w, int h, int mx, int my) {
        String id = archiveId();
        if (id == null) return;
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + h;
        g.fill(x, y, x + w, y + h, hov ? 0xFF2A2A4A : 0xFF14142A);
        g.renderOutline(x, y, w, h, accent());
        g.drawCenteredString(font, "Read lore", x + w / 2, y + (h - 8) / 2, TEXT);
        button(x, y, x + w, y + h, () -> ArchiveLinks.opener.accept(this, id), null, mx, my);
    }

    private int measureCompactBody(int w) {
        List<RichBlock> blocks = markdown(loreText());
        return WikiRichTextRenderer.measureBlocksHeight(font, blocks, w - 6, expanded);
    }

    private void renderCompact(GuiGraphics g, int mx, int my) {
        cardW = Math.min(340, width - 24);
        int pad = 10;
        int inner = cardW - pad * 2;
        int headerH = 36;
        int footerH = 30;

        int costH = node.cost.isEmpty() || lockedOut ? 0 : 14 + node.cost.size() * 14 + 2;
        int prereqH = 0;
        if (!unlocked && !lockedOut) {
            int rows = node.prerequisites.size() +
                    (node.prerequisitesAny.isEmpty() ? 0 : 1 + node.prerequisitesAny.size());
            prereqH = rows == 0 ? 0 : 11 + rows * 10 + 4;
        }
        int yieldH = drawYield(null, 0, 0, inner, 0, 0, true);

        int maxCardH = height - 30;
        int fixed = headerH + footerH + costH + prereqH + yieldH + (yieldH > 0 ? 4 : 0) + 12;
        int loreFull = measureCompactBody(inner);
        int loreView = Math.max(30, Math.min(loreFull, maxCardH - fixed));
        cardH = fixed + loreView;
        cardX = (width - cardW) / 2;
        cardY = (height - cardH) / 2;

        int acc = accent();
        int borderA = canResearch ?
                (int) (0x55 + 0x55 * (0.5f + 0.5f * Math.sin(net.minecraft.Util.getMillis() / 380.0))) :
                0x66;
        g.fill(cardX - 1, cardY - 1, cardX + cardW + 1, cardY + cardH + 1, withAlpha(acc, borderA));
        g.fill(cardX, cardY, cardX + cardW, cardY + cardH, PANEL);
        renderHeader(g, cardX, cardY, cardW, mx, my);

        int x = cardX + pad;
        int y = cardY + headerH + 8;
        drawLore(g, x, y, inner, loreView, true);
        y += loreView + 6;

        if (prereqH > 0) {
            g.fill(x, y - 3, x + inner, y - 2, BORDER);
            y = drawPrereqs(g, x, y, inner);
        }
        if (costH > 0) {
            g.fill(x, y - 3, x + inner, y - 2, BORDER);
            y = drawCosts(g, x, y, inner, false);
        }
        if (yieldH > 0) {
            g.fill(x, y - 3, x + inner, y - 2, BORDER);
            y = drawYield(g, x, y + 2, inner, mx, my, true);
        }

        int fy = cardY + cardH - footerH + 6;
        int loreW = archiveId() == null ? 0 : 90;
        drawResearchButton(g, x, fy, inner - (loreW == 0 ? 0 : loreW + 6), 18, mx, my);
        drawLoreButton(g, x + inner - loreW, fy, loreW, 18, mx, my);
    }

    private void panel(GuiGraphics g, int x, int y, int w, int h, String title) {
        g.fill(x, y, x + w, y + h, PANEL);
        g.renderOutline(x, y, w, h, BORDER);
        g.fill(x, y, x + w, y + 14, PANEL_DEEP);
        g.fill(x, y + 13, x + w, y + 14, withAlpha(accent(), 0x77));
        g.drawString(font, "§7" + title.toUpperCase(), x + 8, y + 3, TEXT, false);
    }

    private void renderFull(GuiGraphics g, int mx, int my) {
        int margin = 8;
        int headerH = 36;
        renderHeader(g, 0, 0, width, mx, my);

        int top = headerH + margin;
        int footerH = 30;
        int bottom = height - footerH - margin;
        int h = bottom - top;
        int midW = Math.min(220, width / 4);
        int rightW = Math.min(240, width / 4);
        int leftW = width - midW - rightW - margin * 4;

        panel(g, margin, top, leftW, h, "Record");
        drawLore(g, margin + 10, top + 22, leftW - 20, h - 30, false);

        int mxp = margin + leftW + margin;
        panel(g, mxp, top, midW, h, "Requirements");
        sideRect = new int[] { mxp, top, mxp + midW, bottom };
        g.enableScissor(mxp + 1, top + 15, mxp + midW - 1, bottom - 1);
        int y = top + 22 - (int) sideScroll;
        int sy = y;
        y = drawPrereqs(g, mxp + 8, y, midW - 16);
        y = drawCosts(g, mxp + 8, y, midW - 16, true);
        if (node.exclusionGroup != null && !node.exclusionGroup.isEmpty() && !unlocked) {
            g.drawString(font, "§6⚠ Choosing this seals sibling paths.", mxp + 8, y, TEXT, false);
            y += 12;
        }
        g.disableScissor();
        sideMax = Math.max(0, (y - sy) + 8 - (h - 26));
        sideScroll = Math.min(sideScroll, sideMax);

        int rx = mxp + midW + margin;
        panel(g, rx, top, rightW, h, "Yield");
        yieldRect = new int[] { rx, top, rx + rightW, bottom };
        g.enableScissor(rx + 1, top + 15, rx + rightW - 1, bottom - 1);
        int ry0 = top + 22 - (int) yieldScroll;
        int ry = drawYield(g, rx + 8, ry0, rightW - 16, mx, my, false);
        if (cachedYield == null || cachedYield.isEmpty()) {
            g.drawString(font, "\u00a78Nothing is known about the yield.", rx + 8, ry, TEXT, false);
            ry += 12;
        }
        g.disableScissor();
        yieldMax = Math.max(0, (ry - ry0) + 8 - (h - 26));
        yieldScroll = Math.min(yieldScroll, yieldMax);
        if (yieldMax > 0) scrollbar(g, rx + rightW - 4, top + 15, h - 17, yieldScroll, yieldMax);

        int fy = height - footerH + 6;
        drawResearchButton(g, width / 2 - 110, fy, 220, 18, mx, my);
        drawLoreButton(g, width / 2 + 116, fy, 90, 18, mx, my);
        g.drawString(font, "§8[F] shrink   [R] research   [Esc] close", margin + 2, fy + 5, DIM, false);
    }

    private List<ResearchNode> unlockedChildren() {
        List<ResearchNode> out = new ArrayList<>();
        for (ResearchNode n : tree.getNodes()) {
            if (n.prerequisites.contains(node.id) || n.prerequisitesAny.contains(node.id)) out.add(n);
        }
        return out;
    }

    private void renderConnections(GuiGraphics g, int mx, int my) {
        List<ResearchNode> requires = new ArrayList<>();
        for (ResourceLocation id : node.prerequisites)
            ResearchTreeRegistry.INSTANCE.getNode(id).ifPresent(requires::add);
        for (ResourceLocation id : node.prerequisitesAny)
            ResearchTreeRegistry.INSTANCE.getNode(id).ifPresent(requires::add);
        List<ResearchNode> unlocks = unlockedChildren();

        int rowH = 12;
        int w = 180;
        int rows = requires.size() + unlocks.size();
        int sections = (requires.isEmpty() ? 0 : 1) + (unlocks.isEmpty() ? 0 : 1);
        int h = 8 + sections * 12 + rows * rowH + 4;
        if (rows == 0) h = 26;
        int px = fullscreen ? width - w - 8 : Math.min(width - w - 4, cardX + cardW - w);
        int py = fullscreen ? 40 : cardY + 38;

        g.fill(px, py, px + w, py + h, 0xFF0A0A12);
        g.renderOutline(px, py, w, h, accent());
        int y = py + 6;
        if (rows == 0) {
            g.drawString(font, "§8(no connections)", px + 8, y + 2, TEXT, false);
            return;
        }
        y = connectionRows(g, px, y, w, "Requires", requires, mx, my);
        connectionRows(g, px, y, w, "Unlocks", unlocks, mx, my);
    }

    private int connectionRows(GuiGraphics g, int px, int y, int w, String label, List<ResearchNode> list, int mx,
                               int my) {
        if (list.isEmpty()) return y;
        g.drawString(font, "§8" + label + ":", px + 8, y, TEXT, false);
        y += 12;
        for (ResearchNode n : list) {
            boolean done = ClientResearchCache.isUnlocked(n.id);
            boolean visible = !n.hidden || done;
            boolean hov = visible && mx >= px + 4 && mx < px + w - 4 && my >= y - 1 && my < y + 11;
            if (hov) g.fill(px + 4, y - 1, px + w - 4, y + 11, 0x22FFFFFF);
            String name = visible ? n.title : "???";
            g.drawString(font, font.plainSubstrByWidth((done ? "§a✔ " : "§8◦ ") + name, w - 20), px + 10, y,
                    visible ? TEXT : DIM, false);
            if (visible) button(px + 4, y - 1, px + w - 4, y + 11, () -> openNode(n), null, mx, my);
            y += 12;
        }
        return y;
    }

    private void openNode(ResearchNode target) {
        for (ResearchTree t : ResearchTreeRegistry.INSTANCE.getAllTrees()) {
            if (t.getNode(target.id).isPresent()) {
                Minecraft.getInstance().setScreen(new ResearchNodeScreen(parent, t, target, terminal));
                return;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0) {
            
            for (int i = buttons.size() - 1; i >= 0; i--) {
                Btn b = buttons.get(i);
                if (b.hit(mx, my)) {
                    b.action().run();
                    return true;
                }
            }
            if (connectionsOpen) {
                connectionsOpen = false;
                return true;
            }
            if (!fullscreen && (mx < cardX || mx >= cardX + cardW || my < cardY || my >= cardY + cardH)) {
                onClose();
                return true;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double delta) {
        if (inside(loreRect, mx, my)) {
            loreScroll = (float) Math.max(0, Math.min(loreMax, loreScroll - delta * 18));
            return true;
        }
        if (fullscreen && inside(yieldRect, mx, my)) {
            yieldScroll = (float) Math.max(0, Math.min(yieldMax, yieldScroll - delta * 18));
            return true;
        }
        if (fullscreen && inside(sideRect, mx, my)) {
            sideScroll = (float) Math.max(0, Math.min(sideMax, sideScroll - delta * 18));
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, delta);
    }

    private static boolean inside(int[] r, double x, double y) {
        return x >= r[0] && x < r[2] && y >= r[1] && y < r[3];
    }

    @Override
    public boolean keyPressed(int kc, int sc, int mod) {
        if (kc == GLFW.GLFW_KEY_ESCAPE) {
            if (connectionsOpen) connectionsOpen = false;
            else onClose();
            return true;
        }
        if (kc == GLFW.GLFW_KEY_F) {
            fullscreen = !fullscreen;
            rememberFullscreen = fullscreen;
            loreScroll = 0;
            return true;
        }
        if (kc == GLFW.GLFW_KEY_R || kc == GLFW.GLFW_KEY_ENTER) {
            research();
            return true;
        }
        return super.keyPressed(kc, sc, mod);
    }
}
