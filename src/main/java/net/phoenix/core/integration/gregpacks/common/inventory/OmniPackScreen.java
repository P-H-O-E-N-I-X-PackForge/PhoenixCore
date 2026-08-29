package net.phoenix.core.integration.gregpacks.common.inventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.phoenix.core.integration.gregpacks.network.CPacketFluidInteract;
import net.phoenix.core.integration.gregpacks.network.GregPacksNetwork;
import net.phoenixvine.wiki.theme.PhoenixTheme;

import org.jetbrains.annotations.NotNull;

public class OmniPackScreen extends AbstractContainerScreen<OmniPackMenu> {

    private static final int C_FLUID = 0xFF0099FF;
    private static final int C_EU = 0xFFFFD700;

    private int cBg, cDark, cLight, cSlot, cText, cBar;

    private float uiScale = 1f;
    private int vw, vh;

    private static final int PAD = 7;
    private static final int COLS = 9;
    private static final int SS = 18;

    private static final int BAR_W = 10;
    private static final int BAR_SEP = 4;
    private static final int BAR_GAP = 3;

    private static final int TAB_W = 70;
    private static final int TAB_H = 14;
    private static final int TAB_OFF = 0;

    private static final int UP_COLS = 2;
    private static final int UP_PAD = 6;

    private boolean upOpen = false;
    private int popX, popY, popW, popH;

    private final int packRows;

    public OmniPackScreen(OmniPackMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        packRows = (int) Math.ceil(menu.getPackSlots() / (double) COLS);
        imageWidth = PAD + COLS * SS + PAD;
        imageHeight = PAD + 10 + packRows * SS + PAD + 4 + 3 * SS + PAD + 4 + SS + PAD;
    }

    @Override
    protected void init() {
        super.init();

        int neededW = imageWidth + BAR_GAP + BAR_W + BAR_SEP + BAR_W + 40;
        int neededH = imageHeight + TAB_H + 40;
        uiScale = (width < neededW || height < neededH) ?
                Math.min((float) width / neededW, (float) height / neededH) : 1f;
        uiScale = Math.max(0.1f, uiScale);
        vw = Math.round(width / uiScale);
        vh = Math.round(height / uiScale);
        leftPos = (vw - imageWidth) / 2;
        topPos = (vh - imageHeight) / 2;

        refreshTheme();
        applyMainLayout();
        hideUpSlots();
    }

    private void refreshTheme() {
        PhoenixTheme t = PhoenixTheme.current();
        cBg = t.panel.getColor();
        cDark = t.border.getColor();
        cLight = t.textDim.getColor();
        cSlot = (t.bg.getColor() & 0x00FFFFFF) | 0xFF000000;
        cText = t.text.getColor();
        cBar = (t.bg.getColor() & 0x00FFFFFF) | 0xFF000000;
    }

    @Override
    public void render(@NotNull GuiGraphics g, int rmx, int rmy, float pt) {
        renderBackground(g);
        refreshTheme();

        int mx = Math.round(rmx / uiScale);
        int my = Math.round(rmy / uiScale);

        g.pose().pushPose();
        g.pose().scale(uiScale, uiScale, 1f);

        super.render(g, mx, my, pt);
        renderTab(g, mx, my);
        if (upOpen) renderPopup(g);
        renderBars(g, mx, my);
        renderTooltip(g, mx, my);

        g.pose().popPose();
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, cBg);
        bevel(g, x, y, imageWidth, imageHeight);

        int divY = y + PAD + 10 + packRows * SS + 4;
        g.fill(x + PAD, divY, x + imageWidth - PAD, divY + 1, cDark);

