package net.phoenix.core.shop.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.math.Axis;

import net.phoenix.chromatic_codes.api.ChromaticEffectsRegistry;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.conflux.client.render.MotionClock;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.shop.RewardSpec;
import net.phoenix.core.shop.ShopEntry;
import net.phoenix.core.shop.network.C2SAddShopEntryPacket;
import net.phoenix.core.shop.network.C2SBuyShopEntryPacket;
import net.phoenix.core.shop.network.C2SRemoveShopEntryPacket;
import net.phoenix.core.shop.network.S2CShopSyncPacket.ShopEntryView;
import net.phoenix.core.shop.reward.CommandShopReward;
import net.phoenix.core.shop.reward.ItemShopReward;
import net.phoenix.core.shop.reward.ResearchUnlockShopReward;
import net.phoenix.core.shop.reward.ThreadShopReward;
import net.phoenixvine.wiki.theme.PhoenixTheme;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@OnlyIn(Dist.CLIENT)
public class PhoenixShopScreen extends Screen {

    private static final String[] REWARD_TYPES = { ItemShopReward.TYPE, CommandShopReward.TYPE,
            ResearchUnlockShopReward.TYPE, ThreadShopReward.TYPE };

    private static final class Mote {

        float x, y, vx, vy, life, maxLife, size;
        int color;
    }

    private List<ShopEntryView> entries;
    private boolean editing = false;

    private int[] palette;
    private int cText, cDim, cBorder, cBorderDim, cPanel1, cPanel2, cBg1, cBg2;

    private static final int MIN_PANEL_W = 520;
    private static final int MIN_PANEL_H = 420;
    private float uiScale = 1f;
    private int vw, vh;

    private float scrollY = 0f;

    private EditBox searchBox;
    private String searchQuery = "";
    private String selectedCategory = "All";

    private EditBox nameBox;
    private EditBox costBox;
    private EditBox paramBox;
    private EditBox categoryBox;
    private ItemStack pendingIcon = ItemStack.EMPTY;
    private int rewardTypeIndex = 0;
    private final List<RewardSpec> pendingRewards = new ArrayList<>();

    private java.util.UUID editingEntryId = null;

    private String pendingName = "New Entry";
    private String pendingCostStr = "1";
    private String pendingCategory = ShopEntry.DEFAULT_CATEGORY;

    private int editorIconPreviewX, editorIconPreviewY;
    private int editorRewardListY;
    private int editorCostLabelX, editorCostLabelY;

    private final List<Mote> motes = new ArrayList<>();
    private final Random random = new Random();
    private final MotionClock clock = new MotionClock();
    private long lastNanos = System.nanoTime();

    private final java.util.Map<java.util.UUID, Float> hoverStart = new java.util.HashMap<>();

    private boolean settingsOpen = false;

    private String warningEntryName = null;
    private float warningTimer = 0f;

    private enum MinigameType { SWEEP, MASH, REACTION }

    private boolean minigameActive = false;
    private MinigameType minigameType;
    private java.util.UUID minigameEntryId;
    private String minigameEntryName;
    private float minigameStartElapsed;
    private String minigameResultText = null;
    private boolean minigameResultSuccess = false;
    private float minigameResultTimer = 0f;

    private float minigameSweetCenter;
    private static final float SWEEP_SWEET_WIDTH = 0.16f;

    private int mashCount;
    private static final int MASH_REQUIRED = 8;
    private static final float MASH_TIME_LIMIT = 2.2f;

    private float reactionGoAt;
    private static final float REACTION_MIN_DELAY = 1.0f;
    private static final float REACTION_MAX_DELAY = 2.4f;
    private static final float REACTION_WINDOW = 0.6f;

    private PhoenixShopScreen(List<ShopEntryView> entries) {
        super(Component.literal("Phoenix Feather Shop"));
        this.entries = entries;
    }

    public static void openOrUpdate(List<ShopEntryView> entries) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PhoenixShopScreen shop) {
            shop.entries = entries;
            return;
        }
        mc.setScreen(new PhoenixShopScreen(entries));
    }

    @Override
    protected void init() {
        float neededW = MIN_PANEL_W + 40f;
        float neededH = MIN_PANEL_H + 40f;
        uiScale = (width < neededW || height < neededH) ?
                Math.min(width / neededW, height / neededH) : 1f;
        uiScale = Math.max(0.1f, uiScale);
        vw = Math.round(width / uiScale);
        vh = Math.round(height / uiScale);

        refreshTheme();
        for (int i = 0; i < 30; i++) motes.add(spawnMote(true));
        rebuildShopWidgets();
    }

    private void refreshTheme() {
        PhoenixTheme t = PhoenixTheme.current();
        palette = new int[] { t.accent.getColor(), t.done.getColor(), t.activeColor.getColor(),
                t.ally.getColor(), t.locked.getColor() };
        cText = t.text.getColor();
        cDim = t.textDim.getColor();
        cBorder = t.accent.getColor();
        cBorderDim = t.border.getColor();
        cPanel1 = (t.panel.getColor() & 0x00FFFFFF) | 0xF0000000;
        cPanel2 = (t.header.getColor() & 0x00FFFFFF) | 0xF0000000;
        cBg1 = t.bg.getColor();
        cBg2 = t.panel.getColor();
    }

    private void rebuildShopWidgets() {
        clearWidgets();

        int panelW = Math.min(vw - 40, MIN_PANEL_W);
        int panelH = Math.min(vh - 40, MIN_PANEL_H);
        int px = (vw - panelW) / 2;
        int py = (vh - panelH) / 2;

        addRenderableWidget(Button.builder(Component.literal("Settings"), b -> {
            settingsOpen = !settingsOpen;
            rebuildShopWidgets();
        }).bounds(px + panelW - 138, py + 6, 64, 16).build());

        if (settingsOpen) {
            boolean minigameOn = PhoenixConfigs.INSTANCE == null || PhoenixConfigs.INSTANCE.shopMinigameEnabled;
            addRenderableWidget(Button.builder(
                    Component.literal("Buy Minigame: " + (minigameOn ? "ON" : "OFF")), b -> {
                        if (PhoenixConfigs.INSTANCE != null) {
                            PhoenixConfigs.INSTANCE.shopMinigameEnabled = !PhoenixConfigs.INSTANCE.shopMinigameEnabled;
                            if (PhoenixConfigs.CONFIG_HOLDER != null) PhoenixConfigs.CONFIG_HOLDER.save();
                        }
                        rebuildShopWidgets();
                    }).bounds(px + panelW - 138, py + 26, 130, 16).build());
        }

        boolean isOp = minecraft != null && minecraft.player != null && minecraft.player.hasPermissions(2);
        if (isOp) {
            addRenderableWidget(Button.builder(Component.literal(editing ? "Browse" : "+ Add Entry"), b -> {
                editing = !editing;
                if (!editing) {
                    pendingRewards.clear();
                    pendingIcon = ItemStack.EMPTY;
                    editingEntryId = null;
                    pendingName = "New Entry";
                    pendingCostStr = "1";
                    pendingCategory = ShopEntry.DEFAULT_CATEGORY;
                }
                rebuildShopWidgets();
            }).bounds(px + 8, py + 6, 90, 16).build());
        }

        if (editing && isOp) {
            buildEditorWidgets(px, py, panelW);
        } else {
            buildBrowseWidgets(px, py, panelW, panelH);
        }
    }

    private static final int CARD_COLS = 2;
    private static final int CARD_GAP = 6;
    private static final int CARD_H = 90;

    private int gridTop(int py) {
        return py + 80;
    }

    private int gridBottom(int py, int panelH) {
        return py + panelH - 10;
    }

    private List<ShopEntryView> visibleEntries() {
        String q = searchQuery.trim().toLowerCase();
        List<ShopEntryView> out = new ArrayList<>();
        for (ShopEntryView e : entries) {
            if (!selectedCategory.equals("All") && !e.category().equalsIgnoreCase(selectedCategory)) continue;
            if (!q.isEmpty() && !e.name().toLowerCase().contains(q)) continue;
            out.add(e);
        }
        return out;
    }

    private List<String> categories() {
        java.util.LinkedHashSet<String> cats = new java.util.LinkedHashSet<>();
        cats.add("All");
        for (ShopEntryView e : entries) cats.add(e.category());
        return new ArrayList<>(cats);
    }

    private record TabRect(String name, int x, int y, int w, int h) {}

    private static final int TAB_H = 14;

    private List<TabRect> layoutTabs(int px, int py) {
        List<TabRect> out = new ArrayList<>();
        int x = px + 8;
        int y = py + 58;
        for (String cat : categories()) {
            int w = font.width(cat) + 10;
            out.add(new TabRect(cat, x, y, w, TAB_H));
            x += w + 4;
        }
        return out;
    }

    private void renderTabs(GuiGraphics g, int px, int py, int mx, int my) {
        for (TabRect tab : layoutTabs(px, py)) {
            boolean selected = tab.name().equals(selectedCategory);
            boolean hover = mx >= tab.x() && mx < tab.x() + tab.w() && my >= tab.y() && my < tab.y() + tab.h();
            int bg = selected ? ((0xFF << 24) | (cBorder & 0xFFFFFF))
                    : hover ? (blend(cPanel1, cPanel2, 0.5f) | 0xFF000000) : cPanel1;
            g.fill(tab.x(), tab.y(), tab.x() + tab.w(), tab.y() + tab.h(), bg);
            int textCol = selected ? contrastColor(cBorder) : cDim;
            drawCenteredNoShadow(g, tab.name(), tab.x() + tab.w() / 2, tab.y() + 3, textCol);
        }
    }

    private int[] cardBounds(int px, int py, int panelW, int slot) {
        int gridX = px + 8;
        int gridY = gridTop(py) - Math.round(scrollY);
        int cardW = (panelW - 16 - CARD_GAP * (CARD_COLS - 1)) / CARD_COLS;
        int col = slot % CARD_COLS;
        int row = slot / CARD_COLS;
        int cx = gridX + col * (cardW + CARD_GAP);
        int cy = gridY + row * (CARD_H + CARD_GAP);
        return new int[] { cx, cy, cardW, CARD_H };
    }

    private void clampScroll(int entryCount, int py, int panelW, int panelH) {
        int rows = (entryCount + CARD_COLS - 1) / CARD_COLS;
        int contentH = rows > 0 ? rows * CARD_H + (rows - 1) * CARD_GAP : 0;
        int viewportH = gridBottom(py, panelH) - gridTop(py);
        float maxScroll = Math.max(0, contentH - viewportH);
        scrollY = Math.max(0f, Math.min(maxScroll, scrollY));
    }

    private boolean isOp() {
        return minecraft != null && minecraft.player != null && minecraft.player.hasPermissions(2);
    }

    private void renderCards(GuiGraphics g, List<ShopEntryView> visible, int px, int py, int panelW, int panelH,
            int mx, int my) {
        if (visible.isEmpty()) {
            String msg = entries.isEmpty() ? "The shop is empty." : "No entries match.";
            g.drawString(font, msg, px + 8, gridTop(py) + 4, cDim, false);
            return;
        }

        int top = gridTop(py);
        int bottom = gridBottom(py, panelH);
        enableScissorScaled(g, px, top, px + panelW, bottom);

        java.util.Set<java.util.UUID> stillHovered = new java.util.HashSet<>();
        for (int i = 0; i < visible.size(); i++) {
            ShopEntryView entry = visible.get(i);
            int[] b = cardBounds(px, py, panelW, i);
            int cx = b[0], cy = b[1], cw = b[2], ch = b[3];
            if (cy + ch < top || cy > bottom) continue;
            boolean hover = mx >= cx && mx < cx + cw && my >= cy && my < cy + ch && my >= top && my < bottom;

            float wiggle = 0f;
            if (hover) {
                stillHovered.add(entry.id());
                float since = hoverStart.computeIfAbsent(entry.id(), id -> clock.getElapsed());
                float held = clock.getElapsed() - since;
                float amplitude = Math.max(0f, 3.5f * (1f - held));
                wiggle = amplitude > 0.01f ? (float) Math.sin(clock.getElapsed() * 16f) * amplitude : 0f;
            }
            float centerX = cx + cw / 2f;
            float centerY = cy + ch / 2f;

            g.pose().pushPose();
            g.pose().translate(centerX, centerY, 0);
            g.pose().mulPose(Axis.ZP.rotationDegrees(wiggle));
            if (hover) g.pose().scale(1.03f, 1.03f, 1f);
            g.pose().translate(-centerX, -centerY, 0);

            renderCard(g, entry, cx, cy, cw, ch, hover);

            g.pose().popPose();
        }
        g.disableScissor();
        hoverStart.keySet().retainAll(stillHovered);

        renderScrollbar(g, visible.size(), px, py, panelW, panelH);
    }

    private void renderScrollbar(GuiGraphics g, int entryCount, int px, int py, int panelW, int panelH) {
        int top = gridTop(py);
        int bottom = gridBottom(py, panelH);
        int rows = (entryCount + CARD_COLS - 1) / CARD_COLS;
        int contentH = rows > 0 ? rows * CARD_H + (rows - 1) * CARD_GAP : 0;
        int viewportH = bottom - top;
        if (contentH <= viewportH) return;

        int trackX = px + panelW - 4;
        g.fill(trackX, top, trackX + 2, bottom, (0x50 << 24));

        int thumbH = Math.max(12, (int) ((float) viewportH / contentH * viewportH));
        float scrollFrac = scrollY / Math.max(1f, contentH - viewportH);
        int thumbY = top + (int) (scrollFrac * (viewportH - thumbH));
        g.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, (0xB0 << 24) | (cBorder & 0xFFFFFF));
    }

    private int accentColor(ShopEntryView entry) {
        float h = MotionClock.hash(entry.id().hashCode());
        int idx = Math.floorMod((int) (h * palette.length), palette.length);
        return palette[idx];
    }

    private void renderCard(GuiGraphics g, ShopEntryView entry, int cx, int cy, int cw, int ch, boolean hover) {
        boolean canAfford = getFeatherCount() >= entry.cost();
        int accent = accentColor(entry);

        int fillColA = hover ? blend(cPanel1, cPanel2, 0.35f) | 0xFF000000 : cPanel1;
        int fillColB = hover ? blend(cPanel2, accent, 0.18f) | 0xFF000000 : cPanel2;
        g.fillGradient(cx, cy, cx + cw, cy + ch, fillColA, fillColB);

        g.fill(cx, cy, cx + 3, cy + ch, (0xFF << 24) | (accent & 0xFFFFFF));

        int borderCol = (0xFF << 24) | ((hover ? cBorder : cBorderDim) & 0xFFFFFF);
        g.fill(cx, cy, cx + cw, cy + 1, borderCol);
        g.fill(cx, cy + ch - 1, cx + cw, cy + ch, borderCol);
        g.fill(cx, cy, cx + 1, cy + ch, borderCol);
        g.fill(cx + cw - 1, cy, cx + cw, cy + ch, borderCol);

        if (hover) {
            int glow = (int) (60 + 60 * MotionClock.Signature.DEFAULT.pulse(clock.getElapsed()));
            g.fill(cx + 2, cy + 1, cx + cw - 1, cy + 2, (glow << 24) | (accent & 0xFFFFFF));
        }

        int slotX = cx + 6, slotY = cy + 6;
        g.fill(slotX - 2, slotY - 2, slotX + 18, slotY + 18, 0x60000000);
        g.fill(slotX - 2, slotY - 2, slotX + 18, slotY - 1, borderCol);
        g.fill(slotX - 2, slotY + 17, slotX + 18, slotY + 18, borderCol);
        g.fill(slotX - 2, slotY - 2, slotX - 1, slotY + 18, borderCol);
        g.fill(slotX + 17, slotY - 2, slotX + 18, slotY + 18, borderCol);
        g.renderItem(entry.icon(), slotX, slotY);

        g.drawString(font, trim(entry.name(), cw - 32), cx + 28, cy + 7, cText, false);

        if (entry.rewardSpecs().size() > 1) {
            String count = "x" + entry.rewardSpecs().size();
            int badgeW = font.width(count) + 4;
            int reserveForX = isOp() ? 10 : 0;
            int bx = cx + cw - 4 - reserveForX - badgeW;
            int by = cy + 3;
            g.fill(bx, by, bx + badgeW, by + 9, (0xFF << 24) | (accent & 0xFFFFFF));
            drawCenteredNoShadow(g, count, bx + badgeW / 2, by + 1, contrastColor(accent));
        }

        List<FormattedCharSequence> desc = font.split(Component.literal(String.join(" | ", entry.rewardDescriptions())),
                cw - 10);
        if (!desc.isEmpty()) {
            g.drawString(font, desc.get(0), cx + 5, cy + 27, cDim, false);
        }

        renderRewardStrip(g, entry, cx, cy, cw);

        int footerY = cy + ch - 20;
        int btnW = Math.min(56, Math.max(36, cw / 3));
        int btnX = cx + cw - btnW - 4;

        String cost = entry.cost() + " Feathers";
        int costMaxW = btnX - (cx + 5) - 4;
        g.drawString(font, Component.literal(trim(cost, Math.max(10, costMaxW)))
                .withStyle(canAfford ? ChatFormatting.GOLD : ChatFormatting.RED), cx + 5, footerY + 2, cText,
                false);

        int btnCol = canAfford ? ((0xFF << 24) | (accent & 0xFFFFFF)) : 0xFF3A1414;
        g.fill(btnX, footerY, btnX + btnW, footerY + 14, btnCol);
        int btnBorder = canAfford ? borderCol : 0xFFFF5555;
        g.fill(btnX, footerY, btnX + btnW, footerY + 1, btnBorder);
        g.fill(btnX, footerY + 13, btnX + btnW, footerY + 14, btnBorder);
        g.fill(btnX, footerY, btnX + 1, footerY + 14, btnBorder);
        g.fill(btnX + btnW - 1, footerY, btnX + btnW, footerY + 14, btnBorder);

        String buyLabel = canAfford ? "BUY" : "LOCKED";
        enableScissorScaled(g, btnX, footerY, btnX + btnW, footerY + 14);
        drawCenteredNoShadow(g, buyLabel, btnX + btnW / 2, footerY + 3,
                canAfford ? contrastColor(accent) : 0xFFFF9999);
        g.disableScissor();

        if (isOp()) {
            g.drawString(font, Component.literal("x").withStyle(ChatFormatting.RED), cx + cw - 9, cy + 3,
                    0xFFFF5555, false);
        }
    }

    private static final int REWARD_STRIP_MAX = 5;

    private void renderRewardStrip(GuiGraphics g, ShopEntryView entry, int cx, int cy, int cw) {
        List<RewardSpec> specs = entry.rewardSpecs();
        if (specs.isEmpty()) return;

        int stripY = cy + 40;
        int shown = Math.min(REWARD_STRIP_MAX, specs.size());
        int x = cx + 5;
        for (int i = 0; i < shown; i++) {
            ItemStack icon = rewardKindIcon(specs.get(i));
            if (!icon.isEmpty()) g.renderItem(icon, x, stripY);
            x += 13;
        }
        if (specs.size() > shown) {
            g.drawString(font, "+" + (specs.size() - shown), x + 1, stripY + 4, cDim, false);
        }
    }

    private ItemStack rewardKindIcon(RewardSpec spec) {
        return switch (spec.type()) {
            case ItemShopReward.TYPE -> spec.itemParam();
            case CommandShopReward.TYPE -> new ItemStack(net.minecraft.world.item.Items.COMMAND_BLOCK);
            case ResearchUnlockShopReward.TYPE -> new ItemStack(net.minecraft.world.item.Items.KNOWLEDGE_BOOK);
            case ThreadShopReward.TYPE -> new ItemStack(net.minecraft.world.item.Items.NETHER_STAR);
            default -> ItemStack.EMPTY;
        };
    }

    private int getFeatherCount() {
        if (minecraft == null || minecraft.player == null) return 0;
        return minecraft.player.getInventory()
                .countItem(net.phoenix.core.common.data.item.PhoenixItems.PHOENIX_FEATHER.get());
    }

    private String trim(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        while (text.length() > 1 && font.width(text + "..") > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "..";
    }

    private int blend(int a, int b, float t) {
        return MotionClock.lerpColor(0xFF000000 | a, 0xFF000000 | b, t);
    }

    private int contrastColor(int bgRgb) {
        int r = (bgRgb >> 16) & 0xFF, gCh = (bgRgb >> 8) & 0xFF, b = bgRgb & 0xFF;
        double luminance = (0.299 * r + 0.587 * gCh + 0.114 * b) / 255.0;
        return luminance > 0.55 ? 0xFF101010 : 0xFFF5F5F5;
    }

    private void drawCenteredNoShadow(GuiGraphics g, String text, int centerX, int y, int color) {
        g.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
    }

    @Override
    public boolean mouseClicked(double rmx, double rmy, int button) {
        double mx = rmx / uiScale;
        double my = rmy / uiScale;
        if (minigameActive) {
            if (button == 0) handleMinigameStrike();
            return true;
        }

        if ((button == 0 || button == 1) && !editing) {
            int panelW = Math.min(vw - 40, MIN_PANEL_W);
            int panelH = Math.min(vh - 40, MIN_PANEL_H);
            int px = (vw - panelW) / 2;
            int py = (vh - panelH) / 2;

            if (button == 0) {
                for (TabRect tab : layoutTabs(px, py)) {
                    if (mx >= tab.x() && mx < tab.x() + tab.w() && my >= tab.y() && my < tab.y() + tab.h()) {
                        selectedCategory = tab.name();
                        scrollY = 0f;
                        return true;
                    }
                }
            }

            List<ShopEntryView> visible = visibleEntries();
            int top = gridTop(py);
            int bottom = gridBottom(py, panelH);
            for (int i = 0; i < visible.size(); i++) {
                ShopEntryView entry = visible.get(i);
                int[] b = cardBounds(px, py, panelW, i);
                int cx = b[0], cy = b[1], cw = b[2], ch = b[3];
                if (mx < cx || mx >= cx + cw || my < cy || my >= cy + ch) continue;
                if (my < top || my >= bottom) continue;

                if (button == 1) {
                    if (isOp()) editEntry(entry);
                    return true;
                }

                if (isOp() && mx >= cx + cw - 10 && my < cy + 10) {
                    PhoenixNetwork.CHANNEL.sendToServer(new C2SRemoveShopEntryPacket(entry.id()));
                } else if (getFeatherCount() < entry.cost()) {
                    warningEntryName = entry.name();
                    warningTimer = 1.5f;
                } else if (PhoenixConfigs.INSTANCE != null && PhoenixConfigs.INSTANCE.shopMinigameEnabled) {
                    startMinigame(entry);
                } else {
                    PhoenixNetwork.CHANNEL.sendToServer(new C2SBuyShopEntryPacket(entry.id()));
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private void editEntry(ShopEntryView entry) {
        editingEntryId = entry.id();
        pendingName = entry.name();
        pendingCostStr = String.valueOf(entry.cost());
        pendingCategory = entry.category();
        pendingIcon = entry.icon().copy();
        pendingRewards.clear();
        pendingRewards.addAll(entry.rewardSpecs());
        rewardTypeIndex = 0;
        editing = true;
        rebuildShopWidgets();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (!editing && !minigameActive) {
            int panelW = Math.min(vw - 40, MIN_PANEL_W);
            int panelH = Math.min(vh - 40, MIN_PANEL_H);
            int py = (vh - panelH) / 2;
            scrollY -= delta * 28f;
            clampScroll(visibleEntries().size(), py, panelW, panelH);
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean mouseDragged(double rmx, double rmy, int button, double dragX, double dragY) {
        return super.mouseDragged(rmx / uiScale, rmy / uiScale, button, dragX / uiScale, dragY / uiScale);
    }

    @Override
    public boolean mouseReleased(double rmx, double rmy, int button) {
        return super.mouseReleased(rmx / uiScale, rmy / uiScale, button);
    }

    private void startMinigame(ShopEntryView entry) {
        minigameActive = true;
        minigameEntryId = entry.id();
        minigameEntryName = entry.name();
        minigameStartElapsed = clock.getElapsed();
        minigameResultText = null;
        minigameType = MinigameType.values()[random.nextInt(MinigameType.values().length)];

        switch (minigameType) {
            case SWEEP -> minigameSweetCenter = 0.2f + random.nextFloat() * 0.6f;
            case MASH -> mashCount = 0;
            case REACTION -> reactionGoAt = minigameStartElapsed + REACTION_MIN_DELAY
                    + random.nextFloat() * (REACTION_MAX_DELAY - REACTION_MIN_DELAY);
        }
    }

    private float currentMinigameMarkerPos() {
        float t = (clock.getElapsed() - minigameStartElapsed) * 1.6f;
        return (float) (0.5 + 0.5 * Math.sin(t));
    }

    private void handleMinigameStrike() {
        switch (minigameType) {
            case SWEEP -> {
                boolean hit = Math.abs(currentMinigameMarkerPos() - minigameSweetCenter) <= SWEEP_SWEET_WIDTH / 2f;
                finishMinigame(hit, hit ? "Struck true!" : "Missed! Try again.");
            }
            case MASH -> {
                mashCount++;
                if (mashCount >= MASH_REQUIRED) finishMinigame(true, "Mashed it!");
            }
            case REACTION -> {
                float now = clock.getElapsed();
                if (now < reactionGoAt) {
                    finishMinigame(false, "Too early!");
                } else if (now - reactionGoAt <= REACTION_WINDOW) {
                    finishMinigame(true, "Lightning reflexes!");
                } else {
                    finishMinigame(false, "Too slow!");
                }
            }
        }
    }

    private void tickMinigame() {
        if (!minigameActive) return;
        float elapsed = clock.getElapsed() - minigameStartElapsed;
        if (minigameType == MinigameType.MASH && elapsed > MASH_TIME_LIMIT) {
            finishMinigame(false, "Out of time!");
        } else if (minigameType == MinigameType.REACTION
                && clock.getElapsed() - reactionGoAt > REACTION_WINDOW) {
            finishMinigame(false, "Too slow!");
        }
    }

    private void finishMinigame(boolean success, String message) {
        minigameActive = false;
        minigameResultSuccess = success;
        minigameResultTimer = 1.4f;
        if (success) {
            minigameResultText = message + " Purchasing " + minigameEntryName + "...";
            PhoenixNetwork.CHANNEL.sendToServer(new C2SBuyShopEntryPacket(minigameEntryId));
        } else {
            minigameResultText = message;
        }
    }

    private void buildBrowseWidgets(int px, int py, int panelW, int panelH) {
        searchBox = new EditBox(font, px + 8, py + 40, panelW - 16, 16, Component.literal("Search"));
        searchBox.setMaxLength(48);
        searchBox.setValue(searchQuery);
        searchBox.setHint(Component.literal("Search shop...").withStyle(ChatFormatting.DARK_GRAY));
        searchBox.setResponder(s -> {
            searchQuery = s;
            scrollY = 0f;
        });
        addRenderableWidget(searchBox);

    }

    private void buildEditorWidgets(int px, int py, int panelW) {
        boolean itemType = REWARD_TYPES[rewardTypeIndex].equals(ItemShopReward.TYPE);

        int y = py + 46;
        nameBox = new EditBox(font, px + 8, y, panelW - 16, 16, Component.literal("Name"));
        nameBox.setMaxLength(48);
        nameBox.setValue(pendingName);
        nameBox.setResponder(s -> pendingName = s);
        addRenderableWidget(nameBox);
        y += 20;

        addRenderableWidget(Button.builder(Component.literal("-"), b -> adjustCost(-1))
                .bounds(px + 8, y, 16, 16).build());
        costBox = new EditBox(font, px + 26, y, 50, 16, Component.literal("Cost"));
        costBox.setMaxLength(6);
        costBox.setValue(pendingCostStr);
        costBox.setResponder(s -> pendingCostStr = s);
        addRenderableWidget(costBox);
        addRenderableWidget(Button.builder(Component.literal("+"), b -> adjustCost(1))
                .bounds(px + 78, y, 16, 16).build());
        editorCostLabelX = px + 98;
        editorCostLabelY = y + 4;

        addRenderableWidget(Button.builder(Component.literal(itemType ? "Select Product..." : "Pick Icon..."),
                b -> openProductPicker(itemType)).bounds(px + 150, y, panelW - 158, 16).build());
        y += 20;

        categoryBox = new EditBox(font, px + 8, y, 150, 16, Component.literal("Category"));
        categoryBox.setMaxLength(24);
        categoryBox.setValue(pendingCategory);
        categoryBox.setHint(Component.literal("Category (e.g. Materials)").withStyle(ChatFormatting.DARK_GRAY));
        categoryBox.setResponder(s -> pendingCategory = s);
        addRenderableWidget(categoryBox);
        y += 20;

        addRenderableWidget(Button.builder(rewardTypeLabel(), b -> {
            rewardTypeIndex = (rewardTypeIndex + 1) % REWARD_TYPES.length;
            rebuildShopWidgets();
        }).bounds(px + 8, y, 150, 16).build());
        editorIconPreviewX = px + panelW - 24;
        editorIconPreviewY = y + 4;
        y += 20;

        if (itemType) {
            paramBox = null;
        } else {
            String paramHint = switch (REWARD_TYPES[rewardTypeIndex]) {
                case CommandShopReward.TYPE -> "Command (%player% substituted)";
                case ResearchUnlockShopReward.TYPE -> "Research flag id";
                case ThreadShopReward.TYPE -> "Thread amount";
                default -> "";
            };
            paramBox = new EditBox(font, px + 8, y, panelW - 16, 16, Component.literal(paramHint));
            paramBox.setMaxLength(128);
            paramBox.setHint(Component.literal(paramHint).withStyle(ChatFormatting.DARK_GRAY));
            addRenderableWidget(paramBox);
            y += 20;

            addRenderableWidget(Button.builder(Component.literal("+ Add Reward to Entry"), b -> addPendingReward())
                    .bounds(px + 8, y, 150, 16).build());
            y += 20;
        }

        editorRewardListY = y + 4;
        y += Math.max(pendingRewards.size(), 1) * 10 + 8;

        addRenderableWidget(Button.builder(Component.literal(editingEntryId != null ? "Save Changes" : "Submit Entry"),
                b -> submitEntry()).bounds(px + 8, y, 100, 16).build());
        if (!pendingRewards.isEmpty()) {
            addRenderableWidget(Button.builder(Component.literal("Clear Rewards"), b -> {
                pendingRewards.clear();
                rebuildShopWidgets();
            }).bounds(px + 112, y, 100, 16).build());
        }
    }

    private void adjustCost(int delta) {
        int cur;
        try {
            cur = Integer.parseInt(pendingCostStr.trim());
        } catch (NumberFormatException e) {
            cur = 0;
        }
        pendingCostStr = String.valueOf(Math.max(0, cur + delta));
        if (costBox != null) costBox.setValue(pendingCostStr);
    }

    private void openProductPicker(boolean itemType) {
        if (minecraft == null) return;
        minecraft.setScreen(new ItemPickerScreen(this, stacks -> {
            if (itemType) {
                for (ItemStack stack : stacks) addItemReward(stack);
            } else if (!stacks.isEmpty()) {
                pendingIcon = stacks.get(0).copy();
            }
            rebuildShopWidgets();
        }));
    }

    private void addItemReward(ItemStack stack) {
        pendingRewards.add(new RewardSpec(ItemShopReward.TYPE, "", stack.copy()));
        if (pendingIcon.isEmpty()) pendingIcon = stack.copy();
    }

    private void addPendingReward() {
        String type = REWARD_TYPES[rewardTypeIndex];
        String param = paramBox != null ? paramBox.getValue() : "";
        pendingRewards.add(new RewardSpec(type, param));
        if (paramBox != null) paramBox.setValue("");
        rebuildShopWidgets();
    }

    private Component rewardTypeLabel() {
        return Component.literal("Reward: " + REWARD_TYPES[rewardTypeIndex]);
    }

    private void submitEntry() {
        if (nameBox == null || costBox == null) return;

        if (pendingRewards.isEmpty() && paramBox != null && !paramBox.getValue().isBlank()) {
            addPendingReward();
        }
        if (pendingRewards.isEmpty()) {
            return;
        }

        int cost;
        try {
            cost = Math.max(0, Integer.parseInt(costBox.getValue().trim()));
        } catch (NumberFormatException e) {
            cost = 0;
        }
        ItemStack icon = pendingIcon.isEmpty() ? new ItemStack(net.minecraft.world.item.Items.NETHER_STAR)
                : pendingIcon;
        String category = categoryBox != null && !categoryBox.getValue().isBlank()
                ? categoryBox.getValue().trim()
                : ShopEntry.DEFAULT_CATEGORY;

        PhoenixNetwork.CHANNEL.sendToServer(new C2SAddShopEntryPacket(nameBox.getValue(), icon, cost,
                new ArrayList<>(pendingRewards), category, editingEntryId));

        editing = false;
        pendingIcon = ItemStack.EMPTY;
        pendingRewards.clear();
        pendingName = "New Entry";
        pendingCostStr = "1";
        pendingCategory = ShopEntry.DEFAULT_CATEGORY;
        editingEntryId = null;
        rebuildShopWidgets();
    }

    private Mote spawnMote(boolean randomY) {
        Mote m = new Mote();
        m.x = random.nextFloat() * vw;
        m.y = randomY ? random.nextFloat() * vh : vh + 4f;
        m.vx = (random.nextFloat() - 0.5f) * 4f;
        m.vy = -2f - random.nextFloat() * 5f;
        m.size = 0.6f + random.nextFloat() * 1.6f;
        m.maxLife = m.life = 4f + random.nextFloat() * 6f;
        m.color = palette[random.nextInt(palette.length)];
        return m;
    }

    private void tickMotes(float dt) {
        for (int i = motes.size() - 1; i >= 0; i--) {
            Mote m = motes.get(i);
            m.x += m.vx * dt;
            m.y += m.vy * dt;
            m.life -= dt;
            if (m.life <= 0) motes.set(i, spawnMote(false));
        }
    }

    private void enableScissorScaled(GuiGraphics g, int x1, int y1, int x2, int y2) {
        g.enableScissor(Math.round(x1 * uiScale), Math.round(y1 * uiScale), Math.round(x2 * uiScale),
                Math.round(y2 * uiScale));
    }

    @Override
    public void render(GuiGraphics g, int rmx, int rmy, float pt) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;
        clock.tick(dt);
        tickMotes(dt);
        if (warningTimer > 0f) warningTimer -= dt;
        if (minigameResultTimer > 0f) {
            minigameResultTimer -= dt;
            if (minigameResultTimer <= 0f) minigameResultText = null;
        }
        tickMinigame();
        refreshTheme();
        float pulse = MotionClock.Signature.DEFAULT.pulse(clock.getElapsed());

        int mx = Math.round(rmx / uiScale);
        int my = Math.round(rmy / uiScale);

        g.pose().pushPose();
        g.pose().scale(uiScale, uiScale, 1f);

        g.fillGradient(0, 0, vw, vh, cBg1, cBg2);
        renderMotes(g, false);

        int panelW = Math.min(vw - 40, MIN_PANEL_W);
        int panelH = Math.min(vh - 40, MIN_PANEL_H);
        int px = (vw - panelW) / 2;
        int py = (vh - panelH) / 2;
        renderPanel(g, px, py, panelW, panelH, pulse);

        Component title = ChromaticEffectsRegistry.parseCustomEffects("&p Phoenix Feather Shop");
        g.drawString(font, title, px + 12, py + 28, cText, false);

        if (!editing && !minigameActive) {
            List<ShopEntryView> visible = visibleEntries();
            clampScroll(visible.size(), py, panelW, panelH);
            renderTabs(g, px, py, mx, my);
            renderCards(g, visible, px, py, panelW, panelH, mx, my);
        } else if (editing) {
            if (editingEntryId != null) {
                String editingLabel = "Editing: " + pendingName;
                g.drawString(font, Component.literal(editingLabel).withStyle(ChatFormatting.ITALIC),
                        px + panelW - font.width(editingLabel) - 8, py + 28, cDim, false);
            }
            g.drawString(font, "Feathers", editorCostLabelX, editorCostLabelY, cDim, false);
            if (!pendingIcon.isEmpty()) {
                g.renderItem(pendingIcon, editorIconPreviewX, editorIconPreviewY);
                g.drawString(font, "Icon", editorIconPreviewX - 20, editorIconPreviewY + 4, cDim, false);
            }
            if (pendingRewards.isEmpty()) {
                g.drawString(font, "No rewards added yet.", px + 8, editorRewardListY, cDim, false);
            } else {
                for (int i = 0; i < pendingRewards.size(); i++) {
                    RewardSpec spec = pendingRewards.get(i);
                    String line = spec.type().equals(ItemShopReward.TYPE)
                            ? "- item: " + spec.itemParam().getHoverName().getString()
                            : "- " + spec.type() + ": " + spec.param();
                    g.drawString(font, line, px + 8, editorRewardListY + i * 10, cDim, false);
                }
            }
        }

        renderMotes(g, true);

        if (warningTimer > 0f && !editing) {
            String warn = "Not enough Phoenix Feathers for " + warningEntryName + "!";
            g.drawCenteredString(font, warn, px + panelW / 2, py + panelH - 34, 0xFFFF5555);
        }

        if (minigameResultText != null) {
            g.drawCenteredString(font, minigameResultText, px + panelW / 2, py + panelH - 34,
                    minigameResultSuccess ? 0xFF55FF55 : 0xFFFF5555);
        }

        if (minigameActive) {
            renderMinigame(g, px, py, panelW, panelH);
        }

        String hint = "Esc to close";
        g.drawString(font, hint, vw - font.width(hint) - 10, 8, cDim, false);

        if (!editing && !minigameActive && isOp()) {
            String editHint = "Right-click a card to edit it";
            g.drawString(font, editHint, px + panelW - font.width(editHint) - 8, py + 28, cDim, false);
        }

        super.render(g, mx, my, pt);

        g.pose().popPose();
    }

    private void renderMinigame(GuiGraphics g, int px, int py, int panelW, int panelH) {

        g.fill(px, py, px + panelW, py + panelH, 0xFF0A0612);
        int centerX = px + panelW / 2;
        int centerY = py + panelH / 2;

        switch (minigameType) {
            case SWEEP -> renderSweepMinigame(g, px, panelW, centerY);
            case MASH -> renderMashMinigame(g, centerX, centerY);
            case REACTION -> renderReactionMinigame(g, centerX, centerY);
        }
    }

    private void renderSweepMinigame(GuiGraphics g, int px, int panelW, int trackY) {
        int trackW = panelW - 60;
        int trackX = px + 30;
        int trackH = 10;

        g.fill(trackX, trackY, trackX + trackW, trackY + trackH, 0xFF2A2A2A);

        int sweetX = trackX + (int) ((minigameSweetCenter - SWEEP_SWEET_WIDTH / 2f) * trackW);
        int sweetW = (int) (SWEEP_SWEET_WIDTH * trackW);
        g.fill(sweetX, trackY, sweetX + sweetW, trackY + trackH, (0xFF << 24) | (cBorder & 0xFFFFFF));

        float markerPos = currentMinigameMarkerPos();
        int markerX = trackX + (int) (markerPos * trackW);
        g.fill(markerX - 1, trackY - 4, markerX + 2, trackY + trackH + 4, 0xFFFFFFFF);

        String prompt = "Buying " + minigameEntryName + " - click or press Space when the marker hits the glow!";
        g.drawCenteredString(font, prompt, px + panelW / 2, trackY - 20, cText);
    }

    private void renderMashMinigame(GuiGraphics g, int centerX, int centerY) {
        String prompt = "Buying " + minigameEntryName + " - click or press Space fast!";
        g.drawCenteredString(font, prompt, centerX, centerY - 30, cText);

        int barW = 200, barH = 14;
        int barX = centerX - barW / 2, barY = centerY;
        g.fill(barX, barY, barX + barW, barY + barH, 0xFF2A2A2A);
        int fillW = (int) (barW * ((float) mashCount / MASH_REQUIRED));
        g.fill(barX, barY, barX + fillW, barY + barH, (0xFF << 24) | (cBorder & 0xFFFFFF));
        g.drawCenteredString(font, mashCount + " / " + MASH_REQUIRED, centerX, barY + 3, 0xFFFFFFFF);

        float remaining = Math.max(0f, MASH_TIME_LIMIT - (clock.getElapsed() - minigameStartElapsed));
        int timeW = (int) (barW * (remaining / MASH_TIME_LIMIT));
        g.fill(barX, barY + barH + 4, barX + timeW, barY + barH + 7, 0xFFFF5555);
    }

    private void renderReactionMinigame(GuiGraphics g, int centerX, int centerY) {
        float now = clock.getElapsed();
        boolean isGo = now >= reactionGoAt;

        String prompt = "Buying " + minigameEntryName + " - " + (isGo ? "CLICK NOW!" : "Wait for it...");
        int col = isGo ? 0xFF55FF55 : cDim;
        g.drawCenteredString(font, prompt, centerX, centerY - 10, col);

        int r = isGo ? (int) (14 + 4 * MotionClock.Signature.DEFAULT.pulse(clock.getElapsed() * 3f)) : 6;
        int dotCol = isGo ? 0xFF55FF55 : 0xFF883333;
        g.fill(centerX - r, centerY + 6 - r, centerX + r, centerY + 6 + r, dotCol);
    }

    private void renderMotes(GuiGraphics g, boolean front) {
        for (Mote m : motes) {
            float lifeT = m.life / m.maxLife;
            int alpha = (int) (Math.min(1f, lifeT * 3f) * (front ? 140 : 90));
            if (alpha <= 0) continue;
            int col = (alpha << 24) | (m.color & 0xFFFFFF);
            int s = Math.max(1, Math.round(m.size));
            g.fill((int) m.x, (int) m.y, (int) m.x + s, (int) m.y + s, col);
        }
    }

    private void renderPanel(GuiGraphics g, int x, int y, int w, int h, float pulse) {
        g.fillGradient(x, y, x + w, y + h, cPanel1, cPanel2);

        int vignette = 18;
        g.fillGradient(x, y, x + w, y + vignette, 0x99000000, 0x00000000);
        g.fillGradient(x, y + h - vignette, x + w, y + h, 0x00000000, 0x99000000);
        g.fillGradient(x, y, x + vignette, y + h, 0x66000000, 0x00000000);
        g.fillGradient(x + w - vignette, y, x + w, y + h, 0x00000000, 0x66000000);

        int borderCol = (0xFF << 24)
                | (MotionClock.lerpColor(0xFF000000 | cBorderDim, 0xFF000000 | cBorder, pulse)
                        & 0xFFFFFF);
        g.fill(x, y, x + w, y + 1, borderCol);
        g.fill(x, y + h - 1, x + w, y + h, borderCol);
        g.fill(x, y, x + 1, y + h, borderCol);
        g.fill(x + w - 1, y, x + w, y + h, borderCol);

        int cs = 3;
        int glow = (int) (100 + 90 * pulse);
        int cornerCol = (Math.min(255, glow) << 24) | (cBorder & 0xFFFFFF);
        g.fill(x, y, x + cs, y + cs, cornerCol);
        g.fill(x + w - cs, y, x + w, y + cs, cornerCol);
        g.fill(x, y + h - cs, x + cs, y + h, cornerCol);
        g.fill(x + w - cs, y + h - cs, x + w, y + h, cornerCol);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (minigameActive && key == 32) {
            handleMinigameStrike();
            return true;
        }
        if (key == 256 && (nameBox == null || !nameBox.isFocused()) && (paramBox == null || !paramBox.isFocused())
                && (searchBox == null || !searchBox.isFocused())
                && (categoryBox == null || !categoryBox.isFocused())) {
            if (minigameActive) {
                minigameActive = false;
                return true;
            }
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