        int uStart = menu.getPackSlots(), uEnd = uStart + menu.getMaxUpgrades();
        for (int i = 0; i < menu.slots.size(); i++) {
            if (i >= uStart && i < uEnd) continue;
            HideableSlot s = menu.getHideableSlot(i);
            if (s.isVisible()) slotInset(g, x + s.x - 1, y + s.y - 1);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, PAD, PAD, cText, false);
        int labelY = PAD + 10 + packRows * SS + 6;
        g.drawString(font, Component.translatable("container.inventory"), PAD, labelY, cText, false);
    }

    private void renderTab(GuiGraphics g, int mx, int my) {
        int tx = leftPos + TAB_OFF;
        int ty = topPos - TAB_H;
        int bg = upOpen ? cBg : 0xFFBBBBBB;

        g.fill(tx, ty, tx + TAB_W, ty + TAB_H, bg);
        g.fill(tx, ty, tx + TAB_W, ty + 1, cDark);
        g.fill(tx, ty, tx + 1, ty + TAB_H, cDark);
        g.fill(tx + TAB_W - 1, ty, tx + TAB_W, ty + TAB_H, cLight);
        g.fill(tx + 1, ty + TAB_H - 1, tx + TAB_W - 1, ty + TAB_H, cBg);

        String lbl = "Upgrades " + (upOpen ? "▼" : "▶");
        g.drawString(font, lbl,
                tx + (TAB_W - font.width(lbl)) / 2,
                ty + (TAB_H - 7) / 2,
                cText, false);
    }

    private void renderPopup(GuiGraphics g) {
        g.fill(popX, popY, popX + popW, popY + popH, cBg);
        bevel(g, popX, popY, popW, popH);

        int uStart = menu.getPackSlots(), uEnd = uStart + menu.getMaxUpgrades();
        for (int i = uStart; i < uEnd; i++) {
            HideableSlot s = menu.getHideableSlot(i);
            if (s.isVisible()) slotInset(g, leftPos + s.x - 1, topPos + s.y - 1);
        }
    }

    private void renderBars(GuiGraphics g, int mx, int my) {
        int bx1 = leftPos + imageWidth + BAR_GAP;
        int bx2 = bx1 + BAR_W + BAR_SEP;
        int by = topPos;
        int bh = imageHeight;

        bar(g, bx1, by, BAR_W, bh);
        int amt = menu.getSyncedFluidAmount(), cap = menu.getSyncedFluidCapacity();
        if (amt > 0 && cap > 0) {
            int fh = (int) ((float) amt / cap * (bh - 2));
            Fluid fl = menu.getSyncedFluid();
            int fc = fl != null ? IClientFluidTypeExtensions.of(fl.getFluidType()).getTintColor() | 0xFF000000 :
                    C_FLUID;
            g.fill(bx1 + 1, by + bh - 1 - fh, bx1 + BAR_W - 1, by + bh - 1, fc);
        }
        if (mx >= bx1 && mx < bx1 + BAR_W && my >= by && my < by + bh) {
            Fluid fl = menu.getSyncedFluid();
            String nm = fl != null ? fl.getFluidType().getDescription().getString() : "Empty";
            g.renderTooltip(font, java.util.List.of(
                    Component.literal("§bFluid: " + nm),
                    Component.literal(amt + " / " + cap + " mB"),
                    Component.literal("§7Left-click to fill, Right-click to drain")),
                    java.util.Optional.empty(), mx, my);
        }

        bar(g, bx2, by, BAR_W, bh);
        long eu = menu.getSyncedEU(), euMax = menu.getSyncedMaxEU();
        if (eu > 0 && euMax > 0) {
            int fh = (int) ((float) eu / euMax * (bh - 2));
            g.fill(bx2 + 1, by + bh - 1 - fh, bx2 + BAR_W - 1, by + bh - 1, C_EU);
        }
        if (mx >= bx2 && mx < bx2 + BAR_W && my >= by && my < by + bh) {
            g.renderTooltip(font, java.util.List.of(
                    Component.literal("§6Energy (EU)"),
                    Component.literal(eu + " / " + euMax + " EU")),
                    java.util.Optional.empty(), mx, my);
        }
    }

    @Override
    public boolean mouseClicked(double rmx, double rmy, int btn) {
        double mx = rmx / uiScale;
        double my = rmy / uiScale;

        if (btn == 0) {
            int tx = leftPos + TAB_OFF, ty = topPos - TAB_H;
            if (mx >= tx && mx < tx + TAB_W && my >= ty && my < ty + TAB_H) {
                upOpen = !upOpen;
                if (upOpen) openPopup();
                else hideUpSlots();
                return true;
            }
        }

        int bx1 = leftPos + imageWidth + BAR_GAP;
        if (mx >= bx1 && mx < bx1 + BAR_W && my >= topPos && my < topPos + imageHeight) {
            if (btn == 0 || btn == 1) {
                GregPacksNetwork.CHANNEL.sendToServer(new CPacketFluidInteract(btn == 0));
                return true;
            }
        }

        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double rmx, double rmy, int button, double dragX, double dragY) {
        return super.mouseDragged(rmx / uiScale, rmy / uiScale, button, dragX / uiScale, dragY / uiScale);
    }

    @Override
    public boolean mouseReleased(double rmx, double rmy, int button) {
        return super.mouseReleased(rmx / uiScale, rmy / uiScale, button);
    }

    @Override
    protected boolean hasClickedOutside(double mx, double my, int guiLeft, int guiTop, int mouseButton) {
        int bx1 = guiLeft + imageWidth + BAR_GAP;
        int bx2 = bx1 + BAR_W + BAR_SEP + BAR_W;
        if (mx >= bx1 && mx <= bx2 && my >= guiTop && my < guiTop + imageHeight) {
            return false;
        }
        return super.hasClickedOutside(mx, my, guiLeft, guiTop, mouseButton);
    }

    private void applyMainLayout() {
        int uStart = menu.getPackSlots(), uEnd = uStart + menu.getMaxUpgrades();

        for (int i = 0; i < uStart; i++)
            show(i, PAD + (i % COLS) * SS, PAD + 10 + (i / COLS) * SS);

        int invY = PAD + 10 + packRows * SS + 8;
        for (int i = 0; i < 27; i++)
            show(uEnd + i, PAD + (i % 9) * SS, invY + (i / 9) * SS);

        int hotY = invY + 3 * SS + 4;
        for (int i = 0; i < 9; i++)
            show(uEnd + 27 + i, PAD + i * SS, hotY);
    }

    private void openPopup() {
        int uStart = menu.getPackSlots(), uEnd = uStart + menu.getMaxUpgrades();
        int rows = (int) Math.ceil(menu.getMaxUpgrades() / (double) UP_COLS);
        popW = UP_PAD + UP_COLS * SS + UP_PAD;
        popH = UP_PAD + rows * SS + UP_PAD;
        popY = topPos;

        int leftCandidate = leftPos - popW - 4;
        popX = leftCandidate >= 2 ? leftCandidate : leftPos + imageWidth + 4;

        for (int i = uStart; i < uEnd; i++) {
            int idx = i - uStart;
            menu.getHideableSlot(i).moveTo(
                    popX - leftPos + UP_PAD + (idx % UP_COLS) * SS,
                    popY - topPos + UP_PAD + (idx / UP_COLS) * SS);
            menu.getHideableSlot(i).show();
        }
    }

    private void hideUpSlots() {
        int uStart = menu.getPackSlots(), uEnd = uStart + menu.getMaxUpgrades();
        for (int i = uStart; i < uEnd; i++) menu.getHideableSlot(i).hide();
    }

    private void show(int i, int x, int y) {
        menu.getHideableSlot(i).moveTo(x, y);
        menu.getHideableSlot(i).show();
    }

    private void bevel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + 1, cDark);
        g.fill(x, y, x + 1, y + h, cDark);
        g.fill(x, y + h - 1, x + w, y + h, cLight);
        g.fill(x + w - 1, y, x + w, y + h, cLight);
    }

    private void slotInset(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + SS, y + 1, cDark);
        g.fill(x, y, x + 1, y + SS, cDark);
        g.fill(x, y + SS - 1, x + SS, y + SS, cLight);
        g.fill(x + SS - 1, y, x + SS, y + SS, cLight);
        g.fill(x + 1, y + 1, x + SS - 1, y + SS - 1, cSlot);
    }

    private void bar(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, cBar);
        bevel(g, x, y, w, h);
    }
}
